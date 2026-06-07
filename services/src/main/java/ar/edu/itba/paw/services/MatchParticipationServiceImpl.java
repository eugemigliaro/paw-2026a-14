package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationAlreadyInvitedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationAlreadyJoinedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationAlreadyPendingException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationClosedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationForbiddenException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationFullException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationInvalidUserException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationIsHostException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNoInvitationException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNoPendingRequestException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNotCancellableException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNotFoundException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNotInviteOnlyException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNotJoinedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNotParticipantException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNotRecurringException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesAlreadyCoveredException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesAlreadyInvitedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesAlreadyJoinedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesAlreadyPendingException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesClosedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesFullException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesStartedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationStartedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationUnauthenticatedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationUserNotFoundException;
import ar.edu.itba.paw.services.internal.MatchDataService;
import ar.edu.itba.paw.services.internal.MatchParticipantDataService;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MatchParticipationServiceImpl implements MatchParticipationService {

    private final MatchDataService matchDataService;
    private final MatchParticipantDataService matchParticipantDataService;
    private final UserService userService;
    private final Clock clock;
    private final MailDispatchService mailDispatchService;
    private final MatchNotificationService matchNotificationService;

    public MatchParticipationServiceImpl(
            final MatchDataService matchDataService,
            final MatchParticipantDataService matchParticipantDataService,
            final UserService userService,
            final Clock clock) {
        this(
                matchDataService,
                matchParticipantDataService,
                userService,
                clock,
                new MailDispatchService() {},
                null);
    }

    @Autowired
    public MatchParticipationServiceImpl(
            final MatchDataService matchDataService,
            final MatchParticipantDataService matchParticipantDataService,
            final UserService userService,
            final Clock clock,
            final MailDispatchService mailDispatchService,
            final MatchNotificationService matchNotificationService) {
        this.matchDataService = matchDataService;
        this.matchParticipantDataService = matchParticipantDataService;
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
            throw new MatchParticipationUnauthenticatedException(
                    "You must be logged in to request to join a recurring event.");
        }

        if (isHost(match, user.getId())) {
            throw new MatchParticipationIsHostException(
                    "Hosts cannot request to join their own event.");
        }

        if (!EventStatus.OPEN.equals(match.getStatus())) {
            throw new MatchParticipationClosedException("The event is not open for join requests.");
        }

        if (match.getVisibility() != EventVisibility.PUBLIC
                || match.getJoinPolicy() != EventJoinPolicy.APPROVAL_REQUIRED) {
            throw new MatchParticipationNotInviteOnlyException(
                    "This event does not require approval to join.");
        }

        if (!match.getStartsAt().isAfter(Instant.now(clock))) {
            throw new MatchParticipationStartedException("The event has already started.");
        }

        if (matchParticipantDataService.hasActiveReservation(matchId, user)) {
            throw new MatchParticipationAlreadyJoinedException(
                    "You are already a confirmed participant.");
        }

        if (matchParticipantDataService.hasPendingRequest(matchId, user)) {
            throw new MatchParticipationAlreadyPendingException(
                    "You already have a pending join request for this event.");
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchParticipationFullException("The event is already full.");
        }

        if (!matchParticipantDataService.createJoinRequest(matchId, user)) {
            throw new MatchParticipationAlreadyPendingException(
                    "You already have a pending join request for this event.");
        }

        matchNotificationService.notifyHostJoinRequestReceived(match, user);
    }

    @Override
    @Transactional
    public void requestToJoinSeries(final Long matchId, final User user) {
        final Match match = requireMatch(matchId);
        if (user == null) {
            throw new MatchParticipationUnauthenticatedException(
                    "You must be logged in to request to join a recurring event.");
        }

        if (isHost(match, user.getId())) {
            throw new MatchParticipationIsHostException(
                    "Hosts cannot request to join their own recurring event series.");
        }

        if (!match.isRecurringOccurrence()) {
            throw new MatchParticipationNotRecurringException(
                    "The event is not a recurring event.");
        }

        final List<Match> occurrences =
                matchDataService.findSeriesOccurrences(match.getSeries().getId());
        final Instant now = Instant.now(clock);
        final Set<Long> activeFutureReservationMatchIds =
                Set.copyOf(
                        matchParticipantDataService.findActiveFutureReservationMatchIdsForSeries(
                                match.getSeries().getId(), user, now));
        final Set<Long> pendingFutureRequestMatchIds =
                Set.copyOf(
                        matchParticipantDataService.findPendingFutureRequestMatchIdsForSeries(
                                match.getSeries().getId(), user, now));
        final SeriesJoinRequestEvaluation evaluation =
                evaluateSeriesJoinRequestTargets(
                        occurrences,
                        activeFutureReservationMatchIds,
                        pendingFutureRequestMatchIds,
                        now);
        if (matchParticipantDataService.hasPendingSeriesRequest(match.getSeries().getId(), user)) {
            throw buildSeriesJoinRequestFailure(evaluation.asPending());
        }
        if (evaluation.requestableOccurrenceCount() == 0) {
            throw buildSeriesJoinRequestFailure(evaluation);
        }

        final Long requestMatchId =
                pendingFutureRequestMatchIds.contains(matchId)
                        ? matchId
                        : evaluation.targetMatchIds().get(0);
        if (!matchParticipantDataService.createSeriesJoinRequestIfSpace(requestMatchId, user)) {
            final Instant currentNow = Instant.now(clock);
            final SeriesJoinRequestEvaluation currentEvaluation =
                    evaluateSeriesJoinRequestTargets(
                            matchDataService.findSeriesOccurrences(match.getSeries().getId()),
                            Set.copyOf(
                                    matchParticipantDataService
                                            .findActiveFutureReservationMatchIdsForSeries(
                                                    match.getSeries().getId(), user, currentNow)),
                            Set.copyOf(
                                    matchParticipantDataService
                                            .findPendingFutureRequestMatchIdsForSeries(
                                                    match.getSeries().getId(), user, currentNow)),
                            currentNow);
            throw buildSeriesJoinRequestFailure(currentEvaluation);
        }

        matchNotificationService.notifyHostJoinRequestReceived(match, user);
    }

    @Override
    @Transactional
    public void cancelJoinRequest(final Long matchId, final User user) {
        if (!matchParticipantDataService.hasPendingRequest(matchId, user)) {
            throw new MatchParticipationNoPendingRequestException(
                    "No pending join request found for this event.");
        }

        if (!matchParticipantDataService.cancelJoinRequest(matchId, user)) {
            throw new MatchParticipationNoPendingRequestException(
                    "No pending join request found for this event.");
        }
    }

    @Override
    public boolean hasPendingRequest(final Long matchId, final User user) {
        return matchParticipantDataService.hasPendingRequest(matchId, user);
    }

    @Override
    public boolean hasPendingSeriesRequest(final Long matchId, final User user) {
        final Match match = requireMatch(matchId);
        return match.isRecurringOccurrence()
                && matchParticipantDataService.hasPendingSeriesRequest(
                        match.getSeries().getId(), user);
    }

    @Override
    public Set<Long> findPendingFutureRequestMatchIdsForSeries(
            final Long seriesId, final User user) {
        if (seriesId == null || user == null) {
            return Set.of();
        }
        return Set.copyOf(
                matchParticipantDataService.findPendingFutureRequestMatchIdsForSeries(
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
                && matchParticipantDataService.isSeriesJoinRequest(matchId, targetUser)) {
            final int approvedRows =
                    matchParticipantDataService.approveSeriesJoinRequest(
                            match.getSeries().getId(), targetUser, Instant.now(clock));
            if (approvedRows <= 0) {
                throw new MatchParticipationFullException(
                        "The event is full; cannot approve more participants.");
            }
            matchNotificationService.notifyPlayerRequestApproved(match, targetUser);
            return;
        }

        if (!EventStatus.OPEN.equals(match.getStatus())) {
            throw new MatchParticipationClosedException("The event is not open.");
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchParticipationFullException(
                    "The event is full; cannot approve more participants.");
        }

        if (!matchParticipantDataService.approveRequest(matchId, targetUser)) {
            throw new MatchParticipationNoPendingRequestException(
                    "No pending join request found for the specified user.");
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

        if (!matchParticipantDataService.rejectRequest(matchId, targetUser)) {
            throw new MatchParticipationNoPendingRequestException(
                    "No pending join request found for the specified user.");
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

        if (!matchParticipantDataService.removeParticipant(matchId, targetUser)) {
            throw new MatchParticipationNotParticipantException(
                    "The specified user is not a confirmed participant of this event.");
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
        return matchParticipantDataService.findPendingRequests(matchId);
    }

    @Override
    public List<PendingJoinRequest> findPendingRequestsForHost(final User host) {
        nonNullUser(host);
        return matchParticipantDataService.findPendingRequestsForHost(host);
    }

    @Override
    public List<User> findConfirmedParticipants(final Long matchId, final User host) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        requireHost(match, host.getId());
        return matchParticipantDataService.findConfirmedParticipants(matchId);
    }

    @Override
    public List<Match> findPendingRequestMatches(final User user) {
        if (user == null) {
            throw new MatchParticipationInvalidUserException("User must be specified");
        }
        return matchParticipantDataService.findPendingRequestMatches(user);
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
                userService
                        .findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new MatchParticipationUserNotFoundException(
                                                "No user found with that email address."));

        if (isHost(match, target.getId())) {
            throw new MatchParticipationIsHostException(
                    "The host user cannot be invited to their own match.");
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
        if (matchParticipantDataService.hasActiveReservation(matchId, target)) {
            throw new MatchParticipationAlreadyJoinedException(
                    "That user is already a confirmed participant.");
        }

        if (matchParticipantDataService.hasInvitation(matchId, target)) {
            throw new MatchParticipationAlreadyInvitedException(
                    "That user already has a pending invitation.");
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchParticipationFullException("The event is already full.");
        }

        if (!matchParticipantDataService.inviteUser(matchId, target)) {
            throw new MatchParticipationAlreadyInvitedException("Could not send the invitation.");
        }

        dispatchMatchInvitation(target, match);
    }

    private void inviteUserToSeries(final Match match, final User target) {
        final List<Match> occurrences =
                matchDataService.findSeriesOccurrences(match.getSeries().getId());
        final SeriesInvitationEvaluation evaluation =
                evaluateSeriesInvitationTargets(occurrences, target);
        if (evaluation.invitableOccurrenceCount() == 0) {
            throw buildSeriesInvitationFailure(evaluation);
        }

        int invitedCount = 0;
        for (final Long targetMatchId : evaluation.targetMatchIds()) {
            if (matchParticipantDataService.inviteUser(targetMatchId, target, true)) {
                invitedCount++;
            }
        }

        if (invitedCount == 0) {
            throw new MatchParticipationSeriesAlreadyCoveredException(
                    "That user is already invited or participating in this series.");
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
            throw new MatchParticipationIsHostException(
                    "The host cannot accept an invitation to their own event.");
        }

        if (!matchParticipantDataService.hasInvitation(matchId, user)) {
            throw new MatchParticipationNoInvitationException(
                    "No pending invitation found for this event.");
        }

        if (match.isRecurringOccurrence()
                && matchParticipantDataService.isSeriesInvitation(matchId, user)) {
            final int acceptedRows =
                    matchParticipantDataService.acceptSeriesInvite(
                            match.getSeries().getId(), user, now);
            if (acceptedRows <= 0) {
                throw new MatchParticipationNoInvitationException(
                        "No pending invitation found for this series.");
            }
            matchNotificationService.notifyHostInviteAccepted(match, user);
            return;
        }

        if (!EventStatus.OPEN.equals(match.getStatus())) {
            throw new MatchParticipationClosedException("The event is not open.");
        }

        if (!match.getStartsAt().isAfter(now)) {
            throw new MatchParticipationStartedException("The event has already started.");
        }

        if (!matchParticipantDataService.acceptInvite(matchId, user)) {
            throw new MatchParticipationNoInvitationException(
                    "No pending invitation found for this event.");
        }

        matchNotificationService.notifyHostInviteAccepted(match, user);
    }

    @Override
    @Transactional
    public void declineInvite(final Long matchId, final User user) {
        final Match match = requireMatch(matchId);
        nonNullUser(user);

        if (!matchParticipantDataService.hasInvitation(matchId, user)) {
            throw new MatchParticipationNoInvitationException(
                    "No pending invitation found for this event.");
        }

        if (match.isRecurringOccurrence()
                && matchParticipantDataService.isSeriesInvitation(matchId, user)) {
            final int declinedRows =
                    matchParticipantDataService.declineSeriesInvite(
                            match.getSeries().getId(), user);
            if (declinedRows <= 0) {
                throw new MatchParticipationNoInvitationException(
                        "No pending invitation found for this series.");
            }
            matchNotificationService.notifyHostInviteDeclined(match, user);
            return;
        }

        if (!matchParticipantDataService.declineInvite(matchId, user)) {
            throw new MatchParticipationNoInvitationException(
                    "No pending invitation found for this event.");
        }

        matchNotificationService.notifyHostInviteDeclined(match, user);
    }

    @Override
    public boolean hasInvitation(final Long matchId, final User user) {
        return matchParticipantDataService.hasInvitation(matchId, user);
    }

    @Override
    public boolean isSeriesInvitation(final Long matchId, final User user) {
        return matchParticipantDataService.isSeriesInvitation(matchId, user);
    }

    @Override
    public List<User> findInvitedUsers(final Long matchId, final User host) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        requireHost(match, host.getId());
        requireInviteOnlyMatch(match);
        return matchParticipantDataService.findInvitedUsers(matchId);
    }

    @Override
    public List<User> findDeclinedInvitees(final Long matchId, final User host) {
        final Match match = requireMatch(matchId);
        nonNullUser(host);
        requireHost(match, host.getId());
        requireInviteOnlyMatch(match);
        return matchParticipantDataService.findDeclinedInvitees(matchId);
    }

    @Override
    public List<Match> findInvitedMatches(final User user) {
        nonNullUser(user);
        return matchParticipantDataService.findInvitedMatches(user);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Match requireMatch(final Long matchId) {
        return matchDataService
                .findById(matchId)
                .orElseThrow(
                        () -> new MatchParticipationNotFoundException("The event does not exist."));
    }

    private void requireHost(final Match match, final Long userId) {
        if (!isHost(match, userId)) {
            throw new MatchParticipationForbiddenException(
                    "Only the host can perform this action.");
        }
    }

    private static boolean isHost(final Match match, final Long userId) {
        return userId != null && userId.equals(match.getHost().getId());
    }

    private static void requireInvitableMatch(final Match match) {
        if (!EventStatus.OPEN.equals(match.getStatus())) {
            throw new MatchParticipationClosedException("The event is not open.");
        }

        requireInviteOnlyMatch(match);
    }

    private static void requireInviteOnlyMatch(final Match match) {
        if (match.getVisibility() != EventVisibility.PRIVATE
                || match.getJoinPolicy() != EventJoinPolicy.INVITE_ONLY) {
            throw new MatchParticipationNotInviteOnlyException(
                    "Invitations are only supported for private invite-only events.");
        }
    }

    private static void requireApprovalBasedMatch(final Match match) {
        if (match.getVisibility() != EventVisibility.PUBLIC
                || match.getJoinPolicy() != EventJoinPolicy.APPROVAL_REQUIRED) {
            throw new MatchParticipationNotInviteOnlyException(
                    "This action is only supported for public events that require approval to join.");
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
            throw new MatchParticipationStartedException("The event has already started.");
        }

        if (!EventStatus.OPEN.equals(match.getStatus())) {
            throw new MatchParticipationNotCancellableException(
                    "This reservation can no longer be cancelled.");
        }

        if (!matchParticipantDataService.hasActiveReservation(match.getId(), user)) {
            throw new MatchParticipationNotJoinedException(
                    "This account does not have an active reservation.");
        }

        if (!matchParticipantDataService.removeParticipant(match.getId(), user)) {
            throw new MatchParticipationNotCancellableException(
                    "This reservation can no longer be cancelled.");
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
            if (matchParticipantDataService.hasActiveReservation(occurrence.getId(), user)) {
                joinedFutureOpenInviteOnlyOccurrenceCount++;
                continue;
            }

            if (matchParticipantDataService.hasInvitation(occurrence.getId(), user)) {
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
            throw new MatchParticipationInvalidUserException("User must be specified.");
        }
    }

    private static MatchParticipationException buildSeriesJoinRequestFailure(
            final SeriesJoinRequestEvaluation evaluation) {
        if (evaluation.futureOccurrenceCount() == 0) {
            return new MatchParticipationSeriesStartedException(
                    "There are no upcoming dates left in this recurring event.");
        }

        if (evaluation.futureOpenApprovalOccurrenceCount() == 0) {
            return new MatchParticipationSeriesClosedException(
                    "The upcoming recurring dates are not open.");
        }

        if (evaluation.joined()) {
            return new MatchParticipationSeriesAlreadyJoinedException(
                    "You are already confirmed for the future recurring dates.");
        }

        if (evaluation.pending()) {
            return new MatchParticipationSeriesAlreadyPendingException(
                    "You already have pending join requests for the future recurring dates.");
        }

        if (evaluation.fullOccurrenceCount() > 0) {
            return new MatchParticipationSeriesFullException(
                    "The available future recurring dates are full.");
        }

        return new MatchParticipationSeriesClosedException(
                "The upcoming recurring dates are not open.");
    }

    private static MatchParticipationException buildSeriesInvitationFailure(
            final SeriesInvitationEvaluation evaluation) {
        if (evaluation.futureOccurrenceCount() == 0) {
            return new MatchParticipationSeriesStartedException(
                    "There are no upcoming dates left in this series.");
        }

        if (evaluation.futureOpenInviteOnlyOccurrenceCount() == 0) {
            return new MatchParticipationSeriesClosedException(
                    "The upcoming dates in this series are not open.");
        }

        if (evaluation.joined()) {
            return new MatchParticipationSeriesAlreadyJoinedException(
                    "That user is already participating in every available date in this series.");
        }

        if (evaluation.invited()) {
            return new MatchParticipationSeriesAlreadyInvitedException(
                    "That user is already invited to every available date in this series.");
        }

        if (evaluation.covered()) {
            return new MatchParticipationSeriesAlreadyCoveredException(
                    "That user is already invited or participating in this series.");
        }

        if (evaluation.fullOccurrenceCount() > 0) {
            return new MatchParticipationSeriesFullException(
                    "The available dates in this series are full.");
        }

        return new MatchParticipationSeriesClosedException(
                "The upcoming dates in this series are not open.");
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
