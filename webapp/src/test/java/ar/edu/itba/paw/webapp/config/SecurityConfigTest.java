package ar.edu.itba.paw.webapp.config;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.types.ReportReason;
import ar.edu.itba.paw.models.types.ReportStatus;
import ar.edu.itba.paw.models.types.ReportTargetType;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = SecurityConfigTest.TestConfig.class)
class SecurityConfigTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext context;

    @Autowired private ModerationService moderationService;

    @BeforeEach
    void setUp() {
        Mockito.when(moderationService.findActiveBan(any(User.class))).thenReturn(Optional.empty());
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void hostRouteRedirectsAnonymousToLogin() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/host/tournaments/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?continue"));
    }

    @Test
    void hostRouteAllowsRegularUser() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/host/tournaments/new").with(authenticatedUser()))
                .andExpect(status().isOk());
    }

    @Test
    void adminRouteRejectsRegularUser() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/admin/reports").with(authenticatedUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRouteAllowsAdminMod() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/admin/reports").with(authenticatedAdminMod()))
                .andExpect(status().isOk());
    }

    @Test
    void moderationRouteRejectsRegularUser() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/moderation/queue").with(authenticatedUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void moderationRouteAllowsAdminMod() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/moderation/queue").with(authenticatedAdminMod()))
                .andExpect(status().isOk());
    }

    @Test
    void myReportsRouteRedirectsAnonymousToLogin() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/reports/mine"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?continue"));
    }

    @Test
    void myReportsRouteAllowsRegularUser() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/reports/mine").with(authenticatedUser())).andExpect(status().isOk());
    }

    @Test
    void reportCreationRouteRedirectsAnonymousToLogin() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/reports/users/player"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?continue"));
    }

    @Test
    void reportCreationRouteAllowsRegularUser() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/reports/users/player").with(authenticatedUser()))
                .andExpect(status().isOk());
    }

    @Test
    void bannedUserIsRedirectedBySecurityFilterForProtectedRoute() throws Exception {
        // 1. Arrange
        Mockito.when(moderationService.findActiveBan(UserUtils.getUser(1L)))
                .thenReturn(Optional.of(sampleBan()));

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/reports/mine").with(authenticatedUser()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/ban"));
    }

    @Test
    void inviteAcceptRouteRedirectsAnonymousToLogin() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(post("/matches/42/invites/accept").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?continue"));
    }

    @Test
    void inviteAcceptRouteAllowsRegularUser() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(post("/matches/42/invites/accept").with(csrf()).with(authenticatedUser()))
                .andExpect(status().isOk());
    }

    @Test
    void publicProfileRouteAllowsAnonymous() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/users/player")).andExpect(status().isOk());
    }

    @Test
    void cssRouteIsNotSecured() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        // web.ignoring() bypasses the filter chain, so an anonymous request reaches the
        // dispatcher (404, no handler) instead of being redirected to /login.
        mockMvc.perform(get("/css/app.css")).andExpect(status().isNotFound());
    }

    @Test
    void jsRouteIsNotSecured() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/js/app.js")).andExpect(status().isNotFound());
    }

    @Test
    void assetsRouteIsNotSecured() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/assets/logo.png")).andExpect(status().isNotFound());
    }

    private static RequestPostProcessor authenticatedUser() {
        return authentication(authenticationFor(UserRole.USER, "ROLE_USER"));
    }

    private static RequestPostProcessor authenticatedAdminMod() {
        return authentication(authenticationFor(UserRole.ADMIN_MOD, "ROLE_ADMIN_MOD", "ROLE_USER"));
    }

    private static Authentication authenticationFor(
            final UserRole role, final String... authorities) {
        final User user = UserUtils.getUser(UserRole.ADMIN_MOD.equals(role) ? 2L : 1L);
        final List<SimpleGrantedAuthority> grantedAuthorities =
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(user, role), null, grantedAuthorities);
    }

    private static UserBan sampleBan() {
        return new UserBan(11L, sampleReport(), Instant.now().plusSeconds(600));
    }

    private static ModerationReport sampleReport() {
        return new ModerationReport(
                1L,
                UserUtils.getUser(1L),
                ReportTargetType.USER,
                1L,
                ReportReason.SPAM,
                "Details",
                ReportStatus.RESOLVED,
                null,
                null,
                null,
                null,
                null,
                (short) 0,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now());
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestConfig {

        @Bean
        AccountAuthService accountAuthService() {
            return Mockito.mock(AccountAuthService.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return Mockito.mock(PasswordEncoder.class);
        }

        @Bean
        MessageSource messageSource() {
            return new StaticMessageSource();
        }

        @Bean
        ModerationService moderationService() {
            return Mockito.mock(ModerationService.class);
        }

        @Bean
        TestRoutes testRoutes() {
            return new TestRoutes();
        }
    }

    @Controller
    static class TestRoutes {

        @GetMapping("/host/tournaments/new")
        @ResponseBody
        String hostTournamentCreate() {
            return "host";
        }

        @GetMapping("/admin/reports")
        @ResponseBody
        String adminReports() {
            return "admin";
        }

        @GetMapping("/moderation/queue")
        @ResponseBody
        String moderationQueue() {
            return "moderation";
        }

        @GetMapping("/reports/mine")
        @ResponseBody
        String myReports() {
            return "my-reports";
        }

        @GetMapping("/reports/users/{username}")
        @ResponseBody
        String reportUser(@PathVariable("username") final String username) {
            return "report-" + username;
        }

        @PostMapping("/matches/{matchId}/invites/accept")
        @ResponseBody
        String acceptInvite(@PathVariable("matchId") final Long matchId) {
            return "accepted-" + matchId;
        }

        @GetMapping("/users/{username}")
        @ResponseBody
        String publicProfile(@PathVariable("username") final String username) {
            return username;
        }
    }
}
