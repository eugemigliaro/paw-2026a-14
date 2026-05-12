package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.MatchReservationException;
import java.math.BigDecimal;
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
    @Mock private UserService userService;

    private MatchReservationServiceImpl matchReservationService;

    @BeforeEach
    public void setUp() {
        matchReservationService =
                new MatchReservationServiceImpl(
                        matchDao,
                        matchParticipantDao,
                        matchNotificationService,
                        userService,
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
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L)).thenReturn(false);
        Mockito.when(matchParticipantDao.createReservationIfSpace(10L, 20L)).thenReturn(true);

        Assertions.assertDoesNotThrow(() -> matchReservationService.reserveSpot(10L, 20L));
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
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 1L)).thenReturn(false);
        Mockito.when(matchParticipantDao.createReservationIfSpace(10L, 1L)).thenReturn(true);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(() -> matchReservationService.reserveSpot(10L, 1L));
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
                        () -> matchReservationService.reserveSpot(10L, 20L));

        // Assert
        Assertions.assertEquals("closed", exception.getCode());
    }

    @Test
    public void testFindActiveFutureReservationMatchIdsForSeriesUsesCurrentClock() {
        // Arrange
        Mockito.when(
                        matchParticipantDao.findActiveFutureReservationMatchIdsForSeries(
                                100L, 20L, FIXED_NOW))
                .thenReturn(List.of(10L, 11L));

        // Exercise
        final Set<Long> matchIds =
                matchReservationService.findActiveFutureReservationMatchIdsForSeries(100L, 20L);

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
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L)).thenReturn(true);

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSpot(10L, 20L));

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
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L)).thenReturn(false);

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSpot(10L, 20L));

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
                        () -> matchReservationService.reserveSpot(10L, 20L));

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
                        () -> matchReservationService.reserveSpot(10L, 20L));

        // Assert
        Assertions.assertEquals("closed", exception.getCode());
    }

    @Test
    public void testReserveSpotRejectsMissingMatch() {
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.empty());

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSpot(10L, 20L));

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
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L))
                .thenReturn(false)
                .thenReturn(false);
        Mockito.when(matchParticipantDao.createReservationIfSpace(10L, 20L)).thenReturn(false);

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSpot(10L, 20L));

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
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L)).thenReturn(false);
        Mockito.when(matchParticipantDao.hasActiveReservation(11L, 20L)).thenReturn(false);
        Mockito.when(matchParticipantDao.createSeriesReservationsIfSpace(100L, 20L, FIXED_NOW))
                .thenReturn(2);

        Assertions.assertDoesNotThrow(() -> matchReservationService.reserveSeries(10L, 20L));
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
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 1L)).thenReturn(false);
        Mockito.when(matchParticipantDao.hasActiveReservation(11L, 1L)).thenReturn(false);
        Mockito.when(matchParticipantDao.createSeriesReservationsIfSpace(100L, 1L, FIXED_NOW))
                .thenReturn(2);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(() -> matchReservationService.reserveSeries(10L, 1L));
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
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.hasActiveReservation(11L, 20L)).thenReturn(true);

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSeries(10L, 20L));

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
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L)).thenReturn(false);
        Mockito.when(matchParticipantDao.hasActiveReservation(11L, 20L)).thenReturn(false);

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSeries(10L, 20L));

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
                        () -> matchReservationService.reserveSeries(10L, 20L));

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
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.hasActiveReservation(11L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.cancelFutureSeriesReservations(100L, 20L, FIXED_NOW))
                .thenReturn(2);

        Assertions.assertDoesNotThrow(
                () -> matchReservationService.cancelSeriesReservations(10L, 20L));
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
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L)).thenReturn(false);
        Mockito.when(matchParticipantDao.hasActiveReservation(11L, 20L)).thenReturn(false);

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.cancelSeriesReservations(10L, 20L));

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
                        () -> matchReservationService.cancelSeriesReservations(10L, 20L));

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
                        () -> matchReservationService.cancelSeriesReservations(10L, 20L));

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

        return new Match(
                10L,
                Sport.FOOTBALL,
                1L,
                "Test Address",
                "Test Match",
                "Test Description",
                startsAt,
                null,
                maxPlayers,
                BigDecimal.ZERO,
                visibility,
                joinPolicy,
                status,
                joinedPlayers,
                null);
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
        return new Match(
                id,
                Sport.FOOTBALL,
                1L,
                "Test Address",
                "Test Match",
                "Test Description",
                startsAt,
                null,
                maxPlayers,
                BigDecimal.ZERO,
                EventVisibility.PUBLIC,
                joinPolicy,
                EventStatus.OPEN,
                joinedPlayers,
                null,
                seriesId,
                occurrenceIndex);
    }
}
