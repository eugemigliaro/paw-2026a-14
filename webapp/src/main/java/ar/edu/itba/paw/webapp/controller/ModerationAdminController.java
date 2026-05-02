package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.AppealDecision;
import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.ReportResolution;
import ar.edu.itba.paw.models.ReportStatus;
import ar.edu.itba.paw.models.ReportTargetType;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.exceptions.ModerationException;
import ar.edu.itba.paw.webapp.form.ModerationResolutionForm;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

    @ModelAttribute("resolutionForm")
    public ModerationResolutionForm resolutionForm() {
        return new ModerationResolutionForm();
    }

    @GetMapping
    public ModelAndView showReports(
            @RequestParam(value = "type", required = false) final List<String> typeFilters,
            @RequestParam(value = "status", required = false) final List<String> statusFilters,
            final Locale locale) {

        final List<ReportTargetType> selectedTypes =
                parseEnumFilters(typeFilters, ReportTargetType::fromDbValue);
        final List<ReportStatus> selectedStatuses =
                parseEnumFilters(statusFilters, ReportStatus::fromDbValue);

        final List<ModerationReportViewModel> reports =
                moderationService.findReports(selectedTypes, selectedStatuses).stream()
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
                "reportCountLabel",
                messageSource.getMessage(
                        "admin.reports.count", new Object[] {reports.size()}, locale));
        mav.addObject(
                "emptyMessage", messageSource.getMessage("admin.reports.empty", null, locale));
        mav.addObject("reports", reports);
        mav.addObject(
                "selectedTypes", selectedTypes.stream().map(ReportTargetType::getDbValue).toList());
        mav.addObject(
                "selectedStatuses",
                selectedStatuses.stream().map(ReportStatus::getDbValue).toList());
        return mav;
    }

    @GetMapping("/{reportId:\\d+}")
    public ModelAndView showReportDetail(
            @PathVariable("reportId") final Long reportId, final Locale locale) {
        final ModerationReport report =
                moderationService
                        .findReportById(reportId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        final ModerationReportViewModel reportVm = toViewModel(report, locale);
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

        mav.addObject("report", reportVm);

        final boolean showResolution =
                report.getResolution() != null
                        || report.getStatus() == ReportStatus.UNDER_REVIEW
                        || report.getStatus() == ReportStatus.PENDING;
        mav.addObject("showResolution", showResolution);

        final boolean showAppeal =
                report.getAppealCount() > 0 || report.getStatus() == ReportStatus.APPEALED;
        mav.addObject("showAppeal", showAppeal);

        final boolean showAppealResolution = showAppeal;
        mav.addObject("showAppealResolution", showAppealResolution);

        if (showResolution) {
            mav.addObject("userBan", userBanViewModel(report, locale));
        }

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
            @PathVariable("reportId") final Long reportId,
            @ModelAttribute("resolutionForm") final ModerationResolutionForm form,
            final Locale locale) {
        return resolveReport(
                reportId,
                ReportResolution.DISMISSED,
                "dismissed",
                form.getResolutionDetails(),
                locale);
    }

    @PostMapping("/{reportId:\\d+}/delete-content")
    public ModelAndView deleteContent(
            @PathVariable("reportId") final Long reportId,
            @ModelAttribute("resolutionForm") final ModerationResolutionForm form,
            final Locale locale) {
        return resolveReport(
                reportId,
                ReportResolution.CONTENT_DELETED,
                "deleted",
                form.getResolutionDetails(),
                locale);
    }

    @PostMapping("/{reportId:\\d+}/ban-user")
    public ModelAndView banUser(
            @PathVariable("reportId") final Long reportId,
            @RequestParam(value = "banDays", required = false, defaultValue = "7")
                    final int banDays,
            @ModelAttribute("resolutionForm") final ModerationResolutionForm form,
            final Locale locale) {
        try {
            return resolveReport(
                    reportId,
                    ReportResolution.USER_BANNED,
                    "banned",
                    form.getResolutionDetails(),
                    locale);
        } catch (final ModerationException ex) {
            return redirectToReportsError(ex.getCode());
        }
    }

    @PostMapping("/{reportId:\\d+}/finalize-appeal")
    public ModelAndView finalizeAppeal(
            @PathVariable("reportId") final Long reportId,
            @RequestParam("appealDecision") final String appealResolution,
            final Locale locale) {
        final AppealDecision parsedAppealDecision =
                AppealDecision.fromDbValue(appealResolution)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        try {
            moderationService.finalizeReportAppeal(
                    reportId, currentAdminUserId(), parsedAppealDecision);
            return redirectToReports("appeal_finalized");
        } catch (final ModerationException ex) {
            return redirectToReportsError(ex.getCode());
        }
    }

    private ModelAndView resolveReport(
            final Long reportId,
            final ReportResolution resolution,
            final String actionCode,
            final String resolutionDetails,
            final Locale locale) {
        try {
            final ModerationReport report =
                    moderationService.resolveReport(
                            reportId,
                            currentAdminUserId(),
                            resolution,
                            resolutionDetails,
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
                report.getResolution() == null ? "" : report.getResolution().getDbValue(),
                report.getDetails(),
                report.getAppealReason(),
                report.getResolutionDetails(),
                report.getAppealCount(),
                formatInstant(report.getCreatedAt(), locale),
                formatInstant(report.getUpdatedAt(), locale),
                formatInstant(report.getAppealedAt(), locale),
                formatInstant(report.getReviewedAt(), locale),
                report.getReviewedByUserId(),
                report.getAppealDecision() == null ? "" : report.getAppealDecision().getDbValue(),
                formatInstant(report.getAppealResolvedAt(), locale),
                report.getAppealResolvedByUserId(),
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

    private static <T> List<T> parseEnumFilters(
            final List<String> rawValues, final Function<String, Optional<T>> parser) {
        if (rawValues == null || rawValues.isEmpty()) {
            return List.of();
        }
        final Set<T> parsed =
                rawValues.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(parser)
                        .flatMap(Optional::stream)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        return List.copyOf(parsed);
    }

    private UserBanViewModel userBanViewModel(final ModerationReport report, final Locale locale) {
        if (report.getTargetType() != ReportTargetType.USER) {
            return null;
        }

        final Optional<UserBan> latestBanForUser =
                moderationService.findLatestBanForUser(report.getTargetId());

        if (latestBanForUser.isEmpty()
                || !latestBanForUser.get().getModerationReportId().equals(report.getId())) {
            return null;
        }

        final UserBan ban = latestBanForUser.get();
        return new UserBanViewModel(
                latestBanForUser.get().getId(), formatInstant(ban.getBannedUntil(), locale));
    }

    public static final class UserBanViewModel {
        private final Long banId;
        private final String bannedUntilLabel;

        private UserBanViewModel(final Long banId, final String bannedUntilLabel) {
            this.banId = banId;
            this.bannedUntilLabel = bannedUntilLabel;
        }

        public Long getBanId() {
            return banId;
        }

        public String getBannedUntilLabel() {
            return bannedUntilLabel;
        }
    }

    public static final class ModerationReportViewModel {
        private final Long id;
        private final Long reporterUserId;
        private final String targetTypeCode;
        private final String targetKey;
        private final String reasonCode;
        private final String statusCode;
        private final String resolutionCode;
        private final String details;
        private final String appealReason;
        private final String resolutionDetails;
        private final int appealCount;
        private final String createdAtLabel;
        private final String updatedAtLabel;
        private final String appealedAtLabel;
        private final String reviewedAtLabel;
        private final Long reviewedByUserId;
        private final String appealDecisionCode;
        private final String appealResolvedAtLabel;
        private final Long appealResolvedByUserId;
        private final boolean appealed;

        private ModerationReportViewModel(
                final Long id,
                final Long reporterUserId,
                final String targetTypeCode,
                final String targetKey,
                final String reasonCode,
                final String statusCode,
                final String resolutionCode,
                final String details,
                final String appealReason,
                final String resolutionDetails,
                final int appealCount,
                final String createdAtLabel,
                final String updatedAtLabel,
                final String appealedAtLabel,
                final String reviewedAtLabel,
                final Long reviewedByUserId,
                final String appealDecisionCode,
                final String appealResolvedAtLabel,
                final Long appealResolvedByUserId,
                final boolean appealed) {
            this.id = id;
            this.reporterUserId = reporterUserId;
            this.targetTypeCode = targetTypeCode;
            this.targetKey = targetKey;
            this.reasonCode = reasonCode;
            this.statusCode = statusCode;
            this.resolutionCode = resolutionCode;
            this.details = details;
            this.appealReason = appealReason;
            this.resolutionDetails = resolutionDetails;
            this.appealCount = appealCount;
            this.createdAtLabel = createdAtLabel;
            this.updatedAtLabel = updatedAtLabel;
            this.appealedAtLabel = appealedAtLabel;
            this.reviewedAtLabel = reviewedAtLabel;
            this.reviewedByUserId = reviewedByUserId;
            this.appealDecisionCode = appealDecisionCode;
            this.appealResolvedAtLabel = appealResolvedAtLabel;
            this.appealResolvedByUserId = appealResolvedByUserId;
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

        public String getResolutionCode() {
            return resolutionCode;
        }

        public String getDetails() {
            return details;
        }

        public String getAppealReason() {
            return appealReason;
        }

        public String getResolutionDetails() {
            return resolutionDetails;
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

        public String getReviewedAtLabel() {
            return reviewedAtLabel;
        }

        public Long getReviewedByUserId() {
            return reviewedByUserId;
        }

        public String getAppealDecisionCode() {
            return appealDecisionCode;
        }

        public String getAppealResolvedAtLabel() {
            return appealResolvedAtLabel;
        }

        public Long getAppealResolvedByUserId() {
            return appealResolvedByUserId;
        }

        public boolean isAppealed() {
            return appealed;
        }
    }
}
