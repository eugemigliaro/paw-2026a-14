package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.types.AppealDecision;
import ar.edu.itba.paw.models.types.ReportReason;
import ar.edu.itba.paw.models.types.ReportResolution;
import ar.edu.itba.paw.models.types.ReportStatus;
import ar.edu.itba.paw.models.types.ReportTargetType;
import java.util.List;
import java.util.Optional;

public interface ModerationService {

    Optional<UserBan> findActiveBan(User user);

    Optional<UserBan> findLatestBanForUser(User user);

    ModerationReport reportContent(
            User reporter,
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

    List<ModerationReport> findReportsByReporter(User reporter);

    List<ModerationReport> findReportsByReporter(
            User reporter, List<ReportTargetType> targetTypes, List<ReportStatus> statuses);

    PaginatedResult<ModerationReport> findReportsByReporter(
            User reporter,
            List<ReportTargetType> targetTypes,
            List<ReportStatus> statuses,
            int page,
            int pageSize);

    Optional<ModerationReport> findReportById(Long reportId);

    ModerationReport markReportUnderReview(Long reportId, User adminUser);

    ModerationReport resolveReport(
            Long reportId,
            User adminUser,
            ReportResolution resolution,
            String resolutionDetails,
            ReportStatus nextStatus);

    ModerationReport appealReport(Long reportId, String appealReason);

    ModerationReport finalizeReportAppeal(
            Long reportId, User adminUser, AppealDecision appealDecision);

    boolean softDeleteReview(User reviewer, User reviewed, String reason, User deletedBy);

    boolean restoreReview(User reviewer, User reviewed);

    boolean softDeleteMatch(Long matchId, User deletedBy, String deleteReason);

    boolean restoreMatch(Long matchId);

    String resolveTargetName(ReportTargetType targetType, Long targetId);
}
