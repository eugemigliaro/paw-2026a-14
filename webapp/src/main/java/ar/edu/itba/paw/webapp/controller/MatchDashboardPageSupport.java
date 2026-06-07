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
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
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
            final Locale locale,
            final MatchDashboardQueryState.DashboardSelection selection,
            final PaginatedResult<Match> result,
            final PaginatedResult<Tournament> tournamentResult,
            final String title,
            final String description,
            final String emptyMessage,
            final MessageSource messageSource,
            final MatchParticipationService matchParticipationService,
            final MatchReservationService matchReservationService) {
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

        mav.addObject("pageTitleCode", pageTitleCode);
        mav.addObject("listTitle", title);
        mav.addObject("listDescription", description);
        mav.addObject("emptyMessage", emptyMessage);
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
                        locale,
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
                        messageSource));
        mav.addObject(
                "events",
                selectedType == EventType.TOURNAMENT
                        ? tournamentResult.getItems()
                        : result.getItems());
        mav.addObject("eventType", selectedType);
        mav.addObject(
                "eventBadgeLabels",
                selectedType == EventType.TOURNAMENT
                        ? EventCardAttributeUtils.tournamentBadgeLabels(
                                tournamentResult.getItems(), locale, messageSource)
                        : EventCardAttributeUtils.matchStatusBadgeLabels(
                                result.getItems(), locale, messageSource));
        mav.addObject(
                "eventRelationshipBadgeCodes",
                selectedType == EventType.TOURNAMENT
                        ? EventCardAttributeUtils.tournamentRelationshipBadgeCodes(
                                tournamentResult.getItems(), currentUser)
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
            final Locale locale,
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
            final List<String> selectedCategories,
            final MessageSource messageSource) {
        final List<FilterGroupViewModel> filterGroups = new ArrayList<>();
        if ("/events".equals(path)) {
            filterGroups.add(
                    new FilterGroupViewModel(
                            messageSource.getMessage("filter.eventType", null, locale),
                            buildEventTypeFilterOptions(
                                    path,
                                    locale,
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
                                    messageSource)));
        }
        filterGroups.add(
                new FilterGroupViewModel(
                        messageSource.getMessage("filter.categories", null, locale),
                        buildSportFilterOptions(
                                path,
                                locale,
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
                                messageSource)));

        if ("/events".equals(path) && selectedType != EventType.TOURNAMENT) {
            filterGroups.add(
                    new FilterGroupViewModel(
                            messageSource.getMessage("filter.category", null, locale),
                            buildCategoryFilterOptions(
                                    path,
                                    locale,
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
                                    messageSource)));
        }

        if (selectedType != EventType.TOURNAMENT) {
            filterGroups.add(
                    new FilterGroupViewModel(
                            messageSource.getMessage("host.filters.status", null, locale),
                            buildStatusFilterOptions(
                                    path,
                                    locale,
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
                                            .toList(),
                                    messageSource)));
        }

        final List<SelectOptionViewModel> sortOptions =
                List.of(
                        sortOption(
                                path,
                                locale,
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
                                messageSource.getMessage("feed.sort.soonest", null, locale)),
                        sortOption(
                                path,
                                locale,
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
                                messageSource.getMessage("feed.sort.price", null, locale)),
                        sortOption(
                                path,
                                locale,
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
                                messageSource.getMessage("feed.sort.spots", null, locale)));

        return new MatchListControlsViewModel(
                path,
                path,
                messageSource.getMessage("feed.aria.search", null, locale),
                searchQuery,
                messageSource.getMessage("feed.search.placeholder", null, locale),
                messageSource.getMessage("feed.search.button", null, locale),
                messageSource.getMessage("feed.sortBy", null, locale),
                sortOptions,
                messageSource.getMessage("filter.title", null, locale),
                filterGroups);
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
            final Locale locale,
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
            final String label) {
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
        return new SelectOptionViewModel(label, null, params, value.equalsIgnoreCase(currentSort));
    }

    private static FilterOptionViewModel filterOption(
            final String path,
            final Locale locale,
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
            final String label) {
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
        return new FilterOptionViewModel(label, null, params, null, active);
    }

    private static List<FilterOptionViewModel> buildEventTypeFilterOptions(
            final String path,
            final Locale locale,
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
            final MessageSource messageSource) {
        return List.of(
                new FilterOptionViewModel(
                        messageSource.getMessage("filter.eventType.matches", null, locale),
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
                        messageSource.getMessage("filter.eventType.tournaments", null, locale),
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
            final Locale locale,
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
            final MessageSource messageSource) {
        return List.of(
                filterOption(
                        path,
                        locale,
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
                        messageSource.getMessage("filter.anySport", null, locale)),
                filterOption(
                        path,
                        locale,
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
                        messageSource.getMessage("sport.football", null, locale)),
                filterOption(
                        path,
                        locale,
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
                        messageSource.getMessage("sport.tennis", null, locale)),
                filterOption(
                        path,
                        locale,
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
                        messageSource.getMessage("sport.basketball", null, locale)),
                filterOption(
                        path,
                        locale,
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
                        messageSource.getMessage("sport.padel", null, locale)),
                filterOption(
                        path,
                        locale,
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
                        messageSource.getMessage("sport.other", null, locale)));
    }

    private static List<FilterOptionViewModel> buildStatusFilterOptions(
            final String path,
            final Locale locale,
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
            final List<String> allowedStatuses,
            final MessageSource messageSource) {
        final List<FilterOptionViewModel> options = new ArrayList<>();
        options.add(
                filterOption(
                        path,
                        locale,
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
                        messageSource.getMessage("filter.anyStatus", null, locale)));

        for (final String status : allowedStatuses) {
            options.add(
                    filterOption(
                            path,
                            locale,
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
                            messageSource.getMessage("match.status." + status, null, locale)));
        }

        return List.copyOf(options);
    }

    private static List<FilterOptionViewModel> buildCategoryFilterOptions(
            final String path,
            final Locale locale,
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
            final MessageSource messageSource) {
        return List.of(
                filterOption(
                        path,
                        locale,
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
                        messageSource.getMessage("filter.anyCategory", null, locale)),
                filterOption(
                        path,
                        locale,
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
                        messageSource.getMessage("category.joined", null, locale)),
                filterOption(
                        path,
                        locale,
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
                        messageSource.getMessage("category.invited", null, locale)),
                filterOption(
                        path,
                        locale,
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
                        messageSource.getMessage("category.pending", null, locale)),
                filterOption(
                        path,
                        locale,
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
                        messageSource.getMessage("category.hosted", null, locale)));
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
