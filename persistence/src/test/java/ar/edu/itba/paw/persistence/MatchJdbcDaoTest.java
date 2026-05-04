package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.EventJoinPolicy;
import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.EventTimeFilter;
import ar.edu.itba.paw.models.EventVisibility;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.Sport;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
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

    @Autowired @NonNull private DataSource dataSource;

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
    public void shouldPersistMatchInDatabaseWhenCreatingMatch() {
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
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);

        final Map<String, Object> row =
                jdbcTemplate.queryForMap(
                        "SELECT host_user_id, title, sport, visibility, join_policy, status, max_players"
                                + " FROM matches WHERE id = ?",
                        created.getId());

        Assertions.assertNotNull(created.getId());
        Assertions.assertEquals(Sport.TENNIS, created.getSport());
        Assertions.assertEquals(hostUserId, ((Number) row.get("host_user_id")).longValue());
        Assertions.assertEquals("Tennis Singles", row.get("title"));
        Assertions.assertEquals("tennis", row.get("sport"));
        Assertions.assertEquals("public", row.get("visibility"));
        Assertions.assertEquals("direct", row.get("join_policy"));
        Assertions.assertEquals("open", row.get("status"));
        Assertions.assertEquals(4, ((Number) row.get("max_players")).intValue());

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
    public void shouldPersistSeriesAndOccurrencesWhenCreatingRecurringMatch() {
        final ZonedDateTime startsAt = ZonedDateTime.now().plusDays(1);
        final ZonedDateTime endsAt = startsAt.plusMinutes(90);

        final Long seriesId =
                matchDao.createMatchSeries(
                        hostUserId,
                        "weekly",
                        startsAt.toInstant(),
                        endsAt.toInstant(),
                        ZoneId.systemDefault().getId(),
                        null,
                        2);
        final Match first =
                matchDao.createMatch(
                        hostUserId,
                        "Stadium A",
                        "Weekly Tennis",
                        "Open match",
                        startsAt.toInstant(),
                        endsAt.toInstant(),
                        4,
                        BigDecimal.ZERO,
                        Sport.TENNIS,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        seriesId,
                        1);
        final Match second =
                matchDao.createMatch(
                        hostUserId,
                        "Stadium A",
                        "Weekly Tennis",
                        "Open match",
                        startsAt.plusWeeks(1).toInstant(),
                        endsAt.plusWeeks(1).toInstant(),
                        4,
                        BigDecimal.ZERO,
                        Sport.TENNIS,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        seriesId,
                        2);

        final Integer seriesCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_series WHERE id = ? AND host_user_id = ?"
                                + " AND frequency = 'weekly'",
                        Integer.class,
                        seriesId,
                        hostUserId);
        final Integer occurrencesCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM matches WHERE series_id = ?",
                        Integer.class,
                        seriesId);
        final Integer secondOccurrenceIndex =
                jdbcTemplate.queryForObject(
                        "SELECT series_occurrence_index FROM matches WHERE id = ?",
                        Integer.class,
                        second.getId());
        final List<Match> occurrences = matchDao.findSeriesOccurrences(seriesId);

        Assertions.assertEquals(1, seriesCount);
        Assertions.assertEquals(2, occurrencesCount);
        Assertions.assertEquals(2, secondOccurrenceIndex);
        Assertions.assertEquals(2, occurrences.size());
        Assertions.assertEquals(first.getId(), occurrences.get(0).getId());
        Assertions.assertEquals(second.getId(), occurrences.get(1).getId());
        Assertions.assertEquals(seriesId, occurrences.get(0).getSeriesId());
        Assertions.assertEquals(1, occurrences.get(0).getSeriesOccurrenceIndex());
    }

    @Test
    public void shouldFailToCreateRecurringOccurrenceWhenSeriesOccurrenceIndexIsMissing() {
        final ZonedDateTime startsAt = ZonedDateTime.now().plusDays(1);
        final ZonedDateTime endsAt = startsAt.plusMinutes(90);
        final Long seriesId =
                matchDao.createMatchSeries(
                        hostUserId,
                        "weekly",
                        startsAt.toInstant(),
                        endsAt.toInstant(),
                        ZoneId.systemDefault().getId(),
                        null,
                        2);

        Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        matchDao.createMatch(
                                hostUserId,
                                "Stadium A",
                                "Incomplete Weekly Tennis",
                                "Open match",
                                startsAt.toInstant(),
                                endsAt.toInstant(),
                                4,
                                BigDecimal.ZERO,
                                Sport.TENNIS,
                                EventVisibility.PUBLIC,
                                EventJoinPolicy.DIRECT,
                                EventStatus.OPEN,
                                null,
                                seriesId,
                                null));

        final Integer persistedRows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM matches WHERE title = 'Incomplete Weekly Tennis'",
                        Integer.class);
        Assertions.assertEquals(0, persistedRows);
    }

    @Test
    public void testFindPublicEventsBySearchText() {
        final long namedHostId = createUser("serena-host", "serena-host@test.com");
        matchDao.createMatch(
                namedHostId,
                "River Court",
                "Morning Football",
                "Fast 5v5 match",
                ZonedDateTime.now().plusDays(1).toInstant(),
                null,
                10,
                BigDecimal.ZERO,
                Sport.FOOTBALL,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT,
                EventStatus.OPEN,
                null);
        insertMatch(
                "Basketball Session",
                "Stretching",
                "basketball",
                10,
                0,
                ZonedDateTime.now().plusDays(1));

        for (final String query : List.of("football", "fast", "river", "serena")) {
            final List<Match> result = findPublicMatchesByQuery(query);

            Assertions.assertEquals(1, result.size(), query);
            Assertions.assertEquals("Morning Football", result.get(0).getTitle(), query);
        }
    }

    @Test
    public void testFindPublicEventsIncludesApprovalRequiredVisibility() {
        matchDao.createMatch(
                hostUserId,
                "Test Address",
                "Approval Required Match",
                "Description",
                ZonedDateTime.now().plusDays(1).toInstant(),
                null,
                10,
                BigDecimal.ZERO,
                Sport.FOOTBALL,
                EventVisibility.PUBLIC,
                EventJoinPolicy.APPROVAL_REQUIRED,
                EventStatus.OPEN,
                null);

        insertMatchWithStatus(
                "Legacy Invite Only Match",
                EventStatus.OPEN,
                ZonedDateTime.now().plusDays(1),
                hostUserId,
                EventVisibility.PRIVATE,
                EventJoinPolicy.INVITE_ONLY);
        insertMatchWithStatus(
                "Private Match",
                EventStatus.OPEN,
                ZonedDateTime.now().plusDays(1),
                hostUserId,
                EventVisibility.PRIVATE,
                EventJoinPolicy.INVITE_ONLY);

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

        Assertions.assertTrue(
                result.stream()
                        .anyMatch(match -> "Approval Required Match".equals(match.getTitle())));
        Assertions.assertTrue(
                result.stream()
                        .noneMatch(match -> "Legacy Invite Only Match".equals(match.getTitle())));
        Assertions.assertTrue(
                result.stream().noneMatch(match -> "Private Match".equals(match.getTitle())));
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
    public void testFindMatchByIdIncludesAvailability() {
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
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);

        final Match found = matchDao.findMatchById(created.getId()).orElseThrow();

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
    public void testFindMatchByIdIncludesJoinedPlayersForNonPublicMatch() {
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
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        null);

        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, created_at, updated_at)"
                        + " VALUES (2, 'player', 'player@test.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (?, 2, 'joined', CURRENT_TIMESTAMP)",
                created.getId());

        final Match found = matchDao.findMatchById(created.getId()).orElseThrow();

        Assertions.assertEquals(created.getId(), found.getId());
        Assertions.assertEquals(EventVisibility.PRIVATE, found.getVisibility());
        Assertions.assertEquals(1, found.getJoinedPlayers());
    }

    @Test
    public void shouldUpdateMatchRowWhenHostOwnsTheMatch() {
        final Match created =
                matchDao.createMatch(
                        hostUserId,
                        "Original Address",
                        "Original Title",
                        "Original Description",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);

        final boolean updated =
                matchDao.updateMatch(
                        created.getId(),
                        hostUserId,
                        "Updated Address",
                        "Updated Title",
                        "Updated Description",
                        ZonedDateTime.now().plusDays(2).toInstant(),
                        ZonedDateTime.now().plusDays(2).plusHours(2).toInstant(),
                        10,
                        new BigDecimal("15"),
                        Sport.TENNIS,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        null);

        final Map<String, Object> row =
                jdbcTemplate.queryForMap(
                        "SELECT address, title, description, sport, visibility, join_policy,"
                                + " max_players, price_per_player"
                                + " FROM matches WHERE id = ?",
                        created.getId());
        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertTrue(updated);
        Assertions.assertEquals("Updated Address", row.get("address"));
        Assertions.assertEquals("Updated Title", row.get("title"));
        Assertions.assertEquals("Updated Description", row.get("description"));
        Assertions.assertEquals("tennis", row.get("sport"));
        Assertions.assertEquals("private", row.get("visibility"));
        Assertions.assertEquals("invite_only", row.get("join_policy"));
        Assertions.assertEquals(10, ((Number) row.get("max_players")).intValue());
        Assertions.assertEquals(new BigDecimal("15.00"), row.get("price_per_player"));
        Assertions.assertEquals("Updated Address", found.getAddress());
        Assertions.assertEquals("Updated Title", found.getTitle());
        Assertions.assertEquals("Updated Description", found.getDescription());
        Assertions.assertEquals(Sport.TENNIS, found.getSport());
        Assertions.assertEquals(EventVisibility.PRIVATE, found.getVisibility());
        Assertions.assertEquals(EventJoinPolicy.INVITE_ONLY, found.getJoinPolicy());
        Assertions.assertEquals(10, found.getMaxPlayers());
        Assertions.assertEquals(new BigDecimal("15.00"), found.getPricePerPlayer());
    }

    @Test
    public void testUpdateMatchRejectsWrongHostUserId() {
        final Match created =
                matchDao.createMatch(
                        hostUserId,
                        "Original Address",
                        "Original Title",
                        "Original Description",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);

        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, created_at, updated_at)"
                        + " VALUES (2, 'other-host', 'other-host@test.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        final boolean updated =
                matchDao.updateMatch(
                        created.getId(),
                        2L,
                        "Updated Address",
                        "Updated Title",
                        "Updated Description",
                        ZonedDateTime.now().plusDays(2).toInstant(),
                        ZonedDateTime.now().plusDays(2).plusHours(2).toInstant(),
                        10,
                        new BigDecimal("15"),
                        Sport.TENNIS,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);

        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertFalse(updated);
        Assertions.assertEquals("Original Address", found.getAddress());
        Assertions.assertEquals("Original Title", found.getTitle());
        Assertions.assertEquals(Sport.FOOTBALL, found.getSport());
        Assertions.assertEquals(8, found.getMaxPlayers());
    }

    @Test
    public void shouldCancelMatchInDatabaseWhenHostOwnsTheMatch() {
        final Match created =
                matchDao.createMatch(
                        hostUserId,
                        "Original Address",
                        "Original Title",
                        "Original Description",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);

        final boolean cancelled = matchDao.cancelMatch(created.getId(), hostUserId);

        final String statusInDb =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM matches WHERE id = ?", String.class, created.getId());
        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertTrue(cancelled);
        Assertions.assertEquals("cancelled", statusInDb);
        Assertions.assertEquals(EventStatus.CANCELLED, found.getStatus());
    }

    @Test
    public void testCancelMatchCancelsOnlySelectedRecurringOccurrence() {
        final ZonedDateTime startsAt = ZonedDateTime.now().plusDays(1);
        final ZonedDateTime endsAt = startsAt.plusMinutes(90);
        final Long seriesId =
                matchDao.createMatchSeries(
                        hostUserId,
                        "weekly",
                        startsAt.toInstant(),
                        endsAt.toInstant(),
                        ZoneId.systemDefault().getId(),
                        null,
                        2);
        final Match firstOccurrence =
                matchDao.createMatch(
                        hostUserId,
                        "Original Address",
                        "Weekly Padel",
                        "First occurrence",
                        startsAt.toInstant(),
                        endsAt.toInstant(),
                        8,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        seriesId,
                        1);
        final Match secondOccurrence =
                matchDao.createMatch(
                        hostUserId,
                        "Original Address",
                        "Weekly Padel",
                        "Second occurrence",
                        startsAt.plusWeeks(1).toInstant(),
                        endsAt.plusWeeks(1).toInstant(),
                        8,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        seriesId,
                        2);

        final boolean cancelled = matchDao.cancelMatch(secondOccurrence.getId(), hostUserId);

        final List<Match> occurrences = matchDao.findSeriesOccurrences(seriesId);
        Assertions.assertTrue(cancelled);
        Assertions.assertEquals(firstOccurrence.getId(), occurrences.get(0).getId());
        Assertions.assertEquals(EventStatus.OPEN, occurrences.get(0).getStatus());
        Assertions.assertEquals(secondOccurrence.getId(), occurrences.get(1).getId());
        Assertions.assertEquals(EventStatus.CANCELLED, occurrences.get(1).getStatus());
    }

    @Test
    public void testCancelMatchRejectsWrongHostUserId() {
        final Match created =
                matchDao.createMatch(
                        hostUserId,
                        "Original Address",
                        "Original Title",
                        "Original Description",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);

        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, created_at, updated_at)"
                        + " VALUES (2, 'other-host', 'other-host@test.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        final boolean cancelled = matchDao.cancelMatch(created.getId(), 2L);

        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertFalse(cancelled);
        Assertions.assertEquals(EventStatus.OPEN, found.getStatus());
    }

    @Test
    public void testCancelMatchUpdatesTimestamp() {
        final Match created =
                matchDao.createMatch(
                        hostUserId,
                        "Original Address",
                        "Original Title",
                        "Original Description",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);

        jdbcTemplate.update(
                "UPDATE matches SET updated_at = TIMESTAMPADD(SECOND, -5, updated_at) WHERE id = ?",
                created.getId());

        final Timestamp beforeCancel =
                jdbcTemplate.queryForObject(
                        "SELECT updated_at FROM matches WHERE id = ?",
                        Timestamp.class,
                        created.getId());

        final boolean cancelled = matchDao.cancelMatch(created.getId(), hostUserId);

        final Timestamp afterUpdate =
                jdbcTemplate.queryForObject(
                        "SELECT updated_at FROM matches WHERE id = ?",
                        Timestamp.class,
                        created.getId());

        Assertions.assertTrue(cancelled);
        Assertions.assertNotNull(beforeCancel);
        Assertions.assertNotNull(afterUpdate);
        Assertions.assertTrue(afterUpdate.after(beforeCancel));
    }

    @Test
    public void testFindHostedMatchesReturnsAllStatusesOrderedBySoonest() {
        insertMatchWithStatus(
                "Host Draft",
                EventStatus.DRAFT,
                ZonedDateTime.now().plusDays(2),
                hostUserId,
                EventVisibility.PRIVATE,
                EventJoinPolicy.INVITE_ONLY);
        insertMatchWithStatus(
                "Host Completed",
                EventStatus.COMPLETED,
                ZonedDateTime.now().plusDays(1),
                hostUserId,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT);
        insertMatchWithStatus(
                "Host Open",
                EventStatus.OPEN,
                ZonedDateTime.now().plusDays(3),
                hostUserId,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT);

        final List<Match> matches =
                matchDao.findHostedMatches(
                        hostUserId,
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(3, matches.size());
        Assertions.assertEquals("Host Completed", matches.get(0).getTitle());
        Assertions.assertEquals("Host Draft", matches.get(1).getTitle());
        Assertions.assertEquals("Host Open", matches.get(2).getTitle());
        Assertions.assertEquals(
                3,
                matchDao.countHostedMatches(
                        hostUserId,
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        ZoneId.systemDefault()));
    }

    @Test
    public void testFindHostedMatchesFiltersFinishedStatuses() {
        insertMatchWithStatus(
                "Host Completed",
                EventStatus.COMPLETED,
                ZonedDateTime.now().minusDays(2),
                hostUserId,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT);
        insertMatchWithStatus(
                "Host Cancelled",
                EventStatus.CANCELLED,
                ZonedDateTime.now().minusDays(1),
                hostUserId,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT);
        insertMatchWithStatus(
                "Host Open Past",
                EventStatus.OPEN,
                ZonedDateTime.now().minusHours(2),
                hostUserId,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT);
        insertMatchWithStatus(
                "Host Open",
                EventStatus.OPEN,
                ZonedDateTime.now().plusDays(1),
                hostUserId,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT);

        final List<Match> finished =
                matchDao.findHostedMatches(
                        hostUserId,
                        null,
                        null,
                        List.of(),
                        null,
                        List.of(EventStatus.COMPLETED, EventStatus.CANCELLED),
                        null,
                        null,
                        null,
                        null,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(3, finished.size());
        Assertions.assertTrue(
                finished.stream()
                        .allMatch(
                                match ->
                                        EventStatus.COMPLETED.equals(match.getStatus())
                                                || EventStatus.CANCELLED.equals(
                                                        match.getStatus())));
        Assertions.assertTrue(
                finished.stream().anyMatch(match -> "Host Open Past".equals(match.getTitle())));
        Assertions.assertEquals(
                3,
                matchDao.countHostedMatches(
                        hostUserId,
                        null,
                        null,
                        List.of(),
                        null,
                        List.of(EventStatus.COMPLETED, EventStatus.CANCELLED),
                        null,
                        null,
                        null,
                        ZoneId.systemDefault()));
    }

    @Test
    public void testFindUpcomingJoinedMatchesIncludesCancelledEvents() {
        final long playerId = createUser("joined-player", "joined-player@test.com");
        final Match openMatch =
                insertMatchWithStatus(
                        "Open Future",
                        EventStatus.OPEN,
                        ZonedDateTime.now().plusDays(2),
                        hostUserId,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT);
        final Match cancelledMatch =
                insertMatchWithStatus(
                        "Cancelled Future",
                        EventStatus.CANCELLED,
                        ZonedDateTime.now().plusDays(3),
                        hostUserId,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT);
        joinMatch(openMatch.getId(), playerId, "joined");
        joinMatch(cancelledMatch.getId(), playerId, "joined");

        final List<Match> upcoming =
                matchDao.findJoinedMatches(
                        playerId,
                        Boolean.TRUE,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(2, upcoming.size());
        Assertions.assertTrue(
                upcoming.stream()
                        .anyMatch(match -> EventStatus.CANCELLED.equals(match.getStatus())));
        Assertions.assertEquals(
                2,
                matchDao.countJoinedMatches(
                        playerId,
                        Boolean.TRUE,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        ZoneId.systemDefault()));
    }

    @Test
    public void testFindPastJoinedMatchesReturnsOnlyPastInDescendingOrder() {
        final long playerId = createUser("past-player", "past-player@test.com");
        final Match olderPast =
                insertMatchWithStatus(
                        "Older Past",
                        EventStatus.COMPLETED,
                        ZonedDateTime.now().minusDays(4),
                        hostUserId,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT);
        final Match newerPast =
                insertMatchWithStatus(
                        "Newer Past",
                        EventStatus.OPEN,
                        ZonedDateTime.now().minusDays(2),
                        hostUserId,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT);
        insertMatchWithStatus(
                "Future Open",
                EventStatus.OPEN,
                ZonedDateTime.now().plusDays(2),
                hostUserId,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT);
        joinMatch(olderPast.getId(), playerId, "checked_in");
        joinMatch(newerPast.getId(), playerId, "joined");

        final List<Match> past =
                matchDao.findJoinedMatches(
                        playerId,
                        Boolean.FALSE,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(2, past.size());
        Assertions.assertEquals("Newer Past", past.get(0).getTitle());
        Assertions.assertEquals("Older Past", past.get(1).getTitle());
        Assertions.assertEquals(EventStatus.COMPLETED, past.get(0).getStatus());
        Assertions.assertEquals(
                2,
                matchDao.countJoinedMatches(
                        playerId,
                        Boolean.FALSE,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        ZoneId.systemDefault()));
    }

    @Test
    public void testFindHostedMatchesAppliesPriceFilter() {
        insertMatchWithStatus(
                "Hosted Budget",
                EventStatus.OPEN,
                ZonedDateTime.now().plusDays(2),
                hostUserId,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT,
                BigDecimal.ZERO);
        insertMatchWithStatus(
                "Hosted Premium",
                EventStatus.OPEN,
                ZonedDateTime.now().plusDays(2),
                hostUserId,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT,
                new BigDecimal("30"));

        final List<Match> result =
                matchDao.findHostedMatches(
                        hostUserId,
                        null,
                        null,
                        List.of(),
                        null,
                        List.of(EventStatus.OPEN),
                        null,
                        new BigDecimal("20"),
                        new BigDecimal("40"),
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Hosted Premium", result.get(0).getTitle());
    }

    @Test
    public void testFindUpcomingJoinedMatchesAppliesPriceFilter() {
        final long playerId = createUser("price-player", "price-player@test.com");
        final Match budget =
                insertMatchWithStatus(
                        "Upcoming Budget",
                        EventStatus.OPEN,
                        ZonedDateTime.now().plusDays(2),
                        hostUserId,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        BigDecimal.ZERO);
        final Match premium =
                insertMatchWithStatus(
                        "Upcoming Premium",
                        EventStatus.OPEN,
                        ZonedDateTime.now().plusDays(2),
                        hostUserId,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        new BigDecimal("25"));
        joinMatch(budget.getId(), playerId, "joined");
        joinMatch(premium.getId(), playerId, "joined");

        final List<Match> result =
                matchDao.findJoinedMatches(
                        playerId,
                        Boolean.TRUE,
                        null,
                        List.of(),
                        null,
                        null,
                        EventTimeFilter.WEEK,
                        new BigDecimal("20"),
                        new BigDecimal("30"),
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Upcoming Premium", result.get(0).getTitle());
    }

    @Test
    public void testCreateFindAndUpdateMatchRoundTripsCoordinates() {
        final ZonedDateTime startsAt = ZonedDateTime.now().plusDays(1);
        final Match created =
                matchDao.createMatch(
                        hostUserId,
                        "Court Address",
                        "Pinned Match",
                        "Open match",
                        startsAt.toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        -34.61,
                        -58.38);

        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertEquals(-34.61, found.getLatitude());
        Assertions.assertEquals(-58.38, found.getLongitude());

        final boolean updated =
                matchDao.updateMatch(
                        created.getId(),
                        hostUserId,
                        "New Court Address",
                        "Moved Match",
                        "Open match",
                        startsAt.plusDays(1).toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        -34.5,
                        -58.5);

        final Match moved = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertTrue(updated);
        Assertions.assertEquals(-34.5, moved.getLatitude());
        Assertions.assertEquals(-58.5, moved.getLongitude());
    }

    @Test
    public void testCreateMatchAllowsAddressOnlyCoordinates() {
        final Match created =
                matchDao.createMatch(
                        hostUserId,
                        "Address Only",
                        "Address Only Match",
                        "Open match",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);

        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertNull(found.getLatitude());
        Assertions.assertNull(found.getLongitude());
    }

    @Test
    public void testMatchCoordinateConstraintsRejectInvalidRows() {
        final ZonedDateTime startsAt = ZonedDateTime.now().plusDays(1);

        Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        matchDao.createMatch(
                                hostUserId,
                                "Invalid Address",
                                "Missing Pair",
                                "Open match",
                                startsAt.toInstant(),
                                null,
                                4,
                                BigDecimal.ZERO,
                                Sport.PADEL,
                                EventVisibility.PUBLIC,
                                EventJoinPolicy.DIRECT,
                                EventStatus.OPEN,
                                null,
                                -34.61,
                                null));
        Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        matchDao.createMatch(
                                hostUserId,
                                "Invalid Address",
                                "Out of Range",
                                "Open match",
                                startsAt.toInstant(),
                                null,
                                4,
                                BigDecimal.ZERO,
                                Sport.PADEL,
                                EventVisibility.PUBLIC,
                                EventJoinPolicy.DIRECT,
                                EventStatus.OPEN,
                                null,
                                -91.0,
                                -58.38));
    }

    @Test
    public void testFindPublicMatchesSortsByDistanceAndPlacesNullCoordinatesLast() {
        final ZonedDateTime startsAt = ZonedDateTime.now().plusDays(1);
        final Match far =
                matchDao.createMatch(
                        hostUserId,
                        "Far Address",
                        "Far Match",
                        "Open match",
                        startsAt.plusHours(1).toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        -34.8,
                        -58.8);
        final Match nearby =
                matchDao.createMatch(
                        hostUserId,
                        "Near Address",
                        "Near Match",
                        "Open match",
                        startsAt.plusHours(2).toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        -34.61,
                        -58.39);
        final Match addressOnly =
                matchDao.createMatch(
                        hostUserId,
                        "Address Only",
                        "Address Only Match",
                        "Open match",
                        startsAt.toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        (Double) null,
                        (Double) null);

        final List<Match> result =
                matchDao.findPublicMatches(
                        null,
                        List.of(Sport.PADEL),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        null,
                        null,
                        MatchSort.DISTANCE,
                        ZoneId.systemDefault(),
                        -34.6,
                        -58.4,
                        0,
                        20);

        Assertions.assertTrue(result.size() >= 3);
        Assertions.assertEquals(nearby.getId(), result.get(0).getId());
        Assertions.assertEquals(far.getId(), result.get(1).getId());
        Assertions.assertEquals(addressOnly.getId(), result.get(2).getId());
    }

    @Test
    public void shouldSoftDeleteMatchAndPersistAuditFields() {
        final Match created =
                matchDao.createMatch(
                        hostUserId,
                        "Original Address",
                        "Original Title",
                        "Original Description",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);

        final long adminId = createUser("admin", "admin@test.com");

        final boolean deleted = matchDao.softDeleteMatch(created.getId(), adminId, "Violation");

        final Map<String, Object> row =
                jdbcTemplate.queryForMap(
                        "SELECT deleted, deleted_by_user_id, delete_reason, status, deleted_at"
                                + " FROM matches WHERE id = ?",
                        created.getId());

        Assertions.assertTrue(deleted);
        Assertions.assertEquals(Boolean.TRUE, row.get("deleted"));
        Assertions.assertEquals(adminId, ((Number) row.get("deleted_by_user_id")).longValue());
        Assertions.assertEquals("Violation", row.get("delete_reason"));
        Assertions.assertEquals("cancelled", row.get("status"));
        Assertions.assertNotNull(row.get("deleted_at"));

        final Match found = matchDao.findById(created.getId()).orElseThrow();
        Assertions.assertTrue(found.isDeleted());
        Assertions.assertEquals(adminId, found.getDeletedByUserId());
        Assertions.assertEquals("Violation", found.getDeleteReason());
        Assertions.assertNotNull(found.getDeletedAt());
        Assertions.assertEquals(EventStatus.CANCELLED, found.getStatus());
    }

    @Test
    public void shouldRestoreSoftDeletedMatchAndClearAuditFields() {
        final Match created =
                matchDao.createMatch(
                        hostUserId,
                        "Original Address",
                        "Original Title",
                        "Original Description",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);
        final long adminId = createUser("admin-restore", "admin-restore@test.com");
        matchDao.softDeleteMatch(created.getId(), adminId, "Violation");

        final boolean restored = matchDao.restoreMatch(created.getId());

        final Map<String, Object> row =
                jdbcTemplate.queryForMap(
                        "SELECT deleted, deleted_by_user_id, delete_reason, deleted_at, status"
                                + " FROM matches WHERE id = ?",
                        created.getId());
        Assertions.assertTrue(restored);
        Assertions.assertEquals(Boolean.FALSE, row.get("deleted"));
        Assertions.assertNull(row.get("deleted_by_user_id"));
        Assertions.assertNull(row.get("delete_reason"));
        Assertions.assertNull(row.get("deleted_at"));
        Assertions.assertEquals("cancelled", row.get("status"));
    }

    @Test
    public void shouldReturnFalseWhenRestoringNonExistingMatch() {
        final long missingMatchId = 999_999L;

        final boolean restored = matchDao.restoreMatch(missingMatchId);

        Assertions.assertFalse(restored);
    }

    @Test
    public void shouldFindPublicMatchByIdOnlyWhenMatchIsPublicAndOpen() {
        final Match publicOpen =
                matchDao.createMatch(
                        hostUserId,
                        "Public Address",
                        "Public Open Match",
                        "Open match",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);
        final Match privateOpen =
                matchDao.createMatch(
                        hostUserId,
                        "Private Address",
                        "Private Open Match",
                        "Open match",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        null);
        final Match publicCancelled =
                matchDao.createMatch(
                        hostUserId,
                        "Cancelled Address",
                        "Public Cancelled Match",
                        "Cancelled match",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.CANCELLED,
                        null);

        final boolean foundPublicOpen =
                matchDao.findPublicMatchById(publicOpen.getId()).isPresent();
        final boolean foundPrivateOpen =
                matchDao.findPublicMatchById(privateOpen.getId()).isPresent();
        final boolean foundPublicCancelled =
                matchDao.findPublicMatchById(publicCancelled.getId()).isPresent();
        final boolean foundMissing = matchDao.findPublicMatchById(999_998L).isPresent();

        final Integer publicOpenCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM matches WHERE id = ?"
                                + " AND visibility = 'public' AND status = 'open'",
                        Integer.class,
                        publicOpen.getId());
        final Integer privateOpenCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM matches WHERE id = ? AND visibility = 'private'",
                        Integer.class,
                        privateOpen.getId());
        final Integer publicCancelledCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM matches WHERE id = ? AND status = 'cancelled'",
                        Integer.class,
                        publicCancelled.getId());

        Assertions.assertEquals(1, publicOpenCount);
        Assertions.assertEquals(1, privateOpenCount);
        Assertions.assertEquals(1, publicCancelledCount);
        Assertions.assertTrue(foundPublicOpen);
        Assertions.assertFalse(foundPrivateOpen);
        Assertions.assertFalse(foundPublicCancelled);
        Assertions.assertFalse(foundMissing);
    }

    @Test
    public void testFindPublicEventsExcludesDeletedMatches() {
        insertMatch(
                "Active Match",
                "Fast 5v5 match",
                "football",
                10,
                0,
                ZonedDateTime.now().plusDays(1));

        final Match toDelete =
                matchDao.createMatch(
                        hostUserId,
                        "Deleted Address",
                        "Deleted Match",
                        "Deleted Description",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);

        final long adminId = createUser("admin2", "admin2@test.com");
        matchDao.softDeleteMatch(toDelete.getId(), adminId, "Violation");

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
        Assertions.assertEquals("Active Match", result.get(0).getTitle());
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
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
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

    private List<Match> findPublicMatchesByQuery(final String query) {
        return matchDao.findPublicMatches(
                query,
                List.of(),
                EventTimeFilter.WEEK,
                null,
                null,
                MatchSort.SOONEST,
                ZoneId.systemDefault(),
                0,
                20);
    }

    private Match insertMatchWithStatus(
            final String title,
            final EventStatus status,
            final ZonedDateTime startsAt,
            final long hostId,
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy) {
        return insertMatchWithStatus(
                title, status, startsAt, hostId, visibility, joinPolicy, BigDecimal.ZERO);
    }

    private Match insertMatchWithStatus(
            final String title,
            final EventStatus status,
            final ZonedDateTime startsAt,
            final long hostId,
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy,
            final BigDecimal pricePerPlayer) {
        return matchDao.createMatch(
                hostId,
                "Test Address",
                title,
                "Description",
                startsAt.toInstant(),
                null,
                10,
                pricePerPlayer,
                Sport.FOOTBALL,
                visibility,
                joinPolicy,
                status,
                null);
    }

    private long createUser(final String username, final String email) {
        final long userId = System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                userId,
                username,
                email);
        return userId;
    }

    private void joinMatch(final long matchId, final long userId, final String status) {
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                matchId,
                userId,
                status);
    }
}
