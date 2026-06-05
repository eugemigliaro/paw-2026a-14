package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.EventCardViewModelUtils.toCard;
import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.toggleValue;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
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
import ar.edu.itba.paw.webapp.utils.PaginationUtils;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FeedPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FilterGroupViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FilterOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.SelectOptionViewModel;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
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
import org.springframework.context.MessageSource;
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
    private final MessageSource messageSource;
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
            final MessageSource messageSource,
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
        this.messageSource = messageSource;
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
            @RequestParam(value = "email", required = false)
                    final String
                            email, // TODO: remove - I don't know what it's being used for (just url
            // building apparently)
            final HttpSession session,
            final Locale locale) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        final ExploreLocation exploreLocation = exploreLocation(session);
        final ZoneId selectedTimezone =
                searchForm.getTimezone() == null
                        ? ZoneId.systemDefault()
                        : searchForm.getTimezone();
        final DateRange selectedDateRange =
                normalizeDateRange(
                        searchForm.getStartDate(), searchForm.getEndDate(), selectedTimezone);
        final PriceRange selectedPriceRange =
                normalizePriceRange(searchForm.getMinPrice(), searchForm.getMaxPrice());
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
        String selectedTimezoneValue =
                searchForm.getTimezone() != null ? searchForm.getTimezone().getId() : null;
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
        mav.addObject("selectedTimezone", selectedTimezoneValue);
        mav.addObject("selectedMinPrice", selectedPriceRange.minPrice());
        mav.addObject("selectedMaxPrice", selectedPriceRange.maxPrice());
        mav.addObject("selectedMinPriceValue", selectedMinPriceValue);
        mav.addObject("selectedMaxPriceValue", selectedMaxPriceValue);
        mav.addObject("selectedDateMinValue", LocalDate.now(selectedTimezone).toString());
        mav.addObject("selectedStartDateValue", selectedStartDateValue);
        mav.addObject("selectedEndDateValue", selectedEndDateValue);
        mav.addObject("sortLabel", messageSource.getMessage("feed.sortBy", null, locale));
        mav.addObject(
                "sortOptions",
                buildSortOptions(
                        searchForm.getQ(),
                        searchForm.getType(),
                        selectedSort,
                        selectedSports,
                        selectedDateRange,
                        selectedTimezone,
                        selectedPriceRange,
                        locale,
                        email));
        mav.addObject("nearMeUnavailable", nearMeUnavailable);
        if (EventType.TOURNAMENT == searchForm.getType()) {
            final PaginatedResult<Tournament> result =
                    tournamentService.searchPublicTournaments(
                            searchForm.getQ(),
                            searchForm.getSport(),
                            selectedDateRange.startDate() == null
                                    ? null
                                    : selectedDateRange
                                            .startDate()
                                            .atStartOfDay(selectedTimezone)
                                            .toInstant(),
                            selectedDateRange.endDate() == null
                                    ? null
                                    : selectedDateRange
                                            .endDate()
                                            .atStartOfDay(selectedTimezone)
                                            .toInstant(),
                            selectedSort,
                            searchForm.getPage(),
                            PAGE_SIZE,
                            searchForm.getTimezone(),
                            selectedPriceRange.minPrice(),
                            selectedPriceRange.maxPrice(),
                            exploreLocation != null ? exploreLocation.latitude() : null,
                            exploreLocation != null ? exploreLocation.longitude() : null);
            mav.addObject(
                    "feedPage",
                    buildTournamentFeedPageViewModel(
                            user,
                            searchForm.getQ(),
                            searchForm.getType(),
                            selectedSort,
                            selectedSports,
                            selectedDateRange,
                            selectedTimezone,
                            selectedStartDateValue,
                            selectedEndDateValue,
                            selectedTimezoneValue,
                            selectedPriceRange,
                            result,
                            locale,
                            email,
                            exploreLocation));
        } else {
            final PaginatedResult<Match> result =
                    matchService.searchPublicMatches(
                            searchForm.getQ(),
                            searchForm.getSport(),
                            selectedDateRange.startDate() == null
                                    ? null
                                    : selectedDateRange
                                            .startDate()
                                            .atStartOfDay(selectedTimezone)
                                            .toInstant(),
                            selectedDateRange.endDate() == null
                                    ? null
                                    : selectedDateRange
                                            .endDate()
                                            .atStartOfDay(selectedTimezone)
                                            .toInstant(),
                            selectedSort,
                            searchForm.getPage(),
                            PAGE_SIZE,
                            selectedTimezone,
                            selectedPriceRange.minPrice(),
                            selectedPriceRange.maxPrice(),
                            exploreLocation != null ? exploreLocation.latitude() : null,
                            exploreLocation != null ? exploreLocation.longitude() : null);
            mav.addObject(
                    "feedPage",
                    buildMatchFeedPageViewModel(
                            user,
                            searchForm.getQ(),
                            searchForm.getType(),
                            selectedSort,
                            selectedSports,
                            selectedDateRange,
                            selectedTimezone,
                            selectedStartDateValue,
                            selectedEndDateValue,
                            selectedTimezoneValue,
                            selectedPriceRange,
                            result,
                            locale,
                            email,
                            exploreLocation));
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

    private FeedPageViewModel buildMatchFeedPageViewModel(
            final User currentUser,
            final String query,
            final EventType selectedType,
            final EventSort selectedSort,
            final List<String> selectedSports,
            final DateRange selectedDateRange,
            final ZoneId selectedTimezone,
            final String selectedStartDateValue,
            final String selectedEndDateValue,
            final String selectedTimezoneValue,
            final PriceRange selectedPriceRange,
            final PaginatedResult<Match> result,
            final Locale locale,
            final String email,
            final ExploreLocation exploreLocation) {

        return new FeedPageViewModel(
                "",
                messageSource.getMessage("feed.hero.title", null, locale),
                messageSource.getMessage("feed.hero.description", null, locale),
                messageSource.getMessage("feed.search.placeholder", null, locale),
                messageSource.getMessage("feed.search.button", null, locale),
                List.of(),
                buildFilterGroups(
                        query,
                        selectedType,
                        selectedSort,
                        selectedSports,
                        selectedDateRange,
                        selectedTimezone,
                        selectedStartDateValue,
                        selectedEndDateValue,
                        selectedTimezoneValue,
                        selectedPriceRange,
                        locale,
                        email),
                result.getItems().stream()
                        .map(
                                match ->
                                        toCard(
                                                match,
                                                selectedTimezone,
                                                locale,
                                                currentUser,
                                                messageSource.getMessage(
                                                        "event.spotsLeft",
                                                        new Object[] {match.getAvailableSpots()},
                                                        locale),
                                                distanceLabel(match, exploreLocation, locale),
                                                messageSource,
                                                matchParticipationService,
                                                matchReservationService))
                        .toList(),
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
                                        selectedTimezone,
                                        selectedPriceRange,
                                        page,
                                        email)),
                result.hasPrevious()
                        ? buildUrl(
                                query,
                                selectedType,
                                selectedSort,
                                selectedSports,
                                selectedDateRange,
                                selectedTimezone,
                                selectedPriceRange,
                                result.getPage() - 1,
                                email)
                        : null,
                result.hasNext()
                        ? buildUrl(
                                query,
                                selectedType,
                                selectedSort,
                                selectedSports,
                                selectedDateRange,
                                selectedTimezone,
                                selectedPriceRange,
                                result.getPage() + 1,
                                email)
                        : null);
    }

    private FeedPageViewModel buildTournamentFeedPageViewModel(
            final User currentUser,
            final String query,
            final EventType selectedType,
            final EventSort selectedSort,
            final List<String> selectedSports,
            final DateRange selectedDateRange,
            final ZoneId selectedTimezone,
            final String selectedStartDateValue,
            final String selectedEndDateValue,
            final String selectedTimezoneValue,
            final PriceRange selectedPriceRange,
            final PaginatedResult<Tournament> result,
            final Locale locale,
            final String email,
            final ExploreLocation exploreLocation) {

        return new FeedPageViewModel(
                "",
                messageSource.getMessage("feed.hero.title", null, locale),
                messageSource.getMessage("feed.hero.description", null, locale),
                messageSource.getMessage("feed.search.placeholder", null, locale),
                messageSource.getMessage("feed.search.button", null, locale),
                List.of(),
                buildFilterGroups(
                        query,
                        selectedType,
                        selectedSort,
                        selectedSports,
                        selectedDateRange,
                        selectedTimezone,
                        selectedStartDateValue,
                        selectedEndDateValue,
                        selectedTimezoneValue,
                        selectedPriceRange,
                        locale,
                        email),
                result.getItems().stream()
                        .map(
                                tournament ->
                                        toCard(
                                                tournament,
                                                selectedTimezone,
                                                locale,
                                                currentUser,
                                                messageSource.getMessage(
                                                        "tournament.card.badge", null, locale),
                                                tournamentStatusLabel(tournament, locale),
                                                distanceLabel(tournament, exploreLocation, locale),
                                                messageSource))
                        .toList(),
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
                                        selectedTimezone,
                                        selectedPriceRange,
                                        page,
                                        email)),
                result.hasPrevious()
                        ? buildUrl(
                                query,
                                selectedType,
                                selectedSort,
                                selectedSports,
                                selectedDateRange,
                                selectedTimezone,
                                selectedPriceRange,
                                result.getPage() - 1,
                                email)
                        : null,
                result.hasNext()
                        ? buildUrl(
                                query,
                                selectedType,
                                selectedSort,
                                selectedSports,
                                selectedDateRange,
                                selectedTimezone,
                                selectedPriceRange,
                                result.getPage() + 1,
                                email)
                        : null);
    }

    private List<SelectOptionViewModel> buildSortOptions(
            final String query,
            final EventType selectedType,
            final EventSort selectedSort,
            final List<String> selectedSports,
            final DateRange selectedDateRange,
            final ZoneId selectedTimezone,
            final PriceRange selectedPriceRange,
            final Locale locale,
            final String email) {
        final List<SelectOptionViewModel> sortOptions = new ArrayList<>();
        sortOptions.add(
                sortOption(
                        query,
                        selectedType,
                        selectedSort,
                        selectedSports,
                        selectedDateRange,
                        selectedTimezone,
                        selectedPriceRange,
                        locale,
                        email,
                        EventSort.SOONEST,
                        "feed.sort.soonest"));
        sortOptions.add(
                sortOption(
                        query,
                        selectedType,
                        selectedSort,
                        selectedSports,
                        selectedDateRange,
                        selectedTimezone,
                        selectedPriceRange,
                        locale,
                        email,
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
                            selectedTimezone,
                            selectedPriceRange,
                            locale,
                            email,
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
                        selectedTimezone,
                        selectedPriceRange,
                        locale,
                        email,
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
            final ZoneId selectedTimezone,
            final PriceRange selectedPriceRange,
            final Locale locale,
            final String email,
            final EventSort sort,
            final String labelCode) {
        return new SelectOptionViewModel(
                messageSource.getMessage(labelCode, null, locale),
                null,
                buildParamsMap(
                        query,
                        1,
                        email,
                        selectedType,
                        sort,
                        selectedSports,
                        selectedDateRange,
                        selectedTimezone,
                        selectedPriceRange),
                sort == selectedSort);
    }

    private List<FilterGroupViewModel> buildFilterGroups(
            final String query,
            final EventType selectedType,
            final EventSort selectedSort,
            final List<String> selectedSports,
            final DateRange selectedDateRange,
            final ZoneId selectedTimezone,
            final String selectedStartDateValue,
            final String selectedEndDateValue,
            final String selectedTimezoneValue,
            final PriceRange selectedPriceRange,
            final Locale locale,
            final String email) {
        final List<FilterGroupViewModel> groups = new ArrayList<>();
        groups.add(
                new FilterGroupViewModel(
                        messageSource.getMessage("filter.eventType", null, locale),
                        List.of(
                                new FilterOptionViewModel(
                                        messageSource.getMessage(
                                                "filter.eventType.matches", null, locale),
                                        null,
                                        buildParamsMap(
                                                query,
                                                1,
                                                email,
                                                EventType.MATCH,
                                                selectedSort,
                                                selectedSports,
                                                selectedDateRange,
                                                selectedTimezone,
                                                selectedPriceRange),
                                        null,
                                        selectedType == EventType.MATCH),
                                new FilterOptionViewModel(
                                        messageSource.getMessage(
                                                "filter.eventType.tournaments", null, locale),
                                        null,
                                        buildParamsMap(
                                                query,
                                                1,
                                                email,
                                                EventType.TOURNAMENT,
                                                selectedSort,
                                                selectedSports,
                                                selectedDateRange,
                                                selectedTimezone,
                                                selectedPriceRange),
                                        null,
                                        selectedType == EventType.TOURNAMENT))));
        groups.add(
                new FilterGroupViewModel(
                        messageSource.getMessage("filter.categories", null, locale),
                        List.of(
                                new FilterOptionViewModel(
                                        messageSource.getMessage("filter.anySport", null, locale),
                                        null,
                                        buildParamsMap(
                                                query,
                                                1,
                                                email,
                                                selectedType,
                                                selectedSort,
                                                List.of(),
                                                selectedDateRange,
                                                selectedTimezone,
                                                selectedPriceRange),
                                        null,
                                        selectedSports.isEmpty()),
                                new FilterOptionViewModel(
                                        messageSource.getMessage("sport.football", null, locale),
                                        null,
                                        buildParamsMap(
                                                query,
                                                1,
                                                email,
                                                selectedType,
                                                selectedSort,
                                                toggleSport(selectedSports, Sport.FOOTBALL),
                                                selectedDateRange,
                                                selectedTimezone,
                                                selectedPriceRange),
                                        null,
                                        isSportSelected(selectedSports, Sport.FOOTBALL)),
                                new FilterOptionViewModel(
                                        messageSource.getMessage("sport.tennis", null, locale),
                                        null,
                                        buildParamsMap(
                                                query,
                                                1,
                                                email,
                                                selectedType,
                                                selectedSort,
                                                toggleSport(selectedSports, Sport.TENNIS),
                                                selectedDateRange,
                                                selectedTimezone,
                                                selectedPriceRange),
                                        null,
                                        isSportSelected(selectedSports, Sport.TENNIS)),
                                new FilterOptionViewModel(
                                        messageSource.getMessage("sport.basketball", null, locale),
                                        null,
                                        buildParamsMap(
                                                query,
                                                1,
                                                email,
                                                selectedType,
                                                selectedSort,
                                                toggleSport(selectedSports, Sport.BASKETBALL),
                                                selectedDateRange,
                                                selectedTimezone,
                                                selectedPriceRange),
                                        null,
                                        isSportSelected(selectedSports, Sport.BASKETBALL)),
                                new FilterOptionViewModel(
                                        messageSource.getMessage("sport.padel", null, locale),
                                        null,
                                        buildParamsMap(
                                                query,
                                                1,
                                                email,
                                                selectedType,
                                                selectedSort,
                                                toggleSport(selectedSports, Sport.PADEL),
                                                selectedDateRange,
                                                selectedTimezone,
                                                selectedPriceRange),
                                        null,
                                        isSportSelected(selectedSports, Sport.PADEL)),
                                new FilterOptionViewModel(
                                        messageSource.getMessage("sport.other", null, locale),
                                        null,
                                        buildParamsMap(
                                                query,
                                                1,
                                                email,
                                                selectedType,
                                                selectedSort,
                                                toggleSport(selectedSports, Sport.OTHER),
                                                selectedDateRange,
                                                selectedTimezone,
                                                selectedPriceRange),
                                        null,
                                        isSportSelected(selectedSports, Sport.OTHER)))));
        return List.copyOf(groups);
    }

    private static String distanceLabel(
            final Match match, final ExploreLocation exploreLocation, final Locale locale) {
        if (match == null || exploreLocation == null || !match.hasCoordinates()) {
            return null;
        }
        final double distanceKm =
                distanceInKilometers(
                        exploreLocation.latitude(),
                        exploreLocation.longitude(),
                        match.getLatitude(),
                        match.getLongitude());
        final Locale resolvedLocale = locale == null ? Locale.ENGLISH : locale;
        final NumberFormat formatter = NumberFormat.getNumberInstance(resolvedLocale);
        if (distanceKm < 1) {
            formatter.setMaximumFractionDigits(0);
            return formatter.format(Math.max(1, Math.round(distanceKm * 1000))) + " m";
        }
        formatter.setMinimumFractionDigits(distanceKm < 10 ? 1 : 0);
        formatter.setMaximumFractionDigits(distanceKm < 10 ? 1 : 0);
        return formatter.format(distanceKm) + " km";
    }

    private static String distanceLabel(
            final Tournament tournament,
            final ExploreLocation exploreLocation,
            final Locale locale) {
        if (tournament == null || exploreLocation == null || !tournament.hasCoordinates()) {
            return null;
        }
        final double distanceKm =
                distanceInKilometers(
                        exploreLocation.latitude(),
                        exploreLocation.longitude(),
                        tournament.getLatitude(),
                        tournament.getLongitude());
        final Locale resolvedLocale = locale == null ? Locale.ENGLISH : locale;
        final NumberFormat formatter = NumberFormat.getNumberInstance(resolvedLocale);
        if (distanceKm < 1) {
            formatter.setMaximumFractionDigits(0);
            return formatter.format(Math.max(1, Math.round(distanceKm * 1000))) + " m";
        }
        formatter.setMinimumFractionDigits(distanceKm < 10 ? 1 : 0);
        formatter.setMaximumFractionDigits(distanceKm < 10 ? 1 : 0);
        return formatter.format(distanceKm) + " km";
    }

    private String tournamentStatusLabel(final Tournament tournament, final Locale locale) {
        if (tournament == null || tournament.getStatus() == null) {
            return null;
        }
        return messageSource.getMessage(
                "tournament.status." + tournament.getStatus().getDbValue(), null, locale);
    }

    private static double distanceInKilometers(
            final double fromLatitude,
            final double fromLongitude,
            final double toLatitude,
            final double toLongitude) {
        final double earthRadiusKm = 6371.0088;
        final double fromLatRad = Math.toRadians(fromLatitude);
        final double toLatRad = Math.toRadians(toLatitude);
        final double deltaLatRad = Math.toRadians(toLatitude - fromLatitude);
        final double deltaLonRad = Math.toRadians(toLongitude - fromLongitude);
        final double a =
                Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2)
                        + Math.cos(fromLatRad)
                                * Math.cos(toLatRad)
                                * Math.sin(deltaLonRad / 2)
                                * Math.sin(deltaLonRad / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
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

    private static DateRange normalizeDateRange(
            final LocalDate rawStartDate,
            final LocalDate rawEndDate,
            final ZoneId
                    zoneId) { // TODO: do not change these fields in controller. Show error in form
        // validation instead.
        LocalDate startDate = rawStartDate;
        LocalDate endDate = rawEndDate;
        final LocalDate today = LocalDate.now(zoneId);

        if (startDate != null && startDate.isBefore(today)) {
            startDate = today;
        }
        if (endDate != null && endDate.isBefore(today)) {
            endDate = today;
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return new DateRange(endDate, startDate);
        }

        return new DateRange(startDate, endDate);
    }

    private static PriceRange
            normalizePriceRange( // TODO: do not change these fields in controller. Show error in
                    // form validation instead.
                    final BigDecimal rawMinPrice,
                    final BigDecimal rawMaxPrice) {
        final BigDecimal minPrice =
                rawMinPrice == null || rawMinPrice.compareTo(BigDecimal.ZERO) < 0
                        ? null
                        : rawMinPrice.stripTrailingZeros();
        final BigDecimal maxPrice =
                rawMaxPrice == null || rawMaxPrice.compareTo(BigDecimal.ZERO) < 0
                        ? null
                        : rawMaxPrice.stripTrailingZeros();

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            return new PriceRange(maxPrice, minPrice);
        }

        return new PriceRange(minPrice, maxPrice);
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
            final String email,
            final EventType selectedType,
            final EventSort selectedSort,
            final List<String> selectedSports,
            final DateRange selectedDateRange,
            final ZoneId selectedTimezone,
            final PriceRange selectedPriceRange) {
        final Map<String, String> params = new LinkedHashMap<>();
        params.put("q", query == null ? "" : query);
        params.put("sort", selectedSort.getQueryValue());
        params.put("page", Integer.toString(page));
        if (email != null && !email.isBlank()) {
            params.put("email", email);
        }
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
        if (selectedTimezone != null) {
            params.put("tz", selectedTimezone.getId());
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
            final ZoneId selectedTimezone,
            final PriceRange selectedPriceRange,
            final int page,
            final String email) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/");
        buildParamsMap(
                        query,
                        page,
                        email,
                        selectedType,
                        selectedSort,
                        selectedSports,
                        selectedDateRange,
                        selectedTimezone,
                        selectedPriceRange)
                .forEach(builder::queryParam);
        return builder.build().encode().toUriString();
    }

    private record PriceRange(BigDecimal minPrice, BigDecimal maxPrice) {}

    private record DateRange(LocalDate startDate, LocalDate endDate) {}

    private record ExploreLocation(Double latitude, Double longitude) {}
}
