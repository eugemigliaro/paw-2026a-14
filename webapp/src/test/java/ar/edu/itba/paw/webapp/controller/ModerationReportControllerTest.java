package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.exceptions.ModerationException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ModerationReportControllerTest {

    private MockMvc mockMvc;
    private ModerationService moderationService;

    @BeforeEach
    void setUp() {
        moderationService = Mockito.mock(ModerationService.class);
        mockMvc =
                MockMvcBuilders.standaloneSetup(new ModerationReportController(moderationService))
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void postMatchReportRedirectsWithSuccess() throws Exception {
        authenticateUser(9L);

        mockMvc.perform(
                        post("/reports/matches/42")
                                .param("reason", "harassment")
                                .param("details", "Bad behavior"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42?report=sent#moderation"));
    }

    @Test
    void postReviewReportRedirectsWithErrorCode() throws Exception {
        authenticateUser(9L);
        Mockito.when(
                        moderationService.reportContent(
                                Mockito.eq(9L),
                                Mockito.any(),
                                Mockito.eq(12L),
                                Mockito.any(),
                                Mockito.any()))
                .thenThrow(new ModerationException("duplicate_report", "Duplicate report"));

        mockMvc.perform(
                        post("/reports/reviews/12")
                                .param("username", "host-player")
                                .param("reason", "inappropriate_content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl("/users/host-player?reportError=duplicate_report#reviews"));
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
