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
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.EventCardViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FilterGroupViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FilterOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.MatchListControlsViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.PaginationItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.SelectOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class MatchDashboardController {

    private static final int PAGE_SIZE = 12;

    private static final List<String> HOST_ALL_STATUS_OPTIONS = List.of("draft", "open");
    private static final List<String> HOST_FINISHED_STATUS_OPTIONS =
            List.of("completed", "cancelled");
    private static final List<String> PLAYER_STATUS_OPTIONS =
            List.of("draft", "open", "completed", "cancelled");
    private static final List<String> HOST_VISIBILITY_OPTIONS =
            List.of("public", "private", "invite_only");

    private final MatchService matchService;
    private final MessageSource messageSource;

    @Autowired
    public MatchDashboardController(
            final MatchService matchService, final MessageSource messageSource) {
        this.matchService = matchService;
        this.messageSource = messageSource;
    }

    @GetMapping("/host/matches")
    public ModelAndView showHostedMatches(
            @RequestParam(value = "q", required = false) final String query,
            @RequestParam(value = "sort", required = false) final String sort,
            @RequestParam(value = "time", required = false) final String time,
            @RequestParam(value = "minPrice", required = false) final BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) final BigDecimal maxPrice,
            @RequestParam(value = "timezone", required = false) final String timezone,
            @RequestParam(value = "sport", required = false) final List<String> sports,
            @RequestParam(value = "visibility", required = false) final List<String> visibility,
            @RequestParam(value = "status", required = false) final List<String> statuses,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            final Locale locale) {
        final long userId = requireAuthenticatedUserId();
        final List<String> selectedSports = normalizeSports(sports);
        final List<String> selectedVisibility =
                normalizeValues(visibility, List.of(), HOST_VISIBILITY_OPTIONS);
        final List<String> selectedStatuses =
                normalizeValues(statuses, List.of(), HOST_ALL_STATUS_OPTIONS);
        final String selectedSort = normalizeSort(sort);
        final String selectedTime = normalizeTime(time);
        final String searchQuery = normalizeQuery(query);
        final String selectedTimezone = normalizeTimezone(timezone);

        final PaginatedResult<Match> result =
                matchService.findHostedMatches(
                        userId,
                        Boolean.TRUE,
                        searchQuery,
                        encodeCsv(selectedSports),
                        encodeCsv(selectedVisibility),
                        encodeCsv(selectedStatuses),
                        selectedTime,
                        minPrice,
                        maxPrice,
                        selectedSort,
                        selectedTimezone,
                        page,
                        PAGE_SIZE);

        return buildListPage(
                "matches/list",
                "/host/matches",
                "page.title.hostUpcomingMatches",
                locale,
                searchQuery,
                selectedSort,
                selectedTime,
                minPrice,
                maxPrice,
                selectedTimezone,
                selectedStatuses,
                selectedSports,
                selectedVisibility,
                result,
                messageSource.getMessage("host.dashboard.title", null, locale),
                messageSource.getMessage("host.dashboard.description", null, locale),
                messageSource.getMessage("host.dashboard.empty", null, locale),
                ShellViewModelFactory.hostShell(messageSource, locale, "/host/matches"));
    }

    @GetMapping("/host/matches/finished")
    public ModelAndView showFinishedHostedMatches(
            @RequestParam(value = "q", required = false) final String query,
            @RequestParam(value = "sort", required = false) final String sort,
            @RequestParam(value = "time", required = false) final String time,
            @RequestParam(value = "minPrice", required = false) final BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) final BigDecimal maxPrice,
            @RequestParam(value = "timezone", required = false) final String timezone,
            @RequestParam(value = "sport", required = false) final List<String> sports,
            @RequestParam(value = "visibility", required = false) final List<String> visibility,
            @RequestParam(value = "status", required = false) final List<String> statuses,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            final Locale locale) {
        final long userId = requireAuthenticatedUserId();
        final List<String> selectedSports = normalizeSports(sports);
        final List<String> selectedVisibility =
                normalizeValues(visibility, List.of(), HOST_VISIBILITY_OPTIONS);
        final List<String> selectedStatuses =
                normalizeValues(
                        statuses, HOST_FINISHED_STATUS_OPTIONS, HOST_FINISHED_STATUS_OPTIONS);
        final String selectedSort = normalizeSort(sort);
        final String selectedTime = normalizeTime(time);
        final String searchQuery = normalizeQuery(query);
        final String selectedTimezone = normalizeTimezone(timezone);

        final PaginatedResult<Match> result =
                matchService.findHostedMatches(
                        userId,
                        Boolean.FALSE,
                        searchQuery,
                        encodeCsv(selectedSports),
                        encodeCsv(selectedVisibility),
                        encodeCsv(selectedStatuses),
                        selectedTime,
                        minPrice,
                        maxPrice,
                        selectedSort,
                        selectedTimezone,
                        page,
                        PAGE_SIZE);

        return buildListPage(
                "matches/list",
                "/host/matches/finished",
                "page.title.hostFinishedMatches",
                locale,
                searchQuery,
                selectedSort,
                selectedTime,
                minPrice,
                maxPrice,
                selectedTimezone,
                selectedStatuses,
                selectedSports,
                selectedVisibility,
                result,
                messageSource.getMessage("host.finished.title", null, locale),
                messageSource.getMessage("host.finished.description", null, locale),
                messageSource.getMessage("host.finished.empty", null, locale),
                ShellViewModelFactory.hostShell(messageSource, locale, "/host/matches/finished"));
    }

    @GetMapping("/player/matches/past")
    public ModelAndView showPastJoinedMatches(
            @RequestParam(value = "q", required = false) final String query,
            @RequestParam(value = "sort", required = false) final String sort,
            @RequestParam(value = "time", required = false) final String time,
            @RequestParam(value = "minPrice", required = false) final BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) final BigDecimal maxPrice,
            @RequestParam(value = "timezone", required = false) final String timezone,
            @RequestParam(value = "sport", required = false) final List<String> sports,
            @RequestParam(value = "status", required = false) final List<String> statuses,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            final Locale locale) {
        final long userId = requireAuthenticatedUserId();
        final List<String> selectedSports = normalizeSports(sports);
        final List<String> selectedStatuses =
                normalizeValues(statuses, List.of(), PLAYER_STATUS_OPTIONS);
        final String selectedSort = normalizeSort(sort);
        final String selectedTime = normalizeTime(time);
        final String searchQuery = normalizeQuery(query);
        final String selectedTimezone = normalizeTimezone(timezone);

        final PaginatedResult<Match> result =
                matchService.findJoinedMatches(
                        userId,
                        Boolean.FALSE,
                        searchQuery,
                        encodeCsv(selectedSports),
                        null,
                        encodeCsv(selectedStatuses),
                        selectedTime,
                        minPrice,
                        maxPrice,
                        selectedSort,
                        selectedTimezone,
                        page,
                        PAGE_SIZE);

        return buildListPage(
                "matches/list",
                "/player/matches/past",
                "page.title.playerPastMatches",
                locale,
                searchQuery,
                selectedSort,
                selectedTime,
                minPrice,
                maxPrice,
                selectedTimezone,
                selectedStatuses,
                selectedSports,
                List.of(),
                result,
                messageSource.getMessage("player.past.title", null, locale),
                messageSource.getMessage("player.past.description", null, locale),
                messageSource.getMessage("player.past.empty", null, locale),
                ShellViewModelFactory.playerShell(messageSource, locale, "/player/matches/past"));
    }

    @GetMapping("/player/matches/upcoming")
    public ModelAndView showUpcomingJoinedMatches(
            @RequestParam(value = "q", required = false) final String query,
            @RequestParam(value = "sort", required = false) final String sort,
            @RequestParam(value = "time", required = false) final String time,
            @RequestParam(value = "minPrice", required = false) final BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) final BigDecimal maxPrice,
            @RequestParam(value = "timezone", required = false) final String timezone,
            @RequestParam(value = "sport", required = false) final List<String> sports,
            @RequestParam(value = "status", required = false) final List<String> statuses,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            final Locale locale) {
        final long userId = requireAuthenticatedUserId();
        final List<String> selectedSports = normalizeSports(sports);
        final List<String> selectedStatuses =
                normalizeValues(statuses, List.of(), PLAYER_STATUS_OPTIONS);
        final String selectedSort = normalizeSort(sort);
        final String selectedTime = normalizeTime(time);
        final String searchQuery = normalizeQuery(query);
        final String selectedTimezone = normalizeTimezone(timezone);

        final PaginatedResult<Match> result =
                matchService.findJoinedMatches(
                        userId,
                        Boolean.TRUE,
                        searchQuery,
                        encodeCsv(selectedSports),
                        null,
                        encodeCsv(selectedStatuses),
                        selectedTime,
                        minPrice,
                        maxPrice,
                        selectedSort,
                        selectedTimezone,
                        page,
                        PAGE_SIZE);

        return buildListPage(
                "matches/list",
                "/player/matches/upcoming",
                "page.title.playerUpcomingMatches",
                locale,
                searchQuery,
                selectedSort,
                selectedTime,
                minPrice,
                maxPrice,
                selectedTimezone,
                selectedStatuses,
                selectedSports,
                List.of(),
                result,
                messageSource.getMessage("player.upcoming.title", null, locale),
                messageSource.getMessage("player.upcoming.description", null, locale),
                messageSource.getMessage("player.upcoming.empty", null, locale),
                ShellViewModelFactory.playerShell(
                        messageSource, locale, "/player/matches/upcoming"));
    }

    private ModelAndView buildListPage(
            final String view,
            final String path,
            final String pageTitleCode,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String time,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final PaginatedResult<Match> result,
            final String title,
            final String description,
            final String emptyMessage,
            final Object shell) {
        final ModelAndView mav = new ModelAndView(view);
        final ZoneId zoneId = ZoneId.systemDefault();

        mav.addObject("shell", shell);
        mav.addObject("pageTitleCode", pageTitleCode);
        mav.addObject("listTitle", title);
        mav.addObject("listDescription", description);
        mav.addObject("emptyMessage", emptyMessage);
        mav.addObject("selectedSort", sort);
        mav.addObject("selectedTime", time);
        mav.addObject("selectedSports", selectedSports);
        mav.addObject("selectedStatuses", selectedStatuses);
        mav.addObject("selectedVisibility", selectedVisibility);
        mav.addObject("selectedTimezone", timezone);
        mav.addObject("selectedMinPriceValue", formatNullablePriceValue(minPrice));
        mav.addObject("selectedMaxPriceValue", formatNullablePriceValue(maxPrice));
        mav.addObject("listSearchForm", buildSearchForm(searchQuery));
        mav.addObject(
                "listControls",
                buildListControls(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility));
        mav.addObject(
                "events",
                result.getItems().stream().map(match -> toCard(match, zoneId, locale)).toList());
        mav.addObject(
                "paginationItems",
                buildPagination(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        result));
        mav.addObject(
                "previousPageHref",
                result.hasPrevious()
                        ? buildPageUrl(
                                path,
                                locale,
                                searchQuery,
                                sort,
                                time,
                                minPrice,
                                maxPrice,
                                timezone,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                result.getPage() - 1)
                        : null);
        mav.addObject(
                "nextPageHref",
                result.hasNext()
                        ? buildPageUrl(
                                path,
                                locale,
                                searchQuery,
                                sort,
                                time,
                                minPrice,
                                maxPrice,
                                timezone,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                result.getPage() + 1)
                        : null);
        return mav;
    }

    private static long requireAuthenticatedUserId() {
        return CurrentAuthenticatedUser.get()
                .map(principal -> principal.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private EventCardViewModel toCard(final Match match, final ZoneId zoneId, final Locale locale) {
        final Locale resolvedLocale = locale == null ? Locale.ENGLISH : locale;
        final String schedule =
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                        .withLocale(resolvedLocale)
                        .format(match.getStartsAt().atZone(zoneId));

        final String priceLabel = toPriceLabel(match.getPricePerPlayer(), locale);
        final String badge =
                messageSource.getMessage(
                        "match.status." + match.getStatus(), null, match.getStatus(), locale);

        return new EventCardViewModel(
                String.valueOf(match.getId()),
                "/matches/" + match.getId(),
                messageSource.getMessage(
                        "sport." + match.getSport().getDbValue(),
                        null,
                        match.getSport().getDisplayName(),
                        locale),
                match.getTitle(),
                match.getAddress(),
                schedule,
                priceLabel,
                badge,
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

    private MatchListControlsViewModel buildListControls(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String selectedTime,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility) {
        final boolean hostView = path.startsWith("/host/");

        final List<FilterGroupViewModel> filterGroups = new ArrayList<>();
        filterGroups.add(
                new FilterGroupViewModel(
                        messageSource.getMessage("filter.categories", null, locale),
                        buildSportFilterOptions(
                                path,
                                locale,
                                searchQuery,
                                sort,
                                selectedTime,
                                minPrice,
                                maxPrice,
                                timezone,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility)));

        if (hostView) {
            filterGroups.add(
                    new FilterGroupViewModel(
                            messageSource.getMessage("host.filters.status", null, locale),
                            buildStatusFilterOptions(
                                    path,
                                    locale,
                                    searchQuery,
                                    sort,
                                    selectedTime,
                                    minPrice,
                                    maxPrice,
                                    timezone,
                                    selectedStatuses,
                                    selectedSports,
                                    selectedVisibility,
                                    path.endsWith("/finished")
                                            ? HOST_FINISHED_STATUS_OPTIONS
                                            : HOST_ALL_STATUS_OPTIONS)));
            filterGroups.add(
                    new FilterGroupViewModel(
                            messageSource.getMessage("host.filters.visibility", null, locale),
                            buildVisibilityFilterOptions(
                                    path,
                                    locale,
                                    searchQuery,
                                    sort,
                                    selectedTime,
                                    minPrice,
                                    maxPrice,
                                    timezone,
                                    selectedStatuses,
                                    selectedSports,
                                    selectedVisibility)));
        } else {
            filterGroups.add(
                    new FilterGroupViewModel(
                            messageSource.getMessage("host.filters.status", null, locale),
                            buildStatusFilterOptions(
                                    path,
                                    locale,
                                    searchQuery,
                                    sort,
                                    selectedTime,
                                    minPrice,
                                    maxPrice,
                                    timezone,
                                    selectedStatuses,
                                    selectedSports,
                                    selectedVisibility,
                                    PLAYER_STATUS_OPTIONS)));
        }

        filterGroups.add(
                new FilterGroupViewModel(
                        messageSource.getMessage("filter.time", null, locale),
                        buildTimeFilterOptions(
                                path,
                                locale,
                                searchQuery,
                                sort,
                                selectedTime,
                                minPrice,
                                maxPrice,
                                timezone,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility)));
        filterGroups.add(
                new FilterGroupViewModel(
                        messageSource.getMessage("filter.price", null, locale),
                        buildPriceFilterOptions(
                                path,
                                locale,
                                searchQuery,
                                sort,
                                selectedTime,
                                timezone,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                minPrice,
                                maxPrice)));

        final List<SelectOptionViewModel> sortOptions =
                List.of(
                        sortOption(
                                path,
                                locale,
                                searchQuery,
                                selectedTime,
                                minPrice,
                                maxPrice,
                                timezone,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                "soonest",
                                sort,
                                messageSource.getMessage("feed.sort.soonest", null, locale)),
                        sortOption(
                                path,
                                locale,
                                searchQuery,
                                selectedTime,
                                minPrice,
                                maxPrice,
                                timezone,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                "price",
                                sort,
                                messageSource.getMessage("feed.sort.price", null, locale)),
                        sortOption(
                                path,
                                locale,
                                searchQuery,
                                selectedTime,
                                minPrice,
                                maxPrice,
                                timezone,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                "spots",
                                sort,
                                messageSource.getMessage("feed.sort.spots", null, locale)));

        return new MatchListControlsViewModel(
                buildSearchAction(
                        path,
                        locale,
                        sort,
                        selectedTime,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility),
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
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String time,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final PaginatedResult<Match> result) {
        if (result.getTotalPages() <= 1) {
            return List.of();
        }

        final List<PaginationItemViewModel> items = new ArrayList<>();
        final int startPage =
                Math.max(2, Math.min(result.getPage() - 1, result.getTotalPages() - 3));
        final int endPage = Math.min(result.getTotalPages() - 1, Math.max(result.getPage() + 1, 4));

        items.add(
                pageItem(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        1,
                        result.getPage()));

        if (startPage > 2) {
            items.add(new PaginationItemViewModel("...", null, false, true));
        }

        for (int page = startPage; page <= endPage; page++) {
            items.add(
                    pageItem(
                            path,
                            locale,
                            searchQuery,
                            sort,
                            time,
                            minPrice,
                            maxPrice,
                            timezone,
                            selectedStatuses,
                            selectedSports,
                            selectedVisibility,
                            page,
                            result.getPage()));
        }

        if (endPage < result.getTotalPages() - 1) {
            items.add(new PaginationItemViewModel("...", null, false, true));
        }

        items.add(
                pageItem(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        result.getTotalPages(),
                        result.getPage()));

        return items;
    }

    private static PaginationItemViewModel pageItem(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String time,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final int page,
            final int currentPage) {
        return new PaginationItemViewModel(
                Integer.toString(page),
                buildPageUrl(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        page),
                page == currentPage,
                false);
    }

    private static String buildPageUrl(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String time,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final int page) {
        final UriComponentsBuilder builder =
                UriComponentsBuilder.fromPath(path).queryParam("page", page);
        if (searchQuery != null && !searchQuery.isBlank()) {
            builder.queryParam("q", searchQuery);
        }
        if (sort != null && !sort.isBlank()) {
            builder.queryParam("sort", sort);
        }
        if (time != null && !time.isBlank()) {
            builder.queryParam("time", time);
        }
        if (minPrice != null) {
            builder.queryParam("minPrice", minPrice);
        }
        if (maxPrice != null) {
            builder.queryParam("maxPrice", maxPrice);
        }
        if (timezone != null && !timezone.isBlank()) {
            builder.queryParam("timezone", timezone);
        }

        final String encodedStatuses = encodeCsv(selectedStatuses);
        final String encodedSports = encodeCsv(selectedSports);
        final String encodedVisibility = encodeCsv(selectedVisibility);

        if (encodedStatuses != null) {
            builder.queryParam("status", encodedStatuses);
        }
        if (encodedSports != null) {
            builder.queryParam("sport", encodedSports);
        }
        if (encodedVisibility != null) {
            builder.queryParam("visibility", encodedVisibility);
        }

        if (locale != null && locale.getLanguage() != null && !locale.getLanguage().isBlank()) {
            builder.queryParam("lang", locale.getLanguage());
        }

        return builder.build().encode().toUriString();
    }

    private String buildSearchAction(
            final String path,
            final Locale locale,
            final String sort,
            final String time,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility) {
        return buildPageUrl(
                path,
                locale,
                null,
                sort,
                time,
                minPrice,
                maxPrice,
                timezone,
                selectedStatuses,
                selectedSports,
                selectedVisibility,
                1);
    }

    private SelectOptionViewModel sortOption(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String time,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final String value,
            final String currentSort,
            final String label) {
        return new SelectOptionViewModel(
                label,
                buildPageUrl(
                        path,
                        locale,
                        searchQuery,
                        value,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        1),
                value.equalsIgnoreCase(currentSort));
    }

    private FilterOptionViewModel filterOption(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String time,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> statuses,
            final List<String> sports,
            final List<String> visibility,
            final boolean active,
            final String label) {
        return new FilterOptionViewModel(
                label,
                buildPageUrl(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        statuses,
                        sports,
                        visibility,
                        1),
                null,
                active);
    }

    private List<FilterOptionViewModel> buildSportFilterOptions(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String time,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility) {
        return List.of(
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        List.of(),
                        selectedVisibility,
                        selectedSports.isEmpty(),
                        messageSource.getMessage("filter.anySport", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        toggleValue(selectedSports, "football"),
                        selectedVisibility,
                        selectedSports.contains("football"),
                        messageSource.getMessage("sport.football", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        toggleValue(selectedSports, "tennis"),
                        selectedVisibility,
                        selectedSports.contains("tennis"),
                        messageSource.getMessage("sport.tennis", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        toggleValue(selectedSports, "basketball"),
                        selectedVisibility,
                        selectedSports.contains("basketball"),
                        messageSource.getMessage("sport.basketball", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        toggleValue(selectedSports, "padel"),
                        selectedVisibility,
                        selectedSports.contains("padel"),
                        messageSource.getMessage("sport.padel", null, locale)));
    }

    private List<FilterOptionViewModel> buildStatusFilterOptions(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String time,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> allowedStatuses) {
        final List<FilterOptionViewModel> options = new ArrayList<>();
        options.add(
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        List.of(),
                        selectedSports,
                        selectedVisibility,
                        selectedStatuses.isEmpty(),
                        messageSource.getMessage("filter.anyStatus", null, locale)));

        for (final String status : allowedStatuses) {
            options.add(
                    filterOption(
                            path,
                            locale,
                            searchQuery,
                            sort,
                            time,
                            minPrice,
                            maxPrice,
                            timezone,
                            toggleValue(selectedStatuses, status),
                            selectedSports,
                            selectedVisibility,
                            selectedStatuses.contains(status),
                            messageSource.getMessage("match.status." + status, null, locale)));
        }

        return List.copyOf(options);
    }

    private List<FilterOptionViewModel> buildVisibilityFilterOptions(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String time,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility) {
        return List.of(
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        List.of(),
                        selectedVisibility.isEmpty(),
                        messageSource.getMessage("filter.anyVisibility", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        toggleValue(selectedVisibility, "public"),
                        selectedVisibility.contains("public"),
                        messageSource.getMessage("visibility.public", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        toggleValue(selectedVisibility, "private"),
                        selectedVisibility.contains("private"),
                        messageSource.getMessage("visibility.private", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        time,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        toggleValue(selectedVisibility, "invite_only"),
                        selectedVisibility.contains("invite_only"),
                        messageSource.getMessage("visibility.inviteOnly", null, locale)));
    }

    private List<FilterOptionViewModel> buildTimeFilterOptions(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String selectedTime,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility) {
        return List.of(
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        "all",
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        "all".equals(selectedTime),
                        messageSource.getMessage("filter.anyTime", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        "today",
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        "today".equals(selectedTime),
                        messageSource.getMessage("filter.today", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        "tomorrow",
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        "tomorrow".equals(selectedTime),
                        messageSource.getMessage("filter.tomorrow", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        "week",
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        "week".equals(selectedTime),
                        messageSource.getMessage("filter.thisWeek", null, locale)));
    }

    private List<FilterOptionViewModel> buildPriceFilterOptions(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String selectedTime,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final BigDecimal selectedMinPrice,
            final BigDecimal selectedMaxPrice) {
        return List.of(
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        selectedTime,
                        null,
                        null,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        selectedMinPrice == null && selectedMaxPrice == null,
                        messageSource.getMessage("filter.anyPrice", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        selectedTime,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        BigDecimal.ZERO.equals(selectedMinPrice)
                                && BigDecimal.ZERO.equals(selectedMaxPrice),
                        messageSource.getMessage("price.free", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        selectedTime,
                        new BigDecimal("0.01"),
                        null,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        selectedMinPrice != null
                                && selectedMinPrice.compareTo(BigDecimal.ZERO) > 0
                                && selectedMaxPrice == null,
                        messageSource.getMessage("filter.paid", null, locale)));
    }

    private static String normalizeQuery(final String query) {
        return query == null ? "" : query.trim();
    }

    private static String normalizeTimezone(final String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.systemDefault().getId();
        }

        try {
            return ZoneId.of(timezone).getId();
        } catch (final Exception ignored) {
            return ZoneId.systemDefault().getId();
        }
    }

    private static List<String> normalizeSports(final List<String> rawSports) {
        final List<String> normalized = normalizeCsvValues(rawSports);
        final List<String> sports = new ArrayList<>();
        for (final String sport : normalized) {
            Sport.fromDbValue(sport).map(Sport::getDbValue).ifPresent(sports::add);
        }
        return List.copyOf(sports);
    }

    private static List<String> normalizeValues(
            final List<String> values,
            final List<String> defaultValues,
            final List<String> allowedValues) {
        final List<String> normalized = normalizeCsvValues(values);
        if (normalized.isEmpty()) {
            return List.copyOf(defaultValues);
        }

        final LinkedHashSet<String> filtered = new LinkedHashSet<>();
        for (final String value : normalized) {
            if (allowedValues.contains(value)) {
                filtered.add(value);
            }
        }

        return List.copyOf(filtered);
    }

    private static FeedSearchForm buildSearchForm(final String searchQuery) {
        final FeedSearchForm form = new FeedSearchForm();
        form.setQ(searchQuery == null ? "" : searchQuery);
        return form;
    }

    private static String formatNullablePriceValue(final BigDecimal price) {
        return price == null ? "" : price.stripTrailingZeros().toPlainString();
    }
}
