package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import java.util.List;
import java.util.Set;

public interface MatchParticipationService {

    // Player actions (public approval-required events)

    void requestToJoin(Long matchId, User user);

    void requestToJoinSeries(Long matchId, User user);

    void cancelJoinRequest(Long matchId, User user);

    boolean hasPendingRequest(Long matchId, User user);

    boolean hasPendingSeriesRequest(Long matchId, User user);

    Set<Long> findPendingFutureRequestMatchIdsForSeries(Long seriesId, User user);

    // Host actions (public approval-required events)

    void approveRequest(Long matchId, User host, User targetUser);

    void rejectRequest(Long matchId, User host, User targetUser);

    void removeParticipant(Long matchId, User host, User targetUser);

    // Queries (public approval-required events)

    List<User> findPendingRequests(Long matchId, User host);

    List<PendingJoinRequest> findPendingRequestsForHost(User host);

    List<User> findConfirmedParticipants(Long matchId, User host);

    List<Match> findPendingRequestMatches(User user);

    // Invite-only flow (private events)

    void inviteUser(Long matchId, User host, String email);

    default void inviteUser(
            final Long matchId, final User host, final String email, final boolean includeSeries) {
        inviteUser(matchId, host, email);
    }

    void acceptInvite(Long matchId, User user);

    void declineInvite(Long matchId, User user);

    boolean hasInvitation(Long matchId, User user);

    default boolean isSeriesInvitation(final Long matchId, final User user) {
        return false;
    }

    List<User> findInvitedUsers(Long matchId, User host);

    List<User> findDeclinedInvitees(Long matchId, User host);

    List<Match> findInvitedMatches(User user);
}
