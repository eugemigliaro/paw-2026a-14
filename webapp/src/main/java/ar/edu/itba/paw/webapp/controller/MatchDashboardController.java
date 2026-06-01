package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.webapp.form.SearchForm;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class MatchDashboardController {

    private static final int PAGE_SIZE = 12;

    private final MatchService matchService;
    private final MatchParticipationService matchParticipationService;
    private final MatchReservationService matchReservationService;
    private final TournamentService tournamentService;
    private final MessageSource messageSource;

    @Autowired
    public MatchDashboardController(
            final MatchService matchService,
            final MatchParticipationService matchParticipationService,
            final MatchReservationService matchReservationService,
            final TournamentService tournamentService,
            final MessageSource messageSource) {
        this.matchService = matchService;
        this.matchParticipationService = matchParticipationService;
        this.matchReservationService = matchReservationService;
        this.tournamentService = tournamentService;
        this.messageSource = messageSource;
    }

    @GetMapping("/events")
    @PreAuthorize("isAuthenticated()")
    public ModelAndView showEventsPage(
            @Valid @ModelAttribute("searchForm") final SearchForm searchForm,
            final BindingResult bindingResult,
            final Locale locale) {
        if (bindingResult.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        final User user = SecurityControllerUtils.requireAuthenticatedUser();
        final MatchDashboardQueryState.DashboardSelection selection =
                MatchDashboardQueryState.resolve(searchForm);
        final SearchForm viewSearchForm = selection.searchForm();
        final ZoneId selectedTimezone = selection.selectedTimezone();
        final Instant startInstant =
                viewSearchForm.getStartDate() == null
                        ? null
                        : viewSearchForm.getStartDate().atStartOfDay(selectedTimezone).toInstant();
        final Instant endInstant =
                viewSearchForm.getEndDate() == null
                        ? null
                        : viewSearchForm
                                .getEndDate()
                                .plusDays(1)
                                .atStartOfDay(selectedTimezone)
                                .toInstant();

        final PaginatedResult<Match> result =
                selection.tournament()
                        ? null
                        : matchService.findDashboardMatches(
                                user,
                                selection.upcoming(),
                                selection.includeHosted(),
                                viewSearchForm.getQ(),
                                viewSearchForm.getSport(),
                                viewSearchForm.getStatus(),
                                startInstant,
                                endInstant,
                                viewSearchForm.getMinPrice(),
                                viewSearchForm.getMaxPrice(),
                                viewSearchForm.getSort(),
                                selectedTimezone,
                                selection.participantStatuses(),
                                viewSearchForm.getPage(),
                                PAGE_SIZE);
        final PaginatedResult<Tournament> tournamentResult =
                selection.tournament()
                        ? tournamentService.findHostedTournaments(
                                user,
                                viewSearchForm.getQ(),
                                viewSearchForm.getSport(),
                                startInstant,
                                endInstant,
                                viewSearchForm.getSort().getQueryValue(),
                                viewSearchForm.getPage(),
                                PAGE_SIZE,
                                selectedTimezone.getId(),
                                viewSearchForm.getMinPrice(),
                                viewSearchForm.getMaxPrice())
                        : null;

        return MatchDashboardPageSupport.buildListPage(
                "events/list",
                "/events",
                "page.title.events",
                locale,
                selection,
                result,
                tournamentResult,
                messageSource.getMessage("events.title", null, locale),
                messageSource.getMessage("events.description", null, locale),
                messageSource.getMessage("events.empty", null, locale),
                messageSource,
                matchParticipationService,
                matchReservationService);
    }
}
