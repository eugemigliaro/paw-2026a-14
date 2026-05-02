package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewReaction;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.exceptions.PlayerReviewException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.ImageUrlHelper;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.PlayerReviewViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.PublicProfilePageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
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
public class PublicProfileController {

    private static final int RECENT_REVIEW_LIMIT = 10;

    private final UserService userService;
    private final PlayerReviewService playerReviewService;
    private final MessageSource messageSource;

    public PublicProfileController(
            final UserService userService,
            final PlayerReviewService playerReviewService,
            final MessageSource messageSource) {
        this.userService = userService;
        this.playerReviewService = playerReviewService;
        this.messageSource = messageSource;
    }

    @GetMapping("/users/{username}")
    public ModelAndView showPublicProfile(
            @PathVariable("username") final String username,
            @RequestParam(value = "reviewForm", required = false) final String reviewForm,
            final Locale locale) {
        final User user =
                userService
                        .findByUsername(username)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        final ModelAndView mav = new ModelAndView("users/profile");
        mav.addObject(
                "pageTitle",
                messageSource.getMessage(
                        "page.title.publicProfile",
                        new Object[] {user.getUsername()},
                        "Match Point | " + user.getUsername(),
                        locale));
        mav.addObject("shell", ShellViewModelFactory.playerShell(messageSource, locale));
        mav.addObject(
                "profilePage",
                new PublicProfilePageViewModel(
                        user.getUsername(),
                        user.getName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getPhone(),
                        ImageUrlHelper.profileUrlFor(user)));
        addReviewModel(mav, user, reviewForm, locale);
        mav.addObject(
                "profileImageAlt",
                messageSource.getMessage(
                        "profile.public.avatarAlt",
                        new Object[] {user.getUsername()},
                        user.getUsername() + " profile picture",
                        locale));
        mav.addObject(
                "profileUsernameLabel",
                messageSource.getMessage("profile.public.username", null, "Username", locale));
        mav.addObject(
                "profileFullNameLabel",
                messageSource.getMessage("profile.public.fullName", null, "Name", locale));
        mav.addObject(
                "profileEmailLabel",
                messageSource.getMessage("profile.public.email", null, "Email", locale));
        mav.addObject(
                "profilePhoneLabel",
                messageSource.getMessage("profile.public.phone", null, "Phone", locale));
        CurrentAuthenticatedUser.get()
                .filter(principal -> principal.getUserId().equals(user.getId()))
                .ifPresent(
                        principal -> {
                            mav.addObject("profileEditHref", "/account");
                            mav.addObject(
                                    "profileEditLabel",
                                    messageSource.getMessage(
                                            "profile.public.edit", null, "Edit profile", locale));
                        });
        return mav;
    }

    @PostMapping("/users/{username}/reviews")
    public ModelAndView submitReview(
            @PathVariable("username") final String username,
            @RequestParam("reaction") final String reactionValue,
            @RequestParam(value = "comment", required = false) final String comment) {
        final User reviewedUser = findUserByUsernameOrThrow(username);
        final AuthenticatedUserPrincipal currentUser = requireAuthenticatedUser();
        final Optional<PlayerReviewReaction> reaction =
                PlayerReviewReaction.fromDbValue(reactionValue);
        if (reaction.isEmpty()) {
            return redirectToProfile(username, "invalid_reaction", null);
        }

        try {
            playerReviewService.submitReview(
                    currentUser.getUserId(), reviewedUser.getId(), reaction.get(), comment);
            return redirectToProfile(username, null, "saved");
        } catch (final PlayerReviewException e) {
            return redirectToProfile(username, e.getCode(), null);
        }
    }

    @PostMapping("/users/{username}/reviews/delete")
    public ModelAndView deleteReview(@PathVariable("username") final String username) {
        final User reviewedUser = findUserByUsernameOrThrow(username);
        final AuthenticatedUserPrincipal currentUser = requireAuthenticatedUser();

        try {
            playerReviewService.deleteReview(currentUser.getUserId(), reviewedUser.getId());
            return redirectToProfile(username, null, "deleted");
        } catch (final PlayerReviewException e) {
            return redirectToProfile(username, e.getCode(), null);
        }
    }

