package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSeries;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.query.EventTimeFilter;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.ParticipantStatus;
import ar.edu.itba.paw.models.types.RecurrenceEndMode;
import ar.edu.itba.paw.models.types.RecurrenceFrequency;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.services.exceptions.MatchCancellationException;
import ar.edu.itba.paw.services.exceptions.MatchUpdateException;
import ar.edu.itba.paw.services.internal.MatchDataService;
import ar.edu.itba.paw.services.internal.MatchParticipantDataService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional(readOnly = true)
public class MatchServiceImpl implements MatchService {

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
    private final MessageSource messageSource;
    private final Clock clock;

    @Autowired
    public MatchServiceImpl(
            final MatchDataService matchDataService,
            final ImageService imageService,
            final MatchParticipantDataService matchParticipantDataService,
            final MatchNotificationService matchNotificationService,
            final SecurityService securityService,
            final RecurringMatchAsyncService recurringMatchAsyncService,
            final MessageSource messageSource,
            final Clock clock) {
        this.matchDataService = matchDataService;
        this.imageService = imageService;
        this.matchParticipantDataService = matchParticipantDataService;
        this.matchNotificationService = matchNotificationService;
        this.securityService = securityService;
        this.recurringMatchAsyncService = recurringMatchAsyncService;
        this.messageSource = messageSource;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Match createMatch(final CreateMatchRequest request) {
        validateScheduleOrThrow(
                request.getStartsAt(),
                request.getEndsAt(),
                new IllegalArgumentException(message("match.schedule.error.startsAtPast")),
                new IllegalArgumentException(message("match.schedule.error.endBeforeStart")));
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
                        request.getStartsAt(),
                        request.getEndsAt(),
                        resolveZone(recurrence.getZoneId()).getId(),
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
                        request.getStartsAt(),
                        request.getEndsAt(),
                        resolveZone(recurrence.getZoneId()).getId(),
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
                request.getStartsAt(),
                request.getEndsAt(),
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
            final Long matchId,
            final User actingUser,
            final UpdateMatchRequest request,
            final ImageMetadata bannerImageMetadata,
            final EventJoinPolicy joinPolicy,
            final EventStatus status) {
        return matchDataService.updateMatch(
                matchId,
                actingUser,
                request.getAddress(),
                request.getTitle(),
                request.getDescription(),
                request.getStartsAt(),
                request.getEndsAt(),
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

    private static boolean hasCoordinates(final Double latitude, final Double longitude) {
        return latitude != null && longitude != null;
    }

    @Override
    @Transactional
    public Match updateMatch(
            final Long matchId, final User actingUser, final UpdateMatchRequest request) {
        final Match match = findEditableMatchForHost(matchId, actingUser);

        validateScheduleOrThrow(
                request.getStartsAt(),
                request.getEndsAt(),
                new MatchUpdateException(
                        MatchUpdateFailureReason.INVALID_SCHEDULE,
                        message("match.schedule.error.startsAtPast")),
                new MatchUpdateException(
                        MatchUpdateFailureReason.INVALID_SCHEDULE,
                        message("match.schedule.error.endBeforeStart")));
        validateUpdateCapacityOrThrow(request.getMaxPlayers());

        final int confirmedParticipants =
                matchParticipantDataService.findConfirmedParticipants(matchId).size();
        if (request.getMaxPlayers() < confirmedParticipants) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.CAPACITY_BELOW_CONFIRMED,
                    message("match.update.error.capacityBelowConfirmed"));
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
                        matchId,
                        actingUser,
                        request,
                        bannerImageMetadata,
                        joinPolicy,
                        request.getStatus());

        if (!updated) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.FORBIDDEN, message("match.update.error.forbidden"));
        }

