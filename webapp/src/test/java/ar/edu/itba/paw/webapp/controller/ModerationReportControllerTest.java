package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.EventJoinPolicy;
import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.EventVisibility;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewReaction;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.exceptions.ModerationException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ModerationReportControllerTest {

    private MockMvc mockMvc;
    private ModerationService moderationService;
    private UserService userService;
    private MatchService matchService;
    private PlayerReviewService playerReviewService;
    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        moderationService = Mockito.mock(ModerationService.class);
        userService = Mockito.mock(UserService.class);
        matchService = Mockito.mock(MatchService.class);
        playerReviewService = Mockito.mock(PlayerReviewService.class);
        messageSource = Mockito.mock(MessageSource.class);
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new ModerationReportController(
                                        moderationService,
                                        userService,
                                        matchService,
                                        playerReviewService,
                                        messageSource))
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserReportPageRenders() throws Exception {
        authenticateUser(9L);
        final User reportedUser =
                new User(42L, "target@test.com", "target", "Target", "User", null, null);
        Mockito.when(userService.findByUsername("target")).thenReturn(Optional.of(reportedUser));

        mockMvc.perform(get("/reports/users/target"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/create"));
    }

    @Test
    void postUserReportRedirectsWithSuccess() throws Exception {
        authenticateUser(9L);
        final User reportedUser =
                new User(42L, "target@test.com", "target", "Target", "User", null, null);
        Mockito.when(userService.findByUsername("target")).thenReturn(Optional.of(reportedUser));

        mockMvc.perform(
                        post("/reports/users/target")
                                .param("reason", "harassment")
                                .param("details", "Bad behavior"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reports/users/target?report=sent"));
    }

    @Test
    void postReviewReportRendersWithErrorCode() throws Exception {
        authenticateUser(9L);
        final PlayerReview review =
                new PlayerReview(
                        12L,
                        7L,
                        42L,
                        PlayerReviewReaction.LIKE,
                        "Great player",
                        Instant.parse("2026-04-10T10:00:00Z"),
                        Instant.parse("2026-04-10T10:00:00Z"),
                        false,
                        null,
                        null,
                        null);
        final User author = new User(7L, "author@test.com", "author", "Author", "User", null, null);
        final User reviewedUser =
                new User(42L, "reviewed@test.com", "reviewed", "Reviewed", "User", null, null);
        Mockito.when(playerReviewService.findReviewByIdIncludingDeleted(12L))
                .thenReturn(Optional.of(review));
        Mockito.when(userService.findById(7L)).thenReturn(Optional.of(author));
        Mockito.when(userService.findById(42L)).thenReturn(Optional.of(reviewedUser));
        Mockito.when(
                        moderationService.reportContent(
                                Mockito.eq(9L),
                                Mockito.any(),
                                Mockito.eq(12L),
                                Mockito.any(),
                                Mockito.any()))
                .thenThrow(new ModerationException("duplicate_report", "Duplicate report"));

        mockMvc.perform(post("/reports/reviews/12").param("reason", "inappropriate_content"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/create"))
                .andExpect(
                        model().attributeHasFieldErrorCode(
                                        "reportForm",
                                        "reason",
                                        "moderation.report.error.duplicate"));
    }

    @Test
    void postMatchReportRedirectsWithSuccess() throws Exception {
        authenticateUser(9L);
        final Match match =
                new Match(
                        42L,
                        Sport.FOOTBALL,
                        7L,
                        "Main Street 123",
                        "Friday Match",
                        "Come play",
                        Instant.parse("2026-04-10T10:00:00Z"),
                        Instant.parse("2026-04-10T12:00:00Z"),
                        10,
                        BigDecimal.ZERO,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        0,
                        null);
        Mockito.when(matchService.findMatchById(42L)).thenReturn(Optional.of(match));

        mockMvc.perform(post("/reports/matches/42").param("reason", "harassment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reports/matches/42?report=sent"));
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
