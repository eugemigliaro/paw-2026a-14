package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BanAppealDecision;
import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.ReportReason;
import ar.edu.itba.paw.models.ReportResolution;
import ar.edu.itba.paw.models.ReportStatus;
import ar.edu.itba.paw.models.ReportTargetType;
import ar.edu.itba.paw.models.ReviewDeleteReason;
import ar.edu.itba.paw.models.UserBan;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ModerationService {

    UserBan banUser(Long userId, Long bannedByUserId, Instant bannedUntil, String reason);

    Optional<UserBan> findActiveBan(Long userId);

    Optional<UserBan> findLatestBanForUser(Long userId);

    UserBan appealBan(Long banId, Long userId, String appealReason);

    UserBan resolveBanAppeal(Long banId, Long adminUserId, BanAppealDecision decision);

    ModerationReport reportContent(
            Long reporterUserId,
            ReportTargetType targetType,
            Long targetId,
            ReportReason reason,
            String details);

    List<ModerationReport> findReports();

    List<ModerationReport> findReports(
            List<ReportTargetType> targetTypes, List<ReportStatus> statuses);

    List<ModerationReport> findReportsByReporter(Long reporterUserId);

    List<ModerationReport> findReportsByReporter(
            Long reporterUserId, List<ReportTargetType> targetTypes, List<ReportStatus> statuses);

    Optional<ModerationReport> findReportById(Long reportId);

    ModerationReport markReportUnderReview(Long reportId, Long adminUserId);

    ModerationReport resolveReport(
            Long reportId,
            Long adminUserId,
            ReportResolution resolution,
            String resolutionDetails,
            ReportStatus nextStatus);

    ModerationReport resolveUserBanReport(
            Long reportId,
            Long adminUserId,
            String banReason,
            int banDurationDays,
            ReportStatus nextStatus);

    ModerationReport appealReport(Long reportId, Long reporterUserId, String appealReason);

    ModerationReport finalizeReportAppeal(
            Long reportId, Long adminUserId, ReportResolution appealResolution);

    boolean softDeleteReview(
            Long reviewerUserId,
            Long reviewedUserId,
            ReviewDeleteReason reason,
            Long deletedByUserId);

    boolean restoreReview(Long reviewerUserId, Long reviewedUserId);

    boolean softDeleteMatch(Long matchId, Long deletedByUserId, String deleteReason);

    String resolveTargetName(ReportTargetType targetType, Long targetId);
}
