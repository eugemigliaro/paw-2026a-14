package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.EventTimeFilter;
import ar.edu.itba.paw.models.EventVisibility;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MatchServiceImpl implements MatchService {

    private static final int DEFAULT_PAGE_SIZE = 12;

    private final MatchDao matchDao;
    private final MatchParticipantDao matchParticipantDao;

    @Autowired
    public MatchServiceImpl(
            final MatchDao matchDao, final MatchParticipantDao matchParticipantDao) {
        this.matchDao = matchDao;
        this.matchParticipantDao = matchParticipantDao;
    }

    @Override
    public Match createMatch(final CreateMatchRequest request) {
        return matchDao.createMatch(
                request.getHostUserId(),
                request.getAddress(),
                request.getTitle(),
                request.getDescription(),
                request.getStartsAt(),
                request.getEndsAt(),
                request.getMaxPlayers(),
                request.getPricePerPlayer(),
                request.getSport(),
                request.getVisibility(),
                request.getJoinPolicy(),
                request.getStatus(),
                request.getBannerImageId());
    }

    @Override
    public Optional<Match> findMatchById(final Long matchId) {
        return matchDao.findMatchById(matchId);
    }

    @Override
    public List<User> findConfirmedParticipants(final Long matchId) {
        return matchParticipantDao.findConfirmedParticipants(matchId);
    }

    @Override
    public PaginatedResult<Match> findHostedMatches(
            final Long hostUserId,
            final Boolean upcoming,
            final String query,
            final String sport,
            final String visibility,
            final String status,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String sort,
            final String timezone,
            final int page,
            final int pageSize) {
        final List<Sport> sportFilters = parseSports(sport);
        final List<EventVisibility> visibilityFilters = parseVisibility(visibility);
        final List<EventStatus> statusFilters = parseStatuses(status);
        final ZoneId zoneId = parseZone(timezone);
        final DateRange dateRange = parseDateRange(startDate, endDate, zoneId);

        return paginate(
                page,
                pageSize,
                DEFAULT_PAGE_SIZE,
                safePageSize ->
                        matchDao.countHostedMatches(
                                hostUserId,
                                upcoming,
                                query,
                                sportFilters,
                                visibilityFilters,
                                statusFilters,
                                EventTimeFilter.ALL,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice,
                                zoneId),
                (offset, safePageSize) ->
                        matchDao.findHostedMatches(
                                hostUserId,
                                upcoming,
                                query,
                                sportFilters,
                                visibilityFilters,
                                statusFilters,
                                EventTimeFilter.ALL,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice,
                                parseSort(sort),
                                zoneId,
                                offset,
                                safePageSize));
    }

    @Override
    public PaginatedResult<Match> findJoinedMatches(
            final Long userId,
            final Boolean upcoming,
            final String query,
            final String sport,
            final String visibility,
            final String status,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String sort,
            final String timezone,
            final int page,
            final int pageSize) {
        final List<Sport> sportFilters = parseSports(sport);
        final List<EventVisibility> visibilityFilters = parseVisibility(visibility);
        final List<EventStatus> statusFilters = parseStatuses(status);
        final ZoneId zoneId = parseZone(timezone);
        final DateRange dateRange = parseDateRange(startDate, endDate, zoneId);

        return paginate(
                page,
                pageSize,
                DEFAULT_PAGE_SIZE,
                safePageSize ->
                        matchDao.countJoinedMatches(
                                userId,
                                upcoming,
                                query,
                                sportFilters,
                                visibilityFilters,
                                statusFilters,
                                EventTimeFilter.ALL,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice,
                                zoneId),
                (offset, safePageSize) ->
                        matchDao.findJoinedMatches(
                                userId,
                                upcoming,
                                query,
                                sportFilters,
                                visibilityFilters,
                                statusFilters,
                                EventTimeFilter.ALL,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice,
                                parseSort(sort),
                                zoneId,
                                offset,
                                safePageSize));
    }

    @Override
    public PaginatedResult<Match> searchPublicMatches(
            final String query,
            final String sport,
            final String startDate,
            final String endDate,
            final String sort,
            final int page,
            final int pageSize,
            final String timezone,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        final List<Sport> sportFilters = parseSports(sport);
        final MatchSort sortFilter = parseSort(sort);
        final ZoneId zoneId = parseZone(timezone);
        final DateRange dateRange = parseDateRange(startDate, endDate, zoneId);

        return paginate(
                page,
                pageSize,
                DEFAULT_PAGE_SIZE,
                safePageSize ->
                        matchDao.countPublicMatches(
                                query,
                                sportFilters,
                                EventTimeFilter.ALL,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice,
                                zoneId),
                (offset, safePageSize) ->
                        matchDao.findPublicMatches(
                                query,
                                sportFilters,
                                EventTimeFilter.ALL,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice,
                                sortFilter,
                                zoneId,
                                offset,
                                safePageSize));
    }

    private static List<Sport> parseSports(final String rawSports) {
        if (rawSports == null || rawSports.isBlank()) {
            return List.of();
        }

        final LinkedHashSet<Sport> sports = new LinkedHashSet<>();
        for (final String rawSport : rawSports.split(",")) {
            if (rawSport == null || rawSport.isBlank()) {
                continue;
            }
            Sport.fromDbValue(rawSport.trim()).ifPresent(sports::add);
        }

        return List.copyOf(sports);
    }

    private static List<EventStatus> parseStatuses(final String rawStatuses) {
        if (rawStatuses == null || rawStatuses.isBlank()) {
            return List.of();
        }

        final LinkedHashSet<EventStatus> statuses = new LinkedHashSet<>();
        for (final String rawStatus : rawStatuses.split(",")) {
            if (rawStatus == null || rawStatus.isBlank()) {
                continue;
            }
            EventStatus.fromDbValue(rawStatus.trim()).ifPresent(statuses::add);
        }

        return List.copyOf(statuses);
    }

    private static List<EventVisibility> parseVisibility(final String rawVisibility) {
        if (rawVisibility == null || rawVisibility.isBlank()) {
            return List.of();
        }

        final LinkedHashSet<EventVisibility> visibility = new LinkedHashSet<>();
        for (final String rawValue : rawVisibility.split(",")) {
            if (rawValue == null || rawValue.isBlank()) {
                continue;
            }
            EventVisibility.fromDbValue(rawValue.trim()).ifPresent(visibility::add);
        }

        return List.copyOf(visibility);
    }

    private static DateRange parseDateRange(
            final String rawStartDate, final String rawEndDate, final ZoneId zoneId) {
        final LocalDate startDate = parseDate(rawStartDate);
        final LocalDate endDate = parseDate(rawEndDate);

        if (startDate == null && endDate == null) {
            return new DateRange(null, null);
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            final Instant start = endDate.atStartOfDay(zoneId).toInstant();
            final Instant endExclusive = startDate.plusDays(1).atStartOfDay(zoneId).toInstant();
            return new DateRange(start, endExclusive);
        }

        final Instant start = startDate == null ? null : startDate.atStartOfDay(zoneId).toInstant();
        final Instant endExclusive =
                endDate == null ? null : endDate.plusDays(1).atStartOfDay(zoneId).toInstant();
        return new DateRange(start, endExclusive);
    }

    private static LocalDate parseDate(final String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(rawDate.trim());
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static MatchSort parseSort(final String rawSort) {
        if (rawSort == null || rawSort.isBlank()) {
            return MatchSort.SOONEST;
        }
        return MatchSort.fromQueryValue(rawSort).orElse(MatchSort.SOONEST);
    }

    private static ZoneId parseZone(final String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.systemDefault();
        }

        try {
            return ZoneId.of(timezone);
        } catch (final Exception ignored) {
            return ZoneId.systemDefault();
        }
    }

    private PaginatedResult<Match> paginate(
            final int page,
            final int pageSize,
            final int defaultPageSize,
            final CountSupplier countSupplier,
            final SliceSupplier sliceSupplier) {
        final int safePage = page > 0 ? page : 1;
        final int safePageSize = pageSize > 0 ? pageSize : defaultPageSize;

        final int totalCount = countSupplier.count(safePageSize);
        final int totalPages = Math.max(1, (totalCount + safePageSize - 1) / safePageSize);
        final int clampedPage = Math.min(safePage, totalPages);
        final int offset = (clampedPage - 1) * safePageSize;
        final List<Match> items = sliceSupplier.items(offset, safePageSize);

        return new PaginatedResult<>(items, totalCount, clampedPage, safePageSize);
    }

    @FunctionalInterface
    private interface CountSupplier {
        int count(int safePageSize);
    }

    @FunctionalInterface
    private interface SliceSupplier {
        List<Match> items(int offset, int safePageSize);
    }

    private record DateRange(Instant start, Instant endExclusive) {}
}
