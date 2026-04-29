package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.BanAppealDecision;
import ar.edu.itba.paw.models.UserBan;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
public class UserBanJdbcDaoTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private static final Instant FUTURE = NOW.plus(7, ChronoUnit.DAYS);
    private static final Instant PAST = NOW.minus(7, ChronoUnit.DAYS);

    @Autowired private UserBanDao userBanDao;

    @Autowired private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(
                "INSERT INTO users "
                        + "(id, username, email, name, last_name, phone, created_at, updated_at)"
                        + " VALUES "
                        + "(1, 'admin', 'admin@test.com', 'Admin', 'User', null,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                        + "(2, 'banned', 'banned@test.com', 'Banned', 'User', null,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
    }

    @Test
    public void testCreateBan() {
        final UserBan ban = userBanDao.createBan(2L, 1L, FUTURE, "Spam");

        Assertions.assertNotNull(ban.getId());
        Assertions.assertEquals(2L, ban.getUserId());
        Assertions.assertEquals(1L, ban.getBannedByUserId());
        Assertions.assertEquals("Spam", ban.getReason());
        Assertions.assertNotNull(ban.getBannedUntil());
        Assertions.assertEquals(0, ban.getAppealCount());
        Assertions.assertNull(ban.getAppealDecision());
    }

    @Test
    public void testFindById() {
        final UserBan ban = userBanDao.createBan(2L, 1L, FUTURE, "Spam");

        final Optional<UserBan> result = userBanDao.findById(ban.getId());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(ban.getId(), result.get().getId());
        Assertions.assertEquals("Spam", result.get().getReason());
    }

    @Test
    public void testFindLatestBanForUser() {
        userBanDao.createBan(2L, 1L, PAST, "First offense");
        final UserBan latest = userBanDao.createBan(2L, 1L, FUTURE, "Second offense");

        final Optional<UserBan> result = userBanDao.findLatestBanForUser(2L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(latest.getId(), result.get().getId());
        Assertions.assertEquals("Second offense", result.get().getReason());
    }

    @Test
    public void testFindActiveBanForUser() {
        // Active ban
        final UserBan ban = userBanDao.createBan(2L, 1L, FUTURE, "Spam");

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(ban.getId(), result.get().getId());
    }

    @Test
    public void testFindActiveBanForUserReturnsEmptyIfExpired() {
        // Expired ban
        userBanDao.createBan(2L, 1L, PAST, "Spam");

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testFindActiveBanForUserReturnsEmptyIfLifted() {
        // Active ban but lifted
        final UserBan ban = userBanDao.createBan(2L, 1L, FUTURE, "Spam");
        userBanDao.appealBan(ban.getId(), "Mistake", NOW);
        userBanDao.resolveAppeal(ban.getId(), 1L, BanAppealDecision.LIFTED, NOW);

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testFindActiveBanForUserReturnsPresentIfUpheld() {
        // Active ban and upheld
        final UserBan ban = userBanDao.createBan(2L, 1L, FUTURE, "Spam");
        userBanDao.appealBan(ban.getId(), "Mistake", NOW);
        userBanDao.resolveAppeal(ban.getId(), 1L, BanAppealDecision.UPHELD, NOW);

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(ban.getId(), result.get().getId());
    }

    @Test
    public void testAppealBan() {
        final UserBan ban = userBanDao.createBan(2L, 1L, FUTURE, "Spam");

        final boolean success = userBanDao.appealBan(ban.getId(), "Sorry", NOW);

        Assertions.assertTrue(success);

        final UserBan appealedBan = userBanDao.findById(ban.getId()).get();
        Assertions.assertEquals("Sorry", appealedBan.getAppealReason());
        Assertions.assertEquals(1, appealedBan.getAppealCount());
        Assertions.assertNotNull(appealedBan.getAppealedAt());
    }

    @Test
    public void testResolveAppeal() {
        final UserBan ban = userBanDao.createBan(2L, 1L, FUTURE, "Spam");
        userBanDao.appealBan(ban.getId(), "Sorry", NOW);

        final boolean success =
                userBanDao.resolveAppeal(ban.getId(), 1L, BanAppealDecision.LIFTED, NOW);

        Assertions.assertTrue(success);

        final UserBan resolvedBan = userBanDao.findById(ban.getId()).get();
        Assertions.assertEquals(BanAppealDecision.LIFTED, resolvedBan.getAppealDecision());
        Assertions.assertEquals(1L, resolvedBan.getAppealResolvedByUserId());
        Assertions.assertNotNull(resolvedBan.getAppealResolvedAt());
    }

    @Test
    public void testFindBansForUser() {
        userBanDao.createBan(2L, 1L, PAST, "First");
        userBanDao.createBan(2L, 1L, FUTURE, "Second");

        final List<UserBan> bans = userBanDao.findBansForUser(2L);

        Assertions.assertEquals(2, bans.size());
        // Ordered by created_at DESC
        Assertions.assertEquals("Second", bans.get(0).getReason());
        Assertions.assertEquals("First", bans.get(1).getReason());
    }

    @Test
    public void testAppealBanFailsWhenAlreadyAppealed() {
        final UserBan ban = userBanDao.createBan(2L, 1L, FUTURE, "Spam");

        final boolean success1 = userBanDao.appealBan(ban.getId(), "Sorry", NOW);
        Assertions.assertTrue(success1);

        final boolean success2 = userBanDao.appealBan(ban.getId(), "Sorry again", NOW);
        Assertions.assertFalse(success2);
    }

    @Test
    public void testResolveAppealFailsWhenNotAppealedOrAlreadyResolved() {
        final UserBan ban = userBanDao.createBan(2L, 1L, FUTURE, "Spam");

        // Fails when not appealed
        final boolean success1 =
                userBanDao.resolveAppeal(ban.getId(), 1L, BanAppealDecision.LIFTED, NOW);
        Assertions.assertFalse(success1);

        userBanDao.appealBan(ban.getId(), "Sorry", NOW);

        // Success when appealed
        final boolean success2 =
                userBanDao.resolveAppeal(ban.getId(), 1L, BanAppealDecision.LIFTED, NOW);
        Assertions.assertTrue(success2);

        // Fails when already resolved
        final boolean success3 =
                userBanDao.resolveAppeal(ban.getId(), 1L, BanAppealDecision.UPHELD, NOW);
        Assertions.assertFalse(success3);
    }
}
