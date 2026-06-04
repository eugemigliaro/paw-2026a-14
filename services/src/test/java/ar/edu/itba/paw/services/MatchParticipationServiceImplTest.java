package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSeries;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.matchParticipation.*;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.utils.MatchUtils;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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

    private RecordingMailDispatchService mailDispatchService;
    private MatchParticipationServiceImpl matchParticipationService;

    private static final Instant NOW = Instant.parse("2026-04-20T10:00:00Z");

    @BeforeEach
    public void setUp() {
        mailDispatchService = new RecordingMailDispatchService();
        final MatchNotificationService matchNotificationService =
                new MatchNotificationServiceImpl(matchParticipantDao, mailDispatchService);
        matchParticipationService =
                new MatchParticipationServiceImpl(
                        matchDao,
                        matchParticipantDao,
                        userService,
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                        mailDispatchService,
                        matchNotificationService);
    }

    @Test
    public void testFindPendingFutureRequestMatchIdsForSeriesUsesCurrentTime() {
        // Arrange
        final User u = UserUtils.getUser(20L);
        Mockito.when(
                        matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                                100L, u, FIXED_NOW))
                .thenReturn(List.of(10L, 11L));

        // Exercise
        final Set<Long> matchIds =
                matchParticipationService.findPendingFutureRequestMatchIdsForSeries(100L, u);

        // Assert
        Assertions.assertEquals(Set.of(10L, 11L), matchIds);
    }

    @Test
    public void testRequestToJoinRejectsHostSelfRequest() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.APPROVAL_REQUIRED,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));

        // Exercise + Assert
        Assertions.assertThrows(
                MatchParticipationIsHostException.class,
                () -> matchParticipationService.requestToJoin(10L, UserUtils.getUser(1L)));
    }

    @Test
    public void testRequestToJoinSeriesRejectsHostSelfRequest() {
        // Arrange
        final Match selectedOccurrence =
                createRecurringMatch(
                        10L,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(3600),
                        4,
                        1,
                        100L,
                        1);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));

        // Exercise + Assert
        Assertions.assertThrows(
                MatchParticipationIsHostException.class,
                () -> matchParticipationService.requestToJoinSeries(10L, UserUtils.getUser(1L)));
    }

    @Test
    public void testInviteUserRejectsHostSelfInvite() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PRIVATE,
                                        EventJoinPolicy.INVITE_ONLY,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));
        final User host = UserUtils.getUser(1L);
        Mockito.when(userService.findByEmail(host.getEmail())).thenReturn(Optional.of(host));

        // Exercise + Assert
        Assertions.assertThrows(
                MatchParticipationIsHostException.class,
                () -> matchParticipationService.inviteUser(10L, host, host.getEmail()));
        Assertions.assertTrue(mailDispatchService.actions.isEmpty());
    }

    @Test
    public void testAcceptInviteRejectsHostWithoutInvitation() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PRIVATE,
                                        EventJoinPolicy.INVITE_ONLY,
                                        EventStatus.CANCELLED,
                                        FIXED_NOW.plusSeconds(3600))));

        // Exercise + Assert
        Assertions.assertThrows(
                MatchParticipationIsHostException.class,
                () -> matchParticipationService.acceptInvite(10L, UserUtils.getUser(1L)));
    }

    @Test
    public void testRemoveParticipantAllowsSelfLeaveForUpcomingPublicMatch() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.DIRECT,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, u)).thenReturn(true);

        Mockito.when(matchParticipantDao.removeParticipant(10L, u)).thenReturn(true);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(() -> matchParticipationService.removeParticipant(10L, u, u));
    }

    @Test
    public void testRemoveParticipantAllowsSelfLeaveForPrivateInviteOnlyMatch() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PRIVATE,
                                        EventJoinPolicy.INVITE_ONLY,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDao.removeParticipant(10L, u)).thenReturn(true);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(() -> matchParticipationService.removeParticipant(10L, u, u));
    }

    @Test
    public void testRemoveParticipantNotifiesHostWhenPlayerLeaves() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PRIVATE,
                                        EventJoinPolicy.INVITE_ONLY,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));
        final User player = UserUtils.getUser(20L);
        final User host = UserUtils.getUser(1L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, player)).thenReturn(true);
        Mockito.when(matchParticipantDao.removeParticipant(10L, player)).thenReturn(true);

        // Exercise
        matchParticipationService.removeParticipant(10L, player, player);

        // Assert
        Assertions.assertEquals(List.of(host.getEmail()), mailDispatchService.recipients);
        Assertions.assertEquals(List.of("player-left"), mailDispatchService.actions);
    }

    @Test
    public void testRemoveParticipantRejectsSelfLeaveForStartedMatch() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.DIRECT,
                                        EventStatus.OPEN,
                                        FIXED_NOW.minusSeconds(60))));

        // Exercise + Assert
        Assertions.assertThrows(
                MatchParticipationStartedException.class,
                () ->
                        matchParticipationService.removeParticipant(
                                10L, UserUtils.getUser(20L), UserUtils.getUser(20L)));
    }

    @Test
    public void testRemoveParticipantRejectsSelfLeaveWithoutActiveReservation() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.DIRECT,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));

        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, u)).thenReturn(false);

        // Exercise + Assert
        Assertions.assertThrows(
                MatchParticipationNotJoinedException.class,
                () -> matchParticipationService.removeParticipant(10L, u, u));
    }

    @Test
    public void testRemoveParticipantStillRequiresHostWhenRemovingAnotherUser() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.APPROVAL_REQUIRED,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));

        // Exercise + Assert
        Assertions.assertThrows(
                MatchParticipationForbiddenException.class,
                () ->
                        matchParticipationService.removeParticipant(
                                10L, UserUtils.getUser(20L), UserUtils.getUser(30L)));
    }

    @Test
    public void testRemoveParticipantAllowsHostToRemoveAnotherUser() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.APPROVAL_REQUIRED,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));
        final User u = UserUtils.getUser(30L);
        Mockito.when(matchParticipantDao.removeParticipant(10L, u)).thenReturn(true);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(
                () -> matchParticipationService.removeParticipant(10L, UserUtils.getUser(1L), u));
    }

    @Test
    public void testRemoveParticipantNotifiesPlayerWhenHostKicksThem() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.APPROVAL_REQUIRED,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));
        final User player = UserUtils.getUser(30L);
        Mockito.when(matchParticipantDao.removeParticipant(10L, player)).thenReturn(true);

        // Exercise
        matchParticipationService.removeParticipant(10L, UserUtils.getUser(1L), player);

        // Assert
        Assertions.assertEquals(List.of(player.getEmail()), mailDispatchService.recipients);
        Assertions.assertEquals(List.of("player-removed"), mailDispatchService.actions);
    }

    @Test
    public void testFindPendingRequestMatchesSkipsHostedMatches() {
        // Arrange
        final Match hostedMatch =
                createMatchWithHost(
                        10L,
                        20L,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(3600));
        final Match otherMatch =
                createMatchWithHost(
                        11L,
                        1L,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(7200));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.findPendingMatchIds(u)).thenReturn(List.of(10L, 11L));
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(hostedMatch));
        Mockito.when(matchDao.findMatchById(11L)).thenReturn(Optional.of(otherMatch));

        // Exercise
        final List<Match> matches = matchParticipationService.findPendingRequestMatches(u);

        // Assert
        Assertions.assertEquals(List.of(otherMatch), matches);
    }

    @Test
    public void testFindInvitedMatchesSkipsHostedMatches() {
        // Arrange
        final Match hostedMatch =
                createMatchWithHost(
                        10L,
                        20L,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(3600));
        final Match otherMatch =
                createMatchWithHost(
                        11L,
                        1L,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(7200));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.findInvitedMatchIds(u)).thenReturn(List.of(10L, 11L));
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(hostedMatch));
        Mockito.when(matchDao.findMatchById(11L)).thenReturn(Optional.of(otherMatch));

        // Exercise
        final List<Match> matches = matchParticipationService.findInvitedMatches(u);

        // Assert
        Assertions.assertEquals(List.of(otherMatch), matches);
    }

    @Test
    public void testRequestToJoinSeriesSucceedsForFutureApprovalRequiredOccurrences() {
        // Arrange
        final Match selectedOccurrence =
                createRecurringMatch(
                        10L,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(3600),
                        4,
                        1,
                        100L,
                        1);
        final Match secondOccurrence =
                createRecurringMatch(
                        11L,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(7200),
                        4,
                        0,
                        100L,
                        2);
        final Match pastOccurrence =
                createRecurringMatch(
                        12L,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        FIXED_NOW.minusSeconds(60),
                        4,
                        0,
                        100L,
                        0);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDao.findSeriesOccurrences(100L))
                .thenReturn(List.of(pastOccurrence, selectedOccurrence, secondOccurrence));

        final User u = UserUtils.getUser(20L);
        Mockito.when(
                        matchParticipantDao.findActiveFutureReservationMatchIdsForSeries(
                                100L, u, FIXED_NOW))
                .thenReturn(List.of());
        Mockito.when(
                        matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                                100L, u, FIXED_NOW))
                .thenReturn(List.of());
        Mockito.when(matchParticipantDao.createSeriesJoinRequestIfSpace(10L, u)).thenReturn(true);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(() -> matchParticipationService.requestToJoinSeries(10L, u));
    }

    @Test
    public void testApproveRequestExpandsSeriesJoinRequest() {
        // Arrange
        final Match selectedOccurrence =
                createRecurringMatch(
                        10L,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(3600),
                        4,
                        4,
                        100L,
                        1);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.isSeriesJoinRequest(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDao.approveSeriesJoinRequest(100L, u, FIXED_NOW))
                .thenReturn(2);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(
                () -> matchParticipationService.approveRequest(10L, UserUtils.getUser(1L), u));
    }

    @Test
    public void testApproveSeriesJoinRequestUsesFutureSeriesWhenAnchorIsClosed() {
        // Arrange
        final Match staleOccurrence =
                createRecurringMatch(
                        10L,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.CANCELLED,
                        FIXED_NOW.minusSeconds(60),
                        4,
                        0,
                        100L,
                        1);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(staleOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.isSeriesJoinRequest(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDao.approveSeriesJoinRequest(100L, u, FIXED_NOW))
                .thenReturn(1);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(
                () -> matchParticipationService.approveRequest(10L, UserUtils.getUser(1L), u));
    }

    @Test
    public void testInviteUserToSeriesInvitesEligibleDatesOnlyAndSendsOneMail() {
        // Arrange
        final Match selectedOccurrence =
                createRecurringMatch(
                        10L,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(3600),
                        4,
                        1,
                        100L,
                        1);
        final Match secondOccurrence =
                createRecurringMatch(
                        11L,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(7200),
                        4,
                        0,
                        100L,
                        2);
        final Match alreadyInvitedOccurrence =
                createRecurringMatch(
                        12L,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(10800),
                        4,
                        1,
                        100L,
                        3);
        final Match alreadyJoinedOccurrence =
                createRecurringMatch(
                        13L,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(14400),
                        4,
                        1,
                        100L,
                        4);
        final Match fullOccurrence =
                createRecurringMatch(
                        14L,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(18000),
                        4,
                        4,
                        100L,
                        5);
        final Match publicOccurrence =
                createRecurringMatch(
                        15L,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(21600),
                        4,
                        0,
                        100L,
                        6);
        final List<Long> invitedMatchIds = new ArrayList<>();
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDao.findSeriesOccurrences(100L))
                .thenReturn(
                        List.of(
                                selectedOccurrence,
                                secondOccurrence,
                                alreadyInvitedOccurrence,
                                alreadyJoinedOccurrence,
                                fullOccurrence,
                                publicOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(userService.findByEmail("player@test.com")).thenReturn(Optional.of(u));
        Mockito.when(matchParticipantDao.hasInvitation(Mockito.anyLong(), Mockito.eq(u)))
                .thenAnswer(invocation -> Long.valueOf(12L).equals(invocation.getArgument(0)));
        Mockito.when(matchParticipantDao.hasActiveReservation(Mockito.anyLong(), Mockito.eq(u)))
                .thenAnswer(invocation -> Long.valueOf(13L).equals(invocation.getArgument(0)));
        Mockito.when(
                        matchParticipantDao.inviteUser(
                                Mockito.anyLong(), Mockito.eq(u), Mockito.eq(true)))
                .thenAnswer(
                        invocation -> {
                            invitedMatchIds.add(invocation.getArgument(0));
                            return true;
                        });
        // Exercise
        matchParticipationService.inviteUser(10L, UserUtils.getUser(1L), "player@test.com", true);

        // Assert
        Assertions.assertEquals(List.of(10L, 11L), invitedMatchIds);
        Assertions.assertEquals(1, mailDispatchService.actions.size());
        Assertions.assertEquals("series-invitation", mailDispatchService.actions.get(0));
        Assertions.assertEquals(u.getEmail(), mailDispatchService.recipients.get(0));
        Assertions.assertEquals(2, mailDispatchService.occurrenceCounts.get(0));
    }

    @Test
    public void testInviteUserSendsInvitationToTarget() {
        final Match match =
                createMatch(
                        10L,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(3600));
        final User target =
                new User(
                        20L,
                        "player@test.com",
                        "player",
                        "Jamie",
                        "Rivera",
                        null,
                        null,
                        UserLanguages.SPANISH);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(match));
        Mockito.when(userService.findByEmail("player@test.com")).thenReturn(Optional.of(target));
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, target)).thenReturn(false);
        Mockito.when(matchParticipantDao.hasInvitation(10L, target)).thenReturn(false);
        Mockito.when(matchParticipantDao.inviteUser(10L, target)).thenReturn(true);

        matchParticipationService.inviteUser(10L, UserUtils.getUser(1L), "player@test.com");

        Assertions.assertEquals(List.of("match-invitation"), mailDispatchService.actions);
        Assertions.assertEquals(List.of(target.getEmail()), mailDispatchService.recipients);
    }

    @Test
    public void testInviteUserToSeriesRejectsWhenAllDatesAlreadyCovered() {
        // Arrange
        final Match selectedOccurrence =
                createRecurringMatch(
                        10L,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(3600),
                        4,
                        1,
                        100L,
                        1);
        final Match alreadyJoinedOccurrence =
                createRecurringMatch(
                        11L,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(7200),
                        4,
                        1,
                        100L,
                        2);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDao.findSeriesOccurrences(100L))
                .thenReturn(List.of(selectedOccurrence, alreadyJoinedOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(userService.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        Mockito.when(matchParticipantDao.hasInvitation(Mockito.anyLong(), Mockito.eq(u)))
                .thenAnswer(invocation -> Long.valueOf(10L).equals(invocation.getArgument(0)));
        Mockito.when(matchParticipantDao.hasActiveReservation(Mockito.anyLong(), Mockito.eq(u)))
                .thenAnswer(invocation -> Long.valueOf(11L).equals(invocation.getArgument(0)));

        // Exercise + Assert
        Assertions.assertThrows(
                MatchParticipationSeriesAlreadyCoveredException.class,
                () ->
                        matchParticipationService.inviteUser(
                                10L, UserUtils.getUser(1L), u.getEmail(), true));
        Assertions.assertTrue(mailDispatchService.actions.isEmpty());
    }

    @Test
    public void testAcceptInviteExpandsSeriesInvitation() {
        // Arrange
        final Match selectedOccurrence =
                createRecurringMatch(
                        10L,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(3600),
                        4,
                        1,
                        100L,
                        1);
        final AtomicInteger acceptedRows = new AtomicInteger();
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.hasInvitation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDao.isSeriesInvitation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDao.acceptSeriesInvite(100L, u, FIXED_NOW))
                .thenAnswer(
                        invocation -> {
                            acceptedRows.set(2);
                            return 2;
                        });

        // Exercise
        matchParticipationService.acceptInvite(10L, u);

        // Assert
        Assertions.assertEquals(2, acceptedRows.get());
    }

    @Test
    public void testAcceptSeriesInviteUsesFutureSeriesWhenAnchorIsClosed() {
        // Arrange
        final Match staleOccurrence =
                createRecurringMatch(
                        10L,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.CANCELLED,
                        FIXED_NOW.minusSeconds(60),
                        4,
                        0,
                        100L,
                        1);
        final AtomicInteger acceptedRows = new AtomicInteger();
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(staleOccurrence));
        Mockito.when(matchParticipantDao.hasInvitation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDao.isSeriesInvitation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDao.acceptSeriesInvite(100L, u, FIXED_NOW))
                .thenAnswer(
                        invocation -> {
                            acceptedRows.set(1);
                            return 1;
                        });

        // Exercise
        matchParticipationService.acceptInvite(10L, u);

        // Assert
        Assertions.assertEquals(1, acceptedRows.get());
    }

    @Test
    public void testDeclineInviteExpandsSeriesInvitation() {
        // Arrange
        final Match selectedOccurrence =
                createRecurringMatch(
                        10L,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(3600),
                        4,
                        1,
                        100L,
                        1);
        final AtomicInteger declinedRows = new AtomicInteger();
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDao.hasInvitation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDao.isSeriesInvitation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDao.declineSeriesInvite(100L, u))
                .thenAnswer(
                        invocation -> {
                            declinedRows.set(2);
                            return 2;
                        });

        // Exercise
        matchParticipationService.declineInvite(10L, u);

        // Assert
        Assertions.assertEquals(2, declinedRows.get());
    }

    @Test
    public void testRequestToJoinSeriesRejectsAlreadyPendingFutureSeries() {
        // Arrange
        final Match selectedOccurrence =
                createRecurringMatch(
                        10L,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(3600),
                        4,
                        1,
                        100L,
                        1);
        final Match secondOccurrence =
                createRecurringMatch(
                        11L,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(7200),
                        4,
                        0,
                        100L,
                        2);
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDao.findSeriesOccurrences(100L))
                .thenReturn(List.of(selectedOccurrence, secondOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(
                        matchParticipantDao.findActiveFutureReservationMatchIdsForSeries(
                                100L, u, FIXED_NOW))
                .thenReturn(List.of());
        Mockito.when(
                        matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                                100L, u, FIXED_NOW))
                .thenReturn(List.of(10L, 11L));

        // Exercise + Assert
        Assertions.assertThrows(
                MatchParticipationSeriesAlreadyPendingException.class,
                () -> matchParticipationService.requestToJoinSeries(10L, u));
    }

    @Test
    public void testRequestToJoinSeriesRejectsNonRecurringMatch() {
        // Arrange
        Mockito.when(matchDao.findMatchById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.APPROVAL_REQUIRED,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));

        // Exercise + Assert
        Assertions.assertThrows(
                MatchParticipationNotRecurringException.class,
                () -> matchParticipationService.requestToJoinSeries(10L, UserUtils.getUser(20L)));
    }

    @Test
    public void testRequestToJoinFailsIfMatchClosed() {
        final Match match =
                createMatch(
                        10L,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.CANCELLED,
                        NOW.plusSeconds(3600));
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(match));

        Assertions.assertThrows(
                MatchParticipationClosedException.class,
                () -> matchParticipationService.requestToJoin(10L, UserUtils.getUser(2L)));
    }

    @Test
    public void testRequestToJoinFailsIfMatchFull() {
        Match match =
                MatchUtils.createMatchWithId(10L, 1L, Sport.FOOTBALL, NOW.plusSeconds(3600), 4);
        match.setJoinedPlayers(4);
        match.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);

        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(match));
        final User u = UserUtils.getUser(2L);
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, u)).thenReturn(false);
        Mockito.when(matchParticipantDao.hasPendingRequest(10L, u)).thenReturn(false);

        Assertions.assertThrows(
                MatchParticipationFullException.class,
                () -> matchParticipationService.requestToJoin(10L, u));
    }

    @Test
    public void testApproveRequestFailsIfNotHost() {
        final Match match =
                createMatch(
                        10L,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        NOW.plusSeconds(3600));
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(match));

        Assertions.assertThrows(
                MatchParticipationForbiddenException.class,
                () ->
                        matchParticipationService.approveRequest(
                                10L, UserUtils.getUser(3L), UserUtils.getUser(2L)));
    }

    private static class RecordingMailDispatchService implements MailDispatchService {

        private final List<String> recipients = new ArrayList<>();
        private final List<String> actions = new ArrayList<>();
        private final List<Integer> occurrenceCounts = new ArrayList<>();

        @Override
        public void sendMatchInvitation(final User recipient, final Match match) {
            actions.add("match-invitation");
            recipients.add(recipient.getEmail());
        }

        @Override
        public void sendSeriesInvitation(
                final User recipient, final Match match, final int occurrenceCount) {
            actions.add("series-invitation");
            recipients.add(recipient.getEmail());
            occurrenceCounts.add(occurrenceCount);
        }

        @Override
        public void sendPlayerLeft(final User host, final Match match, final User player) {
            actions.add("player-left");
            recipients.add(host.getEmail());
        }

        @Override
        public void sendPlayerRemoved(final User player, final Match match) {
            actions.add("player-removed");
            recipients.add(player.getEmail());
        }
    }

    private static Match createMatch(
            final Long id,
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy,
            final EventStatus status,
            final Instant startsAt) {
        return createMatchWithHost(id, 1L, visibility, joinPolicy, status, startsAt);
    }

    private static Match createMatchWithHost(
            final Long id,
            final Long hostUserId,
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy,
            final EventStatus status,
            final Instant startsAt) {
        Match m = MatchUtils.createMatchWithId(id, hostUserId, Sport.FOOTBALL, startsAt, 4);
        m.setVisibility(visibility);
        m.setJoinPolicy(joinPolicy);
        m.setStatus(status);
        return m;
    }

    private static Match createRecurringMatch(
            final Long id,
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy,
            final EventStatus status,
            final Instant startsAt,
            final int maxPlayers,
            final int joinedPlayers,
            final Long seriesId,
            final int occurrenceIndex) {

        MatchSeries series = MatchUtils.getMatchSeries(seriesId, UserUtils.getUser(1L));
        Match m = createMatchWithHost(id, 1L, visibility, joinPolicy, status, startsAt);
        m.setMaxPlayers(maxPlayers);
        m.setSeries(series);
        m.setJoinedPlayers(joinedPlayers);
        m.setSeriesOccurrenceIndex(occurrenceIndex);

        return m;
    }
}
