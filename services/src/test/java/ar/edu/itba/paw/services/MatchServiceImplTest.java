package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EventTimeFilter;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
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
        Mockito.when(
                        matchDao.findPublicMatches(
                                "football",
                                List.of(Sport.FOOTBALL),
                                EventTimeFilter.WEEK,
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
                                EventTimeFilter.WEEK,
                                null,
                                null,
                                ZoneId.of("UTC")))
                .thenReturn(25);

        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        "football", "football", "week", "soonest", 2, 10, "UTC", null, null);

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
                                ZoneId.of("UTC")))
                .thenReturn(1);

        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        null, "padel", null, null, 1, 0, "UTC", null, null);

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
                                ZoneId.of("UTC")))
                .thenReturn(2);

        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        null,
                        "football,tennis,invalid,football",
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
                                minPrice,
                                maxPrice,
                                ZoneId.of("UTC")))
                .thenReturn(1);

        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        null, "padel", null, "price", 1, 12, "UTC", minPrice, maxPrice);

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
                                ZoneId.of("UTC")))
                .thenReturn(13);
        Mockito.when(
                        matchDao.findPublicMatches(
                                null,
                                List.of(Sport.PADEL),
                                EventTimeFilter.ALL,
                                null,
                                null,
                                MatchSort.SOONEST,
                                ZoneId.of("UTC"),
                                12,
                                12))
                .thenReturn(List.of(match));

        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        null, "padel", null, null, 99, 12, "UTC", null, null);

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
                                "open",
                                null));

        Assertions.assertEquals("Updated Title", result.getTitle());
        Assertions.assertEquals(Sport.TENNIS, result.getSport());
        Assertions.assertEquals("Updated Address", result.getAddress());
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
}
