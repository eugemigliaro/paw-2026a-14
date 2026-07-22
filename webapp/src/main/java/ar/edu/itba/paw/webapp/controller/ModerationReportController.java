package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.profileUrlFor;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.moderation.ModerationException;
import ar.edu.itba.paw.models.types.ReportTargetType;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.ReportForm;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ModerationReportController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationReportController.class);

    private final ModerationService moderationService;
    private final UserService userService;
    private final MatchService matchService;
    private final PlayerReviewService playerReviewService;

    @Autowired
    public ModerationReportController(
            final ModerationService moderationService,
            final UserService userService,
            final MatchService matchService,
            final PlayerReviewService playerReviewService) {
        this.moderationService = moderationService;
        this.userService = userService;
        this.matchService = matchService;
        this.playerReviewService = playerReviewService;
    }

    @GetMapping("/reports/users/{username}")
    public ModelAndView showUserReportPage(
            @PathVariable("username") final String username,
            @RequestParam(value = "report", required = false) final String reportStatus,
            @RequestParam(value = "reportError", required = false) final String reportErrorCode,
            @ModelAttribute("reportForm") final ReportForm form,
            final Model model) {
        final User reportedUser = userService.findByUsername(username).orElse(null);
        return baseReportView(
                model.asMap().get("reportSent") == Boolean.TRUE ? "sent" : reportStatus,
                reportErrorCode,
                "page.title.reportUser",
                "report.page.user.description",
                new Object[] {username},
                "report.page.user.title",
                "/reports/users/" + username,
                ReportTargetType.USER,
                reportedUser,
                null,
                null);
    }

    @GetMapping("/reports/reviews/{reviewId}")
    public ModelAndView showReviewReportPage(
            @PathVariable("reviewId") final Long reviewId,
            @RequestParam(value = "report", required = false) final String reportStatus,
            @RequestParam(value = "reportError", required = false) final String reportErrorCode,
            @ModelAttribute("reportForm") final ReportForm form,
            final Model model) {
        final PlayerReview review = playerReviewService.findReviewById(reviewId).orElse(null);
        final User author = review.getReviewer();
        final User reviewedUser = review.getReviewed();
        return baseReportView(
                model.asMap().get("reportSent") == Boolean.TRUE ? "sent" : reportStatus,
                reportErrorCode,
                "page.title.reportReview",
                "report.page.review.description",
                new Object[] {author.getUsername(), reviewedUser.getUsername()},
                "report.page.review.title",
                "/reports/reviews/" + review.getId(),
                ReportTargetType.REVIEW,
                null,
                review,
                null);
    }

    @GetMapping("/reports/matches/{matchId}")
    public ModelAndView showMatchReportPage(
            @PathVariable("matchId") final Long matchId,
            @RequestParam(value = "report", required = false) final String reportStatus,
            @RequestParam(value = "reportError", required = false) final String reportErrorCode,
            @ModelAttribute("reportForm") final ReportForm form,
            final Model model) {
        final Match match = matchService.findMatchById(matchId).orElse(null);
        return baseReportView(
                model.asMap().get("reportSent") == Boolean.TRUE ? "sent" : reportStatus,
                reportErrorCode,
                "page.title.reportMatch",
                "report.page.match.descriptionText",
                new Object[] {match.getTitle()},
                "report.page.match.title",
                "/reports/matches/" + match.getId(),
                ReportTargetType.MATCH,
                null,
                null,
                match);
    }

    @PostMapping("/reports/users/{username}")
    public ModelAndView reportUser(
            @AuthenticatedUser final User user,
            @PathVariable("username") final String username,
            @Valid @ModelAttribute("reportForm") final ReportForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        final User reportedUser = userService.findByUsername(username).orElse(null);

        if (errors.hasErrors()) {
            LOGGER.warn(
                    "Report submission failed validation for user={} targetUser={} reason={} errors={}",
                    user.getId(),
                    username,
                    form.getReason(),
                    errors.getAllErrors());
            return showUserReportPage(username, null, null, form, new ExtendedModelMap());
        }

        try {
            LOGGER.info(
                    "User {} is reporting user {} for reason: {}",
                    user.getId(),
                    reportedUser.getId(),
                    form.getReason());
            moderationService.reportContent(
                    user,
                    ReportTargetType.USER,
                    reportedUser.getId(),
                    form.getReason(),
                    form.getDetails());
            return redirectToReportUser(username, null, "sent", redirectAttributes);
        } catch (final ModerationException e) {
            final String errorMsg = "moderation.report.error." + e.getMessage();
            errors.reject(errorMsg);
            return showUserReportPage(username, null, null, form, new ExtendedModelMap())
                    .addObject(BindingResult.MODEL_KEY_PREFIX + "reportForm", errors);
        }
    }

    @PostMapping("/reports/reviews/{reviewId}")
    public ModelAndView reportReview(
            @AuthenticatedUser final User user,
            @PathVariable("reviewId") final Long reviewId,
            @Valid @ModelAttribute("reportForm") final ReportForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        final PlayerReview review = playerReviewService.findReviewById(reviewId).orElse(null);

        if (errors.hasErrors()) {
            LOGGER.warn(
                    "Report submission failed validation for user={} targetReview={} reason={} errors={}",
                    user.getId(),
                    reviewId,
                    form.getReason(),
                    errors.getAllErrors());
            return showReviewReportPage(reviewId, null, null, form, new ExtendedModelMap());
        }

        try {
            LOGGER.info(
                    "User {} is reporting review {} for reason: {}",
                    user.getId(),
                    review.getId(),
                    form.getReason());
            moderationService.reportContent(
                    user,
                    ReportTargetType.REVIEW,
                    review.getId(),
                    form.getReason(),
                    form.getDetails());
            return redirectToReportReview(review.getId(), null, "sent", redirectAttributes);
        } catch (final ModerationException e) {
            final String errorMsg = "moderation.report.error." + e.getMessage();
            errors.reject(errorMsg);
            return showReviewReportPage(reviewId, null, null, form, new ExtendedModelMap())
                    .addObject(BindingResult.MODEL_KEY_PREFIX + "reportForm", errors);
        }
    }

    @PostMapping("/reports/matches/{matchId}")
    public ModelAndView reportMatch(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            @Valid @ModelAttribute("reportForm") final ReportForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        final Match match = matchService.findMatchById(matchId).orElse(null);

        if (errors.hasErrors()) {
            LOGGER.warn(
                    "Report submission failed validation for user={} targetMatch={} reason={} errors={}",
                    user.getId(),
                    matchId,
                    form.getReason(),
                    errors.getAllErrors());
            return showMatchReportPage(matchId, null, null, form, new ExtendedModelMap());
        }

        try {
            LOGGER.info(
                    "User {} is reporting match {} for reason: {}",
                    user.getId(),
                    match.getId(),
                    form.getReason());
            moderationService.reportContent(
                    user,
                    ReportTargetType.MATCH,
                    match.getId(),
                    form.getReason(),
                    form.getDetails());
            return redirectToReportMatch(match.getId(), null, "sent", redirectAttributes);
        } catch (final ModerationException e) {
            final String errorMsg = "moderation.report.error." + e.getMessage();
            errors.reject(errorMsg);
            return showMatchReportPage(matchId, null, null, form, new ExtendedModelMap())
                    .addObject(BindingResult.MODEL_KEY_PREFIX + "reportForm", errors);
        }
    }

    private ModelAndView baseReportView(
            final String reportStatus,
            final String reportErrorCode,
            final String pageTitleCode,
            final String pageDescriptionCode,
            final Object[] pageDescriptionArguments,
            final String pageTitleLabelCode,
            final String reportActionPath,
            final ReportTargetType targetType,
            final User targetUser,
            final PlayerReview targetReview,
            final Match targetMatch) {
        final ModelAndView mav = new ModelAndView("reports/create");
        mav.addObject("pageTitleCode", pageTitleCode);
        mav.addObject("pageTitleLabelCode", pageTitleLabelCode);
        mav.addObject("pageDescriptionCode", pageDescriptionCode);
        mav.addObject("pageDescriptionArguments", pageDescriptionArguments);
        mav.addObject("reportActionPath", reportActionPath);
        mav.addObject("targetType", targetType);
        mav.addObject("targetUser", targetUser);
        mav.addObject(
                "targetUserProfileImageUrl", targetUser == null ? null : profileUrlFor(targetUser));
        mav.addObject("targetReview", targetReview);
        mav.addObject("targetMatch", targetMatch);
        mav.addObject("reportSent", "sent".equalsIgnoreCase(reportStatus));
        mav.addObject(
                "reportErrorMessageCode",
                reportErrorCode == null ? null : "moderation.report.error." + reportErrorCode);
        return mav;
    }

    private ModelAndView redirectToReportUser(
            final String username, final String errorCode, final String status) {
        final StringBuilder redirect =
                new StringBuilder("redirect:/reports/users/").append(username);
        if (errorCode != null) {
            redirect.append("?reportError=").append(errorCode);
        } else if (status != null) {
            redirect.append("?report=").append(status);
        }
        return new ModelAndView(redirect.toString());
    }

    private ModelAndView redirectToReportUser(
            final String username,
            final String errorCode,
            final String status,
            final RedirectAttributes redirectAttributes) {
        if (status != null) {
            redirectAttributes.addFlashAttribute("reportSent", true);
            return new ModelAndView("redirect:/reports/users/" + username);
        }
        return redirectToReportUser(username, errorCode, null);
    }

    private ModelAndView redirectToReportReview(
            final Long reviewId, final String errorCode, final String status) {
        final StringBuilder redirect =
                new StringBuilder("redirect:/reports/reviews/").append(reviewId);
        if (errorCode != null) {
            redirect.append("?reportError=").append(errorCode);
        } else if (status != null) {
            redirect.append("?report=").append(status);
        }
        return new ModelAndView(redirect.toString());
    }

    private ModelAndView redirectToReportReview(
            final Long reviewId,
            final String errorCode,
            final String status,
            final RedirectAttributes redirectAttributes) {
        if (status != null) {
            redirectAttributes.addFlashAttribute("reportSent", true);
            return new ModelAndView("redirect:/reports/reviews/" + reviewId);
        }
        return redirectToReportReview(reviewId, errorCode, null);
    }

    private ModelAndView redirectToReportMatch(
            final Long matchId, final String errorCode, final String status) {
        final StringBuilder redirect =
                new StringBuilder("redirect:/reports/matches/").append(matchId);
        if (errorCode != null) {
            redirect.append("?reportError=").append(errorCode);
        } else if (status != null) {
            redirect.append("?report=").append(status);
        }
        return new ModelAndView(redirect.toString());
    }

    private ModelAndView redirectToReportMatch(
            final Long matchId,
            final String errorCode,
            final String status,
            final RedirectAttributes redirectAttributes) {
        if (status != null) {
            redirectAttributes.addFlashAttribute("reportSent", true);
            return new ModelAndView("redirect:/reports/matches/" + matchId);
        }
        return redirectToReportMatch(matchId, errorCode, null);
    }
}
