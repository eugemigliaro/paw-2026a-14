package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.MatchReservationException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
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

    private MatchReservationServiceImpl matchReservationService;

    @BeforeEach
    public void setUp() {
        matchReservationService =
                new MatchReservationServiceImpl(
                        matchDao, matchParticipantDao, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    public void testReserveSpotSucceedsForOpenUpcomingMatch() {
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch("public", "open", FIXED_NOW.plusSeconds(3600), 4, 1)));
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L)).thenReturn(false);
        Mockito.when(matchParticipantDao.createReservationIfSpace(10L, 20L)).thenReturn(true);

        Assertions.assertDoesNotThrow(() -> matchReservationService.reserveSpot(10L, 20L));
    }

    @Test
    public void testReserveSpotRejectsDuplicateReservation() {
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch("public", "open", FIXED_NOW.plusSeconds(3600), 4, 1)));
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
                                createMatch("public", "open", FIXED_NOW.plusSeconds(3600), 4, 4)));
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
                                createMatch("public", "open", FIXED_NOW.minusSeconds(60), 4, 1)));

        final MatchReservationException exception =
                Assertions.assertThrows(
                        MatchReservationException.class,
                        () -> matchReservationService.reserveSpot(10L, 20L));

        Assertions.assertEquals("started", exception.getCode());
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
        final Match initialMatch = createMatch("public", "open", FIXED_NOW.plusSeconds(3600), 4, 3);
        final Match fullMatch = createMatch("public", "open", FIXED_NOW.plusSeconds(3600), 4, 4);

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

    private static Match createMatch(
            final String visibility,
            final String status,
            final Instant startsAt,
            final int maxPlayers,
            final int joinedPlayers) {
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
                status,
                joinedPlayers,
                null);
    }
}
