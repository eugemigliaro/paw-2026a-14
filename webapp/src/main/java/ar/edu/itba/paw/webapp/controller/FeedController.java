package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.EventCardViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FeedPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FilterGroupViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FilterOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.NavItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ShellViewModel;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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
            @RequestParam(value = "q", defaultValue = "") final String query,
            @RequestParam(value = "sport", required = false) final String sport,
            @RequestParam(value = "time", defaultValue = "all") final String time,
            @RequestParam(value = "sort", defaultValue = "soonest") final String sort,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            @RequestParam(value = "tz", required = false) final String timezone) {
        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        query, sport, time, sort, page, PAGE_SIZE, timezone);
        final ModelAndView mav = new ModelAndView("feed/index");
        mav.addObject(
                "shell",
                new ShellViewModel(
                        "Match Point",
                        new NavItemViewModel("Switch to Hosting", "/host/events/new", false),
                        List.of()));
        final String safeSort = normalizeSort(sort);
        mav.addObject("selectedSort", safeSort);
        mav.addObject(
                "feedPage", buildFeedPageViewModel(query, sport, time, safeSort, timezone, result));
        return mav;
    }

    private FeedPageViewModel buildFeedPageViewModel(
            final String query,
            final String selectedSport,
            final String selectedTime,
            final String selectedSort,
            final String timezone,
            final PaginatedResult<Match> result) {

        final ZoneId zoneId = parseZone(timezone);
        final String normalizedSport =
                selectedSport == null
                        ? ""
                        : Sport.fromDbValue(selectedSport).map(Sport::getDbValue).orElse("");
        final String normalizedTime = normalizeTime(selectedTime);

        return new FeedPageViewModel(
                "",
                "Find your next game.",
                "Discover local sports events, join communities, and get active with "
                        + "Match Point.",
                "What sports event are you looking for?",
                "Find Matches",
                List.of(),
                buildFilterGroups(query, normalizedSport, normalizedTime, selectedSort, timezone),
                result.getItems().stream().map(match -> toCard(match, zoneId)).toList(),
                result.getPage(),
                result.getTotalPages(),
                result.hasPrevious()
                        ? buildUrl(
                                query,
                                normalizedSport,
                                normalizedTime,
                                selectedSort,
                                result.getPage() - 1,
                                timezone)
                        : null,
                result.hasNext()
                        ? buildUrl(
                                query,
                                normalizedSport,
                                normalizedTime,
                                selectedSort,
                                result.getPage() + 1,
                                timezone)
                        : null);
    }

    private static List<FilterGroupViewModel> buildFilterGroups(
            final String query,
            final String selectedSport,
            final String selectedTime,
            final String selectedSort,
            final String timezone) {

        return List.of(
                new FilterGroupViewModel(
                        "Categories",
                        List.of(
                                new FilterOptionViewModel(
                                        "Any sport",
                                        buildUrl(
                                                query, "", selectedTime, selectedSort, 1, timezone),
                                        null,
                                        selectedSport.isBlank()),
                                new FilterOptionViewModel(
                                        "Football",
                                        buildUrl(
                                                query,
                                                Sport.FOOTBALL.getDbValue(),
                                                selectedTime,
                                                selectedSort,
                                                1,
                                                timezone),
                                        null,
                                        Sport.FOOTBALL
                                                .getDbValue()
                                                .equalsIgnoreCase(selectedSport)),
                                new FilterOptionViewModel(
                                        "Tennis",
                                        buildUrl(
                                                query,
                                                Sport.TENNIS.getDbValue(),
                                                selectedTime,
                                                selectedSort,
                                                1,
                                                timezone),
                                        null,
                                        Sport.TENNIS.getDbValue().equalsIgnoreCase(selectedSport)),
                                new FilterOptionViewModel(
                                        "Basketball",
                                        buildUrl(
                                                query,
                                                Sport.BASKETBALL.getDbValue(),
                                                selectedTime,
                                                selectedSort,
                                                1,
                                                timezone),
                                        null,
                                        Sport.BASKETBALL
                                                .getDbValue()
                                                .equalsIgnoreCase(selectedSport)),
                                new FilterOptionViewModel(
                                        "Padel",
                                        buildUrl(
                                                query,
                                                Sport.PADEL.getDbValue(),
                                                selectedTime,
                                                selectedSort,
                                                1,
                                                timezone),
                                        null,
                                        Sport.PADEL.getDbValue().equalsIgnoreCase(selectedSport)))),
                new FilterGroupViewModel(
                        "Time",
                        List.of(
                                new FilterOptionViewModel(
                                        "Any time",
                                        buildUrl(
                                                query,
                                                selectedSport,
                                                "all",
                                                selectedSort,
                                                1,
                                                timezone),
                                        null,
                                        "all".equalsIgnoreCase(selectedTime)),
                                new FilterOptionViewModel(
                                        "Today",
                                        buildUrl(
                                                query,
                                                selectedSport,
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
                                                selectedSport,
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
                                                selectedSport,
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
                "All levels",
                mediaClassFor(match.getSport()),
                List.of("MP", "IA", "JV"));
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
            final String sport,
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

        if (sport != null && !sport.isBlank()) {
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
}
