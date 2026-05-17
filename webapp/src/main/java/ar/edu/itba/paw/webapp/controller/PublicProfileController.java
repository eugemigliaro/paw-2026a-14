package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.query.PlayerReviewFilter;
import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.exceptions.PlayerReviewException;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.ImageUrlHelper;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FilterOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PaginationItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PlayerReviewViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PublicProfilePageViewModel;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final MessageSource messageSource;

    public PublicProfileController(
            final UserService userService,
            final PlayerReviewService playerReviewService,
            final ModerationService moderationService,
            final MessageSource messageSource) {
        this.userService = userService;
        this.playerReviewService = playerReviewService;
        this.moderationService = moderationService;
        this.messageSource = messageSource;
    }

    @GetMapping("/users/{username}")
    public ModelAndView showPublicProfile(
            @PathVariable("username") final String username,
            @RequestParam(value = "reviewForm", required = false) final String reviewForm,
            @RequestParam(value = "reviewFilter", required = false) final String reviewFilter,
            @RequestParam(value = "reviewPage", defaultValue = "1") final String reviewPage,
            final Model model,
            final Locale locale) {
        final Locale resolvedLocale = locale == null ? Locale.ENGLISH : locale;
        final User user =
                userService
                        .findByUsername(username)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        final ModelAndView mav = new ModelAndView("users/profile");
        mav.addObject("reviewStatus", model.asMap().get("reviewStatus"));
        mav.addObject("reportStatus", model.asMap().get("reportStatus"));
        mav.addObject(
                "pageTitle",
                messageSource.getMessage(
                        "page.title.publicProfile",
                        new Object[] {user.getUsername()},
                        "Match Point | " + user.getUsername(),
                        resolvedLocale));
        mav.addObject("shell", ShellViewModelFactory.playerShell(messageSource, resolvedLocale));
        mav.addObject(
                "profilePage",
                new PublicProfilePageViewModel(
                        user.getUsername(),
                        user.getName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getPhone(),
                        ImageUrlHelper.profileUrlFor(user)));
        addReviewModel(
                mav, user, reviewForm, reviewFilter, parseReviewPage(reviewPage), resolvedLocale);
        mav.addObject(
                "profileImageAlt",
                messageSource.getMessage(
                        "profile.public.avatarAlt",
                        new Object[] {user.getUsername()},
                        user.getUsername() + " profile picture",
                        resolvedLocale));
        mav.addObject(
                "profileUsernameLabel",
                messageSource.getMessage(
                        "profile.public.username", null, "Username", resolvedLocale));
        mav.addObject(
                "profileFullNameLabel",
                messageSource.getMessage("profile.public.fullName", null, "Name", locale));
        mav.addObject(
                "profileEmailLabel",
                messageSource.getMessage("profile.public.email", null, "Email", locale));
        mav.addObject(
                "profilePhoneLabel",
                messageSource.getMessage("profile.public.phone", null, "Phone", resolvedLocale));
        final User currentUser = SecurityControllerUtils.currentUserOrNull();
        final boolean reportUserCanSubmit =
                currentUser != null && !currentUser.getId().equals(user.getId());
        mav.addObject("reportUserCanSubmit", reportUserCanSubmit);
        final Optional<UserBan> activeBan = moderationService.findActiveBan(user);
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
                                    .format(ban.getBannedUntil().atZone(ZoneId.systemDefault())));
                });
        CurrentAuthenticatedUser.get()
                .filter(principal -> principal.getUser().getId().equals(user.getId()))
                .ifPresent(
                        principal -> {
                            mav.addObject("profileEditHref", "/account");
                            mav.addObject(
                                    "profileEditLabel",
                                    messageSource.getMessage(
                                            "profile.public.edit",
                                            null,
                                            "Edit profile",
                                            resolvedLocale));
                        });
        return mav;
    }

    @PostMapping("/users/{username}/reviews")
    @PreAuthorize("isAuthenticated()")
    public ModelAndView submitReview(
            @PathVariable("username") final String username,
            @RequestParam("reaction") final String reactionValue,
            @RequestParam(value = "comment", required = false) final String comment,
            final RedirectAttributes redirectAttributes) {
        final User reviewedUser = findUserByUsernameOrThrow(username);
        final User currentUser = SecurityControllerUtils.requireAuthenticatedUser();
        final Optional<PlayerReviewReaction> reaction =
                PersistableEnum.fromDbValue(PlayerReviewReaction.class, reactionValue);
        if (reaction.isEmpty()) {
            return redirectToProfile(username, "invalid_reaction", null);
        }

        try {
            playerReviewService.submitReview(currentUser, reviewedUser, reaction.get(), comment);
            return redirectToProfile(username, null, "saved", redirectAttributes);
        } catch (final PlayerReviewException e) {
            return redirectToProfile(username, e.getCode(), null);
        }
    }

    @PostMapping("/users/{username}/reviews/delete")
    @PreAuthorize("isAuthenticated() and @securityService.hasReviewed(#username)")
    public ModelAndView deleteReview(
            @PathVariable("username") final String username,
            final RedirectAttributes redirectAttributes) {
        final User reviewedUser = findUserByUsernameOrThrow(username);
        final User currentUser = SecurityControllerUtils.requireAuthenticatedUser();

        try {
            playerReviewService.deleteReview(currentUser, reviewedUser);
            return redirectToProfile(username, null, "deleted", redirectAttributes);
        } catch (final PlayerReviewException e) {
            return redirectToProfile(username, e.getCode(), null);
        }
    }

    private void addReviewModel(
            final ModelAndView mav,
            final User user,
            final String reviewForm,
            final String reviewFilter,
            final int reviewPage,
            final Locale locale) {
        final PlayerReviewSummary summary = playerReviewService.findSummaryForUser(user);
        final PlayerReviewFilter selectedFilter =
                PlayerReviewFilter.fromQueryValueOrDefault(reviewFilter);
        final PaginatedResult<PlayerReview> reviewResult =
                playerReviewService.findReviewsForUser(
                        user, selectedFilter, reviewPage, REVIEW_PAGE_SIZE);
        final List<PlayerReviewViewModel> reviews =
                reviewResult.getItems().stream()
                        .map(review -> toReviewViewModel(review, locale))
                        .toList();
        final User currentUser = SecurityControllerUtils.currentUserOrNull();
        final Optional<PlayerReview> viewerReview =
                currentUser == null
                        ? Optional.empty()
                        : playerReviewService.findReviewByPair(currentUser, user);
        final boolean reviewCanSubmit =
                currentUser != null
                        && !currentUser.getId().equals(user.getId())
                        && playerReviewService.canReview(currentUser, user);
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
        mav.addObject("reviewFilterOptions", reviewFilterOptions(user, selectedFilter, locale));
        mav.addObject("selectedReviewFilter", selectedFilter.getQueryValue());
        mav.addObject("reviewTotalPages", reviewResult.getTotalPages());
        mav.addObject(
                "reviewPaginationItems",
                buildReviewPaginationItems(user, selectedFilter, reviewResult));
        mav.addObject(
                "reviewPreviousPageHref",
                reviewResult.hasPrevious()
                        ? buildReviewPageUrl(user, selectedFilter, reviewResult.getPage() - 1)
                        : null);
        mav.addObject(
                "reviewNextPageHref",
                reviewResult.hasNext()
                        ? buildReviewPageUrl(user, selectedFilter, reviewResult.getPage() + 1)
                        : null);
        mav.addObject("reviewCanSubmit", reviewCanSubmit);
        mav.addObject("reviewFormVisible", reviewCanSubmit && "open".equals(reviewForm));
        mav.addObject("viewerReview", viewerReview.orElse(null));
        mav.addObject("reviewActionPath", profilePath + "/reviews");
        mav.addObject("reviewDeletePath", profilePath + "/reviews/delete");
        mav.addObject(
                "reviewFormPath",
                buildReviewPageUrl(user, selectedFilter, reviewResult.getPage(), "open"));
        mav.addObject(
                "reviewSectionPath",
                buildReviewPageUrl(user, selectedFilter, reviewResult.getPage()));
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
                label, buildReviewPageUrl(user, filter, 1), null, filter == selectedFilter);
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
                buildReviewPageUrl(user, selectedFilter, page),
                page == currentPage,
                false);
    }

    private static String buildReviewPageUrl(
            final User user, final PlayerReviewFilter selectedFilter, final int page) {
        return buildReviewPageUrl(user, selectedFilter, page, null);
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

    private static String updatedAtLabel(final PlayerReview review, final Locale locale) {
        if (review.getUpdatedAt() == null) {
            return "";
        }
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale == null ? Locale.ENGLISH : locale)
                .format(review.getUpdatedAt().atZone(ZoneId.systemDefault()));
    }

    private static int parseReviewPage(final String reviewPage) {
        try {
            final int parsedPage = Integer.parseInt(reviewPage);
            return parsedPage > 0 ? parsedPage : 1;
        } catch (final NumberFormatException e) {
            return 1;
        }
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
