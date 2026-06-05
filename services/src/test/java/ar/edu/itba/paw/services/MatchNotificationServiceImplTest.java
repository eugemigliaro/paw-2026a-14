package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import ar.edu.itba.paw.services.utils.MatchUtils;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@ExtendWith(MockitoExtension.class)
public class MatchNotificationServiceImplTest {

    @Mock private MatchParticipantDao matchParticipantDao;
    @Mock private ThymeleafMailTemplateRenderer templateRenderer;
    @Mock private MessageSource messageSource;
    @Mock private UserService userService;

    private RecordingMailDispatchService mailDispatchService;
    private MatchNotificationServiceImpl matchNotificationService;

    @BeforeEach
    public void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        mailDispatchService = new RecordingMailDispatchService();
        matchNotificationService =
                new MatchNotificationServiceImpl(
                        matchParticipantDao, mailDispatchService, templateRenderer, messageSource);
        Mockito.lenient()
                .when(
                        messageSource.getMessage(
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));
    }

    @AfterEach
    public void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    public void testNotifyMatchUpdatedSendsOneMailPerParticipant() {
        final Match match = createMatch(40L, "Updated Match", EventStatus.OPEN);
        final List<User> participants = List.of(UserUtils.getUser(2L), UserUtils.getUser(3L));
        final MailContent firstMail = new MailContent("updated-1", "<p>1</p>", "1");
        final MailContent secondMail = new MailContent("updated-2", "<p>2</p>", "2");
        Mockito.when(matchParticipantDao.findConfirmedParticipants(40L)).thenReturn(participants);
        Mockito.when(templateRenderer.renderMatchUpdatedNotification(ArgumentMatchers.any()))
                .thenReturn(firstMail, secondMail);

        matchNotificationService.notifyMatchUpdated(match);

        Assertions.assertEquals(
                List.of(participants.get(0).getEmail(), participants.get(1).getEmail()),
                mailDispatchService.recipients);
        Assertions.assertSame(firstMail, mailDispatchService.contents.get(0));
        Assertions.assertSame(secondMail, mailDispatchService.contents.get(1));
    }

    @Test
    public void testNotifyMatchCancelledSendsOneMailPerParticipant() {
        final Match match = createMatch(41L, "Cancelled Match", EventStatus.CANCELLED);
        final List<User> participants = List.of(UserUtils.getUser(2L), UserUtils.getUser(3L));
        final MailContent firstMail = new MailContent("cancelled-1", "<p>1</p>", "1");
        final MailContent secondMail = new MailContent("cancelled-2", "<p>2</p>", "2");
        Mockito.when(matchParticipantDao.findConfirmedParticipants(41L)).thenReturn(participants);
        Mockito.when(templateRenderer.renderMatchCancelledNotification(ArgumentMatchers.any()))
                .thenReturn(firstMail, secondMail);

        matchNotificationService.notifyMatchCancelled(match);

        Assertions.assertEquals(
                List.of(participants.get(0).getEmail(), participants.get(1).getEmail()),
                mailDispatchService.recipients);
        Assertions.assertSame(firstMail, mailDispatchService.contents.get(0));
        Assertions.assertSame(secondMail, mailDispatchService.contents.get(1));
    }

    @Test
    public void testNotifyMatchUpdatedDoesNothingWhenNoParticipants() {
        final Match match = createMatch(42L, "Quiet Match", EventStatus.OPEN);
        Mockito.when(matchParticipantDao.findConfirmedParticipants(42L)).thenReturn(List.of());

        matchNotificationService.notifyMatchUpdated(match);

        Assertions.assertTrue(mailDispatchService.recipients.isEmpty());
        Assertions.assertTrue(mailDispatchService.contents.isEmpty());
    }

    @Test
    public void testNotifyRecurringMatchesUpdatedDeduplicatesAffectedParticipants() {
        // 1. Arrange
        final Match firstOccurrence =
                createRecurringMatch(50L, "Weekly Padel", EventStatus.OPEN, 600L, 1);
        final Match secondOccurrence =
                createRecurringMatch(51L, "Weekly Padel", EventStatus.OPEN, 600L, 2);
        final User firstParticipant = UserUtils.getUser(2L);
        final User secondParticipant = UserUtils.getUser(3L);
        final MailContent mail = new MailContent("series-updated", "<p>updated</p>", "updated");
        Mockito.when(matchParticipantDao.findConfirmedParticipants(50L))
                .thenReturn(List.of(firstParticipant));
        Mockito.when(matchParticipantDao.findConfirmedParticipants(51L))
                .thenReturn(List.of(firstParticipant, secondParticipant));
        Mockito.when(
                        templateRenderer.renderRecurringMatchesUpdatedNotification(
                                ArgumentMatchers.any(), ArgumentMatchers.anyInt()))
                .thenReturn(mail);

        // 2. Exercise
        matchNotificationService.notifyRecurringMatchesUpdated(
                List.of(firstOccurrence, secondOccurrence));

        // 3. Assert
        Assertions.assertEquals(
                List.of(firstParticipant.getEmail(), secondParticipant.getEmail()),
                mailDispatchService.recipients);
    }

    @Test
    public void testNotifyRecurringMatchesCancelledUsesPerRecipientAffectedDateCount() {
        // 1. Arrange
        final Match firstOccurrence =
                createRecurringMatch(52L, "Weekly Padel", EventStatus.CANCELLED, 600L, 1);
        final Match secondOccurrence =
                createRecurringMatch(53L, "Weekly Padel", EventStatus.CANCELLED, 600L, 2);
        final User firstParticipant = UserUtils.getUser(2L);
        final User secondParticipant = UserUtils.getUser(3L);
        final MailContent mail =
                new MailContent("series-cancelled", "<p>cancelled</p>", "cancelled");
        final List<Integer> affectedCounts = new ArrayList<>();
        Mockito.when(matchParticipantDao.findConfirmedParticipants(52L))
                .thenReturn(List.of(firstParticipant));
        Mockito.when(matchParticipantDao.findConfirmedParticipants(53L))
                .thenReturn(List.of(firstParticipant, secondParticipant));
        Mockito.when(
                        templateRenderer.renderRecurringMatchesCancelledNotification(
                                ArgumentMatchers.any(), ArgumentMatchers.anyInt()))
                .thenAnswer(
                        invocation -> {
                            affectedCounts.add(invocation.getArgument(1));
                            return mail;
                        });

        // 2. Exercise
        matchNotificationService.notifyRecurringMatchesCancelled(
                List.of(firstOccurrence, secondOccurrence));

        // 3. Assert
        Assertions.assertEquals(
                List.of(firstParticipant.getEmail(), secondParticipant.getEmail()),
                mailDispatchService.recipients);
        Assertions.assertEquals(List.of(2, 1), affectedCounts);
    }

    @Test
    public void testNotifyHostPlayerLeftSendsMailToHost() {
        final Match match = createMatch(60L, "Weekly Padel", EventStatus.OPEN);
        final User player = UserUtils.getUser(2L);
        final User host = UserUtils.getUser(1L);
        final MailContent mail = new MailContent("player-left", "<p>left</p>", "left");
        Mockito.when(templateRenderer.renderPlayerLeftNotification(ArgumentMatchers.any()))
                .thenReturn(mail);

        matchNotificationService.notifyHostPlayerLeft(match, player);

        Assertions.assertEquals(List.of(host.getEmail()), mailDispatchService.recipients);
        Assertions.assertEquals(List.of(mail), mailDispatchService.contents);
    }

    @Test
    public void testNotifyPlayerRemovedByHostSendsMailToPlayer() {
        final Match match = createMatch(61L, "Weekly Padel", EventStatus.OPEN);
        final User player = UserUtils.getUser(2L);
        final MailContent mail = new MailContent("removed", "<p>removed</p>", "removed");
        Mockito.when(templateRenderer.renderParticipantRemovedNotification(ArgumentMatchers.any()))
                .thenReturn(mail);

        matchNotificationService.notifyPlayerRemovedByHost(match, player);

        Assertions.assertEquals(List.of(player.getEmail()), mailDispatchService.recipients);
        Assertions.assertEquals(List.of(mail), mailDispatchService.contents);
    }

    @Test
    public void testNotifyMatchUpdatedUsesRecipientPreferredLanguage() {
        final Match match = createMatch(62L, "Weekly Padel", EventStatus.OPEN);
        final User participant =
                new User(
                        2L,
                        "player@test.com",
                        "player",
                        "Jamie",
                        "Rivera",
                        null,
                        null,
                        UserLanguages.SPANISH);
        final AtomicReference<Locale> capturedLocale = new AtomicReference<>();
        Mockito.when(matchParticipantDao.findConfirmedParticipants(62L))
                .thenReturn(List.of(participant));
        Mockito.when(
                        messageSource.getMessage(
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.any(Locale.class)))
                .thenAnswer(
                        invocation -> {
                            capturedLocale.set(invocation.getArgument(3));
                            return invocation.getArgument(2);
                        });
        Mockito.when(templateRenderer.renderMatchUpdatedNotification(ArgumentMatchers.any()))
                .thenReturn(new MailContent("updated", "<p>1</p>", "1"));

        matchNotificationService.notifyMatchUpdated(match);

        Assertions.assertEquals(Locale.of("es"), capturedLocale.get());
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

    private static Match createMatch(final long id, final String title, final EventStatus status) {
        Match match =
                MatchUtils.createMatchWithId(
                        id, 1L, Sport.PADEL, Instant.parse("2026-04-06T18:00:00Z"), 10);
        match.setStatus(status);
        return match;
    }

    private static Match createRecurringMatch(
            final long id,
            final String title,
            final EventStatus status,
            final long seriesId,
            final int occurrenceIndex) {
        return new Match(
                id,
                Sport.PADEL,
                UserUtils.getUser(1L),
                "Downtown Club",
                null,
                null,
                title,
                "Description",
                Instant.parse("2026-04-06T18:00:00Z").plusSeconds(604800L * occurrenceIndex),
                Instant.parse("2026-04-06T19:00:00Z").plusSeconds(604800L * occurrenceIndex),
                10,
                BigDecimal.ZERO,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT,
                status,
                0,
                null,
                MatchUtils.getMatchSeries(seriesId, UserUtils.getUser(1L)),
                occurrenceIndex,
                false,
                null,
                null,
                null);
    }
}
