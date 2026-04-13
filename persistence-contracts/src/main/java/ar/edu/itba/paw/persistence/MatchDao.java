package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.EventTimeFilter;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.Sport;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

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
            String status,
            Long bannerImageId);

    Optional<Match> findById(Long matchId);

    Optional<Match> findPublicMatchById(Long matchId);

    List<Match> findPublicMatches(
            String query,
            List<Sport> sports,
            EventTimeFilter timeFilter,
            MatchSort sort,
            ZoneId zoneId,
            int offset,
            int limit);

    default List<Match> findPublicMatches(
            String query,
            List<Sport> sports,
            EventTimeFilter timeFilter,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            MatchSort sort,
            ZoneId zoneId,
            int offset,
            int limit) {
        return findPublicMatches(query, sports, timeFilter, sort, zoneId, offset, limit);
    }

    int countPublicMatches(
            String query, List<Sport> sports, EventTimeFilter timeFilter, ZoneId zoneId);

    default int countPublicMatches(
            String query,
            List<Sport> sports,
            EventTimeFilter timeFilter,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ZoneId zoneId) {
        return countPublicMatches(query, sports, timeFilter, zoneId);
    }
}
