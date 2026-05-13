package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.types.AppealDecision;
import ar.edu.itba.paw.models.types.ReportReason;
import ar.edu.itba.paw.models.types.ReportResolution;
import ar.edu.itba.paw.models.types.ReportStatus;
import ar.edu.itba.paw.models.types.ReportTargetType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ModerationReportJpaDao implements ModerationReportDao {

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public ModerationReport createReport(
            final Long reporterUserId,
            final ReportTargetType targetType,
            final Long targetId,
            final ReportReason reason,
            final String details) {
        final Instant now = Instant.now();
        final ModerationReport report =
                new ModerationReport(
                        null,
                        reporterUserId,
                        targetType,
                        targetId,
                        reason,
                        details,
                        ReportStatus.PENDING,
                        null,
                        null,
                        null,
                        null,
                        null,
                        (short) 0,
                        null,
                        null,
                        null,
                        null,
                        now,
                        now);

        em.persist(report);

        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ModerationReport> findById(final Long reportId) {
        return Optional.ofNullable(em.find(ModerationReport.class, reportId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModerationReport> findReportsByReporter(final Long reporterUserId) {
        return findReportsByReporter(reporterUserId, List.of(), List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModerationReport> findReportsByReporter(
            final Long reporterUserId,
            final List<ReportTargetType> targetTypes,
            final List<ReportStatus> statuses) {
        return findReportsByReporter(reporterUserId, targetTypes, statuses, 1, Integer.MAX_VALUE)
                .getItems();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModerationReport> findReports() {
        return findReports(List.of(), List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModerationReport> findReports(
            final List<ReportTargetType> targetTypes, final List<ReportStatus> statuses) {
        return findReports(targetTypes, statuses, 1, Integer.MAX_VALUE).getItems();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<ModerationReport> findReports(
            final List<ReportTargetType> targetTypes,
            final List<ReportStatus> statuses,
            final int page,
            final int pageSize) {
        return findReportsInternal(null, targetTypes, statuses, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ModerationReport> findLatestUserBanReportByTargetUserId(
            final Long targetUserId) {
        final TypedQuery<ModerationReport> query =
                em.createQuery(
                        "FROM ModerationReport mr "
                                + "WHERE mr.targetType = :targetType "
                                + "AND mr.targetId = :targetUserId "
                                + "AND mr.resolution = :resolution "
                                + "ORDER BY mr.updatedAt DESC, mr.id DESC",
                        ModerationReport.class);
        query.setParameter("targetType", ReportTargetType.USER);
        query.setParameter("targetUserId", targetUserId);
        query.setParameter("resolution", ReportResolution.USER_BANNED);
        query.setMaxResults(1);

        return query.getResultList().stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public int countActiveReportsByReporter(final Long reporterUserId) {
        final TypedQuery<Long> query =
                em.createQuery(
                        "SELECT COUNT(mr) FROM ModerationReport mr "
                                + "WHERE mr.reporterUserId = :reporterUserId "
                                + "AND mr.status IN (:statuses)",
                        Long.class);
        query.setParameter("reporterUserId", reporterUserId);
        query.setParameter(
                "statuses",
                List.of(ReportStatus.PENDING, ReportStatus.UNDER_REVIEW, ReportStatus.APPEALED));

        return query.getSingleResult().intValue();
    }

    @Override
    @Transactional
    public boolean markUnderReview(
            final Long reportId, final Long reviewedByUserId, final Instant reviewedAt) {
        final ModerationReport report = findForUpdate(reportId);

        if (report == null || report.getStatus() != ReportStatus.PENDING) {
            return false;
        }

        report.setStatus(ReportStatus.UNDER_REVIEW);
        report.setReviewedByUserId(reviewedByUserId);
        report.setReviewedAt(reviewedAt);
        report.setUpdatedAt(Instant.now());

        return true;
    }

    @Override
    @Transactional
    public boolean resolveReport(
            final Long reportId,
            final Long reviewedByUserId,
            final ReportResolution resolution,
            final String resolutionDetails,
            final Instant reviewedAt,
            final ReportStatus nextStatus) {
        final ModerationReport report = findForUpdate(reportId);

        if (report == null
                || (report.getStatus() != ReportStatus.PENDING
                        && report.getStatus() != ReportStatus.UNDER_REVIEW)) {
            return false;
        }

        report.setStatus(nextStatus);
        report.setResolution(resolution);
        report.setResolutionDetails(resolutionDetails);
        report.setReviewedByUserId(reviewedByUserId);
        report.setReviewedAt(reviewedAt);
        report.setUpdatedAt(Instant.now());

        return true;
    }

    @Override
    @Transactional
    public boolean appealReport(
            final Long reportId, final String appealReason, final Instant appealedAt) {
        final ModerationReport report = findForUpdate(reportId);

        if (report == null
                || report.getStatus() != ReportStatus.RESOLVED
                || report.getAppealCount() != 0) {
            return false;
        }

        report.setStatus(ReportStatus.APPEALED);
        report.setAppealReason(appealReason);
        report.setAppealCount((short) 1);
        report.setAppealedAt(appealedAt);
        report.setUpdatedAt(Instant.now());

        return true;
    }

    @Override
    @Transactional
    public boolean finalizeAppeal(
            final Long reportId,
            final Long appealResolvedByUserId,
            final AppealDecision appealDecision,
            final Instant appealResolvedAt) {
        final ModerationReport report = findForUpdate(reportId);

        if (report == null || report.getStatus() != ReportStatus.APPEALED) {
            return false;
        }

        report.setStatus(ReportStatus.FINALIZED);
        report.setAppealDecision(appealDecision);
        report.setAppealResolvedByUserId(appealResolvedByUserId);
        report.setAppealResolvedAt(appealResolvedAt);
        report.setUpdatedAt(Instant.now());

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<ModerationReport> findReportsByReporter(
            final Long reporterUserId,
            final List<ReportTargetType> targetTypes,
            final List<ReportStatus> statuses,
            final int page,
            final int pageSize) {
        return findReportsInternal(reporterUserId, targetTypes, statuses, page, pageSize);
    }

    private PaginatedResult<ModerationReport> findReportsInternal(
            final Long reporterUserIdFilter,
            final List<ReportTargetType> targetTypes,
            final List<ReportStatus> statuses,
            final int page,
            final int pageSize) {
        final int safePage = page > 0 ? page : 1;
        final String baseJpql = buildFilteredQuery(reporterUserIdFilter, targetTypes, statuses);

        final String countJpql = baseJpql.replaceFirst("FROM", "SELECT COUNT(mr) FROM");
        final int totalCount =
                executeCountQuery(countJpql, reporterUserIdFilter, targetTypes, statuses);

        final String resultJpql = baseJpql + " ORDER BY mr.createdAt DESC, mr.id DESC";
        final int safeOffset = Math.max(0, (safePage - 1) * pageSize);

        final TypedQuery<ModerationReport> query =
                em.createQuery(resultJpql, ModerationReport.class);
        applyFilterParameters(query, reporterUserIdFilter, targetTypes, statuses);
        query.setFirstResult(safeOffset);
        query.setMaxResults(pageSize);

        final List<ModerationReport> items = query.getResultList();
        return new PaginatedResult<>(items, totalCount, safePage, pageSize);
    }

    private String buildFilteredQuery(
            final Long reporterUserIdFilter,
            final List<ReportTargetType> targetTypes,
            final List<ReportStatus> statuses) {
        final StringBuilder jpql = new StringBuilder("FROM ModerationReport mr WHERE 1=1");

        if (reporterUserIdFilter != null) {
            jpql.append(" AND mr.reporterUserId = :reporterUserId");
        }

        if (targetTypes != null && !targetTypes.isEmpty()) {
            jpql.append(" AND mr.targetType IN :targetTypes");
        }

        if (statuses != null && !statuses.isEmpty()) {
            jpql.append(" AND mr.status IN :statuses");
        }

        return jpql.toString();
    }

    private void applyFilterParameters(
            final TypedQuery<?> query,
            final Long reporterUserIdFilter,
            final List<ReportTargetType> targetTypes,
            final List<ReportStatus> statuses) {
        if (reporterUserIdFilter != null) {
            query.setParameter("reporterUserId", reporterUserIdFilter);
        }

        if (targetTypes != null && !targetTypes.isEmpty()) {
            query.setParameter("targetTypes", targetTypes);
        }

        if (statuses != null && !statuses.isEmpty()) {
            query.setParameter("statuses", statuses);
        }
    }

    private int executeCountQuery(
            final String countJpql,
            final Long reporterUserIdFilter,
            final List<ReportTargetType> targetTypes,
            final List<ReportStatus> statuses) {
        final TypedQuery<Long> query = em.createQuery(countJpql, Long.class);
        applyFilterParameters(query, reporterUserIdFilter, targetTypes, statuses);
        return query.getSingleResult().intValue();
    }

    private ModerationReport findForUpdate(final Long reportId) {
        final TypedQuery<ModerationReport> query =
                em.createQuery(
                                "FROM ModerationReport mr WHERE mr.id = :reportId",
                                ModerationReport.class)
                        .setParameter("reportId", reportId)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE);

        return query.getResultList().stream().findFirst().orElse(null);
    }
}
