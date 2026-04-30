package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.profileUrlFor;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.ReportReason;
import ar.edu.itba.paw.models.ReportTargetType;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.exceptions.ModerationException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ReportMatchViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ReportPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ReportReviewViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ReportUserViewModel;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ModerationReportController {

    private final ModerationService moderationService;
    private final UserService userService;
    private final MatchService matchService;
    private final PlayerReviewService playerReviewService;
    private final MessageSource messageSource;

    public ModerationReportController(
            final ModerationService moderationService,
            final UserService userService,
            final MatchService matchService,
            final PlayerReviewService playerReviewService,
            final MessageSource messageSource) {
        this.moderationService = moderationService;
        this.userService = userService;
        this.matchService = matchService;
        this.playerReviewService = playerReviewService;
        this.messageSource = messageSource;
    }

    @GetMapping("/reports/users/{username}")
    public ModelAndView showUserReportPage(
            @PathVariable("username") final String username,
            @RequestParam(value = "report", required = false) final String reportStatus,
            @RequestParam(value = "reportError", required = false) final String reportErrorCode,
            final Locale locale) {
        requireAuthenticatedUser();
        final User reportedUser = findUserByUsernameOrThrow(username);
        return baseReportView(
                locale,
                reportStatus,
                reportErrorCode,
                messageOrDefault(
                        "page.title.reportUser", null, "Match Point | Report user", locale),
                messageOrDefault(
                        "report.page.user.description",
                        new Object[] {reportedUser.getUsername()},
                        "You are reporting the user " + reportedUser.getUsername() + ".",
                        locale),
                messageOrDefault("report.page.user.title", null, "Report user", locale),
                "/reports/users/" + reportedUser.getUsername(),
                new ReportPageViewModel(
                        "user",
                        new ReportUserViewModel(
                                reportedUser.getUsername(),
                                profileUrlFor(reportedUser),
                                messageOrDefault(
                                        "report.page.user.avatarAlt",
                                        new Object[] {reportedUser.getUsername()},
                                        reportedUser.getUsername() + " profile picture",
                                        locale)),
                        null,
                        null));
    }

    @GetMapping("/reports/reviews/{reviewId}")
    public ModelAndView showReviewReportPage(
            @PathVariable("reviewId") final Long reviewId,
            @RequestParam(value = "report", required = false) final String reportStatus,
            @RequestParam(value = "reportError", required = false) final String reportErrorCode,
            final Locale locale) {
        requireAuthenticatedUser();
        final PlayerReview review = findReviewByIdOrThrow(reviewId);
        final User author = findUserByIdOrThrow(review.getReviewerUserId());
        final User reviewedUser = findUserByIdOrThrow(review.getReviewedUserId());
        return baseReportView(
                locale,
                reportStatus,
                reportErrorCode,
                messageOrDefault(
                        "page.title.reportReview", null, "Match Point | Report review", locale),
                messageOrDefault(
                        "report.page.review.description",
                        new Object[] {author.getUsername(), reviewedUser.getUsername()},
                        "You are reporting the review written by "
                                + author.getUsername()
                                + " about "
                                + reviewedUser.getUsername()
                                + ".",
                        locale),
                messageOrDefault("report.page.review.title", null, "Report review", locale),
                "/reports/reviews/" + review.getId(),
                new ReportPageViewModel(
                        "review",
                        null,
                        new ReportReviewViewModel(
                                author.getUsername(),
                                author.getUsername() == null
                                        ? null
                                        : "/users/" + author.getUsername(),
                                reviewedUser.getUsername(),
                                reviewedUser.getUsername() == null
                                        ? null
                                        : "/users/" + reviewedUser.getUsername(),
                                review.getComment(),
                                formatInstant(
                                        review.getUpdatedAt() == null
                                                ? review.getCreatedAt()
                                                : review.getUpdatedAt(),
                                        locale)),
                        null));
    }

    @GetMapping("/reports/matches/{matchId}")
    public ModelAndView showMatchReportPage(
            @PathVariable("matchId") final Long matchId,
            @RequestParam(value = "report", required = false) final String reportStatus,
            @RequestParam(value = "reportError", required = false) final String reportErrorCode,
            final Locale locale) {
        requireAuthenticatedUser();
        final Match match = findMatchByIdOrThrow(matchId);
        final User host = findUserByIdOrThrow(match.getHostUserId());
        return baseReportView(
                locale,
                reportStatus,
                reportErrorCode,
                messageOrDefault(
                        "page.title.reportMatch", null, "Match Point | Report match", locale),
                messageOrDefault(
                        "report.page.match.descriptionText",
                        new Object[] {match.getTitle()},
                        "You are reporting the match " + match.getTitle() + ".",
                        locale),
                messageOrDefault("report.page.match.title", null, "Report match", locale),
                "/reports/matches/" + match.getId(),
                new ReportPageViewModel(
                        "match",
                        null,
                        null,
                        new ReportMatchViewModel(
                                match.getTitle(),
                                normalizedDescription(match.getDescription()),
                                host.getUsername(),
                                host.getUsername() == null ? null : "/users/" + host.getUsername(),
                                formatInstant(match.getStartsAt(), locale),
                                match.getAddress(),
                                priceLabel(match.getPricePerPlayer(), locale))));
    }

    @PostMapping("/reports/users/{username}")
    public ModelAndView reportUser(
            @PathVariable("username") final String username,
            @RequestParam("reason") final String reason,
            @RequestParam(value = "details", required = false) final String details) {
        final User reportedUser = findUserByUsernameOrThrow(username);
        final AuthenticatedUserPrincipal currentUser = requireAuthenticatedUser();
        final Optional<ReportReason> parsedReason = ReportReason.fromDbValue(reason);
        if (parsedReason.isEmpty() || currentUser.getUserId().equals(reportedUser.getId())) {
            return redirectToReportUser(username, "invalid_report", null);
        }

        try {
            moderationService.reportContent(
                    currentUser.getUserId(),
                    ReportTargetType.USER,
                    reportedUser.getId(),
                    parsedReason.get(),
                    details);
            return redirectToReportUser(username, null, "sent");
        } catch (final ModerationException exception) {
            return redirectToReportUser(username, exception.getCode(), null);
        }
    }

    @PostMapping("/reports/reviews/{reviewId}")
    public ModelAndView reportReview(
            @PathVariable("reviewId") final Long reviewId,
            @RequestParam("reason") final String reason,
            @RequestParam(value = "details", required = false) final String details) {
        final PlayerReview review = findReviewByIdOrThrow(reviewId);
        final AuthenticatedUserPrincipal currentUser = requireAuthenticatedUser();
        final Optional<ReportReason> parsedReason = ReportReason.fromDbValue(reason);
        if (parsedReason.isEmpty()) {
            return redirectToReportReview(review.getId(), "invalid_report", null);
        }

        try {
            moderationService.reportContent(
                    currentUser.getUserId(),
                    ReportTargetType.REVIEW,
                    review.getId(),
                    parsedReason.get(),
                    details);
            return redirectToReportReview(review.getId(), null, "sent");
        } catch (final ModerationException exception) {
            return redirectToReportReview(review.getId(), exception.getCode(), null);
        }
    }

    @PostMapping("/reports/matches/{matchId}")
    public ModelAndView reportMatch(
            @PathVariable("matchId") final Long matchId,
            @RequestParam("reason") final String reason,
            @RequestParam(value = "details", required = false) final String details) {
        final Match match = findMatchByIdOrThrow(matchId);
        final AuthenticatedUserPrincipal currentUser = requireAuthenticatedUser();
        final ReportReason parsedReason = parseReason(reason);
        try {
            moderationService.reportContent(
                    currentUser.getUserId(),
                    ReportTargetType.MATCH,
                    match.getId(),
                    parsedReason,
                    details);
            return redirectToReportMatch(match.getId(), null, "sent");
        } catch (final ModerationException exception) {
            return redirectToReportMatch(match.getId(), exception.getCode(), null);
        }
    }

    private ModelAndView baseReportView(
            final Locale locale,
            final String reportStatus,
            final String reportErrorCode,
            final String pageTitle,
            final String pageDescription,
            final String pageTitleLabel,
            final String reportActionPath,
            final ReportPageViewModel reportPage) {
        final ModelAndView mav = new ModelAndView("reports/create");
        mav.addObject("shell", ShellViewModelFactory.playerShell(messageSource, locale));
        mav.addObject("pageTitle", pageTitle);
        mav.addObject("pageTitleLabel", pageTitleLabel);
        mav.addObject("pageDescription", pageDescription);
        mav.addObject("reportActionPath", reportActionPath);
        mav.addObject("reportPage", reportPage);
        mav.addObject("reportSent", "sent".equalsIgnoreCase(reportStatus));
        mav.addObject(
                "reportErrorMessage",
                reportErrorCode == null
                        ? null
                        : moderationReportErrorMessage(reportErrorCode, locale));
        return mav;
    }

    private String messageOrDefault(
            final String code,
            final Object[] args,
            final String defaultMessage,
            final Locale locale) {
        final String message = messageSource.getMessage(code, args, defaultMessage, locale);
        return message == null ? defaultMessage : message;
    }

    private User findUserByUsernameOrThrow(final String username) {
        return userService
                .findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User findUserByIdOrThrow(final Long userId) {
        return userService
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private PlayerReview findReviewByIdOrThrow(final Long reviewId) {
        return playerReviewService
                .findReviewByIdIncludingDeleted(reviewId)
                .filter(review -> !review.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Match findMatchByIdOrThrow(final Long matchId) {
        return matchService
                .findMatchById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static ReportReason parseReason(final String reason) {
        return ReportReason.fromDbValue(reason)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
    }

    private static AuthenticatedUserPrincipal requireAuthenticatedUser() {
        return CurrentAuthenticatedUser.get()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
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

    private String moderationReportErrorMessage(final String code, final Locale locale) {
        switch (code) {
            case "duplicate_report":
                return messageOrDefault(
                        "moderation.report.error.duplicate",
                        null,
                        "You already have an active report for this target.",
                        locale);
            case "report_limit":
                return messageOrDefault(
                        "moderation.report.error.limit",
                        null,
                        "You reached the active report limit.",
                        locale);
            case "invalid_report":
                return messageOrDefault(
                        "moderation.report.error.invalid",
                        null,
                        "This report request is invalid.",
                        locale);
            default:
                return messageOrDefault(
                        "moderation.report.error.generic",
                        null,
                        "We could not submit the report.",
                        locale);
        }
    }

    private static String formatInstant(final Instant instant, final Locale locale) {
        if (instant == null) {
            return "";
        }
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale == null ? Locale.ENGLISH : locale)
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    private static String normalizedDescription(final String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        return description
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private String priceLabel(final BigDecimal pricePerPlayer, final Locale locale) {
        if (pricePerPlayer == null) {
            return messageOrDefault("price.tbd", null, "Price TBD", locale);
        }
        return pricePerPlayer.compareTo(BigDecimal.ZERO) == 0
                ? messageOrDefault("price.free", null, "Free", locale)
                : messageOrDefault(
                        "price.amount",
                        new Object[] {pricePerPlayer},
                        pricePerPlayer.toString(),
                        locale);
    }
}
