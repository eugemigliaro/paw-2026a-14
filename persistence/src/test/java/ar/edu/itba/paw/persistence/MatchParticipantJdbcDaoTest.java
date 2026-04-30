package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import java.time.Instant;
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
public class MatchParticipantJdbcDaoTest {

    @Autowired private MatchParticipantDao matchParticipantDao;

    @Autowired private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(
                "INSERT INTO images (id, content_type, content_length, content, created_at)"
                        + " VALUES (20, 'image/png', 3, CAST(X'010203' AS BINARY),"
                        + " CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO users "
                        + "(id, username, email, name, last_name, phone, profile_image_id,"
                        + " created_at, updated_at) VALUES "
                        + "(1, 'host', 'host@test.com', 'Host', 'User', null, null,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), "
                        + "(2, 'player', 'player@test.com', 'Player', 'User', '+1 555 123 4567',"
                        + " 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), "
                        + "(3, 'other', 'other@test.com', 'Other', 'User', null, null,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO matches "
                        + "(id, host_user_id, address, title, description, starts_at, max_players, "
                        + "price_per_player, visibility, status, sport, created_at, updated_at) "
                        + "VALUES (?, 1, 'Address', 'Match', 'Description', ?, 2, 0, 'public', "
                        + "'open', 'football', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                10L,
                java.sql.Timestamp.from(Instant.now().plusSeconds(86400)));
    }

    @Test
    public void testCreateReservationIfSpaceInsertsParticipant() {
        final boolean inserted = matchParticipantDao.createReservationIfSpace(10L, 2L);

        Assertions.assertTrue(inserted);
        final Integer rows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE match_id = 10 AND user_id = 2 AND status = 'joined'",
                        Integer.class);
        Assertions.assertEquals(1, rows);
    }

    @Test
    public void testCreateReservationIfSpaceRestoresInactiveParticipantRow() {
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 2, 'cancelled', CURRENT_TIMESTAMP)");

        final boolean inserted = matchParticipantDao.createReservationIfSpace(10L, 2L);

        Assertions.assertTrue(inserted);
        final String status =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM match_participants"
                                + " WHERE match_id = 10 AND user_id = 2",
                        String.class);
        Assertions.assertEquals("joined", status);
        final Integer rows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE match_id = 10 AND user_id = 2",
                        Integer.class);
        Assertions.assertEquals(1, rows);
    }

    @Test
    public void testCreateReservationIfSpaceRejectsInactiveParticipantRowWhenFull() {
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 1, 'joined', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 3, 'joined', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 2, 'cancelled', CURRENT_TIMESTAMP)");

        final boolean inserted = matchParticipantDao.createReservationIfSpace(10L, 2L);

        Assertions.assertFalse(inserted);
        final String status =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM match_participants"
                                + " WHERE match_id = 10 AND user_id = 2",
                        String.class);
        Assertions.assertEquals("cancelled", status);
    }

    @Test
    public void testCreateSeriesReservationsIfSpaceInsertsFutureEligibleOccurrences() {
        final Instant now = Instant.parse("2026-04-05T00:00:00Z");
        insertRecurringSeries(600L, now.plusSeconds(86400));
        insertRecurringMatch(30L, 600L, 1, now.plusSeconds(86400), 2, "open");
        insertRecurringMatch(31L, 600L, 2, now.plusSeconds(172800), 2, "open");
        insertRecurringMatch(32L, 600L, 3, now.minusSeconds(86400), 2, "open");
        insertRecurringMatch(33L, 600L, 4, now.plusSeconds(259200), 2, "cancelled");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (31, 3, 'joined', CURRENT_TIMESTAMP)");

        final int insertedRows = matchParticipantDao.createSeriesReservationsIfSpace(600L, 2L, now);

        Assertions.assertEquals(2, insertedRows);
        final Integer joinedRows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE user_id = 2 AND status = 'joined'"
                                + " AND match_id IN (30, 31)",
                        Integer.class);
        Assertions.assertEquals(2, joinedRows);
        final Integer skippedRows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE user_id = 2 AND match_id IN (32, 33)",
                        Integer.class);
        Assertions.assertEquals(0, skippedRows);
    }

    @Test
    public void testCreateSeriesReservationsIfSpaceRestoresCancelledReservation() {
        final Instant now = Instant.parse("2026-04-05T00:00:00Z");
        insertRecurringSeries(600L, now.plusSeconds(86400));
        insertRecurringMatch(30L, 600L, 1, now.plusSeconds(86400), 2, "open");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (30, 2, 'cancelled', CURRENT_TIMESTAMP)");

        final int insertedRows = matchParticipantDao.createSeriesReservationsIfSpace(600L, 2L, now);

        Assertions.assertEquals(1, insertedRows);
        final String status =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM match_participants"
                                + " WHERE match_id = 30 AND user_id = 2",
                        String.class);
        Assertions.assertEquals("joined", status);
    }

    @Test
    public void testCreateSeriesReservationsIfSpaceRestoresNonActiveParticipantRows() {
        final Instant now = Instant.parse("2026-04-05T00:00:00Z");
        insertRecurringSeries(600L, now.plusSeconds(86400));
        insertRecurringMatch(30L, 600L, 1, now.plusSeconds(86400), 2, "open");
        insertRecurringMatch(31L, 600L, 2, now.plusSeconds(172800), 2, "open");
        insertRecurringMatch(32L, 600L, 3, now.plusSeconds(259200), 2, "open");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (30, 2, 'pending_approval', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (31, 2, 'invited', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (32, 2, 'declined_invite', CURRENT_TIMESTAMP)");

        final int insertedRows = matchParticipantDao.createSeriesReservationsIfSpace(600L, 2L, now);

        Assertions.assertEquals(3, insertedRows);
        final Integer joinedRows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE user_id = 2 AND status = 'joined'"
                                + " AND match_id IN (30, 31, 32)",
                        Integer.class);
        final Integer participantRows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE user_id = 2 AND match_id IN (30, 31, 32)",
                        Integer.class);
        Assertions.assertEquals(3, joinedRows);
        Assertions.assertEquals(3, participantRows);
    }

    @Test
    public void testCreateSeriesReservationsIfSpaceRejectsExistingActiveReservations() {
        final Instant now = Instant.parse("2026-04-05T00:00:00Z");
        insertRecurringSeries(600L, now.plusSeconds(86400));
        insertRecurringMatch(30L, 600L, 1, now.plusSeconds(86400), 2, "open");
        insertRecurringMatch(31L, 600L, 2, now.plusSeconds(172800), 2, "open");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (30, 2, 'joined', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (31, 2, 'checked_in', CURRENT_TIMESTAMP)");

        final int insertedRows = matchParticipantDao.createSeriesReservationsIfSpace(600L, 2L, now);

        Assertions.assertEquals(0, insertedRows);
        final Integer participantRows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE user_id = 2 AND match_id IN (30, 31)",
                        Integer.class);
        final Integer activeRows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE user_id = 2 AND status IN ('joined', 'checked_in')"
                                + " AND match_id IN (30, 31)",
                        Integer.class);
        Assertions.assertEquals(2, participantRows);
        Assertions.assertEquals(2, activeRows);
    }

    @Test
    public void testFindActiveFutureReservationMatchIdsForSeriesReturnsOnlyMatchingActiveRows() {
        final Instant now = Instant.parse("2026-04-05T00:00:00Z");
        insertRecurringSeries(600L, now.plusSeconds(86400));
        insertRecurringSeries(601L, now.plusSeconds(86400));
        insertRecurringMatch(30L, 600L, 1, now.plusSeconds(86400), 4, "open");
        insertRecurringMatch(31L, 600L, 2, now.plusSeconds(172800), 4, "open");
        insertRecurringMatch(32L, 600L, 3, now.plusSeconds(259200), 4, "open");
        insertRecurringMatch(33L, 600L, 0, now.minusSeconds(86400), 4, "open");
        insertRecurringMatch(34L, 601L, 1, now.plusSeconds(86400), 4, "open");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (30, 2, 'joined', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (31, 2, 'checked_in', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (32, 2, 'cancelled', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (33, 2, 'joined', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (34, 2, 'joined', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (30, 3, 'joined', CURRENT_TIMESTAMP)");

        final List<Long> matchIds =
                matchParticipantDao.findActiveFutureReservationMatchIdsForSeries(600L, 2L, now);

        Assertions.assertEquals(List.of(30L, 31L), matchIds);
    }

    @Test
    public void testCreateReservationIfSpaceRestoresOccurrenceAfterSeriesCancellation() {
        final Instant startsAfter = Instant.now().minusSeconds(3600);
        insertRecurringSeries(600L, startsAfter.plusSeconds(86400));
        insertRecurringMatch(30L, 600L, 1, startsAfter.plusSeconds(86400), 2, "open");
        insertRecurringMatch(31L, 600L, 2, startsAfter.plusSeconds(172800), 2, "open");
        final int seriesRows =
                matchParticipantDao.createSeriesReservationsIfSpace(600L, 2L, startsAfter);
        final int cancelledRows =
                matchParticipantDao.cancelFutureSeriesReservations(600L, 2L, startsAfter);

        final boolean inserted = matchParticipantDao.createReservationIfSpace(30L, 2L);

        Assertions.assertEquals(2, seriesRows);
        Assertions.assertEquals(2, cancelledRows);
        Assertions.assertTrue(inserted);
        final String selectedOccurrenceStatus =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM match_participants"
                                + " WHERE match_id = 30 AND user_id = 2",
                        String.class);
        final String otherOccurrenceStatus =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM match_participants"
                                + " WHERE match_id = 31 AND user_id = 2",
                        String.class);
        final Integer rows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE user_id = 2 AND match_id IN (30, 31)",
                        Integer.class);
        Assertions.assertEquals("joined", selectedOccurrenceStatus);
        Assertions.assertEquals("cancelled", otherOccurrenceStatus);
        Assertions.assertEquals(2, rows);
    }

    @Test
    public void testCreateSeriesReservationsIfSpaceRejectsFullOccurrences() {
        final Instant now = Instant.parse("2026-04-05T00:00:00Z");
        insertRecurringSeries(600L, now.plusSeconds(86400));
        insertRecurringMatch(30L, 600L, 1, now.plusSeconds(86400), 1, "open");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (30, 3, 'joined', CURRENT_TIMESTAMP)");

        final int insertedRows = matchParticipantDao.createSeriesReservationsIfSpace(600L, 2L, now);

        Assertions.assertEquals(0, insertedRows);
        Assertions.assertFalse(matchParticipantDao.hasActiveReservation(30L, 2L));
    }

    @Test
    public void testCreateSeriesJoinRequestIfSpaceInsertsSingleSeriesRequest() {
        final Instant now = Instant.now();
        insertRecurringSeries(601L, now.plusSeconds(86400));
        insertRecurringMatch(40L, 601L, 1, now.plusSeconds(86400), 2, "open", "approval_required");
        insertRecurringMatch(41L, 601L, 2, now.plusSeconds(172800), 2, "open", "approval_required");
        insertRecurringMatch(42L, 601L, 0, now.minusSeconds(86400), 2, "open", "approval_required");
        insertRecurringMatch(43L, 601L, 3, now.plusSeconds(259200), 2, "open", "direct");
        insertRecurringMatch(
                44L, 601L, 4, now.plusSeconds(345600), 2, "cancelled", "approval_required");
        insertRecurringMatch(45L, 601L, 5, now.plusSeconds(432000), 1, "open", "approval_required");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (45, 3, 'joined', CURRENT_TIMESTAMP)");

        final boolean requested = matchParticipantDao.createSeriesJoinRequestIfSpace(40L, 2L);

        Assertions.assertTrue(requested);
        final Integer pendingRows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE user_id = 2 AND status = 'pending_approval'"
                                + " AND series_request = TRUE"
                                + " AND match_id IN (40, 41)",
                        Integer.class);
        final Integer skippedRows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE user_id = 2 AND match_id IN (41, 42, 43, 44, 45)",
                        Integer.class);
        final List<PendingJoinRequest> hostRequests =
                matchParticipantDao.findPendingRequestsForHost(1L);
        Assertions.assertEquals(1, pendingRows);
        Assertions.assertEquals(0, skippedRows);
        Assertions.assertEquals(1, hostRequests.size());
        Assertions.assertTrue(hostRequests.get(0).isSeriesRequest());
        Assertions.assertEquals(40L, hostRequests.get(0).getMatch().getId());
        Assertions.assertEquals(2L, hostRequests.get(0).getUser().getId());
    }

    @Test
    public void testCreateSeriesJoinRequestIfSpaceRestoresCancelledRequest() {
        final Instant now = Instant.now();
        insertRecurringSeries(601L, now.plusSeconds(86400));
        insertRecurringMatch(40L, 601L, 1, now.plusSeconds(86400), 2, "open", "approval_required");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (40, 2, 'cancelled', CURRENT_TIMESTAMP)");

        final boolean requested = matchParticipantDao.createSeriesJoinRequestIfSpace(40L, 2L);

        Assertions.assertTrue(requested);
        final String status =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM match_participants"
                                + " WHERE match_id = 40 AND user_id = 2",
                        String.class);
        final Boolean seriesRequest =
                jdbcTemplate.queryForObject(
                        "SELECT series_request FROM match_participants"
                                + " WHERE match_id = 40 AND user_id = 2",
                        Boolean.class);
        Assertions.assertEquals("pending_approval", status);
        Assertions.assertTrue(seriesRequest);
    }

    @Test
    public void testApproveSeriesJoinRequestExpandsOnePendingRequestToFutureOccurrences() {
        final Instant now = Instant.now();
        insertRecurringSeries(601L, now.plusSeconds(86400));
        insertRecurringMatch(40L, 601L, 1, now.plusSeconds(86400), 2, "open", "approval_required");
        insertRecurringMatch(41L, 601L, 2, now.plusSeconds(172800), 2, "open", "approval_required");
        insertRecurringMatch(42L, 601L, 3, now.plusSeconds(259200), 1, "open", "approval_required");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (40, 2, 'pending_approval', CURRENT_TIMESTAMP, TRUE)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (42, 3, 'joined', CURRENT_TIMESTAMP)");

        final int approvedRows =
                matchParticipantDao.approveSeriesJoinRequest(601L, 2L, now.minusSeconds(60));

        Assertions.assertEquals(2, approvedRows);
        final Integer joinedRows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE user_id = 2 AND status = 'joined'"
                                + " AND series_request = FALSE"
                                + " AND match_id IN (40, 41)",
                        Integer.class);
        final Integer pendingRows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE user_id = 2 AND status = 'pending_approval'",
                        Integer.class);
        final Integer skippedRows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE user_id = 2 AND match_id = 42",
                        Integer.class);
        Assertions.assertEquals(2, joinedRows);
        Assertions.assertEquals(0, pendingRows);
        Assertions.assertEquals(0, skippedRows);
    }

    @Test
    public void testCancelFutureSeriesReservationsCancelsOnlyFutureActiveReservations() {
        final Instant now = Instant.parse("2026-04-05T00:00:00Z");
        insertRecurringSeries(600L, now.plusSeconds(86400));
        insertRecurringMatch(30L, 600L, 1, now.plusSeconds(86400), 4, "open");
        insertRecurringMatch(31L, 600L, 2, now.plusSeconds(172800), 4, "open");
        insertRecurringMatch(32L, 600L, 0, now.minusSeconds(86400), 4, "open");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (30, 2, 'joined', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (31, 2, 'checked_in', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (32, 2, 'joined', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (31, 3, 'joined', CURRENT_TIMESTAMP)");

        final int cancelledRows = matchParticipantDao.cancelFutureSeriesReservations(600L, 2L, now);

        Assertions.assertEquals(2, cancelledRows);
        Assertions.assertFalse(matchParticipantDao.hasActiveReservation(30L, 2L));
        Assertions.assertFalse(matchParticipantDao.hasActiveReservation(31L, 2L));
        Assertions.assertTrue(matchParticipantDao.hasActiveReservation(32L, 2L));
        Assertions.assertTrue(matchParticipantDao.hasActiveReservation(31L, 3L));
    }

    @Test
    public void testCancelFutureSeriesReservationsReturnsZeroWithoutFutureReservations() {
        final Instant now = Instant.parse("2026-04-05T00:00:00Z");
        insertRecurringSeries(600L, now.plusSeconds(86400));
        insertRecurringMatch(30L, 600L, 1, now.plusSeconds(86400), 4, "open");

        final int cancelledRows = matchParticipantDao.cancelFutureSeriesReservations(600L, 2L, now);

        Assertions.assertEquals(0, cancelledRows);
    }

    @Test
    public void testCreateReservationIfSpaceRejectsDuplicateReservation() {
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 2, 'joined', CURRENT_TIMESTAMP)");

        final boolean inserted = matchParticipantDao.createReservationIfSpace(10L, 2L);

        Assertions.assertFalse(inserted);
    }

    @Test
    public void testCreateReservationIfSpaceRejectsFullMatch() {
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 2, 'joined', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 3, 'joined', CURRENT_TIMESTAMP)");

        final boolean inserted = matchParticipantDao.createReservationIfSpace(10L, 1L);

        Assertions.assertFalse(inserted);
    }

    @Test
    public void testHasActiveReservationDetectsJoinedParticipant() {
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 2, 'joined', CURRENT_TIMESTAMP)");

        Assertions.assertTrue(matchParticipantDao.hasActiveReservation(10L, 2L));
    }

    @Test
    public void testHasActiveReservationIgnoresCancelledParticipant() {
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 2, 'cancelled', CURRENT_TIMESTAMP)");

        Assertions.assertFalse(matchParticipantDao.hasActiveReservation(10L, 2L));
    }

    @Test
    public void testRemoveParticipantCancelsActiveReservation() {
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 2, 'joined', CURRENT_TIMESTAMP)");

        final boolean cancelled = matchParticipantDao.removeParticipant(10L, 2L);

        Assertions.assertTrue(cancelled);
        Assertions.assertFalse(matchParticipantDao.hasActiveReservation(10L, 2L));
        final String status =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM match_participants"
                                + " WHERE match_id = 10 AND user_id = 2",
                        String.class);
        Assertions.assertEquals("cancelled", status);
    }

    @Test
    public void testRemoveParticipantCancelsPrivateInviteOnlyActiveReservation() {
        jdbcTemplate.update(
                "UPDATE matches SET visibility = 'private', join_policy = 'invite_only'"
                        + " WHERE id = 10");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 2, 'joined', CURRENT_TIMESTAMP)");

        final boolean cancelled = matchParticipantDao.removeParticipant(10L, 2L);

        Assertions.assertTrue(cancelled);
        Assertions.assertFalse(matchParticipantDao.hasActiveReservation(10L, 2L));
    }

    @Test
    public void testFindConfirmedParticipantsReturnsJoinedUsersInJoinOrder() {
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 1, 'joined', TIMESTAMP '2026-04-06 10:00:00')");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 3, 'checked_in', TIMESTAMP '2026-04-06 11:00:00')");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 2, 'joined', TIMESTAMP '2026-04-06 09:00:00')");

        final List<User> participants = matchParticipantDao.findConfirmedParticipants(10L);

        Assertions.assertEquals(3, participants.size());
        Assertions.assertEquals(2L, participants.get(0).getId());
        Assertions.assertEquals("Player", participants.get(0).getName());
        Assertions.assertEquals("User", participants.get(0).getLastName());
        Assertions.assertEquals("+1 555 123 4567", participants.get(0).getPhone());
        Assertions.assertEquals(20L, participants.get(0).getProfileImageId());
        Assertions.assertEquals(1L, participants.get(1).getId());
        Assertions.assertEquals(3L, participants.get(2).getId());
    }

    @Test
    public void testFindConfirmedParticipantsExcludesCancelledUsersAndIncludesHost() {
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 1, 'joined', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 2, 'cancelled', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 3, 'joined', CURRENT_TIMESTAMP)");

        final List<User> participants = matchParticipantDao.findConfirmedParticipants(10L);

        Assertions.assertEquals(2, participants.size());
        Assertions.assertEquals(1L, participants.get(0).getId());
        Assertions.assertEquals(3L, participants.get(1).getId());
    }

    @Test
    public void testCreateJoinRequestAllowsReRequestAfterCancellation() {
        final boolean firstRequestCreated = matchParticipantDao.createJoinRequest(10L, 2L);
        Assertions.assertTrue(firstRequestCreated);

        final boolean cancelled = matchParticipantDao.cancelJoinRequest(10L, 2L);
        Assertions.assertTrue(cancelled);

        final boolean secondRequestCreated = matchParticipantDao.createJoinRequest(10L, 2L);
        Assertions.assertTrue(secondRequestCreated);

        final Integer pendingRows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE match_id = 10 AND user_id = 2"
                                + " AND status = 'pending_approval'",
                        Integer.class);
        Assertions.assertEquals(1, pendingRows);
    }

    private void insertRecurringSeries(final Long seriesId, final Instant startsAt) {
        jdbcTemplate.update(
                "INSERT INTO match_series"
                        + " (id, host_user_id, frequency, starts_at, timezone, occurrence_count,"
                        + " created_at, updated_at)"
                        + " VALUES (?, 1, 'weekly', ?, 'UTC', 4, CURRENT_TIMESTAMP,"
                        + " CURRENT_TIMESTAMP)",
                seriesId,
                java.sql.Timestamp.from(startsAt));
    }

    private void insertRecurringMatch(
            final Long matchId,
            final Long seriesId,
            final int occurrenceIndex,
            final Instant startsAt,
            final int maxPlayers,
            final String status) {
        insertRecurringMatch(
                matchId, seriesId, occurrenceIndex, startsAt, maxPlayers, status, "direct");
    }

    private void insertRecurringMatch(
            final Long matchId,
            final Long seriesId,
            final int occurrenceIndex,
            final Instant startsAt,
            final int maxPlayers,
            final String status,
            final String joinPolicy) {
        jdbcTemplate.update(
                "INSERT INTO matches "
                        + "(id, host_user_id, address, title, description, starts_at,"
                        + " max_players, price_per_player, visibility, join_policy, status, sport,"
                        + " series_id, series_occurrence_index, created_at, updated_at) "
                        + "VALUES (?, 1, 'Address', 'Match', 'Description', ?, ?, 0, "
                        + "'public', ?, ?, 'football', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                matchId,
                java.sql.Timestamp.from(startsAt),
                maxPlayers,
                joinPolicy,
                status,
                seriesId,
                occurrenceIndex);
    }
}
