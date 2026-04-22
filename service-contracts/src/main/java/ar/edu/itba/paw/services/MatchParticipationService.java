package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import java.util.List;

public interface MatchParticipationService {

    // Player actions (public approval-required events)

    void requestToJoin(Long matchId, Long userId);

    void cancelJoinRequest(Long matchId, Long userId);

    boolean hasPendingRequest(Long matchId, Long userId);

    // Host actions (public approval-required events)

    void approveRequest(Long matchId, Long hostUserId, Long targetUserId);

    void rejectRequest(Long matchId, Long hostUserId, Long targetUserId);

    void removeParticipant(Long matchId, Long hostUserId, Long targetUserId);

    // Queries (public approval-required events)

    List<User> findPendingRequests(Long matchId, Long hostUserId);

    List<User> findConfirmedParticipants(Long matchId, Long hostUserId);

    List<Match> findPendingRequestMatches(Long userId);

    // Invite-only flow (private events)

    void inviteUser(Long matchId, Long hostUserId, String email);

    void acceptInvite(Long matchId, Long userId);

    void declineInvite(Long matchId, Long userId);

    boolean hasInvitation(Long matchId, Long userId);

    List<User> findInvitedUsers(Long matchId, Long hostUserId);

    List<User> findDeclinedInvitees(Long matchId, Long hostUserId);

    List<Match> findInvitedMatches(Long userId);
}
