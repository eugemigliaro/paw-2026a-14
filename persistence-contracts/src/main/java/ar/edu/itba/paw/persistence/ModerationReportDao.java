package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.ReportReason;
import ar.edu.itba.paw.models.ReportResolution;
import ar.edu.itba.paw.models.ReportStatus;
import ar.edu.itba.paw.models.ReportTargetType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ModerationReportDao {

    ModerationReport createReport(
            Long reporterUserId,
            ReportTargetType targetType,
            Long targetId,
            String targetKey,
            ReportReason reason,
            String details);

    Optional<ModerationReport> findById(Long reportId);

    List<ModerationReport> findReportsByReporter(Long reporterUserId);

    List<ModerationReport> findActiveReports();

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
            ReportResolution appealResolution,
            Instant appealResolvedAt);
}
