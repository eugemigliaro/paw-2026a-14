package ar.edu.itba.paw.services;

import java.util.Set;

public interface MatchReservationService {

    boolean hasActiveReservation(Long matchId, Long userId);

    Set<Long> findActiveFutureReservationMatchIdsForSeries(Long seriesId, Long userId);

    void reserveSpot(Long matchId, Long userId);

    void reserveSeries(Long matchId, Long userId);

    void cancelSeriesReservations(Long matchId, Long userId);
}
