package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.EventCardViewModelUtils.toCard;
import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.encodeCsv;
import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.normalizeCsvValues;
import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.normalizeSort;
import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.toggleValue;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.FeedSearchForm;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.PaginationUtils;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FeedPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FilterGroupViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FilterOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.SelectOptionViewModel;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class FeedController {

    private static final int PAGE_SIZE = 12;
    private static final String SESSION_EXPLORE_LATITUDE = "exploreLocationLatitude";
    private static final String SESSION_EXPLORE_LONGITUDE = "exploreLocationLongitude";

    private final MatchService matchService;
    private final MatchParticipationService matchParticipationService;
    private final MatchReservationService matchReservationService;
    private final UserService userService;
    private final MessageSource messageSource;

    @Autowired
    public FeedController(
            final MatchService matchService,
            final MatchParticipationService matchParticipationService,
            final MatchReservationService matchReservationService,
            final UserService userService,
            final MessageSource messageSource) {
        this.matchService = matchService;
        this.matchParticipationService = matchParticipationService;
        this.matchReservationService = matchReservationService;
        this.userService = userService;
        this.messageSource = messageSource;
    }

    @GetMapping("/")
    public ModelAndView showFeed(
            @Valid @ModelAttribute("feedSearchForm") final FeedSearchForm feedSearchForm,
            final BindingResult bindingResult,
            @RequestParam(value = "email", required = false) final String email,
            @RequestParam(value = "sport", required = false) final List<String> sports,
            @RequestParam(value = "startDate", required = false) final String startDate,
            @RequestParam(value = "endDate", required = false) final String endDate,
            @RequestParam(value = "sort", defaultValue = "soonest") final String sort,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            @RequestParam(value = "minPrice", required = false) final String minPrice,
            @RequestParam(value = "maxPrice", required = false) final String maxPrice,
            @RequestParam(value = "tz", required = false) final String timezone,
            final HttpSession session,
            final Locale locale) {
        final String query =
                bindingResult.hasFieldErrors("q") || feedSearchForm.getQ() == null
                        ? ""
                        : feedSearchForm.getQ();
        final boolean nearMeUnavailable =
                exploreLocation(session) == null && "distance".equals(sort);
        FeedFilters filters =
                normalizeFilters(sports, startDate, endDate, sort, timezone, minPrice, maxPrice);
        final ExploreLocation exploreLocation = exploreLocation(session);
        if (exploreLocation == null && "distance".equals(filters.selectedSort())) {
            filters = filters.withSort("soonest");
        }
        final PaginatedResult<Match> result =
                searchPublicMatches(query, filters, page, exploreLocation);
        final ModelAndView mav = new ModelAndView("feed/index");
        mav.addObject("shell", ShellViewModelFactory.playerShell(messageSource, locale, "/"));
        mav.addObject("selectedSort", filters.selectedSort());
        mav.addObject("selectedSports", filters.selectedSports());
        mav.addObject("selectedTimezone", filters.timezone());
        mav.addObject("selectedMinPrice", filters.minPrice());
        mav.addObject("selectedMaxPrice", filters.maxPrice());
        mav.addObject("selectedMinPriceValue", formatNullablePriceValue(filters.minPrice()));
        mav.addObject("selectedMaxPriceValue", formatNullablePriceValue(filters.maxPrice()));
        mav.addObject(
                "selectedDateMinValue", LocalDate.now(parseZone(filters.timezone())).toString());
        mav.addObject("selectedStartDateValue", filters.startDate());
        mav.addObject("selectedEndDateValue", filters.endDate());
        mav.addObject("sortLabel", messageSource.getMessage("feed.sortBy", null, locale));
        mav.addObject("sortOptions", buildSortOptions(query, filters, locale, email));
        mav.addObject("nearMeUnavailable", nearMeUnavailable);
        mav.addObject(
                "feedPage",
                buildFeedPageViewModel(query, filters, result, locale, email, exploreLocation));
        mav.addObject("nearMeAvailable", exploreLocation != null);
        return mav;
    }

    @PostMapping("/explore/location")
    public ModelAndView storeExploreLocation(
            @RequestParam(value = "latitude", required = false) final String latitude,
            @RequestParam(value = "longitude", required = false) final String longitude,
            final HttpSession session) {
        final Double parsedLatitude = parseCoordinate(latitude);
        final Double parsedLongitude = parseCoordinate(longitude);
        if (parsedLatitude != null
                && parsedLatitude >= -90
                && parsedLatitude <= 90
                && parsedLongitude != null
                && parsedLongitude >= -180
                && parsedLongitude <= 180) {
            session.setAttribute(SESSION_EXPLORE_LATITUDE, parsedLatitude);
            session.setAttribute(SESSION_EXPLORE_LONGITUDE, parsedLongitude);
        }
        return new ModelAndView("redirect:/?sort=distance");
    }

    private FeedPageViewModel buildFeedPageViewModel(
            final String query,
            final FeedFilters filters,
            final PaginatedResult<Match> result,
            final Locale locale,
            final String email,
            final ExploreLocation exploreLocation) {

        final ZoneId zoneId = parseZone(filters.timezone());
        final Long currentUserId =
                CurrentAuthenticatedUser.get().map(user -> user.getUserId()).orElse(null);

        return new FeedPageViewModel(
                "",
                messageSource.getMessage("feed.hero.title", null, locale),
                messageSource.getMessage("feed.hero.description", null, locale),
                messageSource.getMessage("feed.search.placeholder", null, locale),
                messageSource.getMessage("feed.search.button", null, locale),
                List.of(),
                buildFilterGroups(query, filters, locale, email),
                result.getItems().stream()
                        .map(
                                match ->
                                        toCard(
                                                match,
                                                zoneId,
                                                locale,
                                                currentUserId,
                                                messageSource.getMessage(
                                                        "event.spotsLeft",
                                                        new Object[] {match.getAvailableSpots()},
                                                        locale),
                                                distanceLabel(match, exploreLocation, locale),
                                                messageSource,
                                                userService,
                                                matchParticipationService,
                                                matchReservationService))
                        .toList(),
                result.getPage(),
                result.getTotalPages(),
                PaginationUtils.buildPaginationItems(
                        result.getPage(),
                        result.getTotalPages(),
                        page -> buildUrl(query, filters, page, email, locale)),
                result.hasPrevious()
                        ? buildUrl(query, filters, result.getPage() - 1, email, locale)
                        : null,
                result.hasNext()
                        ? buildUrl(query, filters, result.getPage() + 1, email, locale)
                        : null);
    }

    private PaginatedResult<Match> searchPublicMatches(
            final String query,
            final FeedFilters filters,
            final int page,
            final ExploreLocation exploreLocation) {
        final int safePage = page > 0 ? page : 1;
        final String encodedSports = encodeCsv(filters.selectedSports());

        return matchService.searchPublicMatches(
                query,
                encodedSports,
                filters.startDate(),
                filters.endDate(),
                filters.selectedSort(),
                safePage,
                PAGE_SIZE,
                filters.timezone(),
                filters.minPrice(),
                filters.maxPrice(),
                exploreLocation == null ? null : exploreLocation.latitude(),
                exploreLocation == null ? null : exploreLocation.longitude());
    }

    private List<SelectOptionViewModel> buildSortOptions(
            final String query,
            final FeedFilters filters,
            final Locale locale,
            final String email) {
        final List<SelectOptionViewModel> sortOptions = new ArrayList<>();
        sortOptions.add(sortOption(query, filters, locale, email, "soonest", "feed.sort.soonest"));
        sortOptions.add(sortOption(query, filters, locale, email, "price", "feed.sort.price"));
        sortOptions.add(sortOption(query, filters, locale, email, "spots", "feed.sort.spots"));
        sortOptions.add(
                sortOption(query, filters, locale, email, "distance", "feed.sort.distance"));
        return List.copyOf(sortOptions);
    }

    private SelectOptionViewModel sortOption(
            final String query,
            final FeedFilters filters,
            final Locale locale,
            final String email,
            final String sort,
            final String labelCode) {
        return new SelectOptionViewModel(
                messageSource.getMessage(labelCode, null, locale),
                buildUrl(query, filters.withSort(sort), 1, email, locale),
                sort.equals(filters.selectedSort()));
    }

    private List<FilterGroupViewModel> buildFilterGroups(
            final String query,
            final FeedFilters filters,
            final Locale locale,
            final String email) {
        final List<String> selectedSports = filters.selectedSports();

        return List.of(
                new FilterGroupViewModel(
                        messageSource.getMessage("filter.categories", null, locale),
                        List.of(
                                new FilterOptionViewModel(
                                        messageSource.getMessage("filter.anySport", null, locale),
                                        buildUrl(
                                                query,
                                                filters.withSports(List.of()),
                                                1,
                                                email,
                                                locale),
                                        null,
                                        selectedSports.isEmpty()),
                                new FilterOptionViewModel(
                                        messageSource.getMessage("sport.football", null, locale),
                                        buildUrl(
                                                query,
                                                filters.withSports(
                                                        toggleSport(
                                                                selectedSports, Sport.FOOTBALL)),
                                                1,
                                                email,
                                                locale),
                                        null,
                                        isSportSelected(selectedSports, Sport.FOOTBALL)),
                                new FilterOptionViewModel(
                                        messageSource.getMessage("sport.tennis", null, locale),
                                        buildUrl(
                                                query,
                                                filters.withSports(
                                                        toggleSport(selectedSports, Sport.TENNIS)),
                                                1,
                                                email,
                                                locale),
                                        null,
                                        isSportSelected(selectedSports, Sport.TENNIS)),
                                new FilterOptionViewModel(
                                        messageSource.getMessage("sport.basketball", null, locale),
                                        buildUrl(
                                                query,
                                                filters.withSports(
                                                        toggleSport(
                                                                selectedSports, Sport.BASKETBALL)),
                                                1,
                                                email,
                                                locale),
                                        null,
                                        isSportSelected(selectedSports, Sport.BASKETBALL)),
                                new FilterOptionViewModel(
                                        messageSource.getMessage("sport.padel", null, locale),
                                        buildUrl(
                                                query,
                                                filters.withSports(
                                                        toggleSport(selectedSports, Sport.PADEL)),
                                                1,
                                                email,
                                                locale),
                                        null,
                                        isSportSelected(selectedSports, Sport.PADEL)),
                                new FilterOptionViewModel(
                                        messageSource.getMessage("sport.other", null, locale),
                                        buildUrl(
                                                query,
                                                filters.withSports(
                                                        toggleSport(selectedSports, Sport.OTHER)),
                                                1,
                                                email,
                                                locale),
                                        null,
                                        isSportSelected(selectedSports, Sport.OTHER)))));
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

    private static ZoneId parseZone(final String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.systemDefault();
        }

        try {
            return ZoneId.of(timezone);
        } catch (final Exception ignored) {
            return ZoneId.systemDefault();
        }
    }

    private static String buildUrl(
            final String query,
            final FeedFilters filters,
            final int page,
            final String email,
            final Locale locale) {
        final UriComponentsBuilder builder =
                UriComponentsBuilder.fromPath("/")
                        .queryParam("q", query == null ? "" : query)
                        .queryParam("sort", filters.selectedSort())
                        .queryParam("page", page);

        if (email != null && !email.isBlank()) {
            builder.queryParam("email", email);
        }

        for (final String sport : filters.selectedSports()) {
            builder.queryParam("sport", sport.toLowerCase(Locale.ROOT));
        }
        if (filters.startDate() != null && !filters.startDate().isBlank()) {
            builder.queryParam("startDate", filters.startDate());
        }
        if (filters.endDate() != null && !filters.endDate().isBlank()) {
            builder.queryParam("endDate", filters.endDate());
        }
        if (filters.timezone() != null && !filters.timezone().isBlank()) {
            builder.queryParam("tz", filters.timezone());
        }
        if (filters.minPrice() != null) {
            builder.queryParam("minPrice", formatPriceValue(filters.minPrice()));
        }
        if (filters.maxPrice() != null) {
            builder.queryParam("maxPrice", formatPriceValue(filters.maxPrice()));
        }

        return builder.build().encode().toUriString();
    }

    private static List<String> normalizeSports(final List<String> sports) {
        final List<String> normalized = normalizeCsvValues(sports);
        final List<String> validSports = new ArrayList<>();
        for (final String sport : normalized) {
            Sport.fromDbValue(sport).map(Sport::getDbValue).ifPresent(validSports::add);
        }

        return List.copyOf(validSports);
    }

    private static FeedFilters normalizeFilters(
            final List<String> sports,
            final String startDate,
            final String endDate,
            final String sort,
            final String timezone,
            final String minPrice,
            final String maxPrice) {
        final String normalizedTimezone = normalizeTimezone(timezone);
        final PriceRange priceRange = normalizePriceRange(minPrice, maxPrice);
        final DateRange dateRange = normalizeDateRange(startDate, endDate, normalizedTimezone);

        return new FeedFilters(
                normalizeSports(sports),
                dateRange.startDate(),
                dateRange.endDate(),
                normalizeSort(sort),
                normalizedTimezone,
                priceRange.minPrice(),
                priceRange.maxPrice());
    }

    private static DateRange normalizeDateRange(
            final String rawStartDate, final String rawEndDate, final String timezone) {
        final LocalDate today = LocalDate.now(parseZone(timezone));
        LocalDate startDate = parseDate(rawStartDate);
        LocalDate endDate = parseDate(rawEndDate);

        if (startDate != null && startDate.isBefore(today)) {
            startDate = today;
        }
        if (endDate != null && endDate.isBefore(today)) {
            endDate = today;
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return new DateRange(endDate.toString(), startDate.toString());
        }

        return new DateRange(
                startDate == null ? null : startDate.toString(),
                endDate == null ? null : endDate.toString());
    }

    private static LocalDate parseDate(final String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(rawDate.trim());
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static String normalizeTimezone(final String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return null;
        }
        return timezone.trim();
    }

    private static PriceRange normalizePriceRange(
            final String rawMinPrice, final String rawMaxPrice) {
        final BigDecimal minPrice = parseNonNegativePrice(rawMinPrice);
        final BigDecimal maxPrice = parseNonNegativePrice(rawMaxPrice);

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            return new PriceRange(maxPrice, minPrice);
        }

        return new PriceRange(minPrice, maxPrice);
    }

    private static Double parseCoordinate(final String rawCoordinate) {
        if (rawCoordinate == null || rawCoordinate.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(rawCoordinate.trim());
        } catch (final NumberFormatException exception) {
            return null;
        }
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

    private static BigDecimal parseNonNegativePrice(final String rawPrice) {
        if (rawPrice == null || rawPrice.isBlank()) {
            return null;
        }

        try {
            final BigDecimal price = new BigDecimal(rawPrice.trim());
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                return null;
            }
            return price.stripTrailingZeros();
        } catch (final NumberFormatException exception) {
            return null;
        }
    }

    private static boolean isSportSelected(final List<String> selectedSports, final Sport sport) {
        return normalizeSports(selectedSports).contains(sport.getDbValue());
    }

    private static List<String> toggleSport(
            final List<String> selectedSports, final Sport sportToToggle) {
        return toggleValue(normalizeSports(selectedSports), sportToToggle.getDbValue());
    }

    private static String formatPriceValue(final BigDecimal price) {
        return price.stripTrailingZeros().toPlainString();
    }

    private static String formatNullablePriceValue(final BigDecimal price) {
        return price == null ? "" : formatPriceValue(price);
    }

    private record PriceRange(BigDecimal minPrice, BigDecimal maxPrice) {}

    private record DateRange(String startDate, String endDate) {}

    private record ExploreLocation(Double latitude, Double longitude) {}

    private record FeedFilters(
            List<String> selectedSports,
            String startDate,
            String endDate,
            String selectedSort,
            String timezone,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        private FeedFilters withSports(final List<String> sports) {
            return new FeedFilters(
                    normalizeSports(sports),
                    startDate,
                    endDate,
                    selectedSort,
                    timezone,
                    minPrice,
                    maxPrice);
        }

        private FeedFilters withSort(final String sort) {
            return new FeedFilters(
                    selectedSports,
                    startDate,
                    endDate,
                    normalizeSort(sort),
                    timezone,
                    minPrice,
                    maxPrice);
        }
    }
}
