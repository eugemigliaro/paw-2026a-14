package ar.edu.itba.paw.services.mail;

import java.util.Objects;
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
            helper.setFrom(Objects.requireNonNull(mailProperties.getFrom()));
            helper.setTo(Objects.requireNonNull(recipientEmail));
            helper.setSubject(Objects.requireNonNull(content.getSubject()));
            helper.setText(
                    Objects.requireNonNull(content.getTextBody()),
                    Objects.requireNonNull(content.getHtmlBody()));

            javaMailSender.send(mimeMessage);
        } catch (final MessagingException | MailException exception) {
            throw new IllegalStateException("Failed to send email", exception);
        }
    }
}
