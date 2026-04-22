package ar.edu.itba.paw.webapp.viewmodel;

import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.NavItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ShellViewModel;
import java.util.List;
import java.util.Locale;
import ar.edu.itba.paw.webapp.utils.UrlUtils;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
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

                List<NavItemViewModel> navItems = List.of(
                                new NavItemViewModel(
                                                ms.getMessage("nav.explore", null, locale),
                                                "/",
                                                "/".equals(activePath)));
                if (isAuthenticated()) {
                        navItems = List.of(
                                        new NavItemViewModel(
                                                        ms.getMessage("nav.explore", null, locale),
                                                        UrlUtils.withLang("/", locale),
                                                        "/".equals(activePath)),
                                        new NavItemViewModel(
                                                        ms.getMessage("nav.player.upcomingEvents", null, locale),
                                                        UrlUtils.withLang("/player/matches/upcoming", locale),
                                                        "/player/matches/upcoming".equals(activePath)),
                                        new NavItemViewModel(
                                                        ms.getMessage("nav.player.pendingRequests", null, locale),
                                                        UrlUtils.withLang("/player/matches/requests", locale),
                                                        "/player/matches/requests".equals(activePath)),
                                        new NavItemViewModel(
                                                        ms.getMessage("nav.player.invites", null, locale),
                                                        UrlUtils.withLang("/player/matches/invites", locale),
                                                        "/player/matches/invites".equals(activePath)),
                                        new NavItemViewModel(
                                                        ms.getMessage("nav.player.pastEvents", null, locale),
                                                        UrlUtils.withLang("/player/matches/past", locale),
                                                        "/player/matches/past".equals(activePath)));
                }

                return new ShellViewModel(
                                ms.getMessage("app.brand", null, locale),
                                isAuthenticated()
                                                ? new NavItemViewModel(
                                                                ms.getMessage("nav.switchToHosting", null, locale),
                                                                "/host/matches/new",
                                                                false)
                                                : null,
                                navItems);
        }

        public static ShellViewModel hostShell(final MessageSource ms, final Locale locale) {
                return hostShell(ms, locale, "/host/matches/new");
        }

        public static ShellViewModel hostShell(
                        final MessageSource ms, final Locale locale, final String activePath) {

                List<NavItemViewModel> navItems = List.of(
                                new NavItemViewModel(
                                                ms.getMessage("nav.host.createMatch", null, locale),
                                                "/host/matches/new",
                                                "/host/matches/new".equals(activePath)));
                if (isAuthenticated()) {
                        navItems = List.of(
                                        new NavItemViewModel(
                                                        ms.getMessage("nav.host.createMatch", null, locale),
                                                        "/host/matches/new",
                                                        "/host/matches/new".equals(activePath)),
                                        new NavItemViewModel(
                                                        ms.getMessage("nav.host.upcomingEvents", null, locale),
                                                        "/host/matches",
                                                        "/host/matches".equals(activePath)),
                                        new NavItemViewModel(
                                                        ms.getMessage("nav.host.finishedEvents", null, locale),
                                                        "/host/matches/finished",
                                                        "/host/matches/finished".equals(activePath)));
                }

                return new ShellViewModel(
                                ms.getMessage("app.brand", null, locale),
                                new NavItemViewModel(ms.getMessage("nav.switchToPlayer", null, locale), "/", false),
                                navItems);
        }

        private static boolean isAuthenticated() {
                final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                return authentication != null
                                && authentication.isAuthenticated()
                                && !(authentication instanceof AnonymousAuthenticationToken);
        }
}
