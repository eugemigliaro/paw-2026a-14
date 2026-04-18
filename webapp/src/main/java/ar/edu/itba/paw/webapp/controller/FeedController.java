package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.bannerUrlFor;
import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.encodeCsv;
import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.normalizeCsvValues;
import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.normalizeSort;
import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.normalizeTime;
import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.toggleValue;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.webapp.form.FeedSearchForm;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.EventCardViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FeedPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FilterGroupViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FilterOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.PaginationItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
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

    private final MatchService matchService;
    private final MessageSource messageSource;

    @Autowired
    public FeedController(final MatchService matchService, final MessageSource messageSource) {
        this.matchService = matchService;
        this.messageSource = messageSource;
    }

    @GetMapping("/")
    public ModelAndView showFeed(
            @Valid @ModelAttribute("feedSearchForm") final FeedSearchForm feedSearchForm,
            final BindingResult bindingResult,
            @RequestParam(value = "email", required = false) final String email,
            @RequestParam(value = "sport", required = false) final List<String> sports,
            @RequestParam(value = "time", defaultValue = "all") final String time,
            @RequestParam(value = "sort", defaultValue = "soonest") final String sort,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            @RequestParam(value = "minPrice", required = false) final String minPrice,
            @RequestParam(value = "maxPrice", required = false) final String maxPrice,
            @RequestParam(value = "tz", required = false) final String timezone,
            final Locale locale) {
        final String query =
                bindingResult.hasFieldErrors("q") || feedSearchForm.getQ() == null
                        ? ""
                        : feedSearchForm.getQ();
        final FeedFilters filters =
                normalizeFilters(sports, time, sort, timezone, minPrice, maxPrice);
        final PaginatedResult<Match> result = searchPublicMatches(query, filters, page);
        final ModelAndView mav = new ModelAndView("feed/index");
        mav.addObject("shell", ShellViewModelFactory.playerShell(messageSource, locale, "/"));
        mav.addObject("selectedSort", filters.selectedSort());
        mav.addObject("selectedTime", filters.selectedTime());
        mav.addObject("selectedSports", filters.selectedSports());
        mav.addObject("selectedTimezone", filters.timezone());
        mav.addObject("selectedMinPrice", filters.minPrice());
        mav.addObject("selectedMaxPrice", filters.maxPrice());
        mav.addObject("selectedMinPriceValue", formatNullablePriceValue(filters.minPrice()));
        mav.addObject("selectedMaxPriceValue", formatNullablePriceValue(filters.maxPrice()));
        mav.addObject("feedPage", buildFeedPageViewModel(query, filters, result, locale, email));
        return mav;
    }

    private FeedPageViewModel buildFeedPageViewModel(
            final String query,
            final FeedFilters filters,
            final PaginatedResult<Match> result,
            final Locale locale,
            final String email) {

        final ZoneId zoneId = parseZone(filters.timezone());

        return new FeedPageViewModel(
                "",
                messageSource.getMessage("feed.hero.title", null, locale),
                messageSource.getMessage("feed.hero.description", null, locale),
                messageSource.getMessage("feed.search.placeholder", null, locale),
                messageSource.getMessage("feed.search.button", null, locale),
                List.of(),
                buildFilterGroups(query, filters, locale, email),
                result.getItems().stream().map(match -> toCard(match, zoneId, locale)).toList(),
                result.getPage(),
                result.getTotalPages(),
                buildPaginationItems(
                        query, filters, result.getPage(), result.getTotalPages(), email, locale),
                result.hasPrevious()
                        ? buildUrl(query, filters, result.getPage() - 1, email, locale)
                        : null,
                result.hasNext()
                        ? buildUrl(query, filters, result.getPage() + 1, email, locale)
                        : null);
    }

    private PaginatedResult<Match> searchPublicMatches(
            final String query, final FeedFilters filters, final int page) {
        final int safePage = page > 0 ? page : 1;
        final String encodedSports = encodeCsv(filters.selectedSports());

        return matchService.searchPublicMatches(
                query,
                encodedSports,
                filters.selectedTime(),
                filters.selectedSort(),
                safePage,
                PAGE_SIZE,
                filters.timezone(),
                filters.minPrice(),
                filters.maxPrice());
    }

    private static List<PaginationItemViewModel> buildPaginationItems(
            final String query,
            final FeedFilters filters,
            final int currentPage,
            final int totalPages,
            final String email,
            final Locale locale) {
        if (totalPages <= 1) {
            return List.of();
        }

        final List<PaginationItemViewModel> items = new ArrayList<>();
        final int startPage = Math.max(2, Math.min(currentPage - 1, totalPages - 3));
        final int endPage = Math.min(totalPages - 1, Math.max(currentPage + 1, 4));

        items.add(pageItem(1, query, filters, currentPage, email, locale));

        if (startPage > 2) {
            items.add(new PaginationItemViewModel("...", null, false, true));
        }

        for (int page = startPage; page <= endPage; page++) {
            items.add(pageItem(page, query, filters, currentPage, email, locale));
        }

        if (endPage < totalPages - 1) {
            items.add(new PaginationItemViewModel("...", null, false, true));
        }

        items.add(pageItem(totalPages, query, filters, currentPage, email, locale));

        return items;
    }

    private static PaginationItemViewModel pageItem(
            final int page,
            final String query,
            final FeedFilters filters,
            final int currentPage,
            final String email,
            final Locale locale) {
        return new PaginationItemViewModel(
                Integer.toString(page),
                buildUrl(query, filters, page, email, locale),
                page == currentPage,
                false);
    }

    private List<FilterGroupViewModel> buildFilterGroups(
            final String query,
            final FeedFilters filters,
            final Locale locale,
            final String email) {
        final List<String> selectedSports = filters.selectedSports();
        final String selectedTime = filters.selectedTime();

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
                                        isSportSelected(selectedSports, Sport.PADEL)))),
                new FilterGroupViewModel(
                        messageSource.getMessage("filter.time", null, locale),
                        List.of(
                                new FilterOptionViewModel(
                                        messageSource.getMessage("filter.anyTime", null, locale),
                                        buildUrl(query, filters.withTime("all"), 1, email, locale),
                                        null,
                                        "all".equalsIgnoreCase(selectedTime)),
                                new FilterOptionViewModel(
                                        messageSource.getMessage("filter.today", null, locale),
                                        buildUrl(
                                                query,
                                                filters.withTime(toggleTime(selectedTime, "today")),
                                                1,
                                                email,
                                                locale),
                                        null,
                                        "today".equalsIgnoreCase(selectedTime)),
                                new FilterOptionViewModel(
                                        messageSource.getMessage("filter.tomorrow", null, locale),
                                        buildUrl(
                                                query,
                                                filters.withTime(
                                                        toggleTime(selectedTime, "tomorrow")),
                                                1,
                                                email,
                                                locale),
                                        null,
                                        "tomorrow".equalsIgnoreCase(selectedTime)),
                                new FilterOptionViewModel(
                                        messageSource.getMessage("filter.thisWeek", null, locale),
                                        buildUrl(
                                                query,
                                                filters.withTime(toggleTime(selectedTime, "week")),
                                                1,
                                                email,
                                                locale),
                                        null,
                                        "week".equalsIgnoreCase(selectedTime)))));
    }

    private EventCardViewModel toCard(final Match match, final ZoneId zoneId, final Locale locale) {
        final Locale resolvedLocale = resolvedLocale(locale);
        final String schedule =
                scheduleFormatter(resolvedLocale).format(match.getStartsAt().atZone(zoneId));
        final String priceLabel = toPriceLabel(match.getPricePerPlayer(), locale);

        return new EventCardViewModel(
                String.valueOf(match.getId()),
                "/matches/" + match.getId(),
                toSportLabel(match.getSport(), resolvedLocale),
                match.getTitle(),
                match.getAddress(),
                schedule,
                priceLabel,
                messageSource.getMessage(
                        "event.spotsLeft", new Object[] {match.getAvailableSpots()}, locale),
                null,
                mediaClassFor(match.getSport()),
                bannerUrlFor(match));
    }

    private String toPriceLabel(final BigDecimal pricePerPlayer, final Locale locale) {
        if (pricePerPlayer == null) {
            return messageSource.getMessage("price.tbd", null, locale);
        }
        return pricePerPlayer.compareTo(BigDecimal.ZERO) == 0
                ? messageSource.getMessage("price.free", null, locale)
                : messageSource.getMessage("price.amount", new Object[] {pricePerPlayer}, locale);
    }

    private String toSportLabel(final Sport sport, final Locale locale) {
        return messageSource.getMessage(
                "sport." + sport.getDbValue(), null, sport.getDisplayName(), locale);
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

    private static DateTimeFormatter scheduleFormatter(final Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(resolvedLocale(locale));
    }

    private static Locale resolvedLocale(final Locale locale) {
        return locale == null ? Locale.ENGLISH : locale;
    }

    private static String buildUrl(
            final String query,
            final FeedFilters filters,
            final int page,
            final String email,
            final Locale locale) {
        final UriComponentsBuilder builder =
                UriComponentsBuilder.fromPath("/explore")
                        .queryParam("q", query == null ? "" : query)
                        .queryParam("time", filters.selectedTime())
                        .queryParam("sort", filters.selectedSort())
                        .queryParam("page", page);

        if (email != null && !email.isBlank()) {
            builder.queryParam("email", email);
        }
        if (locale != null && locale.getLanguage() != null && !locale.getLanguage().isBlank()) {
            builder.queryParam("lang", locale.getLanguage());
        }

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

    private static List<String> toggleSport(
            final List<String> selectedSports, final Sport sportToToggle) {
        return toggleValue(normalizeSports(selectedSports), sportToToggle.getDbValue());
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
    }
}
