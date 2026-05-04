package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.UserBan;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
public class UserBanJdbcDaoTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private static final Instant FUTURE = NOW.plus(7, ChronoUnit.DAYS);
    private static final Instant PAST = NOW.minus(7, ChronoUnit.DAYS);

    @Autowired private UserBanDao userBanDao;
    @Autowired private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private long reportId;

    @BeforeEach
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, name, last_name, phone, created_at, updated_at) VALUES "
                        + "(1, 'admin', 'admin@test.com', 'Admin', 'User', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                        + "(2, 'banned', 'banned@test.com', 'Banned', 'User', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                        + "(3, 'other', 'other@test.com', 'Other', 'User', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                        + "(4, 'reporter2', 'reporter2@test.com', 'Reporter', '2', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        jdbcTemplate.update(
                "INSERT INTO moderation_reports (id, reporter_user_id, target_type, target_id, reason, status, appeal_count, created_at, updated_at) VALUES "
                        + "(1, 1, 'user', 2, 'spam', 'resolved', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                        + "(2, 4, 'user', 2, 'harassment', 'resolved', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                        + "(3, 1, 'user', 3, 'spam', 'resolved', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                        + "(4, 2, 'user', 2, 'abuse', 'resolved', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        reportId = 1L;
    }

    @Test
    public void shouldCreateBan_WhenValidDataProvided() {
        final Instant bannedUntil = NOW.plus(30, ChronoUnit.DAYS);

        final UserBan ban = userBanDao.createBan(reportId, bannedUntil);

        Assertions.assertNotNull(ban.getId());
        Assertions.assertEquals(reportId, ban.getModerationReportId());
        Assertions.assertEquals(bannedUntil, ban.getBannedUntil());

        final Long countInDb =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM user_bans WHERE id = ? AND moderation_report_id = ? AND banned_until = ?",
                        Long.class,
                        ban.getId(),
                        reportId,
                        new java.sql.Timestamp(bannedUntil.toEpochMilli()));
        Assertions.assertEquals(1L, countInDb, "Ban should be persisted in database");
    }

    @Test
    public void shouldCreateBan_WithDifferentDurations() {
        final Instant shortBanUntil = NOW.plus(1, ChronoUnit.HOURS);
        final Instant longBanUntil = NOW.plus(365, ChronoUnit.DAYS);

        final UserBan shortBan = userBanDao.createBan(reportId, shortBanUntil);
        final UserBan longBan = userBanDao.createBan(2L, longBanUntil);

        Assertions.assertEquals(shortBanUntil, shortBan.getBannedUntil());
        Assertions.assertEquals(longBanUntil, longBan.getBannedUntil());
    }

    @Test
    public void shouldFindBanById_WhenBanExists() {
        final UserBan created = userBanDao.createBan(reportId, FUTURE);

        final Optional<UserBan> result = userBanDao.findById(created.getId());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(created.getId(), result.get().getId());
        Assertions.assertEquals(FUTURE, result.get().getBannedUntil());
        Assertions.assertEquals(reportId, result.get().getModerationReportId());
    }

    @Test
    public void shouldFindBanById_WhenBanNotFound() {
        final Long nonExistentBanId = 99999L;

        final Optional<UserBan> result = userBanDao.findById(nonExistentBanId);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void shouldFindBanById_ReturnsOnlyExactBan() {
        final UserBan ban1 = userBanDao.createBan(reportId, FUTURE);
        final UserBan ban2 = userBanDao.createBan(2L, FUTURE);

        final Optional<UserBan> result1 = userBanDao.findById(ban1.getId());
        final Optional<UserBan> result2 = userBanDao.findById(ban2.getId());

        Assertions.assertTrue(result1.isPresent());
        Assertions.assertEquals(ban1.getId(), result1.get().getId());

        Assertions.assertTrue(result2.isPresent());
        Assertions.assertEquals(ban2.getId(), result2.get().getId());
    }

    @Test
    public void shouldFindLatestBanForUser_WhenBansExist() {
        final long bannedUserId = 2L;

        userBanDao.createBan(reportId, PAST);
        final UserBan latest = userBanDao.createBan(2L, FUTURE);

        final Optional<UserBan> result = userBanDao.findLatestBanForUser(bannedUserId);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(latest.getId(), result.get().getId());
    }

    @Test
    public void shouldFindLatestBanForUser_WhenNoBansExist() {
        final long userWithoutBan = 3L;

        final Optional<UserBan> result = userBanDao.findLatestBanForUser(userWithoutBan);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void shouldFindLatestBanForUser_OnlyForSpecificUser() {
        final long user1 = 2L;
        final long user2 = 3L;

        userBanDao.createBan(reportId, FUTURE);
        userBanDao.createBan(3L, FUTURE);

        final Optional<UserBan> result1 = userBanDao.findLatestBanForUser(user1);
        final Optional<UserBan> result2 = userBanDao.findLatestBanForUser(user2);

        Assertions.assertTrue(result1.isPresent());
        Assertions.assertTrue(result2.isPresent());
        Assertions.assertNotEquals(result1.get().getId(), result2.get().getId());
    }

    @Test
    public void shouldFindActiveBanForUser_WhenBanIsActive() {
        final long bannedUserId = 2L;
        final UserBan ban = userBanDao.createBan(reportId, FUTURE);

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(bannedUserId, NOW);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(ban.getId(), result.get().getId());
    }

    @Test
    public void shouldFindActiveBanForUser_WhenBanIsExpired() {
        userBanDao.createBan(reportId, PAST);

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertTrue(result.isEmpty(), "Expired ban should not be active");
    }

    @Test
    public void shouldFindActiveBanForUser_WithBoundaryExpiration() {
        final Instant exactNow = NOW;
        final Instant almostExpired = exactNow.plus(1, ChronoUnit.SECONDS);
        final UserBan ban = userBanDao.createBan(reportId, almostExpired);

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, exactNow);

        Assertions.assertTrue(result.isPresent(), "Ban should be active at boundary time");
        Assertions.assertEquals(ban.getId(), result.get().getId());
    }

    @Test
    public void shouldFindActiveBanForUser_WhenBanIsLifted() {
        userBanDao.createBan(reportId, FUTURE);

        jdbcTemplate.update(
                "UPDATE moderation_reports SET appeal_decision = 'lifted' WHERE id = ?", reportId);

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertTrue(result.isEmpty(), "Lifted ban should not be active");
    }

    @Test
    public void shouldFindActiveBanForUser_WhenBanIsUpheld() {
        final UserBan ban = userBanDao.createBan(reportId, FUTURE);

        jdbcTemplate.update(
                "UPDATE moderation_reports SET appeal_decision = 'upheld' WHERE id = ?", reportId);

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertTrue(result.isPresent(), "Upheld ban should remain active");
        Assertions.assertEquals(ban.getId(), result.get().getId());
    }

    @Test
    public void shouldFindActiveBanForUser_WithNullAppealDecision() {
        final UserBan ban = userBanDao.createBan(reportId, FUTURE);

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(ban.getId(), result.get().getId());
    }

    @Test
    public void shouldUpliftBan_WhenBanExists() {
        final UserBan ban = userBanDao.createBan(reportId, FUTURE);

        final Optional<UserBan> activeBefore = userBanDao.findActiveBanForUser(2L, NOW);
        Assertions.assertTrue(activeBefore.isPresent(), "Ban should be active before uplift");

        userBanDao.upliftBan(ban.getId());

        final Instant bannedUntilAfterUplift =
                jdbcTemplate.queryForObject(
                        "SELECT banned_until FROM user_bans WHERE id = ?",
                        (rs, rowNum) -> rs.getTimestamp("banned_until").toInstant(),
                        ban.getId());

        final Instant nowInBd = Instant.now();
        Assertions.assertTrue(
                bannedUntilAfterUplift.isBefore(nowInBd) || bannedUntilAfterUplift.equals(nowInBd),
                "Uplifted ban should have banned_until at or before current time");

        final Optional<UserBan> activeBanAfter =
                userBanDao.findActiveBanForUser(2L, nowInBd.plus(1, ChronoUnit.SECONDS));
        Assertions.assertTrue(
                activeBanAfter.isEmpty(), "Uplifted ban should not be active after uplift");
    }

    @Test
    public void shouldUpliftBan_MultipleTimesIsIdempotent() {
        final UserBan ban = userBanDao.createBan(reportId, FUTURE);

        userBanDao.upliftBan(ban.getId());
        final Instant afterFirstUplift =
                jdbcTemplate.queryForObject(
                        "SELECT banned_until FROM user_bans WHERE id = ?",
                        (rs, rowNum) -> rs.getTimestamp("banned_until").toInstant(),
                        ban.getId());

        userBanDao.upliftBan(ban.getId());
        final Instant afterSecondUplift =
                jdbcTemplate.queryForObject(
                        "SELECT banned_until FROM user_bans WHERE id = ?",
                        (rs, rowNum) -> rs.getTimestamp("banned_until").toInstant(),
                        ban.getId());

        Assertions.assertTrue(afterFirstUplift.isBefore(NOW.plus(10, ChronoUnit.SECONDS)));
        Assertions.assertTrue(afterSecondUplift.isBefore(NOW.plus(10, ChronoUnit.SECONDS)));
    }

    @Test
    public void shouldUpliftBan_WhenBanNotFound() {
        final Long nonExistentBanId = 99999L;

        Assertions.assertDoesNotThrow(() -> userBanDao.upliftBan(nonExistentBanId));
    }

    @Test
    public void shouldUpliftBan_MakesExpiredBanInactive() {
        final UserBan ban = userBanDao.createBan(reportId, FUTURE);

        final Optional<UserBan> activeBefore = userBanDao.findActiveBanForUser(2L, NOW);
        Assertions.assertTrue(activeBefore.isPresent());

        userBanDao.upliftBan(ban.getId());

        final Instant queryTime = Instant.now().plus(1, ChronoUnit.SECONDS);
        final Optional<UserBan> activeAfter = userBanDao.findActiveBanForUser(2L, queryTime);
        Assertions.assertTrue(activeAfter.isEmpty(), "Uplifted ban should not be active");

        final Optional<UserBan> stillExists = userBanDao.findById(ban.getId());
        Assertions.assertTrue(stillExists.isPresent(), "Uplifted ban should still exist");
    }

    @Test
    public void shouldHandleMultipleBansPerUser_ReturnsMostRecent() {
        final long bannedUserId = 2L;

        userBanDao.createBan(reportId, NOW.plus(1, ChronoUnit.DAYS));
        userBanDao.createBan(2L, NOW.plus(2, ChronoUnit.DAYS));
        final UserBan ban3 = userBanDao.createBan(4L, NOW.plus(3, ChronoUnit.DAYS));

        final Optional<UserBan> latest = userBanDao.findLatestBanForUser(bannedUserId);

        Assertions.assertTrue(latest.isPresent());
        Assertions.assertEquals(ban3.getId(), latest.get().getId());
    }

    @Test
    public void shouldFindActiveBanForUser_WithMultipleBans_ReturnsLatestActive() {
        final long bannedUserId = 2L;

        userBanDao.createBan(reportId, PAST);

        final UserBan activeBan = userBanDao.createBan(2L, FUTURE);

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(bannedUserId, NOW);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(activeBan.getId(), result.get().getId());
    }
}
