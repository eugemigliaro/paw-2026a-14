package ar.edu.itba.paw.services.mail;

import ar.edu.itba.paw.services.VerificationPreviewDetail;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class ThymeleafMailTemplateRendererTest {

    @Test
    public void testRenderReservationConfirmationIncludesImportantFields() {
        final ThymeleafMailTemplateRenderer renderer =
                new ThymeleafMailTemplateRenderer(htmlTemplateEngine(), textTemplateEngine());

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
                                        new VerificationPreviewDetail("Price", "$10"))));

        Assertions.assertTrue(content.getHtmlBody().contains("Padel Night"));
        Assertions.assertTrue(
                content.getHtmlBody().contains("http://localhost:8080/verifications/token"));
        Assertions.assertTrue(content.getHtmlBody().contains("One-time verification"));
        Assertions.assertTrue(content.getHtmlBody().contains("Match Point"));
        Assertions.assertTrue(content.getTextBody().contains("Downtown Club"));
        Assertions.assertEquals("Confirm your reservation for Padel Night", content.getSubject());
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
}