        final Match updatedMatch =
                matchDataService
                        .findById(matchId)
                        .orElseThrow(
                                () ->
                                        new MatchUpdateException(
                                                MatchUpdateFailureReason.MATCH_NOT_FOUND,
                                                message("match.update.error.notFound")));
        applyParticipationPolicyTransition(updatedMatch, participationPolicyTransition);
        matchNotificationService.notifyMatchUpdated(updatedMatch);
        return updatedMatch;
    }

    @Override
    public Match findEditableMatchForHost(final Long matchId, final User actingUser) {
        final Match match =
                matchDataService
                        .findById(matchId)
                        .orElseThrow(
                                () ->
                                        new MatchUpdateException(
                                                MatchUpdateFailureReason.MATCH_NOT_FOUND,
                                                message("match.update.error.notFound")));
        validateMatchUpdateAccess(match, actingUser);
        return match;
    }

    @Override
    public Match findEditableRecurringMatchForHost(final Long matchId, final User actingUser) {
        final Match match = findEditableMatchForHost(matchId, actingUser);
        if (!match.isRecurringOccurrence()) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.NOT_RECURRING,
                    message("match.update.error.notRecurring"));
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
                throw new MatchUpdateException(
                        MatchUpdateFailureReason.PENDING_REQUESTS_EXCEED_AVAILABLE,
                        message("match.update.error.pendingRequestsExceedAvailable"));
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
                matchDataService
                        .findById(matchId)
                        .orElseThrow(
                                () ->
                                        new MatchUpdateException(
                                                MatchUpdateFailureReason.MATCH_NOT_FOUND,
                                                message("match.update.error.notFound")));

        validateSeriesUpdateAccess(pivot, actingUser);
        validateScheduleOrThrow(
                request.getStartsAt(),
                request.getEndsAt(),
                new MatchUpdateException(
                        MatchUpdateFailureReason.INVALID_SCHEDULE,
                        message("match.schedule.error.startsAtPast")),
                new MatchUpdateException(
                        MatchUpdateFailureReason.INVALID_SCHEDULE,
                        message("match.schedule.error.endBeforeStart")));
        validateUpdateCapacityOrThrow(request.getMaxPlayers());

        final List<Match> targets = editableFutureSeriesTargets(pivot);
        if (targets.isEmpty()) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.NOT_EDITABLE,
                    message("match.update.error.notEditable"));
        }

        for (final Match target : targets) {
            final int confirmedParticipants =
                    matchParticipantDataService.findConfirmedParticipants(target.getId()).size();
            if (request.getMaxPlayers() < confirmedParticipants) {
                throw new MatchUpdateException(
                        MatchUpdateFailureReason.CAPACITY_BELOW_CONFIRMED,
                        message("match.update.error.capacityBelowConfirmed"));
            }
        }

        final Duration startOffset = Duration.between(pivot.getStartsAt(), request.getStartsAt());
        final Duration requestedDuration =
                request.getEndsAt() == null
                        ? null
                        : Duration.between(request.getStartsAt(), request.getEndsAt());
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
                    targetStartsAt,
                    targetEndsAt,
                    new MatchUpdateException(
                            MatchUpdateFailureReason.INVALID_SCHEDULE,
                            message("match.schedule.error.startsAtPast")),
                    new MatchUpdateException(
                            MatchUpdateFailureReason.INVALID_SCHEDULE,
                            message("match.schedule.error.endBeforeStart")));
            final UpdateMatchRequest targetRequest =
                    new UpdateMatchRequest(
                            request.getAddress(),
                            request.getTitle(),
                            request.getDescription(),
                            targetStartsAt,
                            targetEndsAt,
                            request.getMaxPlayers(),
                            request.getPricePerPlayer(),
                            request.getSport(),
                            request.getVisibility(),
                            request.getJoinPolicy(),
                            target.getStatus(),
                            request.getBannerImage(),
                            request.getLatitude(),
                            request.getLongitude());
            final boolean updated =
                    updateStoredMatch(
                            target.getId(),
                            actingUser,
                            targetRequest,
                            bannerImageMetadata,
                            resolveJoinPolicy(
                                    targetRequest.getVisibility(), targetRequest.getJoinPolicy()),
                            target.getStatus());
            if (!updated) {
                throw new MatchUpdateException(
                        MatchUpdateFailureReason.FORBIDDEN,
                        message("match.update.error.forbidden"));
            }

            final Match updatedMatch =
                    matchDataService
                            .findById(target.getId())
                            .orElseThrow(
                                    () ->
                                            new MatchUpdateException(
                                                    MatchUpdateFailureReason.MATCH_NOT_FOUND,
                                                    message("match.update.error.notFound")));
            updatedMatches.add(updatedMatch);
        }

        matchNotificationService.notifyRecurringMatchesUpdated(updatedMatches);
        return List.copyOf(updatedMatches);
    }

    @Override
    @Transactional
    public Match cancelMatch(final Long matchId, final User actingUser) {
        final Match match =
                matchDataService
                        .findById(matchId)
                        .orElseThrow(
                                () ->
                                        new MatchCancellationException(
                                                MatchCancellationFailureReason.MATCH_NOT_FOUND,
                                                message("match.cancel.error.notFound")));

        final User currentUser = securityService.currentUser();
        if (!match.getHost().getId().equals(actingUser.getId())
                && (currentUser == null || !currentUser.getId().equals(actingUser.getId()))) {
            throw new MatchCancellationException(
                    MatchCancellationFailureReason.FORBIDDEN,
                    message("match.cancel.error.forbidden"));
        }

        if (EventStatus.COMPLETED.equals(match.getStatus())) {
            throw new MatchCancellationException(
                    MatchCancellationFailureReason.FORBIDDEN,
                    message("match.cancel.error.forbidden"));
        }

        if (EventStatus.CANCELLED.equals(match.getStatus())) {
            return match;
        }

        final boolean updated = matchDataService.cancelMatch(matchId, actingUser);
        if (!updated) {
            throw new MatchCancellationException(
                    MatchCancellationFailureReason.FORBIDDEN,
                    message("match.cancel.error.forbidden"));
        }

        final Match cancelledMatch =
                matchDataService
                        .findById(matchId)
                        .orElseThrow(
                                () ->
                                        new MatchCancellationException(
                                                MatchCancellationFailureReason.MATCH_NOT_FOUND,
                                                message("match.cancel.error.notFound")));
        matchNotificationService.notifyMatchCancelled(cancelledMatch);
        return cancelledMatch;
    }

    @Override
    @Transactional
    public List<Match> cancelSeriesFromOccurrence(final Long matchId, final User actingUser) {
        final Match pivot =
                matchDataService
                        .findById(matchId)
                        .orElseThrow(
                                () ->
                                        new MatchCancellationException(
                                                MatchCancellationFailureReason.MATCH_NOT_FOUND,
                                                message("match.cancel.error.notFound")));

        validateSeriesCancellationAccess(pivot, actingUser);

        final List<Match> targets = cancellableFutureSeriesTargets(pivot);
        if (targets.isEmpty()) {
            throw new MatchCancellationException(
                    MatchCancellationFailureReason.FORBIDDEN,
                    message("match.cancel.error.forbidden"));
        }

        final List<Match> cancelledMatches = new ArrayList<>();
        for (final Match target : targets) {
            final boolean updated = matchDataService.cancelMatch(target.getId(), actingUser);
            if (!updated) {
                throw new MatchCancellationException(
                        MatchCancellationFailureReason.FORBIDDEN,
                        message("match.cancel.error.forbidden"));
            }

            final Match cancelledMatch =
                    matchDataService
                            .findById(target.getId())
                            .orElseThrow(
                                    () ->
                                            new MatchCancellationException(
                                                    MatchCancellationFailureReason.MATCH_NOT_FOUND,
                                                    message("match.cancel.error.notFound")));
            cancelledMatches.add(cancelledMatch);
        }

        matchNotificationService.notifyRecurringMatchesCancelled(cancelledMatches);
        return List.copyOf(cancelledMatches);
    }

    private void validateSeriesUpdateAccess(final Match pivot, final User actingUser) {
        validateMatchHostAccess(pivot, actingUser);
        if (!pivot.isRecurringOccurrence()) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.NOT_RECURRING,
                    message("match.update.error.notRecurring"));
        }
        validateEditableMatch(pivot);
    }

    private void validateMatchUpdateAccess(final Match match, final User actingUser) {
        validateMatchHostAccess(match, actingUser);
        validateEditableMatch(match);
    }

    private void validateEditableMatch(final Match match) {
        if (!isEditableMatch(match)) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.NOT_EDITABLE,
                    message("match.update.error.notEditable"));
        }
    }

    private void validateMatchHostAccess(final Match match, final User actingUser) {
        if (!match.getHost().getId().equals(actingUser.getId())) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.FORBIDDEN, message("match.update.error.forbidden"));
        }
    }

    private void validateSeriesCancellationAccess(final Match pivot, final User actingUser) {
        if (!pivot.getHost().getId().equals(actingUser.getId())) {
            throw new MatchCancellationException(
                    MatchCancellationFailureReason.FORBIDDEN,
                    message("match.cancel.error.forbidden"));
        }

        if (!pivot.isRecurringOccurrence()) {
            throw new MatchCancellationException(
                    MatchCancellationFailureReason.NOT_RECURRING,
                    message("match.cancel.error.notRecurring"));
        }
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
                .filter(MatchServiceImpl::isEditableMatch)
                .toList();
    }

    private List<Match> cancellableFutureSeriesTargets(final Match pivot) {
        if (!pivot.isRecurringOccurrence()) {
            return List.of();
        }

        final Instant now = Instant.now(clock);
        return matchDataService.findSeriesOccurrences(pivot.getSeries().getId()).stream()
                .filter(occurrence -> occurrence.getStartsAt().isAfter(now))
                .filter(MatchServiceImpl::isEditableMatch)
                .toList();
    }

    private static boolean isEditableMatch(final Match match) {
        return match.getStatus() != EventStatus.COMPLETED
                && match.getStatus() != EventStatus.CANCELLED;
    }

    @Override
    public Optional<Match> findMatchById(final Long matchId) {
        return matchDataService.findById(matchId);
    }

    @Override
    public Optional<Match> findPublicMatchById(final Long matchId) {
        return matchDataService.findPublicMatchById(matchId);
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
            final Instant startDate,
            final Instant endDate,
            final EventSort sort,
            final int page,
            final int pageSize,
            final ZoneId timezone,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final Double latitude,
            final Double longitude) {
        final EventSort sortFilter =
                hasCoordinates(latitude, longitude) ? sort : withoutDistance(sort);
        final DateRange dateRange = new DateRange(startDate, endDate);

        return paginate(
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
                                maxPrice,
                                timezone),
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
                                timezone,
                                latitude,
                                longitude,
                                offset,
                                safePageSize));
    }

    @Override
    public PaginatedResult<Match> findDashboardMatches(
            User user,
            Boolean upcoming,
            Boolean includeHosted,
            String query,
            List<Sport> sports,
            List<EventStatus> statuses,
            Instant startDate,
            Instant endDate,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            EventSort sort,
            ZoneId timezone,
            List<ParticipantStatus> participantStatuses,
            int page,
            int pageSize) {
        nonNullUser(user);
        validatePageAndSizeOrThrow(page, pageSize, DEFAULT_PAGE_SIZE);

        final int offset = (page - 1) * pageSize;
        final int limit = pageSize;

        final List<Match> paginatedItems =
                matchDataService.findDashboardMatches(
                        user,
                        upcoming,
                        includeHosted != null && includeHosted,
                        query,
                        sports,
                        statuses,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        sort,
                        timezone,
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
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        sort,
                        timezone,
                        participantStatuses);
        return new PaginatedResult<>(paginatedItems, totalCount, page, pageSize);
    }

    private void validatePageAndSizeOrThrow(
            final int page, final int pageSize, final int defaultPageSize) {
        if (page < 1) {
            throw new IllegalArgumentException(message("pagination.error.invalidPage"));
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(message("pagination.error.invalidPageSize"));
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
            throw new IllegalArgumentException(message("match.recurrence.error.invalid"));
        }

        final ZoneId zoneId = resolveZone(recurrence.getZoneId());
        final LocalDateTime firstLocalStart =
                LocalDateTime.ofInstant(request.getStartsAt(), zoneId);
        final Duration duration =
                request.getEndsAt() == null
                        ? null
                        : Duration.between(request.getStartsAt(), request.getEndsAt());
        final int occurrenceCount = resolveOccurrenceCount(firstLocalStart, recurrence);

        return IntStream.range(0, occurrenceCount)
                .mapToObj(
                        index -> {
                            final LocalDateTime occurrenceLocalStart =
                                    addFrequency(firstLocalStart, recurrence.getFrequency(), index);
                            final Instant startsAt =
                                    occurrenceLocalStart.atZone(zoneId).toInstant();
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
                throw new IllegalArgumentException(message("match.recurrence.error.invalid"));
        }
    }

    private int validateOccurrenceCount(final Integer occurrenceCount) {
        if (occurrenceCount == null || occurrenceCount < MIN_RECURRING_OCCURRENCES) {
            throw new IllegalArgumentException(message("match.recurrence.error.tooFewOccurrences"));
        }
        if (occurrenceCount > MAX_RECURRING_OCCURRENCES) {
            throw new IllegalArgumentException(
                    message("match.recurrence.error.tooManyOccurrences"));
        }
        return occurrenceCount;
    }

    private int countOccurrencesUntilDate(
            final LocalDateTime firstLocalStart, final CreateRecurrenceRequest recurrence) {
        final LocalDate untilDate = recurrence.getUntilDate();
        if (untilDate == null || !untilDate.isAfter(firstLocalStart.toLocalDate())) {
            throw new IllegalArgumentException(message("match.recurrence.error.untilDate"));
        }

        int count = 0;
        LocalDateTime occurrenceStart = firstLocalStart;
        while (!occurrenceStart.toLocalDate().isAfter(untilDate)) {
            count++;
            if (count > MAX_RECURRING_OCCURRENCES) {
                throw new IllegalArgumentException(
                        message("match.recurrence.error.tooManyOccurrences"));
            }
            occurrenceStart = addFrequency(firstLocalStart, recurrence.getFrequency(), count);
        }

        if (count < MIN_RECURRING_OCCURRENCES) {
            throw new IllegalArgumentException(message("match.recurrence.error.tooFewOccurrences"));
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

    private static ZoneId resolveZone(final ZoneId zoneId) {
        return zoneId == null ? ZoneId.systemDefault() : zoneId;
    }

    private String message(final String code) {
        final Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(
                Objects.requireNonNull(code), null, code, Objects.requireNonNull(locale));
    }

    private void nonNullUser(final User user) {
        if (user == null) {
            throw new IllegalArgumentException(message("user.error.null"));
        }
    }

    private void validateScheduleOrThrow(
            final Instant startsAt,
            final Instant endsAt,
            final RuntimeException startsAtException,
            final RuntimeException endsAtException) {
        if (startsAt != null && !startsAt.isAfter(Instant.now(clock))) {
            throw startsAtException;
        }

        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw endsAtException;
        }
    }

    private void validateCreateCapacityOrThrow(final int maxPlayers) {
        if (maxPlayers > MAX_PLAYERS_PER_MATCH) {
            throw new IllegalArgumentException(message("match.create.error.capacityAboveMax"));
        }
    }

    private void validateUpdateCapacityOrThrow(final int maxPlayers) {
        if (maxPlayers > MAX_PLAYERS_PER_MATCH) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.CAPACITY_ABOVE_MAX,
                    message("match.update.error.capacityAboveMax"));
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

    private record DateRange(Instant start, Instant endExclusive) {}

    private record OccurrenceWindow(Instant startsAt, Instant endsAt) {}
}
