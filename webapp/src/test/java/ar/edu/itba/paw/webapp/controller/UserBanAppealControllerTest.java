package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserBanAppealControllerTest {

    private MockMvc mockMvc;
    private ModerationService moderationService;

    @BeforeEach
    void setUp() {
        moderationService = Mockito.mock(ModerationService.class);
        final MessageSource messageSource = Mockito.mock(MessageSource.class);
        Mockito.when(
                        messageSource.getMessage(
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new UserBanAppealController(moderationService, messageSource))
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getBanPageRendersForActiveBan() throws Exception {
        authenticateUser(7L);
        Mockito.when(moderationService.findActiveBan(7L)).thenReturn(Optional.of(sampleBan()));

        mockMvc.perform(get("/account/ban"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/banned"));
    }

    @Test
    void postAppealRedirectsOnSuccess() throws Exception {
        authenticateUser(7L);
        Mockito.when(moderationService.findActiveBan(7L)).thenReturn(Optional.of(sampleBan()));

        mockMvc.perform(post("/account/ban/appeal").param("appealReason", "Please review"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/ban?action=appealed"));
    }

    private static UserBan sampleBan() {
        return new UserBan(
                12L,
                7L,
                1L,
                "Spam",
                Instant.now().plusSeconds(3600),
                Instant.now(),
                null,
                0,
                null,
                null,
                null,
                null);
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
