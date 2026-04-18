package ar.edu.itba.paw.webapp.viewmodel;

import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.NavItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ShellViewModel;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.web.util.UriComponentsBuilder;

public final class ShellViewModelFactory {

    private ShellViewModelFactory() {
        // Utility factory for shared page shell models.
    }

    public static ShellViewModel browseShell(final MessageSource ms, final Locale locale) {
        return browseShell(ms, locale, null, "/");
    }

    public static ShellViewModel browseShell(
            final MessageSource ms,
            final Locale locale,
            final String email,
            final String activePath) {
        return new ShellViewModel(
                ms.getMessage("app.brand", null, locale),
                new NavItemViewModel(
                        ms.getMessage("nav.switchToHosting", null, locale),
                        withOptionalIdentity("/host/matches", "/host/matches/new", email, locale),
                        false),
                List.of(
                        new NavItemViewModel(
                                ms.getMessage("nav.explore", null, locale),
                                withLang("/", locale),
                                "/".equals(activePath)),
                        new NavItemViewModel(
                                ms.getMessage("nav.player.pastEvents", null, locale),
                                withOptionalIdentity("/player/matches/past", "/", email, locale),
                                "/player/matches/past".equals(activePath)),
                        new NavItemViewModel(
                                ms.getMessage("nav.player.upcomingEvents", null, locale),
                                withOptionalIdentity(
                                        "/player/matches/upcoming", "/", email, locale),
                                "/player/matches/upcoming".equals(activePath))));
    }

    public static ShellViewModel hostShell(final MessageSource ms, final Locale locale) {
        return hostShell(ms, locale, null, "/host/matches/new");
    }

    public static ShellViewModel hostShell(
            final MessageSource ms,
            final Locale locale,
            final String email,
            final String activePath) {
        return new ShellViewModel(
                ms.getMessage("app.brand", null, locale),
                new NavItemViewModel(
                        ms.getMessage("nav.switchToPlayer", null, locale),
                        withOptionalIdentity("/", "/", email, locale),
                        false),
                List.of(
                        new NavItemViewModel(
                                ms.getMessage("nav.host.createMatch", null, locale),
                                withLang("/host/matches/new", locale),
                                "/host/matches/new".equals(activePath))));
    }

    public static ShellViewModel hostDashboardShell(
            final MessageSource ms,
            final Locale locale,
            final String email,
            final String activePath) {
        return new ShellViewModel(
                ms.getMessage("app.brand", null, locale),
                new NavItemViewModel(
                        ms.getMessage("nav.switchToPlayer", null, locale),
                        withOptionalIdentity("/", "/", email, locale),
                        false),
                List.of(
                        new NavItemViewModel(
                                ms.getMessage("nav.host.createMatch", null, locale),
                                withLang("/host/matches/new", locale),
                                "/host/matches/new".equals(activePath)),
                        new NavItemViewModel(
                                ms.getMessage("nav.host.upcomingEvents", null, locale),
                                urlWithIdentity("/host/matches", email, locale),
                                "/host/matches".equals(activePath)),
                        new NavItemViewModel(
                                ms.getMessage("nav.host.finishedEvents", null, locale),
                                urlWithIdentity("/host/matches/finished", email, locale),
                                "/host/matches/finished".equals(activePath))));
    }

    public static ShellViewModel playerDashboardShell(
            final MessageSource ms,
            final Locale locale,
            final String email,
            final String activePath) {
        return new ShellViewModel(
                ms.getMessage("app.brand", null, locale),
                new NavItemViewModel(
                        ms.getMessage("nav.switchToHosting", null, locale),
                        withOptionalIdentity("/host/matches", "/host/matches/new", email, locale),
                        false),
                List.of(
                        new NavItemViewModel(
                                ms.getMessage("nav.explore", null, locale),
                                withLang("/", locale),
                                "/".equals(activePath)),
                        new NavItemViewModel(
                                ms.getMessage("nav.player.upcomingEvents", null, locale),
                                urlWithIdentity("/player/matches/upcoming", email, locale),
                                "/player/matches/upcoming".equals(activePath)),
                        new NavItemViewModel(
                                ms.getMessage("nav.player.pastEvents", null, locale),
                                urlWithIdentity("/player/matches/past", email, locale),
                                "/player/matches/past".equals(activePath))));
    }

    private static String withOptionalIdentity(
            final String identityPath,
            final String fallbackPath,
            final String email,
            final Locale locale) {
        if (email == null || email.isBlank()) {
            return withLang(fallbackPath, locale);
        }
        return urlWithIdentity(identityPath, email, locale);
    }

    private static String withLang(final String path, final Locale locale) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        if (locale != null && locale.getLanguage() != null && !locale.getLanguage().isBlank()) {
            builder.queryParam("lang", locale.getLanguage());
        }
        return builder.build().encode().toUriString();
    }

    private static String urlWithIdentity(
            final String path, final String email, final Locale locale) {
        final UriComponentsBuilder builder =
                UriComponentsBuilder.fromPath(path).queryParam("email", email);
        if (locale != null && locale.getLanguage() != null && !locale.getLanguage().isBlank()) {
            builder.queryParam("lang", locale.getLanguage());
        }
        return builder.build().encode().toUriString();
    }
}
