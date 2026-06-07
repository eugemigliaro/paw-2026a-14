package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.types.AppealDecision;
import ar.edu.itba.paw.models.types.ReportResolution;
import ar.edu.itba.paw.models.types.ReportStatus;
import ar.edu.itba.paw.models.types.ReportTargetType;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.ModerationTargetSummary;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.exceptions.moderation.ModerationException;
import ar.edu.itba.paw.services.exceptions.moderation.ModerationReportNotFoundException;
import ar.edu.itba.paw.webapp.form.ModerationAppealResolutionForm;
import ar.edu.itba.paw.webapp.form.ModerationResolutionForm;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.PaginationUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.groups.Default;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/admin/reports")
public class ModerationAdminController {
    private static final int PAGE_SIZE = 4;
    private static final int DEFAULT_BAN_DURATION_DAYS = 7;

    private final ModerationService moderationService;
    private final UserService userService;
    private final MessageSource messageSource;

    @Autowired
    public ModerationAdminController(
            final ModerationService moderationService,
            final UserService userService,
            final MessageSource messageSource) {
        this.moderationService = moderationService;
        this.userService = userService;
        this.messageSource = messageSource;
    }

    @ModelAttribute("resolutionForm")
    public ModerationResolutionForm resolutionForm() {
        return new ModerationResolutionForm();
    }

    @ModelAttribute("appealResolutionForm")
    public ModerationAppealResolutionForm appealResolutionForm() {
        return new ModerationAppealResolutionForm();
    }

