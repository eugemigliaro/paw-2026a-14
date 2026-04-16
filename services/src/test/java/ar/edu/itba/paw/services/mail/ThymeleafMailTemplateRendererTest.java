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
                                new Locale("es")));

        Assertions.assertTrue(content.getHtmlBody().contains("Verificación única"));
        Assertions.assertTrue(content.getHtmlBody().contains("Solicitado para"));
        Assertions.assertTrue(content.getHtmlBody().contains("Revisar y confirmar acción"));
        Assertions.assertTrue(content.getHtmlBody().contains("lang=\"es\""));
        Assertions.assertTrue(content.getTextBody().contains("Detalles:"));
        Assertions.assertTrue(content.getTextBody().contains("Este enlace expira el 6 abr 2026"));
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
                "mail.verification.eyebrow", new Locale("es"), "Verificación única");
        messageSource.addMessage("mail.verification.requestedFor", Locale.ENGLISH, "Requested for");
        messageSource.addMessage(
                "mail.verification.requestedFor", new Locale("es"), "Solicitado para");
        messageSource.addMessage("mail.verification.details", Locale.ENGLISH, "Details");
        messageSource.addMessage("mail.verification.details", new Locale("es"), "Detalles");
        messageSource.addMessage(
                "mail.verification.confirmAction", Locale.ENGLISH, "Review and confirm action");
        messageSource.addMessage(
                "mail.verification.confirmAction", new Locale("es"), "Revisar y confirmar acción");
        messageSource.addMessage(
                "mail.verification.expiresAt", Locale.ENGLISH, "This link expires at {0}.");
        messageSource.addMessage(
                "mail.verification.expiresAt", new Locale("es"), "Este enlace expira el {0}.");
        messageSource.addMessage(
                "mail.verification.ignore",
                Locale.ENGLISH,
                "If you did not request this action, you can ignore this email.");
        messageSource.addMessage(
                "mail.verification.ignore",
                new Locale("es"),
                "Si no solicitaste esta acción, podés ignorar este email.");
        return messageSource;
    }
}
