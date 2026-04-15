package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.bannerUrlFor;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.webapp.form.FeedSearchForm;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.EventCardViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FeedPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FilterGroupViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FilterOptionViewModel;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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
            @RequestParam(value = "sport", required = false) final String sport,
            @RequestParam(value = "time", defaultValue = "all") final String time,
            @RequestParam(value = "sort", defaultValue = "soonest") final String sort,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            @RequestParam(value = "tz", required = false) final String timezone,
            final Locale locale) {
        final String query =
                bindingResult.hasFieldErrors("q") || feedSearchForm.getQ() == null
                        ? ""
                        : feedSearchForm.getQ();
        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        query, sport, time, sort, page, PAGE_SIZE, timezone);
        final ModelAndView mav = new ModelAndView("feed/index");
        mav.addObject("shell", ShellViewModelFactory.browseShell(messageSource, locale));
        final String safeSort = normalizeSort(sort);
        mav.addObject("selectedSort", safeSort);
        mav.addObject(
                "feedPage",
                buildFeedPageViewModel(query, sport, time, safeSort, timezone, result, locale));
        return mav;
    }

    private FeedPageViewModel buildFeedPageViewModel(
            final String query,
            final String selectedSport,
            final String selectedTime,
            final String selectedSort,
            final String timezone,
            final PaginatedResult<Match> result,
            final Locale locale) {

        final ZoneId zoneId = parseZone(timezone);
        final String normalizedSport =
                selectedSport == null
                        ? ""
                        : Sport.fromDbValue(selectedSport).map(Sport::getDbValue).orElse("");
        final String normalizedTime = normalizeTime(selectedTime);

        return new FeedPageViewModel(
                "",
                messageSource.getMessage("feed.hero.title", null, locale),
                messageSource.getMessage("feed.hero.description", null, locale),
                messageSource.getMessage("feed.search.placeholder", null, locale),
                messageSource.getMessage("feed.search.button", null, locale),
                List.of(),
                buildFilterGroups(
                        query, normalizedSport, normalizedTime, selectedSort, timezone, locale),
                result.getItems().stream().map(match -> toCard(match, zoneId, locale)).toList(),
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

    private List<FilterGroupViewModel> buildFilterGroups(
            final String query,
            final String selectedSport,
            final String selectedTime,
            final String selectedSort,
            final String timezone,
            final Locale locale) {

        return List.of(
                new FilterGroupViewModel(
                        messageSource.getMessage("filter.categories", null, locale),
                        List.of(
                                new FilterOptionViewModel(
                                        messageSource.getMessage("filter.anySport", null, locale),
                                        buildUrl(
                                                query, "", selectedTime, selectedSort, 1, timezone),
                                        null,
                                        selectedSport.isBlank()),
                                new FilterOptionViewModel(
                                        messageSource.getMessage("sport.football", null, locale),
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
                                        messageSource.getMessage("sport.tennis", null, locale),
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
                                        messageSource.getMessage("sport.basketball", null, locale),
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
                                        messageSource.getMessage("sport.padel", null, locale),
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
                        messageSource.getMessage("filter.time", null, locale),
                        List.of(
                                new FilterOptionViewModel(
                                        messageSource.getMessage("filter.anyTime", null, locale),
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
                                        messageSource.getMessage("filter.today", null, locale),
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
                                        messageSource.getMessage("filter.tomorrow", null, locale),
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
                                        messageSource.getMessage("filter.thisWeek", null, locale),
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

    private EventCardViewModel toCard(final Match match, final ZoneId zoneId, final Locale locale) {
        final Locale resolvedLocale = resolvedLocale(locale);
        final String schedule =
                scheduleFormatter(resolvedLocale).format(match.getStartsAt().atZone(zoneId));
        final String priceLabel = toPriceLabel(match.getPricePerPlayer(), locale);

        return new EventCardViewModel(
                String.valueOf(match.getId()),
                "/events/" + match.getId(),
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
