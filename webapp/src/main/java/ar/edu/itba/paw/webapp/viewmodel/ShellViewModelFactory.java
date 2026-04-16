package ar.edu.itba.paw.webapp.viewmodel;

import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.NavItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ShellViewModel;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;

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
}
