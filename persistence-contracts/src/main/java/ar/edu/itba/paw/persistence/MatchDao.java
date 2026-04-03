package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.EventTimeFilter;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.Sport;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public interface MatchDao {

    Match createMatch(
            Long hostUserId,
            String address,
            String title,
            String description,
            Instant startsAt,
            Instant endsAt,
            int maxPlayers,
            BigDecimal pricePerPlayer,
            Sport sport,
            String visibility,
            String status);

    List<Match> findPublicMatches(
            String query,
            Sport sport,
            EventTimeFilter timeFilter,
            MatchSort sort,
            ZoneId zoneId,
            int offset,
            int limit);

    int countPublicMatches(String query, Sport sport, EventTimeFilter timeFilter, ZoneId zoneId);
}
