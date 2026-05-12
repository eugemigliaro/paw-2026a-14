package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.UserBan;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class UserBanJpaDaoTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private static final Instant FUTURE = NOW.plus(7, ChronoUnit.DAYS);
    private static final Instant PAST = NOW.minus(7, ChronoUnit.DAYS);

    @Autowired private UserBanDao userBanDao;
    @PersistenceContext private EntityManager em;

    private ModerationReport report1;
    private ModerationReport report2;
    private ModerationReport report3;
    private ModerationReport report4;

    @BeforeEach
    public void setUp() {
        em.createNativeQuery(
                        "INSERT INTO users (id, username, email, name, last_name, phone, created_at, updated_at, preferred_language, role) VALUES "
                                + "(1, 'admin', 'admin@test.com', 'Admin', 'User', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'en', 'admin_mod'),"
                                + "(2, 'banned', 'banned@test.com', 'Banned', 'User', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'en', 'user'),"
                                + "(3, 'other', 'other@test.com', 'Other', 'User', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'en', 'user'),"
                                + "(4, 'reporter2', 'reporter2@test.com', 'Reporter', '2', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'en', 'user')")
                .executeUpdate();

        em.createNativeQuery(
                        "INSERT INTO moderation_reports (id, reporter_user_id, target_type, target_id, reason, status, appeal_count, created_at, updated_at) VALUES "
                                + "(1, 1, 'user', 2, 'spam', 'resolved', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                                + "(2, 4, 'user', 2, 'harassment', 'resolved', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                                + "(3, 1, 'user', 3, 'spam', 'resolved', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                                + "(4, 2, 'user', 2, 'other', 'resolved', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .executeUpdate();

        em.flush();
        em.clear();

        report1 = em.find(ModerationReport.class, 1L);
        report2 = em.find(ModerationReport.class, 2L);
        report3 = em.find(ModerationReport.class, 3L);
        report4 = em.find(ModerationReport.class, 4L);
    }

    @Test
    public void shouldCreateBan_WhenValidDataProvided() {
        final Instant bannedUntil = NOW.plus(30, ChronoUnit.DAYS);

        final UserBan ban = userBanDao.createBan(report1, bannedUntil);

        Assertions.assertNotNull(ban.getId());
        Assertions.assertEquals(1, countUserBans());
        assertPersistedBan(ban.getId(), report1.getId(), bannedUntil);
    }

    @Test
    public void shouldCreateBan_WithDifferentDurations() {
        final Instant shortBanUntil = NOW.plus(1, ChronoUnit.HOURS);
        final Instant longBanUntil = NOW.plus(365, ChronoUnit.DAYS);

        final UserBan shortBan = userBanDao.createBan(report1, shortBanUntil);
        final UserBan longBan = userBanDao.createBan(report2, longBanUntil);

        Assertions.assertEquals(2, countUserBans());
        assertPersistedBan(shortBan.getId(), report1.getId(), shortBanUntil);
        assertPersistedBan(longBan.getId(), report2.getId(), longBanUntil);
    }

    @Test
    public void shouldFindBanById_WhenBanExists() {
        final UserBan created = userBanDao.createBan(report1, FUTURE);
        flushAndClear();

        final Optional<UserBan> result = userBanDao.findById(created.getId());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(created.getId(), result.get().getId());
        Assertions.assertEquals(FUTURE, result.get().getBannedUntil());
        Assertions.assertEquals(report1.getId(), result.get().getModerationReport().getId());
        Assertions.assertEquals(1, countUserBans());
        assertPersistedBan(created.getId(), report1.getId(), FUTURE);
    }

    @Test
    public void shouldFindBanById_WhenBanNotFound() {
        final Long nonExistentBanId = 99999L;

        final Optional<UserBan> result = userBanDao.findById(nonExistentBanId);

        Assertions.assertTrue(result.isEmpty());
        Assertions.assertEquals(0, countUserBans());
    }

    @Test
    public void shouldFindBanById_ReturnsOnlyExactBan() {
        final UserBan ban1 = userBanDao.createBan(report1, FUTURE);
        final UserBan ban2 = userBanDao.createBan(report2, FUTURE.plus(1, ChronoUnit.DAYS));
        flushAndClear();

        final Optional<UserBan> result1 = userBanDao.findById(ban1.getId());
        final Optional<UserBan> result2 = userBanDao.findById(ban2.getId());

        Assertions.assertTrue(result1.isPresent());
        Assertions.assertEquals(ban1.getId(), result1.get().getId());
        Assertions.assertTrue(result2.isPresent());
        Assertions.assertEquals(ban2.getId(), result2.get().getId());
        Assertions.assertEquals(2, countUserBans());
        assertPersistedBan(ban1.getId(), report1.getId(), FUTURE);
        assertPersistedBan(ban2.getId(), report2.getId(), FUTURE.plus(1, ChronoUnit.DAYS));
    }

    @Test
    public void shouldFindLatestBanForUser_WhenBansExist() {
        final long bannedUserId = 2L;
        final UserBan expired = userBanDao.createBan(report1, PAST);
        final UserBan latest = userBanDao.createBan(report2, FUTURE);
        flushAndClear();

        final Optional<UserBan> result = userBanDao.findLatestBanForUser(bannedUserId);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(latest.getId(), result.get().getId());
        Assertions.assertEquals(2, countUserBans());
        assertPersistedBan(expired.getId(), report1.getId(), PAST);
        assertPersistedBan(latest.getId(), report2.getId(), FUTURE);
    }

    @Test
    public void shouldFindLatestBanForUser_WhenNoBansExist() {
        final long userWithoutBan = 3L;

        final Optional<UserBan> result = userBanDao.findLatestBanForUser(userWithoutBan);

        Assertions.assertTrue(result.isEmpty());
        Assertions.assertEquals(0, countUserBans());
    }

    @Test
    public void shouldFindLatestBanForUser_OnlyForSpecificUser() {
        final long user1 = 2L;
        final long user2 = 3L;
        final UserBan user1Ban = userBanDao.createBan(report1, FUTURE);
        final UserBan user2Ban = userBanDao.createBan(report3, FUTURE.plus(1, ChronoUnit.DAYS));
        flushAndClear();

        final Optional<UserBan> result1 = userBanDao.findLatestBanForUser(user1);
        final Optional<UserBan> result2 = userBanDao.findLatestBanForUser(user2);

        Assertions.assertTrue(result1.isPresent());
        Assertions.assertEquals(user1Ban.getId(), result1.get().getId());
        Assertions.assertTrue(result2.isPresent());
        Assertions.assertEquals(user2Ban.getId(), result2.get().getId());
        Assertions.assertEquals(2, countUserBans());
        assertPersistedBan(user1Ban.getId(), report1.getId(), FUTURE);
        assertPersistedBan(user2Ban.getId(), report3.getId(), FUTURE.plus(1, ChronoUnit.DAYS));
    }

    @Test
    public void shouldFindActiveBanForUser_WhenBanIsActive() {
        final long bannedUserId = 2L;
        final UserBan ban = userBanDao.createBan(report1, FUTURE);
        flushAndClear();

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(bannedUserId, NOW);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(ban.getId(), result.get().getId());
        Assertions.assertEquals(1, countUserBans());
        assertPersistedBan(ban.getId(), report1.getId(), FUTURE);
    }

    @Test
    public void shouldFindActiveBanForUser_WhenBanIsExpired() {
        final UserBan ban = userBanDao.createBan(report1, PAST);
        flushAndClear();

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertTrue(result.isEmpty(), "Expired ban should not be active");
        Assertions.assertEquals(1, countUserBans());
        assertPersistedBan(ban.getId(), report1.getId(), PAST);
    }

    @Test
    public void shouldFindActiveBanForUser_WithBoundaryExpiration() {
        final Instant exactNow = NOW;
        final Instant almostExpired = exactNow.plus(1, ChronoUnit.SECONDS);
        final UserBan ban = userBanDao.createBan(report1, almostExpired);
        flushAndClear();

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, exactNow);

        Assertions.assertTrue(result.isPresent(), "Ban should be active at boundary time");
        Assertions.assertEquals(ban.getId(), result.get().getId());
        Assertions.assertEquals(1, countUserBans());
        assertPersistedBan(ban.getId(), report1.getId(), almostExpired);
    }

    @Test
    public void shouldFindActiveBanForUser_WhenBanIsLifted() {
        final UserBan ban = userBanDao.createBan(report1, FUTURE);
        markReportAppealDecision(report1.getId(), "lifted");

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertTrue(result.isEmpty(), "Lifted ban should not be active");
        Assertions.assertEquals(1, countUserBans());
        assertPersistedBan(ban.getId(), report1.getId(), FUTURE);
    }

    @Test
    public void shouldFindActiveBanForUser_WhenBanIsUpheld() {
        final UserBan ban = userBanDao.createBan(report1, FUTURE);
        markReportAppealDecision(report1.getId(), "upheld");

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertTrue(result.isPresent(), "Upheld ban should remain active");
        Assertions.assertEquals(ban.getId(), result.get().getId());
        Assertions.assertEquals(1, countUserBans());
        assertPersistedBan(ban.getId(), report1.getId(), FUTURE);
    }

    @Test
    public void shouldFindActiveBanForUser_WithNullAppealDecision() {
        final UserBan ban = userBanDao.createBan(report1, FUTURE);
        flushAndClear();

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(2L, NOW);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(ban.getId(), result.get().getId());
        Assertions.assertEquals(1, countUserBans());
        assertPersistedBan(ban.getId(), report1.getId(), FUTURE);
    }

    @Test
    public void shouldUpliftBan_WhenBanExists() {
        final UserBan ban = userBanDao.createBan(report1, FUTURE);
        flushAndClear();
        final Optional<UserBan> activeBefore = userBanDao.findActiveBanForUser(2L, NOW);
        final Instant beforeUplift = Instant.now();

        userBanDao.upliftBan(ban.getId());

        final Instant afterUplift = Instant.now();
        Assertions.assertTrue(activeBefore.isPresent(), "Ban should be active before uplift");
        Assertions.assertEquals(1, countUserBans());

        final UserBan persisted = findPersistedBan(ban.getId());
        Assertions.assertEquals(report1.getId(), persisted.getModerationReport().getId());
        Assertions.assertFalse(persisted.getBannedUntil().isBefore(beforeUplift));
        Assertions.assertFalse(persisted.getBannedUntil().isAfter(afterUplift));

        final Optional<UserBan> activeBanAfter =
                userBanDao.findActiveBanForUser(2L, afterUplift.plus(1, ChronoUnit.SECONDS));
        Assertions.assertTrue(
                activeBanAfter.isEmpty(), "Uplifted ban should not be active after uplift");
    }

    @Test
    public void shouldUpliftBan_MultipleTimesIsIdempotent() {
        final UserBan ban = userBanDao.createBan(report1, FUTURE);
        flushAndClear();

        userBanDao.upliftBan(ban.getId());
        final Instant afterFirstUplift = findPersistedBan(ban.getId()).getBannedUntil();
        userBanDao.upliftBan(ban.getId());

        final Instant afterSecondUplift = findPersistedBan(ban.getId()).getBannedUntil();
        Assertions.assertEquals(1, countUserBans());
        Assertions.assertFalse(afterFirstUplift.isAfter(Instant.now()));
        Assertions.assertFalse(afterSecondUplift.isBefore(afterFirstUplift));
        Assertions.assertFalse(afterSecondUplift.isAfter(Instant.now()));
        Assertions.assertEquals(
                report1.getId(), findPersistedBan(ban.getId()).getModerationReport().getId());
    }

    @Test
    public void shouldUpliftBan_WhenBanNotFound() {
        final Long nonExistentBanId = 99999L;

        Assertions.assertDoesNotThrow(() -> userBanDao.upliftBan(nonExistentBanId));

        Assertions.assertEquals(0, countUserBans());
    }

    @Test
    public void shouldUpliftBan_MakesExpiredBanInactive() {
        final UserBan ban = userBanDao.createBan(report1, FUTURE);
        flushAndClear();
        final Optional<UserBan> activeBefore = userBanDao.findActiveBanForUser(2L, NOW);

        userBanDao.upliftBan(ban.getId());

        final Instant queryTime = Instant.now().plus(1, ChronoUnit.SECONDS);
        final Optional<UserBan> activeAfter = userBanDao.findActiveBanForUser(2L, queryTime);
        Assertions.assertTrue(activeBefore.isPresent());
        Assertions.assertTrue(activeAfter.isEmpty(), "Uplifted ban should not be active");
        Assertions.assertEquals(1, countUserBans());

        final UserBan persisted = findPersistedBan(ban.getId());
        Assertions.assertEquals(report1.getId(), persisted.getModerationReport().getId());
        Assertions.assertFalse(persisted.getBannedUntil().isAfter(queryTime));
    }

    @Test
    public void shouldHandleMultipleBansPerUser_ReturnsMostRecent() {
        final long bannedUserId = 2L;
        final UserBan ban1 = userBanDao.createBan(report1, NOW.plus(1, ChronoUnit.DAYS));
        final UserBan ban2 = userBanDao.createBan(report2, NOW.plus(2, ChronoUnit.DAYS));
        final UserBan ban3 = userBanDao.createBan(report4, NOW.plus(3, ChronoUnit.DAYS));
        flushAndClear();

        final Optional<UserBan> latest = userBanDao.findLatestBanForUser(bannedUserId);

        Assertions.assertTrue(latest.isPresent());
        Assertions.assertEquals(ban3.getId(), latest.get().getId());
        Assertions.assertEquals(3, countUserBans());
        assertPersistedBan(ban1.getId(), report1.getId(), NOW.plus(1, ChronoUnit.DAYS));
        assertPersistedBan(ban2.getId(), report2.getId(), NOW.plus(2, ChronoUnit.DAYS));
        assertPersistedBan(ban3.getId(), report4.getId(), NOW.plus(3, ChronoUnit.DAYS));
    }

    @Test
    public void shouldFindActiveBanForUser_WithMultipleBans_ReturnsLatestActive() {
        final long bannedUserId = 2L;
        final UserBan expiredBan = userBanDao.createBan(report1, PAST);
        final UserBan activeBan = userBanDao.createBan(report2, FUTURE);
        flushAndClear();

        final Optional<UserBan> result = userBanDao.findActiveBanForUser(bannedUserId, NOW);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(activeBan.getId(), result.get().getId());
        Assertions.assertEquals(2, countUserBans());
        assertPersistedBan(expiredBan.getId(), report1.getId(), PAST);
        assertPersistedBan(activeBan.getId(), report2.getId(), FUTURE);
    }

    private long countUserBans() {
        flushAndClear();
        return em.createQuery("SELECT COUNT(ub) FROM UserBan ub", Long.class).getSingleResult();
    }

    private void assertPersistedBan(
            final Long banId, final Long moderationReportId, final Instant bannedUntil) {
        final UserBan persisted = findPersistedBan(banId);
        Assertions.assertEquals(moderationReportId, persisted.getModerationReport().getId());
        Assertions.assertEquals(bannedUntil, persisted.getBannedUntil());
    }

    private UserBan findPersistedBan(final Long banId) {
        flushAndClear();
        final UserBan persisted = em.find(UserBan.class, banId);
        Assertions.assertNotNull(persisted);
        return persisted;
    }

    private void markReportAppealDecision(final Long reportId, final String appealDecision) {
        em.createNativeQuery(
                        "UPDATE moderation_reports SET appeal_decision = :appealDecision WHERE id = :id")
                .setParameter("appealDecision", appealDecision)
                .setParameter("id", reportId)
                .executeUpdate();
        flushAndClear();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
