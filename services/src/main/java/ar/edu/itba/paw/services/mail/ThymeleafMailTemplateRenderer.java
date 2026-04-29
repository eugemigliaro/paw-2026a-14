package ar.edu.itba.paw.services.mail;

import ar.edu.itba.paw.services.VerificationPreviewDetail;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
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

    public MailContent renderMatchUpdatedNotification(
            final MatchLifecycleMailTemplateData templateData) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = buildMatchLifecycleContext(templateData, locale);
        context.setVariable("mailEyebrow", message("mail.matchLifecycle.updated.eyebrow", locale));
        context.setVariable(
                "title",
                message(
                        "mail.matchLifecycle.updated.title",
                        new Object[] {templateData.getMatchTitle()},
                        locale));
        context.setVariable("summary", message("mail.matchLifecycle.updated.summary", locale));

        return new MailContent(
                message(
                        "mail.matchLifecycle.updated.subject",
                        new Object[] {templateData.getMatchTitle()},
                        locale),
                htmlMailTemplateEngine.process("match-updated", context),
                textMailTemplateEngine.process("match-updated", context));
    }

    public MailContent renderMatchCancelledNotification(
            final MatchLifecycleMailTemplateData templateData) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = buildMatchLifecycleContext(templateData, locale);
        context.setVariable(
                "mailEyebrow", message("mail.matchLifecycle.cancelled.eyebrow", locale));
        context.setVariable(
                "title",
                message(
                        "mail.matchLifecycle.cancelled.title",
                        new Object[] {templateData.getMatchTitle()},
                        locale));
        context.setVariable("summary", message("mail.matchLifecycle.cancelled.summary", locale));
        context.setVariable("notice", message("mail.matchLifecycle.cancelled.notice", locale));

        return new MailContent(
                message(
                        "mail.matchLifecycle.cancelled.subject",
                        new Object[] {templateData.getMatchTitle()},
                        locale),
                htmlMailTemplateEngine.process("match-cancelled", context),
                textMailTemplateEngine.process("match-cancelled", context));
    }

    public MailContent renderBanNotification(final BanMailTemplateData templateData) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = new Context(locale);
        context.setVariable("mailEyebrow", message("mail.moderation.ban.eyebrow", locale));
        context.setVariable("title", message("mail.moderation.ban.title", locale));
        context.setVariable("summary", message("mail.moderation.ban.summary", locale));
        context.setVariable("usernameLabel", message("mail.moderation.ban.username", locale));
        context.setVariable("username", templateData.getUsername());
        context.setVariable("bannedUntilLabel", message("mail.moderation.ban.until", locale));
        context.setVariable("bannedUntil", formatDateTime(templateData.getBannedUntil(), locale));
        context.setVariable("reasonLabel", message("mail.moderation.ban.reason", locale));
        context.setVariable("reason", templateData.getReason());
        context.setVariable("loginLabel", message("mail.moderation.ban.login", locale));
        context.setVariable("loginUrl", templateData.getLoginUrl());
        context.setVariable("lang", locale.getLanguage());

        return new MailContent(
                message("mail.moderation.ban.subject", null, locale),
                htmlMailTemplateEngine.process("moderation-ban", context),
                textMailTemplateEngine.process("moderation-ban", context));
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

    private Context buildMatchLifecycleContext(
            final MatchLifecycleMailTemplateData templateData, final Locale locale) {
        final Context context = new Context(locale);
        context.setVariable("recipientEmail", templateData.getRecipientEmail());
        context.setVariable(
                "requestedForLabel", message("mail.matchLifecycle.requestedFor", locale));
        context.setVariable("detailsLabel", message("mail.matchLifecycle.details", locale));
        context.setVariable("details", buildMatchLifecycleDetails(templateData, locale));
        context.setVariable("lang", locale.getLanguage());
        return context;
    }

    private List<VerificationPreviewDetail> buildMatchLifecycleDetails(
            final MatchLifecycleMailTemplateData templateData, final Locale locale) {
        final List<VerificationPreviewDetail> details = new ArrayList<>();
        details.add(
                new VerificationPreviewDetail(
                        message("mail.matchLifecycle.field.matchTitle", locale),
                        templateData.getMatchTitle()));
        details.add(
                new VerificationPreviewDetail(
                        message("mail.matchLifecycle.field.sport", locale),
                        templateData.getSportLabel()));
        details.add(
                new VerificationPreviewDetail(
                        message("mail.matchLifecycle.field.address", locale),
                        templateData.getAddress()));
        details.add(
                new VerificationPreviewDetail(
                        message("mail.matchLifecycle.field.startsAt", locale),
                        formatDateTime(templateData.getStartsAt(), locale)));
        if (templateData.getEndsAt() != null) {
            details.add(
                    new VerificationPreviewDetail(
                            message("mail.matchLifecycle.field.endsAt", locale),
                            formatDateTime(templateData.getEndsAt(), locale)));
        }
        details.add(
                new VerificationPreviewDetail(
                        message("mail.matchLifecycle.field.status", locale),
                        templateData.getStatusLabel()));
        return List.copyOf(details);
    }

    private static String formatDateTime(final Instant instant, final Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale)
                .format(instant.atZone(ZoneId.systemDefault()));
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
