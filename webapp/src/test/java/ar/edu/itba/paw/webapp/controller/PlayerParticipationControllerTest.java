package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationSeriesAlreadyPendingException;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.webapp.security.annotation.CurrentUserArgumentResolver;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PlayerParticipationControllerTest {

    private MockMvc mockMvc;
    private MatchParticipationService matchParticipationService;

    @BeforeEach
    void setUp() {
        matchParticipationService = Mockito.mock(MatchParticipationService.class);

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new PlayerParticipationController(matchParticipationService))
                        .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void postJoinRequestAsAuthenticatedUserRedirectsWithOneTimeNotice() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/56/join-requests"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/56"))
                .andExpect(flash().attribute("joinRequested", true));
    }

    @Test
    void postSeriesJoinRequestWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/matches/52/recurring-join-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postSeriesJoinRequestAsAuthenticatedUserRedirectsToRecurringRequestedEvent()
            throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/52/recurring-join-requests"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/52"))
                .andExpect(flash().attribute("seriesJoinRequested", true));
    }

    @Test
    void postSeriesJoinRequestFailureRedirectsWithJoinErrorCode() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");
        Mockito.doThrow(new MatchParticipationSeriesAlreadyPendingException())
                .when(matchParticipationService)
                .requestToJoinSeries(ArgumentMatchers.eq(52L), ArgumentMatchers.any(User.class));

        mockMvc.perform(post("/matches/52/recurring-join-requests"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/52?joinError=seriesAlreadyPending"));
    }

    @Test
    void postPrivateInviteDeclineRedirectsToUpcomingMatches() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/51/invites/decline"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches"));
    }
}
