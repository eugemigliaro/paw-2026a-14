package ar.edu.itba.paw.persistence;

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
                "INSERT INTO users (id, username, email, created_at, updated_at) VALUES "
                        + "(1, 'host', 'host@test.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), "
                        + "(2, 'player', 'player@test.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), "
                        + "(3, 'other', 'other@test.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
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

        Assertions.assertEquals(2, participants.size());
        Assertions.assertEquals(2L, participants.get(0).getId());
        Assertions.assertEquals(3L, participants.get(1).getId());
    }

    @Test
    public void testFindConfirmedParticipantsExcludesCancelledUsersAndHost() {
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

        Assertions.assertEquals(1, participants.size());
        Assertions.assertEquals(3L, participants.get(0).getId());
    }
}
