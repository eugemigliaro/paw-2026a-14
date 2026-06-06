package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.User;
import java.time.Instant;
import java.util.List;

public interface MatchParticipantDataService {

    List<User> findConfirmedParticipants(Long matchId);

    boolean hasActiveReservation(Long matchId, User user);

    List<Long> findActiveFutureReservationMatchIdsForSeries(
            Long seriesId, User user, Instant startsAfter);

    boolean createReservationIfSpace(Long matchId, User user);

    int createSeriesReservationsIfSpace(Long seriesId, User user, Instant startsAfter);

    int cancelFutureSeriesReservations(Long seriesId, User user, Instant startsAfter);

    boolean removeParticipant(Long matchId, User user);
}
