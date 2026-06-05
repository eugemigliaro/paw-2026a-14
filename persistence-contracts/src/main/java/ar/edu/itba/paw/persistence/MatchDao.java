package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSeries;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.query.EventTimeFilter;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.ParticipantStatus;
import ar.edu.itba.paw.models.types.RecurrenceFrequency;
import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MatchDao {

    default Match createMatch(
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
            ImageMetadata bannerImageMetadata) {
        return createMatch(
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
                null,
                null,
                null,
                null);
    }

    default Match createMatch(
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
            Double longitude) {
        return createMatch(
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
                null,
                null);
    }

    default Match createMatch(
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
            MatchSeries series,
            Integer seriesOccurrenceIndex) {
        return createMatch(
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
                null,
                null,
                series,
                seriesOccurrenceIndex);
    }

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

    Long createMatchSeries(
            User host,
            RecurrenceFrequency frequency,
            Instant startsAt,
            Instant endsAt,
            String timezone,
            LocalDate untilDate,
            Integer occurrenceCount);

    default boolean updateMatch(
            Long matchId,
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
            ImageMetadata bannerImageMetadata) {
        return updateMatch(
                matchId,
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
                null,
                null);
    }

    boolean updateMatch(
            Long matchId,
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
            Double longitude);

    boolean cancelMatch(Long matchId, User host);

    boolean softDeleteMatch(Long matchId, User deletedBy, String deleteReason);

    boolean restoreMatch(Long matchId);

    Optional<Match> findById(Long matchId);

    Optional<Match> findPublicMatchById(Long matchId);

    List<Match> findSeriesOccurrences(Long seriesId);

    PaginatedResult<Match> findSeriesOccurrencesPage(Long seriesId, int page, int pageSize);

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
            EventSort sort,
            Double latitude,
            Double longitude,
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
            EventSort sort,
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
                null,
                null,
                offset,
                limit);
    }

    default List<Match> findPublicMatches(
            final String query,
            final List<Sport> sports,
            final EventTimeFilter timeFilter,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final EventSort sort,
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
                null,
                null,
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
            BigDecimal maxPrice);

    default int countPublicMatches(
            final String query,
            final List<Sport> sports,
            final EventTimeFilter timeFilter,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        return countPublicMatches(query, sports, timeFilter, null, null, minPrice, maxPrice);
    }

    List<Match> findHostedMatches(
            User host,
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
            EventSort sort,
            int offset,
            int limit);

    default List<Match> findHostedMatches(
            final User host,
            final Boolean upcoming,
            final String query,
            final List<Sport> sports,
            final List<EventVisibility> visibility,
            final List<EventStatus> statuses,
            final EventTimeFilter timeFilter,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final EventSort sort,
            final int offset,
            final int limit) {
        return findHostedMatches(
                host,
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
                offset,
                limit);
    }

    int countHostedMatches(
            User host,
            Boolean upcoming,
            String query,
            List<Sport> sports,
            List<EventVisibility> visibility,
            List<EventStatus> statuses,
            EventTimeFilter timeFilter,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice);

    default int countHostedMatches(
            final User host,
            final Boolean upcoming,
            final String query,
            final List<Sport> sports,
            final List<EventVisibility> visibility,
            final List<EventStatus> statuses,
            final EventTimeFilter timeFilter,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        return countHostedMatches(
                host,
                upcoming,
                query,
                sports,
                visibility,
                statuses,
                timeFilter,
                null,
                null,
                minPrice,
                maxPrice);
    }

    List<Match> findJoinedMatches(
            User user,
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
            EventSort sort,
            int offset,
            int limit);

    default List<Match> findJoinedMatches(
            final User user,
            final Boolean upcoming,
            final String query,
            final List<Sport> sports,
            final List<EventVisibility> visibility,
            final List<EventStatus> statuses,
            final EventTimeFilter timeFilter,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final EventSort sort,
            final int offset,
            final int limit) {
        return findJoinedMatches(
                user,
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
                offset,
                limit);
    }

    int countJoinedMatches(
            User user,
            Boolean upcoming,
            String query,
            List<Sport> sports,
            List<EventVisibility> visibility,
            List<EventStatus> statuses,
            EventTimeFilter timeFilter,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice);

    default int countJoinedMatches(
            final User user,
            final Boolean upcoming,
            final String query,
            final List<Sport> sports,
            final List<EventVisibility> visibility,
            final List<EventStatus> statuses,
            final EventTimeFilter timeFilter,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        return countJoinedMatches(
                user,
                upcoming,
                query,
                sports,
                visibility,
                statuses,
                timeFilter,
                null,
                null,
                minPrice,
                maxPrice);
    }

    List<Match> findDashboardMatches(
            User user,
            Boolean upcoming,
            Boolean includeHosted,
            String query,
            List<Sport> sports,
            List<EventStatus> statuses,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            EventSort sort,
            List<ParticipantStatus> participantStatuses,
            int offset,
            int limit);

    int countDashboardMatches(
            User user,
            Boolean upcoming,
            Boolean includeHosted,
            String query,
            List<Sport> sports,
            List<EventStatus> statuses,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            EventSort sort,
            List<ParticipantStatus> participantStatuses);
}
