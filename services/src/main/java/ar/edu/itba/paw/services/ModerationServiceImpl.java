package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BanAppealDecision;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.ReportReason;
import ar.edu.itba.paw.models.ReportResolution;
import ar.edu.itba.paw.models.ReportStatus;
import ar.edu.itba.paw.models.ReportTargetType;
import ar.edu.itba.paw.models.ReviewDeleteReason;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.persistence.ModerationReportDao;
import ar.edu.itba.paw.persistence.PlayerReviewDao;
import ar.edu.itba.paw.persistence.UserBanDao;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.exceptions.ModerationException;
import ar.edu.itba.paw.services.mail.BanMailTemplateData;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.MailProperties;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModerationServiceImpl implements ModerationService {

    private static final int MAX_ACTIVE_REPORTS = 3;
    private static final int DEFAULT_REPORT_PAGE_SIZE = 24;
    private static final int DEFAULT_BAN_DURATION_DAYS = 7;

    private final UserBanDao userBanDao;
    private final ModerationReportDao moderationReportDao;
    private final UserDao userDao;
    private final MatchDao matchDao;
    private final MatchParticipantDao matchParticipantDao;
    private final PlayerReviewDao playerReviewDao;
    private final MailDispatchService mailDispatchService;
    private final MailProperties mailProperties;
    private final ThymeleafMailTemplateRenderer templateRenderer;
    private final MatchService matchService;
    private final Clock clock;

    @Autowired
    public ModerationServiceImpl(
            final UserBanDao userBanDao,
            final ModerationReportDao moderationReportDao,
            final UserDao userDao,
            final MatchDao matchDao,
            final MatchParticipantDao matchParticipantDao,
            final PlayerReviewDao playerReviewDao,
            final MailDispatchService mailDispatchService,
            final MailProperties mailProperties,
            final ThymeleafMailTemplateRenderer templateRenderer,
            final MatchService matchService,
            final Clock clock) {
        this.userBanDao = userBanDao;
        this.moderationReportDao = moderationReportDao;
        this.userDao = userDao;
        this.matchDao = matchDao;
        this.matchParticipantDao = matchParticipantDao;
        this.playerReviewDao = playerReviewDao;
        this.mailDispatchService = mailDispatchService;
        this.mailProperties = mailProperties;
        this.templateRenderer = templateRenderer;
        this.matchService = matchService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UserBan banUser(
            final Long userId,
            final Long bannedByUserId,
            final Instant bannedUntil,
            final String reason) {
        validateBanRequest(userId, bannedByUserId, bannedUntil, reason);
        final UserBan ban = userBanDao.createBan(userId, bannedByUserId, bannedUntil, reason);
        cancelFutureContentForUser(userId);
        sendBanEmail(userId, bannedUntil, reason);
        return ban;
    }

    @Override
    public Optional<UserBan> findActiveBan(final Long userId) {
        return userBanDao.findActiveBanForUser(userId, Instant.now(clock));
    }

    @Override
    @Transactional
    public UserBan appealBan(final Long banId, final Long userId, final String appealReason) {
        final UserBan ban =
                userBanDao
                        .findLatestBanForUser(userId)
                        .filter(candidate -> candidate.getId().equals(banId))
                        .orElseThrow(
                                () -> new ModerationException("ban_not_found", "Ban not found."));
        if (ban.getAppealCount() >= 1) {
            throw new ModerationException("appeal_limit", "Ban appeal limit reached.");
        }
        if (!userBanDao.appealBan(banId, normalizeText(appealReason, 2000), Instant.now(clock))) {
            throw new ModerationException("appeal_rejected", "Ban appeal could not be stored.");
        }
        return userBanDao.findLatestBanForUser(userId).orElse(ban);
    }

    @Override
    @Transactional
    public UserBan resolveBanAppeal(
            final Long banId, final Long adminUserId, final BanAppealDecision decision) {
        if (!userBanDao.resolveAppeal(banId, adminUserId, decision, Instant.now(clock))) {
            throw new ModerationException("appeal_rejected", "Ban appeal could not be resolved.");
        }
        return userBanDao.findById(banId).orElseThrow();
    }

    @Override
    @Transactional
    public ModerationReport reportContent(
            final Long reporterUserId,
            final ReportTargetType targetType,
            final Long targetId,
            final ReportReason reason,
            final String details) {
        validateReportRequest(reporterUserId, targetType, targetId, reason);

        if (moderationReportDao.countActiveReportsByReporter(reporterUserId)
                >= MAX_ACTIVE_REPORTS) {
            throw new ModerationException("report_limit", "Report limit reached.");
        }

        final String targetKey = reportKey(targetType, targetId);
        final boolean duplicateReport =
                moderationReportDao.findReportsByReporter(reporterUserId).stream()
                        .anyMatch(
                                report ->
                                        report.getTargetKey().equals(targetKey)
                                                && isActiveReport(report.getStatus()));
        if (duplicateReport) {
            throw new ModerationException("duplicate_report", "Report already exists.");
        }

        return moderationReportDao.createReport(
                reporterUserId,
                targetType,
                targetId,
                targetKey,
                reason,
                normalizeText(details, 4000));
    }

    @Override
    public List<ModerationReport> findActiveReports() {
        return moderationReportDao.findActiveReports();
    }

    @Override
    public List<ModerationReport> findReportsByReporter(final Long reporterUserId) {
        if (reporterUserId == null) {
            throw new ModerationException("invalid_report", "Reporter user is required.");
        }
        return moderationReportDao.findReportsByReporter(reporterUserId);
    }

    @Override
    public Optional<ModerationReport> findReportById(final Long reportId) {
        return moderationReportDao.findById(reportId);
    }

    @Override
    @Transactional
    public ModerationReport markReportUnderReview(final Long reportId, final Long adminUserId) {
        if (!moderationReportDao.markUnderReview(reportId, adminUserId, Instant.now(clock))) {
            throw new ModerationException("report_not_found", "Report not found.");
        }
        return moderationReportDao.findById(reportId).orElseThrow();
    }

    @Override
    @Transactional
    public ModerationReport resolveReport(
            final Long reportId,
            final Long adminUserId,
            final ReportResolution resolution,
            final String resolutionDetails,
            final ReportStatus nextStatus) {
        final ModerationReport report =
                moderationReportDao
                        .findById(reportId)
                        .orElseThrow(
                                () ->
                                        new ModerationException(
                                                "report_not_found", "Report not found."));
        applyResolutionEffect(
                report, adminUserId, resolution, normalizeText(resolutionDetails, 4000), null);
        if (!moderationReportDao.resolveReport(
                reportId,
                adminUserId,
                resolution,
                normalizeText(resolutionDetails, 4000),
                Instant.now(clock),
                nextStatus)) {
            throw new ModerationException("report_not_found", "Report not found.");
        }
        return moderationReportDao.findById(reportId).orElseThrow();
    }

    @Override
    @Transactional
    public ModerationReport resolveUserBanReport(
            final Long reportId,
            final Long adminUserId,
            final String banReason,
            final int banDurationDays,
            final ReportStatus nextStatus) {
        final ModerationReport report =
                moderationReportDao
                        .findById(reportId)
                        .orElseThrow(
                                () ->
                                        new ModerationException(
                                                "report_not_found", "Report not found."));
        if (report.getTargetType() != ReportTargetType.USER) {
            throw new ModerationException("invalid_report", "Report target is not a user.");
        }
        final int safeBanDays =
                banDurationDays <= 0 ? DEFAULT_BAN_DURATION_DAYS : Math.min(banDurationDays, 365);
        final Instant bannedUntil = Instant.now(clock).plusSeconds(safeBanDays * 24L * 3600L);
        final String normalizedBanReason = normalizeText(banReason, 2000);
        final String effectiveReason =
                normalizedBanReason == null
                        ? "Report #" + reportId + " moderation action"
                        : normalizedBanReason;
        banUser(report.getTargetId(), adminUserId, bannedUntil, effectiveReason);
        if (!moderationReportDao.resolveReport(
                reportId,
                adminUserId,
                ReportResolution.USER_BANNED,
                effectiveReason,
                Instant.now(clock),
                nextStatus)) {
            throw new ModerationException("report_not_found", "Report not found.");
        }
        return moderationReportDao.findById(reportId).orElseThrow();
    }

    @Override
    @Transactional
    public ModerationReport appealReport(
            final Long reportId, final Long reporterUserId, final String appealReason) {
        final ModerationReport report =
                moderationReportDao
                        .findById(reportId)
                        .filter(found -> found.getReporterUserId().equals(reporterUserId))
                        .orElseThrow(
                                () ->
                                        new ModerationException(
                                                "report_not_found", "Report not found."));
        if (report.getAppealCount() >= 1) {
            throw new ModerationException("appeal_limit", "Report appeal limit reached.");
        }
        if (!moderationReportDao.appealReport(
                reportId, normalizeText(appealReason, 4000), Instant.now(clock))) {
            throw new ModerationException("appeal_rejected", "Report appeal could not be stored.");
        }
        return moderationReportDao.findById(reportId).orElseThrow();
    }

    @Override
    @Transactional
    public ModerationReport finalizeReportAppeal(
            final Long reportId, final Long adminUserId, final ReportResolution appealResolution) {
        final ModerationReport report =
                moderationReportDao
                        .findById(reportId)
                        .orElseThrow(
                                () ->
                                        new ModerationException(
                                                "report_not_found", "Report not found."));
        applyResolutionEffect(report, adminUserId, appealResolution, null, reportId);
        if (!moderationReportDao.finalizeAppeal(
                reportId, adminUserId, appealResolution, Instant.now(clock))) {
            throw new ModerationException("report_not_found", "Report not found.");
        }
        return moderationReportDao.findById(reportId).orElseThrow();
    }

    @Override
    @Transactional
    public boolean softDeleteReview(
            final Long reviewerUserId,
            final Long reviewedUserId,
            final ReviewDeleteReason reason,
            final Long deletedByUserId) {
        return playerReviewDao.softDeleteReview(
                reviewerUserId, reviewedUserId, reason, deletedByUserId);
    }

    @Override
    @Transactional
    public boolean restoreReview(final Long reviewerUserId, final Long reviewedUserId) {
        return playerReviewDao.restoreReview(reviewerUserId, reviewedUserId);
    }

    @Override
    @Transactional
    public boolean softDeleteMatch(
            final Long matchId, final Long deletedByUserId, final String deleteReason) {
        return matchDao.softDeleteMatch(matchId, deletedByUserId, deleteReason);
    }

    private void validateBanRequest(
            final Long userId,
            final Long bannedByUserId,
            final Instant bannedUntil,
            final String reason) {
        if (userId == null || bannedByUserId == null) {
            throw new ModerationException("invalid_ban", "User and moderator are required.");
        }
        if (bannedUntil == null || !bannedUntil.isAfter(Instant.now(clock))) {
            throw new ModerationException("invalid_ban", "Ban must expire in the future.");
        }
        normalizeText(reason, 2000);
    }

    private void validateReportRequest(
            final Long reporterUserId,
            final ReportTargetType targetType,
            final Long targetId,
            final ReportReason reason) {
        if (reporterUserId == null || targetType == null || targetId == null || reason == null) {
            throw new ModerationException("invalid_report", "Report is incomplete.");
        }
    }

    private void cancelFutureContentForUser(final Long userId) {
        final List<Long> participantMatchIds =
                matchService
                        .findJoinedMatches(
                                userId,
                                true,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                0,
                                DEFAULT_REPORT_PAGE_SIZE)
                        .getItems()
                        .stream()
                        .map(Match::getId)
                        .toList();
        for (final Long matchId : participantMatchIds) {
            matchParticipantDao.removeParticipant(matchId, userId);
        }

        final List<Long> hostedMatchIds =
                matchService
                        .findHostedMatches(
                                userId,
                                true,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                0,
                                DEFAULT_REPORT_PAGE_SIZE)
                        .getItems()
                        .stream()
                        .map(Match::getId)
                        .toList();
        for (final Long matchId : hostedMatchIds) {
            matchService.cancelMatch(matchId, userId);
        }
    }

    private void sendBanEmail(final Long userId, final Instant bannedUntil, final String reason) {
        final Locale locale = currentLocale();
        final UserAccount account =
                userDao.findAccountById(userId)
                        .orElseThrow(
                                () -> new ModerationException("user_not_found", "User not found."));
        final MailContent content =
                templateRenderer.renderBanNotification(
                        new BanMailTemplateData(
                                account.getEmail(),
                                account.getUsername(),
                                bannedUntil,
                                reason,
                                stripTrailingSlash(mailProperties.getBaseUrl()) + "/login",
                                locale));
        mailDispatchService.dispatch(account.getEmail(), content);
    }

    private static boolean isActiveReport(final ReportStatus status) {
        return status == ReportStatus.PENDING
                || status == ReportStatus.UNDER_REVIEW
                || status == ReportStatus.APPEALED;
    }

    private static String reportKey(final ReportTargetType targetType, final Long targetId) {
        return targetType.getDbValue() + ":" + targetId;
    }

    private static String normalizeText(final String value, final int maxLength) {
        if (value == null) {
            return null;
        }
        final String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new ModerationException("value_too_long", "Value is too long.");
        }
        return normalized;
    }

    private static Locale currentLocale() {
        final Locale locale = LocaleContextHolder.getLocale();
        return locale == null ? Locale.ENGLISH : locale;
    }

    private static String stripTrailingSlash(final String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private void applyResolutionEffect(
            final ModerationReport report,
            final Long adminUserId,
            final ReportResolution resolution,
            final String resolutionDetails,
            final Long sourceReportId) {
        if (resolution == ReportResolution.CONTENT_DELETED) {
            applyContentDelete(report, adminUserId, resolutionDetails, sourceReportId);
            return;
        }
        if (resolution == ReportResolution.USER_BANNED
                && report.getTargetType() == ReportTargetType.USER) {
            final Instant bannedUntil =
                    Instant.now(clock).plusSeconds(DEFAULT_BAN_DURATION_DAYS * 24L * 3600L);
            final String reason =
                    resolutionDetails == null
                            ? "Report #" + report.getId() + " moderation action"
                            : resolutionDetails;
            banUser(report.getTargetId(), adminUserId, bannedUntil, reason);
        }
    }

    private void applyContentDelete(
            final ModerationReport report,
            final Long adminUserId,
            final String resolutionDetails,
            final Long sourceReportId) {
        if (report.getTargetType() == ReportTargetType.MATCH) {
            softDeleteMatch(report.getTargetId(), adminUserId, resolutionDetails);
            return;
        }
        if (report.getTargetType() == ReportTargetType.REVIEW) {
            final Optional<ar.edu.itba.paw.models.PlayerReview> review =
                    playerReviewDao.findByIdIncludingDeleted(report.getTargetId());
            review.ifPresent(
                    found ->
                            softDeleteReview(
                                    found.getReviewerUserId(),
                                    found.getReviewedUserId(),
                                    ReviewDeleteReason.INAPPROPRIATE_CONTENT,
                                    adminUserId));
            return;
        }
        if (sourceReportId != null) {
            throw new ModerationException("invalid_report", "Invalid content report target.");
        }
    }
}
