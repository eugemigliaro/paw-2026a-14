package ar.edu.itba.paw.services.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
class AsyncMailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncMailSender.class);

    private final MailService mailService;

    @Autowired
    AsyncMailSender(final MailService mailService) {
        this.mailService = mailService;
    }

    @Async("mailTaskExecutor")
    public void send(final String recipientEmail, final MailContent content) {
        try {
            mailService.send(recipientEmail, content);
            LOGGER.debug("Mail dispatched recipient={}", MailLog.maskEmail(recipientEmail));
        } catch (final RuntimeException exception) {
            LOGGER.error(
                    "Mail dispatch failed recipient={}",
                    MailLog.maskEmail(recipientEmail),
                    exception);
        }
    }
}
