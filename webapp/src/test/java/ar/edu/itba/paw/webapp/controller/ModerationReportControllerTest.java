package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.exceptions.ModerationException;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.MatchUtils;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
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
        AuthenticationUtils.authenticateUser(9L);
        final User reportedUser = UserUtils.getUser(42L);
        final String username = reportedUser.getUsername();
        Mockito.when(userService.findByUsername(username)).thenReturn(Optional.of(reportedUser));

        mockMvc.perform(get("/reports/users/" + username))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/create"));
    }

    @Test
    void postUserReportRedirectsWithSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(9L);
        final User reportedUser = UserUtils.getUser(42L);
        final String username = reportedUser.getUsername();
        Mockito.when(userService.findByUsername(username)).thenReturn(Optional.of(reportedUser));

        mockMvc.perform(
                        post("/reports/users/" + username)
                                .param("reason", "harassment")
                                .param("details", "Bad behavior"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reports/users/" + username))
                .andExpect(flash().attribute("reportSent", true));
    }

    @Test
    void postReviewReportRendersWithErrorCode() throws Exception {
        AuthenticationUtils.authenticateUser(9L);
        final PlayerReview review =
                new PlayerReview(
                        12L,
                        UserUtils.getUser(7L),
                        UserUtils.getUser(42L),
                        PlayerReviewReaction.LIKE,
                        "Great player",
                        Instant.parse("2026-04-10T10:00:00Z"),
                        Instant.parse("2026-04-10T10:00:00Z"),
                        false,
                        null,
                        null,
                        null);
        Mockito.when(playerReviewService.findReviewByIdIncludingDeleted(12L))
                .thenReturn(Optional.of(review));
        Mockito.when(
                        moderationService.reportContent(
                                Mockito.any(User.class),
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
        AuthenticationUtils.authenticateUser(9L);
        final Match match =
                MatchUtils.createMatchWithId(
                        42L, 7L, Sport.FOOTBALL, Instant.parse("2026-04-10T10:00:00Z"), 10);

        Mockito.when(matchService.findMatchById(42L)).thenReturn(Optional.of(match));

        mockMvc.perform(post("/reports/matches/42").param("reason", "harassment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reports/matches/42"))
                .andExpect(flash().attribute("reportSent", true));
    }
}
