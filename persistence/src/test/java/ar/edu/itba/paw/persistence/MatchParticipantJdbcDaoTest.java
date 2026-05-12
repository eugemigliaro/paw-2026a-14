package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@Disabled
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
    public void testCreateReservationIfSpaceAllowsHostForInviteOnlyMatch() {
        jdbcTemplate.update(
                "UPDATE matches SET visibility = 'private', join_policy = 'invite_only'"
                        + " WHERE id = 10");

        final boolean inserted = matchParticipantDao.createReservationIfSpace(10L, 1L);

        Assertions.assertTrue(inserted);
        Assertions.assertTrue(matchParticipantDao.hasActiveReservation(10L, 1L));
    }

    @Test
    public void testCreateReservationIfSpaceRejectsNonHostForInviteOnlyMatch() {
        jdbcTemplate.update(
                "UPDATE matches SET visibility = 'private', join_policy = 'invite_only'"
                        + " WHERE id = 10");

        final boolean inserted = matchParticipantDao.createReservationIfSpace(10L, 2L);

        Assertions.assertFalse(inserted);
        Assertions.assertFalse(matchParticipantDao.hasActiveReservation(10L, 2L));
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
    public void testCreateSeriesReservationsIfSpaceAllowsHostForApprovalRequiredOccurrences() {
        final Instant now = Instant.parse("2026-04-05T00:00:00Z");
        insertRecurringSeries(600L, now.plusSeconds(86400));
        insertRecurringMatch(30L, 600L, 1, now.plusSeconds(86400), 2, "open", "approval_required");
        insertRecurringMatch(31L, 600L, 2, now.plusSeconds(172800), 2, "open", "approval_required");

        final int insertedRows = matchParticipantDao.createSeriesReservationsIfSpace(600L, 1L, now);

        Assertions.assertEquals(2, insertedRows);
        Assertions.assertTrue(matchParticipantDao.hasActiveReservation(30L, 1L));
        Assertions.assertTrue(matchParticipantDao.hasActiveReservation(31L, 1L));
    }

    @Test
    public void testCreateSeriesReservationsIfSpaceRejectsNonHostForApprovalRequiredOccurrences() {
        final Instant now = Instant.parse("2026-04-05T00:00:00Z");
        insertRecurringSeries(600L, now.plusSeconds(86400));
        insertRecurringMatch(30L, 600L, 1, now.plusSeconds(86400), 2, "open", "approval_required");
        insertRecurringMatch(31L, 600L, 2, now.plusSeconds(172800), 2, "open", "approval_required");

        final int insertedRows = matchParticipantDao.createSeriesReservationsIfSpace(600L, 2L, now);

        Assertions.assertEquals(0, insertedRows);
        Assertions.assertFalse(matchParticipantDao.hasActiveReservation(30L, 2L));
        Assertions.assertFalse(matchParticipantDao.hasActiveReservation(31L, 2L));
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
    public void testFindPendingFutureRequestMatchIdsForSeriesReturnsOnlyMatchingPendingRows() {
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
                        + " VALUES (30, 2, 'pending_approval', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (31, 2, 'pending_approval', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (32, 2, 'joined', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (33, 2, 'pending_approval', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (34, 2, 'pending_approval', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (30, 3, 'pending_approval', CURRENT_TIMESTAMP)");

        final List<Long> matchIds =
                matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(600L, 2L, now);

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
        insertRecurringMatch(39L, 601L, 0, now.minusSeconds(86400), 2, "open", "approval_required");
        insertRecurringMatch(40L, 601L, 1, now.plusSeconds(86400), 2, "open", "approval_required");
        insertRecurringMatch(41L, 601L, 2, now.plusSeconds(172800), 2, "open", "approval_required");
        insertRecurringMatch(42L, 601L, 3, now.plusSeconds(259200), 1, "open", "approval_required");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (39, 2, 'pending_approval', CURRENT_TIMESTAMP, TRUE)");
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
    public void testHasPendingSeriesRequestIgnoresStaleAnchors() {
        final Instant now = Instant.now();
        insertRecurringSeries(602L, now.plusSeconds(86400));
        insertRecurringMatch(43L, 602L, 1, now.minusSeconds(86400), 4, "open", "approval_required");
        insertRecurringMatch(
                44L, 602L, 2, now.plusSeconds(86400), 4, "cancelled", "approval_required");
        insertRecurringMatch(45L, 602L, 3, now.plusSeconds(172800), 4, "open", "approval_required");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (43, 2, 'pending_approval', CURRENT_TIMESTAMP, TRUE)");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (44, 2, 'pending_approval', CURRENT_TIMESTAMP, TRUE)");

        Assertions.assertFalse(matchParticipantDao.hasPendingSeriesRequest(602L, 2L));

        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (45, 2, 'pending_approval', CURRENT_TIMESTAMP, TRUE)");

        Assertions.assertTrue(matchParticipantDao.hasPendingSeriesRequest(602L, 2L));
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
    public void testFindInvitedMatchIdsCollapsesSeriesInvitationsAndKeepsSingleInvites() {
        final Instant now = Instant.now();
        insertRecurringSeries(700L, now.plusSeconds(86400));
        insertRecurringMatch(70L, 700L, 1, now.plusSeconds(86400), 4, "open", "invite_only");
        insertRecurringMatch(71L, 700L, 2, now.plusSeconds(172800), 4, "open", "invite_only");
        insertRecurringMatch(72L, 700L, 3, now.plusSeconds(259200), 4, "open", "invite_only");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (70, 2, 'invited', TIMESTAMP '2026-04-06 10:00:00', TRUE)");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (71, 2, 'invited', TIMESTAMP '2026-04-06 10:00:00', TRUE)");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (72, 2, 'invited', TIMESTAMP '2026-04-06 11:00:00', FALSE)");

        final List<Long> invitedMatchIds = matchParticipantDao.findInvitedMatchIds(2L);

        Assertions.assertEquals(List.of(70L, 72L), invitedMatchIds);
    }

    @Test
    public void testFindInvitedMatchIdsUsesNextFutureSeriesInvitationAnchor() {
        final Instant now = Instant.now();
        insertRecurringSeries(701L, now.plusSeconds(86400));
        insertRecurringMatch(73L, 701L, 1, now.minusSeconds(86400), 4, "open", "invite_only");
        insertRecurringMatch(74L, 701L, 2, now.plusSeconds(86400), 4, "open", "invite_only");
        insertRecurringMatch(75L, 701L, 3, now.plusSeconds(172800), 4, "open", "invite_only");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (73, 2, 'invited', TIMESTAMP '2026-04-06 10:00:00', TRUE)");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (74, 2, 'invited', TIMESTAMP '2026-04-06 10:00:00', TRUE)");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (75, 2, 'invited', TIMESTAMP '2026-04-06 11:00:00', TRUE)");

        final List<Long> invitedMatchIds = matchParticipantDao.findInvitedMatchIds(2L);

        Assertions.assertEquals(List.of(74L), invitedMatchIds);
    }

    @Test
    public void testIsSeriesInvitationUsesSeriesRequestFlag() {
        final Instant now = Instant.now();
        insertRecurringSeries(700L, now.plusSeconds(86400));
        insertRecurringMatch(70L, 700L, 1, now.plusSeconds(86400), 4, "open", "invite_only");
        insertRecurringMatch(71L, 700L, 2, now.plusSeconds(172800), 4, "open", "invite_only");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (70, 2, 'invited', CURRENT_TIMESTAMP, FALSE)");

        Assertions.assertFalse(matchParticipantDao.isSeriesInvitation(70L, 2L));

        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (71, 2, 'invited', CURRENT_TIMESTAMP, FALSE)");

        Assertions.assertFalse(matchParticipantDao.isSeriesInvitation(70L, 2L));
        Assertions.assertFalse(matchParticipantDao.isSeriesInvitation(71L, 2L));

        jdbcTemplate.update(
                "UPDATE match_participants SET series_request = TRUE"
                        + " WHERE match_id = 70 AND user_id = 2");

        Assertions.assertTrue(matchParticipantDao.isSeriesInvitation(70L, 2L));
        Assertions.assertFalse(matchParticipantDao.isSeriesInvitation(71L, 2L));
    }

    @Test
    public void testAcceptSeriesInviteJoinsOnlySeriesInvitationRows() {
        final Instant now = Instant.now();
        insertRecurringSeries(700L, now.plusSeconds(86400));
        insertRecurringMatch(70L, 700L, 1, now.plusSeconds(86400), 4, "open", "invite_only");
        insertRecurringMatch(71L, 700L, 2, now.plusSeconds(172800), 4, "open", "invite_only");
        insertRecurringMatch(72L, 700L, 3, now.plusSeconds(259200), 4, "open", "invite_only");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (70, 2, 'invited', CURRENT_TIMESTAMP, TRUE)");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (71, 2, 'invited', CURRENT_TIMESTAMP, TRUE)");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (72, 2, 'invited', CURRENT_TIMESTAMP, FALSE)");

        final int acceptedRows =
                matchParticipantDao.acceptSeriesInvite(700L, 2L, now.minusSeconds(60));

        Assertions.assertEquals(2, acceptedRows);
        Assertions.assertEquals("joined", participantStatus(70L, 2L));
        Assertions.assertEquals("joined", participantStatus(71L, 2L));
        Assertions.assertEquals("invited", participantStatus(72L, 2L));
        Assertions.assertFalse(matchParticipantDao.isSeriesInvitation(70L, 2L));
    }

    @Test
    public void testAcceptSeriesInviteClearsStaleInvitationAnchor() {
        final Instant now = Instant.now();
        insertRecurringSeries(702L, now.plusSeconds(86400));
        insertRecurringMatch(76L, 702L, 1, now.minusSeconds(86400), 4, "open", "invite_only");
        insertRecurringMatch(77L, 702L, 2, now.plusSeconds(86400), 4, "open", "invite_only");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (76, 2, 'invited', CURRENT_TIMESTAMP, TRUE)");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (77, 2, 'invited', CURRENT_TIMESTAMP, TRUE)");

        final int acceptedRows =
                matchParticipantDao.acceptSeriesInvite(702L, 2L, now.minusSeconds(60));

        Assertions.assertEquals(1, acceptedRows);
        Assertions.assertEquals("declined_invite", participantStatus(76L, 2L));
        Assertions.assertEquals("joined", participantStatus(77L, 2L));
        Assertions.assertFalse(matchParticipantDao.isSeriesInvitation(76L, 2L));
    }

    @Test
    public void testDeclineSeriesInviteDeclinesOnlySeriesInvitationRows() {
        final Instant now = Instant.now();
        insertRecurringSeries(700L, now.plusSeconds(86400));
        insertRecurringMatch(70L, 700L, 1, now.plusSeconds(86400), 4, "open", "invite_only");
        insertRecurringMatch(71L, 700L, 2, now.plusSeconds(172800), 4, "open", "invite_only");
        insertRecurringMatch(72L, 700L, 3, now.plusSeconds(259200), 4, "open", "invite_only");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (70, 2, 'invited', CURRENT_TIMESTAMP, TRUE)");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (71, 2, 'invited', CURRENT_TIMESTAMP, TRUE)");
        jdbcTemplate.update(
                "INSERT INTO match_participants"
                        + " (match_id, user_id, status, joined_at, series_request)"
                        + " VALUES (72, 2, 'invited', CURRENT_TIMESTAMP, FALSE)");

        final int declinedRows = matchParticipantDao.declineSeriesInvite(700L, 2L);

        Assertions.assertEquals(2, declinedRows);
        Assertions.assertEquals("declined_invite", participantStatus(70L, 2L));
        Assertions.assertEquals("declined_invite", participantStatus(71L, 2L));
        Assertions.assertEquals("invited", participantStatus(72L, 2L));
        Assertions.assertFalse(matchParticipantDao.isSeriesInvitation(70L, 2L));
    }

    @Test
    public void testFindConfirmedParticipantsReturnsJoinedUsersInJoinOrder() {
        jdbcTemplate.update("UPDATE users SET preferred_language = 'es' WHERE id = 2");
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
        Assertions.assertEquals("es", participants.get(0).getPreferredLanguage());
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

    @Test
    public void testCancelPendingInvitationsCancelsOnlyInvitedRows() {
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 2, 'invited', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 3, 'joined', CURRENT_TIMESTAMP)");

        final int updatedRows = matchParticipantDao.cancelPendingInvitations(10L);

        Assertions.assertEquals(1, updatedRows);
        Assertions.assertEquals("cancelled", participantStatus(10L, 2L));
        Assertions.assertEquals("joined", participantStatus(10L, 3L));
    }

    @Test
    public void testCancelPendingRequestsCancelsOnlyPendingApprovalRows() {
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 2, 'pending_approval', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 3, 'joined', CURRENT_TIMESTAMP)");

        final int updatedRows = matchParticipantDao.cancelPendingRequests(10L);

        Assertions.assertEquals(1, updatedRows);
        Assertions.assertEquals("cancelled", participantStatus(10L, 2L));
        Assertions.assertEquals("joined", participantStatus(10L, 3L));
    }

    @Test
    public void testApproveAllPendingRequestsJoinsOnlyPendingApprovalRows() {
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 2, 'pending_approval', CURRENT_TIMESTAMP)");
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (10, 3, 'invited', CURRENT_TIMESTAMP)");

        final int updatedRows = matchParticipantDao.approveAllPendingRequests(10L);

        Assertions.assertEquals(1, updatedRows);
        Assertions.assertEquals("joined", participantStatus(10L, 2L));
        Assertions.assertEquals("invited", participantStatus(10L, 3L));
    }

    private String participantStatus(final Long matchId, final Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM match_participants WHERE match_id = ? AND user_id = ?",
                String.class,
                matchId,
                userId);
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

    @Test
    public void shouldHasPendingRequest_WhenRequestExists() {
        matchParticipantDao.createJoinRequest(10L, 2L);

        final boolean hasPending = matchParticipantDao.hasPendingRequest(10L, 2L);

        Assertions.assertTrue(hasPending);

        final Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE match_id = 10 AND user_id = 2 AND status = 'pending_approval'",
                        Integer.class);
        Assertions.assertEquals(1, count);
    }

    @Test
    public void shouldHasPendingRequest_WhenNoRequestExists() {
        final boolean hasPending = matchParticipantDao.hasPendingRequest(10L, 2L);

        Assertions.assertFalse(hasPending);
    }

    @Test
    public void shouldHasPendingRequest_WhenRequestApproved() {
        matchParticipantDao.createJoinRequest(10L, 2L);
        matchParticipantDao.approveRequest(10L, 2L);

        final boolean hasPending = matchParticipantDao.hasPendingRequest(10L, 2L);

        Assertions.assertFalse(hasPending, "Approved request should not count as pending");
    }

    @Test
    public void shouldCreateJoinRequest_WhenValidDataProvided() {
        final boolean created = matchParticipantDao.createJoinRequest(10L, 2L);

        Assertions.assertTrue(created);

        final Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE match_id = 10 AND user_id = 2 AND status = 'pending_approval'",
                        Integer.class);
        Assertions.assertEquals(1, count);
    }

    @Test
    public void shouldCreateJoinRequest_WhenDuplicateRequestIsRejected() {
        matchParticipantDao.createJoinRequest(10L, 2L);

        final boolean created = matchParticipantDao.createJoinRequest(10L, 2L);

        Assertions.assertFalse(created, "Duplicate request should be rejected");

        final Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE match_id = 10 AND user_id = 2 AND status = 'pending_approval'",
                        Integer.class);
        Assertions.assertEquals(1, count);
    }

    @Test
    public void shouldApproveRequest_WhenPendingRequestExists() {
        matchParticipantDao.createJoinRequest(10L, 2L);

        final boolean approved = matchParticipantDao.approveRequest(10L, 2L);

        Assertions.assertTrue(approved);

        final String status =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM match_participants"
                                + " WHERE match_id = 10 AND user_id = 2",
                        String.class);
        Assertions.assertEquals("joined", status);
    }

    @Test
    public void shouldApproveRequest_WhenNoPendingRequest() {
        final boolean approved = matchParticipantDao.approveRequest(10L, 2L);

        Assertions.assertFalse(approved, "Approve should fail without pending request");
    }

    @Test
    public void shouldRejectRequest_WhenPendingRequestExists() {
        matchParticipantDao.createJoinRequest(10L, 2L);

        final boolean rejected = matchParticipantDao.rejectRequest(10L, 2L);

        Assertions.assertTrue(rejected);

        final String status =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM match_participants"
                                + " WHERE match_id = 10 AND user_id = 2",
                        String.class);
        Assertions.assertEquals(
                "cancelled", status, "Rejected request should set status to cancelled");
    }

    @Test
    public void shouldRejectRequest_WhenNoPendingRequest() {
        final boolean rejected = matchParticipantDao.rejectRequest(10L, 2L);

        Assertions.assertFalse(rejected, "Reject should fail without pending request");
    }

    @Test
    public void shouldFindPendingMatchIds_WhenUserHasPendingRequests() {
        final Long match11 = 11L;
        final Long match12 = 12L;
        insertMatch(match11);
        insertMatch(match12);
        matchParticipantDao.createJoinRequest(10L, 2L);
        matchParticipantDao.createJoinRequest(match11, 2L);
        matchParticipantDao.createJoinRequest(match12, 2L);
        matchParticipantDao.approveRequest(10L, 2L);

        final List<Long> pending = matchParticipantDao.findPendingMatchIds(2L);

        Assertions.assertEquals(2, pending.size());
        Assertions.assertTrue(pending.contains(match11), "Match11 should be in pending");
        Assertions.assertTrue(pending.contains(match12), "Match12 should be in pending");
        Assertions.assertFalse(pending.contains(10L), "Approved request should not be in pending");
    }

    @Test
    public void shouldFindPendingMatchIds_WhenNoRequests() {
        final List<Long> pending = matchParticipantDao.findPendingMatchIds(2L);

        Assertions.assertTrue(pending.isEmpty());
    }

    @Test
    public void shouldInviteUser_WhenValidDataProvided() {
        final boolean invited = matchParticipantDao.inviteUser(10L, 2L);

        Assertions.assertTrue(invited);

        final String status =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM match_participants"
                                + " WHERE match_id = 10 AND user_id = 2",
                        String.class);
        Assertions.assertEquals("invited", status);
    }

    @Test
    public void shouldInviteUser_WhenDuplicateInviteIsRejected() {
        matchParticipantDao.inviteUser(10L, 2L);

        final boolean invited = matchParticipantDao.inviteUser(10L, 2L);

        Assertions.assertFalse(invited, "Duplicate invite should be rejected");

        final Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE match_id = 10 AND user_id = 2 AND status = 'invited'",
                        Integer.class);
        Assertions.assertEquals(1, count);
    }

    @Test
    public void shouldAcceptInvite_WhenInvitationExists() {
        matchParticipantDao.inviteUser(10L, 2L);

        final boolean accepted = matchParticipantDao.acceptInvite(10L, 2L);

        Assertions.assertTrue(accepted);

        final String status =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM match_participants"
                                + " WHERE match_id = 10 AND user_id = 2",
                        String.class);
        Assertions.assertEquals("joined", status);
    }

    @Test
    public void shouldAcceptInvite_WhenNoInvitation() {
        final boolean accepted = matchParticipantDao.acceptInvite(10L, 2L);

        Assertions.assertFalse(accepted, "Accept should fail without invitation");
    }

    @Test
    public void shouldFindInvitedUsers_WhenUsersAreInvited() {
        matchParticipantDao.inviteUser(10L, 2L);
        matchParticipantDao.inviteUser(10L, 3L);

        final List<User> invited = matchParticipantDao.findInvitedUsers(10L);

        Assertions.assertEquals(2, invited.size());
        final var userIds = invited.stream().map(User::getId).toList();
        Assertions.assertTrue(userIds.contains(2L));
        Assertions.assertTrue(userIds.contains(3L));
    }

    @Test
    public void shouldFindInvitedUsers_WhenNoInvitations() {
        final List<User> invited = matchParticipantDao.findInvitedUsers(10L);

        Assertions.assertTrue(invited.isEmpty());
    }

    @Test
    public void shouldFindInvitedUsers_ExcludesAcceptedInvitations() {
        matchParticipantDao.inviteUser(10L, 2L);
        matchParticipantDao.inviteUser(10L, 3L);
        matchParticipantDao.acceptInvite(10L, 2L);

        final List<User> invited = matchParticipantDao.findInvitedUsers(10L);

        Assertions.assertEquals(1, invited.size());
        Assertions.assertEquals(3L, invited.get(0).getId());
    }

    private void insertMatch(final Long matchId) {
        jdbcTemplate.update(
                "INSERT INTO matches "
                        + "(id, host_user_id, address, title, description, starts_at, max_players, "
                        + "price_per_player, visibility, status, sport, created_at, updated_at) "
                        + "VALUES (?, 1, 'Address', 'Match', 'Description', ?, 4, 0, 'public', "
                        + "'open', 'football', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                matchId,
                java.sql.Timestamp.from(Instant.now().plusSeconds(86400)));
    }
}
