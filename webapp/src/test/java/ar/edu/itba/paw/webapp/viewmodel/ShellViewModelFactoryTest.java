package ar.edu.itba.paw.webapp.viewmodel;

import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.NavItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.ShellViewModel;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ShellViewModelFactoryTest {

    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        messageSource = messageSource();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void playerShellForAuthenticatedUserMovesReportLinkToSettingsMenu() {
        // 1. Arrange
        authenticate("ROLE_USER");

        // 2. Exercise
        final ShellViewModel shell =
                ShellViewModelFactory.playerShell(messageSource, Locale.ENGLISH, "/reports/mine");

        // 3. Assert
        Assertions.assertEquals(List.of("Explore", "My matches"), labels(shell.getPrimaryNav()));
        Assertions.assertEquals(
                List.of("Profile", "My reports"), labels(shell.getSettingsMenuItems()));
        Assertions.assertFalse(hrefs(shell.getPrimaryNav()).contains("/reports/mine?lang=en"));
        Assertions.assertTrue(
                hrefs(shell.getSettingsMenuItems()).contains("/reports/mine?lang=en"));
    }

    @Test
    void playerShellForAdminUserIncludesAdminReportsOnlyInSettingsMenu() {
        // 1. Arrange
        authenticate("ROLE_ADMIN_MOD");

        // 2. Exercise
        final ShellViewModel shell =
                ShellViewModelFactory.playerShell(messageSource, Locale.ENGLISH, "/admin/reports");

        // 3. Assert
        Assertions.assertEquals(List.of("Explore", "My matches"), labels(shell.getPrimaryNav()));
        Assertions.assertEquals(
                List.of("Profile", "My reports", "Admin reports"),
                labels(shell.getSettingsMenuItems()));
        Assertions.assertFalse(hrefs(shell.getPrimaryNav()).contains("/admin/reports?lang=en"));
        Assertions.assertTrue(
                hrefs(shell.getSettingsMenuItems()).contains("/admin/reports?lang=en"));
    }

    @Test
    void playerShellForUnauthenticatedUserKeepsSettingsMenuEmpty() {
        // 1. Arrange
        SecurityContextHolder.clearContext();

        // 2. Exercise
        final ShellViewModel shell =
                ShellViewModelFactory.playerShell(messageSource, Locale.ENGLISH, "/");

        // 3. Assert
        Assertions.assertEquals(List.of("Explore"), labels(shell.getPrimaryNav()));
        Assertions.assertTrue(shell.getSettingsMenuItems().isEmpty());
        Assertions.assertNull(shell.getHostMatchNav());
    }

    private static void authenticate(final String role) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "user", null, List.of(new SimpleGrantedAuthority(role))));
    }

    private static List<String> labels(final List<NavItemViewModel> items) {
        return items.stream().map(NavItemViewModel::getLabel).toList();
    }

    private static List<String> hrefs(final List<NavItemViewModel> items) {
        return items.stream().map(NavItemViewModel::getHref).toList();
    }

    private static MessageSource messageSource() {
        final StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("app.brand", Locale.ENGLISH, "Match Point");
        messageSource.addMessage("nav.explore", Locale.ENGLISH, "Explore");
        messageSource.addMessage("nav.player.events", Locale.ENGLISH, "My matches");
        messageSource.addMessage("nav.player.reports", Locale.ENGLISH, "My reports");
        messageSource.addMessage("nav.admin.reports", Locale.ENGLISH, "Admin reports");
        messageSource.addMessage("nav.profile", Locale.ENGLISH, "Profile");
        messageSource.addMessage("nav.hostAMatch", Locale.ENGLISH, "Host a match");
        return messageSource;
    }
}
