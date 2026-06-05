package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.Match;
import java.util.Optional;

public interface MatchDataService {

    Optional<Match> findById(Long matchId);
}
