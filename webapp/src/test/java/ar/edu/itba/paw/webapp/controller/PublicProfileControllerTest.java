package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.PlayerReviewFilter;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.UserSportRatingService;
import ar.edu.itba.paw.services.exceptions.playerReview.PlayerReviewNotFoundException;
import ar.edu.itba.paw.webapp.config.converters.StringToPlayerReviewFilterConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToPlayerReviewReactionConverter;
import ar.edu.itba.paw.webapp.security.annotation.CurrentUserArgumentResolver;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PlayerReviewViewModel;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

class PublicProfileControllerTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T00:00:00Z");

    private MockMvc mockMvc;
    private UserService userService;
    private PlayerReviewService playerReviewService;
    private ModerationService moderationService;
    private UserSportRatingService userSportRatingService;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        playerReviewService = Mockito.mock(PlayerReviewService.class);
        moderationService = Mockito.mock(ModerationService.class);
        userSportRatingService = Mockito.mock(UserSportRatingService.class);

        Mockito.when(userSportRatingService.findRatingsForUser(Mockito.any()))
                .thenReturn(java.util.List.of());
        Mockito.when(moderationService.findActiveBan(Mockito.any(User.class)))
                .thenReturn(Optional.empty());

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new PublicProfileController(
                                        userService,
                                        playerReviewService,
                                        moderationService,
                                        userSportRatingService,
                                        messageSource()))
                        .setConversionService(conversionService())
                        .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                        .setLocaleResolver(localeResolver())
                        .addInterceptors(localeChangeInterceptor())
                        .defaultRequest(get("/").locale(Locale.ENGLISH))
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getProfileReturnsNotFoundWhenUserMissing() throws Exception {
        Mockito.when(userService.findByUsername("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/unknown")).andExpect(status().isNotFound());
    }

    @Test
    void getProfileDoesNotLinkUnknownReviewers() throws Exception {
        final User targetUser = UserUtils.getUser(42L);
        final User unknownReviewer =
                new User(99L, "unknown@test.com", null, null, null, null, null, null);

        final PlayerReview review =
                new PlayerReview(
                        11L,
                        unknownReviewer,
                        targetUser,
                        PlayerReviewReaction.LIKE,
                        "Helpful",
                        Instant.parse("2026-04-10T10:00:00Z"),
                        Instant.parse("2026-04-10T10:00:00Z"),
                        false,
                        null,
                        null,
                        null);

        Mockito.when(userService.findByUsername("target")).thenReturn(Optional.of(targetUser));
        Mockito.when(playerReviewService.findSummaryForUser(targetUser))
                .thenReturn(new PlayerReviewSummary(42L, 1, 0, 1));
        Mockito.when(
                        playerReviewService.findReviewsForUser(
                                targetUser, PlayerReviewFilter.BOTH, 1, 10))
                .thenReturn(new PaginatedResult<>(List.of(review), 1, 1, 10));
        Mockito.when(moderationService.findActiveBan(targetUser)).thenReturn(Optional.empty());

        final MvcResult result =
                mockMvc.perform(get("/users/target"))
                        .andExpect(status().isOk())
                        .andExpect(model().attributeExists("profileReviews"))
                        .andReturn();

        final List<?> reviews = (List<?>) result.getModelAndView().getModel().get("profileReviews");
        final PlayerReviewViewModel firstReview = (PlayerReviewViewModel) reviews.getFirst();
        Assertions.assertEquals("Unknown player", firstReview.getReviewerUsername());
        Assertions.assertNull(firstReview.getReviewerProfileHref());
    }

    @Test
    void postReviewRedirectsWithSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(1L);
        final User user = UserUtils.getUser(42L);
        Mockito.when(userService.findByUsername("target")).thenReturn(Optional.of(user));

        mockMvc.perform(
                        post("/users/target/reviews")
                                .param("reaction", "like")
                                .param("comment", "Great player"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/target#reviews"))
                .andExpect(flash().attribute("reviewStatus", "saved"));
    }

    @Test
    void deleteReviewRedirectsWithSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(1L);
        final User user = UserUtils.getUser(42L);
        Mockito.when(userService.findByUsername("target")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/users/target/reviews/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/target#reviews"))
                .andExpect(flash().attribute("reviewStatus", "deleted"));
    }

    @Test
    void deleteReviewWhenNoReviewRedirectsWithError() throws Exception {
        AuthenticationUtils.authenticateUser(1L);
        final User user = UserUtils.getUser(42L);
        Mockito.when(userService.findByUsername("target")).thenReturn(Optional.of(user));
        Mockito.doThrow(new PlayerReviewNotFoundException("Player review not found."))
                .when(playerReviewService)
                .deleteReview(Mockito.any(), Mockito.eq(user));

        mockMvc.perform(post("/users/target/reviews/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/target?reviewError=not_found#reviews"));
    }

    @Test
    void getPublicProfileRouteRendersPublicProfileForAnonymousUsers() throws Exception {
        final User hostPlayer = hostPlayer();
        Mockito.when(userService.findByUsername("host-player")).thenReturn(Optional.of(hostPlayer));
        Mockito.when(playerReviewService.findSummaryForUser(hostPlayer))
                .thenReturn(new PlayerReviewSummary(9L, 0, 0, 0));
        Mockito.when(
                        playerReviewService.findReviewsForUser(
                                hostPlayer, PlayerReviewFilter.BOTH, 1, 10))
                .thenReturn(new PaginatedResult<>(List.of(), 0, 1, 10));

        mockMvc.perform(get("/users/host-player"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attributeExists("profilePage"))
                .andExpect(model().attributeExists("reviewSummary"))
                .andExpect(model().attributeExists("profileReviews"))
                .andExpect(model().attribute("reviewLikeLabel", "Likes"))
                .andExpect(model().attribute("reviewDislikeLabel", "Dislikes"))
                .andExpect(model().attribute("reviewFormVisible", false))
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty(
                                                "username", Matchers.is("host-player"))))
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty("name", Matchers.is("Jamie"))))
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty("lastName", Matchers.is("Rivera"))))
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty(
                                                "phone", Matchers.is("+1 555 123 4567"))))
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty(
                                                "email", Matchers.is("host@test.com"))))
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty(
                                                "profileImageUrl",
                                                Matchers.is(
                                                        "/assets/default-profile-avatar.svg"))));
    }

    @Test
    void getPublicProfileRouteShowsReviewActionsForEligibleAuthenticatedViewer() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        final User secondPlayer = secondPlayer();
        final PlayerReview viewerReview = viewerReview();
        Mockito.when(userService.findByUsername("second-player"))
                .thenReturn(Optional.of(secondPlayer));
        Mockito.when(playerReviewService.findSummaryForUser(secondPlayer))
                .thenReturn(new PlayerReviewSummary(3L, 1, 0, 1));
        Mockito.when(
                        playerReviewService.findReviewsForUser(
                                secondPlayer, PlayerReviewFilter.BOTH, 1, 10))
                .thenReturn(new PaginatedResult<>(List.of(viewerReview), 21, 1, 10));
        Mockito.when(
                        playerReviewService.findReviewByPair(
                                Mockito.any(User.class), Mockito.eq(secondPlayer)))
                .thenReturn(Optional.of(viewerReview));
        Mockito.when(
                        playerReviewService.canReview(
                                Mockito.any(User.class), Mockito.eq(secondPlayer)))
                .thenReturn(true);

        mockMvc.perform(get("/users/second-player"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attribute("reviewCanSubmit", true))
                .andExpect(model().attribute("reviewFormVisible", false))
                .andExpect(model().attribute("reviewLikeLabel", "Like"))
                .andExpect(model().attribute("reviewDislikeLabel", "Dislikes"))
                .andExpect(
                        model().attribute(
                                        "reviewFormPath",
                                        "/users/second-player?reviewFilter=both&reviewPage=1&reviewForm=open#reviews"))
                .andExpect(model().attributeExists("viewerReview"))
                .andExpect(
                        model().attribute(
                                        "reviewSummary",
                                        Matchers.hasProperty("reviewCount", Matchers.is(1L))));
    }

    @Test
    void getPublicProfileRouteFiltersPositiveReviews() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        final User secondPlayer = secondPlayer();
        final PlayerReview viewerReview = viewerReview();
        Mockito.when(userService.findByUsername("second-player"))
                .thenReturn(Optional.of(secondPlayer));
        Mockito.when(playerReviewService.findSummaryForUser(secondPlayer))
                .thenReturn(new PlayerReviewSummary(3L, 1, 0, 1));
        Mockito.when(
                        playerReviewService.findReviewsForUser(
                                secondPlayer, PlayerReviewFilter.POSITIVE, 1, 10))
                .thenReturn(new PaginatedResult<>(List.of(viewerReview), 1, 1, 10));
        Mockito.when(
                        playerReviewService.findReviewByPair(
                                Mockito.any(User.class), Mockito.eq(secondPlayer)))
                .thenReturn(Optional.of(viewerReview));
        Mockito.when(
                        playerReviewService.canReview(
                                Mockito.any(User.class), Mockito.eq(secondPlayer)))
                .thenReturn(true);

        mockMvc.perform(get("/users/second-player").param("reviewFilter", "positive"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attribute("selectedReviewFilter", "positive"))
                .andExpect(model().attribute("profileReviews", Matchers.hasSize(1)))
                .andExpect(
                        model().attribute(
                                        "profileReviews",
                                        Matchers.hasItem(
                                                Matchers.hasProperty(
                                                        "reaction", Matchers.is("like")))))
                .andExpect(
                        model().attribute(
                                        "reviewFilterOptions",
                                        Matchers.hasItem(
                                                Matchers.allOf(
                                                        Matchers.hasProperty(
                                                                "label", Matchers.is("Positive")),
                                                        Matchers.hasProperty(
                                                                "href",
                                                                Matchers.is(
                                                                        "/users/second-player?reviewFilter=positive&reviewPage=1#reviews")),
                                                        Matchers.hasProperty(
                                                                "active", Matchers.is(true))))));
    }

    @Test
    void getPublicProfileRoutePaginatesReviews() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        final User secondPlayer = secondPlayer();
        final PlayerReview viewerReview = viewerReview();
        Mockito.when(userService.findByUsername("second-player"))
                .thenReturn(Optional.of(secondPlayer));
        Mockito.when(playerReviewService.findSummaryForUser(secondPlayer))
                .thenReturn(new PlayerReviewSummary(3L, 1, 0, 1));
        Mockito.when(
                        playerReviewService.findReviewsForUser(
                                secondPlayer, PlayerReviewFilter.BOTH, 2, 10))
                .thenReturn(new PaginatedResult<>(List.of(viewerReview), 21, 2, 10));
        Mockito.when(
                        playerReviewService.findReviewByPair(
                                Mockito.any(User.class), Mockito.eq(secondPlayer)))
                .thenReturn(Optional.of(viewerReview));
        Mockito.when(
                        playerReviewService.canReview(
                                Mockito.any(User.class), Mockito.eq(secondPlayer)))
                .thenReturn(true);

        mockMvc.perform(get("/users/second-player").param("reviewPage", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attribute("reviewTotalPages", 3))
                .andExpect(
                        model().attribute(
                                        "reviewPreviousPageHref",
                                        "/users/second-player?reviewFilter=both&reviewPage=1#reviews"))
                .andExpect(
                        model().attribute(
                                        "reviewNextPageHref",
                                        "/users/second-player?reviewFilter=both&reviewPage=3#reviews"))
                .andExpect(
                        model().attribute(
                                        "reviewPaginationItems",
                                        Matchers.hasItem(
                                                Matchers.allOf(
                                                        Matchers.hasProperty(
                                                                "label", Matchers.is("2")),
                                                        Matchers.hasProperty(
                                                                "current", Matchers.is(true))))));
    }

    @Test
    void getPublicProfileRouteThrowsErrorForInvalidReviewPage() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        final User secondPlayer = secondPlayer();
        Mockito.when(userService.findByUsername("second-player"))
                .thenReturn(Optional.of(secondPlayer));

        mockMvc.perform(get("/users/second-player").param("reviewPage", "bad"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPublicProfileRouteOpensReviewFormWhenRequested() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        final User secondPlayer = secondPlayer();
        final PlayerReview viewerReview = viewerReview();
        Mockito.when(userService.findByUsername("second-player"))
                .thenReturn(Optional.of(secondPlayer));
        Mockito.when(playerReviewService.findSummaryForUser(secondPlayer))
                .thenReturn(new PlayerReviewSummary(3L, 1, 0, 1));
        Mockito.when(
                        playerReviewService.findReviewsForUser(
                                secondPlayer, PlayerReviewFilter.POSITIVE, 2, 10))
                .thenReturn(new PaginatedResult<>(List.of(viewerReview), 1, 1, 10));
        Mockito.when(
                        playerReviewService.findReviewByPair(
                                Mockito.any(User.class), Mockito.eq(secondPlayer)))
                .thenReturn(Optional.of(viewerReview));
        Mockito.when(
                        playerReviewService.canReview(
                                Mockito.any(User.class), Mockito.eq(secondPlayer)))
                .thenReturn(true);

        mockMvc.perform(
                        get("/users/second-player")
                                .param("reviewFilter", "positive")
                                .param("reviewPage", "2")
                                .param("reviewForm", "open"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attribute("reviewCanSubmit", true))
                .andExpect(model().attribute("reviewFormVisible", true))
                .andExpect(
                        model().attribute(
                                        "reviewSectionPath",
                                        "/users/second-player?reviewFilter=positive&reviewPage=1#reviews"));
    }

    @Test
    void getPublicProfileRouteUsesUploadedProfileImageWhenPresent() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        final User hostWithImage =
                new User(
                        9L,
                        "host@test.com",
                        "host-player",
                        "Jamie",
                        "Rivera",
                        "+1 555 123 4567",
                        new ImageMetadata(500L, "image/png", 3),
                        null);
        Mockito.when(userService.findByUsername("host-player"))
                .thenReturn(Optional.of(hostWithImage));
        Mockito.when(playerReviewService.findSummaryForUser(hostWithImage))
                .thenReturn(new PlayerReviewSummary(9L, 0, 0, 0));
        Mockito.when(
                        playerReviewService.findReviewsForUser(
                                hostWithImage, PlayerReviewFilter.BOTH, 1, 10))
                .thenReturn(new PaginatedResult<>(List.of(), 0, 1, 10));
        Mockito.when(
                        playerReviewService.findReviewByPair(
                                Mockito.any(User.class), Mockito.eq(hostWithImage)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/users/host-player"))
                .andExpect(status().isOk())
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty(
                                                "profileImageUrl", Matchers.is("/images/500"))));
    }

    @Test
    void getOwnPublicProfileRouteShowsEditAction() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        final User hostPlayer = hostPlayer();
        Mockito.when(userService.findByUsername("host-player")).thenReturn(Optional.of(hostPlayer));
        Mockito.when(playerReviewService.findSummaryForUser(hostPlayer))
                .thenReturn(new PlayerReviewSummary(9L, 0, 0, 0));
        Mockito.when(
                        playerReviewService.findReviewsForUser(
                                hostPlayer, PlayerReviewFilter.BOTH, 1, 10))
                .thenReturn(new PaginatedResult<>(List.of(), 0, 1, 10));
        Mockito.when(
                        playerReviewService.findReviewByPair(
                                Mockito.any(User.class), Mockito.eq(hostPlayer)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/users/host-player"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("profileEditHref", "/account"))
                .andExpect(model().attribute("reviewFormVisible", false));
    }

    @Test
    void getOtherPublicProfileRouteDoesNotShowEditAction() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        final User secondPlayer = secondPlayer();
        final PlayerReview viewerReview = viewerReview();
        Mockito.when(userService.findByUsername("second-player"))
                .thenReturn(Optional.of(secondPlayer));
        Mockito.when(playerReviewService.findSummaryForUser(secondPlayer))
                .thenReturn(new PlayerReviewSummary(3L, 1, 0, 1));
        Mockito.when(
                        playerReviewService.findReviewsForUser(
                                secondPlayer, PlayerReviewFilter.BOTH, 1, 10))
                .thenReturn(new PaginatedResult<>(List.of(viewerReview), 21, 1, 10));
        Mockito.when(
                        playerReviewService.findReviewByPair(
                                Mockito.any(User.class), Mockito.eq(secondPlayer)))
                .thenReturn(Optional.of(viewerReview));
        Mockito.when(
                        playerReviewService.canReview(
                                Mockito.any(User.class), Mockito.eq(secondPlayer)))
                .thenReturn(true);

        mockMvc.perform(get("/users/second-player"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attributeDoesNotExist("profileEditHref"));
    }

    @Test
    void getPublicProfileRouteWithSpanishLocaleLocalizesPageCopy() throws Exception {
        final User hostPlayer = hostPlayer();
        Mockito.when(userService.findByUsername("host-player")).thenReturn(Optional.of(hostPlayer));
        Mockito.when(playerReviewService.findSummaryForUser(hostPlayer))
                .thenReturn(new PlayerReviewSummary(9L, 0, 0, 0));
        Mockito.when(
                        playerReviewService.findReviewsForUser(
                                hostPlayer, PlayerReviewFilter.BOTH, 1, 10))
                .thenReturn(new PaginatedResult<>(List.of(), 0, 1, 10));

        mockMvc.perform(get("/users/host-player").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty(
                                                "email", Matchers.is("host@test.com"))))
                .andExpect(model().attribute("profileUsernameLabel", "Usuario"))
                .andExpect(model().attribute("profileEmailLabel", "Email"))
                .andExpect(model().attribute("profilePhoneLabel", "Teléfono"));
    }

    private static User hostPlayer() {
        return new User(
                9L,
                "host@test.com",
                "host-player",
                "Jamie",
                "Rivera",
                "+1 555 123 4567",
                null,
                null);
    }

    private static User secondPlayer() {
        return new User(
                3L, "second@test.com", "second-player", "Second", "Player", null, null, null);
    }

    private static PlayerReview viewerReview() {
        final User reviewer = Mockito.mock(User.class);
        final User reviewed = Mockito.mock(User.class);
        Mockito.when(reviewer.getId()).thenReturn(9L);
        Mockito.when(reviewed.getId()).thenReturn(3L);
        return new PlayerReview(
                1L,
                reviewer,
                reviewed,
                PlayerReviewReaction.LIKE,
                "Good teammate",
                FIXED_NOW,
                FIXED_NOW,
                false,
                null,
                null,
                null);
    }

    private static MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    private static SessionLocaleResolver localeResolver() {
        final SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        return localeResolver;
    }

    private static LocaleChangeInterceptor localeChangeInterceptor() {
        final LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        return localeChangeInterceptor;
    }

    private static DefaultFormattingConversionService conversionService() {
        final DefaultFormattingConversionService conversionService =
                new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToPlayerReviewFilterConverter());
        conversionService.addConverter(new StringToPlayerReviewReactionConverter());
        return conversionService;
    }
}
