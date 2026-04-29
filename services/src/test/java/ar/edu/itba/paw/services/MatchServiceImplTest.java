package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.EventTimeFilter;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
public class MatchServiceImplTest {

    @InjectMocks private MatchServiceImpl matchService;

    @Mock private MatchDao matchDao;
    @Mock private MatchParticipantDao matchParticipantDao;
    @Mock private MatchNotificationService matchNotificationService;
    @Mock private MessageSource messageSource;
    @Mock private Clock clock;

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T00:00:00Z");

    @BeforeEach
    public void setUp() {
        Mockito.lenient().when(clock.instant()).thenReturn(FIXED_NOW);
        Mockito.lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        Mockito.lenient()
                .when(
                        messageSource.getMessage(
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));
    }

    @Test
    public void testSearchPublicMatchesWithValidInputs() {
        final Match match = createTestMatch(1L, "Football", "football");
        final Instant expectedStart = Instant.parse("2026-04-10T00:00:00Z");
        final Instant expectedEndExclusive = Instant.parse("2026-04-17T00:00:00Z");
        Mockito.when(
                        matchDao.findPublicMatches(
                                "football",
                                List.of(Sport.FOOTBALL),
                                EventTimeFilter.ALL,
                                expectedStart,
                                expectedEndExclusive,
                                null,
                                null,
                                MatchSort.SOONEST,
                                ZoneId.of("UTC"),
                                10,
                                10))
                .thenReturn(List.of(match));
        Mockito.when(
                        matchDao.countPublicMatches(
                                "football",
                                List.of(Sport.FOOTBALL),
                                EventTimeFilter.ALL,
                                expectedStart,
                                expectedEndExclusive,
                                null,
                                null,
                                ZoneId.of("UTC")))
                .thenReturn(25);

        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        "football",
                        "football",
                        "2026-04-10",
                        "2026-04-16",
                        "soonest",
                        2,
                        10,
                        "UTC",
                        null,
                        null);

