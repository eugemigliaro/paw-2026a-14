package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EventTimeFilter;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.MatchCancellationException;
import ar.edu.itba.paw.services.exceptions.MatchUpdateException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class MatchServiceImpl implements MatchService {

    private static final int DEFAULT_PAGE_SIZE = 12;

    private final MatchDao matchDao;
    private final MatchParticipantDao matchParticipantDao;
    private final MessageSource messageSource;
    private final Clock clock;

    @Autowired
    public MatchServiceImpl(
            final MatchDao matchDao,
            final MatchParticipantDao matchParticipantDao,
            final MessageSource messageSource,
            final Clock clock) {
        this.matchDao = matchDao;
        this.matchParticipantDao = matchParticipantDao;
        this.messageSource = messageSource;
        this.clock = clock;
    }

    @Override
    public Match createMatch(final CreateMatchRequest request) {
        validateScheduleOrThrow(
                request.getStartsAt(),
                request.getEndsAt(),
                new IllegalArgumentException(message("match.schedule.error.startsAtPast")),
                new IllegalArgumentException(message("match.schedule.error.endBeforeStart")));

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
    public Match updateMatch(
            final Long matchId, final Long actingUserId, final UpdateMatchRequest request) {
        final Match match =
                matchDao.findById(matchId)
                        .orElseThrow(
                                () ->
                                        new MatchUpdateException(
                                                MatchUpdateFailureReason.MATCH_NOT_FOUND,
                                                message("match.update.error.notFound")));

        if (!match.getHostUserId().equals(actingUserId)) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.FORBIDDEN, message("match.update.error.forbidden"));
        }

        validateScheduleOrThrow(
                request.getStartsAt(),
                request.getEndsAt(),
                new MatchUpdateException(
                        MatchUpdateFailureReason.INVALID_SCHEDULE,
                        message("match.schedule.error.startsAtPast")),
                new MatchUpdateException(
                        MatchUpdateFailureReason.INVALID_SCHEDULE,
                        message("match.schedule.error.endBeforeStart")));

        final int confirmedParticipants =
                matchParticipantDao.findConfirmedParticipants(matchId).size();
        if (request.getMaxPlayers() < confirmedParticipants) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.CAPACITY_BELOW_CONFIRMED,
                    message("match.update.error.capacityBelowConfirmed"));
        }

        final boolean updated =
                matchDao.updateMatch(
                        matchId,
                        actingUserId,
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

        if (!updated) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.FORBIDDEN, message("match.update.error.forbidden"));
        }

        return matchDao.findById(matchId)
                .orElseThrow(
                        () ->
                                new MatchUpdateException(
                                        MatchUpdateFailureReason.MATCH_NOT_FOUND,
                                        message("match.update.error.notFound")));
    }

    @Override
    public Match cancelMatch(final Long matchId, final Long actingUserId) {
        final Match match =
                matchDao.findById(matchId)
                        .orElseThrow(
                                () ->
                                        new MatchCancellationException(
                                                MatchCancellationFailureReason.MATCH_NOT_FOUND,
                                                message("match.cancel.error.notFound")));

        if (!match.getHostUserId().equals(actingUserId)) {
            throw new MatchCancellationException(
                    MatchCancellationFailureReason.FORBIDDEN,
                    message("match.cancel.error.forbidden"));
        }

        if ("cancelled".equals(match.getStatus())) {
            return match;
        }

        final boolean updated = matchDao.cancelMatch(matchId, actingUserId);
        if (!updated) {
            throw new MatchCancellationException(
                    MatchCancellationFailureReason.FORBIDDEN,
                    message("match.cancel.error.forbidden"));
        }

        return matchDao.findById(matchId)
                .orElseThrow(
                        () ->
                                new MatchCancellationException(
                                        MatchCancellationFailureReason.MATCH_NOT_FOUND,
                                        message("match.cancel.error.notFound")));
    }

    @Override
    public Optional<Match> findMatchById(final Long matchId) {
        return matchDao.findById(matchId);
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
            final String timezone,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        final int safePage = page > 0 ? page : 1;
        final int safePageSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;

        final List<Sport> sportFilters = parseSports(sport);
        final EventTimeFilter timeFilter = parseTimeFilter(time);
        final MatchSort sortFilter = parseSort(sort);
        final ZoneId zoneId = parseZone(timezone);

        final int totalCount =
                matchDao.countPublicMatches(
                        query, sportFilters, timeFilter, minPrice, maxPrice, zoneId);
        final int totalPages = Math.max(1, (totalCount + safePageSize - 1) / safePageSize);
        final int clampedPage = Math.min(safePage, totalPages);
        final int offset = (clampedPage - 1) * safePageSize;
        final var items =
                matchDao.findPublicMatches(
                        query,
                        sportFilters,
                        timeFilter,
                        minPrice,
                        maxPrice,
                        sortFilter,
                        zoneId,
                        offset,
                        safePageSize);

        return new PaginatedResult<>(items, totalCount, clampedPage, safePageSize);
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

    private String message(final String code) {
        final Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, null, code, locale);
    }

    private void validateScheduleOrThrow(
            final Instant startsAt,
            final Instant endsAt,
            final RuntimeException startsAtException,
            final RuntimeException endsAtException) {
        if (startsAt != null && !startsAt.isAfter(Instant.now(clock))) {
            throw startsAtException;
        }

        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw endsAtException;
        }
    }
}
