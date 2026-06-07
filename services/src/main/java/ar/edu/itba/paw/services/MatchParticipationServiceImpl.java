package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.match.MatchClosedException;
import ar.edu.itba.paw.models.exceptions.match.MatchForbiddenActionException;
import ar.edu.itba.paw.models.exceptions.match.MatchFullException;
import ar.edu.itba.paw.models.exceptions.match.MatchNotFoundException;
import ar.edu.itba.paw.models.exceptions.match.MatchNotRecurringException;
import ar.edu.itba.paw.models.exceptions.match.MatchSeriesClosedException;
import ar.edu.itba.paw.models.exceptions.match.MatchSeriesFullException;
import ar.edu.itba.paw.models.exceptions.match.MatchSeriesStartedException;
import ar.edu.itba.paw.models.exceptions.match.MatchStartedException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationAlreadyInvitedException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationAlreadyJoinedException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationAlreadyPendingException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationIsHostException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationNoInvitationException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationNoPendingRequestException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationNotCancellableException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationNotInviteOnlyException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationNotJoinedException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationNotParticipantException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationSeriesAlreadyCoveredException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationSeriesAlreadyInvitedException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationSeriesAlreadyJoinedException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationSeriesAlreadyPendingException;
import ar.edu.itba.paw.models.exceptions.user.UserNotFoundException;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MatchParticipationServiceImpl implements MatchParticipationService {

    private final MatchDao matchDao;
    private final MatchParticipantDao matchParticipantDao;
    private final UserService userService;
    private final Clock clock;
    private final MailDispatchService mailDispatchService;
    private final MatchNotificationService matchNotificationService;

    public MatchParticipationServiceImpl(
            final MatchDao matchDao,
            final MatchParticipantDao matchParticipantDao,
            final UserService userService,
            final Clock clock) {
        this(matchDao, matchParticipantDao, userService, clock, new MailDispatchService() {}, null);
    }

    @Autowired
    public MatchParticipationServiceImpl(
            final MatchDao matchDao,
            final MatchParticipantDao matchParticipantDao,
            final UserService userService,
            final Clock clock,
            final MailDispatchService mailDispatchService,
            final MatchNotificationService matchNotificationService) {
        this.matchDao = matchDao;
        this.matchParticipantDao = matchParticipantDao;
        this.userService = userService;
        this.clock = clock;
        this.mailDispatchService = mailDispatchService;
        this.matchNotificationService = matchNotificationService;
    }

    // -------------------------------------------------------------------------
    // Player actions
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void requestToJoin(final Long matchId, final User user) {
        final Match match = requireMatch(matchId);
        if (user == null) {
            throw new IllegalArgumentException("exception.user.notNull");
        }

        if (isHost(match, user.getId())) {
            throw new MatchParticipationIsHostException();
        }

        if (!EventStatus.OPEN.equals(match.getStatus())) {
            throw new MatchClosedException();
        }

        if (match.getVisibility() != EventVisibility.PUBLIC
                || match.getJoinPolicy() != EventJoinPolicy.APPROVAL_REQUIRED) {
            throw new MatchParticipationNotInviteOnlyException();
        }

        if (!match.getStartsAt().isAfter(Instant.now(clock))) {
            throw new MatchStartedException();
        }

        if (matchParticipantDao.hasActiveReservation(matchId, user)) {
            throw new MatchParticipationAlreadyJoinedException();
        }

        if (matchParticipantDao.hasPendingRequest(matchId, user)) {
            throw new MatchParticipationAlreadyPendingException();
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchFullException();
        }

        if (!matchParticipantDao.createJoinRequest(matchId, user)) {
            throw new MatchParticipationAlreadyPendingException();
        }

        matchNotificationService.notifyHostJoinRequestReceived(match, user);
    }

    @Override
    @Transactional
    public void requestToJoinSeries(final Long matchId, final User user) {
        final Match match = requireMatch(matchId);
        if (user == null) {
            throw new IllegalArgumentException("exception.user.notNull");
        }

        if (isHost(match, user.getId())) {
            throw new MatchParticipationIsHostException();
        }

        if (!match.isRecurringOccurrence()) {
            throw new MatchNotRecurringException();
        }

        final List<Match> occurrences = matchDao.findSeriesOccurrences(match.getSeries().getId());
        final Instant now = Instant.now(clock);
        final Set<Long> activeFutureReservationMatchIds =
                Set.copyOf(
                        matchParticipantDao.findActiveFutureReservationMatchIdsForSeries(
                                match.getSeries().getId(), user, now));
        final Set<Long> pendingFutureRequestMatchIds =
                Set.copyOf(
                        matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                                match.getSeries().getId(), user, now));
        final SeriesJoinRequestEvaluation evaluation =
                evaluateSeriesJoinRequestTargets(
                        occurrences,
                        activeFutureReservationMatchIds,
                        pendingFutureRequestMatchIds,
                        now);
        if (matchParticipantDao.hasPendingSeriesRequest(match.getSeries().getId(), user)) {
            seriesJoinRequestFailure(evaluation.asPending());
        }
        if (evaluation.requestableOccurrenceCount() == 0) {
            seriesJoinRequestFailure(evaluation);
        }

        final Long requestMatchId =
                pendingFutureRequestMatchIds.contains(matchId)
                        ? matchId
                        : evaluation.targetMatchIds().get(0);
        if (!matchParticipantDao.createSeriesJoinRequestIfSpace(requestMatchId, user)) {
            final Instant currentNow = Instant.now(clock);
            final SeriesJoinRequestEvaluation currentEvaluation =
                    evaluateSeriesJoinRequestTargets(
                            matchDao.findSeriesOccurrences(match.getSeries().getId()),
                            Set.copyOf(
                                    matchParticipantDao
                                            .findActiveFutureReservationMatchIdsForSeries(
                                                    match.getSeries().getId(), user, currentNow)),
                            Set.copyOf(
                                    matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                                            match.getSeries().getId(), user, currentNow)),
                            currentNow);
            seriesJoinRequestFailure(currentEvaluation);
        }

        matchNotificationService.notifyHostJoinRequestReceived(match, user);
    }

    @Override
    @Transactional
    public void cancelJoinRequest(final Long matchId, final User user) {
        if (!matchParticipantDao.hasPendingRequest(matchId, user)) {
            throw new MatchParticipationNoPendingRequestException();
        }

        if (!matchParticipantDao.cancelJoinRequest(matchId, user)) {
            throw new MatchParticipationNoPendingRequestException();
        }
    }

    @Override
    public boolean hasPendingRequest(final Long matchId, final User user) {
        return matchParticipantDao.hasPendingRequest(matchId, user);
    }

    @Override
    public boolean hasPendingSeriesRequest(final Long matchId, final User user) {
        final Match match = requireMatch(matchId);
        return match.isRecurringOccurrence()
                && matchParticipantDao.hasPendingSeriesRequest(match.getSeries().getId(), user);
    }

    @Override
    public Set<Long> findPendingFutureRequestMatchIdsForSeries(
            final Long seriesId, final User user) {
        if (seriesId == null || user == null) {
            return Set.of();
        }
        return Set.copyOf(
                matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                        seriesId, user, Instant.now(clock)));
    }

    // -------------------------------------------------------------------------
    // Host actions
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void approveRequest(final Long matchId, final User host, final User targetUser) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        nonNullUser(targetUser);
        requireHost(match, host.getId());
        requireApprovalBasedMatch(match);

        if (match.isRecurringOccurrence()
                && matchParticipantDao.isSeriesJoinRequest(matchId, targetUser)) {
            final int approvedRows =
                    matchParticipantDao.approveSeriesJoinRequest(
                            match.getSeries().getId(), targetUser, Instant.now(clock));
            if (approvedRows <= 0) {
                throw new MatchFullException();
            }
            matchNotificationService.notifyPlayerRequestApproved(match, targetUser);
            return;
        }

        if (!EventStatus.OPEN.equals(match.getStatus())) {
            throw new MatchClosedException();
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchFullException();
        }

        if (!matchParticipantDao.approveRequest(matchId, targetUser)) {
            throw new MatchParticipationNoPendingRequestException();
        }

        matchNotificationService.notifyPlayerRequestApproved(match, targetUser);
    }

    @Override
    @Transactional
    public void rejectRequest(final Long matchId, final User host, final User targetUser) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        nonNullUser(targetUser);
        requireHost(match, host.getId());
        requireApprovalBasedMatch(match);

        if (!matchParticipantDao.rejectRequest(matchId, targetUser)) {
            throw new MatchParticipationNoPendingRequestException();
        }

        matchNotificationService.notifyPlayerRequestRejected(match, targetUser);
    }

    @Override
    @Transactional
    public void removeParticipant(final Long matchId, final User host, final User targetUser) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        nonNullUser(targetUser);

        if (host.getId().equals(targetUser.getId())) {
            leaveMatch(match, targetUser);
            return;
        }

        requireHost(match, host.getId());

        if (!matchParticipantDao.removeParticipant(matchId, targetUser)) {
            throw new MatchParticipationNotParticipantException();
        }

        matchNotificationService.notifyPlayerRemovedByHost(match, targetUser);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Override
    public List<User> findPendingRequests(final Long matchId, final User host) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        requireHost(match, host.getId());
        requireApprovalBasedMatch(match);
        return matchParticipantDao.findPendingRequests(matchId);
    }

    @Override
    public List<PendingJoinRequest> findPendingRequestsForHost(final User host) {
        nonNullUser(host);
        return matchParticipantDao.findPendingRequestsForHost(host);
    }

    @Override
    public List<User> findConfirmedParticipants(final Long matchId, final User host) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        requireHost(match, host.getId());
        return matchParticipantDao.findConfirmedParticipants(matchId);
    }

    @Override
    public List<Match> findPendingRequestMatches(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("exception.user.notNull");
        }
        final Long userId = user.getId();
        return matchParticipantDao.findPendingMatchIds(user).stream()
                .map(matchDao::findMatchById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(match -> !isHost(match, userId))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Invite-only flow
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void inviteUser(final Long matchId, final User host, final String email) {
        inviteUser(matchId, host, email, false);
    }

    @Override
    @Transactional
    public void inviteUser(
            final Long matchId, final User host, final String email, final boolean includeSeries) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        requireHost(match, host.getId());

        final User target =
                userService.findByEmail(email).orElseThrow(() -> new UserNotFoundException());

        if (isHost(match, target.getId())) {
            throw new MatchParticipationIsHostException();
        }

        requireInvitableMatch(match);

        if (includeSeries && match.isRecurringOccurrence()) {
            inviteUserToSeries(match, target);
            return;
        }

        inviteUserToMatch(match, target);
    }

    private void inviteUserToMatch(final Match match, final User target) {
        final Long matchId = match.getId();
        nonNullUser(target);
        if (matchParticipantDao.hasActiveReservation(matchId, target)) {
            throw new MatchParticipationAlreadyJoinedException();
        }

        if (matchParticipantDao.hasInvitation(matchId, target)) {
            throw new MatchParticipationAlreadyInvitedException();
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchFullException();
        }

        if (!matchParticipantDao.inviteUser(matchId, target)) {
            throw new MatchParticipationAlreadyInvitedException();
        }

        dispatchMatchInvitation(target, match);
    }

    private void inviteUserToSeries(final Match match, final User target) {
        final List<Match> occurrences = matchDao.findSeriesOccurrences(match.getSeries().getId());
        final SeriesInvitationEvaluation evaluation =
                evaluateSeriesInvitationTargets(occurrences, target);
        if (evaluation.invitableOccurrenceCount() == 0) {
            seriesInvitationFailure(evaluation);
        }

        int invitedCount = 0;
        for (final Long targetMatchId : evaluation.targetMatchIds()) {
            if (matchParticipantDao.inviteUser(targetMatchId, target, true)) {
                invitedCount++;
            }
        }

        if (invitedCount == 0) {
            throw new MatchParticipationSeriesAlreadyCoveredException();
        }

        dispatchSeriesInvitation(target, match, invitedCount);
    }

    @Override
    @Transactional
    public void acceptInvite(final Long matchId, final User user) {
        final Match match = requireMatch(matchId);
        nonNullUser(user);
        final Instant now = Instant.now(clock);

        if (isHost(match, user.getId())) {
            throw new MatchParticipationIsHostException();
        }

        if (!matchParticipantDao.hasInvitation(matchId, user)) {
            throw new MatchParticipationNoInvitationException();
        }

        if (match.isRecurringOccurrence()
                && matchParticipantDao.isSeriesInvitation(matchId, user)) {
            final int acceptedRows =
                    matchParticipantDao.acceptSeriesInvite(match.getSeries().getId(), user, now);
            if (acceptedRows <= 0) {
                throw new MatchParticipationNoInvitationException();
            }
            matchNotificationService.notifyHostInviteAccepted(match, user);
            return;
        }

        if (!EventStatus.OPEN.equals(match.getStatus())) {
            throw new MatchClosedException();
        }

        if (!match.getStartsAt().isAfter(now)) {
            throw new MatchStartedException();
        }

        if (!matchParticipantDao.acceptInvite(matchId, user)) {
            throw new MatchParticipationNoInvitationException();
        }

        matchNotificationService.notifyHostInviteAccepted(match, user);
    }

    @Override
    @Transactional
    public void declineInvite(final Long matchId, final User user) {
        final Match match = requireMatch(matchId);
        nonNullUser(user);

        if (!matchParticipantDao.hasInvitation(matchId, user)) {
            throw new MatchParticipationNoInvitationException();
        }

        if (match.isRecurringOccurrence()
                && matchParticipantDao.isSeriesInvitation(matchId, user)) {
            final int declinedRows =
                    matchParticipantDao.declineSeriesInvite(match.getSeries().getId(), user);
            if (declinedRows <= 0) {
                throw new MatchParticipationNoInvitationException();
            }
            matchNotificationService.notifyHostInviteDeclined(match, user);
            return;
        }

        if (!matchParticipantDao.declineInvite(matchId, user)) {
            throw new MatchParticipationNoInvitationException();
        }

        matchNotificationService.notifyHostInviteDeclined(match, user);
    }

    @Override
    public boolean hasInvitation(final Long matchId, final User user) {
        return matchParticipantDao.hasInvitation(matchId, user);
    }

    @Override
    public boolean isSeriesInvitation(final Long matchId, final User user) {
        return matchParticipantDao.isSeriesInvitation(matchId, user);
    }

    @Override
    public List<User> findInvitedUsers(final Long matchId, final User host) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        requireHost(match, host.getId());
        return matchParticipantDao.findInvitedUsers(matchId);
    }

    @Override
    public List<User> findDeclinedInvitees(final Long matchId, final User host) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        requireHost(match, host.getId());
        return matchParticipantDao.findDeclinedInvitees(matchId);
    }

    @Override
    public List<Match> findInvitedMatches(final User user) {
        nonNullUser(user);
        return matchParticipantDao.findInvitedMatchIds(user).stream()
                .map(matchDao::findMatchById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(match -> !isHost(match, user.getId()))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Match requireMatch(final Long matchId) {
        return matchDao.findMatchById(matchId).orElseThrow(() -> new MatchNotFoundException());
    }

    private void requireHost(final Match match, final Long userId) {
        if (!isHost(match, userId)) {
            throw new MatchForbiddenActionException();
        }
    }

    private static boolean isHost(final Match match, final Long userId) {
        return userId != null && userId.equals(match.getHost().getId());
    }

    private static void requireInvitableMatch(final Match match) {
        if (!EventStatus.OPEN.equals(match.getStatus())) {
            throw new MatchClosedException();
        }

        if (match.getVisibility() != EventVisibility.PRIVATE
                || match.getJoinPolicy() != EventJoinPolicy.INVITE_ONLY) {
            throw new MatchParticipationNotInviteOnlyException();
        }
    }

    private static void requireApprovalBasedMatch(final Match match) {
        if (match.getVisibility() != EventVisibility.PUBLIC
                || match.getJoinPolicy() != EventJoinPolicy.APPROVAL_REQUIRED) {
            throw new MatchParticipationNotInviteOnlyException();
        }
    }

    private void dispatchMatchInvitation(final User target, final Match match) {
        mailDispatchService.sendMatchInvitation(target, match);
    }

    private void dispatchSeriesInvitation(
            final User target, final Match match, final int occurrenceCount) {
        mailDispatchService.sendSeriesInvitation(target, match, occurrenceCount);
    }

    private void leaveMatch(final Match match, final User user) {
        nonNullUser(user);

        if (!match.getStartsAt().isAfter(Instant.now(clock))) {
            throw new MatchStartedException();
        }

        if (!EventStatus.OPEN.equals(match.getStatus())) {
            throw new MatchParticipationNotCancellableException();
        }

        if (!matchParticipantDao.hasActiveReservation(match.getId(), user)) {
            throw new MatchParticipationNotJoinedException();
        }

        if (!matchParticipantDao.removeParticipant(match.getId(), user)) {
            throw new MatchParticipationNotCancellableException();
        }

        if (isHost(match, user.getId())) {
            return;
        }

        matchNotificationService.notifyHostPlayerLeft(match, user);
    }

    private SeriesJoinRequestEvaluation evaluateSeriesJoinRequestTargets(
            final List<Match> occurrences,
            final Set<Long> activeFutureReservationMatchIds,
            final Set<Long> pendingFutureRequestMatchIds,
            final Instant now) {
        int futureOccurrenceCount = 0;
        int futureOpenApprovalOccurrenceCount = 0;
        int joinedFutureOpenApprovalOccurrenceCount = 0;
        int pendingFutureOpenApprovalOccurrenceCount = 0;
        final java.util.ArrayList<Long> targetMatchIds = new java.util.ArrayList<>();
        int fullOccurrenceCount = 0;

        for (final Match occurrence : occurrences) {
            if (!occurrence.getStartsAt().isAfter(now)) {
                continue;
            }

            futureOccurrenceCount++;
            if (!isSeriesJoinRequestableOccurrence(occurrence)) {
                continue;
            }

            futureOpenApprovalOccurrenceCount++;
            if (activeFutureReservationMatchIds.contains(occurrence.getId())) {
                joinedFutureOpenApprovalOccurrenceCount++;
                continue;
            }

            if (pendingFutureRequestMatchIds.contains(occurrence.getId())) {
                pendingFutureOpenApprovalOccurrenceCount++;
                continue;
            }

            if (occurrence.getJoinedPlayers() >= occurrence.getMaxPlayers()) {
                fullOccurrenceCount++;
                continue;
            }

            targetMatchIds.add(occurrence.getId());
        }

        final boolean joined =
                futureOpenApprovalOccurrenceCount > 0
                        && joinedFutureOpenApprovalOccurrenceCount
                                == futureOpenApprovalOccurrenceCount;
        final boolean pending =
                futureOpenApprovalOccurrenceCount > 0
                        && pendingFutureOpenApprovalOccurrenceCount
                                == futureOpenApprovalOccurrenceCount;
        final boolean alreadyCovered =
                futureOpenApprovalOccurrenceCount > 0
                        && joinedFutureOpenApprovalOccurrenceCount
                                        + pendingFutureOpenApprovalOccurrenceCount
                                == futureOpenApprovalOccurrenceCount;
        return new SeriesJoinRequestEvaluation(
                futureOccurrenceCount,
                futureOpenApprovalOccurrenceCount,
                joined,
                pending || alreadyCovered,
                List.copyOf(targetMatchIds),
                fullOccurrenceCount);
    }

    private static boolean isSeriesJoinRequestableOccurrence(final Match occurrence) {
        return EventStatus.OPEN.equals(occurrence.getStatus())
                && occurrence.getVisibility() == EventVisibility.PUBLIC
                && occurrence.getJoinPolicy() == EventJoinPolicy.APPROVAL_REQUIRED;
    }

    private SeriesInvitationEvaluation evaluateSeriesInvitationTargets(
            final List<Match> occurrences, final User user) {
        int futureOccurrenceCount = 0;
        int futureOpenInviteOnlyOccurrenceCount = 0;
        int joinedFutureOpenInviteOnlyOccurrenceCount = 0;
        int invitedFutureOpenInviteOnlyOccurrenceCount = 0;
        int fullOccurrenceCount = 0;
        final ArrayList<Long> targetMatchIds = new java.util.ArrayList<>();
        final Instant now = Instant.now(clock);

        nonNullUser(user);

        for (final Match occurrence : occurrences) {
            if (!occurrence.getStartsAt().isAfter(now)) {
                continue;
            }

            futureOccurrenceCount++;
            if (!isSeriesInvitableOccurrence(occurrence)) {
                continue;
            }

            futureOpenInviteOnlyOccurrenceCount++;
            if (matchParticipantDao.hasActiveReservation(occurrence.getId(), user)) {
                joinedFutureOpenInviteOnlyOccurrenceCount++;
                continue;
            }

            if (matchParticipantDao.hasInvitation(occurrence.getId(), user)) {
                invitedFutureOpenInviteOnlyOccurrenceCount++;
                continue;
            }

            if (occurrence.getJoinedPlayers() >= occurrence.getMaxPlayers()) {
                fullOccurrenceCount++;
                continue;
            }

            targetMatchIds.add(occurrence.getId());
        }

        return new SeriesInvitationEvaluation(
                futureOccurrenceCount,
                futureOpenInviteOnlyOccurrenceCount,
                joinedFutureOpenInviteOnlyOccurrenceCount,
                invitedFutureOpenInviteOnlyOccurrenceCount,
                List.copyOf(targetMatchIds),
                fullOccurrenceCount);
    }

    private static boolean isSeriesInvitableOccurrence(final Match occurrence) {
        return EventStatus.OPEN.equals(occurrence.getStatus())
                && occurrence.getVisibility() == EventVisibility.PRIVATE
                && occurrence.getJoinPolicy() == EventJoinPolicy.INVITE_ONLY;
    }

    private static void nonNullUser(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("exception.user.notNull");
        }
    }

    private static void seriesJoinRequestFailure(final SeriesJoinRequestEvaluation evaluation) {
        if (evaluation.futureOccurrenceCount() == 0) {
            throw new MatchSeriesStartedException();
        }

        if (evaluation.futureOpenApprovalOccurrenceCount() == 0) {
            throw new MatchSeriesClosedException();
        }

        if (evaluation.joined()) {
            throw new MatchParticipationSeriesAlreadyJoinedException();
        }

        if (evaluation.pending()) {
            throw new MatchParticipationSeriesAlreadyPendingException();
        }

        if (evaluation.fullOccurrenceCount() > 0) {
            throw new MatchSeriesFullException();
        }

        throw new MatchSeriesClosedException();
    }

    private static void seriesInvitationFailure(final SeriesInvitationEvaluation evaluation) {
        if (evaluation.futureOccurrenceCount() == 0) {
            throw new MatchSeriesStartedException();
        }

        if (evaluation.futureOpenInviteOnlyOccurrenceCount() == 0) {
            throw new MatchSeriesClosedException();
        }

        if (evaluation.joined()) {
            throw new MatchParticipationSeriesAlreadyJoinedException();
        }

        if (evaluation.invited()) {
            throw new MatchParticipationSeriesAlreadyInvitedException();
        }

        if (evaluation.covered()) {
            throw new MatchParticipationSeriesAlreadyCoveredException();
        }

        if (evaluation.fullOccurrenceCount() > 0) {
            throw new MatchSeriesFullException();
        }

        throw new MatchSeriesClosedException();
    }

    private record SeriesJoinRequestEvaluation(
            int futureOccurrenceCount,
            int futureOpenApprovalOccurrenceCount,
            boolean joined,
            boolean pending,
            List<Long> targetMatchIds,
            int fullOccurrenceCount) {

        private int requestableOccurrenceCount() {
            return targetMatchIds.size();
        }

        private SeriesJoinRequestEvaluation asPending() {
            return new SeriesJoinRequestEvaluation(
                    futureOccurrenceCount,
                    futureOpenApprovalOccurrenceCount,
                    joined,
                    true,
                    targetMatchIds,
                    fullOccurrenceCount);
        }
    }

    private record SeriesInvitationEvaluation(
            int futureOccurrenceCount,
            int futureOpenInviteOnlyOccurrenceCount,
            int joinedFutureOpenInviteOnlyOccurrenceCount,
            int invitedFutureOpenInviteOnlyOccurrenceCount,
            List<Long> targetMatchIds,
            int fullOccurrenceCount) {

        private int invitableOccurrenceCount() {
            return targetMatchIds.size();
        }

        private boolean joined() {
            return futureOpenInviteOnlyOccurrenceCount > 0
                    && joinedFutureOpenInviteOnlyOccurrenceCount
                            == futureOpenInviteOnlyOccurrenceCount;
        }

        private boolean invited() {
            return futureOpenInviteOnlyOccurrenceCount > 0
                    && invitedFutureOpenInviteOnlyOccurrenceCount
                            == futureOpenInviteOnlyOccurrenceCount;
        }

        private boolean covered() {
            return futureOpenInviteOnlyOccurrenceCount > 0
                    && joinedFutureOpenInviteOnlyOccurrenceCount
                                    + invitedFutureOpenInviteOnlyOccurrenceCount
                            == futureOpenInviteOnlyOccurrenceCount;
        }
    }
}
