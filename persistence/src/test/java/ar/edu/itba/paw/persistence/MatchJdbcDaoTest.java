package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.EventTimeFilter;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.Sport;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class MatchJdbcDaoTest {

    @Autowired private MatchDao matchDao;

    @Autowired private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private long hostUserId;

    @BeforeEach
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, created_at, updated_at) VALUES (1, 'host', 'host@test.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        hostUserId = 1L;
    }

    @Test
    public void testCreateMatchPersistsSportCorrectly() {
        final ZonedDateTime startsAt = ZonedDateTime.now().plusDays(1);
        final Match created =
                matchDao.createMatch(
                        hostUserId,
                        "Stadium A",
                        "Tennis Singles",
                        "Open match",
                        startsAt.toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.TENNIS,
                        "public",
                        "open",
                        null);

        Assertions.assertNotNull(created.getId());
        Assertions.assertEquals(Sport.TENNIS, created.getSport());

        final List<Match> found =
                matchDao.findPublicMatches(
                        null,
                        List.of(Sport.TENNIS),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);
        Assertions.assertEquals(1, found.size());
        Assertions.assertEquals(Sport.TENNIS, found.get(0).getSport());
    }

    @Test
    public void testFindPublicEventsBySearchText() {
        insertMatch(
                "Morning Football",
                "Fast 5v5 match",
                "football",
                10,
                0,
                ZonedDateTime.now().plusDays(1));
        insertMatch(
                "Basketball Session",
                "Stretching",
                "basketball",
                10,
                0,
                ZonedDateTime.now().plusDays(1));

        final List<Match> result =
                matchDao.findPublicMatches(
                        "football",
                        List.of(),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Morning Football", result.get(0).getTitle());
    }

    @Test
    public void testFindPublicEventsBySportFilter() {
        insertMatch(
                "Morning Football",
                "Fast 5v5 match",
                "football",
                10,
                0,
                ZonedDateTime.now().plusDays(1));
        insertMatch(
                "Evening Padel", "Doubles games", "padel", 10, 0, ZonedDateTime.now().plusDays(1));

        final List<Match> result =
                matchDao.findPublicMatches(
                        null,
                        List.of(Sport.PADEL),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(Sport.PADEL, result.get(0).getSport());
    }

    @Test
    public void testFindPublicEventsExcludesFullEvents() {
        insertMatch(
                "Full Match", "No spots left", "football", 2, 2, ZonedDateTime.now().plusDays(1));
        insertMatch("Open Match", "Has spots", "football", 10, 1, ZonedDateTime.now().plusDays(1));

        final List<Match> result =
                matchDao.findPublicMatches(
                        null,
                        List.of(),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Open Match", result.get(0).getTitle());
    }

    @Test
    public void testFindPublicEventsWithPagination() {
        insertMatch("Event 1", "Description", "football", 10, 0, ZonedDateTime.now().plusHours(2));
        insertMatch("Event 2", "Description", "football", 10, 0, ZonedDateTime.now().plusHours(3));
        insertMatch("Event 3", "Description", "football", 10, 0, ZonedDateTime.now().plusHours(4));

        final List<Match> firstPage =
                matchDao.findPublicMatches(
                        null,
                        List.of(),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        2);
        final List<Match> secondPage =
                matchDao.findPublicMatches(
                        null,
                        List.of(),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        2,
                        2);

        Assertions.assertEquals(2, firstPage.size());
        Assertions.assertEquals(1, secondPage.size());
        Assertions.assertEquals(
                3,
                matchDao.countPublicMatches(
                        null, List.of(), EventTimeFilter.WEEK, null, null, ZoneId.systemDefault()));
    }

    @Test
    public void testFindPublicEventsWithAllTimeFilterShowsOnlyUpcoming() {
        insertMatch(
                "Past Match",
                "Already happened",
                "football",
                10,
                0,
                ZonedDateTime.now().minusHours(2));
        insertMatch(
                "Upcoming Match",
                "Will happen soon",
                "football",
                10,
                0,
                ZonedDateTime.now().plusHours(2));

        final List<Match> result =
                matchDao.findPublicMatches(
                        null,
                        List.of(),
                        EventTimeFilter.ALL,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Upcoming Match", result.get(0).getTitle());
    }

    @Test
    public void testFindPublicEventsByMultipleSportFilters() {
        insertMatch(
                "Morning Football",
                "Fast 5v5 match",
                "football",
                10,
                0,
                ZonedDateTime.now().plusDays(1));
        insertMatch(
                "Evening Padel", "Doubles games", "padel", 10, 0, ZonedDateTime.now().plusDays(1));
        insertMatch(
                "Basketball Session",
                "Stretching",
                "basketball",
                10,
                0,
                ZonedDateTime.now().plusDays(1));

        final List<Match> result =
                matchDao.findPublicMatches(
                        null,
                        List.of(Sport.FOOTBALL, Sport.PADEL),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(
                result.stream()
                        .allMatch(
                                match ->
                                        match.getSport() == Sport.FOOTBALL
                                                || match.getSport() == Sport.PADEL));
    }

    @Test
    public void testFindPublicMatchByIdIncludesAvailability() {
        final Match created =
                matchDao.createMatch(
                        hostUserId,
                        "Downtown Club",
                        "Padel Morning",
                        "Friendly doubles session",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        "public",
                        "open",
                        null);

        final Match found = matchDao.findPublicMatchById(created.getId()).orElseThrow();

        Assertions.assertEquals(created.getId(), found.getId());
        Assertions.assertEquals("Padel Morning", found.getTitle());
        Assertions.assertEquals(4, found.getAvailableSpots());
    }

    @Test
    public void testFindPublicEventsByPriceRange() {
        insertMatch(
                "Budget Match",
                "Free friendly session",
                "football",
                10,
                0,
                ZonedDateTime.now().plusDays(1),
                BigDecimal.ZERO);
        insertMatch(
                "Premium Match",
                "Club booking included",
                "football",
                10,
                0,
                ZonedDateTime.now().plusDays(1),
                new BigDecimal("25"));

        final List<Match> result =
                matchDao.findPublicMatches(
                        null,
                        List.of(),
                        EventTimeFilter.WEEK,
                        new BigDecimal("20"),
                        new BigDecimal("30"),
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Premium Match", result.get(0).getTitle());
        Assertions.assertEquals(
                1,
                matchDao.countPublicMatches(
                        null,
                        List.of(),
                        EventTimeFilter.WEEK,
                        new BigDecimal("20"),
                        new BigDecimal("30"),
                        ZoneId.systemDefault()));
    }

    @Test
    public void testFindByIdIncludesJoinedPlayersForNonPublicMatch() {
        final Match created =
                matchDao.createMatch(
                        hostUserId,
                        "Downtown Club",
                        "Private Football Night",
                        "Private 5v5",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        2,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        "private",
                        "open",
                        null);

        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, created_at, updated_at)"
                        + " VALUES (2, 'player', 'player@test.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (?, 2, 'joined', CURRENT_TIMESTAMP)",
                created.getId());

        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertEquals(created.getId(), found.getId());
        Assertions.assertEquals("private", found.getVisibility());
        Assertions.assertEquals(1, found.getJoinedPlayers());
    }

    private void insertMatch(
            final String title,
            final String description,
            final String sport,
            final int maxPlayers,
            final int joinedPlayers,
            final ZonedDateTime startsAt) {
        insertMatch(
                title, description, sport, maxPlayers, joinedPlayers, startsAt, BigDecimal.ZERO);
    }

    private void insertMatch(
            final String title,
            final String description,
            final String sport,
            final int maxPlayers,
            final int joinedPlayers,
            final ZonedDateTime startsAt,
            final BigDecimal pricePerPlayer) {
        final Match created =
                matchDao.createMatch(
                        hostUserId,
                        "Test Address",
                        title,
                        description,
                        startsAt.toInstant(),
                        null,
                        maxPlayers,
                        pricePerPlayer,
                        Sport.fromDbValue(sport).orElse(Sport.FOOTBALL),
                        "public",
                        "open",
                        null);

        final Long matchId = created.getId();

        for (int i = 0; i < joinedPlayers; i++) {
            final long userId = (matchId * 100L) + i;
            jdbcTemplate.update(
                    "INSERT INTO users (id, username, email, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    userId,
                    "user" + userId,
                    "user" + userId + "@test.com");
            jdbcTemplate.update(
                    "INSERT INTO match_participants (match_id, user_id, status, joined_at) VALUES (?, ?, 'joined', CURRENT_TIMESTAMP)",
                    matchId,
                    userId);
        }
    }
}
