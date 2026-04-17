package ar.edu.itba.paw.webapp.viewmodel;

import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.NavItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ShellViewModel;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class ShellViewModelFactory {

    private ShellViewModelFactory() {
        // Utility factory for shared page shell models.
    }

    public static ShellViewModel browseShell(final MessageSource ms, final Locale locale) {
        return new ShellViewModel(
                ms.getMessage("app.brand", null, locale),
                isAuthenticated()
                        ? new NavItemViewModel(
                                ms.getMessage("nav.switchToHosting", null, locale),
                                "/host/matches/new",
                                false)
                        : null,
                List.of());
    }

    public static ShellViewModel hostShell(final MessageSource ms, final Locale locale) {
        return new ShellViewModel(
                ms.getMessage("app.brand", null, locale),
                new NavItemViewModel(ms.getMessage("nav.switchToPlayer", null, locale), "/", false),
                List.of());
    }

    private static boolean isAuthenticated() {
        final Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
