package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import java.time.Instant;
import java.util.List;

public interface MatchParticipantDao {

    boolean hasActiveReservation(Long matchId, User user);

    List<Long> findActiveFutureReservationMatchIdsForSeries(
            Long seriesId, User user, Instant startsAfter);

    List<Long> findPendingFutureRequestMatchIdsForSeries(
            Long seriesId, User user, Instant startsAfter);

    boolean createReservationIfSpace(Long matchId, User user);

    int createSeriesReservationsIfSpace(Long seriesId, User user, Instant startsAfter);

    int cancelFutureSeriesReservations(Long seriesId, User user, Instant startsAfter);

    List<User> findConfirmedParticipants(Long matchId);

    boolean hasPendingRequest(Long matchId, User user);

    boolean createJoinRequest(Long matchId, User user);

    boolean createSeriesJoinRequestIfSpace(Long matchId, User user);

    List<User> findPendingRequests(Long matchId);

    int countPendingRequests(Long matchId);

    List<PendingJoinRequest> findPendingRequestsForHost(Long hostUserId);

    boolean approveRequest(Long matchId, User user);

    int approveAllPendingRequests(Long matchId);

    int approveSeriesJoinRequest(Long seriesId, User user, Instant startsAfter);

    boolean isSeriesJoinRequest(Long matchId, User user);

    boolean hasPendingSeriesRequest(Long seriesId, User user);

    boolean rejectRequest(Long matchId, User user);

    boolean removeParticipant(Long matchId, User user);

    boolean cancelJoinRequest(Long matchId, User user);

    int cancelPendingRequests(Long matchId);

    List<Long> findPendingMatchIds(User user);

    // Invite-only flow

    boolean inviteUser(Long matchId, User user);

    boolean inviteUser(Long matchId, User user, boolean seriesInvitation);

    boolean hasInvitation(Long matchId, User user);

    boolean isSeriesInvitation(Long matchId, User user);

    boolean acceptInvite(Long matchId, User user);

    int acceptSeriesInvite(Long seriesId, User user, Instant startsAfter);

    boolean declineInvite(Long matchId, User user);

    int declineSeriesInvite(Long seriesId, User user);

    List<User> findInvitedUsers(Long matchId);

    int cancelPendingInvitations(Long matchId);

    List<User> findDeclinedInvitees(Long matchId);

    List<Long> findInvitedMatchIds(User user);
}
