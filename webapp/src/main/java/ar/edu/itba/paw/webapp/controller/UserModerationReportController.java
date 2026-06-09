package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.moderation.ModerationException;
import ar.edu.itba.paw.models.types.ReportStatus;
import ar.edu.itba.paw.models.types.ReportTargetType;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.ModerationTargetSummary;
import ar.edu.itba.paw.webapp.form.ReportAppealForm;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import ar.edu.itba.paw.webapp.security.annotation.CurrentUser;
import ar.edu.itba.paw.webapp.utils.PaginationUtils;
import java.util.List;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/reports/mine")
public class UserModerationReportController {
    private static final int PAGE_SIZE = 4;

    private final ModerationService moderationService;
    private final MessageSource messageSource;

    @Autowired
    public UserModerationReportController(
            final ModerationService moderationService, final MessageSource messageSource) {
        this.moderationService = moderationService;
        this.messageSource = messageSource;
    }

    @ModelAttribute("reportAppealForm")
    public ReportAppealForm reportAppealForm() {
        return new ReportAppealForm();
    }

    @GetMapping
    public ModelAndView showMyReports(
            @CurrentUser final User user,
            @RequestParam(value = "type", required = false, defaultValue = "")
                    final List<ReportTargetType> typeFilters,
            @RequestParam(value = "status", required = false, defaultValue = "")
                    final List<ReportStatus> statusFilters,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            final Locale locale) {
        final PaginatedResult<ModerationReport> result =
                moderationService.findReportsByReporter(
                        user, typeFilters, statusFilters, page, PAGE_SIZE);
        final List<ReportView> reportViews = reportViews(result.getItems());

        final ModelAndView mav = new ModelAndView("reports/mine/list");
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
        mav.addObject("reportViews", reportViews);
        mav.addObject(
                "selectedTypes", typeFilters.stream().map(ReportTargetType::getDbValue).toList());
        mav.addObject(
                "selectedStatuses", statusFilters.stream().map(ReportStatus::getDbValue).toList());
        mav.addObject("hasPreviousPage", result.hasPrevious());
        mav.addObject("hasNextPage", result.hasNext());
        mav.addObject("previousPageHref", buildPageUrl(typeFilters, statusFilters, page - 1));
        mav.addObject("nextPageHref", buildPageUrl(typeFilters, statusFilters, page + 1));
        mav.addObject(
                "paginationItems",
                PaginationUtils.buildPaginationItems(
                        result.getPage(),
                        result.getTotalPages(),
                        paginationPage ->
                                buildPageUrl(typeFilters, statusFilters, paginationPage)));
        return mav;
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
            @AuthenticatedUser final User user,
            @PathVariable("reportId") final Long reportId,
            final Model model,
            final Locale locale) {
        final ModerationReport report =
                moderationService
                        .findReportById(reportId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (report.getReporter() == null || !report.getReporter().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        final ModerationTargetSummary targetSummary =
                moderationService.resolveTarget(report.getTargetType(), report.getTargetId());

        final ModelAndView mav = new ModelAndView("reports/mine/detail");
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
        mav.addObject("report", report);
        mav.addObject("targetSummary", targetSummary);
        mav.addObject("targetHref", targetHref(targetSummary));
        mav.addObject("action", model.asMap().get("action"));
        return mav;
    }

    @PostMapping("/{reportId:\\d+}/appeal")
    public ModelAndView appealReport(
            @AuthenticatedUser final User user,
            @PathVariable("reportId") final Long reportId,
            @Valid @ModelAttribute("reportAppealForm") final ReportAppealForm reportAppealForm,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    messageSource.getMessage("moderation.report.error.invalid", null, locale));
            return new ModelAndView("redirect:/reports/mine/" + reportId);
        }

        try {
            moderationService.appealReport(reportId, user, reportAppealForm.getDetails());
            redirectAttributes.addFlashAttribute("action", "appealed");
            return new ModelAndView("redirect:/reports/mine/" + reportId);
        } catch (final ModerationException e) {
            return new ModelAndView(
                    "redirect:/reports/mine/" + reportId + "?error=" + e.getMessage());
        }
    }

    private List<ReportView> reportViews(final List<ModerationReport> reports) {
        final List<ReportView> reportViews = new java.util.ArrayList<>();
        for (final ModerationReport report : reports) {
            final ModerationTargetSummary targetSummary =
                    moderationService.resolveTarget(report.getTargetType(), report.getTargetId());
            reportViews.add(new ReportView(report, targetSummary, targetHref(targetSummary)));
        }
        return reportViews;
    }

    private String targetHref(final ModerationTargetSummary targetSummary) {
        if (targetSummary == null || !targetSummary.isFound()) {
            return null;
        }
        return switch (targetSummary.getTargetType()) {
            case USER ->
                    targetSummary.getTargetSlug() == null || targetSummary.getTargetSlug().isBlank()
                            ? null
                            : "/users/" + targetSummary.getTargetSlug();
            case MATCH -> "/matches/" + targetSummary.getTargetId();
            case REVIEW -> null;
        };
    }

    public static final class ReportView {
        private final ModerationReport report;
        private final ModerationTargetSummary targetSummary;
        private final String targetHref;

        public ReportView(
                final ModerationReport report,
                final ModerationTargetSummary targetSummary,
                final String targetHref) {
            this.report = report;
            this.targetSummary = targetSummary;
            this.targetHref = targetHref;
        }

        public ModerationReport getReport() {
            return report;
        }

        public ModerationTargetSummary getTargetSummary() {
            return targetSummary;
        }

        public String getTargetHref() {
            return targetHref;
        }
    }
}
