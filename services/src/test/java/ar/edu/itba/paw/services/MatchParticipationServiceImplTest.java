package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.MatchParticipationException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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

    @Mock private MatchDao matchDao;
    @Mock private MatchParticipantDao matchParticipantDao;
    @Mock private UserService userService;

    private MatchParticipationServiceImpl matchParticipationService;

    private static final Instant NOW = Instant.parse("2026-04-20T10:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneId.of("UTC"));

    @BeforeEach
    public void setUp() {
        matchParticipationService =
                new MatchParticipationServiceImpl(
                        matchDao, matchParticipantDao, userService, clock);
    }

    @Test
    public void testRequestToJoinSuccess() {
        final Match match =
                createMatch(
                        10L,
                        1L,
                        "open",
                        "public",
                        "approval_required",
                        NOW.plusSeconds(3600),
                        10,
                        5);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(match));
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 2L)).thenReturn(false);
        Mockito.when(matchParticipantDao.hasPendingRequest(10L, 2L)).thenReturn(false);
        Mockito.when(matchParticipantDao.createJoinRequest(10L, 2L)).thenReturn(true);

        Assertions.assertDoesNotThrow(() -> matchParticipationService.requestToJoin(10L, 2L));
        Mockito.verify(matchParticipantDao).createJoinRequest(10L, 2L);
    }

    @Test
    public void testRequestToJoinFailsIfMatchClosed() {
        final Match match =
                createMatch(
                        10L,
                        1L,
                        "cancelled",
                        "public",
                        "approval_required",
                        NOW.plusSeconds(3600),
                        10,
                        5);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(match));

        final MatchParticipationException ex =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () -> matchParticipationService.requestToJoin(10L, 2L));
        Assertions.assertEquals("closed", ex.getCode());
    }

    @Test
    public void testRequestToJoinFailsIfMatchFull() {
        final Match match =
                createMatch(
                        10L,
                        1L,
                        "open",
                        "public",
                        "approval_required",
                        NOW.plusSeconds(3600),
                        10,
                        10); // 10 joined out of 10 max
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(match));
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 2L)).thenReturn(false);
        Mockito.when(matchParticipantDao.hasPendingRequest(10L, 2L)).thenReturn(false);

        final MatchParticipationException ex =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () -> matchParticipationService.requestToJoin(10L, 2L));
        Assertions.assertEquals("full", ex.getCode());
    }

    @Test
    public void testApproveRequestSuccess() {
        final Match match =
                createMatch(
                        10L,
                        1L,
                        "open",
                        "public",
                        "approval_required",
                        NOW.plusSeconds(3600),
                        10,
                        5);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(match));
        Mockito.when(matchParticipantDao.approveRequest(10L, 2L)).thenReturn(true);

        Assertions.assertDoesNotThrow(() -> matchParticipationService.approveRequest(10L, 1L, 2L));
        Mockito.verify(matchParticipantDao).approveRequest(10L, 2L);
    }

    @Test
    public void testApproveRequestFailsIfNotHost() {
        final Match match =
                createMatch(
                        10L,
                        1L,
                        "open",
                        "public",
                        "approval_required",
                        NOW.plusSeconds(3600),
                        10,
                        5);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(match));

        final MatchParticipationException ex =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () -> matchParticipationService.approveRequest(10L, 3L, 2L));
        Assertions.assertEquals("forbidden", ex.getCode());
    }

    @Test
    public void testInviteUserSuccess() {
        final Match match =
                createMatch(
                        10L, 1L, "open", "private", "invite_only", NOW.plusSeconds(3600), 10, 5);
        final User user = new User(2L, "user@test.com", "user", "User", "Test", null, null);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(match));
        Mockito.when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 2L)).thenReturn(false);
        Mockito.when(matchParticipantDao.hasInvitation(10L, 2L)).thenReturn(false);
        Mockito.when(matchParticipantDao.inviteUser(10L, 2L)).thenReturn(true);

        Assertions.assertDoesNotThrow(
                () -> matchParticipationService.inviteUser(10L, 1L, "user@test.com"));
        Mockito.verify(matchParticipantDao).inviteUser(10L, 2L);
    }

    @Test
    public void testAcceptInviteSuccess() {
        final Match match =
                createMatch(
                        10L, 1L, "open", "private", "invite_only", NOW.plusSeconds(3600), 10, 5);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(match));
        Mockito.when(matchParticipantDao.hasInvitation(10L, 2L)).thenReturn(true);
        Mockito.when(matchParticipantDao.acceptInvite(10L, 2L)).thenReturn(true);

        Assertions.assertDoesNotThrow(() -> matchParticipationService.acceptInvite(10L, 2L));
        Mockito.verify(matchParticipantDao).acceptInvite(10L, 2L);
    }

    private Match createMatch(
            final Long id,
            final Long hostId,
            final String status,
            final String visibility,
            final String joinPolicy,
            final Instant startsAt,
            final int maxPlayers,
            final int joinedPlayers) {
        return new Match(
                id,
                Sport.FOOTBALL,
                hostId,
                "Address",
                "Title",
                "Desc",
                startsAt,
                startsAt.plusSeconds(3600),
                maxPlayers,
                BigDecimal.ZERO,
                visibility,
                joinPolicy,
                status,
                joinedPlayers,
                null);
    }
}
