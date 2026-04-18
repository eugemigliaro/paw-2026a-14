package ar.edu.itba.paw.webapp.viewmodel;

import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.NavItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ShellViewModel;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

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
                                withLang("/", locale),
                                "/".equals(activePath)));
        if (isAuthenticated()) {
            navItems =
                    List.of(
                            new NavItemViewModel(
                                    ms.getMessage("nav.explore", null, locale),
                                    withLang("/", locale),
                                    "/".equals(activePath)),
                            new NavItemViewModel(
                                    ms.getMessage("nav.player.pastEvents", null, locale),
                                    withLang("/player/matches/past", locale),
                                    "/player/matches/past".equals(activePath)),
                            new NavItemViewModel(
                                    ms.getMessage("nav.player.upcomingEvents", null, locale),
                                    withLang("/player/matches/upcoming", locale),
                                    "/player/matches/upcoming".equals(activePath)));
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

        List<NavItemViewModel> navItems =
                List.of(
                        new NavItemViewModel(
                                ms.getMessage("nav.host.createMatch", null, locale),
                                withLang("/host/matches/new", locale),
                                "/host/matches/new".equals(activePath)));
        if (isAuthenticated()) {
            navItems =
                    List.of(
                            new NavItemViewModel(
                                    ms.getMessage("nav.host.createMatch", null, locale),
                                    withLang("/host/matches/new", locale),
                                    "/host/matches/new".equals(activePath)),
                            new NavItemViewModel(
                                    ms.getMessage("nav.host.upcomingEvents", null, locale),
                                    withLang("/host/matches", locale),
                                    "/host/matches".equals(activePath)),
                            new NavItemViewModel(
                                    ms.getMessage("nav.host.finishedEvents", null, locale),
                                    withLang("/host/matches/finished", locale),
                                    "/host/matches/finished".equals(activePath)));
        }

        return new ShellViewModel(
                ms.getMessage("app.brand", null, locale),
                new NavItemViewModel(
                        ms.getMessage("nav.switchToPlayer", null, locale),
                        withLang(activePath, locale),
                        false),
                navItems);
    }

    private static String withLang(final String path, final Locale locale) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        if (locale != null && locale.getLanguage() != null && !locale.getLanguage().isBlank()) {
            builder.queryParam("lang", locale.getLanguage());
        }
        return builder.build().encode().toUriString();
    }

    private static boolean isAuthenticated() {
        final Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
