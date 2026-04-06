package ar.edu.itba.paw.persistence;

public interface MatchParticipantDao {

    boolean hasActiveReservation(Long matchId, Long userId);

    boolean createReservationIfSpace(Long matchId, Long userId);
}
