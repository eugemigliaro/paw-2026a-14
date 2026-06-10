package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSeries;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.match.*;
import ar.edu.itba.paw.models.exceptions.matchRecurrence.*;
import ar.edu.itba.paw.models.exceptions.matchUpdate.*;
import ar.edu.itba.paw.models.exceptions.pagination.InvalidPaginationException;
import ar.edu.itba.paw.models.exceptions.user.InvalidUserException;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.query.EventTimeFilter;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.ParticipantStatus;
import ar.edu.itba.paw.models.types.RecurrenceEndMode;
import ar.edu.itba.paw.models.types.RecurrenceFrequency;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.services.internal.MatchDataService;
import ar.edu.itba.paw.services.internal.MatchParticipantDataService;
import ar.edu.itba.paw.services.utils.DistanceUtils;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional(readOnly = true)
public class MatchServiceImpl implements MatchService {

    private static final String ADMIN_MOD_AUTHORITY = "ROLE_ADMIN_MOD";
    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PLAYERS_PER_MATCH = 1000;
    private static final int MIN_RECURRING_OCCURRENCES = 2;
    private static final int MAX_RECURRING_OCCURRENCES = 52;

    private final MatchDataService matchDataService;
    private final ImageService imageService;
    private final MatchParticipantDataService matchParticipantDataService;
    private final SecurityService securityService;
    private final MatchNotificationService matchNotificationService;
    private final RecurringMatchAsyncService recurringMatchAsyncService;
    private final Clock clock;

