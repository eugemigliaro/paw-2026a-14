package ar.edu.itba.paw.webapp.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
