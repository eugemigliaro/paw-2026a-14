package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
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
    private final Clock clock;

    @Autowired
    public MatchParticipationServiceImpl(
            final MatchDao matchDao,
            final MatchParticipantDao matchParticipantDao,
            final Clock clock) {
        this.matchDao = matchDao;
        this.matchParticipantDao = matchParticipantDao;
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

        if (!"invite_only".equalsIgnoreCase(match.getVisibility())) {
            throw new MatchParticipationException(
                    "not_invite_only",
                    "This event does not require approval to join.");
        }

        if (!match.getStartsAt().isAfter(Instant.now(clock))) {
            throw new MatchParticipationException(
                    "started", "The event has already started.");
        }

        if (userId.equals(match.getHostUserId())) {
            throw new MatchParticipationException(
                    "is_host", "The host cannot request to join their own event.");
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

    // -------------------------------------------------------------------------
    // Host actions
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void approveRequest(
            final Long matchId, final Long hostUserId, final Long targetUserId) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);

        if (!"open".equalsIgnoreCase(match.getStatus())) {
            throw new MatchParticipationException(
                    "closed", "The event is not open.");
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            throw new MatchParticipationException(
                    "full", "The event is full; cannot approve more participants.");
        }

        if (!matchParticipantDao.approveRequest(matchId, targetUserId)) {
            throw new MatchParticipationException(
                    "no_pending_request",
                    "No pending join request found for the specified user.");
        }
    }

    @Override
    @Transactional
    public void rejectRequest(
            final Long matchId, final Long hostUserId, final Long targetUserId) {
        final Match match = requireMatch(matchId);
        requireHost(match, hostUserId);

        if (!matchParticipantDao.rejectRequest(matchId, targetUserId)) {
            throw new MatchParticipationException(
                    "no_pending_request",
                    "No pending join request found for the specified user.");
        }
    }

    @Override
    @Transactional
    public void removeParticipant(
            final Long matchId, final Long hostUserId, final Long targetUserId) {
        final Match match = requireMatch(matchId);
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
    // Helpers
    // -------------------------------------------------------------------------

    private Match requireMatch(final Long matchId) {
        return matchDao
                .findMatchById(matchId)
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
}
