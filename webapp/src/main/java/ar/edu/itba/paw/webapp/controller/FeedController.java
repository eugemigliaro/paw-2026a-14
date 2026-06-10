package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.toggleValue;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.types.EventType;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.webapp.form.SearchForm;
import ar.edu.itba.paw.webapp.security.annotation.CurrentUser;
import ar.edu.itba.paw.webapp.utils.EventCardAttributeUtils;
import ar.edu.itba.paw.webapp.utils.PaginationUtils;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FilterGroupViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FilterOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.SelectOptionViewModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class FeedController {

    private static final int PAGE_SIZE = 12;
    private static final String SESSION_EXPLORE_LATITUDE = "exploreLocationLatitude";
    private static final String SESSION_EXPLORE_LONGITUDE = "exploreLocationLongitude";
    private static final double DEFAULT_MAP_LATITUDE = -34.6037;
    private static final double DEFAULT_MAP_LONGITUDE = -58.3816;
    private static final int DEFAULT_MAP_ZOOM = 14;

    private final MatchService matchService;
    private final MatchParticipationService matchParticipationService;
    private final MatchReservationService matchReservationService;
    private final TournamentService tournamentService;
    private final boolean mapPickerEnabled;
    private final String mapTileUrlTemplate;
    private final String mapAttribution;
    private final double mapDefaultLatitude;
    private final double mapDefaultLongitude;
    private final int mapDefaultZoom;

    @Autowired
    public FeedController(
            final MatchService matchService,
            final MatchParticipationService matchParticipationService,
            final MatchReservationService matchReservationService,
            final TournamentService tournamentService,
            @Value("${map.picker.enabled:false}") final boolean mapPickerEnabled,
            @Value("${map.tiles.urlTemplate:}") final String mapTileUrlTemplate,
            @Value("${map.tiles.attribution:}") final String mapAttribution,
            @Value("${map.default.latitude:" + DEFAULT_MAP_LATITUDE + "}")
                    final double mapDefaultLatitude,
            @Value("${map.default.longitude:" + DEFAULT_MAP_LONGITUDE + "}")
                    final double mapDefaultLongitude,
            @Value("${map.default.zoom:" + DEFAULT_MAP_ZOOM + "}") final int mapDefaultZoom) {
        this.matchService = matchService;
        this.matchParticipationService = matchParticipationService;
        this.matchReservationService = matchReservationService;
        this.tournamentService = tournamentService;
        this.mapPickerEnabled = mapPickerEnabled;
        this.mapTileUrlTemplate = mapTileUrlTemplate == null ? "" : mapTileUrlTemplate;
        this.mapAttribution = mapAttribution == null ? "" : mapAttribution;
        this.mapDefaultLatitude = mapDefaultLatitude;
        this.mapDefaultLongitude = mapDefaultLongitude;
        this.mapDefaultZoom = mapDefaultZoom;
    }

    @GetMapping("/")
    public ModelAndView showFeed(
            @CurrentUser final User user,
            @Valid @ModelAttribute("searchForm") final SearchForm searchForm,
            final BindingResult bindingResult,
            final HttpSession session,
            final Locale locale) {
        // A malformed free-text query should not produce an error page: render the
        // feed with the inline validation message and ignore the invalid query for
        // the search itself. Structural binding problems (page, dates, prices) remain
        // a controlled 400.
        if (hasErrorsOutsideQuery(bindingResult)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        String searchQuery = searchForm.getQ();
        if (bindingResult.hasFieldErrors("q")) {
            searchQuery = "";
            searchForm.setQ(searchQuery);
        }

        final ExploreLocation exploreLocation = exploreLocation(session);
        final DateRange selectedDateRange =
                new DateRange(searchForm.getStartDate(), searchForm.getEndDate());
        final BigDecimal minPrice =
                searchForm.getMinPrice() != null
                        ? searchForm.getMinPrice().stripTrailingZeros()
                        : null;
        final BigDecimal maxPrice =
                searchForm.getMaxPrice() != null
                        ? searchForm.getMaxPrice().stripTrailingZeros()
                        : null;
        final PriceRange selectedPriceRange = new PriceRange(minPrice, maxPrice);
        final boolean nearMeUnavailable =
                exploreLocation == null && searchForm.getSort() == EventSort.DISTANCE;
        final EventSort selectedSort =
                searchForm.getType() == EventType.TOURNAMENT
                                && searchForm.getSort() == EventSort.SPOTS_DESC
                        ? EventSort.SOONEST
                        : nearMeUnavailable ? EventSort.SOONEST : searchForm.getSort();
        String selectedTypeValue =
                searchForm.getType() != null ? searchForm.getType().getDbValue() : null;
        String selectedSortValue =
                searchForm.getSort() != null ? searchForm.getSort().getQueryValue() : null;
        List<String> selectedSports =
                searchForm.getSport() != null
                        ? searchForm.getSport().stream()
                                .map(Sport::getDbValue)
                                .collect(Collectors.toList())
                        : null;
        String selectedMinPriceValue = formatNullablePriceValue(searchForm.getMinPrice());
        String selectedMaxPriceValue = formatNullablePriceValue(searchForm.getMaxPrice());
        String selectedStartDateValue =
                searchForm.getStartDate() != null ? searchForm.getStartDate().toString() : null;
        String selectedEndDateValue =
                searchForm.getEndDate() != null ? searchForm.getEndDate().toString() : null;

        final ModelAndView mav = new ModelAndView("feed/index");
        mav.addObject("selectedType", selectedTypeValue);
        mav.addObject("selectedSort", selectedSortValue);
        mav.addObject("selectedSports", selectedSports);
        mav.addObject("selectedMinPrice", selectedPriceRange.minPrice());
        mav.addObject("selectedMaxPrice", selectedPriceRange.maxPrice());
        mav.addObject("selectedMinPriceValue", selectedMinPriceValue);
        mav.addObject("selectedMaxPriceValue", selectedMaxPriceValue);
        mav.addObject("selectedDateMinValue", LocalDate.now(PlatformTime.ZONE).toString());
        mav.addObject("selectedStartDateValue", selectedStartDateValue);
        mav.addObject("selectedEndDateValue", selectedEndDateValue);
        mav.addObject(
                "sortOptions",
                buildSortOptions(
                        searchQuery,
                        searchForm.getType(),
                        selectedSort,
                        selectedSports,
                        selectedDateRange,
                        selectedPriceRange));
        mav.addObject("nearMeUnavailable", nearMeUnavailable);
        if (EventType.TOURNAMENT == searchForm.getType()) {
            final PaginatedResult<Tournament> result =
                    tournamentService.searchPublicTournaments(
                            searchQuery,
                            searchForm.getSport(),
                            selectedDateRange.startDate(),
                            selectedDateRange.endDate(),
                            selectedSort,
                            searchForm.getPage(),
                            PAGE_SIZE,
                            selectedPriceRange.minPrice(),
                            selectedPriceRange.maxPrice(),
                            exploreLocation != null ? exploreLocation.latitude() : null,
                            exploreLocation != null ? exploreLocation.longitude() : null);
            addTournamentFeedAttributes(
                    mav,
                    user,
                    searchQuery,
                    searchForm.getType(),
                    selectedSort,
                    selectedSports,
                    selectedDateRange,
                    selectedStartDateValue,
                    selectedEndDateValue,
                    selectedPriceRange,
                    result,
                    locale,
                    exploreLocation);
        } else {
            final PaginatedResult<Match> result =
                    matchService.searchPublicMatches(
                            searchQuery,
                            searchForm.getSport(),
                            selectedDateRange.startDate(),
                            selectedDateRange.endDate(),
                            selectedSort,
                            searchForm.getPage(),
                            PAGE_SIZE,
                            selectedPriceRange.minPrice(),
                            selectedPriceRange.maxPrice(),
                            exploreLocation != null ? exploreLocation.latitude() : null,
                            exploreLocation != null ? exploreLocation.longitude() : null);
            addMatchFeedAttributes(
                    mav,
                    user,
                    searchQuery,
                    searchForm.getType(),
                    selectedSort,
                    selectedSports,
                    selectedDateRange,
                    selectedStartDateValue,
                    selectedEndDateValue,
                    selectedPriceRange,
                    result,
                    locale,
                    exploreLocation);
        }
        mav.addObject("nearMeAvailable", exploreLocation != null);
        mav.addObject("mapPickerEnabled", mapPickerEnabled && !mapTileUrlTemplate.isBlank());
        mav.addObject("mapTileUrlTemplate", mapTileUrlTemplate);
        mav.addObject("mapAttribution", mapAttribution);
        mav.addObject("mapDefaultLatitude", mapDefaultLatitude);
        mav.addObject("mapDefaultLongitude", mapDefaultLongitude);
        mav.addObject("mapDefaultZoom", mapDefaultZoom);
        return mav;
    }

    private static boolean hasErrorsOutsideQuery(final BindingResult bindingResult) {
        if (bindingResult.hasGlobalErrors()) {
            return true;
        }
        return bindingResult.getFieldErrors().stream()
                .anyMatch(error -> !"q".equals(error.getField()));
    }

    @PostMapping("/explore/location")
    public ModelAndView storeExploreLocation(
            @RequestParam(value = "latitude", required = false) final Double latitude,
            @RequestParam(value = "longitude", required = false) final Double longitude,
            @RequestParam(value = "type", required = false) final EventType type,
            final HttpSession session) {
        if (latitude != null
                && latitude >= -90
                && latitude <= 90
                && longitude != null
                && longitude >= -180
                && longitude <= 180) {
            session.setAttribute(SESSION_EXPLORE_LATITUDE, latitude);
            session.setAttribute(SESSION_EXPLORE_LONGITUDE, longitude);
        }
        final UriComponentsBuilder redirect =
                UriComponentsBuilder.fromPath("/").queryParam("sort", "distance");
        if (EventType.TOURNAMENT == type) {
            redirect.queryParam("type", EventType.TOURNAMENT.getDbValue());
        }
        return new ModelAndView("redirect:" + redirect.build().encode().toUriString());
    }

    private void addMatchFeedAttributes(
            final ModelAndView mav,
            final User currentUser,
            final String query,
            final EventType selectedType,
            final EventSort selectedSort,
            final List<String> selectedSports,
            final DateRange selectedDateRange,
            final String selectedStartDateValue,
            final String selectedEndDateValue,
            final PriceRange selectedPriceRange,
            final PaginatedResult<Match> result,
            final Locale locale,
            final ExploreLocation exploreLocation) {

        addFeedPageAttributes(
                mav,
                buildFilterGroups(
                        query,
                        selectedType,
                        selectedSort,
                        selectedSports,
                        selectedDateRange,
                        selectedStartDateValue,
                        selectedEndDateValue,
                        selectedPriceRange),
                result.getItems(),
                EventType.MATCH,
                EventCardAttributeUtils.matchSpotsBadgeCodes(result.getItems()),
                EventCardAttributeUtils.matchDistanceLabels(
                        result.getItems(),
                        exploreLocation == null ? null : exploreLocation.latitude(),
                        exploreLocation == null ? null : exploreLocation.longitude(),
                        locale),
                EventCardAttributeUtils.matchRelationshipBadgeCodes(
                        result.getItems(),
                        currentUser,
                        matchParticipationService,
                        matchReservationService),
                result.getPage(),
                result.getTotalPages(),
                PaginationUtils.buildPaginationItems(
                        result.getPage(),
                        result.getTotalPages(),
                        page ->
                                buildUrl(
                                        query,
                                        selectedType,
                                        selectedSort,
                                        selectedSports,
                                        selectedDateRange,
                                        selectedPriceRange,
                                        page)),
                result.hasPrevious()
                        ? buildUrl(
                                query,
                                selectedType,
                                selectedSort,
                                selectedSports,
                                selectedDateRange,
                                selectedPriceRange,
                                result.getPage() - 1)
                        : null,
                result.hasNext()
                        ? buildUrl(
                                query,
                                selectedType,
                                selectedSort,
                                selectedSports,
                                selectedDateRange,
                                selectedPriceRange,
                                result.getPage() + 1)
                        : null);
    }

    private void addTournamentFeedAttributes(
            final ModelAndView mav,
            final User currentUser,
            final String query,
            final EventType selectedType,
            final EventSort selectedSort,
            final List<String> selectedSports,
            final DateRange selectedDateRange,
            final String selectedStartDateValue,
            final String selectedEndDateValue,
            final PriceRange selectedPriceRange,
            final PaginatedResult<Tournament> result,
            final Locale locale,
            final ExploreLocation exploreLocation) {
        final List<Long> tournamentIds = result.getItems().stream().map(Tournament::getId).toList();

        addFeedPageAttributes(
                mav,
                buildFilterGroups(
                        query,
                        selectedType,
                        selectedSort,
                        selectedSports,
                        selectedDateRange,
                        selectedStartDateValue,
                        selectedEndDateValue,
                        selectedPriceRange),
                result.getItems(),
                EventType.TOURNAMENT,
                EventCardAttributeUtils.tournamentBadgeCodes(result.getItems()),
                EventCardAttributeUtils.tournamentDistanceLabels(
                        result.getItems(),
                        exploreLocation == null ? null : exploreLocation.latitude(),
                        exploreLocation == null ? null : exploreLocation.longitude(),
                        locale),
                EventCardAttributeUtils.tournamentRelationshipBadgeCodes(
                        result.getItems(),
                        currentUser,
                        tournamentService.findParticipatingTournamentIds(
                                currentUser, tournamentIds)),
                result.getPage(),
                result.getTotalPages(),
                PaginationUtils.buildPaginationItems(
                        result.getPage(),
                        result.getTotalPages(),
                        page ->
                                buildUrl(
                                        query,
                                        selectedType,
                                        selectedSort,
                                        selectedSports,
                                        selectedDateRange,
                                        selectedPriceRange,
                                        page)),
                result.hasPrevious()
                        ? buildUrl(
                                query,
                                selectedType,
                                selectedSort,
                                selectedSports,
                                selectedDateRange,
                                selectedPriceRange,
                                result.getPage() - 1)
                        : null,
                result.hasNext()
                        ? buildUrl(
                                query,
                                selectedType,
                                selectedSort,
                                selectedSports,
                                selectedDateRange,
                                selectedPriceRange,
                                result.getPage() + 1)
                        : null);
    }

    private void addFeedPageAttributes(
            final ModelAndView mav,
            final List<FilterGroupViewModel> filterGroups,
            final List<?> featuredEvents,
            final EventType featuredEventType,
            final Map<Long, String> eventBadgeCodes,
            final Map<Long, String> eventDistanceLabels,
            final Map<Long, List<String>> eventRelationshipBadgeCodes,
            final int page,
            final int totalPages,
            final List<?> paginationItems,
            final String previousPageHref,
            final String nextPageHref) {
        // Hero/search/sort-by labels are resolved in feed/index.jsp via <spring:message>.
        mav.addObject("feedEyebrow", "");
        mav.addObject("feedFilterGroups", filterGroups);
        mav.addObject("featuredEvents", featuredEvents);
        mav.addObject("featuredEventType", featuredEventType);
        mav.addObject("eventBadgeCodes", eventBadgeCodes);
        mav.addObject("eventDistanceLabels", eventDistanceLabels);
        mav.addObject("eventRelationshipBadgeCodes", eventRelationshipBadgeCodes);
        mav.addObject("feedCurrentPage", page);
        mav.addObject("feedTotalPages", totalPages);
        mav.addObject("feedPaginationItems", paginationItems);
        mav.addObject("feedPreviousPageHref", previousPageHref);
        mav.addObject("feedNextPageHref", nextPageHref);
    }

    private List<SelectOptionViewModel> buildSortOptions(
            final String query,
            final EventType selectedType,
            final EventSort selectedSort,
            final List<String> selectedSports,
            final DateRange selectedDateRange,
            final PriceRange selectedPriceRange) {
        final List<SelectOptionViewModel> sortOptions = new ArrayList<>();
        sortOptions.add(
                sortOption(
                        query,
                        selectedType,
                        selectedSort,
                        selectedSports,
                        selectedDateRange,
                        selectedPriceRange,
                        EventSort.SOONEST,
                        "feed.sort.soonest"));
        sortOptions.add(
                sortOption(
                        query,
                        selectedType,
                        selectedSort,
                        selectedSports,
                        selectedDateRange,
                        selectedPriceRange,
                        EventSort.PRICE_LOW,
                        "feed.sort.price"));
        if (selectedType != EventType.TOURNAMENT) {
            sortOptions.add(
                    sortOption(
                            query,
                            selectedType,
                            selectedSort,
                            selectedSports,
                            selectedDateRange,
                            selectedPriceRange,
                            EventSort.SPOTS_DESC,
                            "feed.sort.spots"));
        }
        sortOptions.add(
                sortOption(
                        query,
                        selectedType,
                        selectedSort,
                        selectedSports,
                        selectedDateRange,
                        selectedPriceRange,
                        EventSort.DISTANCE,
                        "feed.sort.distance"));
        return List.copyOf(sortOptions);
    }

    private SelectOptionViewModel sortOption(
            final String query,
            final EventType selectedType,
            final EventSort selectedSort,
            final List<String> selectedSports,
            final DateRange selectedDateRange,
            final PriceRange selectedPriceRange,
            final EventSort sort,
            final String labelCode) {
        return new SelectOptionViewModel(
                labelCode,
                null,
                buildParamsMap(
                        query,
                        1,
                        selectedType,
                        sort,
                        selectedSports,
                        selectedDateRange,
                        selectedPriceRange),
                sort == selectedSort);
    }

    private List<FilterGroupViewModel> buildFilterGroups(
            final String query,
            final EventType selectedType,
            final EventSort selectedSort,
            final List<String> selectedSports,
            final DateRange selectedDateRange,
            final String selectedStartDateValue,
            final String selectedEndDateValue,
            final PriceRange selectedPriceRange) {
        final List<FilterGroupViewModel> groups = new ArrayList<>();

        groups.add(
                new FilterGroupViewModel(
                        "filter.eventType",
                        List.of(
                                new FilterOptionViewModel(
                                        "filter.eventType.matches",
                                        null,
                                        buildParamsMap(
                                                query,
                                                1,
                                                EventType.MATCH,
                                                selectedSort,
                                                selectedSports,
                                                selectedDateRange,
                                                selectedPriceRange),
                                        null,
                                        selectedType == EventType.MATCH),
                                new FilterOptionViewModel(
                                        "filter.eventType.tournaments",
                                        null,
                                        buildParamsMap(
                                                query,
                                                1,
                                                EventType.TOURNAMENT,
                                                selectedSort,
                                                selectedSports,
                                                selectedDateRange,
                                                selectedPriceRange),
                                        null,
                                        selectedType == EventType.TOURNAMENT))));

        final List<FilterOptionViewModel> sports = new ArrayList<>();

        sports.add(
                new FilterOptionViewModel(
                        "filter.anySport",
                        null,
                        buildParamsMap(
                                query,
                                1,
                                selectedType,
                                selectedSort,
                                List.of(),
                                selectedDateRange,
                                selectedPriceRange),
                        null,
                        selectedSports.isEmpty()));
        for (Sport sport : Sport.values()) {
            if (sport == Sport.OTHER
                    && (selectedType == EventType.TOURNAMENT
                            || selectedType == EventType.TOURNAMENT_MATCHES)) {
                continue;
            }
            sports.add(
                    new FilterOptionViewModel(
                            "sport." + sport.name().toLowerCase(),
                            null,
                            buildParamsMap(
                                    query,
                                    1,
                                    selectedType,
                                    selectedSort,
                                    toggleSport(selectedSports, sport),
                                    selectedDateRange,
                                    selectedPriceRange),
                            null,
                            isSportSelected(selectedSports, sport)));
        }

        groups.add(new FilterGroupViewModel("filter.categories", sports));
        return List.copyOf(groups);
    }

    private static ExploreLocation exploreLocation(final HttpSession session) {
        if (session == null) {
            return null;
        }
        final Object latitude = session.getAttribute(SESSION_EXPLORE_LATITUDE);
        final Object longitude = session.getAttribute(SESSION_EXPLORE_LONGITUDE);
        if (latitude instanceof Double && longitude instanceof Double) {
            return new ExploreLocation((Double) latitude, (Double) longitude);
        }
        return null;
    }

    private static boolean isSportSelected(final List<String> selectedSports, final Sport sport) {
        return selectedSports.contains(sport.getDbValue());
    }

    private static List<String> toggleSport(
            final List<String> selectedSports, final Sport sportToToggle) {
        return toggleValue(selectedSports, sportToToggle.getDbValue());
    }

    private static String formatPriceValue(final BigDecimal price) {
        return price.stripTrailingZeros().toPlainString();
    }

    private static String formatNullablePriceValue(final BigDecimal price) {
        return price == null ? "" : formatPriceValue(price);
    }

    private static Map<String, String> buildParamsMap(
            final String query,
            final int page,
            final EventType selectedType,
            final EventSort selectedSort,
            final List<String> selectedSports,
            final DateRange selectedDateRange,
            final PriceRange selectedPriceRange) {
        final Map<String, String> params = new LinkedHashMap<>();
        params.put("q", query == null ? "" : query);
        params.put("sort", selectedSort.getQueryValue());
        params.put("page", Integer.toString(page));
        if (selectedType == EventType.TOURNAMENT) {
            params.put("type", EventType.TOURNAMENT.getDbValue());
        }

        final String encodedSports =
                selectedSports == null || selectedSports.isEmpty()
                        ? null
                        : String.join(",", selectedSports);
        if (encodedSports != null) {
            params.put("sport", encodedSports);
        }
        if (selectedDateRange.startDate() != null) {
            params.put("startDate", selectedDateRange.startDate().toString());
        }
        if (selectedDateRange.endDate() != null) {
            params.put("endDate", selectedDateRange.endDate().toString());
        }
        if (selectedPriceRange.minPrice() != null) {
            params.put("minPrice", formatPriceValue(selectedPriceRange.minPrice()));
        }
        if (selectedPriceRange.maxPrice() != null) {
            params.put("maxPrice", formatPriceValue(selectedPriceRange.maxPrice()));
        }
        return params;
    }

    private static String buildUrl(
            final String query,
            final EventType selectedType,
            final EventSort selectedSort,
            final List<String> selectedSports,
            final DateRange selectedDateRange,
            final PriceRange selectedPriceRange,
            final int page) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/");
        buildParamsMap(
                        query,
                        page,
                        selectedType,
                        selectedSort,
                        selectedSports,
                        selectedDateRange,
                        selectedPriceRange)
                .forEach(builder::queryParam);
        return builder.build().encode().toUriString();
    }

    private record PriceRange(BigDecimal minPrice, BigDecimal maxPrice) {}

    private record DateRange(LocalDate startDate, LocalDate endDate) {}

    private record ExploreLocation(Double latitude, Double longitude) {}
}
