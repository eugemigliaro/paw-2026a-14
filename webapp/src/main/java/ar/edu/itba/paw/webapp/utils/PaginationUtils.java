package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PaginationItemViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public final class PaginationUtils {

    private PaginationUtils() {}

    public static List<PaginationItemViewModel> buildPaginationItems(
            final int currentPage, final int totalPages, final IntFunction<String> hrefForPage) {
        if (totalPages <= 1) {
            return List.of();
        }

        final List<PaginationItemViewModel> items = new ArrayList<>();
        final int startPage = Math.max(2, Math.min(currentPage - 1, totalPages - 3));
        final int endPage = Math.min(totalPages - 1, Math.max(currentPage + 1, 4));

        items.add(pageItem(1, currentPage, hrefForPage));
        if (startPage > 2) {
            items.add(new PaginationItemViewModel("...", null, false, true));
        }
        for (int page = startPage; page <= endPage; page++) {
            items.add(pageItem(page, currentPage, hrefForPage));
        }
        if (endPage < totalPages - 1) {
            items.add(new PaginationItemViewModel("...", null, false, true));
        }
        items.add(pageItem(totalPages, currentPage, hrefForPage));

        return items;
    }

    private static PaginationItemViewModel pageItem(
            final int page, final int currentPage, final IntFunction<String> hrefForPage) {
        return new PaginationItemViewModel(
                Integer.toString(page), hrefForPage.apply(page), page == currentPage, false);
    }
}
