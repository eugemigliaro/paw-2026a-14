package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewReaction;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
public class PlayerReviewJdbcDaoTest {

    private static final Instant PAST_START = Instant.parse("2026-04-01T18:00:00Z");
    private static final Instant FUTURE_START = Instant.now().plusSeconds(86400);

    @Autowired private PlayerReviewDao playerReviewDao;

    @Autowired private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(
                "INSERT INTO users "
                        + "(id, username, email, name, last_name, phone, created_at, updated_at)"
                        + " VALUES "
                        + "(1, 'host', 'host@test.com', 'Host', 'User', null,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                        + "(2, 'player', 'player@test.com', 'Player', 'User', null,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                        + "(3, 'other', 'other@test.com', 'Other', 'User', null,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                        + "(4, 'outsider', 'outsider@test.com', 'Out', 'Sider', null,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        insertMatch(10L, "completed", PAST_START);
        insertMatch(11L, "open", PAST_START);
        insertMatch(12L, "open", FUTURE_START);
        insertMatch(13L, "cancelled", PAST_START);
    }

    @Test
    public void testUpsertReviewCreatesReview() {
        joinMatch(10L, 2L, "joined");
        joinMatch(10L, 3L, "joined");

        final PlayerReview review =
                playerReviewDao.upsertReview(2L, 3L, PlayerReviewReaction.LIKE, "Good teammate");

        Assertions.assertNotNull(review.getId());
        Assertions.assertEquals(2L, review.getReviewerUserId());
        Assertions.assertEquals(3L, review.getReviewedUserId());
        Assertions.assertEquals(PlayerReviewReaction.LIKE, review.getReaction());
        Assertions.assertEquals("Good teammate", review.getComment());
        Assertions.assertFalse(review.isDeleted());
    }

    @Test
    public void testUpsertReviewUpdatesExistingReview() {
        joinMatch(10L, 2L, "joined");
        joinMatch(10L, 3L, "joined");
        final PlayerReview initial =
                playerReviewDao.upsertReview(2L, 3L, PlayerReviewReaction.LIKE, "Good teammate");

        final PlayerReview updated =
                playerReviewDao.upsertReview(2L, 3L, PlayerReviewReaction.DISLIKE, "Arrived late");

        Assertions.assertEquals(initial.getId(), updated.getId());
        Assertions.assertEquals(PlayerReviewReaction.DISLIKE, updated.getReaction());
        Assertions.assertEquals("Arrived late", updated.getComment());
    }

    @Test
    public void testUpsertReviewRestoresSoftDeletedReview() {
        joinMatch(10L, 2L, "joined");
        joinMatch(10L, 3L, "joined");
        final PlayerReview initial =
                playerReviewDao.upsertReview(2L, 3L, PlayerReviewReaction.LIKE, "Good teammate");
        Assertions.assertTrue(playerReviewDao.softDeleteReview(2L, 3L));

        final PlayerReview restored =
                playerReviewDao.upsertReview(2L, 3L, PlayerReviewReaction.LIKE, "Played well");

        Assertions.assertEquals(initial.getId(), restored.getId());
        Assertions.assertFalse(restored.isDeleted());
        Assertions.assertEquals("Played well", restored.getComment());
    }

    @Test
    public void testSoftDeleteExcludesReviewFromSummary() {
        joinMatch(10L, 2L, "joined");
        joinMatch(10L, 3L, "joined");
        playerReviewDao.upsertReview(2L, 3L, PlayerReviewReaction.LIKE, "Good teammate");

        Assertions.assertTrue(playerReviewDao.softDeleteReview(2L, 3L));
        final PlayerReviewSummary summary = playerReviewDao.getSummaryForUser(3L);

        Assertions.assertEquals(0L, summary.getReviewCount());
        Assertions.assertEquals(0L, summary.getLikeCount());
    }

    @Test
    public void testSoftDeleteReviewWithAdminDetails() {
        joinMatch(10L, 2L, "joined");
        joinMatch(10L, 3L, "joined");
        final PlayerReview initial =
                playerReviewDao.upsertReview(2L, 3L, PlayerReviewReaction.LIKE, "Good teammate");

        final boolean deleted = playerReviewDao.softDeleteReview(2L, 3L, 4L, "spam");

        Assertions.assertTrue(deleted);
        final Optional<PlayerReview> found =
                playerReviewDao.findByIdIncludingDeleted(initial.getId());
        Assertions.assertTrue(found.isPresent());
        Assertions.assertTrue(found.get().isDeleted());
        Assertions.assertEquals(4L, found.get().getDeletedByUserId());
        Assertions.assertEquals("spam", found.get().getDeleteReason());
    }

    @Test
    public void testRestoreReview() {
        joinMatch(10L, 2L, "joined");
        joinMatch(10L, 3L, "joined");

        playerReviewDao.upsertReview(2L, 3L, PlayerReviewReaction.LIKE, "Good teammate");
        playerReviewDao.softDeleteReview(2L, 3L, 4L, "spam");

        final boolean restored = playerReviewDao.restoreReview(2L, 3L);
        Assertions.assertTrue(restored);

        final Optional<PlayerReview> found = playerReviewDao.findByPair(2L, 3L);
        Assertions.assertTrue(found.isPresent());
        Assertions.assertFalse(found.get().isDeleted());
        Assertions.assertNull(found.get().getDeletedByUserId());
        Assertions.assertNull(found.get().getDeleteReason());
        Assertions.assertNull(found.get().getDeletedAt());
    }

    @Test
    public void testSummaryCountsLikesAndDislikes() {
        joinMatch(10L, 1L, "joined");
        joinMatch(10L, 2L, "joined");
        joinMatch(10L, 3L, "joined");
        playerReviewDao.upsertReview(1L, 3L, PlayerReviewReaction.LIKE, "Great");
        playerReviewDao.upsertReview(2L, 3L, PlayerReviewReaction.DISLIKE, null);

        final PlayerReviewSummary summary = playerReviewDao.getSummaryForUser(3L);

        Assertions.assertEquals(3L, summary.getReviewedUserId());
        Assertions.assertEquals(2L, summary.getReviewCount());
        Assertions.assertEquals(1L, summary.getLikeCount());
        Assertions.assertEquals(1L, summary.getDislikeCount());
    }

    @Test
    public void testFindRecentReviewsReturnsNewestFirst() {
        joinMatch(10L, 1L, "joined");
        joinMatch(10L, 2L, "joined");
        joinMatch(10L, 3L, "joined");
        playerReviewDao.upsertReview(1L, 3L, PlayerReviewReaction.LIKE, "First");
        playerReviewDao.upsertReview(2L, 3L, PlayerReviewReaction.LIKE, "Second");

        final List<PlayerReview> reviews = playerReviewDao.findRecentReviewsForUser(3L, 10, 0);

        Assertions.assertEquals(2, reviews.size());
        Assertions.assertEquals("Second", reviews.get(0).getComment());
        Assertions.assertEquals("First", reviews.get(1).getComment());
    }

    @Test
    public void testCanReviewSharedCompletedMatch() {
        joinMatch(10L, 2L, "joined");
        joinMatch(10L, 3L, "checked_in");

        Assertions.assertTrue(playerReviewDao.canReview(2L, 3L));
    }

    @Test
    public void testCanReviewDerivedCompletedOpenPastMatch() {
        joinMatch(11L, 2L, "joined");
        joinMatch(11L, 3L, "joined");

        Assertions.assertTrue(playerReviewDao.canReview(2L, 3L));
    }

    @Test
    public void testCanReviewRejectsInvalidCases() {
        joinMatch(12L, 2L, "joined");
        joinMatch(12L, 3L, "joined");
        joinMatch(13L, 2L, "joined");
        joinMatch(13L, 3L, "joined");

        Assertions.assertFalse(playerReviewDao.canReview(2L, 2L));
        Assertions.assertFalse(playerReviewDao.canReview(2L, 4L));
        Assertions.assertFalse(playerReviewDao.canReview(2L, 3L));
    }

    @Test
    public void testCanReviewRejectsCancelledParticipant() {
        joinMatch(10L, 2L, "joined");
        joinMatch(10L, 3L, "cancelled");

        Assertions.assertFalse(playerReviewDao.canReview(2L, 3L));
    }

    private void insertMatch(final Long id, final String status, final Instant startsAt) {
        jdbcTemplate.update(
                "INSERT INTO matches "
                        + "(id, host_user_id, address, title, description, starts_at,"
                        + " max_players, price_per_player, visibility, status, sport,"
                        + " created_at, updated_at)"
                        + " VALUES (?, 1, 'Address', 'Match', 'Description', ?, 4, 0,"
                        + " 'public', ?, 'football', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id,
                Timestamp.from(startsAt),
                status);
    }

    private void joinMatch(final Long matchId, final Long userId, final String status) {
        jdbcTemplate.update(
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                matchId,
                userId,
                status);
    }
}
