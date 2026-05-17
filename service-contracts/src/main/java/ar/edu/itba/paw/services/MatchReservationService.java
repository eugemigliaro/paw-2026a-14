package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import java.util.Set;

public interface MatchReservationService {

    boolean hasActiveReservation(Long matchId, User user);

    Set<Long> findActiveFutureReservationMatchIdsForSeries(Long seriesId, User user);

    void reserveSpot(Long matchId, User user);

    void reserveSeries(Long matchId, User user);

    void cancelSeriesReservations(Long matchId, User user);
}
