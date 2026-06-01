package ar.edu.itba.paw.services.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingMailService implements MailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingMailService.class);
    private final MailProperties mailProperties;

    public LoggingMailService(final MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    @Override
    public void send(final String recipientEmail, final MailContent content) {
        LOGGER.info(
                "Mail mode=LOG from={} recipient={} subject={} textLength={} htmlLength={}",
                mailProperties.getFrom(),
                MailLog.maskEmail(recipientEmail),
                content.getSubject(),
                content.getTextBody() == null ? 0 : content.getTextBody().length(),
                content.getHtmlBody() == null ? 0 : content.getHtmlBody().length());
    }
}
