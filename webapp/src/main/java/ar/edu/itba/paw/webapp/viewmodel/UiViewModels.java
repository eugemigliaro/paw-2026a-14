package ar.edu.itba.paw.webapp.viewmodel;

import java.util.List;
import java.util.Map;

public final class UiViewModels {

    private UiViewModels() {
        // Utility holder for immutable UI-only view models.
    }

    public static final class FilterGroupViewModel {
        private final String titleCode;
        private final List<FilterOptionViewModel> options;

        public FilterGroupViewModel(
                final String titleCode, final List<FilterOptionViewModel> options) {
            this.titleCode = titleCode;
            this.options = options;
        }

        public String getTitleCode() {
            return titleCode;
        }

        public List<FilterOptionViewModel> getOptions() {
            return options;
        }
    }

    public static final class FilterOptionViewModel {
        private final String labelCode;
        private final String href;
        private final Map<String, String> params;
        private final String meta;
        private final boolean active;

        public FilterOptionViewModel(
                final String labelCode,
                final String href,
                final Map<String, String> params,
                final String meta,
                final boolean active) {
            this.labelCode = labelCode;
            this.href = href;
            this.params = params;
            this.meta = meta;
            this.active = active;
        }

        public String getLabelCode() {
            return labelCode;
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
        private final String labelCode;
        private final String href;
        private final Map<String, String> params;
        private final boolean selected;

        public SelectOptionViewModel(
                final String labelCode,
                final String href,
                final Map<String, String> params,
                final boolean selected) {
            this.labelCode = labelCode;
            this.href = href;
            this.params = params;
            this.selected = selected;
        }

        public String getLabelCode() {
            return labelCode;
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
        private final String searchQuery;
        private final List<SelectOptionViewModel> sortOptions;
        private final List<FilterGroupViewModel> filterGroups;

        public MatchListControlsViewModel(
                final String cleanSearchAction,
                final String searchAction,
                final String searchQuery,
                final List<SelectOptionViewModel> sortOptions,
                final List<FilterGroupViewModel> filterGroups) {
            this.cleanSearchAction = cleanSearchAction;
            this.searchAction = searchAction;
            this.searchQuery = searchQuery;
            this.sortOptions = sortOptions;
            this.filterGroups = filterGroups;
        }

        public String getCleanSearchAction() {
            return cleanSearchAction;
        }

        public String getSearchAction() {
            return searchAction;
        }

        public String getSearchQuery() {
            return searchQuery;
        }

        public List<SelectOptionViewModel> getSortOptions() {
            return sortOptions;
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
