package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import java.time.Instant;
import java.util.List;

public interface MatchParticipantDataService {

    List<User> findConfirmedParticipants(Long matchId);

    List<User> findInvitedUsers(Long matchId);

    List<User> findPendingRequests(Long matchId);

    int countPendingRequests(Long matchId);

    boolean hasActiveReservation(Long matchId, User user);

    List<Long> findActiveFutureReservationMatchIdsForSeries(
            Long seriesId, User user, Instant startsAfter);

    List<Long> findPendingFutureRequestMatchIdsForSeries(
            Long seriesId, User user, Instant startsAfter);

    boolean createReservationIfSpace(Long matchId, User user);

    int createSeriesReservationsIfSpace(Long seriesId, User user, Instant startsAfter);

    int cancelFutureSeriesReservations(Long seriesId, User user, Instant startsAfter);

    boolean hasPendingRequest(Long matchId, User user);

    boolean createJoinRequest(Long matchId, User user);

    boolean createSeriesJoinRequestIfSpace(Long matchId, User user);

    PaginatedResult<PendingJoinRequest> findPendingRequestsForHost(
            User host, int page, int pageSize);

    boolean approveRequest(Long matchId, User user);

    int cancelPendingInvitations(Long matchId);

    int cancelPendingRequests(Long matchId);

    int approveAllPendingRequests(Long matchId);

    int approveSeriesJoinRequest(Long seriesId, User user, Instant startsAfter);

    boolean isSeriesJoinRequest(Long matchId, User user);

    boolean hasPendingSeriesRequest(Long seriesId, User user);

    boolean rejectRequest(Long matchId, User user);

    boolean removeParticipant(Long matchId, User user);

    int cancelFutureReservations(User user, Instant startsAfter);

    boolean cancelJoinRequest(Long matchId, User user);

    List<Long> findPendingMatchIds(User user);

    List<Match> findPendingRequestMatches(User user);

    boolean inviteUser(Long matchId, User user);

    boolean inviteUser(Long matchId, User user, boolean seriesInvitation);

    boolean hasInvitation(Long matchId, User user);

    boolean isSeriesInvitation(Long matchId, User user);

    boolean acceptInvite(Long matchId, User user);

    int acceptSeriesInvite(Long seriesId, User user, Instant startsAfter);

    boolean declineInvite(Long matchId, User user);

    int declineSeriesInvite(Long seriesId, User user);

    List<User> findDeclinedInvitees(Long matchId);

    List<Long> findInvitedMatchIds(User user);

    List<Match> findInvitedMatches(User user);
}
