package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.types.AppealDecision;
import ar.edu.itba.paw.models.types.ReportReason;
import ar.edu.itba.paw.models.types.ReportResolution;
import ar.edu.itba.paw.models.types.ReportStatus;
import ar.edu.itba.paw.models.types.ReportTargetType;
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
public class ModerationReportJdbcDaoTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired private ModerationReportDao moderationReportDao;

    @Autowired private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(
                "INSERT INTO users "
                        + "(id, username, email, name, last_name, phone, created_at, updated_at)"
                        + " VALUES "
                        + "(1, 'reporter', 'rep@test.com', 'Reporter', 'User', null,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                        + "(2, 'admin', 'admin@test.com', 'Admin', 'User', null,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                        + "(3, 'target', 'target@test.com', 'Target', 'User', null,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
                        + "(4, 'reporter2', 'rep2@test.com', 'Reporter2', 'User', null,"
                        + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
    }

    @Test
    public void testCreateReport() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        1L, ReportTargetType.USER, 3L, ReportReason.SPAM, "Too many messages");

        Assertions.assertNotNull(report.getId());
        Assertions.assertEquals(1L, report.getReporterUserId());
        Assertions.assertEquals(ReportTargetType.USER, report.getTargetType());
        Assertions.assertEquals(3L, report.getTargetId());
        Assertions.assertEquals(ReportReason.SPAM, report.getReason());
        Assertions.assertEquals("Too many messages", report.getDetails());
        Assertions.assertEquals(ReportStatus.PENDING, report.getStatus());
    }

    @Test
    public void testFindById() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        1L, ReportTargetType.USER, 3L, ReportReason.SPAM, "Too many messages");

        final Optional<ModerationReport> found = moderationReportDao.findById(report.getId());

        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(report.getId(), found.get().getId());
        Assertions.assertEquals(ReportTargetType.USER, found.get().getTargetType());
    }

    @Test
    public void testFindReportsByReporter() {
        moderationReportDao.createReport(1L, ReportTargetType.USER, 3L, ReportReason.SPAM, "First");
        moderationReportDao.createReport(
                1L, ReportTargetType.USER, 2L, ReportReason.HARASSMENT, "Second");

        final List<ModerationReport> reports = moderationReportDao.findReportsByReporter(1L);

        Assertions.assertEquals(2, reports.size());
        Assertions.assertEquals(1L, reports.get(0).getReporterUserId());
        Assertions.assertEquals(1L, reports.get(1).getReporterUserId());
    }

    @Test
    public void testFindActiveReportsAndCount() {
        // Pending
        moderationReportDao.createReport(
                1L, ReportTargetType.USER, 3L, ReportReason.SPAM, "Pending");

        // Resolve one report so it is no longer active
        ModerationReport toResolve =
                moderationReportDao.createReport(
                        1L, ReportTargetType.USER, 2L, ReportReason.SPAM, "Resolved");
        moderationReportDao.markUnderReview(toResolve.getId(), 2L, NOW);
        moderationReportDao.resolveReport(
                toResolve.getId(),
                2L,
                ReportResolution.DISMISSED,
                "Nothing",
                NOW,
                ReportStatus.RESOLVED);

        final List<ModerationReport> activeReports =
                moderationReportDao.findReports(List.of(), List.of(ReportStatus.PENDING));
        final int activeCount = moderationReportDao.countActiveReportsByReporter(1L);

        Assertions.assertEquals(1, activeReports.size());
        Assertions.assertEquals("Pending", activeReports.get(0).getDetails());
        Assertions.assertEquals(1, activeCount);
    }

    @Test
    public void testMarkUnderReview() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        1L, ReportTargetType.USER, 3L, ReportReason.SPAM, "Too many messages");

        final boolean success = moderationReportDao.markUnderReview(report.getId(), 2L, NOW);

        Assertions.assertTrue(success);
        ModerationReport updated = moderationReportDao.findById(report.getId()).get();
        Assertions.assertEquals(ReportStatus.UNDER_REVIEW, updated.getStatus());
        Assertions.assertEquals(2L, updated.getReviewedByUserId());
        Assertions.assertNotNull(updated.getReviewedAt());
    }

    @Test
    public void testResolveReport() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        1L, ReportTargetType.USER, 3L, ReportReason.SPAM, "Too many messages");
        moderationReportDao.markUnderReview(report.getId(), 2L, NOW);

        final boolean success =
                moderationReportDao.resolveReport(
                        report.getId(),
                        2L,
                        ReportResolution.DISMISSED,
                        "No issue found",
                        NOW,
                        ReportStatus.RESOLVED);

        Assertions.assertTrue(success);
        ModerationReport resolved = moderationReportDao.findById(report.getId()).get();
        Assertions.assertEquals(ReportStatus.RESOLVED, resolved.getStatus());
        Assertions.assertEquals(ReportResolution.DISMISSED, resolved.getResolution());
        Assertions.assertEquals("No issue found", resolved.getResolutionDetails());
    }

    @Test
    public void testAppealReport() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        1L, ReportTargetType.USER, 3L, ReportReason.SPAM, "Too many messages");
        moderationReportDao.markUnderReview(report.getId(), 2L, NOW);
        moderationReportDao.resolveReport(
                report.getId(),
                2L,
                ReportResolution.DISMISSED,
                "No issue found",
                NOW,
                ReportStatus.RESOLVED);

        final boolean success = moderationReportDao.appealReport(report.getId(), "I disagree", NOW);

        Assertions.assertTrue(success);
        ModerationReport appealed = moderationReportDao.findById(report.getId()).get();
        Assertions.assertEquals(ReportStatus.APPEALED, appealed.getStatus());
        Assertions.assertEquals("I disagree", appealed.getAppealReason());
        Assertions.assertEquals(1, appealed.getAppealCount());
    }

    @Test
    public void testFinalizeAppeal() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        1L, ReportTargetType.USER, 3L, ReportReason.SPAM, "Too many messages");
        moderationReportDao.markUnderReview(report.getId(), 2L, NOW);
        moderationReportDao.resolveReport(
                report.getId(),
                2L,
                ReportResolution.DISMISSED,
                "No issue found",
                NOW,
                ReportStatus.RESOLVED);
        moderationReportDao.appealReport(report.getId(), "I disagree", NOW);

        final boolean success =
                moderationReportDao.finalizeAppeal(report.getId(), 2L, AppealDecision.LIFTED, NOW);

        Assertions.assertTrue(success);
        ModerationReport finalized = moderationReportDao.findById(report.getId()).get();
        Assertions.assertEquals(ReportStatus.FINALIZED, finalized.getStatus());
        Assertions.assertEquals(AppealDecision.LIFTED, finalized.getAppealDecision());
        Assertions.assertEquals(2L, finalized.getAppealResolvedByUserId());
        Assertions.assertNotNull(finalized.getAppealResolvedAt());
    }

    @Test
    public void testMarkUnderReviewFailsWhenNotPending() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        1L, ReportTargetType.USER, 3L, ReportReason.SPAM, "Too many messages");
        moderationReportDao.markUnderReview(report.getId(), 2L, NOW);
        moderationReportDao.resolveReport(
                report.getId(),
                2L,
                ReportResolution.DISMISSED,
                "Nothing",
                NOW,
                ReportStatus.RESOLVED);

        final boolean success = moderationReportDao.markUnderReview(report.getId(), 2L, NOW);
        Assertions.assertFalse(success);
    }

    @Test
    public void testResolveReportFailsWhenNotPendingOrUnderReview() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        1L, ReportTargetType.USER, 3L, ReportReason.SPAM, "Too many messages");
        moderationReportDao.markUnderReview(report.getId(), 2L, NOW);
        moderationReportDao.resolveReport(
                report.getId(),
                2L,
                ReportResolution.DISMISSED,
                "Nothing",
                NOW,
                ReportStatus.RESOLVED);

        final boolean success =
                moderationReportDao.resolveReport(
                        report.getId(),
                        2L,
                        ReportResolution.USER_BANNED,
                        "Ban",
                        NOW,
                        ReportStatus.RESOLVED);
        Assertions.assertFalse(success);
    }

    @Test
    public void testAppealReportFailsWhenNotResolvedOrAlreadyAppealed() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        1L, ReportTargetType.USER, 3L, ReportReason.SPAM, "Too many messages");

        final boolean success1 = moderationReportDao.appealReport(report.getId(), "Appeal", NOW);
        Assertions.assertFalse(success1);

        moderationReportDao.markUnderReview(report.getId(), 2L, NOW);
        moderationReportDao.resolveReport(
                report.getId(),
                2L,
                ReportResolution.DISMISSED,
                "Nothing",
                NOW,
                ReportStatus.RESOLVED);

        final boolean success2 = moderationReportDao.appealReport(report.getId(), "Appeal", NOW);
        Assertions.assertTrue(success2);

        final boolean success3 =
                moderationReportDao.appealReport(report.getId(), "Appeal again", NOW);
        Assertions.assertFalse(success3);
    }

    @Test
    public void testFinalizeAppealFailsWhenNotAppealed() {
        final ModerationReport report =
                moderationReportDao.createReport(
                        1L, ReportTargetType.USER, 3L, ReportReason.SPAM, "Too many messages");

        final boolean success =
                moderationReportDao.finalizeAppeal(report.getId(), 2L, AppealDecision.UPHELD, NOW);
        Assertions.assertFalse(success);
    }

    @Test
    public void shouldFindLatestUserBanReportByTargetUserId_WhenReportsExist() {
        final long targetUserId = 3L;

        final ModerationReport report1 =
                moderationReportDao.createReport(
                        1L, ReportTargetType.USER, targetUserId, ReportReason.SPAM, "First ban");
        moderationReportDao.markUnderReview(report1.getId(), 2L, NOW);
        moderationReportDao.resolveReport(
                report1.getId(),
                2L,
                ReportResolution.USER_BANNED,
                "Ban details",
                NOW,
                ReportStatus.RESOLVED);

        final ModerationReport report2 =
                moderationReportDao.createReport(
                        2L,
                        ReportTargetType.USER,
                        targetUserId,
                        ReportReason.HARASSMENT,
                        "Second ban");
        moderationReportDao.markUnderReview(report2.getId(), 2L, NOW);
        moderationReportDao.resolveReport(
                report2.getId(),
                2L,
                ReportResolution.USER_BANNED,
                "Another ban",
                NOW,
                ReportStatus.RESOLVED);

        final Optional<ModerationReport> latest =
                moderationReportDao.findLatestUserBanReportByTargetUserId(targetUserId);

        Assertions.assertTrue(latest.isPresent());
        Assertions.assertEquals(report2.getId(), latest.get().getId());
        Assertions.assertEquals(targetUserId, latest.get().getTargetId());
        Assertions.assertEquals(ReportStatus.RESOLVED, latest.get().getStatus());
        Assertions.assertEquals(ReportResolution.USER_BANNED, latest.get().getResolution());
    }

    @Test
    public void shouldFindLatestUserBanReportByTargetUserId_WhenNoReportsExist() {
        final long targetUserWithNoBans = 2L;

        final Optional<ModerationReport> result =
                moderationReportDao.findLatestUserBanReportByTargetUserId(targetUserWithNoBans);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void shouldFindLatestUserBanReportByTargetUserId_OnlyReturnsUserBans() {
        final long targetUserId = 3L;

        final ModerationReport banReport =
                moderationReportDao.createReport(
                        1L, ReportTargetType.USER, targetUserId, ReportReason.SPAM, "Ban");
        moderationReportDao.markUnderReview(banReport.getId(), 2L, NOW);
        moderationReportDao.resolveReport(
                banReport.getId(),
                2L,
                ReportResolution.USER_BANNED,
                "Banned",
                NOW,
                ReportStatus.RESOLVED);

        final ModerationReport dismissedReport =
                moderationReportDao.createReport(
                        2L, ReportTargetType.USER, targetUserId, ReportReason.SPAM, "Dismissed");
        moderationReportDao.markUnderReview(dismissedReport.getId(), 2L, NOW);
        moderationReportDao.resolveReport(
                dismissedReport.getId(),
                2L,
                ReportResolution.DISMISSED,
                "No violation",
                NOW,
                ReportStatus.RESOLVED);

        final Optional<ModerationReport> result =
                moderationReportDao.findLatestUserBanReportByTargetUserId(targetUserId);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(banReport.getId(), result.get().getId());
        Assertions.assertEquals(ReportResolution.USER_BANNED, result.get().getResolution());
    }

    @Test
    public void shouldFindLatestUserBanReportByTargetUserId_IgnoresPendingReports() {
        final long targetUserId = 3L;

        final ModerationReport resolvedBan =
                moderationReportDao.createReport(
                        1L, ReportTargetType.USER, targetUserId, ReportReason.SPAM, "Resolved ban");
        moderationReportDao.markUnderReview(resolvedBan.getId(), 2L, NOW);
        moderationReportDao.resolveReport(
                resolvedBan.getId(),
                2L,
                ReportResolution.USER_BANNED,
                "Banned",
                NOW,
                ReportStatus.RESOLVED);

        moderationReportDao.createReport(
                2L, ReportTargetType.USER, targetUserId, ReportReason.HARASSMENT, "Pending ban");

        final Optional<ModerationReport> result =
                moderationReportDao.findLatestUserBanReportByTargetUserId(targetUserId);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(resolvedBan.getId(), result.get().getId());
        Assertions.assertEquals(ReportStatus.RESOLVED, result.get().getStatus());
    }

    @Test
    public void shouldFindLatestUserBanReportByTargetUserId_IsolatedPerUser() {
        final long user1 = 2L;
        final long user2 = 3L;

        final ModerationReport ban1 =
                moderationReportDao.createReport(
                        1L, ReportTargetType.USER, user1, ReportReason.SPAM, "Ban user1");
        moderationReportDao.markUnderReview(ban1.getId(), 2L, NOW);
        moderationReportDao.resolveReport(
                ban1.getId(),
                2L,
                ReportResolution.USER_BANNED,
                "Banned",
                NOW,
                ReportStatus.RESOLVED);

        final ModerationReport ban2 =
                moderationReportDao.createReport(
                        2L, ReportTargetType.USER, user2, ReportReason.HARASSMENT, "Ban user2");
        moderationReportDao.markUnderReview(ban2.getId(), 2L, NOW);
        moderationReportDao.resolveReport(
                ban2.getId(),
                2L,
                ReportResolution.USER_BANNED,
                "Banned",
                NOW,
                ReportStatus.RESOLVED);

        final Optional<ModerationReport> result1 =
                moderationReportDao.findLatestUserBanReportByTargetUserId(user1);
        final Optional<ModerationReport> result2 =
                moderationReportDao.findLatestUserBanReportByTargetUserId(user2);

        Assertions.assertTrue(result1.isPresent());
        Assertions.assertEquals(ban1.getId(), result1.get().getId());

        Assertions.assertTrue(result2.isPresent());
        Assertions.assertEquals(ban2.getId(), result2.get().getId());

        Assertions.assertNotEquals(result1.get().getId(), result2.get().getId());
    }
}
