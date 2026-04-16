package ar.edu.itba.paw.webapp.viewmodel;

import ar.edu.itba.paw.webapp.viewmodel.WebViewModels.NavItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.WebViewModels.ShellViewModel;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;

public final class ShellViewModelFactory {

    private ShellViewModelFactory() {
        // Utility factory for shared page shell models.
    }

    public static ShellViewModel browseShell(final MessageSource ms, final Locale locale) {
        final Locale resolvedLocale = locale == null ? Locale.ENGLISH : locale;
        return new ShellViewModel(
                ms.getMessage("app.brand", null, resolvedLocale),
                new NavItemViewModel(
                        ms.getMessage("nav.switchToHosting", null, resolvedLocale),
                        "/host/matches/new",
                        false),
                List.of());
    }

    public static ShellViewModel hostShell(final MessageSource ms, final Locale locale) {
        final Locale resolvedLocale = locale == null ? Locale.ENGLISH : locale;
        return new ShellViewModel(
                ms.getMessage("app.brand", null, resolvedLocale),
                new NavItemViewModel(
                        ms.getMessage("nav.switchToPlayer", null, resolvedLocale), "/", false),
                List.of());
    }
}
