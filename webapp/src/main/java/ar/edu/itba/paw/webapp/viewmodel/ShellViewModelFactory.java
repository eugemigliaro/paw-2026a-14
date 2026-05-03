package ar.edu.itba.paw.webapp.viewmodel;

import ar.edu.itba.paw.webapp.utils.UrlUtils;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.NavItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ShellViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class ShellViewModelFactory {

    private ShellViewModelFactory() {
        // Utility factory for shared page shell models.
    }

    public static ShellViewModel playerShell(final MessageSource ms, final Locale locale) {
        return playerShell(ms, locale, "/");
    }

    public static ShellViewModel playerShell(
            final MessageSource ms, final Locale locale, final String activePath) {

        List<NavItemViewModel> navItems =
                List.of(
                        new NavItemViewModel(
                                ms.getMessage("nav.explore", null, locale),
                                UrlUtils.withLang("/", locale),
                                "/".equals(activePath)));
        if (isAuthenticated()) {
            final ArrayList<NavItemViewModel> authenticatedNavItems =
                    new ArrayList<>(
                            List.of(
                                    new NavItemViewModel(
                                            ms.getMessage("nav.explore", null, locale),
                                            UrlUtils.withLang("/", locale),
                                            "/".equals(activePath)),
                                    new NavItemViewModel(
                                            ms.getMessage("nav.player.events", null, locale),
                                            UrlUtils.withLang("/events", locale),
                                            "/events".equals(activePath)),
                                    new NavItemViewModel(
                                            ms.getMessage("nav.player.reports", null, locale),
                                            UrlUtils.withLang("/reports/mine", locale),
                                            "/reports/mine".equals(activePath))));
            if (hasRole("ROLE_ADMIN_MOD")) {
                authenticatedNavItems.add(
                        new NavItemViewModel(
                                ms.getMessage("nav.admin.reports", null, locale),
                                UrlUtils.withLang("/admin/reports", locale),
                                "/admin/reports".equals(activePath)));
            }
            navItems = authenticatedNavItems;
        }

        return new ShellViewModel(
                ms.getMessage("app.brand", null, locale),
                null,
                isAuthenticated()
                        ? new NavItemViewModel(
                                ms.getMessage("nav.hostAMatch", null, locale),
                                UrlUtils.withLang("/host/matches/new", locale),
                                "/host/matches/new".equals(activePath))
                        : null,
                navItems);
    }

    public static ShellViewModel hostShell(final MessageSource ms, final Locale locale) {
        return hostShell(ms, locale, "/host/matches/new");
    }

    public static ShellViewModel hostShell(
            final MessageSource ms, final Locale locale, final String activePath) {

        List<NavItemViewModel> navItems =
                List.of(
                        new NavItemViewModel(
                                ms.getMessage("nav.host.createMatch", null, locale),
                                UrlUtils.withLang("/host/matches/new", locale),
                                "/host/matches/new".equals(activePath)));
        if (isAuthenticated()) {
            navItems =
                    List.of(
                            new NavItemViewModel(
                                    ms.getMessage("nav.host.createMatch", null, locale),
                                    UrlUtils.withLang("/host/matches/new", locale),
                                    "/host/matches/new".equals(activePath)),
                            new NavItemViewModel(
                                    ms.getMessage("nav.host.joinRequests", null, locale),
                                    UrlUtils.withLang("/host/requests", locale),
                                    "/host/requests".equals(activePath)));
        }

        return new ShellViewModel(
                ms.getMessage("app.brand", null, locale),
                new NavItemViewModel(
                        ms.getMessage("nav.switchToPlayer", null, locale),
                        UrlUtils.withLang("/", locale),
                        false),
                navItems);
    }

    private static boolean isAuthenticated() {
        final Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private static boolean hasRole(final String role) {
        final Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
