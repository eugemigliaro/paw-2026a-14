package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSeries;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventTimeFilter;
import ar.edu.itba.paw.models.query.MatchSort;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.RecurrenceEndMode;
import ar.edu.itba.paw.models.types.RecurrenceFrequency;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.MatchCancellationException;
import ar.edu.itba.paw.services.exceptions.MatchUpdateException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MatchServiceImpl implements MatchService {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PLAYERS_PER_MATCH = 1000;
    private static final int MIN_RECURRING_OCCURRENCES = 2;
    private static final int MAX_RECURRING_OCCURRENCES = 52;

    private final MatchDao matchDao;
    private final MatchParticipantDao matchParticipantDao;
    private final SecurityService securityService;
    private final MatchNotificationService matchNotificationService;
    private final MessageSource messageSource;
    private final Clock clock;

    @Autowired
    public MatchServiceImpl(
            final MatchDao matchDao,
            final MatchParticipantDao matchParticipantDao,
            final MatchNotificationService matchNotificationService,
            final SecurityService securityService,
            final MessageSource messageSource,
            final Clock clock) {
        this.matchDao = matchDao;
        this.matchParticipantDao = matchParticipantDao;
        this.matchNotificationService = matchNotificationService;
        this.securityService = securityService;
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
                matchDao.createMatchSeries(
                        request.getHost(),
                        recurrence.getFrequency().getValue(),
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
                        recurrence.getFrequency().getValue(),
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

        Match firstOccurrence = null;
        for (int i = 0; i < occurrences.size(); i++) {
            final OccurrenceWindow occurrence = occurrences.get(i);
            final Match created = createSeriesOccurrence(request, occurrence, series, i + 1);
            if (firstOccurrence == null) {
                firstOccurrence = created;
            }
        }

        return firstOccurrence;
    }

    private Match createSingleMatch(final CreateMatchRequest request) {
        return matchDao.createMatch(
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
                request.getBannerImageMetadata(),
                request.getLatitude(),
                request.getLongitude());
    }

    private Match createSeriesOccurrence(
            final CreateMatchRequest request,
            final OccurrenceWindow occurrence,
            final MatchSeries series,
            final int seriesOccurrenceIndex) {
        return matchDao.createMatch(
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
                request.getBannerImageMetadata(),
                request.getLatitude(),
                request.getLongitude(),
                series,
                seriesOccurrenceIndex);
    }

    private boolean updateStoredMatch(
            final Long matchId,
            final User actingUser,
            final UpdateMatchRequest request,
            final EventJoinPolicy joinPolicy,
            final EventStatus status) {
        return matchDao.updateMatch(
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
                request.getBannerImageMetadata(),
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
        final Match match =
                matchDao.findById(matchId)
                        .orElseThrow(
                                () ->
                                        new MatchUpdateException(
                                                MatchUpdateFailureReason.MATCH_NOT_FOUND,
                                                message("match.update.error.notFound")));

        if (!match.getHost().getId().equals(actingUser.getId())) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.FORBIDDEN, message("match.update.error.forbidden"));
        }

        if (EventStatus.CANCELLED.equals(match.getStatus())
                || EventStatus.COMPLETED.equals(match.getStatus())) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.NOT_EDITABLE,
                    message("match.update.error.notEditable"));
        }

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
                matchParticipantDao.findConfirmedParticipants(matchId).size();
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

        final boolean updated =
                updateStoredMatch(matchId, actingUser, request, joinPolicy, request.getStatus());

        if (!updated) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.FORBIDDEN, message("match.update.error.forbidden"));
        }

        final Match updatedMatch =
                matchDao.findById(matchId)
                        .orElseThrow(
                                () ->
                                        new MatchUpdateException(
                                                MatchUpdateFailureReason.MATCH_NOT_FOUND,
                                                message("match.update.error.notFound")));
        applyParticipationPolicyTransition(updatedMatch, participationPolicyTransition);
        matchNotificationService.notifyMatchUpdated(updatedMatch);
        return updatedMatch;
    }

    private ParticipationPolicyTransition validateAndPlanParticipationPolicyTransition(
            final Match match, final UpdateMatchRequest request, final int confirmedParticipants) {
        if (wasPrivate(match) && isPublic(request)) {
            return ParticipationPolicyTransition.cancelInvitations(
                    matchParticipantDao.findInvitedUsers(match.getId()));
        }
        if (wasApprovalRequired(match) && isPrivate(request)) {
            return ParticipationPolicyTransition.cancelPendingRequests(
                    matchParticipantDao.findPendingRequests(match.getId()));
        }
        if (wasApprovalRequired(match) && isDirectPublic(request)) {
            final int pendingRequests = matchParticipantDao.countPendingRequests(match.getId());
            final int availableSpots = request.getMaxPlayers() - confirmedParticipants;
            if (pendingRequests > availableSpots) {
                throw new MatchUpdateException(
                        MatchUpdateFailureReason.PENDING_REQUESTS_EXCEED_AVAILABLE,
                        message("match.update.error.pendingRequestsExceedAvailable"));
            }
            return ParticipationPolicyTransition.approvePendingRequests(
                    matchParticipantDao.findPendingRequests(match.getId()));
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
            matchParticipantDao.cancelPendingInvitations(updatedMatch.getId());
            return;
        }
        if (transition.type() == ParticipationPolicyTransitionType.CANCEL_PENDING_REQUESTS) {
            matchNotificationService.notifyPendingRequestClosedByPrivacyChange(
                    updatedMatch, transition.affectedUsers());
            matchParticipantDao.cancelPendingRequests(updatedMatch.getId());
            return;
        }
        matchParticipantDao.approveAllPendingRequests(updatedMatch.getId());
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
                matchDao.findById(matchId)
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
                    matchParticipantDao.findConfirmedParticipants(target.getId()).size();
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
                            request.getBannerImageMetadata(),
                            request.getLatitude(),
                            request.getLongitude());
            final boolean updated =
                    updateStoredMatch(
                            target.getId(),
                            actingUser,
                            targetRequest,
                            resolveJoinPolicy(
                                    targetRequest.getVisibility(), targetRequest.getJoinPolicy()),
                            target.getStatus());
            if (!updated) {
                throw new MatchUpdateException(
                        MatchUpdateFailureReason.FORBIDDEN,
                        message("match.update.error.forbidden"));
            }

            final Match updatedMatch =
                    matchDao.findById(target.getId())
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
                matchDao.findById(matchId)
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

        final boolean updated = matchDao.cancelMatch(matchId, actingUser);
        if (!updated) {
            throw new MatchCancellationException(
                    MatchCancellationFailureReason.FORBIDDEN,
                    message("match.cancel.error.forbidden"));
        }

        final Match cancelledMatch =
                matchDao.findById(matchId)
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
                matchDao.findById(matchId)
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
            final boolean updated = matchDao.cancelMatch(target.getId(), actingUser);
            if (!updated) {
                throw new MatchCancellationException(
                        MatchCancellationFailureReason.FORBIDDEN,
                        message("match.cancel.error.forbidden"));
            }

            final Match cancelledMatch =
                    matchDao.findById(target.getId())
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
        if (!pivot.getHost().getId().equals(actingUser.getId())) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.FORBIDDEN, message("match.update.error.forbidden"));
        }

        if (!pivot.isRecurringOccurrence()) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.NOT_RECURRING,
                    message("match.update.error.notRecurring"));
        }

        if (!isEditableMatch(pivot)) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.NOT_EDITABLE,
                    message("match.update.error.notEditable"));
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
        return matchDao.findSeriesOccurrences(pivot.getSeries().getId()).stream()
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
        return matchDao.findSeriesOccurrences(pivot.getSeries().getId()).stream()
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
        return matchDao.findById(matchId);
    }

    @Override
    public Optional<Match> findPublicMatchById(final Long matchId) {
        return matchDao.findPublicMatchById(matchId);
    }

    @Override
    public List<Match> findSeriesOccurrences(final Long seriesId) {
        if (seriesId == null) {
            return List.of();
        }
        return matchDao.findSeriesOccurrences(seriesId);
    }

    @Override
    public List<User> findConfirmedParticipants(final Long matchId) {
        return matchParticipantDao.findConfirmedParticipants(matchId);
    }

    @Override
    public PaginatedResult<Match> findHostedMatches(
            final User host,
            final Boolean upcoming,
            final String query,
            final String sport,
            final String visibility,
            final String status,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String sort,
            final String timezone,
            final int page,
            final int pageSize) {
        final List<Sport> sportFilters = parseSports(sport);
        final List<EventVisibility> visibilityFilters = parseVisibility(visibility);
        final List<EventStatus> statusFilters = parseStatuses(status);
        final ZoneId zoneId = parseZone(timezone);
        final DateRange dateRange = parseDateRange(startDate, endDate, zoneId);

        return paginate(
                page,
                pageSize,
                DEFAULT_PAGE_SIZE,
                safePageSize ->
                        matchDao.countHostedMatches(
                                host,
                                upcoming,
                                query,
                                sportFilters,
                                visibilityFilters,
                                statusFilters,
                                EventTimeFilter.ALL,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice,
                                zoneId),
                (offset, safePageSize) ->
                        matchDao.findHostedMatches(
                                host,
                                upcoming,
                                query,
                                sportFilters,
                                visibilityFilters,
                                statusFilters,
                                EventTimeFilter.ALL,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice,
                                parseSort(sort),
                                zoneId,
                                offset,
                                safePageSize));
    }

    @Override
    public PaginatedResult<Match> findJoinedMatches(
            final User user,
            final Boolean upcoming,
            final String query,
            final String sport,
            final String visibility,
            final String status,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String sort,
            final String timezone,
            final int page,
            final int pageSize) {
        final List<Sport> sportFilters = parseSports(sport);
        final List<EventVisibility> visibilityFilters = parseVisibility(visibility);
        final List<EventStatus> statusFilters = parseStatuses(status);
        final ZoneId zoneId = parseZone(timezone);
        final DateRange dateRange = parseDateRange(startDate, endDate, zoneId);
        nonNullUser(user);

        return paginate(
                page,
                pageSize,
                DEFAULT_PAGE_SIZE,
                safePageSize ->
                        matchDao.countJoinedMatches(
                                user,
                                upcoming,
                                query,
                                sportFilters,
                                visibilityFilters,
                                statusFilters,
                                EventTimeFilter.ALL,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice,
                                zoneId),
                (offset, safePageSize) ->
                        matchDao.findJoinedMatches(
                                user,
                                upcoming,
                                query,
                                sportFilters,
                                visibilityFilters,
                                statusFilters,
                                EventTimeFilter.ALL,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice,
                                parseSort(sort),
                                zoneId,
                                offset,
                                safePageSize));
    }

    @Override
    public PaginatedResult<Match> searchPublicMatches(
            final String query,
            final String sport,
            final String startDate,
            final String endDate,
            final String sort,
            final int page,
            final int pageSize,
            final String timezone,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        return searchPublicMatches(
                query, sport, startDate, endDate, sort, page, pageSize, timezone, minPrice,
                maxPrice, null, null);
    }

    @Override
    public PaginatedResult<Match> searchPublicMatches(
            final String query,
            final String sport,
            final String startDate,
            final String endDate,
            final String sort,
            final int page,
            final int pageSize,
            final String timezone,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final Double latitude,
            final Double longitude) {
        final List<Sport> sportFilters = parseSports(sport);
        final MatchSort sortFilter =
                hasCoordinates(latitude, longitude)
                        ? parseSort(sort)
                        : withoutDistance(parseSort(sort));
        final ZoneId zoneId = parseZone(timezone);
        final DateRange dateRange = parseDateRange(startDate, endDate, zoneId);

        return paginate(
                page,
                pageSize,
                DEFAULT_PAGE_SIZE,
                safePageSize ->
                        matchDao.countPublicMatches(
                                query,
                                sportFilters,
                                EventTimeFilter.ALL,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice,
                                zoneId),
                (offset, safePageSize) ->
                        matchDao.findPublicMatches(
                                query,
                                sportFilters,
                                EventTimeFilter.ALL,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice,
                                sortFilter,
                                zoneId,
                                latitude,
                                longitude,
                                offset,
                                safePageSize));
    }

    private static MatchSort withoutDistance(final MatchSort sort) {
        return sort == MatchSort.DISTANCE ? MatchSort.SOONEST : sort;
    }

    private static List<Sport> parseSports(final String rawSports) {
        if (rawSports == null || rawSports.isBlank()) {
            return List.of();
        }

        final LinkedHashSet<Sport> sports = new LinkedHashSet<>();
        for (final String rawSport : rawSports.split(",")) {
            if (rawSport == null || rawSport.isBlank()) {
                continue;
            }
            PersistableEnum.fromDbValue(Sport.class, rawSport.trim()).ifPresent(sports::add);
        }

        return List.copyOf(sports);
    }

    private static List<EventStatus> parseStatuses(final String rawStatuses) {
        if (rawStatuses == null || rawStatuses.isBlank()) {
            return List.of();
        }

        final LinkedHashSet<EventStatus> statuses = new LinkedHashSet<>();
        for (final String rawStatus : rawStatuses.split(",")) {
            if (rawStatus == null || rawStatus.isBlank()) {
                continue;
            }
            PersistableEnum.fromDbValue(EventStatus.class, rawStatus.trim())
                    .ifPresent(statuses::add);
        }

        return List.copyOf(statuses);
    }

    private static List<EventVisibility> parseVisibility(final String rawVisibility) {
        if (rawVisibility == null || rawVisibility.isBlank()) {
            return List.of();
        }

        final LinkedHashSet<EventVisibility> visibility = new LinkedHashSet<>();
        for (final String rawValue : rawVisibility.split(",")) {
            if (rawValue == null || rawValue.isBlank()) {
                continue;
            }
            PersistableEnum.fromDbValue(EventVisibility.class, rawValue.trim())
                    .ifPresent(visibility::add);
        }

        return List.copyOf(visibility);
    }

    private static DateRange parseDateRange(
            final String rawStartDate, final String rawEndDate, final ZoneId zoneId) {
        final LocalDate startDate = parseDate(rawStartDate);
        final LocalDate endDate = parseDate(rawEndDate);

        if (startDate == null && endDate == null) {
            return new DateRange(null, null);
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            final Instant start = endDate.atStartOfDay(zoneId).toInstant();
            final Instant endExclusive = startDate.plusDays(1).atStartOfDay(zoneId).toInstant();
            return new DateRange(start, endExclusive);
        }

        final Instant start = startDate == null ? null : startDate.atStartOfDay(zoneId).toInstant();
        final Instant endExclusive =
                endDate == null ? null : endDate.plusDays(1).atStartOfDay(zoneId).toInstant();
        return new DateRange(start, endExclusive);
    }

    private static LocalDate parseDate(final String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(rawDate.trim());
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static MatchSort parseSort(final String rawSort) {
        if (rawSort == null || rawSort.isBlank()) {
            return MatchSort.SOONEST;
        }
        return MatchSort.fromQueryValue(rawSort).orElse(MatchSort.SOONEST);
    }

    private static ZoneId parseZone(final String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.systemDefault();
        }

        try {
            return ZoneId.of(timezone);
        } catch (final Exception ignored) {
            return ZoneId.systemDefault();
        }
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

        return java.util.stream.IntStream.range(0, occurrenceCount)
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
