package ar.edu.itba.paw.services;

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
                                Sport.FOOTBALL,
                                EventTimeFilter.WEEK,
                                MatchSort.SOONEST,
                                ZoneId.of("UTC"),
                                10,
                                10))
                .thenReturn(List.of(match));
        Mockito.when(
                        matchDao.countPublicMatches(
                                "football", Sport.FOOTBALL, EventTimeFilter.WEEK, ZoneId.of("UTC")))
                .thenReturn(25);

        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        "football", "football", "week", "soonest", 2, 10, "UTC");

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
                                Sport.PADEL,
                                EventTimeFilter.ALL,
                                MatchSort.SOONEST,
                                ZoneId.of("UTC"),
                                0,
                                12))
                .thenReturn(List.of(match));
        Mockito.when(
                        matchDao.countPublicMatches(
                                null, Sport.PADEL, EventTimeFilter.ALL, ZoneId.of("UTC")))
                .thenReturn(1);

        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(null, "padel", null, null, 1, 0, "UTC");

        Assertions.assertEquals(1, result.getItems().size());
        Assertions.assertEquals("Padel", result.getItems().get(0).getTitle());
        Assertions.assertEquals(1, result.getTotalPages());
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
    public void testFindPublicMatchByIdDelegates() {
        final Match expectedMatch = createTestMatch(8L, "Late Football", "football");
        Mockito.when(matchDao.findPublicMatchById(8L)).thenReturn(Optional.of(expectedMatch));

        final Optional<Match> result = matchService.findPublicMatchById(8L);

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
