package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.MatchFilterQueryUtils.normalizeCsvValues;

import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventType;
import ar.edu.itba.paw.models.types.ParticipantStatus;
import ar.edu.itba.paw.webapp.form.SearchForm;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class MatchDashboardQueryState {

    private MatchDashboardQueryState() {}

    static DashboardSelection resolve(final SearchForm sourceForm) {
        final SearchForm normalizedForm = copy(sourceForm);
        normalizedForm.normalizeDefaults();

        final boolean tournament = normalizedForm.getType() == EventType.TOURNAMENT;
        final boolean upcoming =
                !"past".equalsIgnoreCase(normalizedForm.getFilter()); // TODO: add enum for this
        final DateRangeContext context =
                upcoming ? DateRangeContext.UPCOMING : DateRangeContext.PAST;
        final DateRange dateRange =
                tournament
                        ? new DateRange(null, null)
                        : normalizeDateRange(
                                normalizedForm.getStartDate(),
                                normalizedForm.getEndDate(),
                                context);

        final List<String> selectedCategories =
                tournament ? List.of() : normalizeCsvValues(normalizedForm.getCategory());
        final List<EventStatus> selectedStatuses =
                tournament ? List.of() : normalizedForm.getStatus();
        final List<ParticipantStatus> participantStatuses =
                tournament ? List.of() : participantStatuses(selectedCategories);
        final Boolean includeHosted = tournament ? Boolean.TRUE : includeHosted(selectedCategories);

        normalizedForm.setCategory(selectedCategories);
        normalizedForm.setStatus(selectedStatuses);
        normalizedForm.setStartDate(dateRange.startDate());
        normalizedForm.setEndDate(dateRange.endDate());

        return new DashboardSelection(
                normalizedForm,
                tournament,
                upcoming,
                dateRange,
                includeHosted,
                participantStatuses);
    }

    private static SearchForm copy(final SearchForm sourceForm) {
        final SearchForm copy = new SearchForm();
        copy.setQ(sourceForm.getQ());
        copy.setSort(sourceForm.getSort());
        copy.setFilter(sourceForm.getFilter());
        copy.setType(sourceForm.getType());
        copy.setStartDate(sourceForm.getStartDate());
        copy.setEndDate(sourceForm.getEndDate());
        copy.setMinPrice(sourceForm.getMinPrice());
        copy.setMaxPrice(sourceForm.getMaxPrice());
        copy.setSport(sourceForm.getSport());
        copy.setStatus(sourceForm.getStatus());
        copy.setCategory(sourceForm.getCategory());
        copy.setVisibility(sourceForm.getVisibility());
        copy.setPage(sourceForm.getPage());
        return copy;
    }

    private static Boolean includeHosted(final List<String> selectedCategories) {
        if (selectedCategories.isEmpty()) {
            return Boolean.FALSE;
        }
        return selectedCategories.contains("hosted") ? Boolean.TRUE : Boolean.FALSE;
    }

    private static List<ParticipantStatus> participantStatuses(
            final List<String> selectedCategories) {
        if (selectedCategories.isEmpty()) {
            return List.of(
                    ParticipantStatus.JOINED,
                    ParticipantStatus.CHECKED_IN,
                    ParticipantStatus.INVITED);
        }

        final List<ParticipantStatus> participantStatuses = new ArrayList<>();
        // TODO: add enum for this (a category enum that includes "hosted", "joined", "invited", and
        // "pending")
        if (selectedCategories.contains("joined")) {
            participantStatuses.add(ParticipantStatus.JOINED);
            participantStatuses.add(ParticipantStatus.CHECKED_IN);
        }
        if (selectedCategories.contains("invited")) {
            participantStatuses.add(ParticipantStatus.INVITED);
        }
        if (selectedCategories.contains("pending")) {
            participantStatuses.add(ParticipantStatus.PENDING_APPROVAL);
        }
        return List.copyOf(participantStatuses);
    }

    private static DateRange normalizeDateRange(
            final LocalDate rawStartDate,
            final LocalDate rawEndDate,
            final DateRangeContext context) {
        LocalDate startDate = rawStartDate;
        LocalDate endDate = rawEndDate;
        final LocalDate today = LocalDate.now(PlatformTime.ZONE);

        if (context == DateRangeContext.UPCOMING) {
            if (startDate == null) {
                startDate = today;
            } else if (startDate.isBefore(today)) {
                startDate = today;
            }
            if (endDate != null && !endDate.isAfter(today)) {
                endDate = null;
            }
        } else if (context == DateRangeContext.PAST) {
            if (endDate == null) {
                endDate = today;
            } else if (endDate.isAfter(today)) {
                endDate = today;
            }
            if (startDate != null && !startDate.isBefore(today)) {
                startDate = null;
            }
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            final LocalDate temporary = startDate;
            startDate = endDate;
            endDate = temporary;
        }

        return new DateRange(startDate, endDate);
    }

    static final class DashboardSelection {

        private final SearchForm searchForm;
        private final boolean tournament;
        private final boolean upcoming;
        private final DateRange dateRange;
        private final Boolean includeHosted;
        private final List<ParticipantStatus> participantStatuses;

        private DashboardSelection(
                final SearchForm searchForm,
                final boolean tournament,
                final boolean upcoming,
                final DateRange dateRange,
                final Boolean includeHosted,
                final List<ParticipantStatus> participantStatuses) {
            this.searchForm = searchForm;
            this.tournament = tournament;
            this.upcoming = upcoming;
            this.dateRange = dateRange;
            this.includeHosted = includeHosted;
            this.participantStatuses = participantStatuses;
        }

        SearchForm searchForm() {
            return searchForm;
        }

        boolean tournament() {
            return tournament;
        }

        boolean upcoming() {
            return upcoming;
        }

        DateRange dateRange() {
            return dateRange;
        }

        Boolean includeHosted() {
            return includeHosted;
        }

        List<ParticipantStatus> participantStatuses() {
            return participantStatuses;
        }
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {}

    private enum DateRangeContext {
        UPCOMING,
        PAST
    }
}
