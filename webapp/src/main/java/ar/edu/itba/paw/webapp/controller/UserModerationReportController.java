package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.ReportStatus;
import ar.edu.itba.paw.models.ReportTargetType;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.exceptions.ModerationException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PaginationItemViewModel;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/reports/mine")
public class UserModerationReportController {
    private static final int PAGE_SIZE = 4;

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
            @RequestParam(value = "page", defaultValue = "1") final int page,
            final Locale locale) {
        final long userId = currentUserId();
        final List<ReportTargetType> selectedTypes =
                parseEnumFilters(typeFilters, ReportTargetType::fromDbValue);
        final List<ReportStatus> selectedStatuses =
                parseEnumFilters(statusFilters, ReportStatus::fromDbValue);
        final PaginatedResult<ModerationReport> result =
                moderationService.findReportsByReporter(
                        userId, selectedTypes, selectedStatuses, page, PAGE_SIZE);
        final List<UserReportViewModel> reports =
                result.getItems().stream().map(report -> toViewModel(report, locale)).toList();

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
                        "reports.mine.count", new Object[] {result.getTotalCount()}, locale));
        mav.addObject("reports", reports);
        mav.addObject(
                "selectedTypes", selectedTypes.stream().map(ReportTargetType::getDbValue).toList());
        mav.addObject(
                "selectedStatuses",
                selectedStatuses.stream().map(ReportStatus::getDbValue).toList());
        mav.addObject("hasPreviousPage", result.hasPrevious());
        mav.addObject("hasNextPage", result.hasNext());
        mav.addObject("previousPageHref", buildPageUrl(selectedTypes, selectedStatuses, page - 1));
        mav.addObject("nextPageHref", buildPageUrl(selectedTypes, selectedStatuses, page + 1));
        mav.addObject(
                "paginationItems", buildPaginationItems(selectedTypes, selectedStatuses, result));
        return mav;
    }

    private List<PaginationItemViewModel> buildPaginationItems(
            final List<ReportTargetType> selectedTypes,
            final List<ReportStatus> selectedStatuses,
            final PaginatedResult<ModerationReport> result) {
        if (result.getTotalPages() <= 1) {
            return List.of();
        }

        final List<PaginationItemViewModel> items = new ArrayList<>();
        final int startPage =
                Math.max(2, Math.min(result.getPage() - 1, result.getTotalPages() - 3));
        final int endPage = Math.min(result.getTotalPages() - 1, Math.max(result.getPage() + 1, 4));

        items.add(pageItem(selectedTypes, selectedStatuses, 1, result.getPage()));
        if (startPage > 2) {
            items.add(new PaginationItemViewModel("...", null, false, true));
        }
        for (int currentPage = startPage; currentPage <= endPage; currentPage++) {
            items.add(pageItem(selectedTypes, selectedStatuses, currentPage, result.getPage()));
        }
        if (endPage < result.getTotalPages() - 1) {
            items.add(new PaginationItemViewModel("...", null, false, true));
        }
        items.add(
                pageItem(
                        selectedTypes, selectedStatuses, result.getTotalPages(), result.getPage()));
        return items;
    }

    private PaginationItemViewModel pageItem(
            final List<ReportTargetType> selectedTypes,
            final List<ReportStatus> selectedStatuses,
            final int page,
            final int currentPage) {
        return new PaginationItemViewModel(
                Integer.toString(page),
                buildPageUrl(selectedTypes, selectedStatuses, page),
                page == currentPage,
                false);
    }

    private String buildPageUrl(
            final List<ReportTargetType> selectedTypes,
            final List<ReportStatus> selectedStatuses,
            final int page) {
        final UriComponentsBuilder builder =
                UriComponentsBuilder.fromPath("/reports/mine").queryParam("page", page);
        selectedTypes.stream()
                .map(ReportTargetType::getDbValue)
                .forEach(type -> builder.queryParam("type", type));
        selectedStatuses.stream()
                .map(ReportStatus::getDbValue)
                .forEach(status -> builder.queryParam("status", status));
        return builder.build().encode().toUriString();
    }

    @GetMapping("/{reportId:\\d+}")
    public ModelAndView showMyReportDetail(
            @PathVariable("reportId") final Long reportId, final Model model, final Locale locale) {
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
        mav.addObject("action", model.asMap().get("action"));
        return mav;
    }

    @PostMapping("/{reportId:\\d+}/appeal")
    public ModelAndView appealReport(
            @PathVariable("reportId") final Long reportId,
            @RequestParam("appealReason") final String appealReason,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        try {
            moderationService.appealReport(reportId, appealReason);
            redirectAttributes.addFlashAttribute("action", "appealed");
            return new ModelAndView("redirect:/reports/mine/" + reportId);
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
                report.getAppealDecision() == null ? "" : report.getAppealDecision().getDbValue(),
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
