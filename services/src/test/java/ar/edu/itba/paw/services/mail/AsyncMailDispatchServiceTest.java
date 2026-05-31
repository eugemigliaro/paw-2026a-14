package ar.edu.itba.paw.services.mail;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.TournamentTeamDao;
import ar.edu.itba.paw.services.utils.UserUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

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

    private AsyncMailDispatchService dispatchService(final MailService mailService) {
        return new AsyncMailDispatchService(
                mailService,
                templateRenderer,
                messageSource,
                new MailProperties(
                        MailMode.LOG,
                        "https://matchpoint.test",
                        "no-reply@matchpoint.test",
                        "",
                        587,
                        "",
                        "",
                        false,
                        true,
                        24),
                tournamentTeamDao);
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
