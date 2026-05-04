package ar.edu.itba.paw.webapp.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertTrue(head.contains("/js/host-create-match.js"));
        assertTrue(head.contains("/js/recurrence-schedule.js"));
        assertTrue(head.contains("/js/event-map.js"));
    }

    @Test
    void hostCreateMatchUsesSharedTimezoneScriptInsteadOfLegacyPageScript() throws IOException {
        final String hostCreateMatch = read("src/main/webapp/WEB-INF/views/host/create-match.jsp");
        final String buttonTag = read("src/main/webapp/WEB-INF/tags/button.tag");

        assertTrue(hostCreateMatch.contains("data-browser-timezone-field=\"true\""));
        assertFalse(hostCreateMatch.contains("/js/create-match.js"));
        assertTrue(
                hostCreateMatch.contains(
                        "<c:url var=\"resolvedFormAction\" value=\"${formAction}\""));
        assertFalse(hostCreateMatch.contains("${pageContext.request.contextPath}${formAction}"));
        assertFalse(buttonTag.contains("name=\"formAction\""));
        assertTrue(buttonTag.contains("name=\"submitAction\""));
    }

    @Test
    void pageSpecificBehaviorUsesExternalScriptsInsteadOfInlineScripts() throws IOException {
        final String accountIndex = read("src/main/webapp/WEB-INF/views/account/index.jsp");
        final String eventsList = read("src/main/webapp/WEB-INF/views/events/list.jsp");
        final String hostCreateMatch = read("src/main/webapp/WEB-INF/views/host/create-match.jsp");
        final String head = read("src/main/webapp/WEB-INF/views/includes/head.jspf");

        assertFalse(accountIndex.contains("<script>"));
        assertFalse(eventsList.contains("<script>"));
        assertFalse(hostCreateMatch.contains("<script>"));
        assertTrue(head.contains("/js/account-edit-form.js"));
        assertTrue(head.contains("/js/events-toggle-filter.js"));
        assertTrue(head.contains("/js/host-create-match.js"));
    }

    @Test
    void hostCreateMatchUsesExternalScriptInsteadOfInlineBehavior() throws IOException {
        final String hostCreateMatch = read("src/main/webapp/WEB-INF/views/host/create-match.jsp");
        final String hostCreateMatchScript = read("src/main/webapp/js/host-create-match.js");

        assertFalse(hostCreateMatch.contains("<script>"));
        assertTrue(Files.exists(Path.of("src/main/webapp/js/host-create-match.js")));
        assertTrue(hostCreateMatchScript.contains("function initializeSegmentedToggle"));
        assertTrue(hostCreateMatchScript.contains("function initRecurrenceFields"));
    }

    @Test
    void hostCreateMatchDoesNotWrapCustomVisibilityTogglesInBrokenLabels() throws IOException {
        final String hostCreateMatch = read("src/main/webapp/WEB-INF/views/host/create-match.jsp");

        assertFalse(hostCreateMatch.contains("label class=\"field\" for=\"match-visibility\""));
        assertFalse(hostCreateMatch.contains("label class=\"field\" for=\"match-join-policy\""));
        assertTrue(hostCreateMatch.contains("id=\"join-policy-field\""));
    }

    @Test
    void hostCreateMatchIncludesLocalizedRecurrenceControls() throws IOException {
        final String hostCreateMatch = read("src/main/webapp/WEB-INF/views/host/create-match.jsp");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(hostCreateMatch.contains("path=\"recurrenceFrequency\""));
        assertTrue(hostCreateMatch.contains("name=\"recurrenceFrequency\""));
        assertTrue(hostCreateMatch.contains("value=\"${selectedRecurrenceFrequency}\""));
        assertFalse(
                hostCreateMatch.contains(
                        "value=\"<c:out value='${selectedRecurrenceFrequency}' />\""));
        assertTrue(hostCreateMatch.contains("host.form.recurrence.frequency"));
        assertTrue(hostCreateMatch.contains("path=\"recurrenceEndMode\""));
        assertTrue(hostCreateMatch.contains("name=\"recurrenceEndMode\""));
        assertTrue(hostCreateMatch.contains("value=\"${selectedRecurrenceEndMode}\""));
        assertFalse(
                hostCreateMatch.contains(
                        "value=\"<c:out value='${selectedRecurrenceEndMode}' />\""));
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
    void feedPaginationAndPriceValidationCopyUseMessageKeys() throws IOException {
        final String feedIndex = read("src/main/webapp/WEB-INF/views/feed/index.jsp");
        final String filterDropdowns = read("src/main/webapp/js/filter-dropdowns.js");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(feedIndex.contains("code=\"pagination.aria\""));
        assertTrue(feedIndex.contains("code=\"feed.pagination.pages\""));
        assertFalse(feedIndex.contains("aria-label=\"Pagination\""));
        assertFalse(feedIndex.contains("aria-label=\"Feed pages\""));
        assertTrue(filterDropdowns.contains("data-price-range-error"));
        assertFalse(filterDropdowns.contains("To must be greater than from."));
        assertEquals("Pagination", english.getProperty("pagination.aria"));
        assertEquals("Feed pages", english.getProperty("feed.pagination.pages"));
        assertEquals("Paginaci\u00f3n", spanish.getProperty("pagination.aria"));
        assertEquals("P\u00e1ginas del feed", spanish.getProperty("feed.pagination.pages"));
    }

    @Test
    void feedClearAllResetsToPublicExploreWithoutPreservingFilters() throws IOException {
        final String feedIndex = read("src/main/webapp/WEB-INF/views/feed/index.jsp");
        final int clearAllIndex = feedIndex.indexOf("var=\"clearFiltersHref\"");
        final int clearAllLabelIndex = feedIndex.indexOf("var=\"clearAllLabel\"");

        assertTrue(clearAllIndex >= 0);
        assertTrue(clearAllLabelIndex > clearAllIndex);
        assertTrue(feedIndex.contains("<c:set var=\"feedPath\" value=\"/\" />"));
        assertTrue(feedIndex.contains("<c:url var=\"clearFiltersHref\" value=\"${feedPath}\" />"));
        assertFalse(
                feedIndex.contains(
                        "<c:url var=\"clearFiltersHref\" value=\"${feedFormAction}\" />"));
        final String clearAllBlock = feedIndex.substring(clearAllIndex, clearAllLabelIndex);
        assertFalse(clearAllBlock.contains("name=\"sort\""));
        assertFalse(clearAllBlock.contains("name=\"startDate\""));
        assertFalse(clearAllBlock.contains("name=\"endDate\""));
        assertFalse(clearAllBlock.contains("name=\"minPrice\""));
        assertFalse(clearAllBlock.contains("name=\"maxPrice\""));
    }

    @Test
    void sortSelectUpdatesOptionUrlsWithBrowserTimezone() throws IOException {
        final String sortSelectTag = read("src/main/webapp/WEB-INF/tags/sortSelect.tag");
        final String filterDropdowns = read("src/main/webapp/js/filter-dropdowns.js");
        final String timezoneScript = read("src/main/webapp/js/timezone-field.js");

        assertTrue(sortSelectTag.contains("data-browser-timezone-url-link=\"true\""));
        assertTrue(sortSelectTag.contains("data-close-on-select=\"true\""));
        assertTrue(sortSelectTag.contains("aria-expanded=\"false\""));
        assertTrue(sortSelectTag.contains("aria-current="));
        assertTrue(filterDropdowns.contains("data-close-on-select"));
        assertTrue(filterDropdowns.contains("sessionStorage.removeItem('pawOpenFilter')"));
        assertFalse(sortSelectTag.contains("aria-haspopup=\"listbox\""));
        assertFalse(sortSelectTag.contains("role=\"listbox\""));
        assertFalse(sortSelectTag.contains("role=\"option\""));
        assertTrue(timezoneScript.contains("data-browser-timezone-url-link"));
        assertFalse(timezoneScript.contains("data-browser-timezone-url-options"));
        assertTrue(timezoneScript.contains("searchParams.set('tz', timezone)"));
    }

    @Test
    void timezoneFieldScriptExistsAndTargetsBrowserTimezoneHook() throws IOException {
        final Path scriptPath = Path.of("src/main/webapp/js/timezone-field.js");

        assertTrue(Files.exists(scriptPath));
        assertTrue(Files.readString(scriptPath).contains("data-browser-timezone-field"));
    }

    @Test
    void languageSwitcherMarksOnlyExplicitLanguageSwitchesForPersistence() throws IOException {
        final String siteHeader = read("src/main/webapp/WEB-INF/views/includes/site-header.jspf");

        assertTrue(siteHeader.contains("<c:param name=\"persistLang\" value=\"true\" />"));
        assertTrue(
                siteHeader.contains(
                        "queryParam.key ne 'lang' and queryParam.key ne 'persistLang'"));
    }

    @Test
    void hostCreateMatchUsesHiddenCoordinateFieldsWithoutVisibleCoordinateCopy()
            throws IOException {
        final String hostCreateMatch = read("src/main/webapp/WEB-INF/views/host/create-match.jsp");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(hostCreateMatch.contains("path=\"latitude\""));
        assertTrue(hostCreateMatch.contains("path=\"longitude\""));
        assertTrue(hostCreateMatch.contains("data-location-picker=\"true\""));
        assertTrue(hostCreateMatch.contains("data-location-zoom-in=\"true\""));
        assertTrue(hostCreateMatch.contains("data-location-zoom-out=\"true\""));
        assertTrue(hostCreateMatch.contains("data-location-current=\"true\""));
        assertTrue(hostCreateMatch.contains("data-location-clear=\"true\""));
        assertTrue(hostCreateMatch.contains("data-location-unavailable-message"));
        assertTrue(hostCreateMatch.contains("data-location-current-status"));
        assertFalse(hostCreateMatch.contains("latitude.placeholder"));
        assertFalse(hostCreateMatch.contains("longitude.placeholder"));
        assertNotNull(english.getProperty("feed.nearMe"));
        assertNotNull(spanish.getProperty("feed.nearMe"));
        assertNotNull(english.getProperty("host.form.location.map"));
        assertNotNull(spanish.getProperty("host.form.location.map"));
        assertNotNull(english.getProperty("host.form.location.zoomIn"));
        assertNotNull(english.getProperty("host.form.location.zoomOut"));
        assertNotNull(spanish.getProperty("host.form.location.zoomIn"));
        assertNotNull(spanish.getProperty("host.form.location.zoomOut"));
    }

    @Test
    void mapPickerUsesCommittedTileDefaults() throws IOException {
        final Properties local = properties("../config/local.example.properties");
        final Properties pampero = properties("../config/pampero.example.properties");

        assertMapPickerDefaults(local);
        assertMapPickerDefaults(pampero);
    }

    @Test
    void locationPickerUsesLeafletWithCabaBoundsAndZoomControls() throws IOException {
        final String script = read("src/main/webapp/js/location-picker.js");
        final String head = read("src/main/webapp/WEB-INF/views/includes/head.jspf");

        assertTrue(script.contains("MIN_ZOOM = 12"));
        assertTrue(script.contains("MAX_ZOOM = 16"));
        assertTrue(script.contains("CABA_BOUNDS"));
        assertTrue(script.contains("maxBoundsViscosity"));
        assertTrue(script.contains("L.map"));
        assertTrue(script.contains("L.tileLayer"));
        assertTrue(script.contains("L.marker"));
        assertTrue(script.contains("L.divIcon"));
        assertTrue(script.contains("isSecureContext"));
        assertTrue(script.contains("data-location-zoom-in"));
        assertTrue(script.contains("data-location-zoom-out"));
        assertTrue(script.contains("data-location-picker"));
        assertTrue(head.contains("/js/vendor/leaflet.js"));
        assertTrue(head.contains("/css/vendor/leaflet.css"));
    }

    @Test
    void committedTilesIncludeEverySupportedPickerZoom() {
        for (int zoom = 12; zoom <= 16; zoom++) {
            final Path zoomPath = Path.of("src/main/webapp/assets/tiles", String.valueOf(zoom));

            assertTrue(Files.exists(zoomPath), "Missing tile zoom directory " + zoom);
            try (var tiles =
                    Files.find(zoomPath, 3, (path, attrs) -> path.toString().endsWith(".png"))) {
                assertTrue(tiles.findAny().isPresent(), "Missing PNG tiles for zoom " + zoom);
            } catch (final IOException e) {
                throw new AssertionError("Could not inspect tile zoom directory " + zoom, e);
            }
        }
    }

    @Test
    void feedIncludesNearMeGeolocationPostWithoutUrlCoordinates() throws IOException {
        final String feedIndex = read("src/main/webapp/WEB-INF/views/feed/index.jsp");
        final String sortSelectTag = read("src/main/webapp/WEB-INF/tags/sortSelect.tag");
        final Path scriptPath = Path.of("src/main/webapp/js/explore-location.js");
        final String script = Files.readString(scriptPath);

        assertTrue(feedIndex.contains("/explore/location"));
        assertTrue(sortSelectTag.contains("data-sort-select=\"true\""));
        assertTrue(feedIndex.contains("data-explore-location-form=\"true\""));
        assertTrue(feedIndex.contains("data-location-unavailable-message"));
        assertTrue(feedIndex.contains("near-me-panel--hidden"));
        assertFalse(feedIndex.contains("data-explore-location-submit=\"true\""));
        assertTrue(feedIndex.contains("event.distanceLabel"));
        assertTrue(feedIndex.contains("sortOptions"));
        assertTrue(Files.exists(scriptPath));
        assertTrue(script.contains("navigator.geolocation"));
        assertTrue(script.contains("isSecureContext"));
        assertTrue(script.contains(".sort-panel__item"));
        assertTrue(script.contains("locationAvailable !== 'true'"));
    }

    @Test
    void overflowMenuScriptExistsAndTargetsOverflowMenuHook() throws IOException {
        final Path scriptPath = Path.of("src/main/webapp/js/overflow-menu.js");

        assertTrue(Files.exists(scriptPath));
        assertTrue(Files.readString(scriptPath).contains("data-overflow-menu"));
    }

    @Test
    void reportFiltersScriptExistsAndTargetsFilterFormHook() throws IOException {
        final Path scriptPath = Path.of("src/main/webapp/js/report-filters.js");
        final String script = Files.readString(scriptPath);

        assertTrue(Files.exists(scriptPath));
        assertTrue(script.contains("report-filter-form"));
        assertTrue(script.contains("input[type=\"checkbox\"]"));
        assertTrue(script.contains(".submit()"));
    }

    @Test
    void matchDetailUsesOverflowMenuForHostActions() throws IOException {
        final String detailView = read("src/main/webapp/WEB-INF/views/matches/detail.jsp");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(detailView.contains("<ui:overflowMenu"));
        assertTrue(detailView.contains("host.manage.menu.trigger"));
        assertTrue(detailView.contains("overflow-menu__item--danger"));
        assertTrue(detailView.contains("hostSeriesEditPath"));
        assertTrue(detailView.contains("hostSeriesCancelPath"));
        assertTrue(detailView.contains("host.manage.editSeries"));
        assertTrue(detailView.contains("host.manage.cancelSeries"));
        assertFalse(detailView.contains("label=\"${hostManageEditLabel}\""));
        assertFalse(detailView.contains("label=\"${hostManageCancelLabel}\""));
        assertEquals("Edit recurring dates", english.getProperty("host.manage.editSeries"));
        assertEquals(
                "Cancel all upcoming recurring dates",
                english.getProperty("host.manage.cancelSeries"));
        assertEquals("Editar fechas recurrentes", spanish.getProperty("host.manage.editSeries"));
        assertEquals(
                "Cancelar todas las fechas recurrentes pr\u00f3ximas",
                spanish.getProperty("host.manage.cancelSeries"));
    }

    @Test
    void matchDetailIncludesPinnedLocationMapWhenCoordinatesExist() throws IOException {
        final String detailView = read("src/main/webapp/WEB-INF/views/matches/detail.jsp");
        final Path scriptPath = Path.of("src/main/webapp/js/event-map.js");
        final String script = Files.readString(scriptPath);
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(detailView.contains("eventPage.mapAvailable"));
        assertTrue(detailView.contains("data-event-map=\"true\""));
        assertTrue(detailView.contains("data-tile-url-template"));
        assertTrue(detailView.contains("data-latitude"));
        assertTrue(detailView.contains("data-longitude"));
        assertTrue(detailView.contains("event.detail.locationMap.aria"));
        assertTrue(Files.exists(scriptPath));
        assertTrue(script.contains("data-event-map"));
        assertTrue(script.contains("L.map"));
        assertTrue(script.contains("L.tileLayer"));
        assertTrue(script.contains("L.marker"));
        assertTrue(script.contains("CABA_BOUNDS"));
        assertNotNull(english.getProperty("event.detail.locationMap.aria"));
        assertNotNull(spanish.getProperty("event.detail.locationMap.aria"));
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
    void matchDetailIncludesLocalizedRecurringJoinRequestControls() throws IOException {
        final String detailView = read("src/main/webapp/WEB-INF/views/matches/detail.jsp");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(detailView.contains("seriesJoinRequestPath"));
        assertTrue(detailView.contains("event.joinRequest.requestThisOccurrence"));
        assertTrue(detailView.contains("event.recurringJoinRequest.cta"));
        assertTrue(detailView.contains("event.recurringJoinRequest.requested"));
        assertEquals(
                "Request all recurring dates",
                english.getProperty("event.recurringJoinRequest.cta"));
        assertEquals(
                "Solicitar todas las fechas recurrentes",
                spanish.getProperty("event.recurringJoinRequest.cta"));
    }

    @Test
    void hostRequestsIncludesAggregateRecurringRequestCopy() throws IOException {
        final String requestView =
                read("src/main/webapp/WEB-INF/views/host/participation/requests.jsp");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(requestView.contains("aggregateRequests"));
        assertTrue(requestView.contains("host.requests.all.description"));
        assertTrue(requestView.contains("host.requests.seriesBadge"));
        assertEquals("Join requests", english.getProperty("nav.host.joinRequests"));
        assertEquals("Recurring dates", english.getProperty("host.requests.seriesBadge"));
        assertEquals("Solicitudes de ingreso", spanish.getProperty("nav.host.joinRequests"));
        assertEquals("Fechas recurrentes", spanish.getProperty("host.requests.seriesBadge"));
    }

    @Test
    void hostInvitesIncludesLocalizedSeriesInviteOption() throws IOException {
        final String inviteView =
                read("src/main/webapp/WEB-INF/views/host/participation/invites.jsp");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(inviteView.contains("seriesInviteAvailable"));
        assertTrue(inviteView.contains("path=\"inviteSeries\""));
        assertTrue(inviteView.contains("host.invites.inviteSeries"));
        assertEquals(
                "Invite to all dates in this series",
                english.getProperty("host.invites.inviteSeries"));
        assertEquals(
                "Invitar a todas las fechas de esta serie",
                spanish.getProperty("host.invites.inviteSeries"));
    }

    @Test
    void matchDetailCollapsesLongRecurringSchedule() throws IOException {
        final String detailView = read("src/main/webapp/WEB-INF/views/matches/detail.jsp");
        final String eventDetailCss = read("src/main/webapp/css/event-detail.css");
        final String recurrenceScript = read("src/main/webapp/js/recurrence-schedule.js");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(detailView.contains("var=\"recurrencePreviewLimit\" value=\"3\""));
        assertTrue(detailView.contains("data-recurrence-extra-date=\"true\""));
        assertTrue(detailView.contains("hidden=\"hidden\""));
        assertTrue(detailView.contains("and not occurrence.current"));
        assertTrue(detailView.contains("<c:when test=\"${not empty occurrence.href}\">"));
        assertTrue(detailView.contains("recurrence-schedule__text"));
        assertTrue(detailView.contains("data-recurrence-toggle=\"true\""));
        assertTrue(detailView.contains("event.recurrence.showMore"));
        assertTrue(detailView.contains("event.recurrence.showLess"));
        assertTrue(eventDetailCss.contains(".recurrence-schedule__item[hidden]"));
        assertTrue(eventDetailCss.contains(".recurrence-schedule__text"));
        assertTrue(recurrenceScript.contains("data-recurrence-toggle"));
        assertTrue(recurrenceScript.contains("aria-expanded"));
        assertEquals("Show more", english.getProperty("event.recurrence.showMore"));
        assertEquals("Show less", english.getProperty("event.recurrence.showLess"));
        assertEquals("Ver m\u00e1s", spanish.getProperty("event.recurrence.showMore"));
        assertEquals("Ver menos", spanish.getProperty("event.recurrence.showLess"));
    }

    @Test
    void matchDetailIncludesLocalizedReservationCancellationControls() throws IOException {
        final String detailView = read("src/main/webapp/WEB-INF/views/matches/detail.jsp");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(detailView.contains("reservationCancelPath"));
        assertTrue(detailView.contains("event.booking.leave"));
        assertTrue(detailView.contains("event.booking.leaveOccurrence"));
        assertTrue(detailView.contains("event.booking.leavingOccurrence"));
        assertTrue(detailView.contains("event.booking.cancelled"));
        assertTrue(detailView.contains("event.booking.occurrenceCancelled"));
        assertEquals("Leave event", english.getProperty("event.booking.leave"));
        assertEquals("Leave this date", english.getProperty("event.booking.leaveOccurrence"));
        assertEquals(
                "You left this date. Your spot is available again.",
                english.getProperty("event.booking.occurrenceCancelled"));
        assertEquals("Dejar evento", spanish.getProperty("event.booking.leave"));
        assertEquals("Dejar esta fecha", spanish.getProperty("event.booking.leaveOccurrence"));
        assertEquals(
                "Dejaste esta fecha. Tu lugar vuelve a estar disponible.",
                spanish.getProperty("event.booking.occurrenceCancelled"));
    }

    @Test
    void messageBundlesIncludeRecurringLifecycleMailCopy() throws IOException {
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertEquals(
                "Recurring event updated",
                english.getProperty("mail.matchLifecycle.recurringUpdated.eyebrow"));
        assertEquals(
                "Recurring event cancelled",
                english.getProperty("mail.matchLifecycle.recurringCancelled.eyebrow"));
        assertEquals(
                "Evento recurrente actualizado",
                spanish.getProperty("mail.matchLifecycle.recurringUpdated.eyebrow"));
        assertEquals(
                "Evento recurrente cancelado",
                spanish.getProperty("mail.matchLifecycle.recurringCancelled.eyebrow"));
    }

    @Test
    void overflowMenuTagExists() {
        assertTrue(Files.exists(Path.of("src/main/webapp/WEB-INF/tags/overflowMenu.tag")));
    }

    @Test
    void eventsListUsesReusableEventsToggleTag() throws IOException {
        final Path toggleTagPath = Path.of("src/main/webapp/WEB-INF/tags/eventsFilterToggle.tag");
        final String eventsList = read("src/main/webapp/WEB-INF/views/events/list.jsp");
        final String profile = read("src/main/webapp/WEB-INF/views/users/profile.jsp");
        final String toggleTag = read("src/main/webapp/WEB-INF/tags/eventsFilterToggle.tag");

        assertTrue(Files.exists(toggleTagPath));
        assertTrue(eventsList.contains("<ui:eventsFilterToggle"));
        assertTrue(profile.contains("<ui:eventsFilterToggle"));
        assertTrue(profile.contains("id=\"profile-review-filter-toggle\""));
        assertFalse(profile.contains("public-profile-review-filter__options"));
        assertFalse(profile.contains("public-profile-review-filter__option"));
        assertTrue(toggleTag.contains("thirdValue"));
        assertTrue(toggleTag.contains("leftHref"));
        assertTrue(toggleTag.contains("data-events-toggle-options"));
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

    private static void assertMapPickerDefaults(final Properties properties) {
        assertEquals("true", properties.getProperty("map.picker.enabled"));
        assertEquals(
                "/assets/tiles/{z}/{x}/{y}.png", properties.getProperty("map.tiles.urlTemplate"));
        assertNotNull(properties.getProperty("map.tiles.attribution"));
        assertFalse(properties.getProperty("map.tiles.attribution").isBlank());
        assertEquals("-34.6037", properties.getProperty("map.default.latitude"));
        assertEquals("-58.3816", properties.getProperty("map.default.longitude"));
        assertEquals("14", properties.getProperty("map.default.zoom"));
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
