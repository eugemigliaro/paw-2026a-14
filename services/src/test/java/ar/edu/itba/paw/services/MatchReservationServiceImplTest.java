package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSeries;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.MatchReservationException;
import ar.edu.itba.paw.services.utils.MatchUtils;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MatchReservationServiceImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T18:00:00Z");

    @Mock private MatchDao matchDao;
    @Mock private MatchParticipantDao matchParticipantDao;
    @Mock private MatchNotificationService matchNotificationService;

    private MatchReservationServiceImpl matchReservationService;

    @BeforeEach
    public void setUp() {
        matchReservationService =
                new MatchReservationServiceImpl(
                        matchDao,
                        matchParticipantDao,
                        matchNotificationService,
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    public void testReserveSpotSucceedsForOpenUpcomingMatch() {
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        EventVisibility.PUBLIC,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600),
                                        4,
                                        1)));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, u)).thenReturn(false);
        Mockito.when(matchParticipantDao.createReservationIfSpace(10L, u)).thenReturn(true);

        Assertions.assertDoesNotThrow(() -> matchReservationService.reserveSpot(10L, u));
    }

    @Test
    public void testReserveSpotSucceedsForHostSelfReservation() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        EventVisibility.PRIVATE,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600),
                                        4,
                                        1)));
        final User u = UserUtils.getUser(1L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, u)).thenReturn(false);
        Mockito.when(matchParticipantDao.createReservationIfSpace(10L, u)).thenReturn(true);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(() -> matchReservationService.reserveSpot(10L, u));
    }

    @Test
    public void testReserveSpotRejectsPrivateInviteOnlyForNonHost() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        EventVisibility.PRIVATE,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600),
                                        4,
                                        1)));

        // Exercise
        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSpot(10L, UserUtils.getUser(20L)));

        // Assert
        Assertions.assertEquals("closed", exception.getCode());
    }

    @Test
    public void testFindActiveFutureReservationMatchIdsForSeriesUsesCurrentClock() {
        // Arrange
        final User u = UserUtils.getUser(20L);
        Mockito.when(
                        matchParticipantDao.findActiveFutureReservationMatchIdsForSeries(
                                100L, u, FIXED_NOW))
                .thenReturn(List.of(10L, 11L));

        // Exercise
        final Set<Long> matchIds =
                matchReservationService.findActiveFutureReservationMatchIdsForSeries(100L, u);

        // Assert
        Assertions.assertEquals(Set.of(10L, 11L), matchIds);
    }

    @Test
    public void testReserveSpotRejectsDuplicateReservation() {
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        EventVisibility.PUBLIC,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600),
                                        4,
                                        1)));

        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, u)).thenReturn(true);

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSpot(10L, u));

        Assertions.assertEquals("already_joined", exception.getCode());
    }

    @Test
    public void testReserveSpotRejectsFullMatchBeforeInsert() {
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        EventVisibility.PUBLIC,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600),
                                        4,
                                        4)));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, u)).thenReturn(false);

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSpot(10L, u));

        Assertions.assertEquals("full", exception.getCode());
    }

    @Test
    public void testReserveSpotRejectsStartedMatch() {
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        EventVisibility.PUBLIC,
                                        EventStatus.OPEN,
                                        FIXED_NOW.minusSeconds(60),
                                        4,
                                        1)));

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSpot(10L, UserUtils.getUser(20L)));

        Assertions.assertEquals("started", exception.getCode());
    }

    @Test
    public void testReserveSpotRejectsCancelledMatch() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        EventVisibility.PUBLIC,
                                        EventStatus.CANCELLED,
                                        FIXED_NOW.plusSeconds(3600),
                                        4,
                                        1)));

        // Exercise
        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSpot(10L, UserUtils.getUser(20L)));

        // Assert
        Assertions.assertEquals("closed", exception.getCode());
    }

    @Test
    public void testReserveSpotRejectsMissingMatch() {
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.empty());

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSpot(10L, UserUtils.getUser(20L)));

        Assertions.assertEquals("not_found", exception.getCode());
    }

    @Test
    public void testReserveSpotMapsLateRaceConditionToFull() {
        final Match initialMatch =
                createMatch(
                        EventVisibility.PUBLIC,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(3600),
                        4,
                        3);
        final Match fullMatch =
                createMatch(
                        EventVisibility.PUBLIC,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(3600),
                        4,
                        4);

        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(Optional.of(initialMatch))
                .thenReturn(Optional.of(fullMatch));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, u)).thenReturn(false);
        Mockito.when(matchParticipantDao.createReservationIfSpace(10L, u)).thenReturn(false);

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSpot(10L, u));

        Assertions.assertEquals("full", exception.getCode());
    }

    @Test
    public void testReserveSeriesSucceedsForFutureEligibleOccurrences() {
        final Match selectedOccurrence =
                createRecurringMatch(10L, FIXED_NOW.plusSeconds(3600), 4, 1, 100L, 1);
        final Match secondOccurrence =
                createRecurringMatch(11L, FIXED_NOW.plusSeconds(7200), 4, 0, 100L, 2);
        final Match startedOccurrence =
                createRecurringMatch(12L, FIXED_NOW.minusSeconds(60), 4, 0, 100L, 3);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDao.findSeriesOccurrences(100L))
                .thenReturn(List.of(startedOccurrence, selectedOccurrence, secondOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, u)).thenReturn(false);
        Mockito.when(matchParticipantDao.hasActiveReservation(11L, u)).thenReturn(false);
        Mockito.when(matchParticipantDao.createSeriesReservationsIfSpace(100L, u, FIXED_NOW))
                .thenReturn(2);

        Assertions.assertDoesNotThrow(() -> matchReservationService.reserveSeries(10L, u));
    }

    @Test
    public void testReserveSeriesSucceedsForHostSelfReservation() {
        // Arrange
        final Match selectedOccurrence =
                createRecurringMatch(
                        10L,
                        FIXED_NOW.plusSeconds(3600),
                        4,
                        1,
                        100L,
                        1,
                        EventJoinPolicy.APPROVAL_REQUIRED);
        final Match secondOccurrence =
                createRecurringMatch(
                        11L,
                        FIXED_NOW.plusSeconds(7200),
                        4,
                        0,
                        100L,
                        2,
                        EventJoinPolicy.APPROVAL_REQUIRED);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDao.findSeriesOccurrences(100L))
                .thenReturn(List.of(selectedOccurrence, secondOccurrence));
        final User u = UserUtils.getUser(1L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, u)).thenReturn(false);
        Mockito.when(matchParticipantDao.hasActiveReservation(11L, u)).thenReturn(false);
        Mockito.when(matchParticipantDao.createSeriesReservationsIfSpace(100L, u, FIXED_NOW))
                .thenReturn(2);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(() -> matchReservationService.reserveSeries(10L, u));
    }

    @Test
    public void testReserveSeriesRejectsDuplicateSeriesMembership() {
        final Match selectedOccurrence =
                createRecurringMatch(10L, FIXED_NOW.plusSeconds(3600), 4, 1, 100L, 1);
        final Match secondOccurrence =
                createRecurringMatch(11L, FIXED_NOW.plusSeconds(7200), 4, 1, 100L, 2);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDao.findSeriesOccurrences(100L))
                .thenReturn(List.of(selectedOccurrence, secondOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDao.hasActiveReservation(11L, u)).thenReturn(true);

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSeries(10L, u));

        Assertions.assertEquals("series_already_joined", exception.getCode());
    }

    @Test
    public void testReserveSeriesRejectsFullFutureOccurrences() {
        final Match selectedOccurrence =
                createRecurringMatch(10L, FIXED_NOW.plusSeconds(3600), 1, 1, 100L, 1);
        final Match secondOccurrence =
                createRecurringMatch(11L, FIXED_NOW.plusSeconds(7200), 1, 1, 100L, 2);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDao.findSeriesOccurrences(100L))
                .thenReturn(List.of(selectedOccurrence, secondOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, u)).thenReturn(false);
        Mockito.when(matchParticipantDao.hasActiveReservation(11L, u)).thenReturn(false);

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSeries(10L, u));

        Assertions.assertEquals("series_full", exception.getCode());
    }

    @Test
    public void testReserveSeriesRejectsSeriesWithoutUpcomingOccurrences() {
        final Match selectedOccurrence =
                createRecurringMatch(10L, FIXED_NOW.minusSeconds(3600), 4, 1, 100L, 1);
        final Match earlierOccurrence =
                createRecurringMatch(11L, FIXED_NOW.minusSeconds(7200), 4, 0, 100L, 2);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDao.findSeriesOccurrences(100L))
                .thenReturn(List.of(earlierOccurrence, selectedOccurrence));

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSeries(10L, UserUtils.getUser(20L)));

        Assertions.assertEquals("series_started", exception.getCode());
    }

    @Test
    public void testCancelSeriesReservationsCancelsFutureActiveReservations() {
        final Match selectedOccurrence =
                createRecurringMatch(10L, FIXED_NOW.plusSeconds(3600), 4, 1, 100L, 1);
        final Match secondOccurrence =
                createRecurringMatch(11L, FIXED_NOW.plusSeconds(7200), 4, 1, 100L, 2);
        final Match pastOccurrence =
                createRecurringMatch(12L, FIXED_NOW.minusSeconds(3600), 4, 1, 100L, 0);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDao.findSeriesOccurrences(100L))
                .thenReturn(List.of(pastOccurrence, selectedOccurrence, secondOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDao.hasActiveReservation(11L, u)).thenReturn(true);
        Mockito.when(matchParticipantDao.cancelFutureSeriesReservations(100L, u, FIXED_NOW))
                .thenReturn(2);

        Assertions.assertDoesNotThrow(
                () -> matchReservationService.cancelSeriesReservations(10L, u));
    }

    @Test
    public void testCancelSeriesReservationsRejectsUnjoinedFutureSeries() {
        final Match selectedOccurrence =
                createRecurringMatch(10L, FIXED_NOW.plusSeconds(3600), 4, 1, 100L, 1);
        final Match secondOccurrence =
                createRecurringMatch(11L, FIXED_NOW.plusSeconds(7200), 4, 1, 100L, 2);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDao.findSeriesOccurrences(100L))
                .thenReturn(List.of(selectedOccurrence, secondOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, u)).thenReturn(false);
        Mockito.when(matchParticipantDao.hasActiveReservation(11L, u)).thenReturn(false);

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.cancelSeriesReservations(10L, u));

        Assertions.assertEquals("series_not_joined", exception.getCode());
    }

    @Test
    public void testCancelSeriesReservationsRejectsSeriesWithoutUpcomingOccurrences() {
        final Match selectedOccurrence =
                createRecurringMatch(10L, FIXED_NOW.minusSeconds(3600), 4, 1, 100L, 1);
        final Match earlierOccurrence =
                createRecurringMatch(11L, FIXED_NOW.minusSeconds(7200), 4, 1, 100L, 0);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDao.findSeriesOccurrences(100L))
                .thenReturn(List.of(earlierOccurrence, selectedOccurrence));

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () ->
                                matchReservationService.cancelSeriesReservations(
                                        10L, UserUtils.getUser(20L)));

        Assertions.assertEquals("series_started", exception.getCode());
    }

    @Test
    public void testCancelSeriesReservationsRejectsNonRecurringMatch() {
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        EventVisibility.PUBLIC,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600),
                                        4,
                                        1)));

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () ->
                                matchReservationService.cancelSeriesReservations(
                                        10L, UserUtils.getUser(20L)));

        Assertions.assertEquals("not_recurring", exception.getCode());
    }

    private static Match createMatch(
            final EventVisibility visibility,
            final EventStatus status,
            final Instant startsAt,
            final int maxPlayers,
            final int joinedPlayers) {

        final EventJoinPolicy joinPolicy =
                visibility.equals(EventVisibility.PUBLIC)
                        ? EventJoinPolicy.DIRECT
                        : EventJoinPolicy.APPROVAL_REQUIRED;

        Match m = MatchUtils.createMatchWithId(10L, 1L, Sport.FOOTBALL, startsAt, maxPlayers);
        m.setVisibility(visibility);
        m.setJoinPolicy(joinPolicy);
        m.setStatus(status);
        m.setJoinedPlayers(joinedPlayers);

        return m;
    }

    private static Match createRecurringMatch(
            final Long id,
            final Instant startsAt,
            final int maxPlayers,
            final int joinedPlayers,
            final Long seriesId,
            final int occurrenceIndex) {
        return createRecurringMatch(
                id,
                startsAt,
                maxPlayers,
                joinedPlayers,
                seriesId,
                occurrenceIndex,
                EventJoinPolicy.DIRECT);
    }

    private static Match createRecurringMatch(
            final Long id,
            final Instant startsAt,
            final int maxPlayers,
            final int joinedPlayers,
            final Long seriesId,
            final int occurrenceIndex,
            final EventJoinPolicy joinPolicy) {

        MatchSeries series = MatchUtils.getMatchSeries(seriesId, UserUtils.getUser(1L));
        Match m = MatchUtils.createMatchWithId(id, 1L, Sport.FOOTBALL, startsAt, maxPlayers);

        m.setJoinedPlayers(joinedPlayers);
        m.setSeries(series);
        m.setSeriesOccurrenceIndex(occurrenceIndex);
        m.setJoinPolicy(joinPolicy);

        return m;
    }
}
