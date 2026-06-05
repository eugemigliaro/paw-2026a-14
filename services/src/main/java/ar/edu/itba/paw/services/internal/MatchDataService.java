package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import java.util.Optional;

public interface MatchDataService {

    Optional<Match> findById(Long matchId);

    boolean softDeleteMatch(Long matchId, User deletedBy, String deleteReason);

    boolean restoreMatch(Long matchId);
}
