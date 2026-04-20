package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import java.util.List;

public interface MatchParticipationService {

    // Player actions

    void requestToJoin(Long matchId, Long userId);

    void cancelJoinRequest(Long matchId, Long userId);

    boolean hasPendingRequest(Long matchId, Long userId);

    // Host actions

    void approveRequest(Long matchId, Long hostUserId, Long targetUserId);

    void rejectRequest(Long matchId, Long hostUserId, Long targetUserId);

    void removeParticipant(Long matchId, Long hostUserId, Long targetUserId);

    // Queries

    List<User> findPendingRequests(Long matchId, Long hostUserId);

    List<User> findConfirmedParticipants(Long matchId, Long hostUserId);
}
