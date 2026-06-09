package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventType;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.webapp.form.SearchForm;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class MatchDashboardController {

    private static final int PAGE_SIZE = 12;
    private static final String MATCHES_PATH = "/matches";
    private static final String TOURNAMENTS_PATH = "/tournaments";

    private final MatchService matchService;
    private final MatchParticipationService matchParticipationService;
    private final MatchReservationService matchReservationService;
    private final TournamentService tournamentService;

    @Autowired
    public MatchDashboardController(
            final MatchService matchService,
            final MatchParticipationService matchParticipationService,
            final MatchReservationService matchReservationService,
            final TournamentService tournamentService) {
        this.matchService = matchService;
        this.matchParticipationService = matchParticipationService;
        this.matchReservationService = matchReservationService;
        this.tournamentService = tournamentService;
    }

    @GetMapping("/events")
    public ModelAndView redirectLegacyEvents(
            @RequestParam final MultiValueMap<String, String> queryParams) {
        final String type = queryParams.getFirst("type");
        final UriComponentsBuilder redirectUrl =
                UriComponentsBuilder.fromPath(
                        "tournament".equalsIgnoreCase(type) ? TOURNAMENTS_PATH : MATCHES_PATH);

        queryParams.forEach(
                (name, values) -> {
                    if (!"type".equals(name)) {
                        values.forEach(value -> redirectUrl.queryParam(name, value));
                    }
                });

        return new ModelAndView("redirect:" + redirectUrl.build().encode().toUriString());
    }

    @GetMapping(MATCHES_PATH)
    public ModelAndView showMatchesPage(
            @AuthenticatedUser final User user,
            @Valid @ModelAttribute("searchForm") final SearchForm searchForm,
            final BindingResult bindingResult) {
        return showDashboardPage(
                user,
                searchForm,
                bindingResult,
                EventType.MATCH,
                MATCHES_PATH,
                "page.title.matches",
                "matches.title");
    }

    @GetMapping(TOURNAMENTS_PATH)
    public ModelAndView showTournamentsPage(
            @AuthenticatedUser final User user,
            @Valid @ModelAttribute("searchForm") final SearchForm searchForm,
            final BindingResult bindingResult) {
        return showDashboardPage(
                user,
                searchForm,
                bindingResult,
                EventType.TOURNAMENT,
                TOURNAMENTS_PATH,
                "page.title.tournaments",
                "tournaments.title");
    }

    private ModelAndView showDashboardPage(
            final User user,
            final SearchForm searchForm,
            final BindingResult bindingResult,
            final EventType routeType,
            final String path,
            final String pageTitleCode,
            final String listTitleCode) {
        searchForm.setType(routeType);
        // A malformed free-text query should not produce an error page: render the
        // listing with the inline validation message and ignore the invalid query
        // for the search itself. Structural binding problems (page, dates, prices)
        // remain a controlled 400.
        if (hasErrorsOutsideQuery(bindingResult)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        final MatchDashboardQueryState.DashboardSelection selection =
                MatchDashboardQueryState.resolve(searchForm);
        final SearchForm viewSearchForm = selection.searchForm();
        final String searchQuery = bindingResult.hasFieldErrors("q") ? "" : viewSearchForm.getQ();

        final PaginatedResult<Match> result =
                selection.tournament()
                        ? null
                        : matchService.findDashboardMatches(
                                user,
                                selection.upcoming(),
                                selection.includeHosted(),
                                searchQuery,
                                viewSearchForm.getSport(),
                                viewSearchForm.getStatus(),
                                viewSearchForm.getStartDate(),
                                viewSearchForm.getEndDate(),
                                viewSearchForm.getMinPrice(),
                                viewSearchForm.getMaxPrice(),
                                viewSearchForm.getSort(),
                                selection.participantStatuses(),
                                viewSearchForm.getPage(),
                                PAGE_SIZE);
        final PaginatedResult<Tournament> tournamentResult =
                selection.tournament()
                        ? tournamentService.findDashboardTournaments(
                                user,
                                selection.upcoming(),
                                selection.includeHosted(),
                                searchQuery,
                                viewSearchForm.getSport(),
                                viewSearchForm.getStartDate(),
                                viewSearchForm.getEndDate(),
                                viewSearchForm.getSort(),
                                viewSearchForm.getPage(),
                                PAGE_SIZE,
                                viewSearchForm.getMinPrice(),
                                viewSearchForm.getMaxPrice(),
                                viewSearchForm.getLatitude(),
                                viewSearchForm.getLongitude())
                        : null;

        final ModelAndView mav =
                MatchDashboardPageSupport.buildListPage(
                        user,
                        "events/list",
                        path,
                        pageTitleCode,
                        listTitleCode,
                        selection,
                        result,
                        tournamentResult,
                        matchParticipationService,
                        matchReservationService,
                        tournamentService);
        if (bindingResult.hasFieldErrors("q")) {
            // Expose the bound form (whose BindingResult carries the query error) so the
            // listing re-renders with the inline validation message instead of the
            // normalized copy, which would drop the error.
            mav.addObject("searchForm", searchForm);
        }
        return mav;
    }

    private static boolean hasErrorsOutsideQuery(final BindingResult bindingResult) {
        if (bindingResult.hasGlobalErrors()) {
            return true;
        }
        return bindingResult.getFieldErrors().stream()
                .anyMatch(error -> !"q".equals(error.getField()));
    }
}
