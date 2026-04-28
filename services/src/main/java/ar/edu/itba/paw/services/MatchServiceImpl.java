package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.EventTimeFilter;
import ar.edu.itba.paw.models.EventVisibility;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.RecurrenceEndMode;
import ar.edu.itba.paw.models.RecurrenceFrequency;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.MatchCancellationException;
import ar.edu.itba.paw.services.exceptions.MatchUpdateException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchServiceImpl implements MatchService {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MIN_RECURRING_OCCURRENCES = 2;
    private static final int MAX_RECURRING_OCCURRENCES = 52;

    private final MatchDao matchDao;
    private final MatchParticipantDao matchParticipantDao;
    private final MatchNotificationService matchNotificationService;
    private final MessageSource messageSource;
    private final Clock clock;

    @Autowired
    public MatchServiceImpl(
            final MatchDao matchDao,
            final MatchParticipantDao matchParticipantDao,
            final MatchNotificationService matchNotificationService,
            final MessageSource messageSource,
            final Clock clock) {
        this.matchDao = matchDao;
        this.matchParticipantDao = matchParticipantDao;
        this.matchNotificationService = matchNotificationService;
        this.messageSource = messageSource;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Match createMatch(final CreateMatchRequest request) {
        validateScheduleOrThrow(
                request.getStartsAt(),
                request.getEndsAt(),
                new IllegalArgumentException(message("match.schedule.error.startsAtPast")),
                new IllegalArgumentException(message("match.schedule.error.endBeforeStart")));

        if (request.isRecurring()) {
            return createRecurringMatch(request);
        }

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

    private Match createRecurringMatch(final CreateMatchRequest request) {
        final CreateRecurrenceRequest recurrence = request.getRecurrence();
        final List<OccurrenceWindow> occurrences = buildOccurrenceWindows(request, recurrence);
        final Long seriesId =
                matchDao.createMatchSeries(
                        request.getHostUserId(),
                        recurrence.getFrequency().getValue(),
                        request.getStartsAt(),
                        request.getEndsAt(),
                        resolveZone(recurrence.getZoneId()).getId(),
                        recurrence.getEndMode() == RecurrenceEndMode.UNTIL_DATE
                                ? recurrence.getUntilDate()
                                : null,
                        recurrence.getEndMode() == RecurrenceEndMode.OCCURRENCE_COUNT
                                ? recurrence.getOccurrenceCount()
                                : null);

        Match firstOccurrence = null;
        for (int i = 0; i < occurrences.size(); i++) {
            final OccurrenceWindow occurrence = occurrences.get(i);
            final Match created =
                    matchDao.createMatch(
                            request.getHostUserId(),
                            request.getAddress(),
                            request.getTitle(),
                            request.getDescription(),
                            occurrence.startsAt(),
                            occurrence.endsAt(),
                            request.getMaxPlayers(),
                            request.getPricePerPlayer(),
                            request.getSport(),
                            request.getVisibility(),
                            request.getJoinPolicy(),
                            request.getStatus(),
                            request.getBannerImageId(),
                            seriesId,
                            i + 1);
            if (firstOccurrence == null) {
                firstOccurrence = created;
            }
        }

        return firstOccurrence;
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

        if (EventStatus.CANCELLED.getValue().equalsIgnoreCase(match.getStatus())
                || EventStatus.COMPLETED.getValue().equalsIgnoreCase(match.getStatus())) {
            throw new MatchUpdateException(
                    MatchUpdateFailureReason.NOT_EDITABLE,
                    message("match.update.error.notEditable"));
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

        final Match updatedMatch =
                matchDao.findById(matchId)
                        .orElseThrow(
                                () ->
                                        new MatchUpdateException(
                                                MatchUpdateFailureReason.MATCH_NOT_FOUND,
                                                message("match.update.error.notFound")));
        matchNotificationService.notifyMatchUpdated(updatedMatch);
        return updatedMatch;
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

        if (EventStatus.COMPLETED.getValue().equalsIgnoreCase(match.getStatus())) {
            throw new MatchCancellationException(
                    MatchCancellationFailureReason.FORBIDDEN,
                    message("match.cancel.error.forbidden"));
        }

        if (EventStatus.CANCELLED.getValue().equalsIgnoreCase(match.getStatus())) {
            return match;
        }

        final boolean updated = matchDao.cancelMatch(matchId, actingUserId);
        if (!updated) {
            throw new MatchCancellationException(
                    MatchCancellationFailureReason.FORBIDDEN,
                    message("match.cancel.error.forbidden"));
        }

        final Match cancelledMatch =
                matchDao.findById(matchId)
                        .orElseThrow(
                                () ->
                                        new MatchCancellationException(
                                                MatchCancellationFailureReason.MATCH_NOT_FOUND,
                                                message("match.cancel.error.notFound")));
        matchNotificationService.notifyMatchCancelled(cancelledMatch);
        return cancelledMatch;
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
    public List<Match> findSeriesOccurrences(final Long seriesId) {
        if (seriesId == null) {
            return List.of();
        }
        return matchDao.findSeriesOccurrences(seriesId);
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

    private List<OccurrenceWindow> buildOccurrenceWindows(
            final CreateMatchRequest request, final CreateRecurrenceRequest recurrence) {
        if (recurrence == null
                || recurrence.getFrequency() == null
                || recurrence.getEndMode() == null) {
            throw new IllegalArgumentException(message("match.recurrence.error.invalid"));
        }

        final ZoneId zoneId = resolveZone(recurrence.getZoneId());
        final LocalDateTime firstLocalStart =
                LocalDateTime.ofInstant(request.getStartsAt(), zoneId);
        final Duration duration =
                request.getEndsAt() == null
                        ? null
                        : Duration.between(request.getStartsAt(), request.getEndsAt());
        final int occurrenceCount = resolveOccurrenceCount(firstLocalStart, recurrence);

        return java.util.stream.IntStream.range(0, occurrenceCount)
                .mapToObj(
                        index -> {
                            final LocalDateTime occurrenceLocalStart =
                                    addFrequency(firstLocalStart, recurrence.getFrequency(), index);
                            final Instant startsAt =
                                    occurrenceLocalStart.atZone(zoneId).toInstant();
                            final Instant endsAt =
                                    duration == null ? null : startsAt.plus(duration);
                            return new OccurrenceWindow(startsAt, endsAt);
                        })
                .toList();
    }

    private int resolveOccurrenceCount(
            final LocalDateTime firstLocalStart, final CreateRecurrenceRequest recurrence) {
        switch (recurrence.getEndMode()) {
            case OCCURRENCE_COUNT:
                return validateOccurrenceCount(recurrence.getOccurrenceCount());
            case UNTIL_DATE:
                return countOccurrencesUntilDate(firstLocalStart, recurrence);
            default:
                throw new IllegalArgumentException(message("match.recurrence.error.invalid"));
        }
    }

    private int validateOccurrenceCount(final Integer occurrenceCount) {
        if (occurrenceCount == null || occurrenceCount < MIN_RECURRING_OCCURRENCES) {
            throw new IllegalArgumentException(message("match.recurrence.error.tooFewOccurrences"));
        }
        if (occurrenceCount > MAX_RECURRING_OCCURRENCES) {
            throw new IllegalArgumentException(
                    message("match.recurrence.error.tooManyOccurrences"));
        }
        return occurrenceCount;
    }

    private int countOccurrencesUntilDate(
            final LocalDateTime firstLocalStart, final CreateRecurrenceRequest recurrence) {
        final LocalDate untilDate = recurrence.getUntilDate();
        if (untilDate == null || !untilDate.isAfter(firstLocalStart.toLocalDate())) {
            throw new IllegalArgumentException(message("match.recurrence.error.untilDate"));
        }

        int count = 0;
        LocalDateTime occurrenceStart = firstLocalStart;
        while (!occurrenceStart.toLocalDate().isAfter(untilDate)) {
            count++;
            if (count > MAX_RECURRING_OCCURRENCES) {
                throw new IllegalArgumentException(
                        message("match.recurrence.error.tooManyOccurrences"));
            }
            occurrenceStart = addFrequency(firstLocalStart, recurrence.getFrequency(), count);
        }

        if (count < MIN_RECURRING_OCCURRENCES) {
            throw new IllegalArgumentException(message("match.recurrence.error.tooFewOccurrences"));
        }
        return count;
    }

    private static LocalDateTime addFrequency(
            final LocalDateTime firstLocalStart,
            final RecurrenceFrequency frequency,
            final int index) {
        switch (frequency) {
            case DAILY:
                return firstLocalStart.plusDays(index);
            case MONTHLY:
                return firstLocalStart.plusMonths(index);
            case WEEKLY:
            default:
                return firstLocalStart.plusWeeks(index);
        }
    }

    private static ZoneId resolveZone(final ZoneId zoneId) {
        return zoneId == null ? ZoneId.systemDefault() : zoneId;
    }

    private String message(final String code) {
        final Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(
                Objects.requireNonNull(code), null, code, Objects.requireNonNull(locale));
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

    private record OccurrenceWindow(Instant startsAt, Instant endsAt) {}
}
