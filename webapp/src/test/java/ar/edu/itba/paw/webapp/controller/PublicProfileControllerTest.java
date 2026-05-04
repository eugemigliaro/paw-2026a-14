package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewFilter;
import ar.edu.itba.paw.models.PlayerReviewReaction;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PlayerReviewViewModel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PublicProfileControllerTest {

    private MockMvc mockMvc;
    private UserService userService;
    private PlayerReviewService playerReviewService;
    private ModerationService moderationService;
    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        playerReviewService = Mockito.mock(PlayerReviewService.class);
        moderationService = Mockito.mock(ModerationService.class);
        messageSource = Mockito.mock(MessageSource.class);

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new PublicProfileController(
                                        userService,
                                        playerReviewService,
                                        moderationService,
                                        messageSource))
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
        final User user = new User(42L, "target@test.com", "target", "Target", "User", null, null);
        final PlayerReview review =
                new PlayerReview(
                        11L,
                        99L,
                        42L,
                        PlayerReviewReaction.LIKE,
                        "Helpful",
                        Instant.parse("2026-04-10T10:00:00Z"),
                        Instant.parse("2026-04-10T10:00:00Z"),
                        null);
        Mockito.when(userService.findByUsername("target")).thenReturn(Optional.of(user));
        Mockito.when(playerReviewService.findSummaryForUser(42L))
                .thenReturn(new PlayerReviewSummary(42L, 1, 0, 1));
        Mockito.when(playerReviewService.findReviewsForUser(42L, PlayerReviewFilter.BOTH, 1, 10))
                .thenReturn(new PaginatedResult<>(List.of(review), 1, 1, 10));
        Mockito.when(userService.findByIds(List.of(99L))).thenReturn(List.of());
        Mockito.when(moderationService.findActiveBan(42L)).thenReturn(Optional.empty());
        Mockito.when(
                        messageSource.getMessage(
                                Mockito.eq("profile.reviews.unknownReviewer"),
                                Mockito.isNull(),
                                Mockito.eq("Unknown player"),
                                Mockito.any()))
                .thenReturn("Unknown player");

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
        authenticateUser(1L);
        final User user = new User(42L, "target@test.com", "target", "Target", "User", null, null);
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
        authenticateUser(1L);
        final User user = new User(42L, "target@test.com", "target", "Target", "User", null, null);
        Mockito.when(userService.findByUsername("target")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/users/target/reviews/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/target#reviews"))
                .andExpect(flash().attribute("reviewStatus", "deleted"));
    }

    private static void authenticateUser(final Long userId) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUserPrincipal(
                                        new UserAccount(
                                                userId,
                                                "user@test.com",
                                                "user",
                                                "{bcrypt}hash",
                                                UserRole.USER,
                                                Instant.parse("2026-04-10T10:00:00Z"))),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
