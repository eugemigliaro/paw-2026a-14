package ar.edu.itba.paw.services.mail;

import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class ThymeleafMailTemplateRendererTest {

    private static final String EVENT_URL = "https://matchpoint.test/matches/40";
    private static final String LOGIN_URL = "http://localhost:8080/login";

    @Test
    public void testMatchNotificationInterpolatesRecipientFacingData() {
        final ThymeleafMailTemplateRenderer renderer = renderer();

        final MailContent content =
                renderer.renderMatchUpdatedNotification(
                        new MatchLifecycleMailTemplateData(
                                "player@test.com",
                                "Padel Night",
                                "Downtown Club",
                                Instant.parse("2026-01-05T13:00:00Z"),
                                Instant.parse("2026-01-05T14:30:00Z"),
                                "Padel",
                                "Open",
                                EVENT_URL,
                                Locale.ENGLISH));

        // The values the caller passed reach the recipient: title, venue, and the action link.
        Assertions.assertTrue(content.getHtmlBody().contains("Padel Night"));
        Assertions.assertTrue(content.getHtmlBody().contains("Downtown Club"));
        Assertions.assertTrue(content.getHtmlBody().contains(EVENT_URL));
        Assertions.assertTrue(content.getSubject().contains("Padel Night"));
        // No message argument was left uninterpolated.
        Assertions.assertFalse(content.getHtmlBody().contains("{0}"));
        Assertions.assertFalse(content.getTextBody().contains("{0}"));
    }

    @Test
    public void testRecurringNotificationReflectsAffectedCount() {
        final ThymeleafMailTemplateRenderer renderer = renderer();
        final MatchLifecycleMailTemplateData data =
                new MatchLifecycleMailTemplateData(
                        "player@test.com",
                        "Weekly Padel",
                        "Downtown Club",
                        Instant.parse("2026-01-05T13:00:00Z"),
                        null,
                        "Padel",
                        "Open",
                        EVENT_URL,
                        Locale.ENGLISH);

        final MailContent seven = renderer.renderRecurringMatchesUpdatedNotification(data, 7);
        final MailContent nine = renderer.renderRecurringMatchesUpdatedNotification(data, 9);

        // Changing the count changes the output and the value itself appears (neither date in
        // this fixture contains a 7 or a 9).
        Assertions.assertNotEquals(seven.getTextBody(), nine.getTextBody());
        Assertions.assertTrue(seven.getTextBody().contains("7"));
        Assertions.assertTrue(nine.getTextBody().contains("9"));
        Assertions.assertFalse(seven.getTextBody().contains("{0}"));
        Assertions.assertFalse(seven.getHtmlBody().contains("{0}"));
    }

    @Test
    public void testNotificationIsRenderedInRecipientLocale() {
        final ThymeleafMailTemplateRenderer renderer = renderer();

        final MailContent english =
                renderer.renderMatchCancelledNotification(cancelledData(Locale.ENGLISH));
        final MailContent spanish =
                renderer.renderMatchCancelledNotification(cancelledData(Locale.of("es")));

        // The document is tagged with the recipient's language and the localized renders differ.
        Assertions.assertTrue(english.getHtmlBody().contains("lang=\"en\""));
        Assertions.assertTrue(spanish.getHtmlBody().contains("lang=\"es\""));
        Assertions.assertNotEquals(english.getHtmlBody(), spanish.getHtmlBody());
        // Localizing must not drop the recipient-facing essentials (caller data, action link).
        Assertions.assertTrue(spanish.getHtmlBody().contains("Padel Night"));
        Assertions.assertTrue(spanish.getHtmlBody().contains(EVENT_URL));
    }

    @Test
    public void testBanNotificationInterpolatesModerationDetails() {
        final ThymeleafMailTemplateRenderer renderer = renderer();

        final MailContent content =
                renderer.renderBanNotification(
                        new BanMailTemplateData(
                                "player@test.com",
                                "player",
                                Instant.parse("2026-04-20T12:00:00Z"),
                                "Repeated abuse",
                                LOGIN_URL,
                                Locale.ENGLISH));

        Assertions.assertTrue(content.getHtmlBody().contains("player"));
        Assertions.assertTrue(content.getHtmlBody().contains("Repeated abuse"));
        Assertions.assertTrue(content.getHtmlBody().contains(LOGIN_URL));
        Assertions.assertFalse(content.getHtmlBody().contains("{0}"));
        Assertions.assertFalse(content.getTextBody().contains("{0}"));
    }

    private static MatchLifecycleMailTemplateData cancelledData(final Locale locale) {
        return new MatchLifecycleMailTemplateData(
                "player@test.com",
                "Padel Night",
                "Downtown Club",
                Instant.parse("2026-01-05T13:00:00Z"),
                null,
                "Padel",
                "Cancelled",
                EVENT_URL,
                locale);
    }

    private static ThymeleafMailTemplateRenderer renderer() {
        return new ThymeleafMailTemplateRenderer(
                htmlTemplateEngine(), textTemplateEngine(), messageSource());
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

    /**
     * Rendering fixture: provides just enough copy for the templates exercised here to render. The
     * tests never assert on these values, so the exact wording is irrelevant; only the en/es
     * cancellation copy is duplicated, so the locale test has localized content to differ on.
     */
    private static StaticMessageSource messageSource() {
        final StaticMessageSource messages = new StaticMessageSource();
        final Locale es = Locale.of("es");

        messages.addMessage("mail.cta.viewEvent", Locale.ENGLISH, "View event");
        messages.addMessage("mail.matchLifecycle.requestedFor", Locale.ENGLISH, "Requested for");
        messages.addMessage("mail.matchLifecycle.details", Locale.ENGLISH, "Details");
        messages.addMessage("mail.matchLifecycle.field.matchTitle", Locale.ENGLISH, "Match");
        messages.addMessage("mail.matchLifecycle.field.sport", Locale.ENGLISH, "Sport");
        messages.addMessage("mail.matchLifecycle.field.address", Locale.ENGLISH, "Venue");
        messages.addMessage("mail.matchLifecycle.field.startsAt", Locale.ENGLISH, "Starts at");
        messages.addMessage("mail.matchLifecycle.field.endsAt", Locale.ENGLISH, "Ends at");
        messages.addMessage("mail.matchLifecycle.field.status", Locale.ENGLISH, "Status");
        messages.addMessage(
                "mail.matchLifecycle.field.affectedDates", Locale.ENGLISH, "Affected dates");
        messages.addMessage(
                "mail.matchLifecycle.affectedDates", Locale.ENGLISH, "{0} recurring dates");

        messages.addMessage("mail.matchLifecycle.updated.eyebrow", Locale.ENGLISH, "Event updated");
        messages.addMessage("mail.matchLifecycle.updated.title", Locale.ENGLISH, "{0} was updated");
        messages.addMessage(
                "mail.matchLifecycle.updated.summary",
                Locale.ENGLISH,
                "The host updated an event you joined.");
        messages.addMessage(
                "mail.matchLifecycle.updated.subject", Locale.ENGLISH, "Event updated: {0}");

        messages.addMessage(
                "mail.matchLifecycle.recurringUpdated.eyebrow",
                Locale.ENGLISH,
                "Recurring event updated");
        messages.addMessage(
                "mail.matchLifecycle.recurringUpdated.title",
                Locale.ENGLISH,
                "Recurring dates for {0} were updated");
        messages.addMessage(
                "mail.matchLifecycle.recurringUpdated.summary",
                Locale.ENGLISH,
                "The host updated {0} recurring dates you joined.");
        messages.addMessage(
                "mail.matchLifecycle.recurringUpdated.subject",
                Locale.ENGLISH,
                "Recurring event updated: {0}");

        messages.addMessage(
                "mail.matchLifecycle.cancelled.eyebrow", Locale.ENGLISH, "Event cancelled");
        messages.addMessage("mail.matchLifecycle.cancelled.eyebrow", es, "Evento cancelado");
        messages.addMessage(
                "mail.matchLifecycle.cancelled.title", Locale.ENGLISH, "{0} was cancelled");
        messages.addMessage("mail.matchLifecycle.cancelled.title", es, "{0} fue cancelado");
        messages.addMessage(
                "mail.matchLifecycle.cancelled.summary",
                Locale.ENGLISH,
                "The host cancelled an event you joined.");
        messages.addMessage(
                "mail.matchLifecycle.cancelled.summary",
                es,
                "El organizador canceló un evento al que te sumaste.");
        messages.addMessage(
                "mail.matchLifecycle.cancelled.notice",
                Locale.ENGLISH,
                "Your reservation is no longer active for this event.");
        messages.addMessage(
                "mail.matchLifecycle.cancelled.notice",
                es,
                "Tu reserva ya no sigue activa para este evento.");
        messages.addMessage(
                "mail.matchLifecycle.cancelled.subject", Locale.ENGLISH, "Event cancelled: {0}");
        messages.addMessage("mail.matchLifecycle.cancelled.subject", es, "Evento cancelado: {0}");

        messages.addMessage("mail.moderation.ban.eyebrow", Locale.ENGLISH, "Moderation notice");
        messages.addMessage(
                "mail.moderation.ban.title", Locale.ENGLISH, "Your account is temporarily banned");
        messages.addMessage(
                "mail.moderation.ban.summary", Locale.ENGLISH, "Review the details below.");
        messages.addMessage("mail.moderation.ban.username", Locale.ENGLISH, "User");
        messages.addMessage("mail.moderation.ban.until", Locale.ENGLISH, "Banned until");
        messages.addMessage("mail.moderation.ban.reason", Locale.ENGLISH, "Reason");
        messages.addMessage("mail.moderation.ban.login", Locale.ENGLISH, "Sign in");
        messages.addMessage(
                "mail.moderation.ban.subject",
                Locale.ENGLISH,
                "Your Match Point account has been temporarily banned");

        return messages;
    }
}
