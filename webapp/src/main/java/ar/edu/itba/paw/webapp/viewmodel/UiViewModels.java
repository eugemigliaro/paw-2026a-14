package ar.edu.itba.paw.webapp.viewmodel;

import java.util.List;
import java.util.Map;

public final class UiViewModels {

    private UiViewModels() {
        // Utility holder for immutable UI-only view models.
    }

    public static final class FilterGroupViewModel {
        private final String title;
        private final List<FilterOptionViewModel> options;

        public FilterGroupViewModel(final String title, final List<FilterOptionViewModel> options) {
            this.title = title;
            this.options = options;
        }

        public String getTitle() {
            return title;
        }

        public List<FilterOptionViewModel> getOptions() {
            return options;
        }
    }

    public static final class FilterOptionViewModel {
        private final String label;
        private final String href;
        private final Map<String, String> params;
        private final String meta;
        private final boolean active;

        public FilterOptionViewModel(
                final String label,
                final String href,
                final Map<String, String> params,
                final String meta,
                final boolean active) {
            this.label = label;
            this.href = href;
            this.params = params;
            this.meta = meta;
            this.active = active;
        }

        public String getLabel() {
            return label;
        }

        public String getHref() {
            return href;
        }

        public Map<String, String> getParams() {
            return params;
        }

        public String getMeta() {
            return meta;
        }

        public boolean isActive() {
            return active;
        }
    }

    public static final class SelectOptionViewModel {
        private final String label;
        private final String href;
        private final Map<String, String> params;
        private final boolean selected;

        public SelectOptionViewModel(
                final String label,
                final String href,
                final Map<String, String> params,
                final boolean selected) {
            this.label = label;
            this.href = href;
            this.params = params;
            this.selected = selected;
        }

        public String getLabel() {
            return label;
        }

        public String getHref() {
            return href;
        }

        public Map<String, String> getParams() {
            return params;
        }

        public boolean isSelected() {
            return selected;
        }
    }

    public static final class MatchListControlsViewModel {
        private final String cleanSearchAction;
        private final String searchAction;
        private final String searchLabel;
        private final String searchQuery;
        private final String searchPlaceholder;
        private final String searchButtonLabel;
        private final String sortLabel;
        private final List<SelectOptionViewModel> sortOptions;
        private final String filterTitle;
        private final List<FilterGroupViewModel> filterGroups;

        public MatchListControlsViewModel(
                final String cleanSearchAction,
                final String searchAction,
                final String searchLabel,
                final String searchQuery,
                final String searchPlaceholder,
                final String searchButtonLabel,
                final String sortLabel,
                final List<SelectOptionViewModel> sortOptions,
                final String filterTitle,
                final List<FilterGroupViewModel> filterGroups) {
            this.cleanSearchAction = cleanSearchAction;
            this.searchAction = searchAction;
            this.searchLabel = searchLabel;
            this.searchQuery = searchQuery;
            this.searchPlaceholder = searchPlaceholder;
            this.searchButtonLabel = searchButtonLabel;
            this.sortLabel = sortLabel;
            this.sortOptions = sortOptions;
            this.filterTitle = filterTitle;
            this.filterGroups = filterGroups;
        }

        public String getCleanSearchAction() {
            return cleanSearchAction;
        }

        public String getSearchAction() {
            return searchAction;
        }

        public String getSearchLabel() {
            return searchLabel;
        }

        public String getSearchQuery() {
            return searchQuery;
        }

        public String getSearchPlaceholder() {
            return searchPlaceholder;
        }

        public String getSearchButtonLabel() {
            return searchButtonLabel;
        }

        public String getSortLabel() {
            return sortLabel;
        }

        public List<SelectOptionViewModel> getSortOptions() {
            return sortOptions;
        }

        public String getFilterTitle() {
            return filterTitle;
        }

        public List<FilterGroupViewModel> getFilterGroups() {
            return filterGroups;
        }
    }

    public static final class PaginationItemViewModel {
        private final String label;
        private final String href;
        private final boolean current;
        private final boolean ellipsis;

        public PaginationItemViewModel(
                final String label,
                final String href,
                final boolean current,
                final boolean ellipsis) {
            this.label = label;
            this.href = href;
            this.current = current;
            this.ellipsis = ellipsis;
        }

        public String getLabel() {
            return label;
        }

        public String getHref() {
            return href;
        }

        public boolean isCurrent() {
            return current;
        }

        public boolean isEllipsis() {
            return ellipsis;
        }
    }
}