        Assertions.assertEquals(1, result.getItems().size());
        Assertions.assertEquals("Football", result.getItems().get(0).getTitle());
        Assertions.assertEquals(25, result.getTotalCount());
        Assertions.assertEquals(3, result.getTotalPages());
        Assertions.assertEquals(2, result.getPage());
    }

    @Test
    public void testSearchPublicMatchesWithNullQuery() {
        final Match match = createTestMatch(1L, "Padel", "padel");
        Mockito.when(
                        matchDao.findPublicMatches(
                                null,
                                List.of(Sport.PADEL),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                null,
                                null,
                                MatchSort.SOONEST,
                                ZoneId.of("UTC"),
                                0,
                                12))
                .thenReturn(List.of(match));
        Mockito.when(
                        matchDao.countPublicMatches(
                                null,
                                List.of(Sport.PADEL),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                null,
                                null,
                                ZoneId.of("UTC")))
                .thenReturn(1);

        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        null, "padel", null, null, null, 1, 0, "UTC", null, null);

        Assertions.assertEquals(1, result.getItems().size());
        Assertions.assertEquals("Padel", result.getItems().get(0).getTitle());
        Assertions.assertEquals(1, result.getTotalPages());
    }

    @Test
    public void testSearchPublicMatchesWithMultipleSports() {
        final Match footballMatch = createTestMatch(1L, "Football", "football");
        final Match tennisMatch = createTestMatch(2L, "Tennis", "tennis");
        Mockito.when(
                        matchDao.findPublicMatches(
                                null,
                                List.of(Sport.FOOTBALL, Sport.TENNIS),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                null,
                                null,
                                MatchSort.SOONEST,
                                ZoneId.of("UTC"),
                                0,
                                12))
                .thenReturn(List.of(footballMatch, tennisMatch));
        Mockito.when(
                        matchDao.countPublicMatches(
                                null,
                                List.of(Sport.FOOTBALL, Sport.TENNIS),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                null,
                                null,
                                ZoneId.of("UTC")))
                .thenReturn(2);

        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        null,
                        "football,tennis,invalid,football",
                        null,
                        null,
                        null,
                        1,
                        12,
                        "UTC",
                        null,
                        null);

        Assertions.assertEquals(2, result.getItems().size());
        Assertions.assertEquals(2, result.getTotalCount());
        Assertions.assertEquals(1, result.getTotalPages());
    }

    @Test
    public void testSearchPublicMatchesForwardsPriceFilters() {
        final Match match = createTestMatch(3L, "Premium Padel", "padel");
        final BigDecimal minPrice = new BigDecimal("10");
        final BigDecimal maxPrice = new BigDecimal("25");
        Mockito.when(
                        matchDao.findPublicMatches(
                                null,
                                List.of(Sport.PADEL),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                minPrice,
                                maxPrice,
                                MatchSort.PRICE_LOW,
                                ZoneId.of("UTC"),
                                0,
                                12))
                .thenReturn(List.of(match));
        Mockito.when(
                        matchDao.countPublicMatches(
                                null,
                                List.of(Sport.PADEL),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                minPrice,
                                maxPrice,
                                ZoneId.of("UTC")))
                .thenReturn(1);

        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        null, "padel", null, null, "price", 1, 12, "UTC", minPrice, maxPrice);

        Assertions.assertEquals(1, result.getItems().size());
        Assertions.assertEquals("Premium Padel", result.getItems().get(0).getTitle());
    }

    @Test
    public void testSearchPublicMatchesClampsOutOfRangePageToLastAvailablePage() {
        final Match match = createTestMatch(4L, "Last Page Match", "padel");
        Mockito.when(
                        matchDao.countPublicMatches(
                                null,
                                List.of(Sport.PADEL),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                null,
                                null,
                                ZoneId.of("UTC")))
                .thenReturn(13);
        Mockito.when(
                        matchDao.findPublicMatches(
                                null,
                                List.of(Sport.PADEL),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                null,
                                null,
                                MatchSort.SOONEST,
                                ZoneId.of("UTC"),
                                12,
                                12))
                .thenReturn(List.of(match));

        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        null, "padel", null, null, null, 99, 12, "UTC", null, null);

        Assertions.assertEquals(2, result.getPage());
        Assertions.assertEquals(2, result.getTotalPages());
        Assertions.assertEquals(1, result.getItems().size());
        Assertions.assertEquals("Last Page Match", result.getItems().get(0).getTitle());
    }

    @Test
    public void testCreateMatchDelegates() {
        final Instant now = FIXED_NOW.plusSeconds(3600);
        final Match expectedMatch = createTestMatch(1L, "Test Match", "football");
        Mockito.when(
                        matchDao.createMatch(
                                1L,
                                "Test Address",
                                "Test Match",
                                "Test Description",
                                now,
                                null,
                                10,
                                BigDecimal.ZERO,
                                Sport.FOOTBALL,
                                "public",
                                "direct",
                                "open",
                                null))
                .thenReturn(expectedMatch);

        final Match result =
                matchService.createMatch(
                        new CreateMatchRequest(
                                1L,
                                "Test Address",
                                "Test Match",
                                "Test Description",
                                now,
                                null,
                                10,
                                BigDecimal.ZERO,
                                Sport.FOOTBALL,
                                "public",
                                "open",
                                null));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1L, result.getId());
        Assertions.assertEquals("Test Match", result.getTitle());
    }

    @Test
    public void testCreateRecurringMatchGeneratesWeeklyOccurrences() {
        // 1. Arrange
        final Instant startsAt = Instant.parse("2026-04-10T18:00:00Z");
        final Instant endsAt = Instant.parse("2026-04-10T19:30:00Z");
        final Match firstOccurrence =
                new Match(
                        101L,
                        Sport.PADEL,
                        1L,
                        "Test Address",
                        "Weekly Padel",
                        "Test Description",
                        startsAt,
                        endsAt,
                        8,
                        BigDecimal.ZERO,
                        "public",
                        "direct",
                        "open",
                        0,
                        null,
                        77L,
                        1);
        final Match secondOccurrence =
                new Match(
                        102L,
                        Sport.PADEL,
                        1L,
                        "Test Address",
                        "Weekly Padel",
                        "Test Description",
                        Instant.parse("2026-04-17T18:00:00Z"),
                        Instant.parse("2026-04-17T19:30:00Z"),
                        8,
                        BigDecimal.ZERO,
                        "public",
                        "direct",
                        "open",
                        0,
                        null,
                        77L,
                        2);
        final Match thirdOccurrence =
                new Match(
                        103L,
                        Sport.PADEL,
                        1L,
                        "Test Address",
                        "Weekly Padel",
                        "Test Description",
                        Instant.parse("2026-04-24T18:00:00Z"),
                        Instant.parse("2026-04-24T19:30:00Z"),
                        8,
                        BigDecimal.ZERO,
                        "public",
                        "direct",
                        "open",
                        0,
                        null,
                        77L,
                        3);
        Mockito.when(matchDao.createMatchSeries(1L, "weekly", startsAt, endsAt, "UTC", null, 3))
                .thenReturn(77L);
        Mockito.when(
                        matchDao.createMatch(
                                1L,
                                "Test Address",
                                "Weekly Padel",
                                "Test Description",
                                firstOccurrence.getStartsAt(),
                                firstOccurrence.getEndsAt(),
                                8,
                                BigDecimal.ZERO,
                                Sport.PADEL,
                                "public",
                                "direct",
                                "open",
                                null,
                                77L,
                                1))
                .thenReturn(firstOccurrence);
        Mockito.when(
                        matchDao.createMatch(
                                1L,
                                "Test Address",
                                "Weekly Padel",
                                "Test Description",
                                secondOccurrence.getStartsAt(),
                                secondOccurrence.getEndsAt(),
                                8,
                                BigDecimal.ZERO,
                                Sport.PADEL,
                                "public",
                                "direct",
                                "open",
                                null,
                                77L,
                                2))
                .thenReturn(secondOccurrence);
        Mockito.when(
                        matchDao.createMatch(
                                1L,
                                "Test Address",
                                "Weekly Padel",
                                "Test Description",
                                thirdOccurrence.getStartsAt(),
                                thirdOccurrence.getEndsAt(),
                                8,
                                BigDecimal.ZERO,
                                Sport.PADEL,
                                "public",
                                "direct",
                                "open",
                                null,
                                77L,
                                3))
                .thenReturn(thirdOccurrence);

        // 2. Exercise
        final Match result =
                matchService.createMatch(
                        new CreateMatchRequest(
                                1L,
                                "Test Address",
                                "Weekly Padel",
                                "Test Description",
                                startsAt,
                                endsAt,
                                8,
                                BigDecimal.ZERO,
                                Sport.PADEL,
                                "public",
                                "direct",
                                "open",
                                null,
                                new CreateRecurrenceRequest(
                                        RecurrenceFrequency.WEEKLY,
                                        RecurrenceEndMode.OCCURRENCE_COUNT,
                                        null,
                                        3,
                                        ZoneId.of("UTC"))));

        // 3. Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(101L, result.getId());
        Assertions.assertEquals(77L, result.getSeriesId());
        Assertions.assertEquals(1, result.getSeriesOccurrenceIndex());
    }

    @Test
    public void testCreateRecurringMatchGeneratesOccurrencesUntilDate() {
        // 1. Arrange
        final Instant startsAt = Instant.parse("2026-04-10T18:00:00Z");
        final Instant endsAt = Instant.parse("2026-04-10T19:30:00Z");
        final java.time.LocalDate untilDate = java.time.LocalDate.of(2026, 4, 17);
        final Match firstOccurrence =
                new Match(
                        111L,
                        Sport.PADEL,
                        1L,
                        "Test Address",
                        "Weekly Padel",
                        "Test Description",
                        startsAt,
                        endsAt,
                        8,
                        BigDecimal.ZERO,
                        "public",
                        "direct",
                        "open",
                        0,
                        null,
                        88L,
                        1);
        final Match secondOccurrence =
                new Match(
                        112L,
                        Sport.PADEL,
                        1L,
                        "Test Address",
                        "Weekly Padel",
                        "Test Description",
                        Instant.parse("2026-04-17T18:00:00Z"),
                        Instant.parse("2026-04-17T19:30:00Z"),
                        8,
                        BigDecimal.ZERO,
                        "public",
                        "direct",
                        "open",
                        0,
                        null,
                        88L,
                        2);
        Mockito.when(
                        matchDao.createMatchSeries(
                                1L, "weekly", startsAt, endsAt, "UTC", untilDate, null))
                .thenReturn(88L);
        Mockito.when(
                        matchDao.createMatch(
                                1L,
                                "Test Address",
                                "Weekly Padel",
                                "Test Description",
                                firstOccurrence.getStartsAt(),
                                firstOccurrence.getEndsAt(),
                                8,
                                BigDecimal.ZERO,
                                Sport.PADEL,
                                "public",
                                "direct",
                                "open",
                                null,
                                88L,
                                1))
                .thenReturn(firstOccurrence);
        Mockito.when(
                        matchDao.createMatch(
                                1L,
                                "Test Address",
                                "Weekly Padel",
                                "Test Description",
                                secondOccurrence.getStartsAt(),
                                secondOccurrence.getEndsAt(),
                                8,
                                BigDecimal.ZERO,
                                Sport.PADEL,
                                "public",
                                "direct",
                                "open",
                                null,
                                88L,
                                2))
                .thenReturn(secondOccurrence);

        // 2. Exercise
        final Match result =
                matchService.createMatch(
                        new CreateMatchRequest(
                                1L,
                                "Test Address",
                                "Weekly Padel",
                                "Test Description",
                                startsAt,
                                endsAt,
                                8,
                                BigDecimal.ZERO,
                                Sport.PADEL,
                                "public",
                                "direct",
                                "open",
                                null,
                                new CreateRecurrenceRequest(
                                        RecurrenceFrequency.WEEKLY,
                                        RecurrenceEndMode.UNTIL_DATE,
                                        untilDate,
                                        null,
                                        ZoneId.of("UTC"))));

        // 3. Assert
        Assertions.assertEquals(111L, result.getId());
        Assertions.assertEquals(88L, result.getSeriesId());
        Assertions.assertEquals(1, result.getSeriesOccurrenceIndex());
    }

    @Test
    public void testCreateRecurringMatchRejectsUntilDateWithoutRepeatedOccurrence() {
        // 1. Arrange
        final Instant startsAt = Instant.parse("2026-04-10T18:00:00Z");
        final Instant endsAt = Instant.parse("2026-04-10T19:30:00Z");

        // 2. Exercise
        final IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                matchService.createMatch(
                                        new CreateMatchRequest(
                                                1L,
                                                "Test Address",
                                                "Weekly Padel",
                                                "Test Description",
                                                startsAt,
                                                endsAt,
                                                8,
                                                BigDecimal.ZERO,
                                                Sport.PADEL,
                                                "public",
                                                "direct",
                                                "open",
                                                null,
                                                new CreateRecurrenceRequest(
                                                        RecurrenceFrequency.WEEKLY,
                                                        RecurrenceEndMode.UNTIL_DATE,
                                                        java.time.LocalDate.of(2026, 4, 12),
                                                        null,
                                                        ZoneId.of("UTC")))));

        // 3. Assert
        Assertions.assertEquals("match.recurrence.error.tooFewOccurrences", exception.getMessage());
    }

    @Test
    public void testCreateMatchRejectsPastStartTime() {
        final IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                matchService.createMatch(
                                        new CreateMatchRequest(
                                                1L,
                                                "Test Address",
                                                "Test Match",
                                                "Test Description",
                                                FIXED_NOW,
                                                null,
                                                10,
                                                BigDecimal.ZERO,
                                                Sport.FOOTBALL,
                                                "public",
                                                "open",
                                                null)));

        Assertions.assertEquals("match.schedule.error.startsAtPast", exception.getMessage());
    }

    @Test
    public void testUpdateMatchRejectsMissingMatch() {
        Mockito.when(matchDao.findById(13L)).thenReturn(Optional.empty());

        final MatchUpdateException exception =
                Assertions.assertThrows(
                        MatchUpdateException.class,
                        () ->
                                matchService.updateMatch(
                                        13L,
                                        1L,
                                        new UpdateMatchRequest(
                                                "Test Address",
                                                "Test Match",
                                                "Test Description",
                                                FIXED_NOW.plusSeconds(3600),
                                                FIXED_NOW.plusSeconds(7200),
                                                10,
                                                BigDecimal.ZERO,
                                                Sport.FOOTBALL,
                                                "public",
                                                "open",
                                                null)));

        Assertions.assertEquals(MatchUpdateFailureReason.MATCH_NOT_FOUND, exception.getReason());
        Assertions.assertEquals("match.update.error.notFound", exception.getMessage());
    }

    @Test
    public void testUpdateMatchRejectsNonOwner() {
        final Match existingMatch = createTestMatch(14L, "Test Match", "football");
        Mockito.when(matchDao.findById(14L)).thenReturn(Optional.of(existingMatch));

        final MatchUpdateException exception =
                Assertions.assertThrows(
                        MatchUpdateException.class,
                        () ->
                                matchService.updateMatch(
                                        14L,
                                        99L,
                                        new UpdateMatchRequest(
                                                "Test Address",
                                                "Test Match",
                                                "Test Description",
                                                FIXED_NOW.plusSeconds(3600),
                                                FIXED_NOW.plusSeconds(7200),
                                                10,
                                                BigDecimal.ZERO,
                                                Sport.FOOTBALL,
                                                "public",
                                                "open",
                                                null)));

        Assertions.assertEquals(MatchUpdateFailureReason.FORBIDDEN, exception.getReason());
        Assertions.assertEquals("match.update.error.forbidden", exception.getMessage());
    }

    @Test
    public void testUpdateMatchRejectsCompletedMatch() {
        final Match completedMatch =
                new Match(
                        17L,
                        Sport.FOOTBALL,
                        1L,
                        "Test Address",
                        "Completed Match",
                        "Test Description",
                        FIXED_NOW.plusSeconds(3600),
                        FIXED_NOW.plusSeconds(7200),
                        10,
                        BigDecimal.ZERO,
                        "public",
                        "completed",
                        0,
                        null);
        Mockito.when(matchDao.findById(17L)).thenReturn(Optional.of(completedMatch));

        final MatchUpdateException exception =
                Assertions.assertThrows(
                        MatchUpdateException.class,
                        () ->
                                matchService.updateMatch(
                                        17L,
                                        1L,
                                        new UpdateMatchRequest(
                                                "Test Address",
                                                "Updated Match",
                                                "Test Description",
                                                FIXED_NOW.plusSeconds(5400),
                                                FIXED_NOW.plusSeconds(9000),
                                                10,
                                                BigDecimal.ZERO,
                                                Sport.FOOTBALL,
                                                "public",
                                                "open",
                                                null)));

        Assertions.assertEquals(MatchUpdateFailureReason.NOT_EDITABLE, exception.getReason());
        Assertions.assertEquals("match.update.error.notEditable", exception.getMessage());
    }

    @Test
    public void testUpdateMatchRejectsPastStartTime() {
        final Match existingMatch = createTestMatch(15L, "Test Match", "football");
        Mockito.when(matchDao.findById(15L)).thenReturn(Optional.of(existingMatch));

        final MatchUpdateException exception =
                Assertions.assertThrows(
                        MatchUpdateException.class,
                        () ->
                                matchService.updateMatch(
                                        15L,
                                        1L,
                                        new UpdateMatchRequest(
                                                "Test Address",
                                                "Test Match",
                                                "Test Description",
                                                FIXED_NOW,
                                                FIXED_NOW.plusSeconds(3600),
                                                10,
                                                BigDecimal.ZERO,
                                                Sport.FOOTBALL,
                                                "public",
                                                "open",
                                                null)));

        Assertions.assertEquals(MatchUpdateFailureReason.INVALID_SCHEDULE, exception.getReason());
        Assertions.assertEquals("match.schedule.error.startsAtPast", exception.getMessage());
    }

    @Test
    public void testUpdateMatchRejectsEndTimeBeforeStartTime() {
        final Match existingMatch = createTestMatch(10L, "Test Match", "football");
        Mockito.when(matchDao.findById(10L)).thenReturn(Optional.of(existingMatch));

        final MatchUpdateException exception =
                Assertions.assertThrows(
                        MatchUpdateException.class,
                        () ->
                                matchService.updateMatch(
                                        10L,
                                        1L,
                                        new UpdateMatchRequest(
                                                "Test Address",
                                                "Test Match",
                                                "Test Description",
                                                FIXED_NOW.plusSeconds(7200),
                                                FIXED_NOW.plusSeconds(3600),
                                                10,
                                                BigDecimal.ZERO,
                                                Sport.FOOTBALL,
                                                "public",
                                                "open",
                                                null)));

        Assertions.assertEquals(MatchUpdateFailureReason.INVALID_SCHEDULE, exception.getReason());
        Assertions.assertEquals("match.schedule.error.endBeforeStart", exception.getMessage());
    }

    @Test
    public void testUpdateMatchRejectsCapacityBelowConfirmedParticipants() {
        final Match existingMatch = createTestMatch(11L, "Test Match", "football");
        Mockito.when(matchDao.findById(11L)).thenReturn(Optional.of(existingMatch));
        Mockito.when(matchParticipantDao.findConfirmedParticipants(11L))
                .thenReturn(
                        List.of(
                                new User(2L, "first@test.com", "first"),
                                new User(3L, "second@test.com", "second")));

        final MatchUpdateException exception =
                Assertions.assertThrows(
                        MatchUpdateException.class,
                        () ->
                                matchService.updateMatch(
                                        11L,
                                        1L,
                                        new UpdateMatchRequest(
                                                "Test Address",
                                                "Test Match",
                                                "Test Description",
                                                FIXED_NOW.plusSeconds(3600),
                                                FIXED_NOW.plusSeconds(7200),
                                                1,
                                                BigDecimal.ZERO,
                                                Sport.FOOTBALL,
                                                "public",
                                                "open",
                                                null)));

        Assertions.assertEquals(
                MatchUpdateFailureReason.CAPACITY_BELOW_CONFIRMED, exception.getReason());
        Assertions.assertEquals(
                "match.update.error.capacityBelowConfirmed", exception.getMessage());
    }

    @Test
    public void testUpdateMatchPersistsAndReturnsUpdatedMatch() {
        final Match existingMatch = createTestMatch(12L, "Old Title", "football");
        final Match updatedMatch =
                new Match(
                        12L,
                        Sport.TENNIS,
                        1L,
                        "Updated Address",
                        "Updated Title",
                        "Updated Description",
                        FIXED_NOW.plusSeconds(3600),
                        FIXED_NOW.plusSeconds(7200),
                        12,
                        BigDecimal.ONE,
                        "public",
                        "approval_required",
                        "open",
                        0,
                        null);
        Mockito.when(matchDao.findById(12L))
                .thenReturn(Optional.of(existingMatch))
                .thenReturn(Optional.of(updatedMatch));
        Mockito.when(matchParticipantDao.findConfirmedParticipants(12L)).thenReturn(List.of());
        Mockito.when(
                        matchDao.updateMatch(
                                12L,
                                1L,
                                "Updated Address",
                                "Updated Title",
                                "Updated Description",
                                FIXED_NOW.plusSeconds(3600),
                                FIXED_NOW.plusSeconds(7200),
                                12,
                                BigDecimal.ONE,
                                Sport.TENNIS,
                                "public",
                                "approval_required",
                                "open",
                                null))
                .thenReturn(true);

        final Match result =
                matchService.updateMatch(
                        12L,
                        1L,
                        new UpdateMatchRequest(
                                "Updated Address",
                                "Updated Title",
                                "Updated Description",
                                FIXED_NOW.plusSeconds(3600),
                                FIXED_NOW.plusSeconds(7200),
                                12,
                                BigDecimal.ONE,
                                Sport.TENNIS,
                                "public",
                                "approval_required",
                                "open",
                                null));

        Assertions.assertEquals("Updated Title", result.getTitle());
        Assertions.assertEquals(Sport.TENNIS, result.getSport());
        Assertions.assertEquals("Updated Address", result.getAddress());
        Assertions.assertEquals("approval_required", result.getJoinPolicy());
        Mockito.verify(matchNotificationService).notifyMatchUpdated(updatedMatch);
    }

    @Test
    public void testUpdateMatchRejectsWhenOwnedUpdateTouchesNoRows() {
        final Match existingMatch = createTestMatch(16L, "Old Title", "football");
        Mockito.when(matchDao.findById(16L)).thenReturn(Optional.of(existingMatch));
        Mockito.when(matchParticipantDao.findConfirmedParticipants(16L)).thenReturn(List.of());
        Mockito.when(
                        matchDao.updateMatch(
                                16L,
                                1L,
                                "Updated Address",
                                "Updated Title",
                                "Updated Description",
                                FIXED_NOW.plusSeconds(3600),
                                FIXED_NOW.plusSeconds(7200),
                                12,
                                BigDecimal.ONE,
                                Sport.TENNIS,
                                "public",
                                "direct",
                                "open",
                                null))
                .thenReturn(false);

        final MatchUpdateException exception =
                Assertions.assertThrows(
                        MatchUpdateException.class,
                        () ->
                                matchService.updateMatch(
                                        16L,
                                        1L,
                                        new UpdateMatchRequest(
                                                "Updated Address",
                                                "Updated Title",
                                                "Updated Description",
                                                FIXED_NOW.plusSeconds(3600),
                                                FIXED_NOW.plusSeconds(7200),
                                                12,
                                                BigDecimal.ONE,
                                                Sport.TENNIS,
                                                "public",
                                                "open",
                                                null)));

        Assertions.assertEquals(MatchUpdateFailureReason.FORBIDDEN, exception.getReason());
        Assertions.assertEquals("match.update.error.forbidden", exception.getMessage());
    }

    @Test
    public void testCancelMatchRejectsMissingMatch() {
        Mockito.when(matchDao.findById(21L)).thenReturn(Optional.empty());

        final MatchCancellationException exception =
                Assertions.assertThrows(
                        MatchCancellationException.class, () -> matchService.cancelMatch(21L, 1L));

        Assertions.assertEquals(
                MatchCancellationFailureReason.MATCH_NOT_FOUND, exception.getReason());
        Assertions.assertEquals("match.cancel.error.notFound", exception.getMessage());
    }

    @Test
    public void testCancelMatchRejectsNonOwner() {
        final Match existingMatch = createTestMatch(22L, "Test Match", "football");
        Mockito.when(matchDao.findById(22L)).thenReturn(Optional.of(existingMatch));

        final MatchCancellationException exception =
                Assertions.assertThrows(
                        MatchCancellationException.class, () -> matchService.cancelMatch(22L, 99L));

        Assertions.assertEquals(MatchCancellationFailureReason.FORBIDDEN, exception.getReason());
        Assertions.assertEquals("match.cancel.error.forbidden", exception.getMessage());
    }

    @Test
    public void testCancelMatchRejectsCompletedMatch() {
        final Match completedMatch =
                new Match(
                        29L,
                        Sport.FOOTBALL,
                        1L,
                        "Test Address",
                        "Completed Match",
                        "Test Description",
                        FIXED_NOW.minusSeconds(7200),
                        FIXED_NOW.minusSeconds(3600),
                        10,
                        BigDecimal.ZERO,
                        "public",
                        "completed",
                        0,
                        null);
        Mockito.when(matchDao.findById(29L)).thenReturn(Optional.of(completedMatch));

        final MatchCancellationException exception =
                Assertions.assertThrows(
                        MatchCancellationException.class, () -> matchService.cancelMatch(29L, 1L));

        Assertions.assertEquals(MatchCancellationFailureReason.FORBIDDEN, exception.getReason());
        Assertions.assertEquals("match.cancel.error.forbidden", exception.getMessage());
    }

    @Test
    public void testCancelMatchPersistsAndReturnsCancelledMatch() {
        final Match existingMatch = createTestMatch(23L, "Test Match", "football");
        final Match cancelledMatch =
                new Match(
                        23L,
                        Sport.FOOTBALL,
                        1L,
                        "Test Address",
                        "Test Match",
                        "Test Description",
                        FIXED_NOW.plusSeconds(3600),
                        null,
                        10,
                        BigDecimal.ZERO,
                        "public",
                        "cancelled",
                        0,
                        null);
        Mockito.when(matchDao.findById(23L))
                .thenReturn(Optional.of(existingMatch))
                .thenReturn(Optional.of(cancelledMatch));
        Mockito.when(matchDao.cancelMatch(23L, 1L)).thenReturn(true);

        final Match result = matchService.cancelMatch(23L, 1L);

        Assertions.assertEquals("cancelled", result.getStatus());
        Assertions.assertEquals(23L, result.getId());
        Mockito.verify(matchNotificationService).notifyMatchCancelled(cancelledMatch);
    }

    @Test
    public void testCancelMatchPersistsRecurringOccurrenceCancellation() {
        // Arrange
        final Match existingMatch =
                new Match(
                        47L,
                        Sport.PADEL,
                        1L,
                        "Test Address",
                        "Weekly Padel",
                        "Second occurrence",
                        FIXED_NOW.plusSeconds(3600),
                        FIXED_NOW.plusSeconds(7200),
                        10,
                        BigDecimal.ZERO,
                        "public",
                        "direct",
                        "open",
                        0,
                        null,
                        600L,
                        2);
        final Match cancelledMatch =
                new Match(
                        47L,
                        Sport.PADEL,
                        1L,
                        "Test Address",
                        "Weekly Padel",
                        "Second occurrence",
                        FIXED_NOW.plusSeconds(3600),
                        FIXED_NOW.plusSeconds(7200),
                        10,
                        BigDecimal.ZERO,
                        "public",
                        "direct",
                        "cancelled",
                        0,
                        null,
                        600L,
                        2);
        Mockito.when(matchDao.findById(47L))
                .thenReturn(Optional.of(existingMatch))
                .thenReturn(Optional.of(cancelledMatch));
        Mockito.when(matchDao.cancelMatch(47L, 1L)).thenReturn(true);

        // Exercise
        final Match result = matchService.cancelMatch(47L, 1L);

        // Assert
        Assertions.assertEquals(47L, result.getId());
        Assertions.assertEquals("cancelled", result.getStatus());
        Assertions.assertEquals(600L, result.getSeriesId());
        Assertions.assertEquals(2, result.getSeriesOccurrenceIndex());
    }

    @Test
    public void testUpdateSeriesFromOccurrenceUpdatesSelectedAndFutureOccurrences() {
        // 1. Arrange
        final Match pastOccurrence =
                recurringMatch(
                        45L,
                        "Past Weekly Padel",
                        FIXED_NOW.minusSeconds(604800),
                        FIXED_NOW.minusSeconds(599400),
                        "open",
                        1);
        final Match pivotOccurrence =
                recurringMatch(
                        46L,
                        "Weekly Padel",
                        FIXED_NOW.plusSeconds(3600),
                        FIXED_NOW.plusSeconds(7200),
                        "open",
                        2);
        final Match futureOccurrence =
                recurringMatch(
                        47L,
                        "Weekly Padel",
                        FIXED_NOW.plusSeconds(608400),
                        FIXED_NOW.plusSeconds(612000),
                        "open",
                        3);
        final Match cancelledOccurrence =
                recurringMatch(
                        48L,
                        "Weekly Padel",
                        FIXED_NOW.plusSeconds(1213200),
                        FIXED_NOW.plusSeconds(1216800),
                        "cancelled",
                        4);
        final Match updatedPivot =
                recurringMatch(
                        46L,
                        "Updated Weekly Padel",
                        FIXED_NOW.plusSeconds(5400),
                        FIXED_NOW.plusSeconds(9000),
                        "open",
                        2);
        final Match updatedFuture =
                recurringMatch(
                        47L,
                        "Updated Weekly Padel",
                        FIXED_NOW.plusSeconds(610200),
                        FIXED_NOW.plusSeconds(613800),
                        "open",
                        3);
        final UpdateMatchRequest request =
                new UpdateMatchRequest(
                        "Updated Address",
                        "Updated Weekly Padel",
                        "Updated Description",
                        FIXED_NOW.plusSeconds(5400),
                        FIXED_NOW.plusSeconds(9000),
                        8,
                        BigDecimal.ONE,
                        Sport.PADEL,
                        "public",
                        "direct",
                        "open",
                        null);
        Mockito.when(matchDao.findById(46L))
                .thenReturn(Optional.of(pivotOccurrence))
                .thenReturn(Optional.of(updatedPivot));
        Mockito.when(matchDao.findById(47L)).thenReturn(Optional.of(updatedFuture));
        Mockito.when(matchDao.findSeriesOccurrences(600L))
                .thenReturn(
                        List.of(
                                pastOccurrence,
                                pivotOccurrence,
                                futureOccurrence,
                                cancelledOccurrence));
        Mockito.when(matchParticipantDao.findConfirmedParticipants(46L)).thenReturn(List.of());
        Mockito.when(matchParticipantDao.findConfirmedParticipants(47L)).thenReturn(List.of());
        Mockito.when(
                        matchDao.updateMatch(
                                46L,
                                1L,
                                "Updated Address",
                                "Updated Weekly Padel",
                                "Updated Description",
                                FIXED_NOW.plusSeconds(5400),
                                FIXED_NOW.plusSeconds(9000),
                                8,
                                BigDecimal.ONE,
                                Sport.PADEL,
                                "public",
                                "direct",
                                "open",
                                null))
                .thenReturn(true);
        Mockito.when(
                        matchDao.updateMatch(
                                47L,
                                1L,
                                "Updated Address",
                                "Updated Weekly Padel",
                                "Updated Description",
                                FIXED_NOW.plusSeconds(610200),
                                FIXED_NOW.plusSeconds(613800),
                                8,
                                BigDecimal.ONE,
                                Sport.PADEL,
                                "public",
                                "direct",
                                "open",
                                null))
                .thenReturn(true);

        // 2. Exercise
        final List<Match> result = matchService.updateSeriesFromOccurrence(46L, 1L, request);

        // 3. Assert
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(46L, result.get(0).getId());
        Assertions.assertEquals(47L, result.get(1).getId());
        Assertions.assertEquals("Updated Weekly Padel", result.get(0).getTitle());
        Assertions.assertEquals(FIXED_NOW.plusSeconds(610200), result.get(1).getStartsAt());
        Mockito.verify(matchNotificationService).notifyRecurringMatchesUpdated(result);
    }

    @Test
    public void testUpdateSeriesFromOccurrenceRejectsNonRecurringMatch() {
        // 1. Arrange
        final Match singleMatch = createTestMatch(46L, "Single Padel", "padel");
        Mockito.when(matchDao.findById(46L)).thenReturn(Optional.of(singleMatch));

        // 2. Exercise
        final MatchUpdateException exception =
                Assertions.assertThrows(
                        MatchUpdateException.class,
                        () ->
                                matchService.updateSeriesFromOccurrence(
                                        46L,
                                        1L,
                                        new UpdateMatchRequest(
                                                "Updated Address",
                                                "Updated Match",
                                                "Updated Description",
                                                FIXED_NOW.plusSeconds(3600),
                                                FIXED_NOW.plusSeconds(7200),
                                                8,
                                                BigDecimal.ONE,
                                                Sport.PADEL,
                                                "public",
                                                "direct",
                                                "open",
                                                null)));

        // 3. Assert
        Assertions.assertEquals(MatchUpdateFailureReason.NOT_RECURRING, exception.getReason());
    }

    @Test
    public void testCancelSeriesFromOccurrenceCancelsSelectedAndFutureOccurrences() {
        // 1. Arrange
        final Match pastOccurrence =
                recurringMatch(
                        45L,
                        "Past Weekly Padel",
                        FIXED_NOW.minusSeconds(604800),
                        FIXED_NOW.minusSeconds(599400),
                        "open",
                        1);
        final Match pivotOccurrence =
                recurringMatch(
                        46L,
                        "Weekly Padel",
                        FIXED_NOW.plusSeconds(3600),
                        FIXED_NOW.plusSeconds(7200),
                        "open",
                        2);
        final Match futureOccurrence =
                recurringMatch(
                        47L,
                        "Weekly Padel",
                        FIXED_NOW.plusSeconds(608400),
                        FIXED_NOW.plusSeconds(612000),
                        "open",
                        3);
        final Match cancelledOccurrence =
                recurringMatch(
                        48L,
                        "Weekly Padel",
                        FIXED_NOW.plusSeconds(1213200),
                        FIXED_NOW.plusSeconds(1216800),
                        "cancelled",
                        4);
        final Match cancelledPivot =
                recurringMatch(
                        46L,
                        "Weekly Padel",
                        FIXED_NOW.plusSeconds(3600),
                        FIXED_NOW.plusSeconds(7200),
                        "cancelled",
                        2);
        final Match cancelledFuture =
                recurringMatch(
                        47L,
                        "Weekly Padel",
                        FIXED_NOW.plusSeconds(608400),
                        FIXED_NOW.plusSeconds(612000),
                        "cancelled",
                        3);
        Mockito.when(matchDao.findById(46L))
                .thenReturn(Optional.of(pivotOccurrence))
                .thenReturn(Optional.of(cancelledPivot));
        Mockito.when(matchDao.findById(47L)).thenReturn(Optional.of(cancelledFuture));
        Mockito.when(matchDao.findSeriesOccurrences(600L))
                .thenReturn(
                        List.of(
                                pastOccurrence,
                                pivotOccurrence,
                                futureOccurrence,
                                cancelledOccurrence));
        Mockito.when(matchDao.cancelMatch(46L, 1L)).thenReturn(true);
        Mockito.when(matchDao.cancelMatch(47L, 1L)).thenReturn(true);

        // 2. Exercise
        final List<Match> result = matchService.cancelSeriesFromOccurrence(46L, 1L);

        // 3. Assert
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(46L, result.get(0).getId());
        Assertions.assertEquals(47L, result.get(1).getId());
        Assertions.assertEquals("cancelled", result.get(0).getStatus());
        Assertions.assertEquals("cancelled", result.get(1).getStatus());
        Mockito.verify(matchNotificationService).notifyRecurringMatchesCancelled(result);
    }

    @Test
    public void testCancelMatchReturnsExistingMatchWhenAlreadyCancelled() {
        final Match existingMatch =
                new Match(
                        24L,
                        Sport.FOOTBALL,
                        1L,
                        "Test Address",
                        "Test Match",
                        "Test Description",
                        FIXED_NOW.plusSeconds(3600),
                        null,
                        10,
                        BigDecimal.ZERO,
                        "public",
                        "cancelled",
                        0,
                        null);
        Mockito.when(matchDao.findById(24L)).thenReturn(Optional.of(existingMatch));

        final Match result = matchService.cancelMatch(24L, 1L);

        Assertions.assertEquals("cancelled", result.getStatus());
        Assertions.assertEquals(24L, result.getId());
        Mockito.verifyNoInteractions(matchNotificationService);
    }

    @Test
    public void testUpdateMatchWithoutConfirmedParticipantsStillDelegatesNotificationService() {
        final Match existingMatch = createTestMatch(25L, "Old Title", "football");
        final Match updatedMatch =
                new Match(
                        25L,
                        Sport.FOOTBALL,
                        1L,
                        "Updated Address",
                        "Updated Title",
                        "Updated Description",
                        FIXED_NOW.plusSeconds(3600),
                        FIXED_NOW.plusSeconds(7200),
                        12,
                        BigDecimal.ONE,
                        "public",
                        "open",
                        0,
                        null);
        Mockito.when(matchDao.findById(25L))
                .thenReturn(Optional.of(existingMatch))
                .thenReturn(Optional.of(updatedMatch));
        Mockito.when(
                        matchDao.updateMatch(
                                25L,
                                1L,
                                "Updated Address",
                                "Updated Title",
                                "Updated Description",
                                FIXED_NOW.plusSeconds(3600),
                                FIXED_NOW.plusSeconds(7200),
                                12,
                                BigDecimal.ONE,
                                Sport.FOOTBALL,
                                "public",
                                "direct",
                                "open",
                                null))
                .thenReturn(true);

        final Match result =
                matchService.updateMatch(
                        25L,
                        1L,
                        new UpdateMatchRequest(
                                "Updated Address",
                                "Updated Title",
                                "Updated Description",
                                FIXED_NOW.plusSeconds(3600),
                                FIXED_NOW.plusSeconds(7200),
                                12,
                                BigDecimal.ONE,
                                Sport.FOOTBALL,
                                "public",
                                "open",
                                null));

        Assertions.assertEquals(25L, result.getId());
        Mockito.verify(matchNotificationService).notifyMatchUpdated(updatedMatch);
    }

    @Test
    public void testCancelMatchWithoutConfirmedParticipantsStillDelegatesNotificationService() {
        final Match existingMatch = createTestMatch(26L, "Test Match", "football");
        final Match cancelledMatch =
                new Match(
                        26L,
                        Sport.FOOTBALL,
                        1L,
                        "Test Address",
                        "Test Match",
                        "Test Description",
                        FIXED_NOW.plusSeconds(3600),
                        null,
                        10,
                        BigDecimal.ZERO,
                        "public",
                        "cancelled",
                        0,
                        null);
        Mockito.when(matchDao.findById(26L))
                .thenReturn(Optional.of(existingMatch))
                .thenReturn(Optional.of(cancelledMatch));
        Mockito.when(matchDao.cancelMatch(26L, 1L)).thenReturn(true);

        final Match result = matchService.cancelMatch(26L, 1L);

        Assertions.assertEquals("cancelled", result.getStatus());
        Mockito.verify(matchNotificationService).notifyMatchCancelled(cancelledMatch);
    }

    @Test
    public void testUpdateMatchFailureSendsNoMail() {
        Mockito.when(matchDao.findById(27L)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                MatchUpdateException.class,
                () ->
                        matchService.updateMatch(
                                27L,
                                1L,
                                new UpdateMatchRequest(
                                        "Test Address",
                                        "Test Match",
                                        "Test Description",
                                        FIXED_NOW.plusSeconds(3600),
                                        FIXED_NOW.plusSeconds(7200),
                                        10,
                                        BigDecimal.ZERO,
                                        Sport.FOOTBALL,
                                        "public",
                                        "open",
                                        null)));

        Mockito.verifyNoInteractions(matchNotificationService);
    }

    @Test
    public void testCancelMatchFailureSendsNoMail() {
        Mockito.when(matchDao.findById(28L)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                MatchCancellationException.class, () -> matchService.cancelMatch(28L, 1L));

        Mockito.verifyNoInteractions(matchNotificationService);
    }

    @Test
    public void testFindPublicMatchByIdDelegates() {
        final Match expectedMatch = createTestMatch(8L, "Late Football", "football");
        Mockito.when(matchDao.findPublicMatchById(8L)).thenReturn(Optional.of(expectedMatch));

        final Optional<Match> result = matchService.findPublicMatchById(8L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Late Football", result.get().getTitle());
    }

    @Test
    public void testFindMatchByIdDelegates() {
        final Match expectedMatch = createTestMatch(9L, "Private Football", "football");
        Mockito.when(matchDao.findById(9L)).thenReturn(Optional.of(expectedMatch));

        final Optional<Match> result = matchService.findMatchById(9L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Private Football", result.get().getTitle());
    }

    @Test
    public void testFindConfirmedParticipantsDelegates() {
        final List<User> expectedParticipants =
                List.of(
                        new User(2L, "first@test.com", "first"),
                        new User(3L, "second@test.com", "second"));
        Mockito.when(matchParticipantDao.findConfirmedParticipants(8L))
                .thenReturn(expectedParticipants);

        final List<User> result = matchService.findConfirmedParticipants(8L);

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("first", result.get(0).getUsername());
        Assertions.assertEquals("second", result.get(1).getUsername());
    }

    @Test
    public void testFindHostedMatchesUsesDefaultDashboardPageSize() {
        final Match hosted = createTestMatch(10L, "Host Match", "padel");
        Mockito.when(
                        matchDao.countHostedMatches(
                                9L,
                                null,
                                null,
                                List.of(),
                                List.of(),
                                List.of(),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                null,
                                null,
                                ZoneId.systemDefault()))
                .thenReturn(1);
        Mockito.when(
                        matchDao.findHostedMatches(
                                9L,
                                null,
                                null,
                                List.of(),
                                List.of(),
                                List.of(),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                null,
                                null,
                                MatchSort.SOONEST,
                                ZoneId.systemDefault(),
                                0,
                                12))
                .thenReturn(List.of(hosted));

        final PaginatedResult<Match> result =
                matchService.findHostedMatches(
                        9L, null, null, null, null, null, null, null, null, null, null, null, 1, 0);

        Assertions.assertEquals(1, result.getItems().size());
        Assertions.assertEquals(1, result.getTotalCount());
        Assertions.assertEquals(12, result.getPageSize());
    }

    @Test
    public void testFindHostedMatchesParsesStatusCsv() {
        final Match completed =
                new Match(
                        11L,
                        Sport.PADEL,
                        9L,
                        "Club",
                        "Completed",
                        "desc",
                        Instant.now(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        "public",
                        "completed",
                        4,
                        null);

        Mockito.when(
                        matchDao.countHostedMatches(
                                9L,
                                null,
                                null,
                                List.of(),
                                List.of(),
                                List.of(EventStatus.COMPLETED, EventStatus.CANCELLED),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                null,
                                null,
                                ZoneId.systemDefault()))
                .thenReturn(1);
        Mockito.when(
                        matchDao.findHostedMatches(
                                9L,
                                null,
                                null,
                                List.of(),
                                List.of(),
                                List.of(EventStatus.COMPLETED, EventStatus.CANCELLED),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                null,
                                null,
                                MatchSort.SOONEST,
                                ZoneId.systemDefault(),
                                0,
                                10))
                .thenReturn(List.of(completed));

        final PaginatedResult<Match> result =
                matchService.findHostedMatches(
                        9L,
                        null,
                        null,
                        null,
                        null,
                        "completed,cancelled,invalid,completed",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        1,
                        10);

        Assertions.assertEquals(1, result.getItems().size());
        Assertions.assertEquals("completed", result.getItems().get(0).getStatus());
    }

    @Test
    public void testFindJoinedMatchesPastPaginatesDescendingDataset() {
        final Match first = createTestMatch(12L, "Past Match", "football");
        Mockito.when(
                        matchDao.countJoinedMatches(
                                4L,
                                Boolean.FALSE,
                                null,
                                List.of(),
                                List.of(),
                                List.of(),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                null,
                                null,
                                ZoneId.systemDefault()))
                .thenReturn(11);
        Mockito.when(
                        matchDao.findJoinedMatches(
                                4L,
                                Boolean.FALSE,
                                null,
                                List.of(),
                                List.of(),
                                List.of(),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                null,
                                null,
                                MatchSort.SOONEST,
                                ZoneId.systemDefault(),
                                10,
                                10))
                .thenReturn(List.of(first));

        final PaginatedResult<Match> result =
                matchService.findJoinedMatches(
                        4L,
                        Boolean.FALSE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        2,
                        10);

        Assertions.assertEquals(2, result.getPage());
        Assertions.assertEquals(2, result.getTotalPages());
        Assertions.assertEquals(1, result.getItems().size());
    }

    @Test
    public void testFindJoinedMatchesUpcomingClampsPageToLastAvailable() {
        final Match first = createTestMatch(13L, "Upcoming Match", "tennis");
        Mockito.when(
                        matchDao.countJoinedMatches(
                                4L,
                                Boolean.TRUE,
                                null,
                                List.of(),
                                List.of(),
                                List.of(),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                null,
                                null,
                                ZoneId.systemDefault()))
                .thenReturn(5);
        Mockito.when(
                        matchDao.findJoinedMatches(
                                4L,
                                Boolean.TRUE,
                                null,
                                List.of(),
                                List.of(),
                                List.of(),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                null,
                                null,
                                MatchSort.SOONEST,
                                ZoneId.systemDefault(),
                                0,
                                5))
                .thenReturn(List.of(first));

        final PaginatedResult<Match> result =
                matchService.findJoinedMatches(
                        4L,
                        Boolean.TRUE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        9,
                        5);

        Assertions.assertEquals(1, result.getPage());
        Assertions.assertEquals(1, result.getTotalPages());
        Assertions.assertEquals("Upcoming Match", result.getItems().get(0).getTitle());
    }

    private Match createTestMatch(final Long id, final String title, final String sport) {
        return new Match(
                id,
                Sport.fromDbValue(sport).orElse(Sport.FOOTBALL),
                1L,
                "Test Address",
                title,
                "Test Description",
                Instant.now(),
                null,
                10,
                BigDecimal.ZERO,
                "public",
                "open",
                0,
                null);
    }

    private Match recurringMatch(
            final Long id,
            final String title,
            final Instant startsAt,
            final Instant endsAt,
            final String status,
            final Integer occurrenceIndex) {
        return new Match(
                id,
                Sport.PADEL,
                1L,
                "Test Address",
                title,
                "Test Description",
                startsAt,
                endsAt,
                10,
                BigDecimal.ZERO,
                "public",
                "direct",
                status,
                0,
                null,
                600L,
                occurrenceIndex);
    }
}
