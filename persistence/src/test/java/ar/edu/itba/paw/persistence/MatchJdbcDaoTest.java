package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.EventTimeFilter;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.Sport;
import java.math.BigDecimal;
import java.sql.Timestamp;
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
                "public",
                "approval_required",
                "open",
                null);

        insertMatchWithStatus(
                "Legacy Invite Only Match",
                "open",
                ZonedDateTime.now().plusDays(1),
                hostUserId,
                "invite_only");
        insertMatchWithStatus(
                "Private Match", "open", ZonedDateTime.now().plusDays(1), hostUserId, "private");

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
                        "public",
                        "open",
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

        final Match found = matchDao.findMatchById(created.getId()).orElseThrow();

        Assertions.assertEquals(created.getId(), found.getId());
        Assertions.assertEquals("private", found.getVisibility());
        Assertions.assertEquals(1, found.getJoinedPlayers());
    }

    @Test
    public void testUpdateMatchUpdatesOwnedMatch() {
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
                        "public",
                        "open",
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
                        "public",
                        "open",
                        null);

        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertTrue(updated);
        Assertions.assertEquals("Updated Address", found.getAddress());
        Assertions.assertEquals("Updated Title", found.getTitle());
        Assertions.assertEquals("Updated Description", found.getDescription());
        Assertions.assertEquals(Sport.TENNIS, found.getSport());
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
                        "public",
                        "open",
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
                        "public",
                        "open",
                        null);

        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertFalse(updated);
        Assertions.assertEquals("Original Address", found.getAddress());
        Assertions.assertEquals("Original Title", found.getTitle());
        Assertions.assertEquals(Sport.FOOTBALL, found.getSport());
        Assertions.assertEquals(8, found.getMaxPlayers());
    }

    @Test
    public void testCancelMatchCancelsOwnedMatch() {
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
                        "public",
                        "open",
                        null);

        final boolean cancelled = matchDao.cancelMatch(created.getId(), hostUserId);

        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertTrue(cancelled);
        Assertions.assertEquals("cancelled", found.getStatus());
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
                        "public",
                        "open",
                        null);

        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, created_at, updated_at)"
                        + " VALUES (2, 'other-host', 'other-host@test.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        final boolean cancelled = matchDao.cancelMatch(created.getId(), 2L);

        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertFalse(cancelled);
        Assertions.assertEquals("open", found.getStatus());
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
                        "public",
                        "open",
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
                "Host Draft", "draft", ZonedDateTime.now().plusDays(2), hostUserId, "private");
        insertMatchWithStatus(
                "Host Completed",
                "completed",
                ZonedDateTime.now().plusDays(1),
                hostUserId,
                "public");
        insertMatchWithStatus(
                "Host Open", "open", ZonedDateTime.now().plusDays(3), hostUserId, "public");

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
                "completed",
                ZonedDateTime.now().minusDays(2),
                hostUserId,
                "public");
        insertMatchWithStatus(
                "Host Cancelled",
                "cancelled",
                ZonedDateTime.now().minusDays(1),
                hostUserId,
                "public");
        insertMatchWithStatus(
                "Host Open Past", "open", ZonedDateTime.now().minusHours(2), hostUserId, "public");
        insertMatchWithStatus(
                "Host Open", "open", ZonedDateTime.now().plusDays(1), hostUserId, "public");

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
                                        EventStatus.COMPLETED
                                                        .getValue()
                                                        .equalsIgnoreCase(match.getStatus())
                                                || EventStatus.CANCELLED
                                                        .getValue()
                                                        .equalsIgnoreCase(match.getStatus())));
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
                        "open",
                        ZonedDateTime.now().plusDays(2),
                        hostUserId,
                        "public");
        final Match cancelledMatch =
                insertMatchWithStatus(
                        "Cancelled Future",
                        "cancelled",
                        ZonedDateTime.now().plusDays(3),
                        hostUserId,
                        "public");
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
                        .anyMatch(
                                match ->
                                        EventStatus.CANCELLED
                                                .getValue()
                                                .equals(match.getStatus())));
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
                        "completed",
                        ZonedDateTime.now().minusDays(4),
                        hostUserId,
                        "public");
        final Match newerPast =
                insertMatchWithStatus(
                        "Newer Past",
                        "open",
                        ZonedDateTime.now().minusDays(2),
                        hostUserId,
                        "public");
        insertMatchWithStatus(
                "Future Open", "open", ZonedDateTime.now().plusDays(2), hostUserId, "public");
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
        Assertions.assertEquals("completed", past.get(0).getStatus());
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
                "open",
                ZonedDateTime.now().plusDays(2),
                hostUserId,
                "public",
                BigDecimal.ZERO);
        insertMatchWithStatus(
                "Hosted Premium",
                "open",
                ZonedDateTime.now().plusDays(2),
                hostUserId,
                "public",
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
                        "open",
                        ZonedDateTime.now().plusDays(2),
                        hostUserId,
                        "public",
                        BigDecimal.ZERO);
        final Match premium =
                insertMatchWithStatus(
                        "Upcoming Premium",
                        "open",
                        ZonedDateTime.now().plusDays(2),
                        hostUserId,
                        "public",
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
    public void testSoftDeleteMatch() {
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
                        "public",
                        "open",
                        null);

        final long adminId = createUser("admin", "admin@test.com");
        final boolean deleted = matchDao.softDeleteMatch(created.getId(), adminId, "Violation");

        Assertions.assertTrue(deleted);

        final Match found = matchDao.findById(created.getId()).orElseThrow();
        Assertions.assertTrue(found.isDeleted());
        Assertions.assertEquals(adminId, found.getDeletedByUserId());
        Assertions.assertEquals("Violation", found.getDeleteReason());
        Assertions.assertNotNull(found.getDeletedAt());
        Assertions.assertEquals("cancelled", found.getStatus());
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
                        "public",
                        "open",
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

    private Match insertMatchWithStatus(
            final String title,
            final String status,
            final ZonedDateTime startsAt,
            final long hostId,
            final String visibility) {
        return insertMatchWithStatus(title, status, startsAt, hostId, visibility, BigDecimal.ZERO);
    }

    private Match insertMatchWithStatus(
            final String title,
            final String status,
            final ZonedDateTime startsAt,
            final long hostId,
            final String visibility,
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