    @GetMapping
    public ModelAndView showReports(
            @RequestParam(value = "type", required = false, defaultValue = "")
                    final List<ReportTargetType> typeFilters,
            @RequestParam(value = "status", required = false, defaultValue = "")
                    final List<ReportStatus> statusFilters,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            final Model model,
            final Locale locale) {

        final PaginatedResult<ModerationReport> result =
                moderationService.findReports(typeFilters, statusFilters, page, PAGE_SIZE);
        final Map<Long, ModerationTargetSummary> targetSummaries =
                targetSummariesByReportId(result.getItems());

        final ModelAndView mav = new ModelAndView("admin/reports/list");
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
                        "admin.reports.count", new Object[] {result.getTotalCount()}, locale));
        mav.addObject(
                "emptyMessage", messageSource.getMessage("admin.reports.empty", null, locale));
        mav.addObject("reports", result.getItems());
        mav.addObject("targetSummaries", targetSummaries);
        mav.addObject("action", model.asMap().get("action"));
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
                UriComponentsBuilder.fromPath("/admin/reports").queryParam("page", page);
        selectedTypes.stream()
                .map(ReportTargetType::getDbValue)
                .forEach(type -> builder.queryParam("type", type));
        selectedStatuses.stream()
                .map(ReportStatus::getDbValue)
                .forEach(status -> builder.queryParam("status", status));
        return builder.build().encode().toUriString();
    }

    @GetMapping("/{reportId:\\d+}")
    public ModelAndView showReportDetail(
            @PathVariable("reportId") final Long reportId, final Locale locale) {
        final ModerationReport report =
                moderationService
                        .findReportById(reportId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return reportDetailModelAndView(report, locale);
    }

    private ModelAndView reportDetailModelAndView(
            final ModerationReport report, final Locale locale) {
        final ModelAndView mav = new ModelAndView("admin/reports/detail");

        mav.addObject(
                "pageTitle",
                messageSource.getMessage("page.title.adminReportDetail", null, locale));
        mav.addObject(
                "pageTitleLabel",
                messageSource.getMessage("admin.reports.detail.title", null, locale));
        mav.addObject(
                "pageDescription",
                messageSource.getMessage("admin.reports.detail.description", null, locale));

        mav.addObject("report", report);
        mav.addObject(
                "targetSummary",
                moderationService.resolveTarget(report.getTargetType(), report.getTargetId()));

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
            mav.addObject("userBan", userBan(report));
        }

        String reporterUsername =
                report.getReporter() != null ? report.getReporter().getUsername() : "";
        String reviewerUsername =
                report.getReviewer() != null ? report.getReviewer().getUsername() : "";

        mav.addObject("reporterUsername", reporterUsername);
        mav.addObject("reviewerUsername", reviewerUsername);

        return mav;
    }

    @PostMapping("/{reportId:\\d+}/under-review")
    public ModelAndView markUnderReview(
            @AuthenticatedUser final User user,
            @PathVariable("reportId") final Long reportId,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        try {
            moderationService.markReportUnderReview(reportId, user);
            return redirectToReports("reviewed", redirectAttributes);
        } catch (final ModerationException ex) {
            return redirectToReportsError("report_not_found");
        }
    }

    @PostMapping("/{reportId:\\d+}/dismiss")
    public ModelAndView dismissReport(
            @AuthenticatedUser final User user,
            @PathVariable("reportId") final Long reportId,
            @Valid @ModelAttribute("resolutionForm") final ModerationResolutionForm form,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        if (bindingResult.hasErrors()) {
            return reportDetailWithErrors(reportId, locale, form, bindingResult);
        }
        return resolveReport(
                user,
                reportId,
                ReportResolution.DISMISSED,
                "dismissed",
                form.getResolutionDetails(),
                DEFAULT_BAN_DURATION_DAYS,
                redirectAttributes,
                locale);
    }

    @PostMapping("/{reportId:\\d+}/delete-content")
    public ModelAndView deleteContent(
            @AuthenticatedUser final User user,
            @PathVariable("reportId") final Long reportId,
            @Valid @ModelAttribute("resolutionForm") final ModerationResolutionForm form,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        if (bindingResult.hasErrors()) {
            return reportDetailWithErrors(reportId, locale, form, bindingResult);
        }
        return resolveReport(
                user,
                reportId,
                ReportResolution.CONTENT_DELETED,
                "deleted",
                form.getResolutionDetails(),
                DEFAULT_BAN_DURATION_DAYS,
                redirectAttributes,
                locale);
    }

    @PostMapping("/{reportId:\\d+}/ban-user")
    public ModelAndView banUser(
            @AuthenticatedUser final User user,
            @PathVariable("reportId") final Long reportId,
            @Validated({Default.class, ModerationResolutionForm.BanAction.class})
                    @ModelAttribute("resolutionForm")
                    final ModerationResolutionForm form,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        if (bindingResult.hasErrors()) {
            return reportDetailWithErrors(reportId, locale, form, bindingResult);
        }
        try {
            return resolveReport(
                    user,
                    reportId,
                    ReportResolution.USER_BANNED,
                    "banned",
                    form.getResolutionDetails(),
                    form.getBanDays(),
                    redirectAttributes,
                    locale);
        } catch (final ModerationException ex) {
            return redirectToReportsError(reportsErrorCode(ex));
        }
    }

    @PostMapping("/{reportId:\\d+}/finalize-appeal")
    public ModelAndView finalizeAppeal(
            @AuthenticatedUser final User user,
            @PathVariable("reportId") final Long reportId,
            @Valid @ModelAttribute("appealResolutionForm")
                    final ModerationAppealResolutionForm form,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        if (bindingResult.hasErrors()) {
            return appealDetailWithErrors(reportId, locale, form, bindingResult);
        }
        try {
            moderationService.finalizeReportAppeal(reportId, user, form.getAppealDecision());
            final String action =
                    form.getAppealDecision() == AppealDecision.UPHELD
                            ? "appeal_upheld"
                            : "appeal_lifted";
            return redirectToReports(action, redirectAttributes);
        } catch (final ModerationException ex) {
            return redirectToReportsError(reportsErrorCode(ex));
        }
    }

    private ModelAndView resolveReport(
            final User currentUser,
            final Long reportId,
            final ReportResolution resolution,
            final String actionCode,
            final String resolutionDetails,
            final int banDurationDays,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        // TODO: get this as a parameter. Controller should have a controller advice that injects
        // the user
        try {
            final ModerationReport report =
                    moderationService.resolveReport(
                            reportId,
                            currentUser,
                            resolution,
                            resolutionDetails,
                            ReportStatus.RESOLVED,
                            banDurationDays);
            if (report == null) {
                return redirectToReportsError("report_not_found");
            }
            return redirectToReports(actionCode, redirectAttributes);
        } catch (final ModerationException ex) {
            return redirectToReportsError(reportsErrorCode(ex));
        }
    }

    private ModelAndView redirectToReports(
            final String actionCode, final RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("action", actionCode);
        return new ModelAndView("redirect:/admin/reports");
    }

    private static String reportsErrorCode(final ModerationException exception) {
        if (exception instanceof ModerationReportNotFoundException) {
            return "report_not_found";
        }
        return "action_failed";
    }

    private ModelAndView redirectToReportsError(final String errorCode) {
        return new ModelAndView("redirect:/admin/reports?error=" + errorCode);
    }

    private ModelAndView reportDetailWithErrors(
            final Long reportId,
            final Locale locale,
            final ModerationResolutionForm form,
            final BindingResult bindingResult) {
        final ModerationReport report =
                moderationService
                        .findReportById(reportId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return reportDetailModelAndView(report, locale)
                .addObject("resolutionForm", form)
                .addObject(BindingResult.MODEL_KEY_PREFIX + "resolutionForm", bindingResult);
    }

    private ModelAndView appealDetailWithErrors(
            final Long reportId,
            final Locale locale,
            final ModerationAppealResolutionForm form,
            final BindingResult bindingResult) {
        final ModerationReport report =
                moderationService
                        .findReportById(reportId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return reportDetailModelAndView(report, locale)
                .addObject("appealResolutionForm", form)
                .addObject(BindingResult.MODEL_KEY_PREFIX + "appealResolutionForm", bindingResult);
    }

    private Map<Long, ModerationTargetSummary> targetSummariesByReportId(
            final List<ModerationReport> reports) {
        final Map<Long, ModerationTargetSummary> targetSummaries = new LinkedHashMap<>();
        for (final ModerationReport report : reports) {
            targetSummaries.put(
                    report.getId(),
                    moderationService.resolveTarget(report.getTargetType(), report.getTargetId()));
        }
        return targetSummaries;
    }

    private UserBan userBan(final ModerationReport report) {
        if (report.getTargetType() != ReportTargetType.USER) {
            return null;
        }

        final User targetUser =
                userService
                        .findById(report.getTargetId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        final Optional<UserBan> latestBanForUser =
                moderationService.findLatestBanForUser(targetUser);

        if (latestBanForUser.isEmpty()
                || !latestBanForUser.get().getModerationReport().getId().equals(report.getId())) {
            return null;
        }

        return latestBanForUser.get();
    }
}
