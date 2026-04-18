package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.EventTimeFilter;
import ar.edu.itba.paw.models.EventVisibility;
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

    Optional<Match> findMatchById(Long matchId);

    List<Match> findPublicMatches(
            String query,
            List<Sport> sports,
            EventTimeFilter timeFilter,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            MatchSort sort,
            ZoneId zoneId,
            int offset,
            int limit);

    int countPublicMatches(
            String query,
            List<Sport> sports,
            EventTimeFilter timeFilter,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ZoneId zoneId);

    List<Match> findHostedMatches(
            Long hostUserId,
            Boolean upcoming,
            String query,
            List<Sport> sports,
            List<EventVisibility> visibility,
            List<EventStatus> statuses,
            EventTimeFilter timeFilter,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            MatchSort sort,
            ZoneId zoneId,
            int offset,
            int limit);

    int countHostedMatches(
            Long hostUserId,
            Boolean upcoming,
            String query,
            List<Sport> sports,
            List<EventVisibility> visibility,
            List<EventStatus> statuses,
            EventTimeFilter timeFilter,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ZoneId zoneId);

    List<Match> findJoinedMatches(
            Long userId,
            Boolean upcoming,
            String query,
            List<Sport> sports,
            List<EventVisibility> visibility,
            List<EventStatus> statuses,
            EventTimeFilter timeFilter,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            MatchSort sort,
            ZoneId zoneId,
            int offset,
            int limit);

    int countJoinedMatches(
            Long userId,
            Boolean upcoming,
            String query,
            List<Sport> sports,
            List<EventVisibility> visibility,
            List<EventStatus> statuses,
            EventTimeFilter timeFilter,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ZoneId zoneId);
}
