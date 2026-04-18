package ar.edu.itba.paw.services.mail;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class ThymeleafMailTemplateRenderer {

    private final TemplateEngine htmlMailTemplateEngine;
    private final TemplateEngine textMailTemplateEngine;
    private final MessageSource messageSource;

    public ThymeleafMailTemplateRenderer(
            final TemplateEngine htmlMailTemplateEngine,
            final TemplateEngine textMailTemplateEngine,
            final MessageSource messageSource) {
        this.htmlMailTemplateEngine = htmlMailTemplateEngine;
        this.textMailTemplateEngine = textMailTemplateEngine;
        this.messageSource = messageSource;
    }

    public MailContent renderReservationConfirmation(
            final VerificationMailTemplateData templateData) {
        return renderActionMail(templateData);
    }

    public MailContent renderActionMail(final VerificationMailTemplateData templateData) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = new Context(locale);
        context.setVariable("title", templateData.getTitle());
        context.setVariable("summary", templateData.getSummary());
        context.setVariable("recipientEmail", templateData.getRecipientEmail());
        context.setVariable("confirmationUrl", templateData.getConfirmationUrl());
        context.setVariable("mailEyebrow", message("mail.verification.eyebrow", locale));
        context.setVariable("requestedForLabel", message("mail.verification.requestedFor", locale));
        context.setVariable("detailsLabel", message("mail.verification.details", locale));
        context.setVariable(
                "confirmActionLabel", message("mail.verification.confirmAction", locale));
        context.setVariable(
                "expiresAtText",
                message(
                        "mail.verification.expiresAt",
                        new Object[] {
                            DateTimeFormatter.ofLocalizedDateTime(
                                            FormatStyle.MEDIUM, FormatStyle.SHORT)
                                    .withLocale(locale)
                                    .format(
                                            templateData
                                                    .getExpiresAt()
                                                    .atZone(ZoneId.systemDefault()))
                        },
                        locale));
        context.setVariable("ignoreNotice", message("mail.verification.ignore", locale));
        context.setVariable("details", templateData.getDetails());
        context.setVariable("lang", locale.getLanguage());

        return new MailContent(
                templateData.getTitle(),
                htmlMailTemplateEngine.process("verification-action", context),
                textMailTemplateEngine.process("verification-action", context));
    }

    private String message(final String code, final Locale locale) {
        return message(code, null, locale);
    }

    private String message(final String code, final Object[] args, final Locale locale) {
        return messageSource.getMessage(code, args, code, locale);
    }

    private static Locale resolvedLocale(final Locale locale) {
        return locale == null ? Locale.ENGLISH : locale;
    }
}
