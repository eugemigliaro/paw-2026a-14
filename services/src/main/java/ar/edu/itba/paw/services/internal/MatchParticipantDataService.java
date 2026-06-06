package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.User;
import java.util.List;

public interface MatchParticipantDataService {

    List<User> findConfirmedParticipants(Long matchId);
}
