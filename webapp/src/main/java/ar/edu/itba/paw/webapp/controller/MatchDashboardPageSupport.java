package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.encodeCsv;
import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.toggleValue;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventCategory;
import ar.edu.itba.paw.models.query.EventFilter;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventType;
import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.webapp.form.SearchForm;
import ar.edu.itba.paw.webapp.utils.EventCardAttributeUtils;
import ar.edu.itba.paw.webapp.utils.PaginationUtils;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FilterGroupViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FilterOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.MatchListControlsViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PaginationItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.SelectOptionViewModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

final class MatchDashboardPageSupport {

    private static final List<EventStatus> PLAYER_STATUS_OPTIONS =
            List.of(EventStatus.OPEN, EventStatus.COMPLETED, EventStatus.CANCELLED);

    private MatchDashboardPageSupport() {}

    static ModelAndView buildListPage(
            final User currentUser,
            final String view,
            final String path,
            final String pageTitleCode,
            final MatchDashboardQueryState.DashboardSelection selection,
            final PaginatedResult<Match> result,
            final PaginatedResult<Tournament> tournamentResult,
            final MatchParticipationService matchParticipationService,
            final MatchReservationService matchReservationService,
            final TournamentService tournamentService) {
        final ModelAndView mav = new ModelAndView(view);
        final SearchForm searchForm = selection.searchForm();
        final EventType selectedType = searchForm.getType();
        final String selectedTypeStr = selectedType.getDbValue();
        final EventFilter selectedFilter =
                searchForm.getFilter() == null ? EventFilter.UPCOMING : searchForm.getFilter();
        final String selectedFilterValue = selectedFilter.name();
        final String searchQuery = searchForm.getQ();
        final String sort = searchForm.getSort().getQueryValue();
        final String startDate =
                searchForm.getStartDate() == null ? null : searchForm.getStartDate().toString();
        final String endDate =
                searchForm.getEndDate() == null ? null : searchForm.getEndDate().toString();
        final BigDecimal minPrice = searchForm.getMinPrice();
        final BigDecimal maxPrice = searchForm.getMaxPrice();
        final List<String> selectedStatusesStr = toDbValues(searchForm.getStatus());
        final List<String> selectedSportsStr = toDbValues(searchForm.getSport());
        final List<String> selectedVisibilityStr = toDbValues(searchForm.getVisibility());
        final List<String> selectedCategories =
                searchForm.getCategory().stream().map(EventCategory::getQueryValue).toList();
        final DateRangeBounds dateBounds = dateRangeBounds(selectedFilter);

        // listTitle (events.title) is resolved in events/list.jsp via <spring:message>;
        // description/empty copy is not rendered on this view.
        mav.addObject("pageTitleCode", pageTitleCode);
        mav.addObject("selectedType", selectedTypeStr);
        mav.addObject("selectedSort", sort);
        mav.addObject("selectedStartDateValue", startDate);
        mav.addObject("selectedEndDateValue", endDate);
        mav.addObject("selectedDateMinValue", dateBounds.minDate());
        mav.addObject("selectedDateMaxValue", dateBounds.maxDate());
        mav.addObject("selectedSports", selectedSportsStr);
        mav.addObject("selectedStatuses", selectedStatusesStr);
        mav.addObject("selectedVisibility", selectedVisibilityStr);
        mav.addObject("selectedCategories", selectedCategories);
        mav.addObject("selectedMinPriceValue", formatNullablePriceValue(minPrice));
        mav.addObject("selectedMaxPriceValue", formatNullablePriceValue(maxPrice));
        mav.addObject("searchForm", searchForm);
        mav.addObject(
                "listControls",
                buildListControls(
                        path,
                        selectedType,
                        selectedFilterValue,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        selectedStatusesStr,
                        selectedSportsStr,
                        selectedVisibilityStr,
                        selectedCategories));
        mav.addObject(
                "events",
                selectedType == EventType.TOURNAMENT
                        ? tournamentResult.getItems()
                        : result.getItems());
        mav.addObject("eventType", selectedType);
        mav.addObject(
                "eventBadgeCodes",
                selectedType == EventType.TOURNAMENT
                        ? EventCardAttributeUtils.tournamentBadgeCodes(tournamentResult.getItems())
                        : EventCardAttributeUtils.matchStatusBadgeCodes(result.getItems()));
        mav.addObject(
                "eventRelationshipBadgeCodes",
                selectedType == EventType.TOURNAMENT
                        ? EventCardAttributeUtils.tournamentRelationshipBadgeCodes(
                                tournamentResult.getItems(),
                                currentUser,
                                tournamentService.findParticipatingTournamentIds(
                                        currentUser,
                                        tournamentResult.getItems().stream()
                                                .map(Tournament::getId)
                                                .toList()))
                        : EventCardAttributeUtils.matchRelationshipBadgeCodes(
                                result.getItems(),
                                currentUser,
                                matchParticipationService,
                                matchReservationService));
        final PaginatedResult<?> pageResult =
                selectedType == EventType.TOURNAMENT ? tournamentResult : result;
        mav.addObject("pageResult", pageResult);
        mav.addObject("pageHasPrevious", pageResult != null && pageResult.hasPrevious());
        mav.addObject("pageHasNext", pageResult != null && pageResult.hasNext());
        mav.addObject("pageNumber", pageResult == null ? 1 : pageResult.getPage());
        mav.addObject(
                "paginationItems",
                buildPagination(
                        path,
                        selectedType,
                        selectedFilterValue,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        selectedStatusesStr,
                        selectedSportsStr,
                        selectedVisibilityStr,
                        selectedCategories,
                        pageResult));
        return mav;
    }

