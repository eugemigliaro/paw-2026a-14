package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSeries;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.exceptions.match.*;
import ar.edu.itba.paw.models.exceptions.matchParticipation.*;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.services.internal.MatchDataService;
import ar.edu.itba.paw.services.internal.MatchParticipantDataService;
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

    @Mock private MatchDataService matchDataService;
    @Mock private MatchParticipantDataService matchParticipantDataService;
    @Mock private UserService userService;

    private RecordingMailDispatchService mailDispatchService;
    private MatchParticipationServiceImpl matchParticipationService;

    private static final Instant NOW = Instant.parse("2026-04-20T10:00:00Z");

    @BeforeEach
    public void setUp() {
        mailDispatchService = new RecordingMailDispatchService();
        final MatchNotificationService matchNotificationService =
                new MatchNotificationServiceImpl(matchParticipantDataService, mailDispatchService);
        matchParticipationService =
                new MatchParticipationServiceImpl(
                        matchDataService,
                        matchParticipantDataService,
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
                        matchParticipantDataService.findPendingFutureRequestMatchIdsForSeries(
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
        Mockito.when(matchDataService.findById(10L))
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
        Mockito.when(matchDataService.findById(10L)).thenReturn(Optional.of(selectedOccurrence));

        // Exercise + Assert
        Assertions.assertThrows(
                MatchParticipationIsHostException.class,
                () -> matchParticipationService.requestToJoinSeries(10L, UserUtils.getUser(1L)));
    }

    @Test
    public void testInviteUserRejectsHostSelfInvite() {
        // Arrange
        Mockito.when(matchDataService.findById(10L))
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
        Mockito.when(matchDataService.findById(10L))
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
        Mockito.when(matchDataService.findById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.DIRECT,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDataService.hasActiveReservation(10L, u)).thenReturn(true);

        Mockito.when(matchParticipantDataService.removeParticipant(10L, u)).thenReturn(true);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(() -> matchParticipationService.removeParticipant(10L, u, u));
    }

    @Test
    public void testRemoveParticipantAllowsSelfLeaveForPrivateInviteOnlyMatch() {
        // Arrange
        Mockito.when(matchDataService.findById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PRIVATE,
                                        EventJoinPolicy.INVITE_ONLY,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDataService.hasActiveReservation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDataService.removeParticipant(10L, u)).thenReturn(true);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(() -> matchParticipationService.removeParticipant(10L, u, u));
    }

    @Test
    public void testRemoveParticipantNotifiesHostWhenPlayerLeaves() {
        // Arrange
        Mockito.when(matchDataService.findById(10L))
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
        Mockito.when(matchParticipantDataService.hasActiveReservation(10L, player))
                .thenReturn(true);
        Mockito.when(matchParticipantDataService.removeParticipant(10L, player)).thenReturn(true);

        // Exercise
        matchParticipationService.removeParticipant(10L, player, player);

        // Assert
        Assertions.assertEquals(List.of(host.getEmail()), mailDispatchService.recipients);
        Assertions.assertEquals(List.of("player-left"), mailDispatchService.actions);
    }

    @Test
    public void testRemoveParticipantRejectsSelfLeaveForStartedMatch() {
        // Arrange
        Mockito.when(matchDataService.findById(10L))
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
                MatchStartedException.class,
                () ->
                        matchParticipationService.removeParticipant(
                                10L, UserUtils.getUser(20L), UserUtils.getUser(20L)));
    }

    @Test
    public void testRemoveParticipantRejectsSelfLeaveWithoutActiveReservation() {
        // Arrange
        Mockito.when(matchDataService.findById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.DIRECT,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));

        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDataService.hasActiveReservation(10L, u)).thenReturn(false);

        // Exercise + Assert
        Assertions.assertThrows(
                MatchParticipationNotJoinedException.class,
                () -> matchParticipationService.removeParticipant(10L, u, u));
    }

    @Test
    public void testRemoveParticipantStillRequiresHostWhenRemovingAnotherUser() {
        // Arrange
        Mockito.when(matchDataService.findById(10L))
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
                MatchForbiddenActionException.class,
                () ->
                        matchParticipationService.removeParticipant(
                                10L, UserUtils.getUser(20L), UserUtils.getUser(30L)));
    }

    @Test
    public void testRemoveParticipantAllowsHostToRemoveAnotherUser() {
        // Arrange
        Mockito.when(matchDataService.findById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.APPROVAL_REQUIRED,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));
        final User u = UserUtils.getUser(30L);
        Mockito.when(matchParticipantDataService.removeParticipant(10L, u)).thenReturn(true);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(
                () -> matchParticipationService.removeParticipant(10L, UserUtils.getUser(1L), u));
    }

    @Test
    public void testRemoveParticipantNotifiesPlayerWhenHostKicksThem() {
        // Arrange
        Mockito.when(matchDataService.findById(10L))
                .thenReturn(
                        Optional.of(
                                createMatch(
                                        10L,
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.APPROVAL_REQUIRED,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));
        final User player = UserUtils.getUser(30L);
        Mockito.when(matchParticipantDataService.removeParticipant(10L, player)).thenReturn(true);

        // Exercise
        matchParticipationService.removeParticipant(10L, UserUtils.getUser(1L), player);

        // Assert
        Assertions.assertEquals(List.of(player.getEmail()), mailDispatchService.recipients);
        Assertions.assertEquals(List.of("player-removed"), mailDispatchService.actions);
    }

    @Test
    public void testFindPendingRequestMatchesDelegatesToDao() {
        // Arrange
        final Match otherMatch =
                createMatchWithHost(
                        11L,
                        1L,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(7200));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDataService.findPendingRequestMatches(u))
                .thenReturn(List.of(otherMatch));

        // Exercise
        final List<Match> matches = matchParticipationService.findPendingRequestMatches(u);

        // Assert
        Assertions.assertEquals(List.of(otherMatch), matches);
    }

    @Test
    public void testFindInvitedMatchesDelegatesToDao() {
        // Arrange
        final Match otherMatch =
                createMatchWithHost(
                        11L,
                        1L,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(7200));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDataService.findInvitedMatches(u))
                .thenReturn(List.of(otherMatch));

        // Exercise
        final List<Match> matches = matchParticipationService.findInvitedMatches(u);

        // Assert
        Assertions.assertEquals(List.of(otherMatch), matches);
    }

    @Test
    public void testFindInvitedUsersAllowsPrivateInviteOnlyMatch() {
        // Arrange
        final Match match =
                createMatch(
                        10L,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        FIXED_NOW.plusSeconds(3600));
        final User invitedUser = UserUtils.getUser(20L);
        Mockito.when(matchDataService.findById(10L)).thenReturn(Optional.of(match));
        Mockito.when(matchParticipantDataService.findInvitedUsers(10L))
                .thenReturn(List.of(invitedUser));

        // Exercise
        final List<User> invitedUsers =
                matchParticipationService.findInvitedUsers(10L, UserUtils.getUser(1L));

        // Assert
        Assertions.assertEquals(List.of(invitedUser), invitedUsers);
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
        Mockito.when(matchDataService.findById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDataService.findSeriesOccurrences(100L))
                .thenReturn(List.of(pastOccurrence, selectedOccurrence, secondOccurrence));

        final User u = UserUtils.getUser(20L);
        Mockito.when(
                        matchParticipantDataService.findActiveFutureReservationMatchIdsForSeries(
                                100L, u, FIXED_NOW))
                .thenReturn(List.of());
        Mockito.when(
                        matchParticipantDataService.findPendingFutureRequestMatchIdsForSeries(
                                100L, u, FIXED_NOW))
                .thenReturn(List.of());
        Mockito.when(matchParticipantDataService.createSeriesJoinRequestIfSpace(10L, u))
                .thenReturn(true);

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
        Mockito.when(matchDataService.findById(10L)).thenReturn(Optional.of(selectedOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDataService.isSeriesJoinRequest(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDataService.approveSeriesJoinRequest(100L, u, FIXED_NOW))
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
        Mockito.when(matchDataService.findById(10L)).thenReturn(Optional.of(staleOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDataService.isSeriesJoinRequest(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDataService.approveSeriesJoinRequest(100L, u, FIXED_NOW))
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
        Mockito.when(matchDataService.findById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDataService.findSeriesOccurrences(100L))
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
        Mockito.when(matchParticipantDataService.hasInvitation(Mockito.anyLong(), Mockito.eq(u)))
                .thenAnswer(invocation -> Long.valueOf(12L).equals(invocation.getArgument(0)));
        Mockito.when(
                        matchParticipantDataService.hasActiveReservation(
                                Mockito.anyLong(), Mockito.eq(u)))
                .thenAnswer(invocation -> Long.valueOf(13L).equals(invocation.getArgument(0)));
        Mockito.when(
                        matchParticipantDataService.inviteUser(
                                Mockito.anyLong(), Mockito.eq(u), Mockito.eq(true)))
                .thenAnswer(
                        invocation -> {
                            final Long targetMatchId = invocation.getArgument(0);
                            if (!List.of(10L, 11L).contains(targetMatchId)) {
                                throw new AssertionError(
                                        "Only eligible future private occurrences should be invited");
                            }
                            return true;
                        });
        // Exercise
        matchParticipationService.inviteUser(10L, UserUtils.getUser(1L), "player@test.com", true);

        // Assert
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
        Mockito.when(matchDataService.findById(10L)).thenReturn(Optional.of(match));
        Mockito.when(userService.findByEmail("player@test.com")).thenReturn(Optional.of(target));
        Mockito.when(matchParticipantDataService.hasActiveReservation(10L, target))
                .thenReturn(false);
        Mockito.when(matchParticipantDataService.hasInvitation(10L, target)).thenReturn(false);
        Mockito.when(matchParticipantDataService.inviteUser(10L, target)).thenReturn(true);

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
        Mockito.when(matchDataService.findById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDataService.findSeriesOccurrences(100L))
                .thenReturn(List.of(selectedOccurrence, alreadyJoinedOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(userService.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        Mockito.when(matchParticipantDataService.hasInvitation(Mockito.anyLong(), Mockito.eq(u)))
                .thenAnswer(invocation -> Long.valueOf(10L).equals(invocation.getArgument(0)));
        Mockito.when(
                        matchParticipantDataService.hasActiveReservation(
                                Mockito.anyLong(), Mockito.eq(u)))
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
        Mockito.when(matchDataService.findById(10L)).thenReturn(Optional.of(selectedOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDataService.hasInvitation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDataService.isSeriesInvitation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDataService.acceptSeriesInvite(100L, u, FIXED_NOW))
                .thenReturn(2);

        // Exercise
        matchParticipationService.acceptInvite(10L, u);

        // Assert
        Assertions.assertEquals(List.of("invite-accepted"), mailDispatchService.actions);
        Assertions.assertEquals(
                List.of(selectedOccurrence.getHost().getEmail()), mailDispatchService.recipients);
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
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchDataService.findById(10L)).thenReturn(Optional.of(staleOccurrence));
        Mockito.when(matchParticipantDataService.hasInvitation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDataService.isSeriesInvitation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDataService.acceptSeriesInvite(100L, u, FIXED_NOW))
                .thenReturn(1);

        // Exercise
        matchParticipationService.acceptInvite(10L, u);

        // Assert
        Assertions.assertEquals(List.of("invite-accepted"), mailDispatchService.actions);
        Assertions.assertEquals(
                List.of(staleOccurrence.getHost().getEmail()), mailDispatchService.recipients);
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
        Mockito.when(matchDataService.findById(10L)).thenReturn(Optional.of(selectedOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(matchParticipantDataService.hasInvitation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDataService.isSeriesInvitation(10L, u)).thenReturn(true);
        Mockito.when(matchParticipantDataService.declineSeriesInvite(100L, u)).thenReturn(2);

        // Exercise
        matchParticipationService.declineInvite(10L, u);

        // Assert
        Assertions.assertEquals(List.of("invite-declined"), mailDispatchService.actions);
        Assertions.assertEquals(
                List.of(selectedOccurrence.getHost().getEmail()), mailDispatchService.recipients);
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
        Mockito.when(matchDataService.findById(10L)).thenReturn(Optional.of(selectedOccurrence));
        Mockito.when(matchDataService.findSeriesOccurrences(100L))
                .thenReturn(List.of(selectedOccurrence, secondOccurrence));
        final User u = UserUtils.getUser(20L);
        Mockito.when(
                        matchParticipantDataService.findActiveFutureReservationMatchIdsForSeries(
                                100L, u, FIXED_NOW))
                .thenReturn(List.of());
        Mockito.when(
                        matchParticipantDataService.findPendingFutureRequestMatchIdsForSeries(
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
        Mockito.when(matchDataService.findById(10L))
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
                MatchNotRecurringException.class,
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
        Mockito.when(matchDataService.findById(10L)).thenReturn(Optional.of(match));

        Assertions.assertThrows(
                MatchClosedException.class,
                () -> matchParticipationService.requestToJoin(10L, UserUtils.getUser(2L)));
    }

    @Test
    public void testRequestToJoinFailsIfMatchFull() {
        Match match =
                MatchUtils.createMatchWithId(10L, 1L, Sport.FOOTBALL, NOW.plusSeconds(3600), 4);
        match.setJoinedPlayers(4);
        match.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);

        Mockito.when(matchDataService.findById(10L)).thenReturn(Optional.of(match));
        final User u = UserUtils.getUser(2L);
        Mockito.when(matchParticipantDataService.hasActiveReservation(10L, u)).thenReturn(false);
        Mockito.when(matchParticipantDataService.hasPendingRequest(10L, u)).thenReturn(false);

        Assertions.assertThrows(
                MatchFullException.class, () -> matchParticipationService.requestToJoin(10L, u));
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
        Mockito.when(matchDataService.findById(10L)).thenReturn(Optional.of(match));

        Assertions.assertThrows(
                MatchForbiddenActionException.class,
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

        @Override
        public void sendInviteAccepted(final User host, final Match match, final User player) {
            actions.add("invite-accepted");
            recipients.add(host.getEmail());
        }

        @Override
        public void sendInviteDeclined(final User host, final Match match, final User player) {
            actions.add("invite-declined");
            recipients.add(host.getEmail());
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
