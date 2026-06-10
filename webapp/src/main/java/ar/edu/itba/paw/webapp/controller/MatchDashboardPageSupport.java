package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.encodeCsv;
import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.toggleValue;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventCategory;
import ar.edu.itba.paw.models.query.EventFilter;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.query.InvolvementScope;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventType;
import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            final String listTitleCode,
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

        // listTitle is resolved in events/list.jsp via <spring:message>;
        // description/empty copy is not rendered on this view.
        mav.addObject("pageTitleCode", pageTitleCode);
        mav.addObject("listTitleCode", listTitleCode);
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

    static ModelAndView buildTournamentMatchListPage(
            final User currentUser,
            final String view,
            final String path,
            final SearchForm searchForm,
            final boolean upcoming,
            final PaginatedResult<TournamentMatch> result,
            final TournamentService tournamentService) {
        final ModelAndView mav = new ModelAndView(view);
        final EventFilter selectedFilter = upcoming ? EventFilter.UPCOMING : EventFilter.PAST;
        final String selectedFilterValue = selectedFilter.name();
        final String searchQuery = searchForm.getQ();
        final String sort = searchForm.getSort().getQueryValue();
        final List<String> selectedSportsStr = toDbValues(searchForm.getSport());
        final List<String> selectedTmStatuses = toDbValues(searchForm.getTmStatus());
        final String involvement = searchForm.getInvolvement().name();

        mav.addObject("pageTitleCode", "page.title.tournamentMatches");
        mav.addObject("listTitleCode", "matches.title");
        mav.addObject("selectedType", searchForm.getType().getDbValue());
        mav.addObject("selectedSort", sort);
        mav.addObject("selectedSports", selectedSportsStr);
        mav.addObject("selectedTmStatuses", selectedTmStatuses);
        mav.addObject("selectedInvolvement", involvement);
        mav.addObject("searchForm", searchForm);
        mav.addObject(
                "listControls",
                buildTournamentMatchListControls(
                        path,
                        selectedFilterValue,
                        searchQuery,
                        sort,
                        selectedSportsStr,
                        selectedTmStatuses,
                        involvement));
        mav.addObject("events", result.getItems());
        mav.addObject("eventType", searchForm.getType());
        mav.addObject(
                "eventBadgeCodes",
                EventCardAttributeUtils.tournamentMatchBadgeCodes(result.getItems()));
        mav.addObject(
                "tournamentTeamDisplayNumbers",
                teamDisplayNumbersFromMatches(result.getItems(), tournamentService));
        mav.addObject(
                "eventRelationshipBadgeCodes",
                EventCardAttributeUtils.tournamentMatchRelationshipBadgeCodes(
                        result.getItems(),
                        currentUser,
                        tournamentService.findParticipatingTournamentIds(
                                currentUser,
                                result.getItems().stream()
                                        .map(m -> m.getTournament().getId())
                                        .toList())));
        mav.addObject("pageResult", result);
        mav.addObject("pageHasPrevious", result.hasPrevious());
        mav.addObject("pageHasNext", result.hasNext());
        mav.addObject("pageNumber", result.getPage());
        mav.addObject(
                "paginationItems",
                buildTournamentMatchPagination(
                        path,
                        selectedFilterValue,
                        searchQuery,
                        sort,
                        selectedSportsStr,
                        selectedTmStatuses,
                        involvement,
                        result));
        return mav;
    }

    private static MatchListControlsViewModel buildTournamentMatchListControls(
            final String path,
            final String selectedFilter,
            final String searchQuery,
            final String sort,
            final List<String> selectedSports,
            final List<String> selectedTmStatuses,
            final String involvement) {
        final String typePath =
                UriComponentsBuilder.fromPath(path)
                        .queryParam("type", EventType.TOURNAMENT_MATCHES.getDbValue())
                        .build()
                        .encode()
                        .toUriString();

        final List<FilterGroupViewModel> filterGroups = new ArrayList<>();
        filterGroups.add(
                new FilterGroupViewModel(
                        "filter.categories",
                        buildTournamentMatchSportFilterOptions(
                                typePath,
                                selectedFilter,
                                searchQuery,
                                sort,
                                selectedSports,
                                selectedTmStatuses,
                                involvement)));

        filterGroups.add(
                new FilterGroupViewModel(
                        "host.filters.status",
                        buildTournamentMatchStatusFilterOptions(
                                typePath,
                                selectedFilter,
                                searchQuery,
                                sort,
                                selectedSports,
                                selectedTmStatuses,
                                involvement)));

        filterGroups.add(
                new FilterGroupViewModel(
                        "matches.tournament.involvementFilter",
                        buildTournamentMatchInvolvementFilterOptions(
                                typePath,
                                selectedFilter,
                                searchQuery,
                                sort,
                                selectedSports,
                                selectedTmStatuses,
                                involvement)));

        final SelectOptionViewModel sortOpt =
                sortOption(
                        typePath,
                        EventType.MATCH,
                        selectedFilter,
                        searchQuery,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        selectedSports,
                        List.of(),
                        List.of(),
                        EventSort.SOONEST.getQueryValue(),
                        sort,
                        "feed.sort.soonest");
        sortOpt.getParams().put("type", "tournament_match");

        return new MatchListControlsViewModel(
                typePath, typePath, searchQuery, List.of(sortOpt), filterGroups);
    }

    private static List<PaginationItemViewModel> buildTournamentMatchPagination(
            final String path,
            final String selectedFilter,
            final String searchQuery,
            final String sort,
            final List<String> selectedSports,
            final List<String> selectedTmStatuses,
            final String involvement,
            final PaginatedResult<?> result) {
        return PaginationUtils.buildPaginationItems(
                result.getPage(),
                result.getTotalPages(),
                page ->
                        buildTournamentMatchPageUrl(
                                path,
                                selectedFilter,
                                searchQuery,
                                sort,
                                selectedSports,
                                selectedTmStatuses,
                                involvement,
                                page));
    }

    private static String buildTournamentMatchPageUrl(
            final String path,
            final String selectedFilter,
            final String searchQuery,
            final String sort,
            final List<String> selectedSports,
            final List<String> selectedTmStatuses,
            final String involvement,
            final int page) {
        final Map<String, String> params = new LinkedHashMap<>();
        params.put("page", Integer.toString(page));
        params.put("type", EventType.TOURNAMENT_MATCHES.getDbValue());
        if (EventFilter.PAST.name().equalsIgnoreCase(selectedFilter)) {
            params.put("filter", EventFilter.PAST.name());
        }
        if (searchQuery != null && !searchQuery.isBlank()) {
            params.put("q", searchQuery);
        }
        if (sort != null && !sort.isBlank()) {
            params.put("sort", sort);
        }
        final String encodedSports = encodeCsv(selectedSports);
        if (encodedSports != null) {
            params.put("sport", encodedSports);
        }
        final String encodedTmStatuses = encodeCsv(selectedTmStatuses);
        if (encodedTmStatuses != null) {
            params.put("tmStatus", encodedTmStatuses);
        }
        if (involvement != null && !InvolvementScope.ALL.name().equals(involvement)) {
            params.put("involvement", involvement);
        }
        final UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        params.forEach(builder::queryParam);
        return builder.build().encode().toUriString();
    }

    private static List<FilterOptionViewModel> buildTournamentMatchSportFilterOptions(
            final String path,
            final String selectedFilter,
            final String searchQuery,
            final String sort,
            final List<String> selectedSports,
            final List<String> selectedTmStatuses,
            final String involvement) {
        final List<FilterOptionViewModel> options = new ArrayList<>();
        final Map<String, String> anyParams = new LinkedHashMap<>();
        anyParams.put("type", EventType.TOURNAMENT_MATCHES.getDbValue());
        if (EventFilter.PAST.name().equalsIgnoreCase(selectedFilter)) {
            anyParams.put("filter", EventFilter.PAST.name());
        }
        addTournamentMatchFilterParams(anyParams, selectedTmStatuses, involvement);
        options.add(
                new FilterOptionViewModel(
                        "filter.anySport", null, anyParams, null, selectedSports.isEmpty()));
        final List<String> sportValues =
                Arrays.stream(Sport.values())
                        .filter(s -> s != Sport.OTHER)
                        .map(Sport::getDbValue)
                        .toList();
        for (final String sportValue : sportValues) {
            final Map<String, String> sportParams = new LinkedHashMap<>(anyParams);
            final List<String> toggledSports = toggleValue(selectedSports, sportValue);
            final String encodedSports = encodeCsv(toggledSports);
            if (encodedSports != null) {
                sportParams.put("sport", encodedSports);
            }
            options.add(
                    new FilterOptionViewModel(
                            "sport." + sportValue,
                            null,
                            sportParams,
                            null,
                            selectedSports.contains(sportValue)));
        }
        return List.copyOf(options);
    }

    private static List<FilterOptionViewModel> buildTournamentMatchStatusFilterOptions(
            final String path,
            final String selectedFilter,
            final String searchQuery,
            final String sort,
            final List<String> selectedSports,
            final List<String> selectedTmStatuses,
            final String involvement) {
        final List<FilterOptionViewModel> options = new ArrayList<>();
        final Map<String, String> anyParams = new LinkedHashMap<>();
        anyParams.put("type", EventType.TOURNAMENT_MATCHES.getDbValue());
        if (EventFilter.PAST.name().equalsIgnoreCase(selectedFilter)) {
            anyParams.put("filter", EventFilter.PAST.name());
        }
        addTournamentMatchFilterParams(anyParams, List.of(), involvement);
        options.add(
                new FilterOptionViewModel(
                        "filter.anyStatus", null, anyParams, null, selectedTmStatuses.isEmpty()));

        final boolean isPast = EventFilter.PAST.name().equalsIgnoreCase(selectedFilter);
        final List<TournamentMatchStatus> visibleStatuses =
                isPast
                        ? List.of(TournamentMatchStatus.DONE, TournamentMatchStatus.AWAITING_RESULT)
                        : List.of(TournamentMatchStatus.SCHEDULED, TournamentMatchStatus.DONE);
        for (final TournamentMatchStatus status : visibleStatuses) {
            final Map<String, String> statusParams = new LinkedHashMap<>();
            statusParams.put("type", EventType.TOURNAMENT_MATCHES.getDbValue());
            if (isPast) {
                statusParams.put("filter", EventFilter.PAST.name());
            }
            final List<String> toggledStatuses =
                    toggleValue(selectedTmStatuses, status.getDbValue());
            addTournamentMatchFilterParams(statusParams, toggledStatuses, involvement);
            options.add(
                    new FilterOptionViewModel(
                            "tournament.match.status." + status.getDbValue(),
                            null,
                            statusParams,
                            null,
                            selectedTmStatuses.contains(status.getDbValue())));
        }
        return List.copyOf(options);
    }

    private static List<FilterOptionViewModel> buildTournamentMatchInvolvementFilterOptions(
            final String path,
            final String selectedFilter,
            final String searchQuery,
            final String sort,
            final List<String> selectedSports,
            final List<String> selectedTmStatuses,
            final String involvement) {
        final List<FilterOptionViewModel> options = new ArrayList<>();
        final Map<String, String> allParams = new LinkedHashMap<>();
        allParams.put("type", EventType.TOURNAMENT_MATCHES.getDbValue());
        if (EventFilter.PAST.name().equalsIgnoreCase(selectedFilter)) {
            allParams.put("filter", EventFilter.PAST.name());
        }
        addTournamentMatchFilterParams(allParams, selectedTmStatuses, null);
        options.add(
                new FilterOptionViewModel(
                        "matches.tournament.involvement.all",
                        null,
                        allParams,
                        null,
                        InvolvementScope.ALL.name().equals(involvement)));

        final Map<String, String> hostParams = new LinkedHashMap<>(allParams);
        hostParams.put("involvement", InvolvementScope.HOST.name());
        options.add(
                new FilterOptionViewModel(
                        "matches.tournament.involvement.host",
                        null,
                        hostParams,
                        null,
                        InvolvementScope.HOST.name().equals(involvement)));

        final Map<String, String> participantParams = new LinkedHashMap<>(allParams);
        participantParams.put("involvement", InvolvementScope.PARTICIPANT.name());
        options.add(
                new FilterOptionViewModel(
                        "matches.tournament.involvement.participant",
                        null,
                        participantParams,
                        null,
                        InvolvementScope.PARTICIPANT.name().equals(involvement)));
        return List.copyOf(options);
    }

    private static void addTournamentMatchFilterParams(
            final Map<String, String> params,
            final List<String> selectedTmStatuses,
            final String involvement) {
        final String encodedTmStatuses = encodeCsv(selectedTmStatuses);
        if (encodedTmStatuses != null) {
            params.put("tmStatus", encodedTmStatuses);
        }
        if (involvement != null && !InvolvementScope.ALL.name().equals(involvement)) {
            params.put("involvement", involvement);
        }
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

        if (selectedType != EventType.TOURNAMENT) {
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
        if ("/events".equals(path) && selectedType == EventType.TOURNAMENT) {
            params.put("type", EventType.TOURNAMENT.getDbValue());
        }
        if (EventFilter.PAST.name().equalsIgnoreCase(selectedFilter)) {
            params.put("filter", EventFilter.PAST.name());
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

        putCsvParam(params, "status", selectedStatuses);
        putCsvParam(params, "sport", selectedSports);
        putCsvParam(params, "visibility", selectedVisibility);
        putCsvParam(params, "category", selectedCategories);
    }

    private static void putCsvParam(
            final Map<String, String> params, final String name, final List<String> values) {
        final String encoded = encodeCsv(values);
        if (encoded != null) {
            params.put(name, encoded);
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
                        selectedStatuses,
                        List.of(),
                        selectedVisibility,
                        selectedCategories,
                        selectedSports.isEmpty(),
                        "filter.anySport"));
        for (final Sport sport : Sport.values()) {
            if (selectedType == EventType.TOURNAMENT && sport == Sport.OTHER) {
                continue;
            }
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
                            selectedStatuses,
                            toggleValue(selectedSports, sport.getDbValue()),
                            selectedVisibility,
                            selectedCategories,
                            selectedSports.contains(sport.getDbValue()),
                            "sport." + sport.getDbValue()));
        }
        return options;
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
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        List.of(),
                        selectedCategories.isEmpty(),
                        "filter.anyCategory"));
        for (final EventCategory category : EventCategory.values()) {
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
                            selectedStatuses,
                            selectedSports,
                            selectedVisibility,
                            toggleValue(selectedCategories, category.getQueryValue()),
                            selectedCategories.contains(category.getQueryValue()),
                            "category." + category.getQueryValue()));
        }
        return options;
    }

    private static DateRangeBounds dateRangeBounds(final EventFilter selectedFilter) {
        final LocalDate today = LocalDate.now(PlatformTime.ZONE);
        if (selectedFilter == EventFilter.PAST) {
            return new DateRangeBounds(null, today.toString());
        }
        return new DateRangeBounds(today.toString(), null);
    }

    private static Map<Long, Integer> teamDisplayNumbersFromMatches(
            final List<TournamentMatch> matches, final TournamentService tournamentService) {
        if (matches == null || matches.isEmpty()) {
            return Map.of();
        }
        final Set<Long> tournamentIds = new LinkedHashSet<>();
        for (final TournamentMatch match : matches) {
            if (match.getTournament() != null && match.getTournament().getId() != null) {
                tournamentIds.add(match.getTournament().getId());
            }
        }
        if (tournamentIds.isEmpty()) {
            return Map.of();
        }
        final List<TournamentTeam> allBracketTeams =
                tournamentService.findBracketTeams(tournamentIds);
        final Map<Long, List<TournamentTeam>> teamsByTournament = new HashMap<>();
        for (final TournamentTeam team : allBracketTeams) {
            if (team.getTournament() != null && team.getTournament().getId() != null) {
                teamsByTournament
                        .computeIfAbsent(team.getTournament().getId(), ignored -> new ArrayList<>())
                        .add(team);
            }
        }
        final Map<Long, Integer> displayNumbers = new LinkedHashMap<>();
        for (final long tournamentId : tournamentIds) {
            final List<TournamentTeam> bracketTeams =
                    teamsByTournament.getOrDefault(tournamentId, List.of());
            for (int index = 0; index < bracketTeams.size(); index++) {
                final TournamentTeam team = bracketTeams.get(index);
                if (team != null && team.getId() != null) {
                    displayNumbers.put(team.getId(), index + 1);
                }
            }
        }
        return displayNumbers;
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
