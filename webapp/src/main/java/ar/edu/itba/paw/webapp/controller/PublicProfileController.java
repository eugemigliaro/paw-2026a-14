package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.UserSportRating;
import ar.edu.itba.paw.models.query.PlayerReviewFilter;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.PlatformTimeZoneService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.UserSportRatingService;
import ar.edu.itba.paw.services.exceptions.playerReview.PlayerReviewInvalidReactionException;
import ar.edu.itba.paw.services.exceptions.playerReview.PlayerReviewNotEligibleException;
import ar.edu.itba.paw.services.exceptions.playerReview.PlayerReviewNotFoundException;
import ar.edu.itba.paw.services.exceptions.playerReview.PlayerReviewSelfReviewException;
import ar.edu.itba.paw.services.exceptions.playerReview.PlayerReviewUserNotFoundException;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import ar.edu.itba.paw.webapp.security.annotation.CurrentUser;
import ar.edu.itba.paw.webapp.utils.ImageUrlHelper;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FilterOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PaginationItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PlayerReviewViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PublicProfilePageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.SportRatingViewModel;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class PublicProfileController {

    private static final int REVIEW_PAGE_SIZE = 10;

    private final UserService userService;
    private final PlayerReviewService playerReviewService;
    private final ModerationService moderationService;
    private final UserSportRatingService userSportRatingService;
    private final MessageSource messageSource;
    private final PlatformTimeZoneService platformTimeZoneService;

    @Autowired
    public PublicProfileController(
            final UserService userService,
            final PlayerReviewService playerReviewService,
            final ModerationService moderationService,
            final UserSportRatingService userSportRatingService,
            final MessageSource messageSource,
            final PlatformTimeZoneService platformTimeZoneService) {
        this.userService = userService;
        this.playerReviewService = playerReviewService;
        this.moderationService = moderationService;
        this.userSportRatingService = userSportRatingService;
        this.messageSource = messageSource;
        this.platformTimeZoneService = platformTimeZoneService;
    }

    @GetMapping("/users/{username}")
    public ModelAndView showPublicProfile(
            @CurrentUser final User user,
            @PathVariable("username") final String username,
            @RequestParam(value = "reviewForm", required = false) final String reviewForm,
            @RequestParam(value = "reviewFilter", required = false, defaultValue = "both")
                    final PlayerReviewFilter reviewFilter,
            @RequestParam(value = "reviewPage", defaultValue = "1") final int reviewPage,
            final Model model,
            final Locale locale) {
        final Locale resolvedLocale = locale == null ? Locale.ENGLISH : locale;
        final User targetUser = findUserByUsernameOrThrow(username);

        final ModelAndView mav = new ModelAndView("users/profile");
        mav.addObject("reviewStatus", model.asMap().get("reviewStatus"));
        mav.addObject("reportStatus", model.asMap().get("reportStatus"));
        mav.addObject(
                "pageTitle",
                messageSource.getMessage(
                        "page.title.publicProfile",
                        new Object[] {targetUser.getUsername()},
                        "Match Point | " + targetUser.getUsername(),
                        resolvedLocale));
        mav.addObject(
                "profilePage",
                new PublicProfilePageViewModel(
                        targetUser.getUsername(),
                        targetUser.getName(),
                        targetUser.getLastName(),
                        targetUser.getEmail(),
                        targetUser.getPhone(),
                        ImageUrlHelper.profileUrlFor(targetUser)));
        addReviewModel(mav, targetUser, user, reviewForm, reviewFilter, reviewPage, resolvedLocale);
        mav.addObject(
                "profileImageAlt",
                messageSource.getMessage(
                        "profile.public.avatarAlt",
                        new Object[] {targetUser.getUsername()},
                        targetUser.getUsername() + " profile picture",
                        resolvedLocale));
        mav.addObject(
                "profileUsernameLabel",
                messageSource.getMessage(
                        "profile.public.username", null, "Username", resolvedLocale));
        mav.addObject(
                "profileFullNameLabel",
                messageSource.getMessage("profile.public.fullName", null, "Name", resolvedLocale));
        mav.addObject(
                "profileEmailLabel",
                messageSource.getMessage("profile.public.email", null, "Email", resolvedLocale));
        mav.addObject(
                "profilePhoneLabel",
                messageSource.getMessage("profile.public.phone", null, "Phone", resolvedLocale));
        final boolean reportUserCanSubmit =
                user != null && !user.getId().equals(targetUser.getId());
        mav.addObject("reportUserCanSubmit", reportUserCanSubmit);
        final Optional<UserBan> activeBan = moderationService.findActiveBan(targetUser);
        mav.addObject("profileBanned", activeBan.isPresent());
        mav.addObject(
                "profileBannedLabel",
                messageSource.getMessage(
                        "profile.public.banned", null, "Temporarily banned", resolvedLocale));
        activeBan.ifPresent(
                ban -> {
                    mav.addObject(
                            "profileBannedUntil",
                            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                    .withLocale(resolvedLocale)
                                    .format(
                                            ban.getBannedUntil()
                                                    .atZone(
                                                            platformTimeZoneService
                                                                    .defaultZone())));
                });
        if (user != null && user.getId().equals(targetUser.getId())) {
            mav.addObject("profileEditHref", "/account");
            mav.addObject(
                    "profileEditLabel",
                    messageSource.getMessage(
                            "profile.public.edit", null, "Edit profile", resolvedLocale));
        }
        addRatingsModel(mav, targetUser, resolvedLocale);
        return mav;
    }

    @PostMapping("/users/{username}/reviews")
    public ModelAndView submitReview(
            @AuthenticatedUser final User user,
            @PathVariable("username") final String username,
            @RequestParam(value = "reaction", required = true) final PlayerReviewReaction reaction,
            @RequestParam(value = "comment", required = false) final String comment,
            final RedirectAttributes redirectAttributes) {
        final User reviewedUser = findUserByUsernameOrThrow(username);
        String errorCode = null;
        try {
            playerReviewService.submitReview(user, reviewedUser, reaction, comment);
            return redirectToProfile(username, null, "saved", redirectAttributes);
        } catch (
                final PlayerReviewUserNotFoundException
                        e) { // TODO: move message code to service (?) and catch generic exception
            // here
            errorCode = "user_not_found";
        } catch (final PlayerReviewInvalidReactionException e) {
            errorCode = "invalid_reaction";
        } catch (final PlayerReviewSelfReviewException e) {
            errorCode = "self_review";
        } catch (final PlayerReviewNotEligibleException e) {
            errorCode = "not_eligible";
        }
        return redirectToProfile(username, errorCode, null);
    }

    @PostMapping("/users/{username}/reviews/delete")
    public ModelAndView deleteReview(
            @AuthenticatedUser final User user,
            @PathVariable("username") final String username,
            final RedirectAttributes redirectAttributes) {
        final User reviewedUser = findUserByUsernameOrThrow(username);

        String errorCode = null;
        try {
            playerReviewService.deleteReview(user, reviewedUser);
            return redirectToProfile(username, null, "deleted", redirectAttributes);
        } catch (
                final PlayerReviewNotFoundException
                        e) { // TODO: move message code to service (?) and catch generic exception
            // here
            errorCode = "not_found";
        } catch (final PlayerReviewUserNotFoundException e) {
            errorCode = "user_not_found";
        }
        return redirectToProfile(username, errorCode, null);
    }

    private void addRatingsModel(final ModelAndView mav, final User user, final Locale locale) {
        final List<SportRatingViewModel> ratings =
                userSportRatingService.findRatingsForUser(user).stream()
                        .sorted(Comparator.comparingInt(UserSportRating::getElo).reversed())
                        .map(
                                r ->
                                        new SportRatingViewModel(
                                                messageSource.getMessage(
                                                        "sport." + r.getSport().getDbValue(),
                                                        null,
                                                        r.getSport().getDisplayName(),
                                                        locale),
                                                r.getElo()))
                        .toList();
        mav.addObject("profileRatings", ratings);
        mav.addObject(
                "profileRatingsTitle",
                messageSource.getMessage("profile.ratings.title", null, "Sport ratings", locale));
        mav.addObject(
                "profileRatingsEmpty",
                messageSource.getMessage(
                        "profile.ratings.empty", null, "No sport ratings yet.", locale));
    }

    private void addReviewModel(
            final ModelAndView mav,
            final User targetUser,
            final User currentUser,
            final String reviewForm,
            final PlayerReviewFilter reviewFilter,
            final int reviewPage,
            final Locale locale) {
        final PlayerReviewSummary summary = playerReviewService.findSummaryForUser(targetUser);
        final PaginatedResult<PlayerReview> reviewResult =
                playerReviewService.findReviewsForUser(
                        targetUser, reviewFilter, reviewPage, REVIEW_PAGE_SIZE);
        final List<PlayerReviewViewModel> reviews =
                reviewResult.getItems().stream()
                        .map(review -> toReviewViewModel(review, locale))
                        .toList();
        final Optional<PlayerReview> viewerReview =
                currentUser == null
                        ? Optional.empty()
                        : playerReviewService.findReviewByPair(currentUser, targetUser);
        final boolean reviewCanSubmit =
                currentUser != null
                        && !currentUser.getId().equals(targetUser.getId())
                        && playerReviewService.canReview(currentUser, targetUser);
        final String profilePath = "/users/" + targetUser.getUsername();

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
        mav.addObject("reviewFilterOptions", reviewFilterOptions(targetUser, reviewFilter, locale));
        mav.addObject("selectedReviewFilter", reviewFilter.getQueryValue());
        mav.addObject("reviewTotalPages", reviewResult.getTotalPages());
        mav.addObject(
                "reviewPaginationItems",
                buildReviewPaginationItems(targetUser, reviewFilter, reviewResult));
        mav.addObject(
                "reviewPreviousPageHref",
                reviewResult.hasPrevious()
                        ? buildReviewPageUrl(
                                targetUser, reviewFilter, reviewResult.getPage() - 1, null)
                        : null);
        mav.addObject(
                "reviewNextPageHref",
                reviewResult.hasNext()
                        ? buildReviewPageUrl(
                                targetUser, reviewFilter, reviewResult.getPage() + 1, null)
                        : null);
        mav.addObject("reviewCanSubmit", reviewCanSubmit);
        mav.addObject("reviewFormVisible", reviewCanSubmit && "open".equals(reviewForm));
        mav.addObject("viewerReview", viewerReview.orElse(null));
        mav.addObject("reviewActionPath", profilePath + "/reviews");
        mav.addObject("reviewDeletePath", profilePath + "/reviews/delete");
        mav.addObject(
                "reviewFormPath",
                buildReviewPageUrl(targetUser, reviewFilter, reviewResult.getPage(), "open"));
        mav.addObject(
                "reviewSectionPath",
                buildReviewPageUrl(targetUser, reviewFilter, reviewResult.getPage(), null));
        if (reviewCanSubmit) {
            mav.addObject(
                    "reviewCommentPromptLabel",
                    messageSource.getMessage(
                            "profile.reviews.commentPrompt",
                            new Object[] {targetUser.getUsername()},
                            locale));
        }
        final boolean isSelf =
                currentUser != null && currentUser.getId().equals(targetUser.getId());
        if (!reviewCanSubmit && !isSelf) {
            final String lockedKey =
                    currentUser == null
                            ? "profile.reviews.locked.anonymous"
                            : "profile.reviews.locked.authenticated";
            final Object[] lockedArgs =
                    currentUser == null ? null : new Object[] {targetUser.getUsername()};
            mav.addObject(
                    "reviewLockedMessage", messageSource.getMessage(lockedKey, lockedArgs, locale));
        }
    }

    private List<FilterOptionViewModel> reviewFilterOptions(
            final User user, final PlayerReviewFilter selectedFilter, final Locale locale) {
        return List.of(
                reviewFilterOption(user, PlayerReviewFilter.BOTH, selectedFilter, locale),
                reviewFilterOption(user, PlayerReviewFilter.POSITIVE, selectedFilter, locale),
                reviewFilterOption(user, PlayerReviewFilter.BAD, selectedFilter, locale));
    }

    private FilterOptionViewModel reviewFilterOption(
            final User user,
            final PlayerReviewFilter filter,
            final PlayerReviewFilter selectedFilter,
            final Locale locale) {
        final String label =
                messageSource.getMessage(
                        "profile.reviews.filter." + filter.getQueryValue(), null, locale);
        return new FilterOptionViewModel(
                label,
                buildReviewPageUrl(user, filter, 1, null),
                null,
                null,
                filter == selectedFilter);
    }

    private static List<PaginationItemViewModel> buildReviewPaginationItems(
            final User user,
            final PlayerReviewFilter selectedFilter,
            final PaginatedResult<PlayerReview> result) {
        if (result.getTotalPages() <= 1) {
            return List.of();
        }

        final List<PaginationItemViewModel> items = new ArrayList<>();
        final int startPage =
                Math.max(2, Math.min(result.getPage() - 1, result.getTotalPages() - 3));
        final int endPage = Math.min(result.getTotalPages() - 1, Math.max(result.getPage() + 1, 4));

        items.add(reviewPageItem(user, selectedFilter, 1, result.getPage()));

        if (startPage > 2) {
            items.add(new PaginationItemViewModel("...", null, false, true));
        }

        for (int page = startPage; page <= endPage; page++) {
            items.add(reviewPageItem(user, selectedFilter, page, result.getPage()));
        }

        if (endPage < result.getTotalPages() - 1) {
            items.add(new PaginationItemViewModel("...", null, false, true));
        }

        items.add(reviewPageItem(user, selectedFilter, result.getTotalPages(), result.getPage()));

        return items;
    }

    private static PaginationItemViewModel reviewPageItem(
            final User user,
            final PlayerReviewFilter selectedFilter,
            final int page,
            final int currentPage) {
        return new PaginationItemViewModel(
                Integer.toString(page),
                buildReviewPageUrl(user, selectedFilter, page, null),
                page == currentPage,
                false);
    }

    private static String buildReviewPageUrl(
            final User user,
            final PlayerReviewFilter selectedFilter,
            final int page,
            final String reviewForm) {
        final UriComponentsBuilder builder =
                UriComponentsBuilder.fromPath("/users/" + user.getUsername())
                        .queryParam("reviewFilter", selectedFilter.getQueryValue())
                        .queryParam("reviewPage", page);
        if (reviewForm != null && !reviewForm.isBlank()) {
            builder.queryParam("reviewForm", reviewForm);
        }
        return builder.fragment("reviews").build().toUriString();
    }

    private PlayerReviewViewModel toReviewViewModel(
            final PlayerReview review, final Locale locale) {
        final User reviewer = review.getReviewer();
        final String reviewerUsername =
                reviewer == null || reviewer.getUsername() == null
                        ? messageSource.getMessage(
                                "profile.reviews.unknownReviewer", null, "Unknown player", locale)
                        : reviewer.getUsername();
        final String reviewerProfileHref =
                reviewer == null
                                || reviewer.getUsername() == null
                                || reviewer.getUsername().isBlank()
                        ? null
                        : "/users/" + reviewer.getUsername();
        return new PlayerReviewViewModel(
                review.getId(),
                reviewerUsername,
                reviewerProfileHref,
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

    private String updatedAtLabel(final PlayerReview review, final Locale locale) {
        if (review.getUpdatedAt() == null) {
            return "";
        }
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale == null ? Locale.ENGLISH : locale)
                .format(review.getUpdatedAt().atZone(platformTimeZoneService.defaultZone()));
    }

    private User findUserByUsernameOrThrow(final String username) {
        return userService
                .findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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

    private static ModelAndView redirectToProfile(
            final String username,
            final String errorCode,
            final String status,
            final RedirectAttributes redirectAttributes) {
        if (status != null) {
            redirectAttributes.addFlashAttribute("reviewStatus", status);
            return new ModelAndView("redirect:/users/" + username + "#reviews");
        }
        return redirectToProfile(username, errorCode, null);
    }
}
