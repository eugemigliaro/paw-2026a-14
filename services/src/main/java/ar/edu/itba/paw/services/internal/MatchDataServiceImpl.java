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
import ar.edu.itba.paw.persistence.MatchDao;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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
            final Double longitude) {
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
                longitude);
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
    public Long createMatchSeries(
            final User host,
            final RecurrenceFrequency frequency,
            final Instant startsAt,
            final Instant endsAt,
            final String timezone,
            final LocalDate untilDate,
            final Integer occurrenceCount) {
        return matchDao.createMatchSeries(
                host, frequency, startsAt, endsAt, timezone, untilDate, occurrenceCount);
    }

    @Override
    public boolean updateMatch(
            final Long matchId,
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
            final Double longitude) {
        return matchDao.updateMatch(
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
                latitude,
                longitude);
    }

    @Override
    public boolean cancelMatch(final Long matchId, final User host) {
        return matchDao.cancelMatch(matchId, host);
    }

    @Override
    public Optional<Match> findById(final Long matchId) {
        return matchDao.findById(matchId);
    }

    @Override
    public Optional<Match> findPublicMatchById(final Long matchId) {
        return matchDao.findPublicMatchById(matchId);
    }

    @Override
    public List<Match> findSeriesOccurrences(final Long seriesId) {
        return matchDao.findSeriesOccurrences(seriesId);
    }

    @Override
    public PaginatedResult<Match> findSeriesOccurrencesPage(
            final Long seriesId, final int page, final int pageSize) {
        return matchDao.findSeriesOccurrencesPage(seriesId, page, pageSize);
    }

    @Override
    public List<Match> findPublicMatches(
            final String query,
            final List<Sport> sports,
            final EventTimeFilter timeFilter,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final EventSort sort,
            final ZoneId zoneId,
            final Double latitude,
            final Double longitude,
            final int offset,
            final int limit) {
        return matchDao.findPublicMatches(
                query,
                sports,
                timeFilter,
                startsAtFrom,
                startsAtTo,
                minPrice,
                maxPrice,
                sort,
                zoneId,
                latitude,
                longitude,
                offset,
                limit);
    }

    @Override
    public int countPublicMatches(
            final String query,
            final List<Sport> sports,
            final EventTimeFilter timeFilter,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final ZoneId zoneId) {
        return matchDao.countPublicMatches(
                query, sports, timeFilter, startsAtFrom, startsAtTo, minPrice, maxPrice, zoneId);
    }

    @Override
    public List<Match> findDashboardMatches(
            final User user,
            final Boolean upcoming,
            final Boolean includeHosted,
            final String query,
            final List<Sport> sports,
            final List<EventStatus> statuses,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final EventSort sort,
            final ZoneId zoneId,
            final List<ParticipantStatus> participantStatuses,
            final int offset,
            final int limit) {
        return matchDao.findDashboardMatches(
                user,
                upcoming,
                includeHosted,
                query,
                sports,
                statuses,
                startsAtFrom,
                startsAtTo,
                minPrice,
                maxPrice,
                sort,
                zoneId,
                participantStatuses,
                offset,
                limit);
    }

    @Override
    public int countDashboardMatches(
            final User user,
            final Boolean upcoming,
            final Boolean includeHosted,
            final String query,
            final List<Sport> sports,
            final List<EventStatus> statuses,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final EventSort sort,
            final ZoneId zoneId,
            final List<ParticipantStatus> participantStatuses) {
        return matchDao.countDashboardMatches(
                user,
                upcoming,
                includeHosted,
                query,
                sports,
                statuses,
                startsAtFrom,
                startsAtTo,
                minPrice,
                maxPrice,
                sort,
                zoneId,
                participantStatuses);
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