    private static MatchListControlsViewModel buildListControls(
            final String path,
            final EventType selectedType,
            final String selectedFilter,
            final String searchQuery,
            final String sort,
            final String selectedStartDate,
            final String selectedEndDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories) {
        final List<FilterGroupViewModel> filterGroups = new ArrayList<>();
        if ("/events".equals(path)) {
            filterGroups.add(
                    new FilterGroupViewModel(
                            "filter.eventType",
                            buildEventTypeFilterOptions(
                                    path,
                                    selectedType,
                                    selectedFilter,
                                    searchQuery,
                                    sort,
                                    selectedStartDate,
                                    selectedEndDate,
                                    minPrice,
                                    maxPrice,
                                    selectedStatuses,
                                    selectedSports,
                                    selectedVisibility,
                                    selectedCategories)));
        }
        filterGroups.add(
                new FilterGroupViewModel(
                        "filter.categories",
                        buildSportFilterOptions(
                                path,
                                selectedType,
                                selectedFilter,
                                searchQuery,
                                sort,
                                selectedStartDate,
                                selectedEndDate,
                                minPrice,
                                maxPrice,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                selectedCategories)));

        if ("/events".equals(path) && selectedType != EventType.TOURNAMENT) {
            filterGroups.add(
                    new FilterGroupViewModel(
                            "filter.category",
                            buildCategoryFilterOptions(
                                    path,
                                    selectedType,
                                    selectedFilter,
                                    searchQuery,
                                    sort,
                                    selectedStartDate,
                                    selectedEndDate,
                                    minPrice,
                                    maxPrice,
                                    selectedStatuses,
                                    selectedSports,
                                    selectedVisibility,
                                    selectedCategories)));
        }

        if (selectedType != EventType.TOURNAMENT) {
            filterGroups.add(
                    new FilterGroupViewModel(
                            "host.filters.status",
                            buildStatusFilterOptions(
                                    path,
                                    selectedType,
                                    selectedFilter,
                                    searchQuery,
                                    sort,
                                    selectedStartDate,
                                    selectedEndDate,
                                    minPrice,
                                    maxPrice,
                                    selectedStatuses,
                                    selectedSports,
                                    selectedVisibility,
                                    selectedCategories,
                                    PLAYER_STATUS_OPTIONS.stream()
                                            .map(EventStatus::getDbValue)
                                            .toList())));
        }

        final List<SelectOptionViewModel> sortOptions =
                List.of(
                        sortOption(
                                path,
                                selectedType,
                                selectedFilter,
                                searchQuery,
                                selectedStartDate,
                                selectedEndDate,
                                minPrice,
                                maxPrice,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                selectedCategories,
                                EventSort.SOONEST.getQueryValue(),
                                sort,
                                "feed.sort.soonest"),
                        sortOption(
                                path,
                                selectedType,
                                selectedFilter,
                                searchQuery,
                                selectedStartDate,
                                selectedEndDate,
                                minPrice,
                                maxPrice,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                selectedCategories,
                                EventSort.PRICE_LOW.getQueryValue(),
                                sort,
                                "feed.sort.price"),
                        sortOption(
                                path,
                                selectedType,
                                selectedFilter,
                                searchQuery,
                                selectedStartDate,
                                selectedEndDate,
                                minPrice,
                                maxPrice,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                selectedCategories,
                                EventSort.SPOTS_DESC.getQueryValue(),
                                sort,
                                "feed.sort.spots"));

        return new MatchListControlsViewModel(path, path, searchQuery, sortOptions, filterGroups);
    }

    private static List<PaginationItemViewModel> buildPagination(
            final String path,
            final EventType selectedType,
            final String selectedFilter,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories,
            final PaginatedResult<?> result) {
        if (result == null) {
            return List.of();
        }

        return PaginationUtils.buildPaginationItems(
                result.getPage(),
                result.getTotalPages(),
                page ->
                        buildPageUrl(
                                path,
                                selectedType,
                                selectedFilter,
                                searchQuery,
                                sort,
                                startDate,
                                endDate,
                                minPrice,
                                maxPrice,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                selectedCategories,
                                page));
    }

