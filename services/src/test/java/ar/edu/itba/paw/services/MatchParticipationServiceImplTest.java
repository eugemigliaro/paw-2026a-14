package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EventJoinPolicy;
import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.EventVisibility;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.MatchParticipationException;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;

@ExtendWith(MockitoExtension.class)
public class MatchParticipationServiceImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T18:00:00Z");

    @Mock private MatchDao matchDao;
    @Mock private MatchParticipantDao matchParticipantDao;
    @Mock private UserService userService;
    @Mock private ThymeleafMailTemplateRenderer templateRenderer;

    private RecordingMailDispatchService mailDispatchService;
    private MatchParticipationServiceImpl matchParticipationService;

    private static final Instant NOW = Instant.parse("2026-04-20T10:00:00Z");

    @BeforeEach
    public void setUp() {
        mailDispatchService = new RecordingMailDispatchService();
        final StaticMessageSource messageSource = new StaticMessageSource();
        final MatchNotificationService matchNotificationService =
                new MatchNotificationServiceImpl(
                        matchParticipantDao,
                        mailDispatchService,
                        templateRenderer,
                        messageSource,
                        userService);
        matchParticipationService =
                new MatchParticipationServiceImpl(
                        matchDao,
                        matchParticipantDao,
                        userService,
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                        mailDispatchService,
                        templateRenderer,
                        messageSource,
                        matchNotificationService);
    }

    @Test
    public void testFindPendingFutureRequestMatchIdsForSeriesUsesCurrentTime() {
        // Arrange
        Mockito.when(
                        matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                                100L, 20L, FIXED_NOW))
                .thenReturn(List.of(10L, 11L));

        // Exercise
        final Set<Long> matchIds =
                matchParticipationService.findPendingFutureRequestMatchIdsForSeries(100L, 20L);

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

        // Exercise
        final MatchParticipationException exception =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () -> matchParticipationService.requestToJoin(10L, 1L));

        // Assert
        Assertions.assertEquals("is_host", exception.getCode());
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

        // Exercise
        final MatchParticipationException exception =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () -> matchParticipationService.requestToJoinSeries(10L, 1L));

        // Assert
        Assertions.assertEquals("is_host", exception.getCode());
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
        Mockito.when(userService.findByEmail("host@test.com"))
                .thenReturn(Optional.of(new User(1L, "host@test.com", "host-player")));

        // Exercise
        final MatchParticipationException exception =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () -> matchParticipationService.inviteUser(10L, 1L, "host@test.com"));

        // Assert
        Assertions.assertEquals("is_host", exception.getCode());
        Assertions.assertTrue(mailDispatchService.contents.isEmpty());
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

        // Exercise
        final MatchParticipationException exception =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () -> matchParticipationService.acceptInvite(10L, 1L));

        // Assert
        Assertions.assertEquals("is_host", exception.getCode());
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
                                        EventVisibility.PRIVATE,
                                        EventJoinPolicy.INVITE_ONLY,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.removeParticipant(10L, 20L)).thenReturn(true);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(
                () -> matchParticipationService.removeParticipant(10L, 20L, 20L));
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
        final User player =
                new User(20L, "player@test.com", "player-account", "Jamie", "Rivera", null, null);
        final User host = new User(1L, "host@test.com", "host-account", "Host", "User", null, null);
        final MailContent mail = new MailContent("player-left", "<p>left</p>", "left");
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.removeParticipant(10L, 20L)).thenReturn(true);
        Mockito.when(userService.findById(20L)).thenReturn(Optional.of(player));
        Mockito.when(userService.findById(1L)).thenReturn(Optional.of(host));
        Mockito.when(templateRenderer.renderPlayerLeftNotification(Mockito.any())).thenReturn(mail);

        // Exercise
        matchParticipationService.removeParticipant(10L, 20L, 20L);

        // Assert
        Assertions.assertEquals(List.of("host@test.com"), mailDispatchService.recipients);
        Assertions.assertEquals(List.of(mail), mailDispatchService.contents);
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
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.DIRECT,
                                        EventStatus.OPEN,
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
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.APPROVAL_REQUIRED,
                                        EventStatus.OPEN,
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
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.APPROVAL_REQUIRED,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));
        Mockito.when(matchParticipantDao.removeParticipant(10L, 30L)).thenReturn(true);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(
                () -> matchParticipationService.removeParticipant(10L, 1L, 30L));
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
        final User player =
                new User(30L, "player@test.com", "player-account", "Alex", "Morgan", null, null);
        final MailContent mail = new MailContent("removed", "<p>removed</p>", "removed");
        Mockito.when(matchParticipantDao.removeParticipant(10L, 30L)).thenReturn(true);
        Mockito.when(userService.findById(30L)).thenReturn(Optional.of(player));
        Mockito.when(templateRenderer.renderParticipantRemovedNotification(Mockito.any()))
                .thenReturn(mail);

        // Exercise
        matchParticipationService.removeParticipant(10L, 1L, 30L);

        // Assert
        Assertions.assertEquals(List.of("player@test.com"), mailDispatchService.recipients);
        Assertions.assertEquals(List.of(mail), mailDispatchService.contents);
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
        Mockito.when(matchParticipantDao.findPendingMatchIds(20L)).thenReturn(List.of(10L, 11L));
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(hostedMatch));
        Mockito.when(matchDao.findMatchById(11L)).thenReturn(Optional.of(otherMatch));

        // Exercise
        final List<Match> matches = matchParticipationService.findPendingRequestMatches(20L);

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
        Mockito.when(matchParticipantDao.findInvitedMatchIds(20L)).thenReturn(List.of(10L, 11L));
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(hostedMatch));
        Mockito.when(matchDao.findMatchById(11L)).thenReturn(Optional.of(otherMatch));

        // Exercise
        final List<Match> matches = matchParticipationService.findInvitedMatches(20L);

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
        Mockito.when(
                        matchParticipantDao.findActiveFutureReservationMatchIdsForSeries(
                                100L, 20L, FIXED_NOW))
                .thenReturn(List.of());
        Mockito.when(
                        matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                                100L, 20L, FIXED_NOW))
                .thenReturn(List.of());
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
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
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
        Mockito.when(matchParticipantDao.isSeriesJoinRequest(10L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.approveSeriesJoinRequest(100L, 20L, FIXED_NOW))
                .thenReturn(1);

        // Exercise and Assert
        Assertions.assertDoesNotThrow(() -> matchParticipationService.approveRequest(10L, 1L, 20L));
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
        final AtomicInteger mailOccurrenceCount = new AtomicInteger();
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
        Mockito.when(userService.findByEmail("player@test.com"))
                .thenReturn(Optional.of(new User(20L, "player@test.com", "player")));
        Mockito.when(matchParticipantDao.hasInvitation(Mockito.anyLong(), Mockito.eq(20L)))
                .thenAnswer(invocation -> Long.valueOf(12L).equals(invocation.getArgument(0)));
        Mockito.when(matchParticipantDao.hasActiveReservation(Mockito.anyLong(), Mockito.eq(20L)))
                .thenAnswer(invocation -> Long.valueOf(13L).equals(invocation.getArgument(0)));
        Mockito.when(
                        matchParticipantDao.inviteUser(
                                Mockito.anyLong(), Mockito.eq(20L), Mockito.eq(true)))
                .thenAnswer(
                        invocation -> {
                            invitedMatchIds.add(invocation.getArgument(0));
                            return true;
                        });
        Mockito.when(
                        templateRenderer.renderSeriesInvitationNotification(
                                Mockito.any(), Mockito.anyInt()))
                .thenAnswer(
                        invocation -> {
                            mailOccurrenceCount.set(invocation.getArgument(1, Integer.class));
                            return new MailContent("Series invitation", "<p>series</p>", "series");
                        });

        // Exercise
        matchParticipationService.inviteUser(10L, 1L, "player@test.com", true);

        // Assert
        Assertions.assertEquals(List.of(10L, 11L), invitedMatchIds);
        Assertions.assertEquals(1, mailDispatchService.contents.size());
        Assertions.assertEquals("player@test.com", mailDispatchService.recipients.get(0));
        Assertions.assertEquals(
                "Series invitation", mailDispatchService.contents.get(0).getSubject());
        Assertions.assertEquals(2, mailOccurrenceCount.get());
    }

    @Test
    public void testInviteUserUsesRecipientPreferredLanguageForMail() {
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
        final AtomicReference<java.util.Locale> capturedLocale = new AtomicReference<>();
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(match));
        Mockito.when(userService.findByEmail("player@test.com")).thenReturn(Optional.of(target));
        Mockito.when(matchParticipantDao.hasActiveReservation(10L, 20L)).thenReturn(false);
        Mockito.when(matchParticipantDao.hasInvitation(10L, 20L)).thenReturn(false);
        Mockito.when(matchParticipantDao.inviteUser(10L, 20L)).thenReturn(true);
        Mockito.when(templateRenderer.renderMatchInvitationNotification(Mockito.any()))
                .thenAnswer(
                        invocation -> {
                            capturedLocale.set(
                                    invocation
                                            .<ar.edu.itba.paw.services.mail
                                                            .MatchLifecycleMailTemplateData>
                                                    getArgument(0)
                                            .getLocale());
                            return new MailContent("invitation", "<p>invite</p>", "invite");
                        });

        matchParticipationService.inviteUser(10L, 1L, "player@test.com");

        Assertions.assertEquals(java.util.Locale.of("es"), capturedLocale.get());
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
        Mockito.when(userService.findByEmail("player@test.com"))
                .thenReturn(Optional.of(new User(20L, "player@test.com", "player")));
        Mockito.when(matchParticipantDao.hasInvitation(Mockito.anyLong(), Mockito.eq(20L)))
                .thenAnswer(invocation -> Long.valueOf(10L).equals(invocation.getArgument(0)));
        Mockito.when(matchParticipantDao.hasActiveReservation(Mockito.anyLong(), Mockito.eq(20L)))
                .thenAnswer(invocation -> Long.valueOf(11L).equals(invocation.getArgument(0)));

        // Exercise
        final MatchParticipationException exception =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () ->
                                matchParticipationService.inviteUser(
                                        10L, 1L, "player@test.com", true));

        // Assert
        Assertions.assertEquals("series_already_covered", exception.getCode());
        Assertions.assertTrue(mailDispatchService.contents.isEmpty());
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
        Mockito.when(matchParticipantDao.hasInvitation(10L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.isSeriesInvitation(10L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.acceptSeriesInvite(100L, 20L, FIXED_NOW))
                .thenAnswer(
                        invocation -> {
                            acceptedRows.set(2);
                            return 2;
                        });

        // Exercise
        matchParticipationService.acceptInvite(10L, 20L);

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
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(staleOccurrence));
        Mockito.when(matchParticipantDao.hasInvitation(10L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.isSeriesInvitation(10L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.acceptSeriesInvite(100L, 20L, FIXED_NOW))
                .thenAnswer(
                        invocation -> {
                            acceptedRows.set(1);
                            return 1;
                        });

        // Exercise
        matchParticipationService.acceptInvite(10L, 20L);

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
        Mockito.when(matchParticipantDao.hasInvitation(10L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.isSeriesInvitation(10L, 20L)).thenReturn(true);
        Mockito.when(matchParticipantDao.declineSeriesInvite(100L, 20L))
                .thenAnswer(
                        invocation -> {
                            declinedRows.set(2);
                            return 2;
                        });

        // Exercise
        matchParticipationService.declineInvite(10L, 20L);

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
        Mockito.when(
                        matchParticipantDao.findActiveFutureReservationMatchIdsForSeries(
                                100L, 20L, FIXED_NOW))
                .thenReturn(List.of());
        Mockito.when(
                        matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                                100L, 20L, FIXED_NOW))
                .thenReturn(List.of(10L, 11L));

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
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.APPROVAL_REQUIRED,
                                        EventStatus.OPEN,
                                        FIXED_NOW.plusSeconds(3600))));

        // Exercise
        final MatchParticipationException exception =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () -> matchParticipationService.requestToJoinSeries(10L, 20L));

        // Assert
        Assertions.assertEquals("not_recurring", exception.getCode());
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

        final MatchParticipationException ex =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () -> matchParticipationService.requestToJoin(10L, 2L));
        Assertions.assertEquals("closed", ex.getCode());
    }

    @Test
    public void testRequestToJoinFailsIfMatchFull() {
        final Match match =
                new Match(
                        10L,
                        Sport.FOOTBALL,
                        1L,
                        "Test Address",
                        "Test Match",
                        "Test Description",
                        NOW.plusSeconds(3600),
                        null,
                        4,
                        BigDecimal.ZERO,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        4,
                        null);
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
    public void testApproveRequestFailsIfNotHost() {
        final Match match =
                createMatch(
                        10L,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        NOW.plusSeconds(3600));
        Mockito.when(matchDao.findMatchById(10L)).thenReturn(Optional.of(match));

        final MatchParticipationException ex =
                Assertions.assertThrows(
                        MatchParticipationException.class,
                        () -> matchParticipationService.approveRequest(10L, 3L, 2L));
        Assertions.assertEquals("forbidden", ex.getCode());
    }

    private static class RecordingMailDispatchService implements MailDispatchService {

        private final List<String> recipients = new ArrayList<>();
        private final List<MailContent> contents = new ArrayList<>();

        @Override
        public void dispatch(final String recipientEmail, final MailContent content) {
            recipients.add(recipientEmail);
            contents.add(content);
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
        return new Match(
                id,
                Sport.FOOTBALL,
                hostUserId,
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
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy,
            final EventStatus status,
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
