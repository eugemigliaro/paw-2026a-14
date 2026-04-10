package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EventTimeFilter;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
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
    public Optional<Match> findPublicMatchById(final Long matchId) {
        return matchDao.findPublicMatchById(matchId);
    }

    @Override
    public List<User> findConfirmedParticipants(final Long matchId) {
        return matchParticipantDao.findConfirmedParticipants(matchId);
    }

    @Override
    public PaginatedResult<Match> searchPublicMatches(
            final String query,
            final String sport,
            final String time,
            final String sort,
            final int page,
            final int pageSize,
            final String timezone) {
        final int safePage = page > 0 ? page : 1;
        final int safePageSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
        final int offset = (safePage - 1) * safePageSize;

        final List<Sport> sportFilters = parseSports(sport);
        final EventTimeFilter timeFilter = parseTimeFilter(time);
        final MatchSort sortFilter = parseSort(sort);
        final ZoneId zoneId = parseZone(timezone);

        final var items =
                matchDao.findPublicMatches(
                        query, sportFilters, timeFilter, sortFilter, zoneId, offset, safePageSize);
        final int totalCount = matchDao.countPublicMatches(query, sportFilters, timeFilter, zoneId);

        return new PaginatedResult<>(items, totalCount, safePage, safePageSize);
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

    private static EventTimeFilter parseTimeFilter(final String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return EventTimeFilter.ALL;
        }

        switch (rawTime.toLowerCase(Locale.ROOT)) {
            case "all":
                return EventTimeFilter.ALL;
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
}
