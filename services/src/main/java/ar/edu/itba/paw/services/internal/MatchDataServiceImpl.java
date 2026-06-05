package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSeries;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.persistence.MatchDao;
import java.math.BigDecimal;
import java.time.Instant;
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
    public Match createMatch(
            final User host,
            final String address,
            final String title,
            final String description,
            final Instant startsAt,
            final Instant endsAt,
            final int maxPlayers,
            final BigDecimal pricePerPlayer,
            final Sport sport,
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy,
            final EventStatus status,
            final ImageMetadata bannerImageMetadata,
            final Double latitude,
            final Double longitude,
            final MatchSeries series,
            final Integer seriesOccurrenceIndex) {
        return matchDao.createMatch(
                host,
                address,
                title,
                description,
                startsAt,
                endsAt,
                maxPlayers,
                pricePerPlayer,
                sport,
                visibility,
                joinPolicy,
                status,
                bannerImageMetadata,
                latitude,
                longitude,
                series,
                seriesOccurrenceIndex);
    }

    @Override
    public Optional<Match> findById(final Long matchId) {
        return matchDao.findById(matchId);
    }

    @Override
    public boolean softDeleteMatch(
            final Long matchId, final User deletedBy, final String deleteReason) {
        return matchDao.softDeleteMatch(matchId, deletedBy, deleteReason);
    }

    @Override
    public boolean restoreMatch(final Long matchId) {
        return matchDao.restoreMatch(matchId);
    }
}
