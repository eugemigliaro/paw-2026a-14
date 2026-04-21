package ar.edu.itba.paw.services.mail;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

public class SmtpMailService implements MailService {

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    public SmtpMailService(
            final JavaMailSender javaMailSender, final MailProperties mailProperties) {
        this.javaMailSender = javaMailSender;
        this.mailProperties = mailProperties;
    }

    @Override
    public void send(final String recipientEmail, final MailContent content) {
        try {
            final MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(recipientEmail);
            helper.setSubject(content.getSubject());
            helper.setText(content.getTextBody(), content.getHtmlBody());
            javaMailSender.send(mimeMessage);
        } catch (final MessagingException | MailException exception) {
            throw new IllegalStateException("Failed to send email", exception);
        }
    }
}
