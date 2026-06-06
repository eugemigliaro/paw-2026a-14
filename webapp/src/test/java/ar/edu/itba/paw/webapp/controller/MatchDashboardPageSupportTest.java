package ar.edu.itba.paw.webapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
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
import ar.edu.itba.paw.webapp.form.SearchForm;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PaginationItemViewModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.web.servlet.ModelAndView;

class MatchDashboardPageSupportTest {

    private MessageSource messageSource;
    private MatchParticipationService matchParticipationService;
    private MatchReservationService matchReservationService;

    @BeforeEach
    void setUp() {
        final StaticMessageSource staticMessageSource = new StaticMessageSource();
        staticMessageSource.addMessage("events.title", Locale.ENGLISH, "Events");
        staticMessageSource.addMessage("events.description", Locale.ENGLISH, "Description");
        staticMessageSource.addMessage("events.empty", Locale.ENGLISH, "Empty");
        staticMessageSource.addMessage("page.title.events", Locale.ENGLISH, "Events page");
        staticMessageSource.addMessage("filter.eventType", Locale.ENGLISH, "Type");
        staticMessageSource.addMessage("filter.eventType.matches", Locale.ENGLISH, "Matches");
        staticMessageSource.addMessage(
                "filter.eventType.tournaments", Locale.ENGLISH, "Tournaments");
        staticMessageSource.addMessage("filter.categories", Locale.ENGLISH, "Sports");
        staticMessageSource.addMessage("filter.category", Locale.ENGLISH, "Category");
        staticMessageSource.addMessage("host.filters.status", Locale.ENGLISH, "Status");
        staticMessageSource.addMessage("feed.sort.soonest", Locale.ENGLISH, "Soonest");
        staticMessageSource.addMessage("feed.sort.price", Locale.ENGLISH, "Price");
        staticMessageSource.addMessage("feed.sort.spots", Locale.ENGLISH, "Spots");
        staticMessageSource.addMessage("feed.sortBy", Locale.ENGLISH, "Sort by");
        staticMessageSource.addMessage("feed.aria.search", Locale.ENGLISH, "Search");
        staticMessageSource.addMessage("feed.search.placeholder", Locale.ENGLISH, "Search...");
        staticMessageSource.addMessage("feed.search.button", Locale.ENGLISH, "Go");
        staticMessageSource.addMessage("filter.title", Locale.ENGLISH, "Filters");
        staticMessageSource.addMessage("filter.anySport", Locale.ENGLISH, "Any sport");
        staticMessageSource.addMessage("sport.football", Locale.ENGLISH, "Football");
        staticMessageSource.addMessage("sport.tennis", Locale.ENGLISH, "Tennis");
        staticMessageSource.addMessage("sport.basketball", Locale.ENGLISH, "Basketball");
        staticMessageSource.addMessage("sport.padel", Locale.ENGLISH, "Padel");
        staticMessageSource.addMessage("sport.other", Locale.ENGLISH, "Other");
        staticMessageSource.addMessage("filter.anyStatus", Locale.ENGLISH, "Any status");
        staticMessageSource.addMessage("match.status.open", Locale.ENGLISH, "Open");
        staticMessageSource.addMessage("match.status.completed", Locale.ENGLISH, "Completed");
        staticMessageSource.addMessage("match.status.cancelled", Locale.ENGLISH, "Cancelled");
        staticMessageSource.addMessage("filter.anyCategory", Locale.ENGLISH, "Any category");
        staticMessageSource.addMessage("category.joined", Locale.ENGLISH, "Joined");
        staticMessageSource.addMessage("category.invited", Locale.ENGLISH, "Invited");
        staticMessageSource.addMessage("category.pending", Locale.ENGLISH, "Pending");
        staticMessageSource.addMessage("category.hosted", Locale.ENGLISH, "Hosted");
        staticMessageSource.addMessage("tournament.card.badge", Locale.ENGLISH, "Tournament");
        staticMessageSource.addMessage("tournament.status.open", Locale.ENGLISH, "Open");
        messageSource = staticMessageSource;
        matchParticipationService = mock(MatchParticipationService.class);
        matchReservationService = mock(MatchReservationService.class);
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
        searchForm.setTimezone(ZoneId.of("UTC"));
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
                        Locale.ENGLISH,
                        selection,
                        null,
                        tournamentResult,
                        "Events",
                        "Description",
                        "Empty",
                        messageSource,
                        matchParticipationService,
                        matchReservationService);

        assertEquals("tournament", mav.getModel().get("selectedType"));
        assertNull(mav.getModel().get("selectedStartDateValue"));
        assertNull(mav.getModel().get("selectedEndDateValue"));
        assertEquals(
                LocalDate.now(ZoneId.of("UTC")).toString(),
                mav.getModel().get("selectedDateMaxValue"));
        assertNull(mav.getModel().get("selectedDateMinValue"));
        assertEquals(List.of(), mav.getModel().get("selectedCategories"));
        assertEquals("UTC", mav.getModel().get("selectedTimezone"));
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
        searchForm.setTimezone(ZoneId.of("UTC"));

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
                        Locale.ENGLISH,
                        selection,
                        matchResult,
                        tournamentResult,
                        "Events",
                        "Description",
                        "Empty",
                        messageSource,
                        matchParticipationService,
                        matchReservationService);

        assertEquals("match", mav.getModel().get("selectedType"));
        assertEquals(List.of("joined", "pending"), mav.getModel().get("selectedCategories"));
        assertEquals(
                LocalDate.now(ZoneId.of("UTC")).toString(),
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
