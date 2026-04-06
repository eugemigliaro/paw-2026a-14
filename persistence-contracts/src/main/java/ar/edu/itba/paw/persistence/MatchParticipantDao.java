package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import java.util.List;

public interface MatchParticipantDao {

    boolean hasActiveReservation(Long matchId, Long userId);

    boolean createReservationIfSpace(Long matchId, Long userId);

    List<User> findConfirmedParticipants(Long matchId);
}
