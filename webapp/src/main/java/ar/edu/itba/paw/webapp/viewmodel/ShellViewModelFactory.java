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
        return new ShellViewModel(
                ms.getMessage("app.brand", null, locale),
                new NavItemViewModel(
                        ms.getMessage("nav.switchToHosting", null, locale),
                        "/host/matches/new",
                        false),
                List.of());
    }

    public static ShellViewModel hostShell(final MessageSource ms, final Locale locale) {
        return new ShellViewModel(
                ms.getMessage("app.brand", null, locale),
                new NavItemViewModel(ms.getMessage("nav.switchToPlayer", null, locale), "/", false),
                List.of());
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
                        urlWithIdentity("/player/matches/upcoming", email, locale),
                        false),
                List.of(
                        new NavItemViewModel(
                                ms.getMessage("nav.host.allEvents", null, locale),
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
                        urlWithIdentity("/host/matches", email, locale),
                        false),
                List.of(
                        new NavItemViewModel(
                                ms.getMessage("nav.player.upcomingEvents", null, locale),
                                urlWithIdentity("/player/matches/upcoming", email, locale),
                                "/player/matches/upcoming".equals(activePath)),
                        new NavItemViewModel(
                                ms.getMessage("nav.player.pastEvents", null, locale),
                                urlWithIdentity("/player/matches/past", email, locale),
                                "/player/matches/past".equals(activePath))));
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
