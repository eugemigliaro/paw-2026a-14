package ar.edu.itba.paw.services.mail;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.persistence.TournamentTeamDao;
import ar.edu.itba.paw.services.utils.MatchUtils;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@ExtendWith(MockitoExtension.class)
public class AsyncMailDispatchServiceTest {

    @Mock private ThymeleafMailTemplateRenderer templateRenderer;
    @Mock private MessageSource messageSource;
    @Mock private TournamentTeamDao tournamentTeamDao;

    @Test
    public void testStandardMailActionSendsRenderedContent() {
        final RecordingMailService mailService = new RecordingMailService(false);
        final AsyncMailDispatchService asyncMailDispatchService = dispatchService(mailService);
        final MailContent content = new MailContent("subject", "<p>html</p>", "text");
        final User user = UserUtils.getUser(2L);
        Mockito.when(templateRenderer.renderUnbanNotification(ArgumentMatchers.any()))
                .thenReturn(content);

        asyncMailDispatchService.sendUnbanNotice(user);

        Assertions.assertEquals(user.getEmail(), mailService.recipientEmail);
        Assertions.assertEquals("subject", mailService.content.getSubject());
        Assertions.assertEquals("<p>html</p>", mailService.content.getHtmlBody());
        Assertions.assertEquals("text", mailService.content.getTextBody());
    }

    @Test
    public void testStandardMailActionSwallowsMailFailures() {
        final RecordingMailService mailService = new RecordingMailService(true);
        final AsyncMailDispatchService asyncMailDispatchService = dispatchService(mailService);
        final User user = UserUtils.getUser(2L);
        Mockito.when(templateRenderer.renderUnbanNotification(ArgumentMatchers.any()))
                .thenReturn(new MailContent("subject", "<p>html</p>", "text"));

        Assertions.assertDoesNotThrow(() -> asyncMailDispatchService.sendUnbanNotice(user));
    }

    @Test
    public void testMatchNotificationEmailContainsDeepLinkToEvent() {
        final RecordingMailService mailService = new RecordingMailService(false);
        final StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("mail.cta.viewEvent", Locale.ENGLISH, "View event");
        final ThymeleafMailTemplateRenderer realRenderer =
                new ThymeleafMailTemplateRenderer(
                        htmlTemplateEngine(), textTemplateEngine(), messages);
        final AsyncMailDispatchService asyncMailDispatchService =
                new AsyncMailDispatchService(
                        mailService, realRenderer, messages, mailProperties(), tournamentTeamDao);
        final Match match =
                MatchUtils.createMatchWithId(
                        40L, 1L, Sport.PADEL, Instant.parse("2026-04-06T18:00:00Z"), 10);

        asyncMailDispatchService.sendMatchUpdated(UserUtils.getUser(2L), match);

        Assertions.assertTrue(
                mailService.content.getHtmlBody().contains("https://matchpoint.test/matches/40"));
        Assertions.assertTrue(
                mailService.content.getTextBody().contains("https://matchpoint.test/matches/40"));
        Assertions.assertTrue(mailService.content.getHtmlBody().contains("?lang=en"));
    }

    @Test
    public void testTournamentBracketEmailContainsDeepLinkToBracket() {
        final RecordingMailService mailService = new RecordingMailService(false);
        final StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("mail.cta.viewBracket", Locale.ENGLISH, "View bracket");
        messages.addMessage("mail.cta.viewTournament", Locale.ENGLISH, "View tournament");
        final ThymeleafMailTemplateRenderer realRenderer =
                new ThymeleafMailTemplateRenderer(
                        htmlTemplateEngine(), textTemplateEngine(), messages);
        final AsyncMailDispatchService asyncMailDispatchService =
                new AsyncMailDispatchService(
                        mailService, realRenderer, messages, mailProperties(), tournamentTeamDao);

        asyncMailDispatchService.sendTournamentBracketPublished(
                UserUtils.getUser(2L), tournamentWithId(7L));

        Assertions.assertTrue(
                mailService
                        .content
                        .getHtmlBody()
                        .contains("https://matchpoint.test/tournaments/7/bracket?lang=en"));
        Assertions.assertTrue(mailService.content.getHtmlBody().contains("View bracket"));
        Assertions.assertTrue(
                mailService
                        .content
                        .getTextBody()
                        .contains("https://matchpoint.test/tournaments/7/bracket?lang=en"));
    }

    private AsyncMailDispatchService dispatchService(final MailService mailService) {
        return new AsyncMailDispatchService(
                mailService, templateRenderer, messageSource, mailProperties(), tournamentTeamDao);
    }

    private static MailProperties mailProperties() {
        return new MailProperties(
                MailMode.LOG,
                "https://matchpoint.test",
                "no-reply@matchpoint.test",
                "",
                587,
                "",
                "",
                false,
                true,
                24);
    }

    private static Tournament tournamentWithId(final long id) {
        return new Tournament(
                id,
                UserUtils.getUser(1L),
                Sport.PADEL,
                "Saturday Cup",
                "Tournament description",
                "Downtown Club",
                null,
                null,
                Instant.parse("2026-04-06T18:00:00Z"),
                Instant.parse("2026-04-06T21:00:00Z"),
                BigDecimal.ZERO,
                null,
                TournamentFormat.SINGLE_ELIMINATION,
                8,
                1,
                true,
                false,
                Instant.parse("2026-04-01T18:00:00Z"),
                Instant.parse("2026-04-05T18:00:00Z"),
                TournamentStatus.IN_PROGRESS,
                Instant.parse("2026-03-20T18:00:00Z"),
                Instant.parse("2026-03-20T18:00:00Z"));
    }

    private static TemplateEngine htmlTemplateEngine() {
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("mail/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        final TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }

    private static TemplateEngine textTemplateEngine() {
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("mail/");
        resolver.setSuffix(".txt");
        resolver.setTemplateMode("TEXT");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        final TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }

    private static class RecordingMailService implements MailService {

        private final boolean fail;
        private String recipientEmail;
        private MailContent content;

        private RecordingMailService(final boolean fail) {
            this.fail = fail;
        }

        @Override
        public void send(final String recipientEmail, final MailContent content) {
            if (fail) {
                throw new IllegalStateException("smtp failed");
            }
            this.recipientEmail = recipientEmail;
            this.content = content;
        }
    }
}
