package ar.edu.itba.paw.webapp.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class ViewTemplateAssetsTest {

    @Test
    void sharedHeadLoadsTimezoneFieldScript() throws IOException {
        final String head = read("src/main/webapp/WEB-INF/views/includes/head.jspf");

        assertTrue(head.contains("/js/timezone-field.js"));
        assertTrue(head.contains("/css/auth.css"));
        assertTrue(head.contains("/js/overflow-menu.js"));
    }

    @Test
    void hostCreateMatchUsesSharedTimezoneScriptInsteadOfLegacyPageScript() throws IOException {
        final String hostCreateMatch = read("src/main/webapp/WEB-INF/views/host/create-match.jsp");

        assertTrue(hostCreateMatch.contains("data-browser-timezone-field=\"true\""));
        assertFalse(hostCreateMatch.contains("/js/create-match.js"));
    }

    @Test
    void hostCreateMatchIncludesLocalizedRecurrenceControls() throws IOException {
        final String hostCreateMatch = read("src/main/webapp/WEB-INF/views/host/create-match.jsp");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(hostCreateMatch.contains("path=\"recurrenceFrequency\""));
        assertTrue(hostCreateMatch.contains("host.form.recurrence.frequency"));
        assertTrue(hostCreateMatch.contains("path=\"recurrenceEndMode\""));
        assertTrue(hostCreateMatch.contains("path=\"recurrenceUntilDate\""));
        assertTrue(hostCreateMatch.contains("path=\"recurrenceOccurrenceCount\""));
        assertTrue(hostCreateMatch.contains("host.form.recurrence.endMode"));
        assertEquals("Repeat every", english.getProperty("host.form.recurrence.frequency"));
        assertEquals("Repetir cada", spanish.getProperty("host.form.recurrence.frequency"));
        assertEquals("Recurrence ends", english.getProperty("host.form.recurrence.endMode"));
        assertEquals("La recurrencia termina", spanish.getProperty("host.form.recurrence.endMode"));
    }

    @Test
    void feedTimezoneInputsUseBrowserTimezoneFieldHook() throws IOException {
        final String feedIndex = read("src/main/webapp/WEB-INF/views/feed/index.jsp");

        assertEquals(3, countOccurrences(feedIndex, "data-browser-timezone-field=\"true\""));
    }

    @Test
    void timezoneFieldScriptExistsAndTargetsBrowserTimezoneHook() throws IOException {
        final Path scriptPath = Path.of("src/main/webapp/js/timezone-field.js");

        assertTrue(Files.exists(scriptPath));
        assertTrue(Files.readString(scriptPath).contains("data-browser-timezone-field"));
    }

    @Test
    void overflowMenuScriptExistsAndTargetsOverflowMenuHook() throws IOException {
        final Path scriptPath = Path.of("src/main/webapp/js/overflow-menu.js");

        assertTrue(Files.exists(scriptPath));
        assertTrue(Files.readString(scriptPath).contains("data-overflow-menu"));
    }

    @Test
    void matchDetailUsesOverflowMenuForHostActions() throws IOException {
        final String detailView = read("src/main/webapp/WEB-INF/views/matches/detail.jsp");

        assertTrue(detailView.contains("<ui:overflowMenu"));
        assertTrue(detailView.contains("host.manage.menu.trigger"));
        assertTrue(detailView.contains("overflow-menu__item--danger"));
        assertFalse(detailView.contains("label=\"${hostManageEditLabel}\""));
        assertFalse(detailView.contains("label=\"${hostManageCancelLabel}\""));
    }

    @Test
    void matchDetailIncludesLocalizedRecurringCancellationControls() throws IOException {
        final String detailView = read("src/main/webapp/WEB-INF/views/matches/detail.jsp");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(detailView.contains("seriesReservationCancelPath"));
        assertTrue(detailView.contains("event.recurringReservation.leave"));
        assertTrue(detailView.contains("event.recurringReservation.cancelled"));
        assertEquals(
                "Leave recurring dates", english.getProperty("event.recurringReservation.leave"));
        assertEquals(
                "Dejar fechas recurrentes",
                spanish.getProperty("event.recurringReservation.leave"));
    }

    @Test
    void matchDetailIncludesLocalizedReservationCancellationControls() throws IOException {
        final String detailView = read("src/main/webapp/WEB-INF/views/matches/detail.jsp");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(detailView.contains("reservationCancelPath"));
        assertTrue(detailView.contains("event.booking.leave"));
        assertTrue(detailView.contains("event.booking.cancelled"));
        assertEquals("Leave event", english.getProperty("event.booking.leave"));
        assertEquals("Dejar evento", spanish.getProperty("event.booking.leave"));
    }

    @Test
    void overflowMenuTagExists() {
        assertTrue(Files.exists(Path.of("src/main/webapp/WEB-INF/tags/overflowMenu.tag")));
    }

    @Test
    void authCssExists() {
        assertTrue(Files.exists(Path.of("src/main/webapp/css/auth.css")));
    }

    private static String read(final String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private static Properties properties(final String relativePath) throws IOException {
        final Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of(relativePath))) {
            properties.load(reader);
        }
        return properties;
    }

    private static int countOccurrences(final String input, final String token) {
        int count = 0;
        int index = 0;
        while ((index = input.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }
}
