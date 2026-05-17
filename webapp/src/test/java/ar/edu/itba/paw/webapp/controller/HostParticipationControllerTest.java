package ar.edu.itba.paw.webapp.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HostParticipationControllerTest {

    private MockMvc mockMvc;
    private MatchService matchService;
    private MatchParticipationService matchParticipationService;
    private MessageSource messageSource;
    private UserService userService;

    @BeforeEach
    void setUp() {
        matchService = Mockito.mock(MatchService.class);
        matchParticipationService = Mockito.mock(MatchParticipationService.class);
        messageSource = Mockito.mock(MessageSource.class);
        userService = Mockito.mock(UserService.class);

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HostParticipationController(
                                        matchService,
                                        matchParticipationService,
                                        userService,
                                        messageSource))
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approveRequestRedirectsWithSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(1L);

        final User host = Mockito.mock(User.class);
        final User requestedUser = Mockito.mock(User.class);

        Match mockMatch = Mockito.mock(Match.class);

        when(mockMatch.getHost()).thenReturn(host);
        when(mockMatch.getHost().getId()).thenReturn(1L);
        when(mockMatch.getJoinPolicy()).thenReturn(EventJoinPolicy.APPROVAL_REQUIRED);

        when(matchService.findMatchById(42L)).thenReturn(Optional.of(mockMatch));
        when(userService.findById(9L)).thenReturn(Optional.of(requestedUser));

        mockMvc.perform(post("/host/matches/42/requests/9/approve"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/host/matches/42/requests"))
                .andExpect(flash().attribute("action", "approved"));
    }

    @Test
    void rejectRequestRedirectsWithSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(1L);

        final User host = Mockito.mock(User.class);
        final User requestedUser = Mockito.mock(User.class);

        Match mockMatch = Mockito.mock(Match.class);

        when(mockMatch.getHost()).thenReturn(host);
        when(mockMatch.getHost().getId()).thenReturn(1L);
        when(mockMatch.getJoinPolicy()).thenReturn(EventJoinPolicy.APPROVAL_REQUIRED);

        when(matchService.findMatchById(42L)).thenReturn(Optional.of(mockMatch));
        when(userService.findById(9L)).thenReturn(Optional.of(requestedUser));

        mockMvc.perform(post("/host/matches/42/requests/9/reject"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/host/matches/42/requests"))
                .andExpect(flash().attribute("action", "rejected"));
    }

    @Test
    void inviteUserRedirectsWithSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(1L);

        final User host = Mockito.mock(User.class);
        final User requestedUser = Mockito.mock(User.class);
        Match mockMatch = Mockito.mock(Match.class);

        when(mockMatch.getHost()).thenReturn(host);
        when(mockMatch.getHost().getId()).thenReturn(1L);
        when(mockMatch.getJoinPolicy()).thenReturn(EventJoinPolicy.INVITE_ONLY);

        when(matchService.findMatchById(42L)).thenReturn(Optional.of(mockMatch));
        when(userService.findById(9L)).thenReturn(Optional.of(requestedUser));

        mockMvc.perform(post("/host/matches/42/invites").param("email", "test@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/host/matches/42/invites"))
                .andExpect(flash().attribute("action", "invited"));
    }

    @Test
    void removeParticipantRedirectsWithSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(1L);

        final User requestedUser = Mockito.mock(User.class);
        when(userService.findById(9L)).thenReturn(Optional.of(requestedUser));

        mockMvc.perform(post("/host/matches/42/participants/9/remove"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/host/matches/42/participants"))
                .andExpect(flash().attribute("action", "removed"));
    }
}
