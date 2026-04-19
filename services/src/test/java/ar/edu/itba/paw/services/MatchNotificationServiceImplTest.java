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
import java.util.List;
import java.util.Locale;
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
}