    private static String buildPageUrl(
            final String path,
            final EventType selectedType,
            final String selectedFilter,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories,
            final int page) {
        final Map<String, String> params =
                buildParamsMap(
                        path,
                        selectedType,
                        selectedFilter,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        selectedCategories,
                        page);
        final UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        params.forEach(builder::queryParam);
        return builder.build().encode().toUriString();
    }

    private static Map<String, String> buildParamsMap(
            final String path,
            final EventType selectedType,
            final String selectedFilter,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories,
            final int page) {
        final Map<String, String> params = new LinkedHashMap<>();
        params.put("page", Integer.toString(page));
        if ("/events"
                .equals(path)) { // TODO: this is a bit hacky, find a better way to determine which
            // params to include
            if (selectedType == EventType.TOURNAMENT) {
                params.put("type", EventType.TOURNAMENT.getDbValue());
            }
            if ("past".equalsIgnoreCase(selectedFilter)) {
                params.put("filter", EventFilter.PAST.name());
            }
        }
        addCommonQueryParams(
                params,
                searchQuery,
                sort,
                startDate,
                endDate,
                minPrice,
                maxPrice,
                selectedStatuses,
                selectedSports,
                selectedVisibility,
                selectedCategories);
        return params;
    }

    private static void addCommonQueryParams(
            final Map<String, String> params,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories) {
        if (searchQuery != null && !searchQuery.isBlank()) {
            params.put("q", searchQuery);
        }
        if (sort != null && !sort.isBlank()) {
            params.put("sort", sort);
        }
        if (startDate != null && !startDate.isBlank()) {
            params.put("startDate", startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            params.put("endDate", endDate);
        }
        if (minPrice != null) {
            params.put("minPrice", minPrice.toPlainString());
        }
        if (maxPrice != null) {
            params.put("maxPrice", maxPrice.toPlainString());
        }

        final String encodedStatuses =
                encodeCsv(selectedStatuses); // TODO: is this necessary? is there a better way to do
        // it?
        final String encodedSports = encodeCsv(selectedSports);
        final String encodedVisibility = encodeCsv(selectedVisibility);
        final String encodedCategories = encodeCsv(selectedCategories);

        if (encodedStatuses != null) {
            params.put("status", encodedStatuses);
        }
        if (encodedSports != null) {
            params.put("sport", encodedSports);
        }
        if (encodedVisibility != null) {
            params.put("visibility", encodedVisibility);
        }
        if (encodedCategories != null) {
            params.put("category", encodedCategories);
        }
    }

    private static SelectOptionViewModel sortOption(
            final String path,
            final EventType selectedType,
            final String selectedFilter,
            final String searchQuery,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories,
            final String value,
            final String currentSort,
            final String labelCode) {
        final Map<String, String> params =
                buildParamsMap(
                        path,
                        selectedType,
                        selectedFilter,
                        searchQuery,
                        value,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        selectedCategories,
                        1);
        return new SelectOptionViewModel(
                labelCode, null, params, value.equalsIgnoreCase(currentSort));
    }

    private static FilterOptionViewModel filterOption(
            final String path,
            final EventType selectedType,
            final String selectedFilter,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final List<String> statuses,
            final List<String> sports,
            final List<String> visibility,
            final List<String> categories,
            final boolean active,
            final String labelCode) {
        final Map<String, String> params =
                buildParamsMap(
                        path,
                        selectedType,
                        selectedFilter,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        statuses,
                        sports,
                        visibility,
                        categories,
                        1);
        return new FilterOptionViewModel(labelCode, null, params, null, active);
    }

    private static List<FilterOptionViewModel> buildEventTypeFilterOptions(
            final String path,
            final EventType selectedType,
            final String selectedFilter,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories) {
        return List.of(
                new FilterOptionViewModel(
                        "filter.eventType.matches",
                        null,
                        buildParamsMap(
                                path,
                                EventType.MATCH,
                                selectedFilter,
                                searchQuery,
                                sort,
                                startDate,
                                endDate,
                                minPrice,
                                maxPrice,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                selectedCategories,
                                1),
                        null,
                        selectedType == EventType.MATCH),
                new FilterOptionViewModel(
                        "filter.eventType.tournaments",
                        null,
                        buildParamsMap(
                                path,
                                EventType.TOURNAMENT,
                                selectedFilter,
                                searchQuery,
                                sort,
                                null,
                                null,
                                null,
                                null,
                                List.of(),
                                selectedSports,
                                List.of(),
                                List.of(),
                                1),
                        null,
                        selectedType == EventType.TOURNAMENT));
    }

    private static List<FilterOptionViewModel> buildSportFilterOptions(
            final String path,
            final EventType selectedType,
            final String selectedFilter,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories) {
        return List.of(
                filterOption(
                        path,
                        selectedType,
                        selectedFilter,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        selectedStatuses,
                        List.of(),
                        selectedVisibility,
                        selectedCategories,
                        selectedSports.isEmpty(),
                        "filter.anySport"),
                filterOption(
                        path,
                        selectedType,
                        selectedFilter,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        selectedStatuses,
                        toggleValue(selectedSports, "football"),
                        selectedVisibility,
                        selectedCategories,
                        selectedSports.contains("football"),
                        "sport.football"),
                filterOption(
                        path,
                        selectedType,
                        selectedFilter,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        selectedStatuses,
                        toggleValue(selectedSports, "tennis"),
                        selectedVisibility,
                        selectedCategories,
                        selectedSports.contains("tennis"),
                        "sport.tennis"),
                filterOption(
                        path,
                        selectedType,
                        selectedFilter,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        selectedStatuses,
                        toggleValue(selectedSports, "basketball"),
                        selectedVisibility,
                        selectedCategories,
                        selectedSports.contains("basketball"),
                        "sport.basketball"),
                filterOption(
                        path,
                        selectedType,
                        selectedFilter,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        selectedStatuses,
                        toggleValue(selectedSports, "padel"),
                        selectedVisibility,
                        selectedCategories,
                        selectedSports.contains("padel"),
                        "sport.padel"),
                filterOption(
                        path,
                        selectedType,
                        selectedFilter,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        selectedStatuses,
                        toggleValue(selectedSports, "other"),
                        selectedVisibility,
                        selectedCategories,
                        selectedSports.contains("other"),
                        "sport.other"));
    }

    private static List<FilterOptionViewModel> buildStatusFilterOptions(
            final String path,
            final EventType selectedType,
            final String selectedFilter,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories,
            final List<String> allowedStatuses) {
        final List<FilterOptionViewModel> options = new ArrayList<>();
        options.add(
                filterOption(
                        path,
                        selectedType,
                        selectedFilter,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        List.of(),
                        selectedSports,
                        selectedVisibility,
                        selectedCategories,
                        selectedStatuses.isEmpty(),
                        "filter.anyStatus"));

        for (final String status : allowedStatuses) {
            options.add(
                    filterOption(
                            path,
                            selectedType,
                            selectedFilter,
                            searchQuery,
                            sort,
                            startDate,
                            endDate,
                            minPrice,
                            maxPrice,
                            toggleValue(selectedStatuses, status),
                            selectedSports,
                            selectedVisibility,
                            selectedCategories,
                            selectedStatuses.contains(status),
                            "match.status." + status));
        }

        return List.copyOf(options);
    }

    private static List<FilterOptionViewModel> buildCategoryFilterOptions(
            final String path,
            final EventType selectedType,
            final String selectedFilter,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories) {
        return List.of(
                filterOption(
                        path,
                        selectedType,
                        selectedFilter,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        List.of(),
                        selectedCategories.isEmpty(),
                        "filter.anyCategory"),
                filterOption(
                        path,
                        selectedType,
                        selectedFilter,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        toggleValue(selectedCategories, "joined"),
                        selectedCategories.contains("joined"),
                        "category.joined"),
                filterOption(
                        path,
                        selectedType,
                        selectedFilter,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        toggleValue(selectedCategories, "invited"),
                        selectedCategories.contains("invited"),
                        "category.invited"),
                filterOption(
                        path,
                        selectedType,
                        selectedFilter,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        toggleValue(selectedCategories, "pending"),
                        selectedCategories.contains("pending"),
                        "category.pending"),
                filterOption(
                        path,
                        selectedType,
                        selectedFilter,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        toggleValue(selectedCategories, "hosted"),
                        selectedCategories.contains("hosted"),
                        "category.hosted"));
    }

    private static DateRangeBounds dateRangeBounds(final EventFilter selectedFilter) {
        final LocalDate today = LocalDate.now(PlatformTime.ZONE);
        if (selectedFilter == EventFilter.PAST) {
            return new DateRangeBounds(null, today.toString());
        }
        return new DateRangeBounds(today.toString(), null);
    }

    static <T extends PersistableEnum> List<String> toDbValues(final List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        final List<String> dbValues = new ArrayList<>();
        for (final T entry : values) {
            if (entry != null) {
                dbValues.add(entry.getDbValue());
            }
        }
        return List.copyOf(dbValues);
    }

    private static String formatNullablePriceValue(final BigDecimal price) {
        return price == null ? "" : price.stripTrailingZeros().toPlainString();
    }

    private record DateRangeBounds(String minDate, String maxDate) {}
}
