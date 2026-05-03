package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.bannerUrlFor;
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
import ar.edu.itba.paw.webapp.form.FeedSearchForm;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.EventCardViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.EventRelationshipBadgeViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FilterGroupViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FilterOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.MatchListControlsViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.PaginationItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.SelectOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class MatchDashboardController {

    private static final int PAGE_SIZE = 12;

    private static final List<String> PLAYER_STATUS_OPTIONS =
            List.of("open", "completed", "cancelled");

    private final MatchService matchService;
    private final MatchParticipationService matchParticipationService;
    private final MatchReservationService matchReservationService;
    private final MessageSource messageSource;

    @Autowired
    public MatchDashboardController(
            final MatchService matchService,
            final MatchParticipationService matchParticipationService,
            final MatchReservationService matchReservationService,
            final MessageSource messageSource) {
        this.matchService = matchService;
        this.matchParticipationService = matchParticipationService;
        this.matchReservationService = matchReservationService;
        this.messageSource = messageSource;
    }

    @GetMapping("/events")
    public ModelAndView showEventsPage(
            @RequestParam(value = "q", required = false) final String query,
            @RequestParam(value = "sort", required = false) final String sort,
            @RequestParam(value = "startDate", required = false) final String startDate,
            @RequestParam(value = "endDate", required = false) final String endDate,
            @RequestParam(value = "minPrice", required = false) final BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) final BigDecimal maxPrice,
            @RequestParam(value = "tz", required = false) final String timezone,
            @RequestParam(value = "sport", required = false) final List<String> sports,
            @RequestParam(value = "status", required = false) final List<String> statuses,
            @RequestParam(value = "category", required = false) final List<String> categories,
            @RequestParam(value = "filter", defaultValue = "upcoming") final String filter,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            final Locale locale) {
        final long userId = requireAuthenticatedUserId();
        final List<String> selectedSports = normalizeSports(sports);
        final List<String> selectedStatuses =
                normalizeValues(statuses, List.of(), PLAYER_STATUS_OPTIONS);
        final List<String> selectedCategories =
                normalizeValues(
                        categories, List.of(), List.of("joined", "invited", "pending", "hosted"));
        final String selectedSort = normalizeSort(sort);
        final String searchQuery = normalizeQuery(query);
        final String selectedTimezone = normalizeTimezone(timezone);

        final DateRangeContext context =
                "past".equalsIgnoreCase(filter) ? DateRangeContext.PAST : DateRangeContext.UPCOMING;
        final DateRange selectedDateRange =
                normalizeDateRange(startDate, endDate, context, ZoneId.of(selectedTimezone));

        final PaginatedResult<Match> result =
                getAllUserEventsForFilter(
                        userId,
                        context,
                        searchQuery,
                        encodeCsv(selectedSports),
                        encodeCsv(selectedStatuses),
                        selectedDateRange.startDate(),
                        encodeCsv(selectedCategories),
                        selectedDateRange.endDate(),
                        minPrice,
                        maxPrice,
                        selectedSort,
                        selectedTimezone,
                        page,
                        PAGE_SIZE);

        return buildListPage(
                "events/list",
                "/events",
                "page.title.events",
                locale,
                searchQuery,
                selectedSort,
                selectedDateRange.startDate(),
                selectedDateRange.endDate(),
                minPrice,
                maxPrice,
                selectedTimezone,
                selectedStatuses,
                selectedSports,
                List.of(),
                selectedCategories,
                result,
                messageSource.getMessage("events.title", null, locale),
                messageSource.getMessage("events.description", null, locale),
                messageSource.getMessage("events.empty", null, locale),
                ShellViewModelFactory.playerShell(messageSource, locale, "/events"));
    }

    private ModelAndView buildListPage(
            final String view,
            final String path,
            final String pageTitleCode,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories,
            final PaginatedResult<Match> result,
            final String title,
            final String description,
            final String emptyMessage,
            final Object shell) {
        final ModelAndView mav = new ModelAndView(view);
        final ZoneId zoneId = ZoneId.of(timezone);
        final DateRangeBounds dateBounds = dateRangeBounds(path, ZoneId.of(timezone));
        final Long currentUserId =
                CurrentAuthenticatedUser.get().map(user -> user.getUserId()).orElse(null);

        mav.addObject("shell", shell);
        mav.addObject("pageTitleCode", pageTitleCode);
        mav.addObject("listTitle", title);
        mav.addObject("listDescription", description);
        mav.addObject("emptyMessage", emptyMessage);
        mav.addObject("selectedSort", sort);
        mav.addObject("selectedStartDateValue", startDate);
        mav.addObject("selectedEndDateValue", endDate);
        mav.addObject("selectedDateMinValue", dateBounds.minDate());
        mav.addObject("selectedDateMaxValue", dateBounds.maxDate());
        mav.addObject("selectedSports", selectedSports);
        mav.addObject("selectedStatuses", selectedStatuses);
        mav.addObject("selectedVisibility", selectedVisibility);
        mav.addObject("selectedCategories", selectedCategories);
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
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        selectedCategories));
        mav.addObject(
                "events",
                result.getItems().stream()
                        .map(
                                match ->
                                        toCard(
                                                match,
                                                zoneId,
                                                locale,
                                                path.startsWith("/host/"),
                                                currentUserId))
                        .toList());
        mav.addObject(
                "paginationItems",
                buildPagination(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        selectedCategories,
                        result));
        mav.addObject(
                "previousPageHref",
                result.hasPrevious()
                        ? buildPageUrl(
                                path,
                                locale,
                                searchQuery,
                                sort,
                                startDate,
                                endDate,
                                minPrice,
                                maxPrice,
                                timezone,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                selectedCategories,
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
                                startDate,
                                endDate,
                                minPrice,
                                maxPrice,
                                timezone,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                selectedCategories,
                                result.getPage() + 1)
                        : null);
        return mav;
    }

    private static long requireAuthenticatedUserId() {
        return CurrentAuthenticatedUser.get()
                .map(principal -> principal.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private PaginatedResult<Match> getAllUserEventsForFilter(
            final long userId,
            final DateRangeContext context,
            final String searchQuery,
            final String sports,
            final String statuses,
            final String startDate,
            final String categories,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String sort,
            final String timezone,
            final int requestedPage,
            final int pageSize) {
        final Boolean isUpcoming = context == DateRangeContext.UPCOMING;
        final LocalDate today = LocalDate.now(ZoneId.of(timezone));
        final List<String> categoryList =
                normalizeCsvValues(categories == null ? List.of() : List.of(categories));
        final boolean allowJoined = categoryList.isEmpty() || categoryList.contains("joined");
        final boolean allowInvited = categoryList.isEmpty() || categoryList.contains("invited");
        final boolean allowPending = categoryList.contains("pending");
        final boolean allowHosted = categoryList.isEmpty() || categoryList.contains("hosted");

        // Fetch pending and invited matches (small, non-paginated lists).
        // Pending matches are split by date so stale pending requests show in past.
        final List<Match> pendingMatches =
                !allowPending
                        ? List.of()
                        : matchParticipationService.findPendingRequestMatches(userId).stream()
                                .filter(match -> belongsToContext(match, context, today, timezone))
                                .filter(
                                        match ->
                                                matchesFilters(
                                                        match,
                                                        sports,
                                                        statuses,
                                                        minPrice,
                                                        maxPrice,
                                                        searchQuery))
                                .toList();
        final List<Match> invitedMatches =
                !allowInvited
                        ? List.of()
                        : matchParticipationService.findInvitedMatches(userId).stream()
                                .filter(match -> belongsToContext(match, context, today, timezone))
                                .filter(
                                        match ->
                                                matchesFilters(
                                                        match,
                                                        sports,
                                                        statuses,
                                                        minPrice,
                                                        maxPrice,
                                                        searchQuery))
                                .toList();

        // Fetch joined and hosted matches with pagination
        // We fetch with a larger page size to ensure we get reasonable results even
        // when combined
        final int fetchPageSize =
                pageSize * 3; // Fetch 3x the page size to have more data to work with
        final PaginatedResult<Match> joinedMatches =
                !allowJoined
                        ? new PaginatedResult<>(List.of(), 0, requestedPage, fetchPageSize)
                        : matchService.findJoinedMatches(
                                userId,
                                isUpcoming,
                                searchQuery,
                                sports,
                                null,
                                statuses,
                                startDate,
                                endDate,
                                minPrice,
                                maxPrice,
                                sort,
                                timezone,
                                requestedPage,
                                fetchPageSize);

        final PaginatedResult<Match> hostedMatches =
                !allowHosted
                        ? new PaginatedResult<>(List.of(), 0, requestedPage, fetchPageSize)
                        : matchService.findHostedMatches(
                                userId,
                                isUpcoming,
                                searchQuery,
                                sports,
                                null,
                                statuses,
                                startDate,
                                endDate,
                                minPrice,
                                maxPrice,
                                sort,
                                timezone,
                                requestedPage,
                                fetchPageSize);

        // Combine all results and deduplicate by match ID
        final LinkedHashSet<Long> seenIds = new LinkedHashSet<>();
        final List<Match> combined = new ArrayList<>();

        // Add pending matches
        for (final Match m : pendingMatches) {
            if (seenIds.add(m.getId())) {
                combined.add(m);
            }
        }

        // Add invited matches
        for (final Match m : invitedMatches) {
            if (seenIds.add(m.getId())) {
                combined.add(m);
            }
        }

        // Add joined matches
        for (final Match m : joinedMatches.getItems()) {
            if (seenIds.add(m.getId())) {
                combined.add(m);
            }
        }

        // Add hosted matches
        for (final Match m : hostedMatches.getItems()) {
            if (seenIds.add(m.getId())) {
                combined.add(m);
            }
        }

        sortCombinedMatches(combined, sort, context);

        // Re-paginate the combined results
        final int totalCount = combined.size();
        final int startIndex = (requestedPage - 1) * pageSize;
        final int endIndex = Math.min(startIndex + pageSize, combined.size());

        final List<Match> paginatedItems;
        if (startIndex >= combined.size()) {
            paginatedItems = List.of();
        } else {
            paginatedItems = combined.subList(startIndex, endIndex);
        }

        return new PaginatedResult<>(paginatedItems, totalCount, requestedPage, pageSize);
    }

    private static void sortCombinedMatches(
            final List<Match> matches, final String sort, final DateRangeContext context) {
        final Comparator<Match> chronological =
                Comparator.comparing(Match::getStartsAt).thenComparing(Match::getId);
        final Comparator<Match> contextualChronological =
                context == DateRangeContext.PAST ? chronological.reversed() : chronological;

        final String normalizedSort = normalizeSort(sort);
        final Comparator<Match> comparator;
        switch (normalizedSort) {
            case "price":
                comparator =
                        Comparator.comparing(
                                        (Match match) ->
                                                match.getPricePerPlayer() == null
                                                        ? BigDecimal.ZERO
                                                        : match.getPricePerPlayer())
                                .thenComparing(contextualChronological);
                break;
            case "spots":
                comparator =
                        Comparator.comparingInt(Match::getAvailableSpots)
                                .reversed()
                                .thenComparing(contextualChronological);
                break;
            case "soonest":
            default:
                comparator = contextualChronological;
                break;
        }
        matches.sort(comparator);
    }

    private static boolean belongsToContext(
            final Match match,
            final DateRangeContext context,
            final LocalDate today,
            final String timezone) {
        final LocalDate matchDate = match.getStartsAt().atZone(ZoneId.of(timezone)).toLocalDate();
        if (context == DateRangeContext.PAST) {
            return !matchDate.isAfter(today);
        }
        return !matchDate.isBefore(today);
    }

    private static boolean matchesFilters(
            final Match match,
            final String sports,
            final String statuses,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String searchQuery) {
        // Filter by sport
        if (sports != null && !sports.isBlank()) {
            final List<String> sportList = normalizeCsvValues(List.of(sports));
            if (!sportList.isEmpty() && !sportList.contains(match.getSport().getDbValue())) {
                return false;
            }
        }

        // Filter by status
        if (statuses != null && !statuses.isBlank()) {
            final List<String> statusList = normalizeCsvValues(List.of(statuses));
            if (!statusList.isEmpty() && !statusList.contains(match.getStatus())) {
                return false;
            }
        }

        // Filter by price range
        if (match.getPricePerPlayer() != null) {
            if (minPrice != null && match.getPricePerPlayer().compareTo(minPrice) < 0) {
                return false;
            }
            if (maxPrice != null && match.getPricePerPlayer().compareTo(maxPrice) > 0) {
                return false;
            }
        }

        // Filter by search query (title and host name)
        if (searchQuery != null && !searchQuery.isBlank()) {
            final String lowerQuery = searchQuery.trim().toLowerCase(Locale.ROOT);
            final boolean titleMatches =
                    match.getTitle().toLowerCase(Locale.ROOT).contains(lowerQuery);
            if (!titleMatches) {
                return false;
            }
        }

        return true;
    }

    private static List<String> normalizeCsvValues(final List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return List.of();
        }

        final LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (final String rawValue : rawValues) {
            if (rawValue == null || rawValue.isBlank()) {
                continue;
            }

            for (final String part : rawValue.split(",")) {
                if (part == null || part.isBlank()) {
                    continue;
                }
                normalized.add(part.trim().toLowerCase(Locale.ROOT));
            }
        }

        return List.copyOf(normalized);
    }

    private EventCardViewModel toCard(
            final Match match,
            final ZoneId zoneId,
            final Locale locale,
            final boolean hostDashboardView,
            final Long currentUserId) {
        final Locale resolvedLocale = locale == null ? Locale.ENGLISH : locale;
        final ZonedDateTime startsAt = match.getStartsAt().atZone(zoneId);
        final String schedule =
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                        .withLocale(resolvedLocale)
                        .format(startsAt);
        final String dateLabel =
                DateTimeFormatter.ofPattern("EEE, MMM d", resolvedLocale).format(startsAt);
        final String timeLabel =
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                        .withLocale(resolvedLocale)
                        .format(startsAt);

        final String priceLabel = toPriceLabel(match.getPricePerPlayer(), locale);
        final String badge =
                messageSource.getMessage(
                        "match.status." + match.getStatus(), null, match.getStatus(), locale);
        final List<EventRelationshipBadgeViewModel> relationshipBadges =
                relationshipBadgesFor(match, currentUserId, locale);

        final String cardHref = "/matches/" + match.getId();

        return new EventCardViewModel(
                String.valueOf(match.getId()),
                cardHref,
                messageSource.getMessage(
                        "sport." + match.getSport().getDbValue(),
                        null,
                        match.getSport().getDisplayName(),
                        locale),
                match.getTitle(),
                match.getAddress(),
                schedule,
                dateLabel,
                timeLabel,
                priceLabel,
                badge,
                relationshipBadges,
                recurringLabelFor(match, locale),
                null,
                mediaClassFor(match.getSport()),
                bannerUrlFor(match));
    }

    private List<EventRelationshipBadgeViewModel> relationshipBadgesFor(
            final Match match, final Long currentUserId, final Locale locale) {
        if (currentUserId == null) {
            return List.of();
        }
        final List<EventRelationshipBadgeViewModel> badges = new ArrayList<>();
        if (currentUserId.equals(match.getHostUserId())) {
            badges.add(relationshipBadge("my_event", locale));
        }
        if (matchParticipationService.hasPendingRequest(match.getId(), currentUserId)) {
            badges.add(relationshipBadge("pending", locale));
        } else if (matchParticipationService.hasInvitation(match.getId(), currentUserId)) {
            badges.add(relationshipBadge("invited", locale));
        } else if (matchReservationService.hasActiveReservation(match.getId(), currentUserId)) {
            badges.add(relationshipBadge("going", locale));
        }
        return List.copyOf(badges);
    }

    private EventRelationshipBadgeViewModel relationshipBadge(
            final String type, final Locale locale) {
        return new EventRelationshipBadgeViewModel(
                type, messageSource.getMessage("event.relationship." + type, null, locale));
    }

    private String recurringLabelFor(final Match match, final Locale locale) {
        return match.isRecurringOccurrence()
                ? messageSource.getMessage("event.recurringBadge", null, locale)
                : null;
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
                return "media-tile--padel";
            case OTHER:
            default:
                return "media-tile--other";
        }
    }

    private MatchListControlsViewModel buildListControls(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String selectedStartDate,
            final String selectedEndDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories) {
        final List<FilterGroupViewModel> filterGroups = new ArrayList<>();
        filterGroups.add(
                new FilterGroupViewModel(
                        messageSource.getMessage("filter.categories", null, locale),
                        buildSportFilterOptions(
                                path,
                                locale,
                                searchQuery,
                                sort,
                                selectedStartDate,
                                selectedEndDate,
                                minPrice,
                                maxPrice,
                                timezone,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                selectedCategories)));

        if ("/events".equals(path)) {
            filterGroups.add(
                    new FilterGroupViewModel(
                            messageSource.getMessage("filter.category", null, locale),
                            buildCategoryFilterOptions(
                                    path,
                                    locale,
                                    searchQuery,
                                    sort,
                                    selectedStartDate,
                                    selectedEndDate,
                                    minPrice,
                                    maxPrice,
                                    timezone,
                                    selectedStatuses,
                                    selectedSports,
                                    selectedVisibility,
                                    selectedCategories)));
        }

        filterGroups.add(
                new FilterGroupViewModel(
                        messageSource.getMessage("host.filters.status", null, locale),
                        buildStatusFilterOptions(
                                path,
                                locale,
                                searchQuery,
                                sort,
                                selectedStartDate,
                                selectedEndDate,
                                minPrice,
                                maxPrice,
                                timezone,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                selectedCategories,
                                PLAYER_STATUS_OPTIONS)));
        final List<SelectOptionViewModel> sortOptions =
                List.of(
                        sortOption(
                                path,
                                locale,
                                searchQuery,
                                selectedStartDate,
                                selectedEndDate,
                                minPrice,
                                maxPrice,
                                timezone,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                selectedCategories,
                                "soonest",
                                sort,
                                messageSource.getMessage("feed.sort.soonest", null, locale)),
                        sortOption(
                                path,
                                locale,
                                searchQuery,
                                selectedStartDate,
                                selectedEndDate,
                                minPrice,
                                maxPrice,
                                timezone,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                selectedCategories,
                                "price",
                                sort,
                                messageSource.getMessage("feed.sort.price", null, locale)),
                        sortOption(
                                path,
                                locale,
                                searchQuery,
                                selectedStartDate,
                                selectedEndDate,
                                minPrice,
                                maxPrice,
                                timezone,
                                selectedStatuses,
                                selectedSports,
                                selectedVisibility,
                                selectedCategories,
                                "spots",
                                sort,
                                messageSource.getMessage("feed.sort.spots", null, locale)));

        return new MatchListControlsViewModel(
                buildSearchAction(
                        path, locale, sort, null, null, null, null, timezone, List.of(), List.of(),
                        List.of(), List.of()),
                buildSearchAction(
                        path,
                        locale,
                        sort,
                        selectedStartDate,
                        selectedEndDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        selectedCategories),
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
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories,
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
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        selectedCategories,
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
                            startDate,
                            endDate,
                            minPrice,
                            maxPrice,
                            timezone,
                            selectedStatuses,
                            selectedSports,
                            selectedVisibility,
                            selectedCategories,
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
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        selectedCategories,
                        result.getTotalPages(),
                        result.getPage()));

        return items;
    }

    private static PaginationItemViewModel pageItem(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories,
            final int page,
            final int currentPage) {
        return new PaginationItemViewModel(
                Integer.toString(page),
                buildPageUrl(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        selectedCategories,
                        page),
                page == currentPage,
                false);
    }

    private static String buildPageUrl(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories,
            final int page) {
        final UriComponentsBuilder builder =
                UriComponentsBuilder.fromPath(path).queryParam("page", page);
        if ("/events".equals(path)) {
            final ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                final String filter = attrs.getRequest().getParameter("filter");
                if ("past".equalsIgnoreCase(filter)) {
                    builder.queryParam("filter", "past");
                }
            }
        }
        if (searchQuery != null && !searchQuery.isBlank()) {
            builder.queryParam("q", searchQuery);
        }
        if (sort != null && !sort.isBlank()) {
            builder.queryParam("sort", sort);
        }
        if (startDate != null && !startDate.isBlank()) {
            builder.queryParam("startDate", startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            builder.queryParam("endDate", endDate);
        }
        if (minPrice != null) {
            builder.queryParam("minPrice", minPrice);
        }
        if (maxPrice != null) {
            builder.queryParam("maxPrice", maxPrice);
        }
        if (timezone != null && !timezone.isBlank()) {
            builder.queryParam("tz", timezone);
        }

        final String encodedStatuses = encodeCsv(selectedStatuses);
        final String encodedSports = encodeCsv(selectedSports);
        final String encodedVisibility = encodeCsv(selectedVisibility);
        final String encodedCategories = encodeCsv(selectedCategories);

        if (encodedStatuses != null) {
            builder.queryParam("status", encodedStatuses);
        }
        if (encodedSports != null) {
            builder.queryParam("sport", encodedSports);
        }
        if (encodedVisibility != null) {
            builder.queryParam("visibility", encodedVisibility);
        }
        if (encodedCategories != null) {
            builder.queryParam("category", encodedCategories);
        }

        return builder.build().encode().toUriString();
    }

    private String buildSearchAction(
            final String path,
            final Locale locale,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories) {
        return buildPageUrl(
                path,
                locale,
                null,
                sort,
                startDate,
                endDate,
                minPrice,
                maxPrice,
                timezone,
                selectedStatuses,
                selectedSports,
                selectedVisibility,
                selectedCategories,
                1);
    }

    private SelectOptionViewModel sortOption(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories,
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
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        selectedCategories,
                        1),
                value.equalsIgnoreCase(currentSort));
    }

    private FilterOptionViewModel filterOption(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> statuses,
            final List<String> sports,
            final List<String> visibility,
            final List<String> categories,
            final boolean active,
            final String label) {
        return new FilterOptionViewModel(
                label,
                buildPageUrl(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        statuses,
                        sports,
                        visibility,
                        categories,
                        1),
                null,
                active);
    }

    private List<FilterOptionViewModel> buildSportFilterOptions(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories) {
        return List.of(
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        List.of(),
                        selectedVisibility,
                        selectedCategories,
                        selectedSports.isEmpty(),
                        messageSource.getMessage("filter.anySport", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        toggleValue(selectedSports, "football"),
                        selectedVisibility,
                        selectedCategories,
                        selectedSports.contains("football"),
                        messageSource.getMessage("sport.football", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        toggleValue(selectedSports, "tennis"),
                        selectedVisibility,
                        selectedCategories,
                        selectedSports.contains("tennis"),
                        messageSource.getMessage("sport.tennis", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        toggleValue(selectedSports, "basketball"),
                        selectedVisibility,
                        selectedCategories,
                        selectedSports.contains("basketball"),
                        messageSource.getMessage("sport.basketball", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        toggleValue(selectedSports, "padel"),
                        selectedVisibility,
                        selectedCategories,
                        selectedSports.contains("padel"),
                        messageSource.getMessage("sport.padel", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        toggleValue(selectedSports, "other"),
                        selectedVisibility,
                        selectedCategories,
                        selectedSports.contains("other"),
                        messageSource.getMessage("sport.other", null, locale)));
    }

    private List<FilterOptionViewModel> buildStatusFilterOptions(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories,
            final List<String> allowedStatuses) {
        final List<FilterOptionViewModel> options = new ArrayList<>();
        options.add(
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
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
                            searchQuery,
                            sort,
                            startDate,
                            endDate,
                            minPrice,
                            maxPrice,
                            timezone,
                            toggleValue(selectedStatuses, status),
                            selectedSports,
                            selectedVisibility,
                            selectedCategories,
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
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories) {
        return List.of(
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        List.of(),
                        selectedCategories,
                        selectedVisibility.isEmpty(),
                        messageSource.getMessage("filter.anyVisibility", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        toggleValue(selectedVisibility, "public"),
                        selectedCategories,
                        selectedVisibility.contains("public"),
                        messageSource.getMessage("visibility.public", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        toggleValue(selectedVisibility, "private"),
                        selectedCategories,
                        selectedVisibility.contains("private"),
                        messageSource.getMessage("visibility.private", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        toggleValue(selectedVisibility, "invite_only"),
                        selectedCategories,
                        selectedVisibility.contains("invite_only"),
                        messageSource.getMessage("visibility.inviteOnly", null, locale)));
    }

    private List<FilterOptionViewModel> buildCategoryFilterOptions(
            final String path,
            final Locale locale,
            final String searchQuery,
            final String sort,
            final String startDate,
            final String endDate,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final String timezone,
            final List<String> selectedStatuses,
            final List<String> selectedSports,
            final List<String> selectedVisibility,
            final List<String> selectedCategories) {
        return List.of(
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        List.of(),
                        selectedCategories.isEmpty(),
                        messageSource.getMessage("filter.anyCategory", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        toggleValue(selectedCategories, "joined"),
                        selectedCategories.contains("joined"),
                        messageSource.getMessage("category.joined", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        toggleValue(selectedCategories, "invited"),
                        selectedCategories.contains("invited"),
                        messageSource.getMessage("category.invited", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        toggleValue(selectedCategories, "pending"),
                        selectedCategories.contains("pending"),
                        messageSource.getMessage("category.pending", null, locale)),
                filterOption(
                        path,
                        locale,
                        searchQuery,
                        sort,
                        startDate,
                        endDate,
                        minPrice,
                        maxPrice,
                        timezone,
                        selectedStatuses,
                        selectedSports,
                        selectedVisibility,
                        toggleValue(selectedCategories, "hosted"),
                        selectedCategories.contains("hosted"),
                        messageSource.getMessage("category.hosted", null, locale)));
    }

    private static String normalizeQuery(final String query) {
        return query == null ? "" : query.trim();
    }

    private static DateRange normalizeDateRange(
            final String rawStartDate,
            final String rawEndDate,
            final DateRangeContext context,
            final ZoneId zoneId) {
        LocalDate startDate = parseDate(rawStartDate);
        LocalDate endDate = parseDate(rawEndDate);
        final LocalDate today = LocalDate.now(zoneId);

        if (context == DateRangeContext.UPCOMING) {
            if (startDate == null) {
                startDate = today;
            } else if (startDate.isBefore(today)) {
                startDate = today;
            }
            // Discard an endDate that is today-or-before — it was likely
            // left over from a past filter and would create an empty window.
            if (endDate != null && !endDate.isAfter(today)) {
                endDate = null;
            }
        } else if (context == DateRangeContext.PAST) {
            if (endDate == null) {
                endDate = today;
            } else if (endDate.isAfter(today)) {
                endDate = today;
            }
            // Discard a startDate that is today-or-after — it was likely
            // left over from an upcoming filter and would create an empty window.
            if (startDate != null && !startDate.isBefore(today)) {
                startDate = null;
            }
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            final LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        return new DateRange(
                startDate == null ? null : startDate.toString(),
                endDate == null ? null : endDate.toString());
    }

    private static DateRangeBounds dateRangeBounds(final String path, final ZoneId zoneId) {
        final LocalDate today = LocalDate.now(zoneId);
        if (path.endsWith("/finished") || path.endsWith("/past")) {
            return new DateRangeBounds(null, today.toString());
        }
        return new DateRangeBounds(today.toString(), null);
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

    private record DateRange(String startDate, String endDate) {}

    private record DateRangeBounds(String minDate, String maxDate) {}

    private enum DateRangeContext {
        UPCOMING,
        PAST
    }
}
