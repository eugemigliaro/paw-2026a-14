package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.AppealDecision;
import ar.edu.itba.paw.models.types.ReportReason;
import ar.edu.itba.paw.models.types.ReportResolution;
import ar.edu.itba.paw.models.types.ReportStatus;
import ar.edu.itba.paw.models.types.ReportTargetType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationServiceImpl.class);

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
    public Optional<UserBan> findActiveBan(final User user) {
        return userBanDao.findActiveBanForUser(user, Instant.now(clock));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserBan> findLatestBanForUser(final User user) {
        return userBanDao.findLatestBanForUser(user);
    }

    @Override
    @Transactional
    public ModerationReport reportContent(
            final User reporter,
            final ReportTargetType targetType,
            final Long targetId,
            final ReportReason reason,
            final String details) {
        validateReportRequest(reporter, targetType, targetId, reason);

        if (ReportTargetType.USER.equals(targetType) && targetId.equals(reporter.getId())) {
            LOGGER.warn("Self report attempt by userId={}", reporter.getId());
            throw new ModerationException("self_report", "Cannot report yourself.");
        }

        if (moderationReportDao.countActiveReportsByReporter(reporter) >= MAX_ACTIVE_REPORTS) {
            LOGGER.warn("Report limit reached for userId={}", reporter.getId());
            throw new ModerationException("report_limit", "Report limit reached.");
        }

        final boolean duplicateReport =
                moderationReportDao.findReportsByReporter(reporter).stream()
                        .anyMatch(
                                report ->
                                        report.getTargetType() == targetType
                                                && report.getTargetId().equals(targetId)
                                                && isActiveReport(report.getStatus()));
        if (duplicateReport) {
            LOGGER.warn(
                    "Duplicate report attempt by userId={} targetType={} targetId={}",
                    reporter.getId(),
                    targetType,
                    targetId);
            throw new ModerationException("duplicate_report", "Report already exists.");
        }

        try {
            final ModerationReport report =
                    moderationReportDao.createReport(
                            reporter, targetType, targetId, reason, normalizeText(details, 4000));
            LOGGER.info(
                    "Report created id={} reporterId={} targetType={} targetId={}",
                    report.getId(),
                    reporter.getId(),
                    targetType,
                    targetId);
            return report;
        } catch (DuplicateKeyException e) {
            LOGGER.warn(
                    "Duplicate report (DB) by userId={} targetType={} targetId={}",
                    reporter.getId(),
                    targetType,
                    targetId);
            throw new ModerationException("duplicate_report", "Report already exists.");
        } catch (DataIntegrityViolationException e) {
            LOGGER.error(
                    "Data integrity violation creating report by userId={} targetType={} targetId={}",
                    reporter.getId(),
                    targetType,
                    targetId,
                    e);
            throw new ModerationException("invalid_report", "Invalid report data.");
        } catch (DataAccessException e) {
            LOGGER.error(
                    "DB error creating report by userId={} targetType={} targetId={}",
                    reporter.getId(),
                    targetType,
                    targetId,
                    e);
            throw new ModerationException("report_failed", "Failed to create report.");
        } catch (ModerationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error(
                    "Unexpected error creating report by userId={} targetType={} targetId={}",
                    reporter.getId(),
                    targetType,
                    targetId,
                    e);
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
    public PaginatedResult<ModerationReport> findReports(
            final List<ReportTargetType> targetTypes,
            final List<ReportStatus> statuses,
            final int page,
            final int pageSize) {
        return moderationReportDao.findReports(
                targetTypes, statuses, normalizePage(page), normalizePageSize(pageSize));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModerationReport> findReportsByReporter(final User reporter) {
        return findReportsByReporter(reporter, List.of(), List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModerationReport> findReportsByReporter(
            final User reporter,
            final List<ReportTargetType> targetTypes,
            final List<ReportStatus> statuses) {
        nonNullUser(reporter);
        return moderationReportDao.findReportsByReporter(reporter, targetTypes, statuses);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<ModerationReport> findReportsByReporter(
            final User reporter,
            final List<ReportTargetType> targetTypes,
            final List<ReportStatus> statuses,
            final int page,
            final int pageSize) {
        nonNullUser(reporter);
        return moderationReportDao.findReportsByReporter(
                reporter, targetTypes, statuses, normalizePage(page), normalizePageSize(pageSize));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ModerationReport> findReportById(final Long reportId) {
        return moderationReportDao.findById(reportId);
    }

    @Override
    @Transactional
    public ModerationReport markReportUnderReview(final Long reportId, final User adminUser) {
        nonNullUser(adminUser);
        if (!moderationReportDao.markUnderReview(reportId, adminUser, Instant.now(clock))) {
            throw new ModerationException("report_not_found", "Report not found.");
        }
        return moderationReportDao.findById(reportId).orElseThrow();
    }

    @Override
    @Transactional
    public ModerationReport resolveReport(
            final Long reportId,
            final User adminUser,
            final ReportResolution resolution,
            final String resolutionDetails,
            final ReportStatus nextStatus) {
        nonNullUser(adminUser);
        final ModerationReport report =
                moderationReportDao
                        .findById(reportId)
                        .orElseThrow(
                                () ->
                                        new ModerationException(
                                                "report_not_found", "Report not found."));
        applyResolutionEffect(
                report, adminUser, resolution, normalizeText(resolutionDetails, 4000), reportId);
        if (!moderationReportDao.resolveReport(
                reportId,
                adminUser,
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
            final Long reportId, final User adminUser, final AppealDecision appealDecision) {
        nonNullUser(adminUser);
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
                reportId, adminUser, appealDecision, Instant.now(clock))) {
            throw new ModerationException(
                    "appeal_finalization_failed", "Appeal could not be finalized.");
        }
        return moderationReportDao.findById(reportId).orElseThrow();
    }

    @Override
    @Transactional
    public boolean softDeleteReview(
            final User reviewer, final User reviewed, final String reason, final User deletedBy) {
        return playerReviewDao.softDeleteReview(
                reviewer, reviewed, deletedBy, normalizeText(reason, 4000));
    }

    @Override
    @Transactional
    public boolean restoreReview(final User reviewer, final User reviewed) {
        return playerReviewDao.restoreReview(reviewer, reviewed);
    }

    @Override
    @Transactional
    public boolean softDeleteMatch(
            final Long matchId, final User deletedBy, final String deleteReason) {
        return matchDao.softDeleteMatch(matchId, deletedBy, deleteReason);
    }

    @Override
    @Transactional
    public boolean restoreMatch(final Long matchId) {
        return matchDao.restoreMatch(matchId);
    }

    private void validateReportRequest(
            final User reporter,
            final ReportTargetType targetType,
            final Long targetId,
            final ReportReason reason) {
        if (reporter == null || targetType == null || targetId == null || reason == null) {
            throw new ModerationException("invalid_report", "Report is incomplete.");
        }
    }

    private void cancelFutureContentForUser(final User user) {
        final List<Long> participantMatchIds =
                matchService
                        .findJoinedMatches(
                                user,
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
            matchParticipantDao.removeParticipant(matchId, user);
        }

        final List<Long> hostedMatchIds =
                matchService
                        .findHostedMatches(
                                user,
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
            matchService.cancelMatch(matchId, user);
        }
    }

    private void sendBanEmail(
            final Long moderationReportId,
            final User user,
            final Instant bannedUntil,
            final String reason) {
        nonNullUser(user);
        final Locale locale = UserLanguages.toLocale(user.getPreferredLanguage());
        final String localizedReason =
                reason == null || reason.isBlank()
                        ? messageSource.getMessage(
                                "moderation.action.defaultReason",
                                new Object[] {moderationReportId},
                                locale)
                        : reason;
        final MailContent content =
                templateRenderer.renderBanNotification(
                        new BanMailTemplateData(
                                user.getEmail(),
                                user.getUsername(),
                                bannedUntil,
                                localizedReason,
                                stripTrailingSlash(mailProperties.getBaseUrl()) + "/login",
                                locale));
        mailDispatchService.dispatch(user.getEmail(), content);
    }

    private void sendUnbanEmail(final User user) {
        nonNullUser(user);
        final Locale locale = UserLanguages.toLocale(user.getPreferredLanguage());
        final MailContent content =
                templateRenderer.renderUnbanNotification(
                        new UnbanMailTemplateData(
                                user.getEmail(),
                                user.getUsername(),
                                stripTrailingSlash(mailProperties.getBaseUrl()) + "/login",
                                locale));
        mailDispatchService.dispatch(user.getEmail(), content);
    }

    private static boolean isActiveReport(final ReportStatus status) {
        return status == ReportStatus.PENDING
                || status == ReportStatus.UNDER_REVIEW
                || status == ReportStatus.APPEALED;
    }

    private static int normalizePage(final int page) {
        return page > 0 ? page : 1;
    }

    private int normalizePageSize(final int pageSize) {
        return pageSize > 0 ? pageSize : DEFAULT_REPORT_PAGE_SIZE;
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
                                    .map(review -> review.getReviewer().getUsername())
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
            final User adminUser,
            final ReportResolution resolution,
            final String resolutionDetails,
            final Long sourceReportId) {
        if (resolution == ReportResolution.CONTENT_DELETED) {
            applyContentDelete(report, adminUser, resolutionDetails, sourceReportId);
            return;
        }
        if (resolution == ReportResolution.USER_BANNED
                && report.getTargetType() == ReportTargetType.USER) {
            final Instant bannedUntil =
                    Instant.now(clock).plusSeconds(DEFAULT_BAN_DURATION_DAYS * 24L * 3600L);
            final User user =
                    userDao.findById(report.getTargetId())
                            .orElseThrow(
                                    () ->
                                            new ModerationException(
                                                    "user_not_found", "User not found."));
            banUser(report, bannedUntil, user, resolutionDetails);
        }
    }

    private void nonNullUser(final User user) {
        if (user == null) {
            throw new ModerationException("invalid_report", "Reporter user is required.");
        }
    }

    private void applyContentDelete(
            final ModerationReport report,
            final User adminUser,
            final String resolutionDetails,
            final Long sourceReportId) {
        if (report.getTargetType() == ReportTargetType.MATCH) {
            softDeleteMatch(report.getTargetId(), adminUser, resolutionDetails);
            return;
        }
        if (report.getTargetType() == ReportTargetType.REVIEW) {
            final Optional<PlayerReview> review =
                    playerReviewDao.findByIdIncludingDeleted(report.getTargetId());
            review.ifPresent(
                    found ->
                            softDeleteReview(
                                    found.getReviewer(),
                                    found.getReviewed(),
                                    resolutionDetails,
                                    adminUser));
            return;
        }
        if (sourceReportId != null) {
            throw new ModerationException("invalid_report", "Invalid content report target.");
        }
    }

    private void upliftReportDecision(final ModerationReport report) {
        if (report.getTargetType() == ReportTargetType.REVIEW) {
            final Optional<PlayerReview> review =
                    playerReviewDao.findByIdIncludingDeleted(report.getTargetId());
            review.ifPresent(found -> restoreReview(found.getReviewer(), found.getReviewed()));
            return;
        }
        if (report.getTargetType() == ReportTargetType.USER) {
            final User user =
                    userDao.findById(report.getTargetId())
                            .orElseThrow(
                                    () ->
                                            new ModerationException(
                                                    "user_not_found", "User not found."));
            final Optional<UserBan> ban = userBanDao.findActiveBanForUser(user, Instant.now(clock));
            ban.ifPresent(found -> userBanDao.upliftBan(found.getId()));
            sendUnbanEmail(user);
            return;
        }
        if (report.getTargetType() == ReportTargetType.MATCH) {
            final Optional<Match> match = matchDao.findById(report.getTargetId());
            if (match.isEmpty()) {
                return;
            }
            if (!match.get().isDeleted()) {
                throw new ModerationException("invalid_report", "Invalid match report target.");
            }
            matchDao.restoreMatch(match.get().getId());
            return;
        }
    }

    private UserBan banUser(
            final ModerationReport report,
            final Instant bannedUntil,
            final User user,
            final String banReason) {
        final UserBan ban = userBanDao.createBan(report, bannedUntil);
        cancelFutureContentForUser(user);
        sendBanEmail(report.getId(), user, bannedUntil, banReason);
        return ban;
    }
}
