package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.User;
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
            User reporter,
            ReportTargetType targetType,
            Long targetId,
            ReportReason reason,
            String details);

    Optional<ModerationReport> findById(Long reportId);

    List<ModerationReport> findReportsByReporter(User reporter);

    List<ModerationReport> findReportsByReporter(
            User reporter, List<ReportTargetType> targetTypes, List<ReportStatus> statuses);

    List<ModerationReport> findReports();

    List<ModerationReport> findReports(
            List<ReportTargetType> targetTypes, List<ReportStatus> statuses);

    PaginatedResult<ModerationReport> findReports(
            List<ReportTargetType> targetTypes,
            List<ReportStatus> statuses,
            int page,
            int pageSize);

    Optional<ModerationReport> findLatestUserBanReportByTargetUserId(User targetUser);

    int countActiveReportsByReporter(User reporter);

    boolean markUnderReview(Long reportId, User reviewedBy, Instant reviewedAt);

    boolean resolveReport(
            Long reportId,
            User reviewedBy,
            ReportResolution resolution,
            String resolutionDetails,
            Instant reviewedAt,
            ReportStatus nextStatus);

    boolean appealReport(Long reportId, String appealReason, Instant appealedAt);

    boolean finalizeAppeal(
            Long reportId,
            User appealResolvedBy,
            AppealDecision appealDecision,
            Instant appealResolvedAt);

    PaginatedResult<ModerationReport> findReportsByReporter(
            User reporter,
            List<ReportTargetType> targetTypes,
            List<ReportStatus> statuses,
            int page,
            int pageSize);
}
