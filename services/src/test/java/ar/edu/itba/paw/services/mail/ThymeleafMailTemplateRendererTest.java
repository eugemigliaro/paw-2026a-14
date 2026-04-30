package ar.edu.itba.paw.services.mail;

import ar.edu.itba.paw.services.VerificationPreviewDetail;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class ThymeleafMailTemplateRendererTest {

    @Test
    public void testRenderReservationConfirmationIncludesImportantFields() {
        final ThymeleafMailTemplateRenderer renderer =
                new ThymeleafMailTemplateRenderer(
                        htmlTemplateEngine(), textTemplateEngine(), messageSource());

        final MailContent content =
                renderer.renderReservationConfirmation(
                        new VerificationMailTemplateData(
                                "Confirm your reservation for Padel Night",
                                "Use this one-time link to reserve the spot.",
                                "player@test.com",
                                "http://localhost:8080/verifications/token",
                                Instant.parse("2026-04-06T18:00:00Z"),
                                List.of(
                                        new VerificationPreviewDetail("Venue", "Downtown Club"),
                                        new VerificationPreviewDetail("Price", "$10")),
                                Locale.ENGLISH));

        Assertions.assertTrue(content.getHtmlBody().contains("Padel Night"));
        Assertions.assertTrue(
                content.getHtmlBody().contains("http://localhost:8080/verifications/token"));
        Assertions.assertTrue(content.getHtmlBody().contains("One-time verification"));
        Assertions.assertTrue(content.getHtmlBody().contains("Match Point"));
        Assertions.assertTrue(content.getTextBody().contains("Downtown Club"));
        Assertions.assertEquals("Confirm your reservation for Padel Night", content.getSubject());
    }

    @Test
    public void testRenderReservationConfirmationLocalizesMailWrapperAndExpiry() {
        final ThymeleafMailTemplateRenderer renderer =
                new ThymeleafMailTemplateRenderer(
                        htmlTemplateEngine(), textTemplateEngine(), messageSource());

        final MailContent content =
                renderer.renderReservationConfirmation(
                        new VerificationMailTemplateData(
                                "Confirmá tu reserva para Noche de Pádel",
                                "Usá este enlace único para reservar tu lugar.",
                                "jugadora@test.com",
                                "http://localhost:8080/verifications/token?lang=es",
                                Instant.parse("2026-04-06T18:00:00Z"),
                                List.of(
                                        new VerificationPreviewDetail("Deporte", "Pádel"),
                                        new VerificationPreviewDetail("Precio", "$10")),
                                Locale.of("es")));

        Assertions.assertTrue(content.getHtmlBody().contains("Verificación única"));
        Assertions.assertTrue(content.getHtmlBody().contains("Solicitado para"));
        Assertions.assertTrue(content.getHtmlBody().contains("Revisar y confirmar acción"));
        Assertions.assertTrue(content.getHtmlBody().contains("lang=\"es\""));
        Assertions.assertTrue(content.getTextBody().contains("Detalles:"));
        Assertions.assertTrue(content.getTextBody().contains("Este enlace expira el 6 abr 2026"));
    }

    @Test
    public void testRenderMatchUpdatedNotificationIncludesImportantFields() {
        final ThymeleafMailTemplateRenderer renderer =
                new ThymeleafMailTemplateRenderer(
                        htmlTemplateEngine(), textTemplateEngine(), messageSource());

        final MailContent content =
                renderer.renderMatchUpdatedNotification(
                        new MatchLifecycleMailTemplateData(
                                "player@test.com",
                                "Padel Night",
                                "Downtown Club",
                                Instant.parse("2026-04-06T18:00:00Z"),
                                Instant.parse("2026-04-06T19:30:00Z"),
                                "Padel",
                                "Open",
                                Locale.ENGLISH));

        Assertions.assertTrue(content.getHtmlBody().contains("Event updated"));
        Assertions.assertTrue(content.getHtmlBody().contains("Padel Night"));
        Assertions.assertTrue(content.getHtmlBody().contains("Downtown Club"));
        Assertions.assertTrue(content.getTextBody().contains("Sport"));
        Assertions.assertTrue(content.getTextBody().contains("Status"));
        Assertions.assertEquals("Event updated: Padel Night", content.getSubject());
    }

    @Test
    public void testRenderMatchCancelledNotificationLocalizesWrapperAndNotice() {
        final ThymeleafMailTemplateRenderer renderer =
                new ThymeleafMailTemplateRenderer(
                        htmlTemplateEngine(), textTemplateEngine(), messageSource());

        final MailContent content =
                renderer.renderMatchCancelledNotification(
                        new MatchLifecycleMailTemplateData(
                                "jugadora@test.com",
                                "Noche de Padel",
                                "Club Centro",
                                Instant.parse("2026-04-06T18:00:00Z"),
                                null,
                                "Padel",
                                "Cancelado",
                                Locale.of("es")));

        Assertions.assertTrue(content.getHtmlBody().contains("Evento cancelado"));
        Assertions.assertTrue(content.getHtmlBody().contains("Tu reserva ya no sigue activa"));
        Assertions.assertTrue(content.getHtmlBody().contains("lang=\"es\""));
        Assertions.assertTrue(content.getTextBody().contains("Solicitado para"));
        Assertions.assertTrue(content.getTextBody().contains("Estado"));
        Assertions.assertEquals("Evento cancelado: Noche de Padel", content.getSubject());
    }

    @Test
    public void testRenderRecurringMatchesUpdatedNotificationSummarizesAffectedDates() {
        final ThymeleafMailTemplateRenderer renderer =
                new ThymeleafMailTemplateRenderer(
                        htmlTemplateEngine(), textTemplateEngine(), messageSource());

        final MailContent content =
                renderer.renderRecurringMatchesUpdatedNotification(
                        new MatchLifecycleMailTemplateData(
                                "player@test.com",
                                "Weekly Padel",
                                "Downtown Club",
                                Instant.parse("2026-04-06T18:00:00Z"),
                                Instant.parse("2026-04-06T19:30:00Z"),
                                "Padel",
                                "Open",
                                Locale.ENGLISH),
                        3);

        Assertions.assertTrue(content.getHtmlBody().contains("Recurring event updated"));
        Assertions.assertTrue(content.getTextBody().contains("Affected dates"));
        Assertions.assertTrue(content.getTextBody().contains("3 recurring dates"));
        Assertions.assertEquals("Recurring event updated: Weekly Padel", content.getSubject());
    }

    @Test
    public void testRenderRecurringMatchesCancelledNotificationLocalizesNotice() {
        final ThymeleafMailTemplateRenderer renderer =
                new ThymeleafMailTemplateRenderer(
                        htmlTemplateEngine(), textTemplateEngine(), messageSource());

        final MailContent content =
                renderer.renderRecurringMatchesCancelledNotification(
                        new MatchLifecycleMailTemplateData(
                                "jugadora@test.com",
                                "Noche semanal de Padel",
                                "Club Centro",
                                Instant.parse("2026-04-06T18:00:00Z"),
                                null,
                                "Padel",
                                "Cancelado",
                                Locale.of("es")),
                        2);

        Assertions.assertTrue(content.getHtmlBody().contains("Evento recurrente cancelado"));
        Assertions.assertTrue(content.getTextBody().contains("Fechas afectadas"));
        Assertions.assertTrue(content.getTextBody().contains("2 fechas recurrentes"));
        Assertions.assertTrue(content.getHtmlBody().contains("Tus reservas ya no siguen activas"));
        Assertions.assertEquals(
                "Evento recurrente cancelado: Noche semanal de Padel", content.getSubject());
    }

    @Test
    public void testRenderSeriesInvitationNotificationUsesSeriesCopy() {
        final ThymeleafMailTemplateRenderer renderer =
                new ThymeleafMailTemplateRenderer(
                        htmlTemplateEngine(), textTemplateEngine(), messageSource());

        final MailContent content =
                renderer.renderSeriesInvitationNotification(
                        new MatchLifecycleMailTemplateData(
                                "player@test.com",
                                "Weekly Padel",
                                "Downtown Club",
                                Instant.parse("2026-04-06T18:00:00Z"),
                                Instant.parse("2026-04-06T19:30:00Z"),
                                "Padel",
                                "Open",
                                Locale.ENGLISH),
                        3);

        Assertions.assertTrue(content.getHtmlBody().contains("Series invitation"));
        Assertions.assertTrue(content.getHtmlBody().contains("all available dates in this series"));
        Assertions.assertTrue(content.getTextBody().contains("Series dates"));
        Assertions.assertTrue(content.getTextBody().contains("3 dates in this series"));
        Assertions.assertEquals("You are invited to a series: Weekly Padel", content.getSubject());
    }

    private static TemplateEngine htmlTemplateEngine() {
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("mail/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        final TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }

    private static TemplateEngine textTemplateEngine() {
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("mail/");
        resolver.setSuffix(".txt");
        resolver.setTemplateMode("TEXT");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        final TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }

    private static StaticMessageSource messageSource() {
        final StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage(
                "mail.verification.eyebrow", Locale.ENGLISH, "One-time verification");
        messageSource.addMessage(
                "mail.verification.eyebrow", Locale.of("es"), "Verificación única");
        messageSource.addMessage("mail.verification.requestedFor", Locale.ENGLISH, "Requested for");
        messageSource.addMessage(
                "mail.verification.requestedFor", Locale.of("es"), "Solicitado para");
        messageSource.addMessage("mail.verification.details", Locale.ENGLISH, "Details");
        messageSource.addMessage("mail.verification.details", Locale.of("es"), "Detalles");
        messageSource.addMessage(
                "mail.verification.confirmAction", Locale.ENGLISH, "Review and confirm action");
        messageSource.addMessage(
                "mail.verification.confirmAction", Locale.of("es"), "Revisar y confirmar acción");
        messageSource.addMessage(
                "mail.verification.expiresAt", Locale.ENGLISH, "This link expires at {0}.");
        messageSource.addMessage(
                "mail.verification.expiresAt", Locale.of("es"), "Este enlace expira el {0}.");
        messageSource.addMessage(
                "mail.verification.ignore",
                Locale.ENGLISH,
                "If you did not request this action, you can ignore this email.");
        messageSource.addMessage(
                "mail.verification.ignore",
                Locale.of("es"),
                "Si no solicitaste esta acción, podés ignorar este email.");
        messageSource.addMessage(
                "mail.matchLifecycle.requestedFor", Locale.ENGLISH, "Requested for");
        messageSource.addMessage(
                "mail.matchLifecycle.requestedFor", Locale.of("es"), "Solicitado para");
        messageSource.addMessage("mail.matchLifecycle.details", Locale.ENGLISH, "Details");
        messageSource.addMessage("mail.matchLifecycle.details", Locale.of("es"), "Detalles");
        messageSource.addMessage("mail.matchLifecycle.field.matchTitle", Locale.ENGLISH, "Match");
        messageSource.addMessage("mail.matchLifecycle.field.matchTitle", Locale.of("es"), "Evento");
        messageSource.addMessage("mail.matchLifecycle.field.sport", Locale.ENGLISH, "Sport");
        messageSource.addMessage("mail.matchLifecycle.field.sport", Locale.of("es"), "Deporte");
        messageSource.addMessage("mail.matchLifecycle.field.address", Locale.ENGLISH, "Venue");
        messageSource.addMessage("mail.matchLifecycle.field.address", Locale.of("es"), "Lugar");
        messageSource.addMessage("mail.matchLifecycle.field.startsAt", Locale.ENGLISH, "Starts at");
        messageSource.addMessage("mail.matchLifecycle.field.startsAt", Locale.of("es"), "Empieza");
        messageSource.addMessage("mail.matchLifecycle.field.endsAt", Locale.ENGLISH, "Ends at");
        messageSource.addMessage("mail.matchLifecycle.field.endsAt", Locale.of("es"), "Termina");
        messageSource.addMessage("mail.matchLifecycle.field.status", Locale.ENGLISH, "Status");
        messageSource.addMessage("mail.matchLifecycle.field.status", Locale.of("es"), "Estado");
        messageSource.addMessage(
                "mail.matchLifecycle.field.affectedDates", Locale.ENGLISH, "Affected dates");
        messageSource.addMessage(
                "mail.matchLifecycle.field.affectedDates", Locale.of("es"), "Fechas afectadas");
        messageSource.addMessage(
                "mail.matchLifecycle.affectedDates", Locale.ENGLISH, "{0} recurring dates");
        messageSource.addMessage(
                "mail.matchLifecycle.affectedDates", Locale.of("es"), "{0} fechas recurrentes");
        messageSource.addMessage(
                "mail.matchLifecycle.updated.eyebrow", Locale.ENGLISH, "Event updated");
        messageSource.addMessage(
                "mail.matchLifecycle.updated.eyebrow", Locale.of("es"), "Evento actualizado");
        messageSource.addMessage(
                "mail.matchLifecycle.updated.subject", Locale.ENGLISH, "Event updated: {0}");
        messageSource.addMessage(
                "mail.matchLifecycle.updated.subject", Locale.of("es"), "Evento actualizado: {0}");
        messageSource.addMessage(
                "mail.matchLifecycle.updated.title", Locale.ENGLISH, "{0} was updated");
        messageSource.addMessage(
                "mail.matchLifecycle.updated.title", Locale.of("es"), "{0} fue actualizado");
        messageSource.addMessage(
                "mail.matchLifecycle.updated.summary",
                Locale.ENGLISH,
                "The host updated an event you joined. Review the latest details below.");
        messageSource.addMessage(
                "mail.matchLifecycle.updated.summary",
                Locale.of("es"),
                "El organizador actualizo un evento al que te sumaste. Revisa los ultimos detalles abajo.");
        messageSource.addMessage(
                "mail.matchLifecycle.cancelled.eyebrow", Locale.ENGLISH, "Event cancelled");
        messageSource.addMessage(
                "mail.matchLifecycle.cancelled.eyebrow", Locale.of("es"), "Evento cancelado");
        messageSource.addMessage(
                "mail.matchLifecycle.cancelled.subject", Locale.ENGLISH, "Event cancelled: {0}");
        messageSource.addMessage(
                "mail.matchLifecycle.cancelled.subject", Locale.of("es"), "Evento cancelado: {0}");
        messageSource.addMessage(
                "mail.matchLifecycle.cancelled.title", Locale.ENGLISH, "{0} was cancelled");
        messageSource.addMessage(
                "mail.matchLifecycle.cancelled.title", Locale.of("es"), "{0} fue cancelado");
        messageSource.addMessage(
                "mail.matchLifecycle.cancelled.summary",
                Locale.ENGLISH,
                "The host cancelled an event you joined.");
        messageSource.addMessage(
                "mail.matchLifecycle.cancelled.summary",
                Locale.of("es"),
                "El organizador cancelo un evento al que te habias sumado.");
        messageSource.addMessage(
                "mail.matchLifecycle.cancelled.notice",
                Locale.ENGLISH,
                "Your reservation is no longer active for this event.");
        messageSource.addMessage(
                "mail.matchLifecycle.cancelled.notice",
                Locale.of("es"),
                "Tu reserva ya no sigue activa para este evento.");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringUpdated.eyebrow",
                Locale.ENGLISH,
                "Recurring event updated");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringUpdated.eyebrow",
                Locale.of("es"),
                "Evento recurrente actualizado");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringUpdated.subject",
                Locale.ENGLISH,
                "Recurring event updated: {0}");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringUpdated.subject",
                Locale.of("es"),
                "Evento recurrente actualizado: {0}");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringUpdated.title",
                Locale.ENGLISH,
                "Recurring dates for {0} were updated");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringUpdated.title",
                Locale.of("es"),
                "Fechas recurrentes de {0} actualizadas");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringUpdated.summary",
                Locale.ENGLISH,
                "The host updated {0} upcoming recurring dates you joined. Review the latest details below.");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringUpdated.summary",
                Locale.of("es"),
                "El organizador actualizo {0} proximas fechas recurrentes a las que te sumaste.");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringCancelled.eyebrow",
                Locale.ENGLISH,
                "Recurring event cancelled");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringCancelled.eyebrow",
                Locale.of("es"),
                "Evento recurrente cancelado");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringCancelled.subject",
                Locale.ENGLISH,
                "Recurring event cancelled: {0}");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringCancelled.subject",
                Locale.of("es"),
                "Evento recurrente cancelado: {0}");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringCancelled.title",
                Locale.ENGLISH,
                "Recurring dates for {0} were cancelled");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringCancelled.title",
                Locale.of("es"),
                "Fechas recurrentes de {0} canceladas");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringCancelled.summary",
                Locale.ENGLISH,
                "The host cancelled {0} upcoming recurring dates you joined.");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringCancelled.summary",
                Locale.of("es"),
                "El organizador cancelo {0} proximas fechas recurrentes a las que te habias sumado.");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringCancelled.notice",
                Locale.ENGLISH,
                "Your reservations are no longer active for these recurring dates.");
        messageSource.addMessage(
                "mail.matchLifecycle.recurringCancelled.notice",
                Locale.of("es"),
                "Tus reservas ya no siguen activas para estas fechas recurrentes.");
        messageSource.addMessage(
                "mail.matchInvitation.requestedFor", Locale.ENGLISH, "Invitation for");
        messageSource.addMessage(
                "mail.matchInvitation.requestedFor", Locale.of("es"), "Invitacion para");
        messageSource.addMessage(
                "mail.matchInvitation.details", Locale.ENGLISH, "Invitation details");
        messageSource.addMessage(
                "mail.matchInvitation.details", Locale.of("es"), "Detalles de la invitacion");
        messageSource.addMessage("mail.matchInvitation.field.matchTitle", Locale.ENGLISH, "Event");
        messageSource.addMessage(
                "mail.matchInvitation.field.matchTitle", Locale.of("es"), "Evento");
        messageSource.addMessage("mail.matchInvitation.field.sport", Locale.ENGLISH, "Sport");
        messageSource.addMessage("mail.matchInvitation.field.sport", Locale.of("es"), "Deporte");
        messageSource.addMessage("mail.matchInvitation.field.address", Locale.ENGLISH, "Venue");
        messageSource.addMessage("mail.matchInvitation.field.address", Locale.of("es"), "Lugar");
        messageSource.addMessage(
                "mail.matchInvitation.field.startsAt", Locale.ENGLISH, "Starts at");
        messageSource.addMessage("mail.matchInvitation.field.startsAt", Locale.of("es"), "Empieza");
        messageSource.addMessage("mail.matchInvitation.field.endsAt", Locale.ENGLISH, "Ends at");
        messageSource.addMessage("mail.matchInvitation.field.endsAt", Locale.of("es"), "Termina");
        messageSource.addMessage("mail.matchInvitation.field.status", Locale.ENGLISH, "Status");
        messageSource.addMessage("mail.matchInvitation.field.status", Locale.of("es"), "Estado");
        messageSource.addMessage(
                "mail.matchInvitation.field.seriesDates", Locale.ENGLISH, "Series dates");
        messageSource.addMessage(
                "mail.matchInvitation.field.seriesDates", Locale.of("es"), "Fechas de la serie");
        messageSource.addMessage(
                "mail.matchInvitation.seriesDates", Locale.ENGLISH, "{0} dates in this series");
        messageSource.addMessage(
                "mail.matchInvitation.seriesDates", Locale.of("es"), "{0} fechas de esta serie");
        messageSource.addMessage(
                "mail.matchInvitation.single.eyebrow", Locale.ENGLISH, "Event invitation");
        messageSource.addMessage(
                "mail.matchInvitation.single.eyebrow", Locale.of("es"), "Invitacion a evento");
        messageSource.addMessage(
                "mail.matchInvitation.single.subject", Locale.ENGLISH, "You are invited: {0}");
        messageSource.addMessage(
                "mail.matchInvitation.single.subject", Locale.of("es"), "Estas invitado: {0}");
        messageSource.addMessage(
                "mail.matchInvitation.single.title", Locale.ENGLISH, "You are invited to {0}");
        messageSource.addMessage(
                "mail.matchInvitation.single.title", Locale.of("es"), "Estas invitado a {0}");
        messageSource.addMessage(
                "mail.matchInvitation.single.summary",
                Locale.ENGLISH,
                "The host invited you to this private event. Review the details before responding.");
        messageSource.addMessage(
                "mail.matchInvitation.single.summary",
                Locale.of("es"),
                "El organizador te invito a este evento privado.");
        messageSource.addMessage(
                "mail.matchInvitation.series.eyebrow", Locale.ENGLISH, "Series invitation");
        messageSource.addMessage(
                "mail.matchInvitation.series.eyebrow", Locale.of("es"), "Invitacion a serie");
        messageSource.addMessage(
                "mail.matchInvitation.series.subject",
                Locale.ENGLISH,
                "You are invited to a series: {0}");
        messageSource.addMessage(
                "mail.matchInvitation.series.subject",
                Locale.of("es"),
                "Estas invitado a una serie: {0}");
        messageSource.addMessage(
                "mail.matchInvitation.series.title",
                Locale.ENGLISH,
                "You are invited to all dates in {0}");
        messageSource.addMessage(
                "mail.matchInvitation.series.title",
                Locale.of("es"),
                "Estas invitado a todas las fechas de {0}");
        messageSource.addMessage(
                "mail.matchInvitation.series.summary",
                Locale.ENGLISH,
                "The host invited you to all available dates in this series. This invitation covers {0} dates where you were not already invited or participating.");
        messageSource.addMessage(
                "mail.matchInvitation.series.summary",
                Locale.of("es"),
                "El organizador te invito a todas las fechas disponibles de esta serie.");
        return messageSource;
    }
}
