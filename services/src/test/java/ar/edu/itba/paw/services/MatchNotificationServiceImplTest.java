package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@ExtendWith(MockitoExtension.class)
public class MatchNotificationServiceImplTest {

    @InjectMocks private MatchNotificationServiceImpl matchNotificationService;

    @Mock private MatchParticipantDao matchParticipantDao;
    @Mock private MailDispatchService mailDispatchService;
    @Mock private ThymeleafMailTemplateRenderer templateRenderer;
    @Mock private MessageSource messageSource;
    @Mock private UserService userService;

    @BeforeEach
    public void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
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
        final Match match = createMatch(40L, "Updated Match", "open");
        final List<User> participants =
                List.of(
                        new User(2L, "first@test.com", "first"),
                        new User(3L, "second@test.com", "second"));
        final MailContent firstMail = new MailContent("updated-1", "<p>1</p>", "1");
        final MailContent secondMail = new MailContent("updated-2", "<p>2</p>", "2");
        Mockito.when(matchParticipantDao.findConfirmedParticipants(40L)).thenReturn(participants);
        Mockito.when(templateRenderer.renderMatchUpdatedNotification(ArgumentMatchers.any()))
                .thenReturn(firstMail, secondMail);

        matchNotificationService.notifyMatchUpdated(match);

        final ArgumentCaptor<String> recipientCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<MailContent> contentCaptor =
                ArgumentCaptor.forClass(MailContent.class);
        Mockito.verify(mailDispatchService, Mockito.times(2))
                .dispatch(recipientCaptor.capture(), contentCaptor.capture());

        Assertions.assertEquals(
                List.of("first@test.com", "second@test.com"), recipientCaptor.getAllValues());
        Assertions.assertSame(firstMail, contentCaptor.getAllValues().get(0));
        Assertions.assertSame(secondMail, contentCaptor.getAllValues().get(1));
    }

    @Test
    public void testNotifyMatchCancelledSendsOneMailPerParticipant() {
        final Match match = createMatch(41L, "Cancelled Match", "cancelled");
        final List<User> participants =
                List.of(
                        new User(2L, "first@test.com", "first"),
                        new User(3L, "second@test.com", "second"));
        final MailContent firstMail = new MailContent("cancelled-1", "<p>1</p>", "1");
        final MailContent secondMail = new MailContent("cancelled-2", "<p>2</p>", "2");
        Mockito.when(matchParticipantDao.findConfirmedParticipants(41L)).thenReturn(participants);
        Mockito.when(templateRenderer.renderMatchCancelledNotification(ArgumentMatchers.any()))
                .thenReturn(firstMail, secondMail);

        matchNotificationService.notifyMatchCancelled(match);

        final ArgumentCaptor<String> recipientCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<MailContent> contentCaptor =
                ArgumentCaptor.forClass(MailContent.class);
        Mockito.verify(mailDispatchService, Mockito.times(2))
                .dispatch(recipientCaptor.capture(), contentCaptor.capture());

        Assertions.assertEquals(
                List.of("first@test.com", "second@test.com"), recipientCaptor.getAllValues());
        Assertions.assertSame(firstMail, contentCaptor.getAllValues().get(0));
        Assertions.assertSame(secondMail, contentCaptor.getAllValues().get(1));
    }

    @Test
    public void testNotifyMatchUpdatedDoesNothingWhenNoParticipants() {
        final Match match = createMatch(42L, "Quiet Match", "open");
        Mockito.when(matchParticipantDao.findConfirmedParticipants(42L)).thenReturn(List.of());

        matchNotificationService.notifyMatchUpdated(match);

        Mockito.verifyNoInteractions(mailDispatchService, templateRenderer);
    }

    @Test
    public void testNotifyRecurringMatchesUpdatedDeduplicatesAffectedParticipants() {
        // 1. Arrange
        final Match firstOccurrence = createRecurringMatch(50L, "Weekly Padel", "open", 600L, 1);
        final Match secondOccurrence = createRecurringMatch(51L, "Weekly Padel", "open", 600L, 2);
        final User firstParticipant = new User(2L, "first@test.com", "first");
        final User secondParticipant = new User(3L, "second@test.com", "second");
        final MailContent mail = new MailContent("series-updated", "<p>updated</p>", "updated");
        final List<String> recipients = new ArrayList<>();
        Mockito.when(matchParticipantDao.findConfirmedParticipants(50L))
                .thenReturn(List.of(firstParticipant));
        Mockito.when(matchParticipantDao.findConfirmedParticipants(51L))
                .thenReturn(List.of(firstParticipant, secondParticipant));
        Mockito.when(
                        templateRenderer.renderRecurringMatchesUpdatedNotification(
                                ArgumentMatchers.any(), ArgumentMatchers.anyInt()))
                .thenReturn(mail);
        Mockito.doAnswer(
                        invocation -> {
                            recipients.add(invocation.getArgument(0));
                            return null;
                        })
                .when(mailDispatchService)
                .dispatch(ArgumentMatchers.anyString(), ArgumentMatchers.any());

        // 2. Exercise
        matchNotificationService.notifyRecurringMatchesUpdated(
                List.of(firstOccurrence, secondOccurrence));

        // 3. Assert
        Assertions.assertEquals(List.of("first@test.com", "second@test.com"), recipients);
    }

    @Test
    public void testNotifyRecurringMatchesCancelledUsesPerRecipientAffectedDateCount() {
        // 1. Arrange
        final Match firstOccurrence =
                createRecurringMatch(52L, "Weekly Padel", "cancelled", 600L, 1);
        final Match secondOccurrence =
                createRecurringMatch(53L, "Weekly Padel", "cancelled", 600L, 2);
        final User firstParticipant = new User(2L, "first@test.com", "first");
        final User secondParticipant = new User(3L, "second@test.com", "second");
        final MailContent mail =
                new MailContent("series-cancelled", "<p>cancelled</p>", "cancelled");
        final List<Integer> affectedCounts = new ArrayList<>();
        final List<String> recipients = new ArrayList<>();
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
        Mockito.doAnswer(
                        invocation -> {
                            recipients.add(invocation.getArgument(0));
                            return null;
                        })
                .when(mailDispatchService)
                .dispatch(ArgumentMatchers.anyString(), ArgumentMatchers.any());

        // 2. Exercise
        matchNotificationService.notifyRecurringMatchesCancelled(
                List.of(firstOccurrence, secondOccurrence));

        // 3. Assert
        Assertions.assertEquals(List.of("first@test.com", "second@test.com"), recipients);
        Assertions.assertEquals(List.of(2, 1), affectedCounts);
    }

    private static Match createMatch(final long id, final String title, final String status) {
        return new Match(
                id,
                Sport.PADEL,
                1L,
                "Downtown Club",
                title,
                "Description",
                Instant.parse("2026-04-06T18:00:00Z"),
                Instant.parse("2026-04-06T19:00:00Z"),
                10,
                BigDecimal.ZERO,
                "public",
                status,
                0,
                null);
    }

    private static Match createRecurringMatch(
            final long id,
            final String title,
            final String status,
            final long seriesId,
            final int occurrenceIndex) {
        return new Match(
                id,
                Sport.PADEL,
                1L,
                "Downtown Club",
                title,
                "Description",
                Instant.parse("2026-04-06T18:00:00Z").plusSeconds(604800L * occurrenceIndex),
                Instant.parse("2026-04-06T19:00:00Z").plusSeconds(604800L * occurrenceIndex),
                10,
                BigDecimal.ZERO,
                "public",
                "direct",
                status,
                0,
                null,
                seriesId,
                occurrenceIndex);
    }
}