    private void addReviewModel(
            final ModelAndView mav, final User user, final String reviewForm, final Locale locale) {
        final PlayerReviewSummary summary = playerReviewService.findSummaryForUser(user.getId());
        final List<PlayerReviewViewModel> reviews =
                playerReviewService
                        .findRecentReviewsForUser(user.getId(), RECENT_REVIEW_LIMIT, 0)
                        .stream()
                        .map(review -> toReviewViewModel(review, locale))
                        .toList();
        final Long currentUserId =
                CurrentAuthenticatedUser.get()
                        .map(AuthenticatedUserPrincipal::getUserId)
                        .orElse(null);
        final Optional<PlayerReview> viewerReview =
                currentUserId == null
                        ? Optional.empty()
                        : playerReviewService.findReviewByPair(currentUserId, user.getId());
        final boolean reviewCanSubmit =
                currentUserId != null
                        && !currentUserId.equals(user.getId())
                        && playerReviewService.canReview(currentUserId, user.getId());
        final String profilePath = "/users/" + user.getUsername();

        mav.addObject("reviewSummary", summary);
        mav.addObject(
                "reviewLikeLabel",
                reviewCountLabel(
                        summary.getLikeCount(),
                        "profile.reviews.like",
                        "profile.reviews.likes",
                        locale));
        mav.addObject(
                "reviewDislikeLabel",
                reviewCountLabel(
                        summary.getDislikeCount(),
                        "profile.reviews.dislike",
                        "profile.reviews.dislikes",
                        locale));
        mav.addObject("profileReviews", reviews);
        mav.addObject("reviewCanSubmit", reviewCanSubmit);
        mav.addObject("reviewFormVisible", reviewCanSubmit && "open".equals(reviewForm));
        mav.addObject("viewerReview", viewerReview.orElse(null));
        mav.addObject("reviewActionPath", profilePath + "/reviews");
        mav.addObject("reviewDeletePath", profilePath + "/reviews/delete");
        mav.addObject("reviewFormPath", profilePath + "?reviewForm=open#reviews");
        mav.addObject("reviewSectionPath", profilePath + "#reviews");
    }

    private PlayerReviewViewModel toReviewViewModel(
            final PlayerReview review, final Locale locale) {
        final User reviewer =
                userService
                        .findById(review.getReviewerUserId())
                        .orElse(
                                new User(
                                        review.getReviewerUserId(),
                                        null,
                                        messageSource.getMessage(
                                                "profile.reviews.unknownReviewer",
                                                null,
                                                "Unknown player",
                                                locale)));
        return new PlayerReviewViewModel(
                reviewer.getUsername(),
                reviewer.getUsername() == null ? null : "/users/" + reviewer.getUsername(),
                review.getReaction().getDbValue(),
                reactionLabel(review.getReaction(), locale),
                review.getComment(),
                updatedAtLabel(review, locale));
    }

    private String reactionLabel(final PlayerReviewReaction reaction, final Locale locale) {
        return messageSource.getMessage(
                "profile.reviews.reaction." + reaction.getDbValue(),
                null,
                reaction.getDbValue(),
                locale);
    }

    private String reviewCountLabel(
            final long count,
            final String singularCode,
            final String pluralCode,
            final Locale locale) {
        return messageSource.getMessage(count == 1L ? singularCode : pluralCode, null, locale);
    }

    private static String updatedAtLabel(final PlayerReview review, final Locale locale) {
        if (review.getUpdatedAt() == null) {
            return "";
        }
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale == null ? Locale.ENGLISH : locale)
                .format(review.getUpdatedAt().atZone(ZoneId.systemDefault()));
    }

    private User findUserByUsernameOrThrow(final String username) {
        return userService
                .findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static AuthenticatedUserPrincipal requireAuthenticatedUser() {
        return CurrentAuthenticatedUser.get()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private static ModelAndView redirectToProfile(
            final String username, final String errorCode, final String status) {
        final StringBuilder redirect = new StringBuilder("redirect:/users/").append(username);
        if (errorCode != null) {
            redirect.append("?reviewError=").append(errorCode);
        } else if (status != null) {
            redirect.append("?review=").append(status);
        }
        redirect.append("#reviews");
        return new ModelAndView(redirect.toString());
    }
}
