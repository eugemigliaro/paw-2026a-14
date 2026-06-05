package ar.edu.itba.paw.services.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncMailDispatchService implements MailDispatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncMailDispatchService.class);

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
            LOGGER.debug("Mail dispatched recipient={}", maskEmail(recipientEmail));
        } catch (final RuntimeException exception) {
            LOGGER.error("Mail dispatch failed recipient={}", maskEmail(recipientEmail), exception);
        }
    }

    private static String maskEmail(final String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }

        final int atIndex = email.indexOf('@');
        if (atIndex <= 1 || atIndex == email.length() - 1) {
            return "***";
        }
        return email.charAt(0) + "***@" + email.substring(atIndex + 1);
    }
}
