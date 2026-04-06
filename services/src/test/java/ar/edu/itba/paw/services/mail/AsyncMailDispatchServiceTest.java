package ar.edu.itba.paw.services.mail;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AsyncMailDispatchServiceTest {

    @Mock private MailService mailService;

    @Test
    public void testDispatchDelegatesToMailService() {
        final AsyncMailDispatchService asyncMailDispatchService =
                new AsyncMailDispatchService(mailService);
        final MailContent content = new MailContent("subject", "<p>html</p>", "text");

        Assertions.assertDoesNotThrow(
                () -> asyncMailDispatchService.dispatch("player@test.com", content));
    }

    @Test
    public void testDispatchSwallowsMailFailures() {
        final AsyncMailDispatchService asyncMailDispatchService =
                new AsyncMailDispatchService(mailService);
        final MailContent content = new MailContent("subject", "<p>html</p>", "text");

        Mockito.doThrow(new IllegalStateException("smtp failed"))
                .when(mailService)
                .send("player@test.com", content);

        Assertions.assertDoesNotThrow(
                () -> asyncMailDispatchService.dispatch("player@test.com", content));
    }
}
