package ar.edu.itba.paw.services.mail;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncMailDispatchService implements MailDispatchService {

    private static final Log LOGGER = LogFactory.getLog(AsyncMailDispatchService.class);

    private final MailService mailService;

    @Autowired
    public AsyncMailDispatchService(final MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    @Async("mailTaskExecutor")
    public void dispatch(final String recipientEmail, final MailContent content) {
        try {
            mailService.send(recipientEmail, content);
        } catch (final RuntimeException exception) {
            LOGGER.error("Failed to dispatch verification email to " + recipientEmail, exception);
        }
    }
}
