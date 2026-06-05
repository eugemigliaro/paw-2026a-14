package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSeries;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MatchDataService {

    Match createMatch(
            User host,
            String address,
            String title,
            String description,
            Instant startsAt,
            Instant endsAt,
            int maxPlayers,
            BigDecimal pricePerPlayer,
            Sport sport,
            EventVisibility visibility,
            EventJoinPolicy joinPolicy,
            EventStatus status,
            ImageMetadata bannerImageMetadata,
            Double latitude,
            Double longitude,
            MatchSeries series,
            Integer seriesOccurrenceIndex);

    Optional<Match> findById(Long matchId);

    List<Match> findSeriesOccurrences(Long seriesId);

    boolean softDeleteMatch(Long matchId, User deletedBy, String deleteReason);

    boolean restoreMatch(Long matchId);
}
