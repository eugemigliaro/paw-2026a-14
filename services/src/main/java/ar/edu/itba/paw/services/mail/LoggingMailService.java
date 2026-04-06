package ar.edu.itba.paw.services.mail;

public class LoggingMailService implements MailService {

    private final MailProperties mailProperties;

    public LoggingMailService(final MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    @Override
    public void send(final String recipientEmail, final MailContent content) {
        System.out.printf(
                "MAIL MODE=LOG from=%s to=%s subject=\"%s\"%n%s%n",
                mailProperties.getFrom(),
                recipientEmail,
                content.getSubject(),
                content.getTextBody());
    }
}
