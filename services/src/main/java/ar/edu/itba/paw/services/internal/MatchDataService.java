package ar.edu.itba.paw.services.internal;

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
import java.time.ZoneId;
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
            Double longitude);

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

    Optional<Match> findById(Long matchId);

    Optional<Match> findPublicMatchById(Long matchId);

    List<Match> findSeriesOccurrences(Long seriesId);

    PaginatedResult<Match> findSeriesOccurrencesPage(Long seriesId, int page, int pageSize);

    List<Match> findPublicMatches(
            String query,
            List<Sport> sports,
            EventTimeFilter timeFilter,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            EventSort sort,
            ZoneId zoneId,
            Double latitude,
            Double longitude,
            int offset,
            int limit);

    int countPublicMatches(
            String query,
            List<Sport> sports,
            EventTimeFilter timeFilter,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ZoneId zoneId);

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
            ZoneId zoneId,
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
            ZoneId zoneId,
            List<ParticipantStatus> participantStatuses);

    boolean softDeleteMatch(Long matchId, User deletedBy, String deleteReason);

    boolean restoreMatch(Long matchId);
}
