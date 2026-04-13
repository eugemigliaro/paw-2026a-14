package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.bannerUrlFor;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.webapp.form.FeedSearchForm;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import ar.edu.itba.paw.webapp.viewmodel.WebViewModels.EventCardViewModel;
import ar.edu.itba.paw.webapp.viewmodel.WebViewModels.FeedPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.WebViewModels.FilterGroupViewModel;
import ar.edu.itba.paw.webapp.viewmodel.WebViewModels.FilterOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.WebViewModels.PaginationItemViewModel;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class FeedController {

    private static final int PAGE_SIZE = 12;
    private static final DateTimeFormatter SCHEDULE_FORMATTER =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT);

    private final MatchService matchService;

    @Autowired
    public FeedController(final MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/")
    public ModelAndView showFeed(
            @Valid @ModelAttribute("feedSearchForm") final FeedSearchForm feedSearchForm,
            final BindingResult bindingResult,
            @RequestParam(value = "sport", required = false) final List<String> sports,
            @RequestParam(value = "time", defaultValue = "all") final String time,
            @RequestParam(value = "sort", defaultValue = "soonest") final String sort,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            @RequestParam(value = "minPrice", required = false) final String minPrice,
            @RequestParam(value = "maxPrice", required = false) final String maxPrice,
            @RequestParam(value = "tz", required = false) final String timezone) {
        final String query =
                bindingResult.hasFieldErrors("q") || feedSearchForm.getQ() == null
                        ? ""
                        : feedSearchForm.getQ();
        final FeedFilters filters =
                normalizeFilters(sports, time, sort, timezone, minPrice, maxPrice);
        final PaginatedResult<Match> result = searchPublicMatches(query, filters, page);
        final ModelAndView mav = new ModelAndView("feed/index");
        mav.addObject("shell", ShellViewModelFactory.browseShell());
        mav.addObject("selectedSort", filters.selectedSort());
        mav.addObject("selectedTime", filters.selectedTime());
        mav.addObject("selectedSports", filters.selectedSports());
        mav.addObject("selectedTimezone", filters.timezone());
        mav.addObject("selectedMinPrice", filters.minPrice());
        mav.addObject("selectedMaxPrice", filters.maxPrice());
        mav.addObject("selectedMinPriceValue", formatNullablePriceValue(filters.minPrice()));
        mav.addObject("selectedMaxPriceValue", formatNullablePriceValue(filters.maxPrice()));
        mav.addObject("feedPage", buildFeedPageViewModel(query, filters, result));
        return mav;
    }

    private FeedPageViewModel buildFeedPageViewModel(
            final String query, final FeedFilters filters, final PaginatedResult<Match> result) {

        final ZoneId zoneId = parseZone(filters.timezone());

        return new FeedPageViewModel(
                "",
                "Find your next game.",
                "Discover local sports matches, join communities, and get active with "
                        + "Match Point.",
                "What sports match are you looking for?",
                "Find Matches",
                List.of(),
                buildFilterGroups(query, filters),
                result.getItems().stream().map(match -> toCard(match, zoneId)).toList(),
                result.getPage(),
                result.getTotalPages(),
                buildPaginationItems(query, filters, result.getPage(), result.getTotalPages()),
                result.hasPrevious() ? buildUrl(query, filters, result.getPage() - 1) : null,
                result.hasNext() ? buildUrl(query, filters, result.getPage() + 1) : null);
    }

    private PaginatedResult<Match> searchPublicMatches(
            final String query, final FeedFilters filters, final int page) {
        final int safePage = page > 0 ? page : 1;
        final String encodedSports = encodeSports(filters.selectedSports());

        if (!filters.hasExtraFilters()) {
            return matchService.searchPublicMatches(
                    query,
                    encodedSports,
                    filters.selectedTime(),
                    filters.selectedSort(),
                    safePage,
                    PAGE_SIZE,
                    filters.timezone());
        }

        final PaginatedResult<Match> firstPage =
                matchService.searchPublicMatches(
                        query,
                        encodedSports,
                        filters.selectedTime(),
                        filters.selectedSort(),
                        1,
                        PAGE_SIZE,
                        filters.timezone());

        final List<Match> filteredMatches = new ArrayList<>();
        appendFilteredMatches(filteredMatches, firstPage.getItems(), filters);

        for (int currentPage = 2; currentPage <= firstPage.getTotalPages(); currentPage++) {
            final PaginatedResult<Match> nextPage =
                    matchService.searchPublicMatches(
                            query,
                            encodedSports,
                            filters.selectedTime(),
                            filters.selectedSort(),
                            currentPage,
                            PAGE_SIZE,
                            filters.timezone());
            appendFilteredMatches(filteredMatches, nextPage.getItems(), filters);
        }

        final int fromIndex = Math.min((safePage - 1) * PAGE_SIZE, filteredMatches.size());
        final int toIndex = Math.min(fromIndex + PAGE_SIZE, filteredMatches.size());

        return new PaginatedResult<>(
                List.copyOf(filteredMatches.subList(fromIndex, toIndex)),
                filteredMatches.size(),
                safePage,
                PAGE_SIZE);
    }

    private static void appendFilteredMatches(
            final List<Match> destination, final List<Match> matches, final FeedFilters filters) {
        for (final Match match : matches) {
            if (matchesFilters(match, filters)) {
                destination.add(match);
            }
        }
    }

    private static boolean matchesFilters(final Match match, final FeedFilters filters) {
        return matchesSports(match, filters.selectedSports())
                && matchesPriceRange(match, filters.minPrice(), filters.maxPrice());
    }

    private static boolean matchesSports(final Match match, final List<String> selectedSports) {
        if (selectedSports == null || selectedSports.isEmpty()) {
            return true;
        }

        return selectedSports.contains(match.getSport().getDbValue());
    }

    private static boolean matchesPriceRange(
            final Match match, final BigDecimal minPrice, final BigDecimal maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return true;
        }

        if (match.getPricePerPlayer() == null) {
            return false;
        }

        if (minPrice != null && match.getPricePerPlayer().compareTo(minPrice) < 0) {
            return false;
        }

        return maxPrice == null || match.getPricePerPlayer().compareTo(maxPrice) <= 0;
    }

    private static List<PaginationItemViewModel> buildPaginationItems(
            final String query,
            final FeedFilters filters,
            final int currentPage,
            final int totalPages) {
        if (totalPages <= 1) {
            return List.of();
        }

        final List<PaginationItemViewModel> items = new ArrayList<>();
        final int startPage = Math.max(2, Math.min(currentPage - 1, totalPages - 3));
        final int endPage = Math.min(totalPages - 1, Math.max(currentPage + 1, 4));

        items.add(pageItem(1, query, filters, currentPage));

        if (startPage > 2) {
            items.add(new PaginationItemViewModel("...", null, false, true));
        }

        for (int page = startPage; page <= endPage; page++) {
            items.add(pageItem(page, query, filters, currentPage));
        }

        if (endPage < totalPages - 1) {
            items.add(new PaginationItemViewModel("...", null, false, true));
        }

        items.add(pageItem(totalPages, query, filters, currentPage));

        return items;
    }

    private static PaginationItemViewModel pageItem(
            final int page, final String query, final FeedFilters filters, final int currentPage) {
        return new PaginationItemViewModel(
                Integer.toString(page), buildUrl(query, filters, page), page == currentPage, false);
    }

    private static List<FilterGroupViewModel> buildFilterGroups(
            final String query, final FeedFilters filters) {
        final List<String> selectedSports = filters.selectedSports();
        final String selectedTime = filters.selectedTime();
        final String selectedSort = filters.selectedSort();
        final String timezone = filters.timezone();

        return List.of(
                new FilterGroupViewModel(
                        "Sports",
                        List.of(
                                new FilterOptionViewModel(
                                        "Football",
                                        buildUrl(
                                                query,
                                                filters.withSports(
                                                        toggleSport(
                                                                selectedSports, Sport.FOOTBALL)),
                                                1),
                                        null,
                                        isSportSelected(selectedSports, Sport.FOOTBALL)),
                                new FilterOptionViewModel(
                                        "Tennis",
                                        buildUrl(
                                                query,
                                                filters.withSports(
                                                        toggleSport(selectedSports, Sport.TENNIS)),
                                                1),
                                        null,
                                        isSportSelected(selectedSports, Sport.TENNIS)),
                                new FilterOptionViewModel(
                                        "Basketball",
                                        buildUrl(
                                                query,
                                                filters.withSports(
                                                        toggleSport(
                                                                selectedSports, Sport.BASKETBALL)),
                                                1),
                                        null,
                                        isSportSelected(selectedSports, Sport.BASKETBALL)),
                                new FilterOptionViewModel(
                                        "Padel",
                                        buildUrl(
                                                query,
                                                filters.withSports(
                                                        toggleSport(selectedSports, Sport.PADEL)),
                                                1),
                                        null,
                                        isSportSelected(selectedSports, Sport.PADEL)))),
                new FilterGroupViewModel(
                        "Time",
                        List.of(
                                new FilterOptionViewModel(
                                        "Today",
                                        buildUrl(
                                                query,
                                                filters.withTime(toggleTime(selectedTime, "today")),
                                                1),
                                        null,
                                        "today".equalsIgnoreCase(selectedTime)),
                                new FilterOptionViewModel(
                                        "Tomorrow",
                                        buildUrl(
                                                query,
                                                filters.withTime(
                                                        toggleTime(selectedTime, "tomorrow")),
                                                1),
                                        null,
                                        "tomorrow".equalsIgnoreCase(selectedTime)),
                                new FilterOptionViewModel(
                                        "This week",
                                        buildUrl(
                                                query,
                                                filters.withTime(toggleTime(selectedTime, "week")),
                                                1),
                                        null,
                                        "week".equalsIgnoreCase(selectedTime)))));
    }

    private static EventCardViewModel toCard(final Match match, final ZoneId zoneId) {
        final String schedule = SCHEDULE_FORMATTER.format(match.getStartsAt().atZone(zoneId));
        final String priceLabel =
                match.getPricePerPlayer() == null
                        ? "Price TBD"
                        : match.getPricePerPlayer().compareTo(BigDecimal.ZERO) == 0
                                ? "Free"
                                : "$" + match.getPricePerPlayer();

        return new EventCardViewModel(
                String.valueOf(match.getId()),
                "/matches/" + match.getId(),
                match.getSport().getDisplayName(),
                match.getTitle(),
                match.getAddress(),
                schedule,
                priceLabel,
                match.getAvailableSpots() + " spots left",
                null,
                mediaClassFor(match.getSport()),
                bannerUrlFor(match));
    }

    private static String mediaClassFor(final Sport sport) {
        switch (sport) {
            case FOOTBALL:
                return "media-tile--football";
            case TENNIS:
                return "media-tile--tennis";
            case BASKETBALL:
                return "media-tile--basketball";
            case PADEL:
            default:
                return "media-tile--padel";
        }
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

    private static String buildUrl(final String query, final FeedFilters filters, final int page) {
        final UriComponentsBuilder builder =
                UriComponentsBuilder.fromPath("/")
                        .queryParam("q", query == null ? "" : query)
                        .queryParam("time", filters.selectedTime())
                        .queryParam("sort", filters.selectedSort())
                        .queryParam("page", page);

        for (final String sport : filters.selectedSports()) {
            builder.queryParam("sport", sport.toLowerCase(Locale.ROOT));
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

    private static String normalizeSort(final String sort) {
        if (sort == null || sort.isBlank()) {
            return MatchSort.SOONEST.getQueryValue();
        }
        return MatchSort.fromQueryValue(sort)
                .map(MatchSort::getQueryValue)
                .orElse(MatchSort.SOONEST.getQueryValue());
    }

    private static String normalizeTime(final String time) {
        if (time == null || time.isBlank()) {
            return "all";
        }
        switch (time.toLowerCase(Locale.ROOT)) {
            case "today":
                return "today";
            case "tomorrow":
                return "tomorrow";
            case "week":
                return "week";
            default:
                return "all";
        }
    }

    private static List<String> normalizeSports(final List<String> sports) {
        if (sports == null || sports.isEmpty()) {
            return List.of();
        }

        final LinkedHashSet<String> normalizedSports = new LinkedHashSet<>();
        for (final String sportValue : sports) {
            if (sportValue == null || sportValue.isBlank()) {
                continue;
            }
            for (final String sport : sportValue.split(",")) {
                if (sport == null || sport.isBlank()) {
                    continue;
                }
                Sport.fromDbValue(sport.trim())
                        .map(Sport::getDbValue)
                        .ifPresent(normalizedSports::add);
            }
        }

        return List.copyOf(normalizedSports);
    }

    private static FeedFilters normalizeFilters(
            final List<String> sports,
            final String time,
            final String sort,
            final String timezone,
            final String minPrice,
            final String maxPrice) {
        final PriceRange priceRange = normalizePriceRange(minPrice, maxPrice);

        return new FeedFilters(
                normalizeSports(sports),
                normalizeTime(time),
                normalizeSort(sort),
                normalizeTimezone(timezone),
                priceRange.minPrice(),
                priceRange.maxPrice());
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

    private static String encodeSports(final List<String> sports) {
        return String.join(",", normalizeSports(sports));
    }

    private static List<String> toggleSport(
            final List<String> selectedSports, final Sport sportToToggle) {
        final List<String> toggledSports = new ArrayList<>(normalizeSports(selectedSports));
        if (!toggledSports.remove(sportToToggle.getDbValue())) {
            toggledSports.add(sportToToggle.getDbValue());
        }

        return List.copyOf(toggledSports);
    }

    private static String toggleTime(final String selectedTime, final String timeToToggle) {
        return timeToToggle.equalsIgnoreCase(selectedTime) ? "all" : timeToToggle;
    }

    private static String formatPriceValue(final BigDecimal price) {
        return price.stripTrailingZeros().toPlainString();
    }

    private static String formatNullablePriceValue(final BigDecimal price) {
        return price == null ? "" : formatPriceValue(price);
    }

    private record PriceRange(BigDecimal minPrice, BigDecimal maxPrice) {}

    private record FeedFilters(
            List<String> selectedSports,
            String selectedTime,
            String selectedSort,
            String timezone,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        private FeedFilters withSports(final List<String> sports) {
            return new FeedFilters(
                    normalizeSports(sports),
                    selectedTime,
                    selectedSort,
                    timezone,
                    minPrice,
                    maxPrice);
        }

        private FeedFilters withTime(final String time) {
            return new FeedFilters(
                    selectedSports,
                    normalizeTime(time),
                    selectedSort,
                    timezone,
                    minPrice,
                    maxPrice);
        }

        private boolean hasExtraFilters() {
            return minPrice != null || maxPrice != null || selectedSports.size() > 1;
        }
    }
}
