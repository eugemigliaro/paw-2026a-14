package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.MatchParticipationException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MatchParticipationServiceImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T18:00:00Z");

    @Mock private MatchDao matchDao;
    @Mock private MatchParticipantDao matchParticipantDao;
    @Mock private UserService userService;

    private MatchParticipationServiceImpl matchParticipationService;

    @BeforeEach
    public void setUp() {
        matchParticipationService =
                new MatchParticipationServiceImpl(
                        matchDao,
                        matchParticipantDao,
                        userService,
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    public void testRemoveParticipantAllowsSelfLeaveForUpcomingPublicMatch() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        "public",
                                        "direct",
                                        "open",
                                        FIXED_NOW.plusSeconds(3600))));
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.removeParticipant(10L, 20L)).thenReturn(true);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(
                () -> matchParticipationService.removeParticipant(10L, 20L, 20L));
    }

    @Test
    public void testRemoveParticipantAllowsSelfLeaveForPrivateInviteOnlyMatch() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        "private",
                                        "invite_only",
                                        "open",
                                        FIXED_NOW.plusSeconds(3600))));
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.removeParticipant(10L, 20L)).thenReturn(true);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(
                () -> matchParticipationService.removeParticipant(10L, 20L, 20L));
    }

    @Test
    public void testRemoveParticipantRejectsSelfLeaveForStartedMatch() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        "public",
                                        "direct",
                                        "open",
                                        FIXED_NOW.minusSeconds(60))));

        // Exercise
        final MatchParticipationException exception =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () -> matchParticipationService.removeParticipant(10L, 20L, 20L));

        // Assert
        Assertions.assertEquals("started", exception.getCode());
    }

    @Test
    public void testRemoveParticipantRejectsSelfLeaveWithoutActiveReservation() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        "public",
                                        "direct",
                                        "open",
                                        FIXED_NOW.plusSeconds(3600))));
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L)).thenReturn(false);

        // Exercise
        final MatchParticipationException exception =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () -> matchParticipationService.removeParticipant(10L, 20L, 20L));

        // Assert
        Assertions.assertEquals("not_joined", exception.getCode());
    }

    @Test
    public void testRemoveParticipantStillRequiresHostWhenRemovingAnotherUser() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        "public",
                                        "approval_required",
                                        "open",
                                        FIXED_NOW.plusSeconds(3600))));

        // Exercise
        final MatchParticipationException exception =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () -> matchParticipationService.removeParticipant(10L, 20L, 30L));

        // Assert
        Assertions.assertEquals("forbidden", exception.getCode());
    }

    @Test
    public void testRemoveParticipantAllowsHostToRemoveAnotherUser() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        "public",
                                        "approval_required",
                                        "open",
                                        FIXED_NOW.plusSeconds(3600))));
        Mockito.when(matchParticipantDao.removeParticipant(10L, 30L)).thenReturn(true);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(
                () -> matchParticipationService.removeParticipant(10L, 1L, 30L));
    }

    @Test
    public void testRequestToJoinSeriesSucceedsForFutureApprovalRequiredOccurrences() {
        // Arrange
        final Match selectedOccurrence =
                createRecurringMatch(
                        10L,
                        "public",
                        "approval_required",
                        "open",
                        FIXED_NOW.plusSeconds(3600),
                        4,
                        1,
                        100L,
                        1);
        final Match secondOccurrence =
                createRecurringMatch(
                        11L,
                        "public",
                        "approval_required",
                        "open",
                        FIXED_NOW.plusSeconds(7200),
                        4,
                        0,
                        100L,
                        2);
        final Match pastOccurrence =
                createRecurringMatch(
                        12L,
                        "public",
                        "approval_required",
                        "open",
                        FIXED_NOW.minusSeconds(60),
                        4,
                        0,
                        100L,
                        0);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDao.findSeriesOccurrences(100L))
                .thenReturn(List.of(pastOccurrence, selectedOccurrence, secondOccurrence));
        Mockito.when(matchParticipantDao.createSeriesJoinRequestIfSpace(10L, 20L)).thenReturn(true);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(
                () -> matchParticipationService.requestToJoinSeries(10L, 20L));
    }

    @Test
    public void testApproveRequestExpandsSeriesJoinRequest() {
        // Arrange
        final Match selectedOccurrence =
                createRecurringMatch(
                        10L,
                        "public",
                        "approval_required",
                        "open",
                        FIXED_NOW.plusSeconds(3600),
                        4,
                        4,
                        100L,
                        1);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchParticipantDao.isSeriesJoinRequest(10L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.approveSeriesJoinRequest(100L, 20L, FIXED_NOW))
                .thenReturn(2);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(() -> matchParticipationService.approveRequest(10L, 1L, 20L));
    }

    @Test
    public void testRequestToJoinSeriesRejectsAlreadyPendingFutureSeries() {
        // Arrange
        final Match selectedOccurrence =
                createRecurringMatch(
                        10L,
                        "public",
                        "approval_required",
                        "open",
                        FIXED_NOW.plusSeconds(3600),
                        4,
                        1,
                        100L,
                        1);
        final Match secondOccurrence =
                createRecurringMatch(
                        11L,
                        "public",
                        "approval_required",
                        "open",
                        FIXED_NOW.plusSeconds(7200),
                        4,
                        0,
                        100L,
                        2);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDao.findSeriesOccurrences(100L))
                .thenReturn(List.of(selectedOccurrence, secondOccurrence));
        Mockito.when(matchParticipantDao.hasPendingRequest(10L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.hasPendingRequest(11L, 20L)).thenReturn(true);

        // Exercise
        final MatchParticipationException exception =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () -> matchParticipationService.requestToJoinSeries(10L, 20L));

        // Assert
        Assertions.assertEquals("series_already_pending", exception.getCode());
    }

    @Test
    public void testRequestToJoinSeriesRejectsNonRecurringMatch() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        "public",
                                        "approval_required",
                                        "open",
                                        FIXED_NOW.plusSeconds(3600))));

        // Exercise
        final MatchParticipationException exception =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () -> matchParticipationService.requestToJoinSeries(10L, 20L));

        // Assert
        Assertions.assertEquals("not_recurring", exception.getCode());
    }

    private static Match createMatch(
            final Long id,
            final String visibility,
            final String joinPolicy,
            final String status,
            final Instant startsAt) {
        return new Match(
                id,
                Sport.FOOTBALL,
                1L,
                "Test Address",
                "Test Match",
                "Test Description",
                startsAt,
                null,
                4,
                BigDecimal.ZERO,
                visibility,
                joinPolicy,
                status,
                1,
                null);
    }

    private static Match createRecurringMatch(
            final Long id,
            final String visibility,
            final String joinPolicy,
            final String status,
            final Instant startsAt,
            final int maxPlayers,
            final int joinedPlayers,
            final Long seriesId,
            final int occurrenceIndex) {
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
                visibility,
                joinPolicy,
                status,
                joinedPlayers,
                null,
                seriesId,
                occurrenceIndex);
    }
}
