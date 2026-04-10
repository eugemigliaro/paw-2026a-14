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
            @RequestParam(value = "tz", required = false) final String timezone) {
        final String query =
                bindingResult.hasFieldErrors("q") || feedSearchForm.getQ() == null
                        ? ""
                        : feedSearchForm.getQ();
        final List<String> normalizedSports = normalizeSports(sports);
        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        query,
                        encodeSports(normalizedSports),
                        time,
                        sort,
                        page,
                        PAGE_SIZE,
                        timezone);
        final ModelAndView mav = new ModelAndView("feed/index");
        mav.addObject("shell", ShellViewModelFactory.browseShell());
        final String safeSort = normalizeSort(sort);
        mav.addObject("selectedSort", safeSort);
        mav.addObject("selectedSports", normalizedSports);
        mav.addObject(
                "feedPage",
                buildFeedPageViewModel(query, normalizedSports, time, safeSort, timezone, result));
        return mav;
    }

    private FeedPageViewModel buildFeedPageViewModel(
            final String query,
            final List<String> selectedSports,
            final String selectedTime,
            final String selectedSort,
            final String timezone,
            final PaginatedResult<Match> result) {

        final ZoneId zoneId = parseZone(timezone);
        final List<String> normalizedSports = normalizeSports(selectedSports);
        final String normalizedTime = normalizeTime(selectedTime);

        return new FeedPageViewModel(
                "",
                "Find your next game.",
                "Discover local sports events, join communities, and get active with "
                        + "Match Point.",
                "What sports event are you looking for?",
                "Find Matches",
                List.of(),
                buildFilterGroups(query, normalizedSports, normalizedTime, selectedSort, timezone),
                result.getItems().stream().map(match -> toCard(match, zoneId)).toList(),
                result.getPage(),
                result.getTotalPages(),
                result.hasPrevious()
                        ? buildUrl(
                                query,
                                normalizedSports,
                                normalizedTime,
                                selectedSort,
                                result.getPage() - 1,
                                timezone)
                        : null,
                result.hasNext()
                        ? buildUrl(
                                query,
                                normalizedSports,
                                normalizedTime,
                                selectedSort,
                                result.getPage() + 1,
                                timezone)
                        : null);
    }

    private static List<FilterGroupViewModel> buildFilterGroups(
            final String query,
            final List<String> selectedSports,
            final String selectedTime,
            final String selectedSort,
            final String timezone) {

        return List.of(
                new FilterGroupViewModel(
                        "Sports",
                        List.of(
                                new FilterOptionViewModel(
                                        "Football",
                                        buildUrl(
                                                query,
                                                toggleSport(selectedSports, Sport.FOOTBALL),
                                                selectedTime,
                                                selectedSort,
                                                1,
                                                timezone),
                                        null,
                                        isSportSelected(selectedSports, Sport.FOOTBALL)),
                                new FilterOptionViewModel(
                                        "Tennis",
                                        buildUrl(
                                                query,
                                                toggleSport(selectedSports, Sport.TENNIS),
                                                selectedTime,
                                                selectedSort,
                                                1,
                                                timezone),
                                        null,
                                        isSportSelected(selectedSports, Sport.TENNIS)),
                                new FilterOptionViewModel(
                                        "Basketball",
                                        buildUrl(
                                                query,
                                                toggleSport(selectedSports, Sport.BASKETBALL),
                                                selectedTime,
                                                selectedSort,
                                                1,
                                                timezone),
                                        null,
                                        isSportSelected(selectedSports, Sport.BASKETBALL)),
                                new FilterOptionViewModel(
                                        "Padel",
                                        buildUrl(
                                                query,
                                                toggleSport(selectedSports, Sport.PADEL),
                                                selectedTime,
                                                selectedSort,
                                                1,
                                                timezone),
                                        null,
                                        isSportSelected(selectedSports, Sport.PADEL)))),
                new FilterGroupViewModel(
                        "Time",
                        List.of(
                                new FilterOptionViewModel(
                                        "Today",
                                        buildUrl(
                                                query,
                                                selectedSports,
                                                "today",
                                                selectedSort,
                                                1,
                                                timezone),
                                        null,
                                        "today".equalsIgnoreCase(selectedTime)),
                                new FilterOptionViewModel(
                                        "Tomorrow",
                                        buildUrl(
                                                query,
                                                selectedSports,
                                                "tomorrow",
                                                selectedSort,
                                                1,
                                                timezone),
                                        null,
                                        "tomorrow".equalsIgnoreCase(selectedTime)),
                                new FilterOptionViewModel(
                                        "This week",
                                        buildUrl(
                                                query,
                                                selectedSports,
                                                "week",
                                                selectedSort,
                                                1,
                                                timezone),
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
                "/events/" + match.getId(),
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

    private static String buildUrl(
            final String query,
            final List<String> sports,
            final String time,
            final String sort,
            final int page,
            final String timezone) {

        final UriComponentsBuilder builder =
                UriComponentsBuilder.fromPath("/")
                        .queryParam("q", query == null ? "" : query)
                        .queryParam("time", time == null ? "all" : time)
                        .queryParam("sort", normalizeSort(sort))
                        .queryParam("page", page);

        for (final String sport : normalizeSports(sports)) {
            builder.queryParam("sport", sport.toLowerCase(Locale.ROOT));
        }
        if (timezone != null && !timezone.isBlank()) {
            builder.queryParam("tz", timezone);
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
        for (final String sport : sports) {
            if (sport == null || sport.isBlank()) {
                continue;
            }
            Sport.fromDbValue(sport).map(Sport::getDbValue).ifPresent(normalizedSports::add);
        }

        return List.copyOf(normalizedSports);
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
}
