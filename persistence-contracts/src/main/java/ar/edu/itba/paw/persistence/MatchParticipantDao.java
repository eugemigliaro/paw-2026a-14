package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import java.util.List;

public interface MatchParticipantDao {

    boolean hasActiveReservation(Long matchId, Long userId);

    boolean createReservationIfSpace(Long matchId, Long userId);

    List<User> findConfirmedParticipants(Long matchId);

    boolean hasPendingRequest(Long matchId, Long userId);

    boolean createJoinRequest(Long matchId, Long userId);

    List<User> findPendingRequests(Long matchId);

    boolean approveRequest(Long matchId, Long userId);

    boolean rejectRequest(Long matchId, Long userId);

    boolean removeParticipant(Long matchId, Long userId);

    boolean cancelJoinRequest(Long matchId, Long userId);

    List<Long> findPendingMatchIds(Long userId);
}
