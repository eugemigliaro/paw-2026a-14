package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.AppealDecision;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.ReportReason;
import ar.edu.itba.paw.models.ReportResolution;
import ar.edu.itba.paw.models.ReportStatus;
import ar.edu.itba.paw.models.ReportTargetType;
import ar.edu.itba.paw.models.User;
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
import ar.edu.itba.paw.services.mail.UnbanMailTemplateData;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
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
    private final MessageSource messageSource;
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
            final MessageSource messageSource,
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
        this.messageSource = messageSource;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserBan> findActiveBan(final Long userId) {
        return userBanDao.findActiveBanForUser(userId, Instant.now(clock));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserBan> findLatestBanForUser(final Long userId) {
        return userBanDao.findLatestBanForUser(userId);
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

        if (ReportTargetType.USER.equals(targetType) && targetId.equals(reporterUserId)) {
            throw new ModerationException("self_report", "Cannot report yourself.");
        }

        if (moderationReportDao.countActiveReportsByReporter(reporterUserId)
                >= MAX_ACTIVE_REPORTS) {
            throw new ModerationException("report_limit", "Report limit reached.");
        }

        final boolean duplicateReport =
                moderationReportDao.findReportsByReporter(reporterUserId).stream()
                        .anyMatch(
                                report ->
                                        report.getTargetType() == targetType
                                                && report.getTargetId().equals(targetId)
                                                && isActiveReport(report.getStatus()));
        if (duplicateReport) {
            throw new ModerationException("duplicate_report", "Report already exists.");
        }

        try {
            return moderationReportDao.createReport(
                    reporterUserId, targetType, targetId, reason, normalizeText(details, 4000));
        } catch (DuplicateKeyException e) {
            throw new ModerationException("duplicate_report", "Report already exists.");
        } catch (DataIntegrityViolationException e) {
            throw new ModerationException("invalid_report", "Invalid report data.");
        } catch (DataAccessException e) {
            throw new ModerationException("report_failed", "Failed to create report.");
        } catch (ModerationException e) {
            throw e;
        } catch (Exception e) {
            throw new ModerationException("report_error", "An unexpected error occurred.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModerationReport> findReports() {
        return moderationReportDao.findReports();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModerationReport> findReports(
            List<ReportTargetType> targetTypes, List<ReportStatus> statuses) {
        return moderationReportDao.findReports(targetTypes, statuses);
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
        if (reporterUserId == null) {
            throw new ModerationException("invalid_report", "Reporter user is required.");
        }
        return moderationReportDao.findReportsByReporter(reporterUserId, targetTypes, statuses);
    }

    @Override
    @Transactional(readOnly = true)
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
                report, adminUserId, resolution, normalizeText(resolutionDetails, 4000), reportId);
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
    public ModerationReport appealReport(final Long reportId, final String appealReason) {
        final ModerationReport report =
                moderationReportDao
                        .findById(reportId)
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
            final Long reportId, final Long adminUserId, final AppealDecision appealDecision) {
        final ModerationReport report =
                moderationReportDao
                        .findById(reportId)
                        .orElseThrow(
                                () ->
                                        new ModerationException(
                                                "report_not_found", "Report not found."));

        if (appealDecision == AppealDecision.LIFTED) {
            upliftReportDecision(report);
        }

        if (!moderationReportDao.finalizeAppeal(
                reportId, adminUserId, appealDecision, Instant.now(clock))) {
            throw new ModerationException(
                    "appeal_finalization_failed", "Appeal could not be finalized.");
        }
        return moderationReportDao.findById(reportId).orElseThrow();
    }

    @Override
    @Transactional
    public boolean softDeleteReview(
            final Long reviewerUserId,
            final Long reviewedUserId,
            final String reason,
            final Long deletedByUserId) {
        return playerReviewDao.softDeleteReview(
                reviewerUserId, reviewedUserId, deletedByUserId, normalizeText(reason, 4000));
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

    private void sendUnbanEmail(final Long userId, final Locale locale) {
        final UserAccount account =
                userDao.findAccountById(userId)
                        .orElseThrow(
                                () -> new ModerationException("user_not_found", "User not found."));
        final MailContent content =
                templateRenderer.renderUnbanNotification(
                        new UnbanMailTemplateData(
                                account.getEmail(),
                                account.getUsername(),
                                stripTrailingSlash(mailProperties.getBaseUrl()) + "/login",
                                locale));
        mailDispatchService.dispatch(account.getEmail(), content);
    }

    private static boolean isActiveReport(final ReportStatus status) {
        return status == ReportStatus.PENDING
                || status == ReportStatus.UNDER_REVIEW
                || status == ReportStatus.APPEALED;
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveTargetName(final ReportTargetType targetType, final Long targetId) {
        final Locale locale = currentLocale();
        final String name =
                switch (targetType) {
                    case USER ->
                            userDao.findById(targetId)
                                    .map(User::getUsername)
                                    .orElseGet(
                                            () ->
                                                    messageSource.getMessage(
                                                            "moderation.target.user.fallback",
                                                            new Object[] {targetId},
                                                            locale));
                    case MATCH ->
                            matchDao.findById(targetId)
                                    .map(Match::getTitle)
                                    .orElseGet(
                                            () ->
                                                    messageSource.getMessage(
                                                            "moderation.target.match.fallback",
                                                            new Object[] {targetId},
                                                            locale));
                    case REVIEW ->
                            playerReviewDao
                                    .findByIdIncludingDeleted(targetId)
                                    .flatMap(review -> userDao.findById(review.getReviewerUserId()))
                                    .map(User::getUsername)
                                    .map(
                                            username ->
                                                    messageSource.getMessage(
                                                            "moderation.target.review.label",
                                                            new Object[] {username},
                                                            locale))
                                    .orElseGet(
                                            () ->
                                                    messageSource.getMessage(
                                                            "moderation.target.review.fallback",
                                                            new Object[] {targetId},
                                                            locale));
                };

        return name;
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
                            ? messageSource.getMessage(
                                    "moderation.action.defaultReason",
                                    new Object[] {report.getId()},
                                    currentLocale())
                            : resolutionDetails;
            banUser(sourceReportId, bannedUntil, report.getTargetId(), reason);
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
            final Optional<PlayerReview> review =
                    playerReviewDao.findByIdIncludingDeleted(report.getTargetId());
            review.ifPresent(
                    found ->
                            softDeleteReview(
                                    found.getReviewerUserId(),
                                    found.getReviewedUserId(),
                                    resolutionDetails,
                                    adminUserId));
            return;
        }
        if (sourceReportId != null) {
            throw new ModerationException("invalid_report", "Invalid content report target.");
        }
    }

    @Transactional
    private void upliftReportDecision(final ModerationReport report) {
        if (report.getTargetType() == ReportTargetType.REVIEW) {
            final Optional<PlayerReview> review =
                    playerReviewDao.findByIdIncludingDeleted(report.getTargetId());
            review.ifPresent(
                    found -> restoreReview(found.getReviewerUserId(), found.getReviewedUserId()));
            return;
        }
        if (report.getTargetType() == ReportTargetType.USER) {
            final Optional<UserBan> ban =
                    userBanDao.findActiveBanForUser(report.getTargetId(), Instant.now(clock));
            ban.ifPresent(found -> userBanDao.upliftBan(found.getId()));
            sendUnbanEmail(report.getTargetId(), currentLocale());
            return;
        }
    }

    @Transactional
    private UserBan banUser(
            final Long moderationReportId,
            final Instant bannedUntil,
            final Long userId,
            final String banReason) {
        final UserBan ban = userBanDao.createBan(moderationReportId, bannedUntil);
        cancelFutureContentForUser(userId);
        sendBanEmail(userId, bannedUntil, banReason);
        return ban;
    }
}
