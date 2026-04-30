package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.ReportStatus;
import ar.edu.itba.paw.models.ReportTargetType;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.exceptions.ModerationException;
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
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/reports/mine")
public class UserModerationReportController {

    private final ModerationService moderationService;
    private final MessageSource messageSource;

    public UserModerationReportController(
            final ModerationService moderationService, final MessageSource messageSource) {
        this.moderationService = moderationService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public ModelAndView showMyReports(
            @RequestParam(value = "type", required = false) final List<String> typeFilters,
            @RequestParam(value = "status", required = false) final List<String> statusFilters,
            final Locale locale) {
        final long userId = currentUserId();
        final List<ReportTargetType> selectedTypes =
                parseEnumFilters(typeFilters, ReportTargetType::fromDbValue);
        final List<ReportStatus> selectedStatuses =
                parseEnumFilters(statusFilters, ReportStatus::fromDbValue);
        final List<UserReportViewModel> reports =
                moderationService
                        .findReportsByReporter(userId, selectedTypes, selectedStatuses)
                        .stream()
                        .map(report -> toViewModel(report, locale))
                        .toList();

        final ModelAndView mav = new ModelAndView("reports/mine/list");
        mav.addObject(
                "shell", ShellViewModelFactory.playerShell(messageSource, locale, "/reports/mine"));
        mav.addObject("pageTitle", messageSource.getMessage("page.title.myReports", null, locale));
        mav.addObject(
                "pageTitleLabel", messageSource.getMessage("reports.mine.title", null, locale));
        mav.addObject(
                "pageDescription",
                messageSource.getMessage("reports.mine.description", null, locale));
        mav.addObject("emptyMessage", messageSource.getMessage("reports.mine.empty", null, locale));
        mav.addObject(
                "reportCountLabel",
                messageSource.getMessage(
                        "reports.mine.count", new Object[] {reports.size()}, locale));
        mav.addObject("reports", reports);
        mav.addObject(
                "selectedTypes", selectedTypes.stream().map(ReportTargetType::getDbValue).toList());
        mav.addObject(
                "selectedStatuses",
                selectedStatuses.stream().map(ReportStatus::getDbValue).toList());
        return mav;
    }

    @GetMapping("/{reportId:\\d+}")
    public ModelAndView showMyReportDetail(
            @PathVariable("reportId") final Long reportId, final Locale locale) {
        final long userId = currentUserId();
        final ModerationReport report =
                moderationService
                        .findReportById(reportId)
                        .filter(found -> found.getReporterUserId().equals(userId))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        final ModelAndView mav = new ModelAndView("reports/mine/detail");
        mav.addObject(
                "shell", ShellViewModelFactory.playerShell(messageSource, locale, "/reports/mine"));
        mav.addObject(
                "pageTitle", messageSource.getMessage("page.title.myReportDetail", null, locale));
        mav.addObject(
                "pageTitleLabel",
                messageSource.getMessage(
                        "reports.mine.detail.title", new Object[] {report.getId()}, locale));
        mav.addObject(
                "pageDescription",
                messageSource.getMessage("reports.mine.detail.description", null, locale));
        mav.addObject(
                "appealAllowed",
                report.getStatus() == ReportStatus.RESOLVED && report.getAppealCount() < 1);
        mav.addObject("report", toViewModel(report, locale));
        return mav;
    }

    @PostMapping("/{reportId:\\d+}/appeal")
    public ModelAndView appealReport(
            @PathVariable("reportId") final Long reportId,
            @RequestParam("appealReason") final String appealReason,
            final Locale locale) {
        final long userId = currentUserId();
        try {
            moderationService.appealReport(reportId, userId, appealReason);
            return new ModelAndView("redirect:/reports/mine/" + reportId + "?action=appealed");
        } catch (final ModerationException exception) {
            return new ModelAndView(
                    "redirect:/reports/mine/" + reportId + "?error=" + exception.getCode());
        }
    }

    private long currentUserId() {
        return CurrentAuthenticatedUser.get()
                .map(AuthenticatedUserPrincipal::getUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private UserReportViewModel toViewModel(final ModerationReport report, final Locale locale) {
        return new UserReportViewModel(
                report.getId(),
                report.getTargetType() == null ? "" : report.getTargetType().getDbValue(),
                moderationService.resolveTargetName(report.getTargetType(), report.getTargetId()),
                report.getReason() == null ? "" : report.getReason().getDbValue(),
                report.getStatus() == null ? "" : report.getStatus().getDbValue(),
                report.getResolution() == null ? "" : report.getResolution().getDbValue(),
                report.getDetails(),
                report.getResolutionDetails(),
                report.getAppealReason(),
                report.getAppealResolution() == null
                        ? ""
                        : report.getAppealResolution().getDbValue(),
                report.getAppealCount(),
                formatInstant(report.getCreatedAt(), locale),
                formatInstant(report.getUpdatedAt(), locale),
                formatInstant(report.getReviewedAt(), locale),
                formatInstant(report.getAppealedAt(), locale),
                formatInstant(report.getAppealResolvedAt(), locale));
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

    public static final class UserReportViewModel {
        private final Long id;
        private final String targetTypeCode;
        private final String targetKey;
        private final String reasonCode;
        private final String statusCode;
        private final String resolutionCode;
        private final String details;
        private final String resolutionDetails;
        private final String appealReason;
        private final String appealResolutionCode;
        private final int appealCount;
        private final String createdAtLabel;
        private final String updatedAtLabel;
        private final String reviewedAtLabel;
        private final String appealedAtLabel;
        private final String appealResolvedAtLabel;

        private UserReportViewModel(
                final Long id,
                final String targetTypeCode,
                final String targetKey,
                final String reasonCode,
                final String statusCode,
                final String resolutionCode,
                final String details,
                final String resolutionDetails,
                final String appealReason,
                final String appealResolutionCode,
                final int appealCount,
                final String createdAtLabel,
                final String updatedAtLabel,
                final String reviewedAtLabel,
                final String appealedAtLabel,
                final String appealResolvedAtLabel) {
            this.id = id;
            this.targetTypeCode = targetTypeCode;
            this.targetKey = targetKey;
            this.reasonCode = reasonCode;
            this.statusCode = statusCode;
            this.resolutionCode = resolutionCode;
            this.details = details;
            this.resolutionDetails = resolutionDetails;
            this.appealReason = appealReason;
            this.appealResolutionCode = appealResolutionCode;
            this.appealCount = appealCount;
            this.createdAtLabel = createdAtLabel;
            this.updatedAtLabel = updatedAtLabel;
            this.reviewedAtLabel = reviewedAtLabel;
            this.appealedAtLabel = appealedAtLabel;
            this.appealResolvedAtLabel = appealResolvedAtLabel;
        }

        public Long getId() {
            return id;
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

        public String getResolutionDetails() {
            return resolutionDetails;
        }

        public String getAppealReason() {
            return appealReason;
        }

        public String getAppealResolutionCode() {
            return appealResolutionCode;
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

        public String getReviewedAtLabel() {
            return reviewedAtLabel;
        }

        public String getAppealedAtLabel() {
            return appealedAtLabel;
        }

        public String getAppealResolvedAtLabel() {
            return appealResolvedAtLabel;
        }
    }
}
