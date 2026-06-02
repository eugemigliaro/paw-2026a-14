package ar.edu.itba.paw.webapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.paw.models.types.EventType;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.ParticipantStatus;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.webapp.form.SearchForm;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchDashboardQueryStateTest {

    @Test
    void resolveClearsMatchOnlyFiltersForTournamentQueries() {
        final SearchForm searchForm = new SearchForm();
        searchForm.setType(EventType.TOURNAMENT);
        searchForm.setCategory(List.of("joined", "hosted"));
        searchForm.setStatus(List.of());
        searchForm.setSport(List.of(Sport.PADEL));
        searchForm.setVisibility(List.of(EventVisibility.PUBLIC));
        searchForm.setStartDate(LocalDate.of(2099, 4, 10));
        searchForm.setEndDate(LocalDate.of(2099, 4, 11));

        final MatchDashboardQueryState.DashboardSelection selection =
                MatchDashboardQueryState.resolve(searchForm);

        assertTrue(selection.tournament());
        assertTrue(selection.searchForm().getCategory().isEmpty());
        assertNull(selection.searchForm().getStartDate());
        assertNull(selection.searchForm().getEndDate());
        assertNull(selection.includeHosted());
        assertTrue(selection.participantStatuses().isEmpty());
    }

    @Test
    void resolveDerivesPastMatchSelectionsFromCategories() {
        final SearchForm searchForm = new SearchForm();
        searchForm.setFilter("past");
        searchForm.setCategory(List.of("joined", "pending", "hosted"));
        searchForm.setStartDate(null);
        searchForm.setEndDate(null);

        final MatchDashboardQueryState.DashboardSelection selection =
                MatchDashboardQueryState.resolve(searchForm);

        final ZoneId zoneId = ZoneId.systemDefault();
        assertFalse(selection.tournament());
        assertEquals(LocalDate.now(zoneId), selection.searchForm().getEndDate());
        assertNull(selection.searchForm().getStartDate());
        assertEquals(Boolean.TRUE, selection.includeHosted());
        assertEquals(
                List.of(
                        ParticipantStatus.JOINED,
                        ParticipantStatus.CHECKED_IN,
                        ParticipantStatus.PENDING_APPROVAL),
                selection.participantStatuses());
        assertNotNull(selection.selectedTimezone());
    }
}
