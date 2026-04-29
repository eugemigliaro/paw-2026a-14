package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.MatchParticipationException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchParticipationServiceImpl implements MatchParticipationService {

    private final MatchDao matchDao;
    private final MatchParticipantDao matchParticipantDao;
    private final UserService userService;
    private final Clock clock;

    @Autowired
    public MatchParticipationServiceImpl(
            final MatchDao matchDao,
            final MatchParticipantDao matchParticipantDao,
            final UserService userService,
            final Clock clock) {
        this.matchDao = matchDao;
        this.matchParticipantDao = matchParticipantDao;
        this.userService = userService;
        this.clock = clock;
    }

    // -------------------------------------------------------------------------
    // Player actions
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void requestToJoin(final Long matchId, final Long userId) {
        final Match match = requireMatch(matchId);

        if (!"open".equalsIgnoreCase(match.getStatus())) {
            throw new MatchParticipationException(
                    "closed", "The event is not open for join requests.");
        }

        if (!"public".equalsIgnoreCase(match.getVisibility())
                || !"approval_required".equalsIgnoreCase(match.getJoinPolicy())) {
            throw new MatchParticipationException(
                    "not_invite_only", "This event does not require approval to join.");
        }

        if (!match.getStartsAt().isAfter(Instant.now(clock))) {
            throw new MatchParticipationException("started", "The event has already started.");
        }

        if (matchParticipantDao.hasActiveReservation(matchId, userId)) {
            throw new MatchParticipationException(
                    "already_joined", "You are already a confirmed participant.");
        }

        if (matchParticipantDao.hasPendingRequest(matchId, userId)) {
            throw new MatchParticipationException(
                    "already_pending", "You already have a pending join request for this event.");
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchParticipationException("full", "The event is already full.");
        }

        if (!matchParticipantDao.createJoinRequest(matchId, userId)) {
            throw new MatchParticipationException(
                    "already_pending", "You already have a pending join request for this event.");
        }
    }

    @Override
    @Transactional
    public void requestToJoinSeries(final Long matchId, final Long userId) {
        final Match match = requireMatch(matchId);

        if (!match.isRecurringOccurrence()) {
            throw new MatchParticipationException(
                    "not_recurring", "The event is not a recurring event.");
        }

        final List<Match> occurrences = matchDao.findSeriesOccurrences(match.getSeriesId());
        final SeriesJoinRequestEvaluation evaluation =
                evaluateSeriesJoinRequestTargets(occurrences, userId);
        if (matchParticipantDao.hasPendingSeriesRequest(match.getSeriesId(), userId)) {
            throw buildSeriesJoinRequestFailure(evaluation.asPending());
        }
        if (evaluation.requestableOccurrenceCount() == 0) {
            throw buildSeriesJoinRequestFailure(evaluation);
        }

        final Long requestMatchId =
                matchParticipantDao.hasPendingRequest(matchId, userId)
                        ? matchId
                        : evaluation.targetMatchIds().get(0);
        if (!matchParticipantDao.createSeriesJoinRequestIfSpace(requestMatchId, userId)) {
            final SeriesJoinRequestEvaluation currentEvaluation =
                    evaluateSeriesJoinRequestTargets(
                            matchDao.findSeriesOccurrences(match.getSeriesId()), userId);
            throw buildSeriesJoinRequestFailure(currentEvaluation);
        }
    }

    @Override
    @Transactional
    public void cancelJoinRequest(final Long matchId, final Long userId) {
        if (!matchParticipantDao.hasPendingRequest(matchId, userId)) {
            throw new MatchParticipationException(
                    "no_pending_request", "No pending join request found for this event.");
        }

        if (!matchParticipantDao.cancelJoinRequest(matchId, userId)) {
            throw new MatchParticipationException(
                    "no_pending_request", "No pending join request found for this event.");
        }
    }

    @Override
    public boolean hasPendingRequest(final Long matchId, final Long userId) {
        return matchParticipantDao.hasPendingRequest(matchId, userId);
    }

    @Override
    public boolean hasPendingSeriesRequest(final Long matchId, final Long userId) {
        final Match match = requireMatch(matchId);
        return match.isRecurringOccurrence()
                && matchParticipantDao.hasPendingSeriesRequest(match.getSeriesId(), userId);
    }

    // -------------------------------------------------------------------------
    // Host actions
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void approveRequest(final Long matchId, final Long hostUserId, final Long targetUserId) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);

        if (!"open".equalsIgnoreCase(match.getStatus())) {
            throw new MatchParticipationException("closed", "The event is not open.");
        }

        if (match.isRecurringOccurrence()
                && matchParticipantDao.isSeriesJoinRequest(matchId, targetUserId)) {
            final int approvedRows =
                    matchParticipantDao.approveSeriesJoinRequest(
                            match.getSeriesId(), targetUserId, Instant.now(clock));
            if (approvedRows <= 0) {
                throw new MatchParticipationException(
                        "full", "The event is full; cannot approve more participants.");
            }
            return;
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchParticipationException(
                    "full", "The event is full; cannot approve more participants.");
        }

        if (!matchParticipantDao.approveRequest(matchId, targetUserId)) {
            throw new MatchParticipationException(
                    "no_pending_request", "No pending join request found for the specified user.");
        }
    }

    @Override
    @Transactional
    public void rejectRequest(final Long matchId, final Long hostUserId, final Long targetUserId) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);

        if (!matchParticipantDao.rejectRequest(matchId, targetUserId)) {
            throw new MatchParticipationException(
                    "no_pending_request", "No pending join request found for the specified user.");
        }
    }

    @Override
    @Transactional
    public void removeParticipant(
            final Long matchId, final Long hostUserId, final Long targetUserId) {
        final Match match = requireMatch(matchId);

        if (hostUserId.equals(targetUserId)) {
            leaveMatch(match, targetUserId);
            return;
        }

        requireHost(match, hostUserId);

        if (!matchParticipantDao.removeParticipant(matchId, targetUserId)) {
            throw new MatchParticipationException(
                    "not_participant",
                    "The specified user is not a confirmed participant of this event.");
        }
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Override
    public List<User> findPendingRequests(final Long matchId, final Long hostUserId) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);
        return matchParticipantDao.findPendingRequests(matchId);
    }

    @Override
    public List<PendingJoinRequest> findPendingRequestsForHost(final Long hostUserId) {
        return matchParticipantDao.findPendingRequestsForHost(hostUserId);
    }

    @Override
    public List<User> findConfirmedParticipants(final Long matchId, final Long hostUserId) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);
        return matchParticipantDao.findConfirmedParticipants(matchId);
    }

    @Override
    public List<Match> findPendingRequestMatches(final Long userId) {
        return matchParticipantDao.findPendingMatchIds(userId).stream()
                .map(matchDao::findMatchById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Invite-only flow
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void inviteUser(final Long matchId, final Long hostUserId, final String email) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);

        if (!"open".equalsIgnoreCase(match.getStatus())) {
            throw new MatchParticipationException("closed", "The event is not open.");
        }

        if (!"private".equalsIgnoreCase(match.getVisibility())
                || !"invite_only".equalsIgnoreCase(match.getJoinPolicy())) {
            throw new MatchParticipationException(
                    "not_invite_only",
                    "Invitations are only supported for private invite-only events.");
        }

        final User target =
                userService
                        .findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new MatchParticipationException(
                                                "user_not_found",
                                                "No user found with that email address."));

        if (matchParticipantDao.hasActiveReservation(matchId, target.getId())) {
            throw new MatchParticipationException(
                    "already_joined", "That user is already a confirmed participant.");
        }

        if (matchParticipantDao.hasInvitation(matchId, target.getId())) {
            throw new MatchParticipationException(
                    "already_invited", "That user already has a pending invitation.");
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchParticipationException("full", "The event is already full.");
        }

        if (!matchParticipantDao.inviteUser(matchId, target.getId())) {
            throw new MatchParticipationException(
                    "already_invited", "Could not send the invitation.");
        }
    }

    @Override
    @Transactional
    public void acceptInvite(final Long matchId, final Long userId) {
        final Match match = requireMatch(matchId);

        if (!"open".equalsIgnoreCase(match.getStatus())) {
            throw new MatchParticipationException("closed", "The event is not open.");
        }

        if (!match.getStartsAt().isAfter(Instant.now(clock))) {
            throw new MatchParticipationException("started", "The event has already started.");
        }

        if (!matchParticipantDao.hasInvitation(matchId, userId)) {
            throw new MatchParticipationException(
                    "no_invitation", "No pending invitation found for this event.");
        }

        if (!matchParticipantDao.acceptInvite(matchId, userId)) {
            throw new MatchParticipationException(
                    "no_invitation", "No pending invitation found for this event.");
        }
    }

    @Override
    @Transactional
    public void declineInvite(final Long matchId, final Long userId) {
        if (!matchParticipantDao.hasInvitation(matchId, userId)) {
            throw new MatchParticipationException(
                    "no_invitation", "No pending invitation found for this event.");
        }

        if (!matchParticipantDao.declineInvite(matchId, userId)) {
            throw new MatchParticipationException(
                    "no_invitation", "No pending invitation found for this event.");
        }
    }

    @Override
    public boolean hasInvitation(final Long matchId, final Long userId) {
        return matchParticipantDao.hasInvitation(matchId, userId);
    }

    @Override
    public List<User> findInvitedUsers(final Long matchId, final Long hostUserId) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);
        return matchParticipantDao.findInvitedUsers(matchId);
    }

    @Override
    public List<User> findDeclinedInvitees(final Long matchId, final Long hostUserId) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);
        return matchParticipantDao.findDeclinedInvitees(matchId);
    }

    @Override
    public List<Match> findInvitedMatches(final Long userId) {
        return matchParticipantDao.findInvitedMatchIds(userId).stream()
                .map(matchDao::findMatchById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Match requireMatch(final Long matchId) {
        return matchDao.findMatchById(matchId)
                .orElseThrow(
                        () ->
                                new MatchParticipationException(
                                        "not_found", "The event does not exist."));
    }

    private void requireHost(final Match match, final Long userId) {
        if (!match.getHostUserId().equals(userId)) {
            throw new MatchParticipationException(
                    "forbidden", "Only the host can perform this action.");
        }
    }

    private void leaveMatch(final Match match, final Long userId) {
        if (!match.getStartsAt().isAfter(Instant.now(clock))) {
            throw new MatchParticipationException("started", "The event has already started.");
        }

        if (!"open".equalsIgnoreCase(match.getStatus())) {
            throw new MatchParticipationException(
                    "not_cancellable", "This reservation can no longer be cancelled.");
        }

        if (!matchParticipantDao.hasActiveReservation(match.getId(), userId)) {
            throw new MatchParticipationException(
                    "not_joined", "This account does not have an active reservation.");
        }

        if (!matchParticipantDao.removeParticipant(match.getId(), userId)) {
            throw new MatchParticipationException(
                    "not_cancellable", "This reservation can no longer be cancelled.");
        }
    }

    private SeriesJoinRequestEvaluation evaluateSeriesJoinRequestTargets(
            final List<Match> occurrences, final Long userId) {
        int futureOccurrenceCount = 0;
        int futureOpenApprovalOccurrenceCount = 0;
        int joinedFutureOpenApprovalOccurrenceCount = 0;
        int pendingFutureOpenApprovalOccurrenceCount = 0;
        final java.util.ArrayList<Long> targetMatchIds = new java.util.ArrayList<>();
        int fullOccurrenceCount = 0;
        final Instant now = Instant.now(clock);

        for (final Match occurrence : occurrences) {
            if (!occurrence.getStartsAt().isAfter(now)) {
                continue;
            }

            futureOccurrenceCount++;
            if (!isSeriesJoinRequestableOccurrence(occurrence)) {
                continue;
            }

            futureOpenApprovalOccurrenceCount++;
            if (matchParticipantDao.hasActiveReservation(occurrence.getId(), userId)) {
                joinedFutureOpenApprovalOccurrenceCount++;
                continue;
            }

            if (matchParticipantDao.hasPendingRequest(occurrence.getId(), userId)) {
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
        return "open".equalsIgnoreCase(occurrence.getStatus())
                && "public".equalsIgnoreCase(occurrence.getVisibility())
                && "approval_required".equalsIgnoreCase(occurrence.getJoinPolicy());
    }

    private static MatchParticipationException buildSeriesJoinRequestFailure(
            final SeriesJoinRequestEvaluation evaluation) {
        if (evaluation.futureOccurrenceCount() == 0) {
            return new MatchParticipationException(
                    "series_started", "There are no upcoming dates left in this recurring event.");
        }

        if (evaluation.futureOpenApprovalOccurrenceCount() == 0) {
            return new MatchParticipationException(
                    "series_closed", "The upcoming recurring dates are not open.");
        }

        if (evaluation.joined()) {
            return new MatchParticipationException(
                    "series_already_joined",
                    "You are already confirmed for the future recurring dates.");
        }

        if (evaluation.pending()) {
            return new MatchParticipationException(
                    "series_already_pending",
                    "You already have pending join requests for the future recurring dates.");
        }

        if (evaluation.fullOccurrenceCount() > 0) {
            return new MatchParticipationException(
                    "series_full", "The available future recurring dates are full.");
        }

        return new MatchParticipationException(
                "series_closed", "The upcoming recurring dates are not open.");
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
}
