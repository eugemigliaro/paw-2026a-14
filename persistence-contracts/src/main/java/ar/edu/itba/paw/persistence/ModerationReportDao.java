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

public interface ModerationReportDao {

    ModerationReport createReport(
            Long reporterUserId,
            ReportTargetType targetType,
            Long targetId,
            ReportReason reason,
            String details);

    Optional<ModerationReport> findById(Long reportId);

    List<ModerationReport> findReportsByReporter(Long reporterUserId);

    List<ModerationReport> findReportsByReporter(
            Long reporterUserId, List<ReportTargetType> targetTypes, List<ReportStatus> statuses);

    List<ModerationReport> findReports();

    List<ModerationReport> findReports(
            List<ReportTargetType> targetTypes, List<ReportStatus> statuses);

    PaginatedResult<ModerationReport> findReports(
            List<ReportTargetType> targetTypes,
            List<ReportStatus> statuses,
            int page,
            int pageSize);

    Optional<ModerationReport> findLatestUserBanReportByTargetUserId(Long targetUserId);

    int countActiveReportsByReporter(Long reporterUserId);

    boolean markUnderReview(Long reportId, Long reviewedByUserId, Instant reviewedAt);

    boolean resolveReport(
            Long reportId,
            Long reviewedByUserId,
            ReportResolution resolution,
            String resolutionDetails,
            Instant reviewedAt,
            ReportStatus nextStatus);

    boolean appealReport(Long reportId, String appealReason, Instant appealedAt);

    boolean finalizeAppeal(
            Long reportId,
            Long appealResolvedByUserId,
            AppealDecision appealDecision,
            Instant appealResolvedAt);

    PaginatedResult<ModerationReport> findReportsByReporter(
            Long reporterUserId,
            List<ReportTargetType> targetTypes,
            List<ReportStatus> statuses,
            int page,
            int pageSize);
}
