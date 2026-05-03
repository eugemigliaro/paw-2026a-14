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

    default Match createMatch(
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
            String joinPolicy,
            String status,
            Long bannerImageId) {
        return createMatch(
                hostUserId,
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
                bannerImageId,
                null,
                null,
                null,
                null);
    }

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
            String joinPolicy,
            String status,
            Long bannerImageId,
            Double latitude,
            Double longitude,
            Long seriesId,
            Integer seriesOccurrenceIndex);

    default Match createMatch(
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
            String joinPolicy,
            String status,
            Long bannerImageId,
            Double latitude,
            Double longitude) {
        return createMatch(
                hostUserId,
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
                bannerImageId,
                latitude,
                longitude,
                null,
                null);
    }

    default Match createMatch(
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
            String joinPolicy,
            String status,
            Long bannerImageId,
            Long seriesId,
            Integer seriesOccurrenceIndex) {
        return createMatch(
                hostUserId,
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
                bannerImageId,
                null,
                null,
                seriesId,
                seriesOccurrenceIndex);
    }

    Long createMatchSeries(
            Long hostUserId,
            String frequency,
            Instant startsAt,
            Instant endsAt,
            String timezone,
            java.time.LocalDate untilDate,
            Integer occurrenceCount);

    default Match createMatch(
            final Long hostUserId,
            final String address,
            final String title,
            final String description,
            final Instant startsAt,
            final Instant endsAt,
            final int maxPlayers,
            final BigDecimal pricePerPlayer,
            final Sport sport,
            final String visibility,
            final String status,
            final Long bannerImageId) {
        final String defaultJoinPolicy =
                "public".equalsIgnoreCase(visibility) ? "direct" : "approval_required";
        return createMatch(
                hostUserId,
                address,
                title,
                description,
                startsAt,
                endsAt,
                maxPlayers,
                pricePerPlayer,
                sport,
                visibility,
                defaultJoinPolicy,
                status,
                bannerImageId,
                null,
                null,
                null,
                null);
    }

    default boolean updateMatch(
            final Long matchId,
            final Long hostUserId,
            final String address,
            final String title,
            final String description,
            final Instant startsAt,
            final Instant endsAt,
            final int maxPlayers,
            final BigDecimal pricePerPlayer,
            final Sport sport,
            final String visibility,
            final String status,
            final Long bannerImageId) {
        final String defaultJoinPolicy =
                "private".equalsIgnoreCase(visibility) ? "invite_only" : "direct";
        return updateMatch(
                matchId,
                hostUserId,
                address,
                title,
                description,
                startsAt,
                endsAt,
                maxPlayers,
                pricePerPlayer,
                sport,
                visibility,
                defaultJoinPolicy,
                status,
                bannerImageId);
    }

    boolean updateMatch(
            Long matchId,
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
            String joinPolicy,
            String status,
            Long bannerImageId,
            Double latitude,
            Double longitude);

    default boolean updateMatch(
            Long matchId,
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
            String joinPolicy,
            String status,
            Long bannerImageId) {
        return updateMatch(
                matchId,
                hostUserId,
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
                bannerImageId,
                null,
                null);
    }

    boolean cancelMatch(Long matchId, Long hostUserId);

    Optional<Match> findById(Long matchId);

    Optional<Match> findPublicMatchById(Long matchId);

    List<Match> findSeriesOccurrences(Long seriesId);

    default Optional<Match> findMatchById(final Long matchId) {
        return findById(matchId);
    }

    List<Match> findPublicMatches(
            String query,
            List<Sport> sports,
            EventTimeFilter timeFilter,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            MatchSort sort,
            ZoneId zoneId,
            int offset,
            int limit);

    default List<Match> findPublicMatches(
            String query,
            List<Sport> sports,
            EventTimeFilter timeFilter,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            MatchSort sort,
            ZoneId zoneId,
            Double latitude,
            Double longitude,
            int offset,
            int limit) {
        return findPublicMatches(
                query,
                sports,
                timeFilter,
                startsAtFrom,
                startsAtTo,
                minPrice,
                maxPrice,
                sort,
                zoneId,
                offset,
                limit);
    }

    default List<Match> findPublicMatches(
            final String query,
            final List<Sport> sports,
            final EventTimeFilter timeFilter,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final MatchSort sort,
            final ZoneId zoneId,
            final int offset,
            final int limit) {
        return findPublicMatches(
                query,
                sports,
                timeFilter,
                null,
                null,
                minPrice,
                maxPrice,
                sort,
                zoneId,
                offset,
                limit);
    }

    int countPublicMatches(
            String query,
            List<Sport> sports,
            EventTimeFilter timeFilter,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ZoneId zoneId);

    default int countPublicMatches(
            final String query,
            final List<Sport> sports,
            final EventTimeFilter timeFilter,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final ZoneId zoneId) {
        return countPublicMatches(
                query, sports, timeFilter, null, null, minPrice, maxPrice, zoneId);
    }

    List<Match> findHostedMatches(
            Long hostUserId,
            Boolean upcoming,
            String query,
            List<Sport> sports,
            List<EventVisibility> visibility,
            List<EventStatus> statuses,
            EventTimeFilter timeFilter,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            MatchSort sort,
            ZoneId zoneId,
            int offset,
            int limit);

    default List<Match> findHostedMatches(
            final Long hostUserId,
            final Boolean upcoming,
            final String query,
            final List<Sport> sports,
            final List<EventVisibility> visibility,
            final List<EventStatus> statuses,
            final EventTimeFilter timeFilter,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final MatchSort sort,
            final ZoneId zoneId,
            final int offset,
            final int limit) {
        return findHostedMatches(
                hostUserId,
                upcoming,
                query,
                sports,
                visibility,
                statuses,
                timeFilter,
                null,
                null,
                minPrice,
                maxPrice,
                sort,
                zoneId,
                offset,
                limit);
    }

    int countHostedMatches(
            Long hostUserId,
            Boolean upcoming,
            String query,
            List<Sport> sports,
            List<EventVisibility> visibility,
            List<EventStatus> statuses,
            EventTimeFilter timeFilter,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ZoneId zoneId);

    default int countHostedMatches(
            final Long hostUserId,
            final Boolean upcoming,
            final String query,
            final List<Sport> sports,
            final List<EventVisibility> visibility,
            final List<EventStatus> statuses,
            final EventTimeFilter timeFilter,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final ZoneId zoneId) {
        return countHostedMatches(
                hostUserId,
                upcoming,
                query,
                sports,
                visibility,
                statuses,
                timeFilter,
                null,
                null,
                minPrice,
                maxPrice,
                zoneId);
    }

    List<Match> findJoinedMatches(
            Long userId,
            Boolean upcoming,
            String query,
            List<Sport> sports,
            List<EventVisibility> visibility,
            List<EventStatus> statuses,
            EventTimeFilter timeFilter,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            MatchSort sort,
            ZoneId zoneId,
            int offset,
            int limit);

    default List<Match> findJoinedMatches(
            final Long userId,
            final Boolean upcoming,
            final String query,
            final List<Sport> sports,
            final List<EventVisibility> visibility,
            final List<EventStatus> statuses,
            final EventTimeFilter timeFilter,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final MatchSort sort,
            final ZoneId zoneId,
            final int offset,
            final int limit) {
        return findJoinedMatches(
                userId,
                upcoming,
                query,
                sports,
                visibility,
                statuses,
                timeFilter,
                null,
                null,
                minPrice,
                maxPrice,
                sort,
                zoneId,
                offset,
                limit);
    }

    int countJoinedMatches(
            Long userId,
            Boolean upcoming,
            String query,
            List<Sport> sports,
            List<EventVisibility> visibility,
            List<EventStatus> statuses,
            EventTimeFilter timeFilter,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ZoneId zoneId);

    default int countJoinedMatches(
            final Long userId,
            final Boolean upcoming,
            final String query,
            final List<Sport> sports,
            final List<EventVisibility> visibility,
            final List<EventStatus> statuses,
            final EventTimeFilter timeFilter,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final ZoneId zoneId) {
        return countJoinedMatches(
                userId,
                upcoming,
                query,
                sports,
                visibility,
                statuses,
                timeFilter,
                null,
                null,
                minPrice,
                maxPrice,
                zoneId);
    }
}
