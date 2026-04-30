package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import java.time.Instant;
import java.util.List;

public interface MatchParticipantDao {

    boolean hasActiveReservation(Long matchId, Long userId);

    List<Long> findActiveFutureReservationMatchIdsForSeries(
            Long seriesId, Long userId, Instant startsAfter);

    boolean createReservationIfSpace(Long matchId, Long userId);

    int createSeriesReservationsIfSpace(Long seriesId, Long userId, Instant startsAfter);

    int cancelFutureSeriesReservations(Long seriesId, Long userId, Instant startsAfter);

    List<User> findConfirmedParticipants(Long matchId);

    boolean hasPendingRequest(Long matchId, Long userId);

    boolean createJoinRequest(Long matchId, Long userId);

    boolean createSeriesJoinRequestIfSpace(Long matchId, Long userId);

    List<User> findPendingRequests(Long matchId);

    List<PendingJoinRequest> findPendingRequestsForHost(Long hostUserId);

    boolean approveRequest(Long matchId, Long userId);

    int approveSeriesJoinRequest(Long seriesId, Long userId, Instant startsAfter);

    boolean isSeriesJoinRequest(Long matchId, Long userId);

    boolean hasPendingSeriesRequest(Long seriesId, Long userId);

    boolean rejectRequest(Long matchId, Long userId);

    boolean removeParticipant(Long matchId, Long userId);

    boolean cancelJoinRequest(Long matchId, Long userId);

    List<Long> findPendingMatchIds(Long userId);

    // Invite-only flow

    boolean inviteUser(Long matchId, Long userId);

    boolean inviteUser(Long matchId, Long userId, boolean seriesInvitation);

    boolean hasInvitation(Long matchId, Long userId);

    boolean isSeriesInvitation(Long matchId, Long userId);

    boolean acceptInvite(Long matchId, Long userId);

    int acceptSeriesInvite(Long seriesId, Long userId, Instant startsAfter);

    boolean declineInvite(Long matchId, Long userId);

    int declineSeriesInvite(Long seriesId, Long userId);

    List<User> findInvitedUsers(Long matchId);

    List<User> findDeclinedInvitees(Long matchId);

    List<Long> findInvitedMatchIds(Long userId);
}
