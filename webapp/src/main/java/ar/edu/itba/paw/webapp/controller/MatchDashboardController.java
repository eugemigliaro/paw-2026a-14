package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.bannerUrlFor;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.MvpIdentityService;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.EventCardViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.PaginationItemViewModel;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
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

    private final MatchService matchService;
    private final MvpIdentityService mvpIdentityService;
    private final MessageSource messageSource;

    @Autowired
    public MatchDashboardController(
            final MatchService matchService,
            final MvpIdentityService mvpIdentityService,
            final MessageSource messageSource) {
        this.matchService = matchService;
        this.mvpIdentityService = mvpIdentityService;
        this.messageSource = messageSource;
    }

    @GetMapping("/host/matches")
    public ModelAndView showHostedMatches(
            @RequestParam("email") final String email,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            final Locale locale) {
        final User user = resolveUser(email);
        final PaginatedResult<Match> result =
                matchService.findHostedMatches(user.getId(), page, PAGE_SIZE);

        return buildListPage(
                "host/all-matches",
                "/host/matches",
                email,
                locale,
                result,
                messageSource.getMessage("host.dashboard.title", null, locale),
                messageSource.getMessage("host.dashboard.description", null, locale),
                messageSource.getMessage("host.dashboard.empty", null, locale),
                ShellViewModelFactory.hostDashboardShell(
                        messageSource, locale, email, "/host/matches"));
    }

    @GetMapping("/host/matches/finished")
    public ModelAndView showFinishedHostedMatches(
            @RequestParam("email") final String email,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            final Locale locale) {
        final User user = resolveUser(email);
        final PaginatedResult<Match> result =
                matchService.findFinishedHostedMatches(user.getId(), page, PAGE_SIZE);

        return buildListPage(
                "host/finished-matches",
                "/host/matches/finished",
                email,
                locale,
                result,
                messageSource.getMessage("host.finished.title", null, locale),
                messageSource.getMessage("host.finished.description", null, locale),
                messageSource.getMessage("host.finished.empty", null, locale),
                ShellViewModelFactory.hostDashboardShell(
                        messageSource, locale, email, "/host/matches/finished"));
    }

    @GetMapping("/player/matches/past")
    public ModelAndView showPastJoinedMatches(
            @RequestParam("email") final String email,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            final Locale locale) {
        final User user = resolveUser(email);
        final PaginatedResult<Match> result =
                matchService.findPastJoinedMatches(user.getId(), page, PAGE_SIZE);

        return buildListPage(
                "player/past-matches",
                "/player/matches/past",
                email,
                locale,
                result,
                messageSource.getMessage("player.past.title", null, locale),
                messageSource.getMessage("player.past.description", null, locale),
                messageSource.getMessage("player.past.empty", null, locale),
                ShellViewModelFactory.playerDashboardShell(
                        messageSource, locale, email, "/player/matches/past"));
    }

    @GetMapping("/player/matches/upcoming")
    public ModelAndView showUpcomingJoinedMatches(
            @RequestParam("email") final String email,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            final Locale locale) {
        final User user = resolveUser(email);
        final PaginatedResult<Match> result =
                matchService.findUpcomingJoinedMatches(user.getId(), page, PAGE_SIZE);

        return buildListPage(
                "player/upcoming-matches",
                "/player/matches/upcoming",
                email,
                locale,
                result,
                messageSource.getMessage("player.upcoming.title", null, locale),
                messageSource.getMessage("player.upcoming.description", null, locale),
                messageSource.getMessage("player.upcoming.empty", null, locale),
                ShellViewModelFactory.playerDashboardShell(
                        messageSource, locale, email, "/player/matches/upcoming"));
    }

    private ModelAndView buildListPage(
            final String view,
            final String path,
            final String email,
            final Locale locale,
            final PaginatedResult<Match> result,
            final String title,
            final String description,
            final String emptyMessage,
            final Object shell) {
        final ModelAndView mav = new ModelAndView(view);
        final ZoneId zoneId = ZoneId.systemDefault();

        mav.addObject("shell", shell);
        mav.addObject("listTitle", title);
        mav.addObject("listDescription", description);
        mav.addObject("emptyMessage", emptyMessage);
        mav.addObject(
                "events",
                result.getItems().stream().map(match -> toCard(match, zoneId, locale)).toList());
        mav.addObject("paginationItems", buildPagination(path, email, locale, result));
        mav.addObject(
                "previousPageHref",
                result.hasPrevious()
                        ? buildPageUrl(path, email, locale, result.getPage() - 1)
                        : null);
        mav.addObject(
                "nextPageHref",
                result.hasNext() ? buildPageUrl(path, email, locale, result.getPage() + 1) : null);
        return mav;
    }

    private User resolveUser(final String email) {
        try {
            return mvpIdentityService.resolveOrCreateByEmail(email);
        } catch (final IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }
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

    private static List<PaginationItemViewModel> buildPagination(
            final String path,
            final String email,
            final Locale locale,
            final PaginatedResult<Match> result) {
        if (result.getTotalPages() <= 1) {
            return List.of();
        }

        final List<PaginationItemViewModel> items = new ArrayList<>();
        final int startPage =
                Math.max(2, Math.min(result.getPage() - 1, result.getTotalPages() - 3));
        final int endPage = Math.min(result.getTotalPages() - 1, Math.max(result.getPage() + 1, 4));

        items.add(pageItem(path, email, locale, 1, result.getPage()));

        if (startPage > 2) {
            items.add(new PaginationItemViewModel("...", null, false, true));
        }

        for (int page = startPage; page <= endPage; page++) {
            items.add(pageItem(path, email, locale, page, result.getPage()));
        }

        if (endPage < result.getTotalPages() - 1) {
            items.add(new PaginationItemViewModel("...", null, false, true));
        }

        items.add(pageItem(path, email, locale, result.getTotalPages(), result.getPage()));

        return items;
    }

    private static PaginationItemViewModel pageItem(
            final String path,
            final String email,
            final Locale locale,
            final int page,
            final int currentPage) {
        return new PaginationItemViewModel(
                Integer.toString(page),
                buildPageUrl(path, email, locale, page),
                page == currentPage,
                false);
    }

    private static String buildPageUrl(
            final String path, final String email, final Locale locale, final int page) {
        final UriComponentsBuilder builder =
                UriComponentsBuilder.fromPath(path)
                        .queryParam("email", email)
                        .queryParam("page", page);

        if (locale != null && locale.getLanguage() != null && !locale.getLanguage().isBlank()) {
            builder.queryParam("lang", locale.getLanguage());
        }

        return builder.build().encode().toUriString();
    }
}
