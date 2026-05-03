package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.AppealDecision;
import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.ReportReason;
import ar.edu.itba.paw.models.ReportResolution;
import ar.edu.itba.paw.models.ReportStatus;
import ar.edu.itba.paw.models.ReportTargetType;
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
}
