package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.UserSportRating;
import ar.edu.itba.paw.models.exceptions.playerReview.PlayerReviewException;
import ar.edu.itba.paw.models.query.PlayerReviewFilter;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.UserSportRatingService;
import ar.edu.itba.paw.webapp.form.ReviewForm;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import ar.edu.itba.paw.webapp.security.annotation.CurrentUser;
import ar.edu.itba.paw.webapp.utils.ImageUrlHelper;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FilterOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PaginationItemViewModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

    @Autowired
    public PublicProfileController(
            final UserService userService,
            final PlayerReviewService playerReviewService,
            final ModerationService moderationService,
            final UserSportRatingService userSportRatingService) {
        this.userService = userService;
        this.playerReviewService = playerReviewService;
        this.moderationService = moderationService;
        this.userSportRatingService = userSportRatingService;
    }

    @ModelAttribute("playerReviewForm")
    public ReviewForm playerReviewForm() {
        return new ReviewForm();
    }

    @GetMapping("/users/{username}")
    public ModelAndView showPublicProfile(
            @CurrentUser final User user,
            @PathVariable("username") final String username,
            @RequestParam(value = "reviewForm", required = false) final String reviewForm,
            @RequestParam(value = "reviewFilter", required = false, defaultValue = "both")
                    final PlayerReviewFilter reviewFilter,
            @RequestParam(value = "reviewPage", defaultValue = "1") final int reviewPage,
            final Model model) {
        final User targetUser = findUserByUsernameOrThrow(username);

        final ModelAndView mav = new ModelAndView("users/profile");
        mav.addObject("reviewStatus", model.asMap().get("reviewStatus"));
        mav.addObject("reportStatus", model.asMap().get("reportStatus"));
        mav.addObject("targetUser", targetUser);
        mav.addObject("profileImageUrl", ImageUrlHelper.profileUrlFor(targetUser));
        addReviewModel(mav, targetUser, user, reviewForm, reviewFilter, reviewPage);
        final boolean reportUserCanSubmit =
                user != null && !user.getId().equals(targetUser.getId());
        mav.addObject("reportUserCanSubmit", reportUserCanSubmit);
        final Optional<UserBan> activeBan = moderationService.findActiveBan(targetUser);
        mav.addObject("profileBanned", activeBan.isPresent());
        activeBan.ifPresent(
                ban -> {
                    mav.addObject("profileBannedUntilDateTime", ban.getBannedUntilDateTime());
                });
        if (user != null && user.getId().equals(targetUser.getId())) {
            mav.addObject("profileEditHref", "/account");
        }
        addRatingsModel(mav, targetUser);
        return mav;
    }

    @PostMapping("/users/{username}/reviews")
    public ModelAndView submitReview(
            @AuthenticatedUser final User user,
            @PathVariable("username") final String username,
            @Valid @ModelAttribute("playerReviewForm") final ReviewForm reviewForm,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return redirectToProfile(username, "invalid_form", null);
        }

        final User reviewedUser = findUserByUsernameOrThrow(username);
        try {
            playerReviewService.submitReview(
                    user, reviewedUser, reviewForm.getReaction(), reviewForm.getComment());
            return redirectToProfile(username, null, "saved", redirectAttributes);
        } catch (final PlayerReviewException e) {
            final String errorCode = e.getMessage();
            return redirectToProfile(username, errorCode, null);
        }
    }

    @PostMapping("/users/{username}/reviews/delete")
    public ModelAndView deleteReview(
            @AuthenticatedUser final User user,
            @PathVariable("username") final String username,
            final RedirectAttributes redirectAttributes) {
        final User reviewedUser = findUserByUsernameOrThrow(username);

        try {
            playerReviewService.deleteReview(user, reviewedUser);
            return redirectToProfile(username, null, "deleted", redirectAttributes);
        } catch (final PlayerReviewException e) {
            final String errorCode = e.getMessage();
            return redirectToProfile(username, errorCode, null);
        }
    }

    private void addRatingsModel(final ModelAndView mav, final User user) {
        final List<UserSportRating> ratings =
                userSportRatingService.findRatingsForUser(user).stream()
                        .sorted(Comparator.comparingInt(UserSportRating::getElo).reversed())
                        .toList();
        mav.addObject("profileRatings", ratings);
    }

    private void addReviewModel(
            final ModelAndView mav,
            final User targetUser,
            final User currentUser,
            final String reviewForm,
            final PlayerReviewFilter reviewFilter,
            final int reviewPage) {
        final PlayerReviewSummary summary = playerReviewService.findSummaryForUser(targetUser);
        final PaginatedResult<PlayerReview> reviewResult =
                playerReviewService.findReviewsForUser(
                        targetUser, reviewFilter, reviewPage, REVIEW_PAGE_SIZE);
        final Optional<PlayerReview> viewerReview =
                currentUser == null
                        ? Optional.empty()
                        : playerReviewService.findReviewByPair(currentUser, targetUser);
        final boolean reviewCanSubmit =
                currentUser != null
                        && targetUser != null
                        && !currentUser.getId().equals(targetUser.getId())
                        && playerReviewService.canReview(currentUser, targetUser);
        final String profilePath = "/users/" + targetUser.getUsername();

        mav.addObject("reviewSummary", summary);
        mav.addObject("profileReviews", reviewResult.getItems());
        mav.addObject("reviewFilterOptions", reviewFilterOptions(targetUser, reviewFilter));
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
            mav.addObject("reviewCommentPromptCode", "profile.reviews.commentPrompt");
        }
        final boolean isSelf =
                currentUser != null
                        && targetUser != null
                        && currentUser.getId().equals(targetUser.getId());
        if (!reviewCanSubmit && !isSelf) {
            final String lockedKey =
                    currentUser == null
                            ? "profile.reviews.locked.anonymous"
                            : "profile.reviews.locked.authenticated";
            mav.addObject("reviewLockedCode", lockedKey);
        }
    }

    private List<FilterOptionViewModel> reviewFilterOptions(
            final User user, final PlayerReviewFilter selectedFilter) {
        return List.of(
                reviewFilterOption(user, PlayerReviewFilter.BOTH, selectedFilter),
                reviewFilterOption(user, PlayerReviewFilter.POSITIVE, selectedFilter),
                reviewFilterOption(user, PlayerReviewFilter.BAD, selectedFilter));
    }

    private FilterOptionViewModel reviewFilterOption(
            final User user,
            final PlayerReviewFilter filter,
            final PlayerReviewFilter selectedFilter) {
        return new FilterOptionViewModel(
                "profile.reviews.filter." + filter.getQueryValue(),
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
