package ar.edu.itba.paw.services.mail;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class ThymeleafMailTemplateRenderer {

    private static final DateTimeFormatter EXPIRY_FORMATTER =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withLocale(Locale.US);

    private final TemplateEngine htmlMailTemplateEngine;
    private final TemplateEngine textMailTemplateEngine;

    public ThymeleafMailTemplateRenderer(
            final TemplateEngine htmlMailTemplateEngine,
            final TemplateEngine textMailTemplateEngine) {
        this.htmlMailTemplateEngine = htmlMailTemplateEngine;
        this.textMailTemplateEngine = textMailTemplateEngine;
    }

    public MailContent renderReservationConfirmation(
            final VerificationMailTemplateData templateData) {
        final Context context = new Context(Locale.US);
        context.setVariable("title", templateData.getTitle());
        context.setVariable("summary", templateData.getSummary());
        context.setVariable("recipientEmail", templateData.getRecipientEmail());
        context.setVariable("confirmationUrl", templateData.getConfirmationUrl());
        context.setVariable(
                "expiresAtLabel",
                EXPIRY_FORMATTER.format(
                        templateData.getExpiresAt().atZone(ZoneId.systemDefault())));
        context.setVariable("details", templateData.getDetails());

        return new MailContent(
                templateData.getTitle(),
                htmlMailTemplateEngine.process("verification-action", context),
                textMailTemplateEngine.process("verification-action", context));
    }
}
