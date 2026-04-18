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
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
            final String time,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String sort,
            final String timezone,
            final int page,
            final int pageSize) {
        final List<Sport> sportFilters = parseSports(sport);
        final List<EventVisibility> visibilityFilters = parseVisibility(visibility);
        final List<EventStatus> statusFilters = parseStatuses(status);
        final EventTimeFilter timeFilter = parseTimeFilter(time);
        final ZoneId zoneId = parseZone(timezone);

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
                                timeFilter,
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
                                timeFilter,
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
            final String time,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String sort,
            final String timezone,
            final int page,
            final int pageSize) {
        final List<Sport> sportFilters = parseSports(sport);
        final List<EventVisibility> visibilityFilters = parseVisibility(visibility);
        final List<EventStatus> statusFilters = parseStatuses(status);
        final EventTimeFilter timeFilter = parseTimeFilter(time);
        final ZoneId zoneId = parseZone(timezone);

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
                                timeFilter,
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
                                timeFilter,
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
            final String time,
            final String sort,
            final int page,
            final int pageSize,
            final String timezone,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        final List<Sport> sportFilters = parseSports(sport);
        final EventTimeFilter timeFilter = parseTimeFilter(time);
        final MatchSort sortFilter = parseSort(sort);
        final ZoneId zoneId = parseZone(timezone);

        return paginate(
                page,
                pageSize,
                DEFAULT_PAGE_SIZE,
                safePageSize ->
                        matchDao.countPublicMatches(
                                query, sportFilters, timeFilter, minPrice, maxPrice, zoneId),
                (offset, safePageSize) ->
                        matchDao.findPublicMatches(
                                query,
                                sportFilters,
                                timeFilter,
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

    private static EventTimeFilter parseTimeFilter(final String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return EventTimeFilter.ALL;
        }

        switch (rawTime.toLowerCase(Locale.ROOT)) {
            case "all":
                return EventTimeFilter.ALL;
            case "future":
                return EventTimeFilter.FUTURE;
            case "today":
                return EventTimeFilter.TODAY;
            case "tomorrow":
                return EventTimeFilter.TOMORROW;
            case "week":
                return EventTimeFilter.WEEK;
            default:
                return EventTimeFilter.ALL;
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
}
