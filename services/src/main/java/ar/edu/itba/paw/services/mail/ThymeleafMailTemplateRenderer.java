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
        final Context context = buildMatchLifecycleContext(templateData, locale, null);
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

    public MailContent renderPlayerJoinedNotification(
            final MatchLifecycleMailTemplateData templateData) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = buildMatchLifecycleContext(templateData, locale, null);
        context.setVariable(
                "mailEyebrow", message("mail.participation.playerJoined.eyebrow", locale));
        context.setVariable(
                "title",
                message(
                        "mail.participation.playerJoined.title",
                        new Object[] {templateData.getActorName(), templateData.getMatchTitle()},
                        locale));
        context.setVariable("summary", message("mail.participation.playerJoined.summary", locale));

        return new MailContent(
                message(
                        "mail.participation.playerJoined.subject",
                        new Object[] {templateData.getMatchTitle()},
                        locale),
                htmlMailTemplateEngine.process("match-updated", context),
                textMailTemplateEngine.process("match-updated", context));
    }

    public MailContent renderJoinRequestReceivedNotification(
            final MatchLifecycleMailTemplateData templateData) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = buildMatchLifecycleContext(templateData, locale, null);
        context.setVariable(
                "mailEyebrow", message("mail.participation.joinRequestReceived.eyebrow", locale));
        context.setVariable(
                "title",
                message(
                        "mail.participation.joinRequestReceived.title",
                        new Object[] {templateData.getActorName(), templateData.getMatchTitle()},
                        locale));
        context.setVariable(
                "summary", message("mail.participation.joinRequestReceived.summary", locale));

        return new MailContent(
                message(
                        "mail.participation.joinRequestReceived.subject",
                        new Object[] {templateData.getActorName()},
                        locale),
                htmlMailTemplateEngine.process("match-updated", context),
                textMailTemplateEngine.process("match-updated", context));
    }

    public MailContent renderJoinRequestApprovedNotification(
            final MatchLifecycleMailTemplateData templateData) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = buildMatchLifecycleContext(templateData, locale, null);
        context.setVariable(
                "mailEyebrow", message("mail.participation.requestApproved.eyebrow", locale));
        context.setVariable(
                "title",
                message(
                        "mail.participation.requestApproved.title",
                        new Object[] {templateData.getMatchTitle()},
                        locale));
        context.setVariable(
                "summary", message("mail.participation.requestApproved.summary", locale));

        return new MailContent(
                message(
                        "mail.participation.requestApproved.subject",
                        new Object[] {templateData.getMatchTitle()},
                        locale),
                htmlMailTemplateEngine.process("match-updated", context),
                textMailTemplateEngine.process("match-updated", context));
    }

    public MailContent renderJoinRequestRejectedNotification(
            final MatchLifecycleMailTemplateData templateData) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = buildMatchLifecycleContext(templateData, locale, null);
        context.setVariable(
                "mailEyebrow", message("mail.participation.requestRejected.eyebrow", locale));
        context.setVariable(
                "title",
                message(
                        "mail.participation.requestRejected.title",
                        new Object[] {templateData.getMatchTitle()},
                        locale));
        context.setVariable(
                "summary", message("mail.participation.requestRejected.summary", locale));

        return new MailContent(
                message(
                        "mail.participation.requestRejected.subject",
                        new Object[] {templateData.getMatchTitle()},
                        locale),
                htmlMailTemplateEngine.process("match-updated", context),
                textMailTemplateEngine.process("match-updated", context));
    }

    public MailContent renderInviteAcceptedNotification(
            final MatchLifecycleMailTemplateData templateData) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = buildMatchLifecycleContext(templateData, locale, null);
        context.setVariable(
                "mailEyebrow", message("mail.participation.inviteAccepted.eyebrow", locale));
        context.setVariable(
                "title",
                message(
                        "mail.participation.inviteAccepted.title",
                        new Object[] {templateData.getActorName(), templateData.getMatchTitle()},
                        locale));
        context.setVariable(
                "summary", message("mail.participation.inviteAccepted.summary", locale));

        return new MailContent(
                message(
                        "mail.participation.inviteAccepted.subject",
                        new Object[] {templateData.getActorName()},
                        locale),
                htmlMailTemplateEngine.process("match-updated", context),
                textMailTemplateEngine.process("match-updated", context));
    }

    public MailContent renderInviteDeclinedNotification(
            final MatchLifecycleMailTemplateData templateData) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = buildMatchLifecycleContext(templateData, locale, null);
        context.setVariable(
                "mailEyebrow", message("mail.participation.inviteDeclined.eyebrow", locale));
        context.setVariable(
                "title",
                message(
                        "mail.participation.inviteDeclined.title",
                        new Object[] {templateData.getActorName(), templateData.getMatchTitle()},
                        locale));
        context.setVariable(
                "summary", message("mail.participation.inviteDeclined.summary", locale));

        return new MailContent(
                message(
                        "mail.participation.inviteDeclined.subject",
                        new Object[] {templateData.getActorName()},
                        locale),
                htmlMailTemplateEngine.process("match-updated", context),
                textMailTemplateEngine.process("match-updated", context));
    }

    public MailContent renderPlayerLeftNotification(
            final MatchLifecycleMailTemplateData templateData) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = buildMatchLifecycleContext(templateData, locale, null);
        context.setVariable(
                "mailEyebrow", message("mail.participation.playerLeft.eyebrow", locale));
        context.setVariable(
                "title",
                message(
                        "mail.participation.playerLeft.title",
                        new Object[] {templateData.getActorName(), templateData.getMatchTitle()},
                        locale));
        context.setVariable("summary", message("mail.participation.playerLeft.summary", locale));

        return new MailContent(
                message(
                        "mail.participation.playerLeft.subject",
                        new Object[] {templateData.getMatchTitle()},
                        locale),
                htmlMailTemplateEngine.process("match-updated", context),
                textMailTemplateEngine.process("match-updated", context));
    }

    public MailContent renderParticipantRemovedNotification(
            final MatchLifecycleMailTemplateData templateData) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = buildMatchLifecycleContext(templateData, locale, null);
        context.setVariable(
                "mailEyebrow", message("mail.participation.playerRemoved.eyebrow", locale));
        context.setVariable(
                "title",
                message(
                        "mail.participation.playerRemoved.title",
                        new Object[] {templateData.getMatchTitle()},
                        locale));
        context.setVariable("summary", message("mail.participation.playerRemoved.summary", locale));

        return new MailContent(
                message(
                        "mail.participation.playerRemoved.subject",
                        new Object[] {templateData.getMatchTitle()},
                        locale),
                htmlMailTemplateEngine.process("match-updated", context),
                textMailTemplateEngine.process("match-updated", context));
    }

    public MailContent renderMatchCancelledNotification(
            final MatchLifecycleMailTemplateData templateData) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = buildMatchLifecycleContext(templateData, locale, null);
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

    public MailContent renderRecurringMatchesUpdatedNotification(
            final MatchLifecycleMailTemplateData templateData, final int occurrenceCount) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = buildMatchLifecycleContext(templateData, locale, occurrenceCount);
        context.setVariable(
                "mailEyebrow", message("mail.matchLifecycle.recurringUpdated.eyebrow", locale));
        context.setVariable(
                "title",
                message(
                        "mail.matchLifecycle.recurringUpdated.title",
                        new Object[] {templateData.getMatchTitle()},
                        locale));
        context.setVariable(
                "summary",
                message(
                        "mail.matchLifecycle.recurringUpdated.summary",
                        new Object[] {occurrenceCount},
                        locale));

        return new MailContent(
                message(
                        "mail.matchLifecycle.recurringUpdated.subject",
                        new Object[] {templateData.getMatchTitle()},
                        locale),
                htmlMailTemplateEngine.process("match-updated", context),
                textMailTemplateEngine.process("match-updated", context));
    }

    public MailContent renderRecurringMatchesCancelledNotification(
            final MatchLifecycleMailTemplateData templateData, final int occurrenceCount) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = buildMatchLifecycleContext(templateData, locale, occurrenceCount);
        context.setVariable(
                "mailEyebrow", message("mail.matchLifecycle.recurringCancelled.eyebrow", locale));
        context.setVariable(
                "title",
                message(
                        "mail.matchLifecycle.recurringCancelled.title",
                        new Object[] {templateData.getMatchTitle()},
                        locale));
        context.setVariable(
                "summary",
                message(
                        "mail.matchLifecycle.recurringCancelled.summary",
                        new Object[] {occurrenceCount},
                        locale));
        context.setVariable(
                "notice", message("mail.matchLifecycle.recurringCancelled.notice", locale));

        return new MailContent(
                message(
                        "mail.matchLifecycle.recurringCancelled.subject",
                        new Object[] {templateData.getMatchTitle()},
                        locale),
                htmlMailTemplateEngine.process("match-cancelled", context),
                textMailTemplateEngine.process("match-cancelled", context));
    }

    public MailContent renderMatchInvitationNotification(
            final MatchLifecycleMailTemplateData templateData) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = buildMatchInvitationContext(templateData, locale, null);
        context.setVariable("mailEyebrow", message("mail.matchInvitation.single.eyebrow", locale));
        context.setVariable(
                "title",
                message(
                        "mail.matchInvitation.single.title",
                        new Object[] {templateData.getMatchTitle()},
                        locale));
        context.setVariable("summary", message("mail.matchInvitation.single.summary", locale));

        return new MailContent(
                message(
                        "mail.matchInvitation.single.subject",
                        new Object[] {templateData.getMatchTitle()},
                        locale),
                htmlMailTemplateEngine.process("match-updated", context),
                textMailTemplateEngine.process("match-updated", context));
    }

    public MailContent renderSeriesInvitationNotification(
            final MatchLifecycleMailTemplateData templateData, final int occurrenceCount) {
        final Locale locale = resolvedLocale(templateData.getLocale());
        final Context context = buildMatchInvitationContext(templateData, locale, occurrenceCount);
        context.setVariable("mailEyebrow", message("mail.matchInvitation.series.eyebrow", locale));
        context.setVariable(
                "title",
                message(
                        "mail.matchInvitation.series.title",
                        new Object[] {templateData.getMatchTitle()},
                        locale));
        context.setVariable(
                "summary",
                message(
                        "mail.matchInvitation.series.summary",
                        new Object[] {occurrenceCount},
                        locale));

        return new MailContent(
                message(
                        "mail.matchInvitation.series.subject",
                        new Object[] {templateData.getMatchTitle()},
                        locale),
                htmlMailTemplateEngine.process("match-updated", context),
                textMailTemplateEngine.process("match-updated", context));
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
            final MatchLifecycleMailTemplateData templateData,
            final Locale locale,
            final Integer occurrenceCount) {
        final Context context = new Context(locale);
        context.setVariable("recipientEmail", templateData.getRecipientEmail());
        context.setVariable(
                "requestedForLabel", message("mail.matchLifecycle.requestedFor", locale));
        context.setVariable("detailsLabel", message("mail.matchLifecycle.details", locale));
        context.setVariable(
                "details", buildMatchLifecycleDetails(templateData, locale, occurrenceCount));
        context.setVariable("lang", locale.getLanguage());
        return context;
    }

    private Context buildMatchInvitationContext(
            final MatchLifecycleMailTemplateData templateData,
            final Locale locale,
            final Integer occurrenceCount) {
        final Context context = new Context(locale);
        context.setVariable("recipientEmail", templateData.getRecipientEmail());
        context.setVariable(
                "requestedForLabel", message("mail.matchInvitation.requestedFor", locale));
        context.setVariable("detailsLabel", message("mail.matchInvitation.details", locale));
        context.setVariable(
                "details", buildMatchInvitationDetails(templateData, locale, occurrenceCount));
        context.setVariable("lang", locale.getLanguage());
        return context;
    }

    private List<VerificationPreviewDetail> buildMatchLifecycleDetails(
            final MatchLifecycleMailTemplateData templateData,
            final Locale locale,
            final Integer occurrenceCount) {
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
        if (occurrenceCount != null) {
            details.add(
                    new VerificationPreviewDetail(
                            message("mail.matchLifecycle.field.affectedDates", locale),
                            message(
                                    "mail.matchLifecycle.affectedDates",
                                    new Object[] {occurrenceCount},
                                    locale)));
        }
        return List.copyOf(details);
    }

    private List<VerificationPreviewDetail> buildMatchInvitationDetails(
            final MatchLifecycleMailTemplateData templateData,
            final Locale locale,
            final Integer occurrenceCount) {
        final List<VerificationPreviewDetail> details = new ArrayList<>();
        details.add(
                new VerificationPreviewDetail(
                        message("mail.matchInvitation.field.matchTitle", locale),
                        templateData.getMatchTitle()));
        details.add(
                new VerificationPreviewDetail(
                        message("mail.matchInvitation.field.sport", locale),
                        templateData.getSportLabel()));
        details.add(
                new VerificationPreviewDetail(
                        message("mail.matchInvitation.field.address", locale),
                        templateData.getAddress()));
        details.add(
                new VerificationPreviewDetail(
                        message("mail.matchInvitation.field.startsAt", locale),
                        formatDateTime(templateData.getStartsAt(), locale)));
        if (templateData.getEndsAt() != null) {
            details.add(
                    new VerificationPreviewDetail(
                            message("mail.matchInvitation.field.endsAt", locale),
                            formatDateTime(templateData.getEndsAt(), locale)));
        }
        details.add(
                new VerificationPreviewDetail(
                        message("mail.matchInvitation.field.status", locale),
                        templateData.getStatusLabel()));
        if (occurrenceCount != null) {
            details.add(
                    new VerificationPreviewDetail(
                            message("mail.matchInvitation.field.seriesDates", locale),
                            message(
                                    "mail.matchInvitation.seriesDates",
                                    new Object[] {occurrenceCount},
                                    locale)));
        }
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
