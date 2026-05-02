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
                        + "(2, 'banned', 'banned@test.com', 'Banned', 'User', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        jdbcTemplate.update(
                "INSERT INTO moderation_reports (id, reporter_user_id, target_type, target_id, reason, status, appeal_count, created_at, updated_at) VALUES "
                        + "(1, 1, 'user', 2, 'spam', 'resolved', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        reportId = 1L;
    }

    @Test
    public void testCreateBan() {
        final UserBan ban = userBanDao.createBan(reportId, FUTURE);

        Assertions.assertNotNull(ban.getId());
        Assertions.assertEquals(FUTURE, ban.getBannedUntil());
        Assertions.assertEquals(reportId, ban.getModerationReportId());
    }

    @Test
    public void testFindById() {
        final UserBan ban = userBanDao.createBan(reportId, FUTURE);

        final Optional<UserBan> result = userBanDao.findById(ban.getId());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(ban.getId(), result.get().getId());
        Assertions.assertEquals(FUTURE, result.get().getBannedUntil());
    }

    @Test
    public void testFindLatestBanForUser() {
        userBanDao.createBan(reportId, PAST);
        final UserBan latest = userBanDao.createBan(reportId, FUTURE);

        final Optional<UserBan> result = userBanDao.findLatestBanForUser(2L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(latest.getId(), result.get().getId());
    }

    @Test
    public void testFindActiveBanForUser() {
        final UserBan ban = userBanDao.createBan(reportId, FUTURE);

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(ban.getId(), result.get().getId());
    }

    @Test
    public void testFindActiveBanForUserReturnsEmptyIfExpired() {
        userBanDao.createBan(reportId, PAST);

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testFindActiveBanForUserReturnsEmptyIfLifted() {
        userBanDao.createBan(reportId, FUTURE);
        jdbcTemplate.update(
                "UPDATE moderation_reports SET appeal_decision = 'lifted' WHERE id = ?", reportId);

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testFindActiveBanForUserReturnsPresentIfUpheld() {
        final UserBan ban = userBanDao.createBan(reportId, FUTURE);

        jdbcTemplate.update(
                "UPDATE moderation_reports SET appeal_decision = 'upheld' WHERE id = ?", reportId);

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(ban.getId(), result.get().getId());
    }
}
