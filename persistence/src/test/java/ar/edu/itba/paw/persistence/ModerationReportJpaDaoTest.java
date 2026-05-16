package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.AppealDecision;
import ar.edu.itba.paw.models.types.ReportReason;
import ar.edu.itba.paw.models.types.ReportResolution;
import ar.edu.itba.paw.models.types.ReportStatus;
import ar.edu.itba.paw.models.types.ReportTargetType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
public class ModerationReportJpaDaoTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired private ModerationReportDao moderationReportDao;

    @PersistenceContext private EntityManager em;

    private User reporter;
    private User admin;
    private User target;

    @BeforeEach
    public void setUp() {
        em.createNativeQuery(
                        "INSERT INTO users "
                                + "(id, username, email, name, last_name, phone, created_at, updated_at)"
                                + " VALUES "
                                + "(1, 'reporter', 'rep@test.com', 'Reporter', 'User', null,"
                                + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                                + "(2, 'admin', 'admin@test.com', 'Admin', 'User', null,"
                                + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                                + "(3, 'target', 'target@test.com', 'Target', 'User', null,"
                                + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .executeUpdate();

        reporter = em.find(User.class, 1L);
        admin = em.find(User.class, 2L);
        target = em.find(User.class, 3L);
    }

    @Test
    public void testCreateReport() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        target.getId(),
                        ReportReason.SPAM,
                        "Too many messages");

        Assertions.assertNotNull(report.getId());
        Assertions.assertEquals(reporter.getId(), report.getReporter().getId());
        Assertions.assertEquals(ReportTargetType.USER, report.getTargetType());
        Assertions.assertEquals(target.getId(), report.getTargetId());
        Assertions.assertEquals(ReportReason.SPAM, report.getReason());
        Assertions.assertEquals("Too many messages", report.getDetails());
        Assertions.assertEquals(ReportStatus.PENDING, report.getStatus());
        assertPersistedStatus(report.getId(), ReportStatus.PENDING);
    }

    @Test
    public void testFindById() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        target.getId(),
                        ReportReason.SPAM,
                        "Too many messages");

        final Optional<ModerationReport> found = moderationReportDao.findById(report.getId());

        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(report.getId(), found.get().getId());
        Assertions.assertEquals(ReportTargetType.USER, found.get().getTargetType());
        assertPersistedStatus(report.getId(), ReportStatus.PENDING);
    }

    @Test
    public void testFindReportsByReporter() {
        moderationReportDao.createReport(
                reporter, ReportTargetType.USER, target.getId(), ReportReason.SPAM, "First");
        moderationReportDao.createReport(
                reporter, ReportTargetType.USER, admin.getId(), ReportReason.HARASSMENT, "Second");

        final List<ModerationReport> reports = moderationReportDao.findReportsByReporter(reporter);

        Assertions.assertEquals(2, reports.size());
        Assertions.assertEquals(reporter.getId(), reports.get(0).getReporter().getId());
        Assertions.assertEquals(reporter.getId(), reports.get(1).getReporter().getId());
        assertPersistedStatus(reports.get(0).getId(), ReportStatus.PENDING);
        assertPersistedStatus(reports.get(1).getId(), ReportStatus.PENDING);
    }

    @Test
    public void testFindActiveReportsAndCount() {
        moderationReportDao.createReport(
                reporter, ReportTargetType.USER, target.getId(), ReportReason.SPAM, "Pending");

        final ModerationReport toResolve =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        admin.getId(),
                        ReportReason.SPAM,
                        "Resolved");
        moderationReportDao.markUnderReview(toResolve.getId(), admin, NOW);
        moderationReportDao.resolveReport(
                toResolve.getId(),
                admin,
                ReportResolution.DISMISSED,
                "Nothing",
                NOW,
                ReportStatus.RESOLVED);

        final List<ModerationReport> activeReports =
                moderationReportDao.findReports(List.of(), List.of(ReportStatus.PENDING));
        final int activeCount = moderationReportDao.countActiveReportsByReporter(reporter);

        Assertions.assertEquals(1, activeReports.size());
        Assertions.assertEquals("Pending", activeReports.get(0).getDetails());
        Assertions.assertEquals(1, activeCount);
        assertPersistedStatus(activeReports.get(0).getId(), ReportStatus.PENDING);
    }

    @Test
    public void testMarkUnderReview() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        target.getId(),
                        ReportReason.SPAM,
                        "Too many messages");

        final boolean success = moderationReportDao.markUnderReview(report.getId(), admin, NOW);

        Assertions.assertTrue(success);
        flushAndClear();
        final ModerationReport updated = moderationReportDao.findById(report.getId()).get();
        Assertions.assertEquals(ReportStatus.UNDER_REVIEW, updated.getStatus());
        Assertions.assertEquals(admin.getId(), updated.getReviewer().getId());
        Assertions.assertNotNull(updated.getReviewedAt());
        assertPersistedStatus(report.getId(), ReportStatus.UNDER_REVIEW);
    }

    @Test
    public void testResolveReport() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        target.getId(),
                        ReportReason.SPAM,
                        "Too many messages");
        moderationReportDao.markUnderReview(report.getId(), admin, NOW);

        final boolean success =
                moderationReportDao.resolveReport(
                        report.getId(),
                        admin,
                        ReportResolution.DISMISSED,
                        "No issue found",
                        NOW,
                        ReportStatus.RESOLVED);

        Assertions.assertTrue(success);
        flushAndClear();
        final ModerationReport resolved = moderationReportDao.findById(report.getId()).get();
        Assertions.assertEquals(ReportStatus.RESOLVED, resolved.getStatus());
        Assertions.assertEquals(ReportResolution.DISMISSED, resolved.getResolution());
        Assertions.assertEquals("No issue found", resolved.getResolutionDetails());
        assertPersistedStatus(report.getId(), ReportStatus.RESOLVED);
    }

    @Test
    public void testAppealReport() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        target.getId(),
                        ReportReason.SPAM,
                        "Too many messages");
        moderationReportDao.markUnderReview(report.getId(), admin, NOW);
        moderationReportDao.resolveReport(
                report.getId(),
                admin,
                ReportResolution.DISMISSED,
                "No issue found",
                NOW,
                ReportStatus.RESOLVED);

        final boolean success = moderationReportDao.appealReport(report.getId(), "I disagree", NOW);

        Assertions.assertTrue(success);
        flushAndClear();
        final ModerationReport appealed = moderationReportDao.findById(report.getId()).get();
        Assertions.assertEquals(ReportStatus.APPEALED, appealed.getStatus());
        Assertions.assertEquals("I disagree", appealed.getAppealReason());
        Assertions.assertEquals(1, appealed.getAppealCount());
        assertPersistedStatus(report.getId(), ReportStatus.APPEALED);
    }

    @Test
    public void testFinalizeAppeal() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        target.getId(),
                        ReportReason.SPAM,
                        "Too many messages");
        moderationReportDao.markUnderReview(report.getId(), admin, NOW);
        moderationReportDao.resolveReport(
                report.getId(),
                admin,
                ReportResolution.DISMISSED,
                "No issue found",
                NOW,
                ReportStatus.RESOLVED);
        moderationReportDao.appealReport(report.getId(), "I disagree", NOW);

        final boolean success =
                moderationReportDao.finalizeAppeal(
                        report.getId(), admin, AppealDecision.LIFTED, NOW);

        Assertions.assertTrue(success);
        flushAndClear();
        final ModerationReport finalized = moderationReportDao.findById(report.getId()).get();
        Assertions.assertEquals(ReportStatus.FINALIZED, finalized.getStatus());
        Assertions.assertEquals(AppealDecision.LIFTED, finalized.getAppealDecision());
        Assertions.assertEquals(admin.getId(), finalized.getAppealResolvedBy().getId());
        Assertions.assertNotNull(finalized.getAppealResolvedAt());
        assertPersistedStatus(report.getId(), ReportStatus.FINALIZED);
    }

    @Test
    public void testMarkUnderReviewFailsWhenNotPending() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        target.getId(),
                        ReportReason.SPAM,
                        "Too many messages");
        moderationReportDao.markUnderReview(report.getId(), admin, NOW);
        moderationReportDao.resolveReport(
                report.getId(),
                admin,
                ReportResolution.DISMISSED,
                "Nothing",
                NOW,
                ReportStatus.RESOLVED);

        final boolean success = moderationReportDao.markUnderReview(report.getId(), admin, NOW);
        Assertions.assertFalse(success);
        assertPersistedStatus(report.getId(), ReportStatus.RESOLVED);
    }

    @Test
    public void testResolveReportFailsWhenNotPendingOrUnderReview() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        target.getId(),
                        ReportReason.SPAM,
                        "Too many messages");
        moderationReportDao.markUnderReview(report.getId(), admin, NOW);
        moderationReportDao.resolveReport(
                report.getId(),
                admin,
                ReportResolution.DISMISSED,
                "Nothing",
                NOW,
                ReportStatus.RESOLVED);

        final boolean success =
                moderationReportDao.resolveReport(
                        report.getId(),
                        admin,
                        ReportResolution.USER_BANNED,
                        "Ban",
                        NOW,
                        ReportStatus.RESOLVED);
        Assertions.assertFalse(success);
        assertPersistedStatus(report.getId(), ReportStatus.RESOLVED);
    }

    @Test
    public void testAppealReportFailsWhenNotResolvedOrAlreadyAppealed() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        target.getId(),
                        ReportReason.SPAM,
                        "Too many messages");

        final boolean success1 = moderationReportDao.appealReport(report.getId(), "Appeal", NOW);
        Assertions.assertFalse(success1);
        assertPersistedStatus(report.getId(), ReportStatus.PENDING);

        moderationReportDao.markUnderReview(report.getId(), admin, NOW);
        moderationReportDao.resolveReport(
                report.getId(),
                admin,
                ReportResolution.DISMISSED,
                "Nothing",
                NOW,
                ReportStatus.RESOLVED);

        final boolean success2 = moderationReportDao.appealReport(report.getId(), "Appeal", NOW);
        Assertions.assertTrue(success2);
        assertPersistedStatus(report.getId(), ReportStatus.APPEALED);

        final boolean success3 =
                moderationReportDao.appealReport(report.getId(), "Appeal again", NOW);
        Assertions.assertFalse(success3);
        assertPersistedStatus(report.getId(), ReportStatus.APPEALED);
    }

    @Test
    public void testFinalizeAppealFailsWhenNotAppealed() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        target.getId(),
                        ReportReason.SPAM,
                        "Too many messages");

        final boolean success =
                moderationReportDao.finalizeAppeal(
                        report.getId(), admin, AppealDecision.UPHELD, NOW);
        Assertions.assertFalse(success);
        assertPersistedStatus(report.getId(), ReportStatus.PENDING);
    }

    @Test
    public void shouldFindLatestUserBanReportByTargetUserId_WhenReportsExist() {
        final ModerationReport report1 =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        target.getId(),
                        ReportReason.SPAM,
                        "First ban");
        moderationReportDao.markUnderReview(report1.getId(), admin, NOW);
        moderationReportDao.resolveReport(
                report1.getId(),
                admin,
                ReportResolution.USER_BANNED,
                "Ban details",
                NOW,
                ReportStatus.RESOLVED);

        final ModerationReport report2 =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        target.getId(),
                        ReportReason.HARASSMENT,
                        "Second ban");
        moderationReportDao.markUnderReview(report2.getId(), admin, NOW);
        moderationReportDao.resolveReport(
                report2.getId(),
                admin,
                ReportResolution.USER_BANNED,
                "Another ban",
                NOW,
                ReportStatus.RESOLVED);

        final Optional<ModerationReport> latest =
                moderationReportDao.findLatestUserBanReportByTargetUser(target);

        Assertions.assertTrue(latest.isPresent());
        Assertions.assertEquals(report2.getId(), latest.get().getId());
        Assertions.assertEquals(target.getId(), latest.get().getTargetId());
        Assertions.assertEquals(ReportStatus.RESOLVED, latest.get().getStatus());
        Assertions.assertEquals(ReportResolution.USER_BANNED, latest.get().getResolution());
        assertPersistedStatus(report1.getId(), ReportStatus.RESOLVED);
        assertPersistedStatus(report2.getId(), ReportStatus.RESOLVED);
    }

    @Test
    public void shouldFindLatestUserBanReportByTargetUserId_WhenNoReportsExist() {
        final Optional<ModerationReport> result =
                moderationReportDao.findLatestUserBanReportByTargetUser(admin);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void shouldFindLatestUserBanReportByTargetUserId_OnlyReturnsUserBans() {
        final ModerationReport banReport =
                moderationReportDao.createReport(
                        reporter, ReportTargetType.USER, target.getId(), ReportReason.SPAM, "Ban");
        moderationReportDao.markUnderReview(banReport.getId(), admin, NOW);
        moderationReportDao.resolveReport(
                banReport.getId(),
                admin,
                ReportResolution.USER_BANNED,
                "Banned",
                NOW,
                ReportStatus.RESOLVED);

        final ModerationReport dismissedReport =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        target.getId(),
                        ReportReason.SPAM,
                        "Dismissed");
        moderationReportDao.markUnderReview(dismissedReport.getId(), admin, NOW);
        moderationReportDao.resolveReport(
                dismissedReport.getId(),
                admin,
                ReportResolution.DISMISSED,
                "No violation",
                NOW,
                ReportStatus.RESOLVED);

        final Optional<ModerationReport> result =
                moderationReportDao.findLatestUserBanReportByTargetUser(target);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(banReport.getId(), result.get().getId());
        Assertions.assertEquals(ReportResolution.USER_BANNED, result.get().getResolution());
        assertPersistedStatus(banReport.getId(), ReportStatus.RESOLVED);
        assertPersistedStatus(dismissedReport.getId(), ReportStatus.RESOLVED);
    }

    @Test
    public void shouldFindLatestUserBanReportByTargetUserId_IgnoresPendingReports() {
        final ModerationReport resolvedBan =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        target.getId(),
                        ReportReason.SPAM,
                        "Resolved ban");
        moderationReportDao.markUnderReview(resolvedBan.getId(), admin, NOW);
        moderationReportDao.resolveReport(
                resolvedBan.getId(),
                admin,
                ReportResolution.USER_BANNED,
                "Banned",
                NOW,
                ReportStatus.RESOLVED);

        moderationReportDao.createReport(
                reporter,
                ReportTargetType.USER,
                target.getId(),
                ReportReason.HARASSMENT,
                "Pending ban");

        final Optional<ModerationReport> result =
                moderationReportDao.findLatestUserBanReportByTargetUser(target);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(resolvedBan.getId(), result.get().getId());
        Assertions.assertEquals(ReportStatus.RESOLVED, result.get().getStatus());
        assertPersistedStatus(resolvedBan.getId(), ReportStatus.RESOLVED);
    }

    @Test
    public void shouldFindLatestUserBanReportByTargetUserId_IsolatedPerUser() {
        final ModerationReport ban1 =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        admin.getId(),
                        ReportReason.SPAM,
                        "Ban user1");
        moderationReportDao.markUnderReview(ban1.getId(), admin, NOW);
        moderationReportDao.resolveReport(
                ban1.getId(),
                admin,
                ReportResolution.USER_BANNED,
                "Banned",
                NOW,
                ReportStatus.RESOLVED);

        final ModerationReport ban2 =
                moderationReportDao.createReport(
                        reporter,
                        ReportTargetType.USER,
                        target.getId(),
                        ReportReason.HARASSMENT,
                        "Ban user2");
        moderationReportDao.markUnderReview(ban2.getId(), admin, NOW);
        moderationReportDao.resolveReport(
                ban2.getId(),
                admin,
                ReportResolution.USER_BANNED,
                "Banned",
                NOW,
                ReportStatus.RESOLVED);

        final Optional<ModerationReport> result1 =
                moderationReportDao.findLatestUserBanReportByTargetUser(admin);
        final Optional<ModerationReport> result2 =
                moderationReportDao.findLatestUserBanReportByTargetUser(target);

        Assertions.assertTrue(result1.isPresent());
        Assertions.assertEquals(ban1.getId(), result1.get().getId());

        Assertions.assertTrue(result2.isPresent());
        Assertions.assertEquals(ban2.getId(), result2.get().getId());

        Assertions.assertNotEquals(result1.get().getId(), result2.get().getId());
        assertPersistedStatus(ban1.getId(), ReportStatus.RESOLVED);
        assertPersistedStatus(ban2.getId(), ReportStatus.RESOLVED);
    }

    private void assertPersistedStatus(final Long reportId, final ReportStatus expectedStatus) {
        flushAndClear();
        final String status =
                (String)
                        em.createNativeQuery("SELECT status FROM moderation_reports WHERE id = :id")
                                .setParameter("id", reportId)
                                .getSingleResult();
        Assertions.assertEquals(expectedStatus.getDbValue(), status);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
