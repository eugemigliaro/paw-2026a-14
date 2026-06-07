package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MatchParticipantDataServiceImpl implements MatchParticipantDataService {

    private final MatchParticipantDao matchParticipantDao;

    @Autowired
    public MatchParticipantDataServiceImpl(final MatchParticipantDao matchParticipantDao) {
        this.matchParticipantDao = Objects.requireNonNull(matchParticipantDao);
    }

    @Override
    public List<User> findConfirmedParticipants(final Long matchId) {
        return matchParticipantDao.findConfirmedParticipants(matchId);
    }

    @Override
    public List<User> findInvitedUsers(final Long matchId) {
        return matchParticipantDao.findInvitedUsers(matchId);
    }

    @Override
    public List<User> findPendingRequests(final Long matchId) {
        return matchParticipantDao.findPendingRequests(matchId);
    }

    @Override
    public int countPendingRequests(final Long matchId) {
        return matchParticipantDao.countPendingRequests(matchId);
    }

    @Override
    public boolean hasActiveReservation(final Long matchId, final User user) {
        return matchParticipantDao.hasActiveReservation(matchId, user);
    }

    @Override
    public List<Long> findActiveFutureReservationMatchIdsForSeries(
            final Long seriesId, final User user, final Instant startsAfter) {
        return matchParticipantDao.findActiveFutureReservationMatchIdsForSeries(
                seriesId, user, startsAfter);
    }

    @Override
    public List<Long> findPendingFutureRequestMatchIdsForSeries(
            final Long seriesId, final User user, final Instant startsAfter) {
        return matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                seriesId, user, startsAfter);
    }

    @Override
    public boolean createReservationIfSpace(final Long matchId, final User user) {
        return matchParticipantDao.createReservationIfSpace(matchId, user);
    }

    @Override
    public int createSeriesReservationsIfSpace(
            final Long seriesId, final User user, final Instant startsAfter) {
        return matchParticipantDao.createSeriesReservationsIfSpace(seriesId, user, startsAfter);
    }

    @Override
    public int cancelFutureSeriesReservations(
            final Long seriesId, final User user, final Instant startsAfter) {
        return matchParticipantDao.cancelFutureSeriesReservations(seriesId, user, startsAfter);
    }

    @Override
    public boolean hasPendingRequest(final Long matchId, final User user) {
        return matchParticipantDao.hasPendingRequest(matchId, user);
    }

    @Override
    public boolean createJoinRequest(final Long matchId, final User user) {
        return matchParticipantDao.createJoinRequest(matchId, user);
    }

    @Override
    public boolean createSeriesJoinRequestIfSpace(final Long matchId, final User user) {
        return matchParticipantDao.createSeriesJoinRequestIfSpace(matchId, user);
    }

    @Override
    public List<PendingJoinRequest> findPendingRequestsForHost(final User host) {
        return matchParticipantDao.findPendingRequestsForHost(host);
    }

    @Override
    public boolean approveRequest(final Long matchId, final User user) {
        return matchParticipantDao.approveRequest(matchId, user);
    }

    @Override
    public int cancelPendingInvitations(final Long matchId) {
        return matchParticipantDao.cancelPendingInvitations(matchId);
    }

    @Override
    public int cancelPendingRequests(final Long matchId) {
        return matchParticipantDao.cancelPendingRequests(matchId);
    }

    @Override
    public int approveAllPendingRequests(final Long matchId) {
        return matchParticipantDao.approveAllPendingRequests(matchId);
    }

    @Override
    public int approveSeriesJoinRequest(
            final Long seriesId, final User user, final Instant startsAfter) {
        return matchParticipantDao.approveSeriesJoinRequest(seriesId, user, startsAfter);
    }

    @Override
    public boolean isSeriesJoinRequest(final Long matchId, final User user) {
        return matchParticipantDao.isSeriesJoinRequest(matchId, user);
    }

    @Override
    public boolean hasPendingSeriesRequest(final Long seriesId, final User user) {
        return matchParticipantDao.hasPendingSeriesRequest(seriesId, user);
    }

    @Override
    public boolean rejectRequest(final Long matchId, final User user) {
        return matchParticipantDao.rejectRequest(matchId, user);
    }

    @Override
    public boolean removeParticipant(final Long matchId, final User user) {
        return matchParticipantDao.removeParticipant(matchId, user);
    }

    @Override
    public int cancelFutureReservations(final User user, final Instant startsAfter) {
        return matchParticipantDao.cancelFutureReservations(user, startsAfter);
    }

    @Override
    public boolean cancelJoinRequest(final Long matchId, final User user) {
        return matchParticipantDao.cancelJoinRequest(matchId, user);
    }

    @Override
    public List<Long> findPendingMatchIds(final User user) {
        return matchParticipantDao.findPendingMatchIds(user);
    }

    @Override
    public List<Match> findPendingRequestMatches(final User user) {
        return matchParticipantDao.findPendingRequestMatches(user);
    }

    @Override
    public boolean inviteUser(final Long matchId, final User user) {
        return matchParticipantDao.inviteUser(matchId, user);
    }

    @Override
    public boolean inviteUser(final Long matchId, final User user, final boolean seriesInvitation) {
        return matchParticipantDao.inviteUser(matchId, user, seriesInvitation);
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
    public boolean acceptInvite(final Long matchId, final User user) {
        return matchParticipantDao.acceptInvite(matchId, user);
    }

    @Override
    public int acceptSeriesInvite(final Long seriesId, final User user, final Instant startsAfter) {
        return matchParticipantDao.acceptSeriesInvite(seriesId, user, startsAfter);
    }

    @Override
    public boolean declineInvite(final Long matchId, final User user) {
        return matchParticipantDao.declineInvite(matchId, user);
    }

    @Override
    public int declineSeriesInvite(final Long seriesId, final User user) {
        return matchParticipantDao.declineSeriesInvite(seriesId, user);
    }

    @Override
    public List<User> findDeclinedInvitees(final Long matchId) {
        return matchParticipantDao.findDeclinedInvitees(matchId);
    }

    @Override
    public List<Long> findInvitedMatchIds(final User user) {
        return matchParticipantDao.findInvitedMatchIds(user);
    }

    @Override
    public List<Match> findInvitedMatches(final User user) {
        return matchParticipantDao.findInvitedMatches(user);
    }
}