    @Autowired
    public MatchServiceImpl(
            final MatchDataService matchDataService,
            final ImageService imageService,
            final MatchParticipantDataService matchParticipantDataService,
            final MatchNotificationService matchNotificationService,
            final SecurityService securityService,
            final RecurringMatchAsyncService recurringMatchAsyncService,
            final Clock clock) {
        this.matchDataService = matchDataService;
        this.imageService = imageService;
        this.matchParticipantDataService = matchParticipantDataService;
        this.matchNotificationService = matchNotificationService;
        this.securityService = securityService;
        this.recurringMatchAsyncService = recurringMatchAsyncService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Match createMatch(final CreateMatchRequest request) {
        validateScheduleOrThrow(
                toInstant(request.getStartDate(), request.getStartTime()),
                toInstant(request.getEndDate(), request.getEndTime()),
                new MatchInvalidScheduleException());
        validateCreateCapacityOrThrow(request.getMaxPlayers());

        if (request.isRecurring()) {
            return createRecurringMatch(request);
        }

        return createSingleMatch(request);
    }

    private Match createRecurringMatch(final CreateMatchRequest request) {
        final CreateRecurrenceRequest recurrence = request.getRecurrence();
        final List<OccurrenceWindow> occurrences = buildOccurrenceWindows(request, recurrence);

        Instant now = Instant.now(clock);
        final Long seriesId =
                matchDataService.createMatchSeries(
                        request.getHost(),
                        recurrence.getFrequency(),
                        toInstant(request.getStartDate(), request.getStartTime()),
                        toInstant(request.getEndDate(), request.getEndTime()),
                        PlatformTime.ZONE.getId(),
                        recurrence.getEndMode() == RecurrenceEndMode.UNTIL_DATE
                                ? recurrence.getUntilDate()
                                : null,
                        recurrence.getEndMode() == RecurrenceEndMode.OCCURRENCE_COUNT
                                ? recurrence.getOccurrenceCount()
                                : null);

        final MatchSeries series =
                new MatchSeries(
                        seriesId,
                        request.getHost(),
                        recurrence.getFrequency(),
                        toInstant(request.getStartDate(), request.getStartTime()),
                        toInstant(request.getEndDate(), request.getEndTime()),
                        PlatformTime.ZONE.getId(),
                        recurrence.getEndMode() == RecurrenceEndMode.UNTIL_DATE
                                ? recurrence.getUntilDate()
                                : null,
                        recurrence.getEndMode() == RecurrenceEndMode.OCCURRENCE_COUNT
                                ? recurrence.getOccurrenceCount()
                                : null,
                        now,
                        now);

        ImageMetadata bannerImageMetadata =
                imageService.resolveImageMetadata(request.getBannerImage());
        final Match firstOccurrence =
                createSeriesOccurrence(request, occurrences.get(0), series, bannerImageMetadata, 1);
        final List<RecurringMatchAsyncService.OccurrenceWindowData> remainingOccurrences =
                occurrences.subList(1, occurrences.size()).stream()
                        .map(
                                occurrence ->
                                        new RecurringMatchAsyncService.OccurrenceWindowData(
                                                occurrence.startsAt(), occurrence.endsAt()))
                        .toList();
        scheduleRemainingSeriesOccurrenceCreation(
                request, series, bannerImageMetadata, remainingOccurrences, 2);

        return firstOccurrence;
    }

    private void scheduleRemainingSeriesOccurrenceCreation(
            final CreateMatchRequest request,
            final MatchSeries series,
            final ImageMetadata bannerImageMetadata,
            final List<RecurringMatchAsyncService.OccurrenceWindowData> remainingOccurrences,
            final int startIndex) {
        if (remainingOccurrences.isEmpty()) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            recurringMatchAsyncService.createSeriesOccurrencesAsync(
                                    request,
                                    series,
                                    bannerImageMetadata,
                                    remainingOccurrences,
                                    startIndex);
                        }
                    });
            return;
        }

        recurringMatchAsyncService.createSeriesOccurrencesAsync(
                request, series, bannerImageMetadata, remainingOccurrences, startIndex);
    }

    private Match createSingleMatch(final CreateMatchRequest request) {
        ImageMetadata bannerImageMetadata =
                imageService.resolveImageMetadata(request.getBannerImage());
        return matchDataService.createMatch(
                request.getHost(),
                request.getAddress(),
                request.getTitle(),
                request.getDescription(),
                toInstant(request.getStartDate(), request.getStartTime()),
                toInstant(request.getEndDate(), request.getEndTime()),
                request.getMaxPlayers(),
                request.getPricePerPlayer(),
                request.getSport(),
                request.getVisibility(),
                resolveJoinPolicy(request.getVisibility(), request.getJoinPolicy()),
                request.getStatus(),
                bannerImageMetadata,
                request.getLatitude(),
                request.getLongitude());
    }

    private Match createSeriesOccurrence(
            final CreateMatchRequest request,
            final OccurrenceWindow occurrence,
            final MatchSeries series,
            final ImageMetadata bannerImageMetadata,
            final int seriesOccurrenceIndex) {
        return matchDataService.createMatch(
                request.getHost(),
                request.getAddress(),
                request.getTitle(),
                request.getDescription(),
                occurrence.startsAt(),
                occurrence.endsAt(),
                request.getMaxPlayers(),
                request.getPricePerPlayer(),
                request.getSport(),
                request.getVisibility(),
                resolveJoinPolicy(request.getVisibility(), request.getJoinPolicy()),
                request.getStatus(),
                bannerImageMetadata,
                request.getLatitude(),
                request.getLongitude(),
                series,
                seriesOccurrenceIndex);
    }

    private boolean updateStoredMatch(
            final Match match,
            final Long matchId,
            final User actingUser,
            final UpdateMatchRequest request,
            final Instant startsAt,
            final Instant endsAt,
            final ImageMetadata bannerImageMetadata,
            final EventJoinPolicy joinPolicy,
            final EventStatus status) {
        if (!isMatchHost(match, actingUser)) {
            return matchDataService.updateMatch(
                    matchId,
                    request.getAddress(),
                    request.getTitle(),
                    request.getDescription(),
                    startsAt,
                    endsAt,
                    request.getMaxPlayers(),
                    request.getPricePerPlayer(),
                    request.getSport(),
                    request.getVisibility(),
                    joinPolicy,
                    status,
                    bannerImageMetadata,
                    request.getLatitude(),
                    request.getLongitude());
        }
        return matchDataService.updateMatch(
                matchId,
                actingUser,
                request.getAddress(),
                request.getTitle(),
                request.getDescription(),
                startsAt,
                endsAt,
                request.getMaxPlayers(),
                request.getPricePerPlayer(),
                request.getSport(),
                request.getVisibility(),
                joinPolicy,
                status,
                bannerImageMetadata,
                request.getLatitude(),
                request.getLongitude());
    }

    private static EventJoinPolicy resolveJoinPolicy(
            final EventVisibility visibility, final EventJoinPolicy joinPolicy) {
        return EventVisibility.PRIVATE == visibility ? EventJoinPolicy.INVITE_ONLY : joinPolicy;
    }

    private static Instant toInstant(final LocalDate date, final LocalTime time) {
        return date == null || time == null ? null : PlatformTime.toInstant(date, time);
    }

    private static boolean hasCoordinates(final Double latitude, final Double longitude) {
        return latitude != null && longitude != null;
    }

    @Override
    @Transactional
    public Match updateMatch(
            final Long matchId, final User actingUser, final UpdateMatchRequest request) {
        final Match match = findEditableMatchForHost(matchId, actingUser);

        final Instant startsAt = toInstant(request.getStartDate(), request.getStartTime());
        final Instant endsAt = toInstant(request.getEndDate(), request.getEndTime());

        validateScheduleOrThrow(startsAt, endsAt, new MatchUpdateInvalidScheduleException());
        validateUpdateCapacityOrThrow(request.getMaxPlayers());

        final int confirmedParticipants =
                matchParticipantDataService.findConfirmedParticipants(matchId).size();
        if (request.getMaxPlayers() < confirmedParticipants) {
            throw new MatchUpdateCapacityBelowConfirmedException();
        }
        final ParticipationPolicyTransition participationPolicyTransition =
                validateAndPlanParticipationPolicyTransition(match, request, confirmedParticipants);

        final EventJoinPolicy joinPolicy =
                EventVisibility.PRIVATE == request.getVisibility()
                        ? EventJoinPolicy.INVITE_ONLY
                        : request.getJoinPolicy();

        ImageMetadata bannerImageMetadata =
                imageService.resolveImageMetadata(request.getBannerImage());
        if (bannerImageMetadata == null) {
            bannerImageMetadata = match.getBannerImageMetadata();
        }
        final boolean updated =
                updateStoredMatch(
                        match,
                        matchId,
                        actingUser,
                        request,
                        startsAt,
                        endsAt,
                        bannerImageMetadata,
                        joinPolicy,
                        request.getStatus());

        if (!updated) {
            throw new MatchForbiddenActionException();
        }

        final Match updatedMatch =
                matchDataService.findById(matchId).orElseThrow(() -> new MatchNotFoundException());
        applyParticipationPolicyTransition(updatedMatch, participationPolicyTransition);
        matchNotificationService.notifyMatchUpdated(updatedMatch);
        return updatedMatch;
    }

    @Override
    public Match findEditableMatchForHost(final Long matchId, final User actingUser) {
        final Match match =
                matchDataService.findById(matchId).orElseThrow(() -> new MatchNotFoundException());
        validateMatchUpdateAccess(match, actingUser);
        return match;
    }

    @Override
    public Match findEditableRecurringMatchForHost(final Long matchId, final User actingUser) {
        final Match match = findEditableMatchForHost(matchId, actingUser);
        if (!match.isRecurringOccurrence()) {
            throw new MatchNotRecurringException();
        }
        return match;
    }

    private ParticipationPolicyTransition validateAndPlanParticipationPolicyTransition(
            final Match match, final UpdateMatchRequest request, final int confirmedParticipants) {
        if (wasPrivate(match) && isPublic(request)) {
            return ParticipationPolicyTransition.cancelInvitations(
                    matchParticipantDataService.findInvitedUsers(match.getId()));
        }
        if (wasApprovalRequired(match) && isPrivate(request)) {
            return ParticipationPolicyTransition.cancelPendingRequests(
                    matchParticipantDataService.findPendingRequests(match.getId()));
        }
        if (wasApprovalRequired(match) && isDirectPublic(request)) {
            final int pendingRequests =
                    matchParticipantDataService.countPendingRequests(match.getId());
            final int availableSpots = request.getMaxPlayers() - confirmedParticipants;
            if (pendingRequests > availableSpots) {
                throw new MatchUpdatePendingRequestsExceedAvailableException();
            }
            return ParticipationPolicyTransition.approvePendingRequests(
                    matchParticipantDataService.findPendingRequests(match.getId()));
        }
        return ParticipationPolicyTransition.none();
    }

    private void applyParticipationPolicyTransition(
            final Match updatedMatch, final ParticipationPolicyTransition transition) {
        if (transition.isEmpty()) {
            return;
        }
        if (transition.type() == ParticipationPolicyTransitionType.CANCEL_INVITATIONS) {
            matchNotificationService.notifyInvitationOpenedToPublic(
                    updatedMatch, transition.affectedUsers());
            matchParticipantDataService.cancelPendingInvitations(updatedMatch.getId());
            return;
        }
        if (transition.type() == ParticipationPolicyTransitionType.CANCEL_PENDING_REQUESTS) {
            matchNotificationService.notifyPendingRequestClosedByPrivacyChange(
                    updatedMatch, transition.affectedUsers());
            matchParticipantDataService.cancelPendingRequests(updatedMatch.getId());
            return;
        }
        matchParticipantDataService.approveAllPendingRequests(updatedMatch.getId());
        for (final User user : transition.affectedUsers()) {
            matchNotificationService.notifyPlayerRequestApproved(updatedMatch, user);
        }
    }

    private static boolean wasPrivate(final Match match) {
        return EventVisibility.PRIVATE == match.getVisibility();
    }

    private static boolean wasApprovalRequired(final Match match) {
        return EventVisibility.PUBLIC == match.getVisibility()
                && EventJoinPolicy.APPROVAL_REQUIRED == match.getJoinPolicy();
    }

    private static boolean isPublic(final UpdateMatchRequest request) {
        return EventVisibility.PUBLIC == request.getVisibility();
    }

    private static boolean isPrivate(final UpdateMatchRequest request) {
        return EventVisibility.PRIVATE == request.getVisibility();
    }

    private static boolean isDirectPublic(final UpdateMatchRequest request) {
        return isPublic(request) && EventJoinPolicy.DIRECT == request.getJoinPolicy();
    }

    private enum ParticipationPolicyTransitionType {
        NONE,
        CANCEL_INVITATIONS,
        CANCEL_PENDING_REQUESTS,
        APPROVE_PENDING_REQUESTS
    }

    private record ParticipationPolicyTransition(
            ParticipationPolicyTransitionType type, List<User> affectedUsers) {

        private static ParticipationPolicyTransition none() {
            return new ParticipationPolicyTransition(
                    ParticipationPolicyTransitionType.NONE, List.of());
        }

        private static ParticipationPolicyTransition cancelInvitations(final List<User> users) {
            return new ParticipationPolicyTransition(
                    ParticipationPolicyTransitionType.CANCEL_INVITATIONS, safeUsers(users));
        }

        private static ParticipationPolicyTransition cancelPendingRequests(final List<User> users) {
            return new ParticipationPolicyTransition(
                    ParticipationPolicyTransitionType.CANCEL_PENDING_REQUESTS, safeUsers(users));
        }

        private static ParticipationPolicyTransition approvePendingRequests(
                final List<User> users) {
            return new ParticipationPolicyTransition(
                    ParticipationPolicyTransitionType.APPROVE_PENDING_REQUESTS, safeUsers(users));
        }

        private boolean isEmpty() {
            return type == ParticipationPolicyTransitionType.NONE || affectedUsers.isEmpty();
        }

        private static List<User> safeUsers(final List<User> users) {
            if (users == null || users.isEmpty()) {
                return List.of();
            }
            return List.copyOf(users);
        }
    }

    @Override
    @Transactional
    public List<Match> updateSeriesFromOccurrence(
            final Long matchId, final User actingUser, final UpdateMatchRequest request) {
        final Match pivot =
                matchDataService.findById(matchId).orElseThrow(() -> new MatchNotFoundException());

        validateSeriesUpdateAccess(pivot, actingUser);

        final Instant requestStartsAt = toInstant(request.getStartDate(), request.getStartTime());
        final Instant requestEndsAt = toInstant(request.getEndDate(), request.getEndTime());

        validateScheduleOrThrow(
                requestStartsAt, requestEndsAt, new MatchUpdateInvalidScheduleException());
        validateUpdateCapacityOrThrow(request.getMaxPlayers());

        final List<Match> targets = editableFutureSeriesTargets(pivot);
        if (targets.isEmpty()) {
            throw new MatchUpdateNotEditableException();
        }

        for (final Match target : targets) {
            final int confirmedParticipants =
                    matchParticipantDataService.findConfirmedParticipants(target.getId()).size();
            if (request.getMaxPlayers() < confirmedParticipants) {
                throw new MatchUpdateCapacityBelowConfirmedException();
            }
        }

        final Duration startOffset = Duration.between(pivot.getStartsAt(), requestStartsAt);
        final Duration requestedDuration =
                requestEndsAt == null ? null : Duration.between(requestStartsAt, requestEndsAt);
        final List<Match> updatedMatches = new ArrayList<>();

        ImageMetadata bannerImageMetadata =
                imageService.resolveImageMetadata(request.getBannerImage());
        if (bannerImageMetadata == null) {
            bannerImageMetadata = pivot.getBannerImageMetadata();
        }

        for (final Match target : targets) {
            final Instant targetStartsAt = target.getStartsAt().plus(startOffset);
            final Instant targetEndsAt =
                    requestedDuration == null ? null : targetStartsAt.plus(requestedDuration);
            validateScheduleOrThrow(
                    targetStartsAt, targetEndsAt, new MatchUpdateInvalidScheduleException());
            final boolean updated =
                    updateStoredMatch(
                            target,
                            target.getId(),
                            actingUser,
                            request,
                            targetStartsAt,
                            targetEndsAt,
                            bannerImageMetadata,
                            resolveJoinPolicy(request.getVisibility(), request.getJoinPolicy()),
                            target.getStatus());
            if (!updated) {
                throw new MatchForbiddenActionException();
            }

            final Match updatedMatch =
                    matchDataService
                            .findById(target.getId())
                            .orElseThrow(() -> new MatchNotFoundException());
            updatedMatches.add(updatedMatch);
        }

        matchNotificationService.notifyRecurringMatchesUpdated(updatedMatches);
        return List.copyOf(updatedMatches);
    }

    @Override
    @Transactional
    public Match cancelMatch(final Long matchId, final User actingUser) {
        final Match match =
                matchDataService.findById(matchId).orElseThrow(() -> new MatchNotFoundException());

        validateMatchCancellationAccess(match, actingUser);

        if (EventStatus.COMPLETED.equals(match.getStatus())) {
            throw new MatchForbiddenActionException();
        }

        if (EventStatus.CANCELLED.equals(match.getStatus())) {
            return match;
        }

        if (hasMatchEnded(match)) {
            throw new MatchForbiddenActionException();
        }

        final boolean updated = cancelStoredMatch(match, actingUser);
        if (!updated) {
            throw new MatchForbiddenActionException();
        }

        final Match cancelledMatch =
                matchDataService.findById(matchId).orElseThrow(() -> new MatchNotFoundException());
        matchNotificationService.notifyMatchCancelled(cancelledMatch);
        return cancelledMatch;
    }

    @Override
    @Transactional
    public List<Match> cancelSeriesFromOccurrence(final Long matchId, final User actingUser) {
        final Match pivot =
                matchDataService.findById(matchId).orElseThrow(() -> new MatchNotFoundException());

        validateSeriesCancellationAccess(pivot, actingUser);

        final List<Match> targets = cancellableFutureSeriesTargets(pivot);
        if (targets.isEmpty()) {
            throw new MatchForbiddenActionException();
        }

        final List<Match> cancelledMatches = new ArrayList<>();
        for (final Match target : targets) {
            final boolean updated = cancelStoredMatch(target, actingUser);
            if (!updated) {
                throw new MatchForbiddenActionException();
            }

            final Match cancelledMatch =
                    matchDataService
                            .findById(target.getId())
                            .orElseThrow(() -> new MatchNotFoundException());
            cancelledMatches.add(cancelledMatch);
        }

        matchNotificationService.notifyRecurringMatchesCancelled(cancelledMatches);
        return List.copyOf(cancelledMatches);
    }

    private void validateSeriesUpdateAccess(final Match pivot, final User actingUser) {
        validateMatchManagementAccess(pivot, actingUser);
        if (!pivot.isRecurringOccurrence()) {
            throw new MatchNotRecurringException();
        }
        validateEditableMatch(pivot);
    }

    private void validateMatchUpdateAccess(final Match match, final User actingUser) {
        validateMatchManagementAccess(match, actingUser);
        validateEditableMatch(match);
    }

    private void validateEditableMatch(final Match match) {
        if (!isEditableMatch(match)) {
            throw new MatchUpdateNotEditableException();
        }
    }

    private void validateMatchManagementAccess(final Match match, final User actingUser) {
        if (!canManageMatch(match, actingUser)) {
            throw new MatchForbiddenActionException();
        }
    }

    private void validateMatchCancellationAccess(final Match match, final User actingUser) {
        if (!canManageMatch(match, actingUser)) {
            throw new MatchForbiddenActionException();
        }
    }

    private void validateSeriesCancellationAccess(final Match pivot, final User actingUser) {
        if (!canManageMatch(pivot, actingUser)) {
            throw new MatchForbiddenActionException();
        }

        if (!pivot.isRecurringOccurrence()) {
            throw new MatchNotRecurringException();
        }

        if (!isMatchManagementLifecycleOpen(pivot)) {
            throw new MatchForbiddenActionException();
        }
    }

    private boolean cancelStoredMatch(final Match match, final User actingUser) {
        if (isMatchHost(match, actingUser)) {
            return matchDataService.cancelMatch(match.getId(), actingUser);
        }
        return matchDataService.cancelMatch(match.getId());
    }

    private boolean canManageMatch(final Match match, final User actingUser) {
        return isMatchHost(match, actingUser) || securityService.canActAsAdminMod(actingUser);
    }

    private List<Match> editableFutureSeriesTargets(final Match pivot) {
        if (!pivot.isRecurringOccurrence() || pivot.getSeriesOccurrenceIndex() == null) {
            return List.of();
        }

        final int pivotIndex = pivot.getSeriesOccurrenceIndex();
        final Instant now = Instant.now(clock);
        return matchDataService.findSeriesOccurrences(pivot.getSeries().getId()).stream()
                .filter(occurrence -> occurrence.getSeriesOccurrenceIndex() != null)
                .filter(occurrence -> occurrence.getSeriesOccurrenceIndex() >= pivotIndex)
                .filter(occurrence -> occurrence.getStartsAt().isAfter(now))
                .filter(this::isEditableMatch)
                .toList();
    }

    private List<Match> cancellableFutureSeriesTargets(final Match pivot) {
        if (!pivot.isRecurringOccurrence()) {
            return List.of();
        }

        final Instant now = Instant.now(clock);
        return matchDataService.findSeriesOccurrences(pivot.getSeries().getId()).stream()
                .filter(occurrence -> occurrence.getStartsAt().isAfter(now))
                .filter(this::isEditableMatch)
                .toList();
    }

    private boolean isEditableMatch(final Match match) {
        final Instant startsAt = match.getStartsAt();
        return match.getStatus() != EventStatus.COMPLETED
                && match.getStatus() != EventStatus.CANCELLED
                && startsAt != null
                && startsAt.isAfter(Instant.now(clock));
    }

    private boolean hasEventStarted(final Match match) {
        return match.getStartsAt() != null && !match.getStartsAt().isAfter(Instant.now(clock));
    }

    private static boolean isHost(final Match match, final User viewer) {
        return viewer != null
                && viewer.getId() != null
                && match.getHost() != null
                && viewer.getId().equals(match.getHost().getId());
    }

    private boolean canMutate(final Match match, final User actingUser) {
        return isHost(match, actingUser) || isAdminMod();
    }

    private static boolean isVisibleToViewer(
            final Match match,
            final boolean hostViewer,
            final boolean manager,
            final boolean activeParticipant,
            final boolean invitedViewer) {
        if (EventStatus.DRAFT == match.getStatus()) {
            return manager;
        }
        if (match.getVisibility() == EventVisibility.PRIVATE
                || EventStatus.CANCELLED == match.getStatus()) {
            return hostViewer || manager || activeParticipant || invitedViewer;
        }
        return match.getVisibility() == EventVisibility.PUBLIC;
    }

    private boolean isAdminMod() {
        final Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .filter(Objects::nonNull)
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_MOD_AUTHORITY::equals);
    }

    @Override
    public Optional<Match> findMatchById(final Long matchId) {
        return matchDataService.findById(matchId);
    }

    @Override
    public Optional<Match> findVisibleMatchById(final Long matchId, final User viewer) {
        return matchDataService.findById(matchId).filter(match -> canViewMatch(match, viewer));
    }

    @Override
    public boolean canViewMatch(final Match match, final User viewer) {
        if (match == null) {
            return false;
        }
        if (securityService.canActAsAdminMod(viewer)) {
            return true;
        }
        if (EventStatus.DRAFT == match.getStatus()) {
            return isMatchHost(match, viewer);
        }
        if (EventVisibility.PRIVATE == match.getVisibility()
                || EventStatus.CANCELLED == match.getStatus()) {
            return hasMatchRelationship(match, viewer);
        }
        return EventVisibility.PUBLIC == match.getVisibility();
    }

    @Override
    public MatchManagementPermissions getMatchManagementPermissions(
            final Match match, final User viewer) {
        if (match == null || viewer == null) {
            return MatchManagementPermissions.none();
        }

        final boolean hostViewer = isMatchHost(match, viewer);
        final boolean elevatedViewer = securityService.canActAsAdminMod(viewer);
        final boolean canManage = hostViewer || elevatedViewer;
        final boolean lifecycleAllowsManagement = isMatchManagementLifecycleOpen(match);
        final boolean canManageCurrentMatch = canManage && lifecycleAllowsManagement;
        final boolean recurringOccurrence = match.isRecurringOccurrence();

        return new MatchManagementPermissions(
                hostViewer,
                canManage,
                hostViewer && lifecycleAllowsManagement,
                canManageCurrentMatch,
                canManageCurrentMatch,
                canManageCurrentMatch && recurringOccurrence,
                canManageCurrentMatch && recurringOccurrence);
    }

    @Override
    public MatchInteractionState getMatchInteractionState(
            final Match match, final List<Match> seriesOccurrences, final User viewer) {
        if (match == null) {
            return emptyInteractionState(viewer);
        }

        final boolean hostViewer = isMatchHost(match, viewer);
        final boolean authenticated = viewer != null;
        final boolean confirmedParticipant =
                authenticated
                        && matchParticipantDataService.hasActiveReservation(match.getId(), viewer);
        final boolean rawPendingJoinRequest =
                authenticated
                        && !hostViewer
                        && EventJoinPolicy.APPROVAL_REQUIRED == match.getJoinPolicy()
                        && matchParticipantDataService.hasPendingRequest(match.getId(), viewer);
        final boolean invitedPlayer =
                authenticated
                        && !hostViewer
                        && EventJoinPolicy.INVITE_ONLY == match.getJoinPolicy()
                        && matchParticipantDataService.hasInvitation(match.getId(), viewer);
        final List<Match> occurrences =
                seriesOccurrences == null ? List.of() : List.copyOf(seriesOccurrences);
        final SeriesReservationState seriesReservationState =
                buildSeriesReservationState(match, occurrences, viewer, hostViewer);
        final SeriesJoinRequestState seriesJoinRequestState =
                hostViewer
                        ? new SeriesJoinRequestState(false, false)
                        : buildSeriesJoinRequestState(match, occurrences, viewer);

        return new MatchInteractionState(
                confirmedParticipant,
                rawPendingJoinRequest && !seriesJoinRequestState.pending(),
                invitedPlayer,
                isMatchReservable(match, hostViewer),
                confirmedParticipant && isReservationCancellable(match),
                seriesReservationState.available(),
                seriesReservationState.joined(),
                seriesReservationState.cancellable(),
                !hostViewer && isJoinRequestable(match) && !seriesJoinRequestState.pending(),
                seriesJoinRequestState.available(),
                seriesJoinRequestState.pending(),
                !authenticated,
                !authenticated,
                !authenticated);
    }

    private static MatchInteractionState emptyInteractionState(final User viewer) {
        if (viewer == null) {
            return MatchInteractionState.anonymous();
        }
        return new MatchInteractionState(
                false, false, false, false, false, false, false, false, false, false, false, false,
                false, false);
    }

    private SeriesReservationState buildSeriesReservationState(
            final Match match,
            final List<Match> occurrences,
            final User viewer,
            final boolean hostViewer) {
        if (!match.isRecurringOccurrence() || occurrences.isEmpty()) {
            return new SeriesReservationState(false, false, false);
        }

        final Set<Long> activeFutureReservationMatchIds =
                viewer == null
                        ? Set.of()
                        : Set.copyOf(
                                matchParticipantDataService
                                        .findActiveFutureReservationMatchIdsForSeries(
                                                match.getSeries().getId(),
                                                viewer,
                                                Instant.now(clock)));
        final SeriesReservationEvaluation evaluation =
                evaluateSeriesReservationTargets(
                        occurrences, viewer, activeFutureReservationMatchIds, hostViewer);
        return new SeriesReservationState(
                !evaluation.targetMatchIds().isEmpty(),
                evaluation.joined(),
                evaluation.activeFutureReservationCount() > 0);
    }

    private SeriesReservationEvaluation evaluateSeriesReservationTargets(
            final List<Match> occurrences,
            final User viewer,
            final Set<Long> activeFutureReservationMatchIds,
            final boolean hostViewer) {
        final List<Long> targetMatchIds = new ArrayList<>();
        int futureOpenOccurrenceCount = 0;
        int joinedFutureOpenOccurrenceCount = 0;
        int activeFutureReservationCount = 0;
        final Instant now = Instant.now(clock);

        for (final Match occurrence : occurrences) {
            if (!occurrence.getStartsAt().isAfter(now)) {
                continue;
            }

            final boolean alreadyJoined =
                    activeFutureReservationMatchIds.contains(occurrence.getId());
            if (alreadyJoined) {
                activeFutureReservationCount++;
            }

            if (!isSeriesReservationOpenOccurrence(occurrence, hostViewer)) {
                continue;
            }

            futureOpenOccurrenceCount++;
            if (alreadyJoined) {
                joinedFutureOpenOccurrenceCount++;
                continue;
            }

            if (occurrence.getAvailableSpots() <= 0) {
                continue;
            }

            targetMatchIds.add(occurrence.getId());
        }

        final boolean joined =
                viewer != null
                        && futureOpenOccurrenceCount > 0
                        && joinedFutureOpenOccurrenceCount == futureOpenOccurrenceCount;
        return new SeriesReservationEvaluation(
                List.copyOf(targetMatchIds), joined, activeFutureReservationCount);
    }

    private static boolean isSeriesReservationOpenOccurrence(
            final Match occurrence, final boolean hostViewer) {
        return EventStatus.OPEN == occurrence.getStatus()
                && (hostViewer
                        || (occurrence.getVisibility() == EventVisibility.PUBLIC
                                && occurrence.getJoinPolicy() == EventJoinPolicy.DIRECT));
    }

    private SeriesJoinRequestState buildSeriesJoinRequestState(
            final Match match, final List<Match> occurrences, final User viewer) {
        if (!match.isRecurringOccurrence()) {
            return new SeriesJoinRequestState(false, false);
        }

        if (viewer != null
                && matchParticipantDataService.hasPendingSeriesRequest(
                        match.getSeries().getId(), viewer)) {
            return new SeriesJoinRequestState(false, true);
        }

        final Instant now = Instant.now(clock);
        final Set<Long> activeFutureReservationMatchIds =
                viewer == null
                        ? Set.of()
                        : Set.copyOf(
                                matchParticipantDataService
                                        .findActiveFutureReservationMatchIdsForSeries(
                                                match.getSeries().getId(), viewer, now));
        final Set<Long> pendingFutureRequestMatchIds =
                viewer == null
                        ? Set.of()
                        : Set.copyOf(
                                matchParticipantDataService
                                        .findPendingFutureRequestMatchIdsForSeries(
                                                match.getSeries().getId(), viewer, now));
        final SeriesJoinRequestEvaluation evaluation =
                evaluateSeriesJoinRequestTargets(
                        occurrences,
                        viewer,
                        activeFutureReservationMatchIds,
                        pendingFutureRequestMatchIds);
        return new SeriesJoinRequestState(
                !evaluation.targetMatchIds().isEmpty(), evaluation.pending());
    }

    private SeriesJoinRequestEvaluation evaluateSeriesJoinRequestTargets(
            final List<Match> occurrences,
            final User viewer,
            final Set<Long> activeFutureReservationMatchIds,
            final Set<Long> pendingFutureRequestMatchIds) {
        final List<Long> targetMatchIds = new ArrayList<>();
        int futureOpenApprovalOccurrenceCount = 0;
        int pendingFutureOpenApprovalOccurrenceCount = 0;
        final Instant now = Instant.now(clock);

        for (final Match occurrence : occurrences) {
            if (!occurrence.getStartsAt().isAfter(now)
                    || !isSeriesJoinRequestOpenOccurrence(occurrence)) {
                continue;
            }

            futureOpenApprovalOccurrenceCount++;
            if (activeFutureReservationMatchIds.contains(occurrence.getId())) {
                continue;
            }

            if (pendingFutureRequestMatchIds.contains(occurrence.getId())) {
                pendingFutureOpenApprovalOccurrenceCount++;
                continue;
            }

            if (occurrence.getAvailableSpots() <= 0) {
                continue;
            }

            targetMatchIds.add(occurrence.getId());
        }

        final boolean pending =
                viewer != null
                        && futureOpenApprovalOccurrenceCount > 0
                        && pendingFutureOpenApprovalOccurrenceCount
                                == futureOpenApprovalOccurrenceCount;
        return new SeriesJoinRequestEvaluation(List.copyOf(targetMatchIds), pending);
    }

    private static boolean isSeriesJoinRequestOpenOccurrence(final Match occurrence) {
        return occurrence.getVisibility() == EventVisibility.PUBLIC
                && occurrence.getJoinPolicy() == EventJoinPolicy.APPROVAL_REQUIRED
                && EventStatus.OPEN == occurrence.getStatus();
    }

    private boolean isMatchReservable(final Match match, final boolean hostViewer) {
        return EventStatus.OPEN == match.getStatus()
                && (hostViewer
                        || (match.getVisibility() == EventVisibility.PUBLIC
                                && match.getJoinPolicy() == EventJoinPolicy.DIRECT))
                && !hasMatchStarted(match)
                && match.getAvailableSpots() > 0;
    }

    private boolean isJoinRequestable(final Match match) {
        return match.getVisibility() == EventVisibility.PUBLIC
                && match.getJoinPolicy() == EventJoinPolicy.APPROVAL_REQUIRED
                && EventStatus.OPEN == match.getStatus()
                && !hasMatchStarted(match)
                && match.getAvailableSpots() > 0;
    }

    private boolean isReservationCancellable(final Match match) {
        return EventStatus.OPEN == match.getStatus() && !hasMatchStarted(match);
    }

    private boolean hasMatchRelationship(final Match match, final User viewer) {
        return isMatchHost(match, viewer)
                || (viewer != null
                        && (matchParticipantDataService.hasActiveReservation(match.getId(), viewer)
                                || matchParticipantDataService.hasInvitation(
                                        match.getId(), viewer)));
    }

    private static boolean isMatchHost(final Match match, final User viewer) {
        return viewer != null
                && match.getHost() != null
                && Objects.equals(viewer.getId(), match.getHost().getId());
    }

    private boolean isMatchManagementLifecycleOpen(final Match match) {
        return EventStatus.COMPLETED != match.getStatus()
                && EventStatus.CANCELLED != match.getStatus()
                && !hasMatchEnded(match);
    }

    private boolean hasMatchEnded(final Match match) {
        final Instant endsAt = match.getEndsAt() == null ? match.getStartsAt() : match.getEndsAt();
        return !endsAt.isAfter(Instant.now(clock));
    }

    private boolean hasMatchStarted(final Match match) {
        return !match.getStartsAt().isAfter(Instant.now(clock));
    }

    @Override
    public Optional<Match> findPublicMatchById(final Long matchId) {
        return matchDataService.findPublicMatchById(matchId);
    }

    @Override
    public MatchActionCapabilities actionCapabilities(final Match match, final User viewer) {
        if (match == null || match.getId() == null) {
            return new MatchActionCapabilities(
                    false, false, false, false, false, false, false, false, false);
        }

        final boolean hostViewer = isHost(match, viewer);
        final boolean manager = canMutate(match, viewer);
        final boolean activeParticipant =
                viewer != null
                        && matchParticipantDataService.hasActiveReservation(match.getId(), viewer);
        final boolean invitedViewer =
                viewer != null && matchParticipantDataService.hasInvitation(match.getId(), viewer);
        final boolean visible =
                isVisibleToViewer(match, hostViewer, manager, activeParticipant, invitedViewer);
        final boolean editable = manager && isEditableMatch(match);
        final boolean reservable =
                EventStatus.OPEN == match.getStatus()
                        && !hasEventStarted(match)
                        && match.getAvailableSpots() > 0
                        && (hostViewer
                                || (match.getVisibility() == EventVisibility.PUBLIC
                                        && match.getJoinPolicy() == EventJoinPolicy.DIRECT));
        final boolean cancellableReservation =
                activeParticipant
                        && EventStatus.OPEN == match.getStatus()
                        && !hasEventStarted(match);
        final boolean requestable =
                EventVisibility.PUBLIC == match.getVisibility()
                        && EventJoinPolicy.APPROVAL_REQUIRED == match.getJoinPolicy()
                        && EventStatus.OPEN == match.getStatus()
                        && !hasEventStarted(match)
                        && match.getAvailableSpots() > 0
                        && !hostViewer
                        && !activeParticipant
                        && (viewer == null
                                || !matchParticipantDataService.hasPendingRequest(
                                        match.getId(), viewer));

        return new MatchActionCapabilities(
                visible,
                editable,
                editable,
                editable,
                reservable,
                cancellableReservation,
                requestable,
                editable && match.isRecurringOccurrence(),
                editable && match.isRecurringOccurrence());
    }

    @Override
    public List<Match> findSeriesOccurrences(final Long seriesId) {
        if (seriesId == null) {
            return List.of();
        }
        return matchDataService.findSeriesOccurrences(seriesId);
    }

    @Override
    public PaginatedResult<Match> findSeriesOccurrencesPage(
            final Long seriesId, final int page, final int pageSize) {
        if (seriesId == null) {
            final int safePageSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
            return new PaginatedResult<>(List.of(), 0, 1, safePageSize);
        }
        return matchDataService.findSeriesOccurrencesPage(seriesId, page, pageSize);
    }

    @Override
    public List<User> findConfirmedParticipants(final Long matchId) {
        return matchParticipantDataService.findConfirmedParticipants(matchId);
    }

    @Override
    public PaginatedResult<Match> searchPublicMatches(
            final String query,
            final List<Sport> sport,
            final LocalDate startDate,
            final LocalDate endDate,
            final EventSort sort,
            final int page,
            final int pageSize,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final Double latitude,
            final Double longitude) {
        final EventSort sortFilter =
                hasCoordinates(latitude, longitude) ? sort : withoutDistance(sort);
        final DateRange dateRange = DateRange.of(startDate, endDate);

        final PaginatedResult<Match> result =
                paginate(
                        page,
                        pageSize,
                        DEFAULT_PAGE_SIZE,
                        safePageSize ->
                                matchDataService.countPublicMatches(
                                        query,
                                        sport,
                                        EventTimeFilter.ALL,
                                        dateRange.start(),
                                        dateRange.endExclusive(),
                                        minPrice,
                                        maxPrice),
                        (offset, safePageSize) ->
                                matchDataService.findPublicMatches(
                                        query,
                                        sport,
                                        EventTimeFilter.ALL,
                                        dateRange.start(),
                                        dateRange.endExclusive(),
                                        minPrice,
                                        maxPrice,
                                        sortFilter,
                                        latitude,
                                        longitude,
                                        offset,
                                        safePageSize));

        if (sort == EventSort.DISTANCE && hasCoordinates(latitude, longitude)) {
            hydrateDistances(result.getItems(), latitude, longitude);
        }

        return result;
    }

    @Override
    public PaginatedResult<Match> findDashboardMatches(
            User user,
            Boolean upcoming,
            Boolean includeHosted,
            String query,
            List<Sport> sports,
            List<EventStatus> statuses,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            EventSort sort,
            List<ParticipantStatus> participantStatuses,
            int page,
            int pageSize) {
        nonNullUser(user);
        validatePageAndSizeOrThrow(page, pageSize, DEFAULT_PAGE_SIZE);

        final int offset = (page - 1) * pageSize;
        final int limit = pageSize;

        final DateRange dateRange = DateRange.of(startDate, endDate);

        final List<Match> paginatedItems =
                matchDataService.findDashboardMatches(
                        user,
                        upcoming,
                        includeHosted != null && includeHosted,
                        query,
                        sports,
                        statuses,
                        dateRange.start(),
                        dateRange.endExclusive(),
                        minPrice,
                        maxPrice,
                        sort,
                        participantStatuses,
                        offset,
                        limit);

        final int totalCount =
                matchDataService.countDashboardMatches(
                        user,
                        upcoming,
                        includeHosted != null && includeHosted,
                        query,
                        sports,
                        statuses,
                        dateRange.start(),
                        dateRange.endExclusive(),
                        minPrice,
                        maxPrice,
                        sort,
                        participantStatuses);
        return new PaginatedResult<>(paginatedItems, totalCount, page, pageSize);
    }

    private void validatePageAndSizeOrThrow(
            final int page, final int pageSize, final int defaultPageSize) {
        if (page < 1) {
            throw new InvalidPaginationException("invalidPage");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new InvalidPaginationException("invalidPageSize");
        }
    }

    private static EventSort withoutDistance(final EventSort sort) {
        return sort == EventSort.DISTANCE ? EventSort.SOONEST : sort;
    }

    private List<OccurrenceWindow> buildOccurrenceWindows(
            final CreateMatchRequest request, final CreateRecurrenceRequest recurrence) {
        if (recurrence == null
                || recurrence.getFrequency() == null
                || recurrence.getEndMode() == null) {
            throw new MatchRecurrenceInvalidException();
        }

        final LocalDateTime firstLocalStart =
                LocalDateTime.of(request.getStartDate(), request.getStartTime());
        final Instant requestStartsAt = toInstant(request.getStartDate(), request.getStartTime());
        final Instant requestEndsAt = toInstant(request.getEndDate(), request.getEndTime());
        final Duration duration =
                requestEndsAt == null ? null : Duration.between(requestStartsAt, requestEndsAt);
        final int occurrenceCount = resolveOccurrenceCount(firstLocalStart, recurrence);

        return IntStream.range(0, occurrenceCount)
                .mapToObj(
                        index -> {
                            final LocalDateTime occurrenceLocalStart =
                                    addFrequency(firstLocalStart, recurrence.getFrequency(), index);
                            final Instant startsAt =
                                    occurrenceLocalStart.atZone(PlatformTime.ZONE).toInstant();
                            final Instant endsAt =
                                    duration == null ? null : startsAt.plus(duration);
                            return new OccurrenceWindow(startsAt, endsAt);
                        })
                .toList();
    }

    private int resolveOccurrenceCount(
            final LocalDateTime firstLocalStart, final CreateRecurrenceRequest recurrence) {
        switch (recurrence.getEndMode()) {
            case OCCURRENCE_COUNT:
                return validateOccurrenceCount(recurrence.getOccurrenceCount());
            case UNTIL_DATE:
                return countOccurrencesUntilDate(firstLocalStart, recurrence);
            default:
                throw new MatchRecurrenceInvalidException();
        }
    }

    private int validateOccurrenceCount(final Integer occurrenceCount) {
        if (occurrenceCount == null || occurrenceCount < MIN_RECURRING_OCCURRENCES) {
            throw new MatchRecurrenceTooFewOccurrencesException();
        }
        if (occurrenceCount > MAX_RECURRING_OCCURRENCES) {
            throw new MatchRecurrenceTooManyOccurrencesException();
        }
        return occurrenceCount;
    }

    private int countOccurrencesUntilDate(
            final LocalDateTime firstLocalStart, final CreateRecurrenceRequest recurrence) {
        final LocalDate untilDate = recurrence.getUntilDate();
        if (untilDate == null || !untilDate.isAfter(firstLocalStart.toLocalDate())) {
            throw new MatchRecurrenceUntilDateException();
        }

        int count = 0;
        LocalDateTime occurrenceStart = firstLocalStart;
        while (!occurrenceStart.toLocalDate().isAfter(untilDate)) {
            count++;
            if (count > MAX_RECURRING_OCCURRENCES) {
                throw new MatchRecurrenceTooManyOccurrencesException();
            }
            occurrenceStart = addFrequency(firstLocalStart, recurrence.getFrequency(), count);
        }

        if (count < MIN_RECURRING_OCCURRENCES) {
            throw new MatchRecurrenceTooFewOccurrencesException();
        }
        return count;
    }

    private static LocalDateTime addFrequency(
            final LocalDateTime firstLocalStart,
            final RecurrenceFrequency frequency,
            final int index) {
        switch (frequency) {
            case DAILY:
                return firstLocalStart.plusDays(index);
            case MONTHLY:
                return firstLocalStart.plusMonths(index);
            case WEEKLY:
            default:
                return firstLocalStart.plusWeeks(index);
        }
    }

    private void nonNullUser(final User user) {
        if (user == null) {
            throw new InvalidUserException();
        }
    }

    private void validateScheduleOrThrow(
            final Instant startsAt, final Instant endsAt, final RuntimeException exception) {
        if (startsAt != null && !startsAt.isAfter(Instant.now(clock))) {
            throw exception;
        }

        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw exception;
        }
    }

    private void validateCreateCapacityOrThrow(final int maxPlayers) {
        if (maxPlayers > MAX_PLAYERS_PER_MATCH) {
            throw new MatchCapacityAboveMaxException();
        }
    }

    private void validateUpdateCapacityOrThrow(final int maxPlayers) {
        if (maxPlayers > MAX_PLAYERS_PER_MATCH) {
            throw new MatchUpdateCapacityAboveMaxException();
        }
    }

    private void hydrateDistances(
            final List<Match> matches, final Double latitude, final Double longitude) {
        for (Match match : matches) {
            if (match.getLatitude() != null && match.getLongitude() != null) {
                match.setDistanceKmFromViewer(
                        DistanceUtils.distanceKm(
                                latitude, longitude, match.getLatitude(), match.getLongitude()));
            }
        }
    }

    private PaginatedResult<Match> paginate(
            final int page,
            final int pageSize,
            final int defaultPageSize,
            final CountSupplier countSupplier,
            final SliceSupplier sliceSupplier) {
        final int safePage = page > 0 ? page : 1;
        final int safePageSize = pageSize > 0 ? pageSize : defaultPageSize;

        final int totalCount = countSupplier.count(safePageSize);
        final int totalPages = Math.max(1, (totalCount + safePageSize - 1) / safePageSize);
        final int clampedPage = Math.min(safePage, totalPages);
        final int offset = (clampedPage - 1) * safePageSize;
        final List<Match> items = sliceSupplier.items(offset, safePageSize);

        return new PaginatedResult<>(items, totalCount, clampedPage, safePageSize);
    }

    @FunctionalInterface
    private interface CountSupplier {
        int count(int safePageSize);
    }

    @FunctionalInterface
    private interface SliceSupplier {
        List<Match> items(int offset, int safePageSize);
    }

    private record DateRange(Instant start, Instant endExclusive) {
        static DateRange of(final LocalDate start, final LocalDate end) {
            return new DateRange(
                    start == null ? null : start.atStartOfDay(PlatformTime.ZONE).toInstant(),
                    end == null
                            ? null
                            : end.plusDays(1).atStartOfDay(PlatformTime.ZONE).toInstant());
        }
    }

    private record OccurrenceWindow(Instant startsAt, Instant endsAt) {}

    private record SeriesReservationState(boolean available, boolean joined, boolean cancellable) {}

    private record SeriesReservationEvaluation(
            List<Long> targetMatchIds, boolean joined, int activeFutureReservationCount) {}

    private record SeriesJoinRequestState(boolean available, boolean pending) {}

    private record SeriesJoinRequestEvaluation(List<Long> targetMatchIds, boolean pending) {}
}
