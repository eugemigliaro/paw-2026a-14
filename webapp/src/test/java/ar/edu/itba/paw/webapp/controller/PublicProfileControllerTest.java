package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.UserService;
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
    void postReviewRedirectsWithSuccess() throws Exception {
        authenticateUser(1L);
        final User user = new User(42L, "target@test.com", "target", "Target", "User", null, null);
        Mockito.when(userService.findByUsername("target")).thenReturn(Optional.of(user));

        mockMvc.perform(
                        post("/users/target/reviews")
                                .param("reaction", "like")
                                .param("comment", "Great player"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/target?review=saved#reviews"));
    }

    @Test
    void deleteReviewRedirectsWithSuccess() throws Exception {
        authenticateUser(1L);
        final User user = new User(42L, "target@test.com", "target", "Target", "User", null, null);
        Mockito.when(userService.findByUsername("target")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/users/target/reviews/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/target?review=deleted#reviews"));
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
