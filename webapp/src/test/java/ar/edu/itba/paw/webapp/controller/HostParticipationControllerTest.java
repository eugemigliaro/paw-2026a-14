package ar.edu.itba.paw.webapp.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
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

class HostParticipationControllerTest {

    private MockMvc mockMvc;
    private MatchService matchService;
    private MatchParticipationService matchParticipationService;
    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        matchService = Mockito.mock(MatchService.class);
        matchParticipationService = Mockito.mock(MatchParticipationService.class);
        messageSource = Mockito.mock(MessageSource.class);

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HostParticipationController(
                                        matchService, matchParticipationService, messageSource))
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approveRequestRedirectsWithSuccess() throws Exception {
        authenticateUser(1L); // Host user
        Match mockMatch = Mockito.mock(Match.class);
        when(mockMatch.getHostUserId()).thenReturn(1L);
        when(mockMatch.getJoinPolicy()).thenReturn("approval_required");
        when(matchService.findMatchById(42L)).thenReturn(Optional.of(mockMatch));

        mockMvc.perform(post("/host/matches/42/requests/9/approve"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/host/matches/42/requests"))
                .andExpect(flash().attribute("action", "approved"));
    }

    @Test
    void rejectRequestRedirectsWithSuccess() throws Exception {
        authenticateUser(1L);
        Match mockMatch = Mockito.mock(Match.class);
        when(mockMatch.getHostUserId()).thenReturn(1L);
        when(mockMatch.getJoinPolicy()).thenReturn("approval_required");
        when(matchService.findMatchById(42L)).thenReturn(Optional.of(mockMatch));

        mockMvc.perform(post("/host/matches/42/requests/9/reject"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/host/matches/42/requests"))
                .andExpect(flash().attribute("action", "rejected"));
    }

    @Test
    void inviteUserRedirectsWithSuccess() throws Exception {
        authenticateUser(1L);
        Match mockMatch = Mockito.mock(Match.class);
        when(mockMatch.getHostUserId()).thenReturn(1L);
        when(mockMatch.getJoinPolicy()).thenReturn("invite_only");
        when(matchService.findMatchById(42L)).thenReturn(Optional.of(mockMatch));

        mockMvc.perform(post("/host/matches/42/invites").param("email", "test@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/host/matches/42/invites"))
                .andExpect(flash().attribute("action", "invited"));
    }

    @Test
    void removeParticipantRedirectsWithSuccess() throws Exception {
        authenticateUser(1L);

        mockMvc.perform(post("/host/matches/42/participants/9/remove"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/host/matches/42/participants"))
                .andExpect(flash().attribute("action", "removed"));
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
