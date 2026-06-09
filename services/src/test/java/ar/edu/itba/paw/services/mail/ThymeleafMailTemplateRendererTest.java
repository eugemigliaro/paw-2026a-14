package ar.edu.itba.paw.services.mail;

import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

// the idea is to test the emails like a black box
// assert that the user gets the data we are using when we call it
// assert that we are using the preferred language for a given user
// assert we are not leaking things like {0}

public class ThymeleafMailTemplateRendererTest {

    private static final String EVENT_URL = "https://matchpoint.test/matches/40";
    private static final String LOGIN_URL = "http://localhost:8080/login";
    private static final Locale SPANISH = Locale.of("es");

    @Test
    public void testMatchNotificationCarriesCallerDataToRecipient() {
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

        // The data the caller passed reaches the recipient: title, venue, and the action link.
        Assertions.assertTrue(content.getHtmlBody().contains("Padel Night"));
        Assertions.assertTrue(content.getHtmlBody().contains("Downtown Club"));
        Assertions.assertTrue(content.getHtmlBody().contains(EVENT_URL));
        Assertions.assertTrue(content.getSubject().contains("Padel Night"));
        assertNothingLeaked(content);
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

        // The affected count drives the output: different counts render differently and the value
        // itself appears (neither date in this fixture contains a 7 or a 9).
        Assertions.assertNotEquals(seven.getTextBody(), nine.getTextBody());
        Assertions.assertTrue(seven.getTextBody().contains("7"));
        Assertions.assertTrue(nine.getTextBody().contains("9"));
        assertNothingLeaked(seven);
    }

    @Test
    public void testNotificationIsRenderedInRecipientLocale() {
        final ThymeleafMailTemplateRenderer renderer = renderer();

        final MailContent english =
                renderer.renderMatchCancelledNotification(cancelledData(Locale.ENGLISH));
        final MailContent spanish =
                renderer.renderMatchCancelledNotification(cancelledData(SPANISH));

        // The recipient's locale drives the document language and the localized copy differs.
        Assertions.assertTrue(english.getHtmlBody().contains("lang=\"en\""));
        Assertions.assertTrue(spanish.getHtmlBody().contains("lang=\"es\""));
        Assertions.assertNotEquals(english.getHtmlBody(), spanish.getHtmlBody());
        // Localizing must not drop the recipient-facing essentials (caller data, action link).
        Assertions.assertTrue(spanish.getHtmlBody().contains("Padel Night"));
        Assertions.assertTrue(spanish.getHtmlBody().contains(EVENT_URL));
        assertNothingLeaked(english);
    }

    @Test
    public void testBanNotificationCarriesModerationDetails() {
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
        assertNothingLeaked(content);
    }

    /**
     * No raw message code and no un-interpolated argument reaches the recipient. This catches a
     * renderer that asks for a key the bundle does not define (the code would leak through) or a
     * message whose {@code {0}} argument was never supplied, without coupling the test to which
     * specific keys the renderer uses.
     */
    private static void assertNothingLeaked(final MailContent content) {
        Assertions.assertFalse(content.getHtmlBody().contains("mail."));
        Assertions.assertFalse(content.getTextBody().contains("mail."));
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
                templateEngine("HTML", ".html"), templateEngine("TEXT", ".txt"), messageSource());
    }

    private static TemplateEngine templateEngine(final String mode, final String suffix) {
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("mail/");
        resolver.setSuffix(suffix);
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        final TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }

    // mock the message source necessary for this tests
    // never checking these as contents of the template
    private static StaticMessageSource messageSource() {
        final StaticMessageSource messages = new StaticMessageSource();

        en(messages, "mail.cta.viewEvent", "View event");
        en(messages, "mail.matchLifecycle.requestedFor", "Requested for");
        en(messages, "mail.matchLifecycle.details", "Details");
        en(messages, "mail.matchLifecycle.field.matchTitle", "Match");
        en(messages, "mail.matchLifecycle.field.sport", "Sport");
        en(messages, "mail.matchLifecycle.field.address", "Venue");
        en(messages, "mail.matchLifecycle.field.startsAt", "Starts at");
        en(messages, "mail.matchLifecycle.field.endsAt", "Ends at");
        en(messages, "mail.matchLifecycle.field.status", "Status");
        en(messages, "mail.matchLifecycle.field.affectedDates", "Affected dates");
        en(messages, "mail.matchLifecycle.affectedDates", "{0} recurring dates");

        en(messages, "mail.matchLifecycle.updated.eyebrow", "Event updated");
        en(messages, "mail.matchLifecycle.updated.title", "{0} was updated");
        en(
                messages,
                "mail.matchLifecycle.updated.summary",
                "The host updated an event you joined.");
        en(messages, "mail.matchLifecycle.updated.subject", "Event updated: {0}");

        en(messages, "mail.matchLifecycle.recurringUpdated.eyebrow", "Recurring event updated");
        en(
                messages,
                "mail.matchLifecycle.recurringUpdated.title",
                "Recurring dates for {0} updated");
        en(messages, "mail.matchLifecycle.recurringUpdated.summary", "The host updated {0} dates.");
        en(
                messages,
                "mail.matchLifecycle.recurringUpdated.subject",
                "Recurring event updated: {0}");

        // Cancellation copy in both locales so the locale render genuinely differs.
        both(
                messages,
                "mail.matchLifecycle.cancelled.eyebrow",
                "Event cancelled",
                "Evento cancelado");
        both(
                messages,
                "mail.matchLifecycle.cancelled.title",
                "{0} was cancelled",
                "{0} fue cancelado");
        both(
                messages,
                "mail.matchLifecycle.cancelled.summary",
                "The host cancelled an event you joined.",
                "El organizador canceló un evento al que te sumaste.");
        both(
                messages,
                "mail.matchLifecycle.cancelled.notice",
                "Your reservation is no longer active for this event.",
                "Tu reserva ya no sigue activa para este evento.");
        both(
                messages,
                "mail.matchLifecycle.cancelled.subject",
                "Event cancelled: {0}",
                "Evento cancelado: {0}");

        en(messages, "mail.moderation.ban.eyebrow", "Moderation notice");
        en(messages, "mail.moderation.ban.title", "Your account is temporarily banned");
        en(messages, "mail.moderation.ban.summary", "Review the details below.");
        en(messages, "mail.moderation.ban.username", "User");
        en(messages, "mail.moderation.ban.until", "Banned until");
        en(messages, "mail.moderation.ban.reason", "Reason");
        en(messages, "mail.moderation.ban.login", "Sign in");
        en(messages, "mail.moderation.ban.subject", "Your account has been temporarily banned");

        return messages;
    }

    private static void en(
            final StaticMessageSource messages, final String code, final String text) {
        messages.addMessage(code, Locale.ENGLISH, text);
    }

    private static void both(
            final StaticMessageSource messages,
            final String code,
            final String english,
            final String spanish) {
        messages.addMessage(code, Locale.ENGLISH, english);
        messages.addMessage(code, SPANISH, spanish);
    }
}
