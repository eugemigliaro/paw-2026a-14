package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BanAppealDecision;
import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.ReportResolution;
import ar.edu.itba.paw.models.ReportStatus;
import ar.edu.itba.paw.models.ReportTargetType;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.exceptions.ModerationException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN_MOD')")
public class ModerationAdminController {

    private final ModerationService moderationService;
    private final MessageSource messageSource;

    @Autowired
    public ModerationAdminController(
            final ModerationService moderationService, final MessageSource messageSource) {
        this.moderationService = moderationService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public ModelAndView showReports(final Locale locale) {
        final List<ModerationReportViewModel> reports =
                moderationService.findActiveReports().stream()
                        .map(report -> toViewModel(report, locale))
                        .toList();

        final ModelAndView mav = new ModelAndView("admin/reports/list");
        mav.addObject(
                "shell",
                ShellViewModelFactory.playerShell(messageSource, locale, "/admin/reports"));
        mav.addObject(
                "pageTitle", messageSource.getMessage("page.title.adminReports", null, locale));
        mav.addObject(
                "pageTitleLabel", messageSource.getMessage("admin.reports.title", null, locale));
        mav.addObject(
                "pageDescription",
                messageSource.getMessage("admin.reports.description", null, locale));
        mav.addObject(
                "activeReportCountLabel",
                messageSource.getMessage(
                        "admin.reports.count", new Object[] {reports.size()}, locale));
        mav.addObject(
                "emptyMessage", messageSource.getMessage("admin.reports.empty", null, locale));
        mav.addObject("reports", reports);
        return mav;
    }

    @GetMapping("/{reportId:\\d+}")
    public ModelAndView showReportDetail(
            @PathVariable("reportId") final Long reportId, final Locale locale) {
        final ModerationReport report =
                moderationService
                        .findReportById(reportId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        final ModelAndView mav = new ModelAndView("admin/reports/detail");
        mav.addObject(
                "shell",
                ShellViewModelFactory.playerShell(messageSource, locale, "/admin/reports"));
        mav.addObject(
                "pageTitle",
                messageSource.getMessage("page.title.adminReportDetail", null, locale));
        mav.addObject(
                "pageTitleLabel",
                messageSource.getMessage("admin.reports.detail.title", null, locale));
        mav.addObject(
                "pageDescription",
                messageSource.getMessage("admin.reports.detail.description", null, locale));
        mav.addObject("report", toViewModel(report, locale));
        mav.addObject("banAppeal", banAppealViewModel(report, locale));
        return mav;
    }

    @PostMapping("/{reportId:\\d+}/under-review")
    public ModelAndView markUnderReview(
            @PathVariable("reportId") final Long reportId, final Locale locale) {
        try {
            moderationService.markReportUnderReview(reportId, currentAdminUserId());
            return redirectToReports("reviewed");
        } catch (final ModerationException ex) {
            return redirectToReportsError("report_not_found");
        }
    }

    @PostMapping("/{reportId:\\d+}/dismiss")
    public ModelAndView dismissReport(
            @PathVariable("reportId") final Long reportId, final Locale locale) {
        return resolveReport(reportId, ReportResolution.DISMISSED, "dismissed", locale);
    }

    @PostMapping("/{reportId:\\d+}/warn")
    public ModelAndView warnUser(
            @PathVariable("reportId") final Long reportId, final Locale locale) {
        return resolveReport(reportId, ReportResolution.WARNING, "warned", locale);
    }

    @PostMapping("/{reportId:\\d+}/delete-content")
    public ModelAndView deleteContent(
            @PathVariable("reportId") final Long reportId, final Locale locale) {
        return resolveReport(reportId, ReportResolution.CONTENT_DELETED, "deleted", locale);
    }

    @PostMapping("/{reportId:\\d+}/ban-user")
    public ModelAndView banUser(
            @PathVariable("reportId") final Long reportId,
            @RequestParam(value = "banDays", required = false, defaultValue = "7")
                    final int banDays,
            @RequestParam(value = "banReason", required = false) final String banReason,
            final Locale locale) {
        try {
            moderationService.resolveUserBanReport(
                    reportId, currentAdminUserId(), banReason, banDays, ReportStatus.RESOLVED);
            return redirectToReports("banned");
        } catch (final ModerationException ex) {
            return redirectToReportsError(ex.getCode());
        }
    }

    @PostMapping("/{reportId:\\d+}/finalize-appeal")
    public ModelAndView finalizeAppeal(
            @PathVariable("reportId") final Long reportId,
            @RequestParam("appealResolution") final String appealResolution,
            final Locale locale) {
        final ReportResolution parsedResolution =
                ReportResolution.fromDbValue(appealResolution)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        try {
            moderationService.finalizeReportAppeal(
                    reportId, currentAdminUserId(), parsedResolution);
            return redirectToReports("appeal_finalized");
        } catch (final ModerationException ex) {
            return redirectToReportsError(ex.getCode());
        }
    }

    @PostMapping("/{reportId:\\d+}/resolve-ban-appeal")
    public ModelAndView resolveBanAppeal(
            @PathVariable("reportId") final Long reportId,
            @RequestParam("decision") final String decisionCode,
            final Locale locale) {
        final BanAppealDecision decision =
                BanAppealDecision.fromDbValue(decisionCode)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        final ModerationReport report =
                moderationService
                        .findReportById(reportId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final UserBan ban =
                Optional.ofNullable(moderationService.findLatestBanForUser(report.getTargetId()))
                        .orElse(Optional.empty())
                        .filter(candidate -> candidate.getAppealCount() > 0)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        try {
            moderationService.resolveBanAppeal(ban.getId(), currentAdminUserId(), decision);
            return redirectToReports("ban_appeal_resolved");
        } catch (final ModerationException ex) {
            return redirectToReportsError(ex.getCode());
        }
    }

    private ModelAndView resolveReport(
            final Long reportId,
            final ReportResolution resolution,
            final String actionCode,
            final Locale locale) {
        try {
            final ModerationReport report =
                    moderationService.resolveReport(
                            reportId,
                            currentAdminUserId(),
                            resolution,
                            null,
                            ReportStatus.RESOLVED);
            if (report == null) {
                return redirectToReportsError("report_not_found");
            }
            return redirectToReports(actionCode);
        } catch (final ModerationException ex) {
            return redirectToReportsError(ex.getCode());
        }
    }

    private ModelAndView redirectToReports(final String actionCode) {
        return new ModelAndView("redirect:/admin/reports?action=" + actionCode);
    }

    private ModelAndView redirectToReportsError(final String errorCode) {
        return new ModelAndView("redirect:/admin/reports?error=" + errorCode);
    }

    private long currentAdminUserId() {
        return CurrentAuthenticatedUser.get()
                .map(AuthenticatedUserPrincipal::getUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private ModerationReportViewModel toViewModel(
            final ModerationReport report, final Locale locale) {
        return new ModerationReportViewModel(
                report.getId(),
                report.getReporterUserId(),
                report.getTargetType() == null ? "" : report.getTargetType().getDbValue(),
                moderationService.resolveTargetName(report.getTargetType(), report.getTargetId()),
                report.getReason() == null ? "" : report.getReason().getDbValue(),
                report.getStatus() == null ? "" : report.getStatus().getDbValue(),
                report.getDetails(),
                report.getAppealReason(),
                report.getAppealCount(),
                formatInstant(report.getCreatedAt(), locale),
                formatInstant(report.getUpdatedAt(), locale),
                formatInstant(report.getAppealedAt(), locale),
                isAppealed(report));
    }

    private boolean isAppealed(final ModerationReport report) {
        return report.getStatus() == ReportStatus.APPEALED;
    }

    private String formatInstant(final Instant instant, final Locale locale) {
        return instant == null
                ? ""
                : DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(locale)
                        .withZone(ZoneId.systemDefault())
                        .format(instant);
    }

    private BanAppealViewModel banAppealViewModel(
            final ModerationReport report, final Locale locale) {
        if (report.getTargetType() != ReportTargetType.USER) {
            return null;
        }
        return Optional.ofNullable(moderationService.findLatestBanForUser(report.getTargetId()))
                .orElse(Optional.empty())
                .filter(ban -> ban.getAppealCount() > 0)
                .map(
                        ban ->
                                new BanAppealViewModel(
                                        ban.getId(),
                                        ban.getAppealReason(),
                                        ban.getAppealDecision() == null
                                                ? ""
                                                : ban.getAppealDecision().getDbValue(),
                                        formatInstant(ban.getAppealedAt(), locale),
                                        ban.getAppealResolvedAt() == null))
                .orElse(null);
    }

    public static final class BanAppealViewModel {
        private final Long banId;
        private final String appealReason;
        private final String appealDecisionCode;
        private final String appealedAtLabel;
        private final boolean pendingResolution;

        private BanAppealViewModel(
                final Long banId,
                final String appealReason,
                final String appealDecisionCode,
                final String appealedAtLabel,
                final boolean pendingResolution) {
            this.banId = banId;
            this.appealReason = appealReason;
            this.appealDecisionCode = appealDecisionCode;
            this.appealedAtLabel = appealedAtLabel;
            this.pendingResolution = pendingResolution;
        }

        public Long getBanId() {
            return banId;
        }

        public String getAppealReason() {
            return appealReason;
        }

        public String getAppealDecisionCode() {
            return appealDecisionCode;
        }

        public String getAppealedAtLabel() {
            return appealedAtLabel;
        }

        public boolean isPendingResolution() {
            return pendingResolution;
        }
    }

    public static final class ModerationReportViewModel {
        private final Long id;
        private final Long reporterUserId;
        private final String targetTypeCode;
        private final String targetKey;
        private final String reasonCode;
        private final String statusCode;
        private final String details;
        private final String appealReason;
        private final int appealCount;
        private final String createdAtLabel;
        private final String updatedAtLabel;
        private final String appealedAtLabel;
        private final boolean appealed;

        private ModerationReportViewModel(
                final Long id,
                final Long reporterUserId,
                final String targetTypeCode,
                final String targetKey,
                final String reasonCode,
                final String statusCode,
                final String details,
                final String appealReason,
                final int appealCount,
                final String createdAtLabel,
                final String updatedAtLabel,
                final String appealedAtLabel,
                final boolean appealed) {
            this.id = id;
            this.reporterUserId = reporterUserId;
            this.targetTypeCode = targetTypeCode;
            this.targetKey = targetKey;
            this.reasonCode = reasonCode;
            this.statusCode = statusCode;
            this.details = details;
            this.appealReason = appealReason;
            this.appealCount = appealCount;
            this.createdAtLabel = createdAtLabel;
            this.updatedAtLabel = updatedAtLabel;
            this.appealedAtLabel = appealedAtLabel;
            this.appealed = appealed;
        }

        public Long getId() {
            return id;
        }

        public Long getReporterUserId() {
            return reporterUserId;
        }

        public String getTargetTypeCode() {
            return targetTypeCode;
        }

        public String getTargetKey() {
            return targetKey;
        }

        public String getReasonCode() {
            return reasonCode;
        }

        public String getStatusCode() {
            return statusCode;
        }

        public String getDetails() {
            return details;
        }

        public String getAppealReason() {
            return appealReason;
        }

        public int getAppealCount() {
            return appealCount;
        }

        public String getCreatedAtLabel() {
            return createdAtLabel;
        }

        public String getUpdatedAtLabel() {
            return updatedAtLabel;
        }

        public String getAppealedAtLabel() {
            return appealedAtLabel;
        }

        public boolean isAppealed() {
            return appealed;
        }
    }
}
