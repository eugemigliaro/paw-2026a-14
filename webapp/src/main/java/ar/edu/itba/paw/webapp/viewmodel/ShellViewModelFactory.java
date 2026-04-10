package ar.edu.itba.paw.webapp.viewmodel;

import ar.edu.itba.paw.webapp.viewmodel.WebViewModels.NavItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.WebViewModels.ShellViewModel;
import java.util.List;

public final class ShellViewModelFactory {

    private ShellViewModelFactory() {
        // Utility factory for shared page shell models.
    }

    public static ShellViewModel browseShell() {
        return new ShellViewModel(
                "Match Point",
                new NavItemViewModel("Switch to Hosting", "/host/events/new", false),
                List.of());
    }

    public static ShellViewModel hostShell() {
        return new ShellViewModel(
                "Match Point", new NavItemViewModel("Switch to Player", "/", false), List.of());
    }
}
