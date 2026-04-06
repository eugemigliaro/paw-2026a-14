package ar.edu.itba.paw.services;

public interface MatchReservationService {

    boolean hasActiveReservation(Long matchId, Long userId);

    void reserveSpot(Long matchId, Long userId);
}
