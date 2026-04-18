package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.EventTimeFilter;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MatchServiceImplTest {

    @InjectMocks private MatchServiceImpl matchService;

    @Mock private MatchDao matchDao;
    @Mock private MatchParticipantDao matchParticipantDao;

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
        final Instant now = Instant.now();
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
    public void testFindMatchByIdDelegates() {
        final Match expectedMatch = createTestMatch(8L, "Late Football", "football");
        Mockito.when(matchDao.findMatchById(8L)).thenReturn(Optional.of(expectedMatch));

        final Optional<Match> result = matchService.findMatchById(8L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Late Football", result.get().getTitle());
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
                                MatchSort.SOONEST,
                                ZoneId.systemDefault(),
                                0,
                                12))
                .thenReturn(List.of(hosted));

        final PaginatedResult<Match> result =
                matchService.findHostedMatches(
                        9L, null, null, null, null, null, null, null, null, null, null, 1, 0);

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
}
