package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.persistence.MatchDao;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MatchDataServiceImpl implements MatchDataService {

    private final MatchDao matchDao;

    public MatchDataServiceImpl(final MatchDao matchDao) {
        this.matchDao = Objects.requireNonNull(matchDao);
    }

    @Override
    public Optional<Match> findById(final Long matchId) {
        return matchDao.findById(matchId);
    }
}
