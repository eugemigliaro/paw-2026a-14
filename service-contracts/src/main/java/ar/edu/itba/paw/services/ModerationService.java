package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.types.AppealDecision;
import ar.edu.itba.paw.models.types.ReportReason;
import ar.edu.itba.paw.models.types.ReportResolution;
import ar.edu.itba.paw.models.types.ReportStatus;
import ar.edu.itba.paw.models.types.ReportTargetType;
import java.util.List;
import java.util.Optional;

public interface ModerationService {

    Optional<UserBan> findActiveBan(Long userId);

    Optional<UserBan> findLatestBanForUser(Long userId);

    ModerationReport reportContent(
            Long reporterUserId,
            ReportTargetType targetType,
            Long targetId,
            ReportReason reason,
            String details);

    List<ModerationReport> findReports();

    List<ModerationReport> findReports(
            List<ReportTargetType> targetTypes, List<ReportStatus> statuses);

    PaginatedResult<ModerationReport> findReports(
            List<ReportTargetType> targetTypes,
            List<ReportStatus> statuses,
            int page,
            int pageSize);

    List<ModerationReport> findReportsByReporter(Long reporterUserId);

    List<ModerationReport> findReportsByReporter(
            Long reporterUserId, List<ReportTargetType> targetTypes, List<ReportStatus> statuses);

    PaginatedResult<ModerationReport> findReportsByReporter(
            Long reporterUserId,
            List<ReportTargetType> targetTypes,
            List<ReportStatus> statuses,
            int page,
            int pageSize);

    Optional<ModerationReport> findReportById(Long reportId);

    ModerationReport markReportUnderReview(Long reportId, Long adminUserId);

    ModerationReport resolveReport(
            Long reportId,
            Long adminUserId,
            ReportResolution resolution,
            String resolutionDetails,
            ReportStatus nextStatus);

    ModerationReport appealReport(Long reportId, String appealReason);

    ModerationReport finalizeReportAppeal(
            Long reportId, Long adminUserId, AppealDecision appealDecision);

    boolean softDeleteReview(
            Long reviewerUserId, Long reviewedUserId, String reason, Long deletedByUserId);

    boolean restoreReview(Long reviewerUserId, Long reviewedUserId);

    boolean softDeleteMatch(Long matchId, Long deletedByUserId, String deleteReason);

    boolean restoreMatch(Long matchId);

    String resolveTargetName(ReportTargetType targetType, Long targetId);
}
