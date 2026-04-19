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
        messageSource.addMessage(
                "mail.matchLifecycle.field.matchTitle", Locale.ENGLISH, "Match");
        messageSource.addMessage("mail.matchLifecycle.field.matchTitle", Locale.of("es"), "Evento");
        messageSource.addMessage("mail.matchLifecycle.field.sport", Locale.ENGLISH, "Sport");
        messageSource.addMessage("mail.matchLifecycle.field.sport", Locale.of("es"), "Deporte");
        messageSource.addMessage("mail.matchLifecycle.field.address", Locale.ENGLISH, "Venue");
        messageSource.addMessage("mail.matchLifecycle.field.address", Locale.of("es"), "Lugar");
        messageSource.addMessage(
                "mail.matchLifecycle.field.startsAt", Locale.ENGLISH, "Starts at");
        messageSource.addMessage(
                "mail.matchLifecycle.field.startsAt", Locale.of("es"), "Empieza");
        messageSource.addMessage("mail.matchLifecycle.field.endsAt", Locale.ENGLISH, "Ends at");
        messageSource.addMessage("mail.matchLifecycle.field.endsAt", Locale.of("es"), "Termina");
        messageSource.addMessage("mail.matchLifecycle.field.status", Locale.ENGLISH, "Status");
        messageSource.addMessage("mail.matchLifecycle.field.status", Locale.of("es"), "Estado");
        messageSource.addMessage(
                "mail.matchLifecycle.updated.eyebrow", Locale.ENGLISH, "Event updated");
        messageSource.addMessage(
                "mail.matchLifecycle.updated.eyebrow", Locale.of("es"), "Evento actualizado");
        messageSource.addMessage(
                "mail.matchLifecycle.updated.subject",
                Locale.ENGLISH,
                "Event updated: {0}");
        messageSource.addMessage(
                "mail.matchLifecycle.updated.subject",
                Locale.of("es"),
                "Evento actualizado: {0}");
        messageSource.addMessage(
                "mail.matchLifecycle.updated.title", Locale.ENGLISH, "{0} was updated");
        messageSource.addMessage(
                "mail.matchLifecycle.updated.title",
                Locale.of("es"),
                "{0} fue actualizado");
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
                "mail.matchLifecycle.cancelled.subject",
                Locale.ENGLISH,
                "Event cancelled: {0}");
        messageSource.addMessage(
                "mail.matchLifecycle.cancelled.subject",
                Locale.of("es"),
                "Evento cancelado: {0}");
        messageSource.addMessage(
                "mail.matchLifecycle.cancelled.title", Locale.ENGLISH, "{0} was cancelled");
        messageSource.addMessage(
                "mail.matchLifecycle.cancelled.title",
                Locale.of("es"),
                "{0} fue cancelado");
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
        return messageSource;
    }
}
