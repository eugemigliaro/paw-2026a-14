package ar.edu.itba.paw.services.mail;

public class MailContent {

    private final String subject;
    private final String htmlBody;
    private final String textBody;

    public MailContent(final String subject, final String htmlBody, final String textBody) {
        this.subject = subject;
        this.htmlBody = htmlBody;
        this.textBody = textBody;
    }

    public String getSubject() {
        return subject;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public String getTextBody() {
        return textBody;
    }
}
