package ar.edu.itba.paw.webapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.query.EventCategory;
import ar.edu.itba.paw.models.query.EventFilter;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventType;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.ParticipantStatus;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.webapp.form.SearchForm;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PaginationItemViewModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

class MatchDashboardPageSupportTest {

    private MatchParticipationService matchParticipationService;
    private MatchReservationService matchReservationService;
    private TournamentService tournamentService;

    @BeforeEach
    void setUp() {
        matchParticipationService = mock(MatchParticipationService.class);
        matchReservationService = mock(MatchReservationService.class);
        tournamentService = mock(TournamentService.class);
    }

    @Test
    void buildListPagePreservesTournamentStateAndPaginationQueryParameters() {
        final SearchForm searchForm = new SearchForm();
        searchForm.setType(EventType.TOURNAMENT);
        searchForm.setFilter(EventFilter.PAST);
        searchForm.setQ("summer cup");
        searchForm.setSport(List.of(Sport.PADEL, Sport.TENNIS));
        searchForm.setStatus(List.of(EventStatus.OPEN));
        searchForm.setVisibility(List.of(EventVisibility.PUBLIC));
        searchForm.setCategory(List.of(EventCategory.JOINED, EventCategory.HOSTED));
        searchForm.setMinPrice(new BigDecimal("12.50"));
        searchForm.setMaxPrice(new BigDecimal("50.00"));
        searchForm.setStartDate(LocalDate.of(2099, 4, 10));
        searchForm.setEndDate(LocalDate.of(2099, 4, 11));
        searchForm.setPage(2);

        final MatchDashboardQueryState.DashboardSelection selection =
                MatchDashboardQueryState.resolve(searchForm);
        final PaginatedResult<Tournament> tournamentResult =
                new PaginatedResult<>(List.of(), 2, 2, 1);

        final ModelAndView mav =
                MatchDashboardPageSupport.buildListPage(
                        null,
                        "events/list",
                        "/events",
                        "page.title.events",
                        selection,
                        null,
                        tournamentResult,
                        matchParticipationService,
                        matchReservationService,
                        tournamentService);

        assertEquals("tournament", mav.getModel().get("selectedType"));
        assertNull(mav.getModel().get("selectedStartDateValue"));
        assertNull(mav.getModel().get("selectedEndDateValue"));
        assertEquals(
                LocalDate.now(PlatformTime.ZONE).toString(),
                mav.getModel().get("selectedDateMaxValue"));
        assertNull(mav.getModel().get("selectedDateMinValue"));
        assertEquals(List.of(), mav.getModel().get("selectedCategories"));
        assertNull(mav.getModel().get("selectedTimezone"));
        assertEquals(2, mav.getModel().get("pageNumber"));
        assertTrue((Boolean) mav.getModel().get("pageHasPrevious"));
        assertFalse((Boolean) mav.getModel().get("pageHasNext"));

        @SuppressWarnings("unchecked")
        final List<PaginationItemViewModel> paginationItems =
                (List<PaginationItemViewModel>) mav.getModel().get("paginationItems");
        assertNotNull(paginationItems);
        assertEquals(2, paginationItems.size());
        assertTrue(paginationItems.get(0).getHref().contains("type=tournament"));
        assertTrue(paginationItems.get(0).getHref().contains("filter=PAST"));
        assertTrue(paginationItems.get(0).getHref().contains("q=summer%20cup"));
        assertTrue(paginationItems.get(0).getHref().contains("sport=padel"));
        assertTrue(paginationItems.get(0).getHref().contains("minPrice=12.50"));
        assertTrue(paginationItems.get(0).getHref().contains("maxPrice=50.00"));
    }

    @Test
    void buildListPageKeepsMatchCategoryAndStatusStateForMatchQueries() {
        final SearchForm searchForm = new SearchForm();
        searchForm.setType(EventType.MATCH);
        searchForm.setFilter(EventFilter.UPCOMING);
        searchForm.setCategory(List.of(EventCategory.JOINED, EventCategory.PENDING));
        searchForm.setStatus(List.of(EventStatus.OPEN, EventStatus.COMPLETED));
        searchForm.setSport(List.of(Sport.PADEL));
        final MatchDashboardQueryState.DashboardSelection selection =
                MatchDashboardQueryState.resolve(searchForm);
        final PaginatedResult<Match> matchResult = new PaginatedResult<>(List.of(), 0, 1, 1);
        final PaginatedResult<Tournament> tournamentResult =
                new PaginatedResult<>(List.of(), 0, 1, 1);

        final ModelAndView mav =
                MatchDashboardPageSupport.buildListPage(
                        null,
                        "events/list",
                        "/events",
                        "page.title.events",
                        selection,
                        matchResult,
                        tournamentResult,
                        matchParticipationService,
                        matchReservationService,
                        tournamentService);

        assertEquals("match", mav.getModel().get("selectedType"));
        assertEquals(List.of("joined", "pending"), mav.getModel().get("selectedCategories"));
        assertEquals(
                LocalDate.now(PlatformTime.ZONE).toString(),
                mav.getModel().get("selectedDateMinValue"));
        assertEquals(
                List.of(
                        ParticipantStatus.JOINED,
                        ParticipantStatus.CHECKED_IN,
                        ParticipantStatus.PENDING_APPROVAL),
                MatchDashboardQueryState.resolve(searchForm).participantStatuses());
        assertNotNull(mav.getModel().get("listControls"));
        assertNotNull(mav.getModel().get("events"));
    }
}
