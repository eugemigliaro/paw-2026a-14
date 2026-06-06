package ar.edu.itba.paw.services.internal;

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
    public boolean removeParticipant(final Long matchId, final User user) {
        return matchParticipantDao.removeParticipant(matchId, user);
    }
}
