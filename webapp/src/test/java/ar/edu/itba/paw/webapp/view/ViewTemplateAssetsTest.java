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
    void sharedHeadDoesNotLoadBrowserTimezoneScript() throws IOException {
        final String head = read("src/main/webapp/WEB-INF/views/includes/head.jspf");

        assertFalse(head.contains("/js/timezone-field.js"));
        assertTrue(head.contains("/css/auth.css"));
        assertTrue(head.contains("/js/overflow-menu.js"));
        assertTrue(head.contains("/js/host-create-match.js"));
        assertTrue(head.contains("/js/event-map.js"));
        assertTrue(head.contains("/js/event-detail-host-actions.js"));
    }

    @Test
    void hostCreateMatchUsesPlatformTimezoneInsteadOfBrowserTimezoneField() throws IOException {
        final String hostCreateMatch = read("src/main/webapp/WEB-INF/views/host/create-match.jsp");
        final String buttonTag = read("src/main/webapp/WEB-INF/tags/button.tag");

        assertFalse(hostCreateMatch.contains("data-browser-timezone-field=\"true\""));
        assertFalse(hostCreateMatch.contains("name=\"tz\""));
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
        assertTrue(head.contains("/js/file-upload-preview.js"));
        assertTrue(head.contains("/js/events-toggle-filter.js"));
        assertTrue(head.contains("/js/host-create-match.js"));
    }

    @Test
    void imageUploadsExposeClientSidePreviewHooks() throws IOException {
        final String accountIndex = read("src/main/webapp/WEB-INF/views/account/index.jsp");
        final String hostCreateMatch = read("src/main/webapp/WEB-INF/views/host/create-match.jsp");
        final String hostTournamentCreate =
                read("src/main/webapp/WEB-INF/views/host/tournaments/create.jsp");
        final String script = read("src/main/webapp/js/file-upload-preview.js");
        final String authCss = read("src/main/webapp/css/auth.css");
        final String hostCreateCss = read("src/main/webapp/css/host-create.css");

        assertTrue(accountIndex.contains("data-image-preview-container=\"true\""));
        assertTrue(accountIndex.contains("data-image-preview-input=\"true\""));
        assertTrue(accountIndex.contains("accept=\"image/*\""));
        assertTrue(accountIndex.contains("image-upload-preview--profile"));
        assertTrue(hostCreateMatch.contains("id=\"match-banner-image\""));
        assertTrue(hostCreateMatch.contains("data-image-preview-container=\"true\""));
        assertTrue(hostCreateMatch.contains("data-image-preview-input=\"true\""));
        assertTrue(hostCreateMatch.contains("accept=\"image/*\""));
        assertTrue(hostCreateMatch.contains("path=\"bannerImage\""));
        assertTrue(hostCreateMatch.contains("auth-notice auth-notice--error upload-card__error"));
        assertTrue(hostCreateMatch.contains("image-upload-preview--banner"));
        assertTrue(hostTournamentCreate.contains("id=\"tournament-banner-image\""));
        assertTrue(hostTournamentCreate.contains("data-image-preview-container=\"true\""));
        assertTrue(hostTournamentCreate.contains("data-image-preview-input=\"true\""));
        assertTrue(hostTournamentCreate.contains("accept=\"image/*\""));
        assertTrue(hostTournamentCreate.contains("path=\"bannerImage\""));
        assertTrue(
                hostTournamentCreate.contains("auth-notice auth-notice--error upload-card__error"));
        assertTrue(hostTournamentCreate.contains("image-upload-preview--banner"));
        assertTrue(script.contains("URL.createObjectURL"));
        assertTrue(script.contains("URL.revokeObjectURL"));
        assertTrue(script.contains("data-image-preview-input"));
        assertTrue(authCss.contains(".image-upload-preview--profile"));
        assertTrue(authCss.contains("border-radius: 999px"));
        assertTrue(hostCreateCss.contains(".image-upload-preview--banner"));
        assertTrue(hostCreateCss.contains("border-radius: 12px"));
    }

    @Test
    void hostBannerImageValidationMessagesAreLocalized() throws IOException {
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertEquals(
                "Please upload a JPG, PNG, WEBP, or GIF image.",
                english.getProperty("host.form.bannerImage.error.invalidFormat"));
        assertEquals(
                "Sub\u00ed una imagen en formato JPG, PNG, WEBP o GIF.",
                spanish.getProperty("host.form.bannerImage.error.invalidFormat"));
        assertEquals(
                "The uploaded image is empty.",
                english.getProperty("host.form.bannerImage.error.empty"));
        assertEquals(
                "La imagen subida est\u00e1 vac\u00eda.",
                spanish.getProperty("host.form.bannerImage.error.empty"));
        assertEquals(
                "The uploaded image must be 5 MB or smaller.",
                english.getProperty("host.form.bannerImage.error.tooLarge"));
        assertEquals(
                "La imagen subida debe pesar 5 MB o menos.",
                spanish.getProperty("host.form.bannerImage.error.tooLarge"));
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
    void feedDoesNotSubmitBrowserTimezone() throws IOException {
        final String feedIndex = read("src/main/webapp/WEB-INF/views/feed/index.jsp");

        assertEquals(0, countOccurrences(feedIndex, "data-browser-timezone-field=\"true\""));
        assertFalse(feedIndex.contains("name=\"tz\""));
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
    void feedClearAllKeepsEventTypeAndFilter() throws IOException {
        final String feedIndex = read("src/main/webapp/WEB-INF/views/feed/index.jsp");
        final int clearAllIndex = feedIndex.indexOf("var=\"clearFiltersHref\"");
        final int clearAllLabelIndex = feedIndex.indexOf("var=\"clearAllLabel\"");

        assertTrue(clearAllIndex >= 0);
        assertTrue(clearAllLabelIndex > clearAllIndex);
        assertTrue(feedIndex.contains("<c:set var=\"feedPath\" value=\"/\" />"));
        assertTrue(feedIndex.contains("<c:url var=\"clearFiltersHref\" value=\"${feedPath}\">"));
        assertTrue(feedIndex.contains("<c:param name=\"type\" value=\"${selectedType}\" />"));
        assertTrue(
                feedIndex.contains(
                        "<c:param name=\"filter\" value=\"${searchForm.filterName}\" />"));
        final String clearAllBlock = feedIndex.substring(clearAllIndex, clearAllLabelIndex);
        assertFalse(clearAllBlock.contains("name=\"sort\""));
        assertFalse(clearAllBlock.contains("name=\"startDate\""));
        assertFalse(clearAllBlock.contains("name=\"endDate\""));
        assertFalse(clearAllBlock.contains("name=\"minPrice\""));
        assertFalse(clearAllBlock.contains("name=\"maxPrice\""));
    }

    @Test
    void eventsClearAllKeepsEventTypeSortAndFilterState() throws IOException {
        final String eventsList = read("src/main/webapp/WEB-INF/views/events/list.jsp");
        final int clearAllIndex = eventsList.indexOf("var=\"clearSearchHref\"");
        final int clearAllLabelIndex = eventsList.indexOf("var=\"clearAllLabel\"");

        assertTrue(clearAllIndex >= 0);
        assertTrue(clearAllLabelIndex > clearAllIndex);
        assertTrue(
                eventsList.contains(
                        "<c:url var=\"clearSearchHref\" value=\"${listControls.cleanSearchAction}\">"));
        assertTrue(eventsList.contains("<c:param name=\"type\" value=\"${searchForm.type}\" />"));
        assertTrue(
                eventsList.contains(
                        "<c:param name=\"filter\" value=\"${searchForm.filterName}\" />"));
        final String clearAllBlock = eventsList.substring(clearAllIndex, clearAllLabelIndex);
        assertFalse(clearAllBlock.contains("name=\"sort\""));
        assertFalse(clearAllBlock.contains("name=\"startDate\""));
        assertFalse(clearAllBlock.contains("name=\"endDate\""));
        assertFalse(clearAllBlock.contains("name=\"tz\""));
        assertFalse(clearAllBlock.contains("name=\"minPrice\""));
        assertFalse(clearAllBlock.contains("name=\"maxPrice\""));
    }

    @Test
    void feedEventTypeFilterRendersAsIconToggle() throws IOException {
        final String feedIndex = read("src/main/webapp/WEB-INF/views/feed/index.jsp");
        final String feedCss = read("src/main/webapp/css/feed.css");
        final String eventsToggleScript = read("src/main/webapp/js/events-toggle-filter.js");
        final String toggleTag = read("src/main/webapp/WEB-INF/tags/eventsFilterToggle.tag");

        assertTrue(feedIndex.contains("var=\"eventTypeFilterTitle\" code=\"filter.eventType\""));
        assertTrue(feedIndex.contains("<ui:eventsFilterToggle"));
        assertTrue(feedIndex.contains("className=\"feed-event-type-toggle\""));
        assertTrue(feedIndex.contains("leftIcon=\"ball\""));
        assertTrue(feedIndex.contains("rightIcon=\"trophy\""));
        assertTrue(feedIndex.contains("iconOnly=\"${true}\""));
        assertFalse(feedIndex.contains("data-filter-name=\"${eventTypeFilterTitle}\""));
        assertTrue(feedCss.contains(".feed-event-type-toggle"));
        assertTrue(feedCss.contains(".feed-event-type-toggle .events-toggle-icon"));
        assertTrue(eventsToggleScript.contains("toUpperCase()"));
        assertTrue(eventsToggleScript.contains("optionCount === 2 && selectedIndex === 1"));
        assertTrue(toggleTag.contains("iconOnly"));
        assertTrue(toggleTag.contains("leftIcon"));
    }

    @Test
    void hostVisibilityToggleRestoresPublicJoinPolicyValue() throws IOException {
        final String hostCreateScript = read("src/main/webapp/js/host-create-match.js");
        final String hostCreateMatch = read("src/main/webapp/WEB-INF/views/host/create-match.jsp");
        final String head = read("src/main/webapp/WEB-INF/views/includes/head.jspf");

        assertTrue(hostCreateMatch.contains("selectedVisibility"));
        assertTrue(hostCreateMatch.contains("currentValue=\"${selectedVisibility}\""));
        assertTrue(hostCreateMatch.contains("currentValue=\"${selectedJoinPolicy}\""));
        assertTrue(hostCreateMatch.contains("selectedVisibility eq 'private'"));
        assertTrue(hostCreateScript.contains("var lastPublicJoinPolicy"));
        assertTrue(hostCreateScript.contains("function isPublicJoinPolicy"));
        assertTrue(
                hostCreateScript.contains(
                        "syncSegmentedToggle(joinPolicyToggle, joinPolicyInput, \"\")"));
        assertTrue(
                hostCreateScript.contains(
                        "syncSegmentedToggle(joinPolicyToggle, joinPolicyInput, lastPublicJoinPolicy || defaultPublicJoinPolicy)"));
        assertTrue(head.contains("/js/host-create-match.js?v=20260606"));
    }

    @Test
    void sortSelectDoesNotMutateOptionUrlsWithBrowserTimezone() throws IOException {
        final String sortSelectTag = read("src/main/webapp/WEB-INF/tags/sortSelect.tag");
        final String filterDropdowns = read("src/main/webapp/js/filter-dropdowns.js");

        assertFalse(sortSelectTag.contains("data-browser-timezone-url-link=\"true\""));
        assertTrue(sortSelectTag.contains("data-close-on-select=\"true\""));
        assertTrue(sortSelectTag.contains("aria-expanded=\"false\""));
        assertTrue(sortSelectTag.contains("aria-current="));
        assertTrue(filterDropdowns.contains("data-close-on-select"));
        assertTrue(filterDropdowns.contains("sessionStorage.removeItem('pawOpenFilter')"));
        assertFalse(sortSelectTag.contains("aria-haspopup=\"listbox\""));
        assertFalse(sortSelectTag.contains("role=\"listbox\""));
        assertFalse(sortSelectTag.contains("role=\"option\""));
    }

    @Test
    void timezoneFieldScriptIsRemoved() {
        final Path scriptPath = Path.of("src/main/webapp/js/timezone-field.js");

        assertFalse(Files.exists(scriptPath));
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
    void siteHeaderDecidesNavigationFromRequestAndSecurityContext() throws IOException {
        final String siteHeader = read("src/main/webapp/WEB-INF/views/includes/site-header.jspf");

        assertTrue(siteHeader.contains("requestScope['javax.servlet.forward.request_uri']"));
        assertTrue(siteHeader.contains("pageContext.request.requestURI"));
        assertTrue(siteHeader.contains("sec:authorize access=\"isAnonymous()\""));
        assertTrue(siteHeader.contains("sec:authorize access=\"isAuthenticated()\""));
        assertTrue(siteHeader.contains("sec:authorize access=\"hasRole('ADMIN_MOD')\""));
        assertTrue(siteHeader.contains("nav.explore"));
        assertTrue(siteHeader.contains("nav.player.events"));
        assertTrue(siteHeader.contains("nav.hostAMatch"));
        assertTrue(siteHeader.contains("nav.hostATournament"));
        assertTrue(siteHeader.contains("nav.profile"));
        assertTrue(siteHeader.contains("nav.player.reports"));
        assertTrue(siteHeader.contains("nav.admin.reports"));
        assertTrue(siteHeader.contains("nav.login"));
        assertTrue(siteHeader.contains("nav.register"));
        assertTrue(siteHeader.contains("nav.logout"));
        assertFalse(siteHeader.contains("${shell."));
        assertFalse(siteHeader.contains("ShellViewModelFactory"));
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
        assertFalse(hostCreateMatch.contains("data-location-current=\"true\""));
        assertTrue(hostCreateMatch.contains("data-location-clear=\"true\""));
        assertFalse(hostCreateMatch.contains("data-location-unavailable-message"));
        assertFalse(hostCreateMatch.contains("data-location-current-status"));
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
        assertFalse(english.containsKey("host.form.location.current"));
        assertFalse(spanish.containsKey("host.form.location.current"));
    }

    @Test
    void hostTournamentCreateUsesSharedLocationPickerSection() throws IOException {
        final String hostTournamentCreate =
                read("src/main/webapp/WEB-INF/views/host/tournaments/create.jsp");

        assertTrue(hostTournamentCreate.contains("data-location-picker=\"true\""));
        assertTrue(
                hostTournamentCreate.contains(
                        "data-latitude-input=\"#tournament-location-latitude\""));
        assertTrue(
                hostTournamentCreate.contains(
                        "data-longitude-input=\"#tournament-location-longitude\""));
        assertTrue(hostTournamentCreate.contains("data-location-map=\"true\""));
        assertTrue(hostTournamentCreate.contains("data-location-zoom-in=\"true\""));
        assertTrue(hostTournamentCreate.contains("data-location-zoom-out=\"true\""));
        assertFalse(hostTournamentCreate.contains("data-location-current=\"true\""));
        assertTrue(hostTournamentCreate.contains("data-location-clear=\"true\""));
        assertTrue(hostTournamentCreate.contains("host.form.location.map"));
        assertTrue(hostTournamentCreate.contains("host.form.location.map.aria"));
        assertTrue(hostTournamentCreate.contains("mapDefaultLatitude"));
        assertTrue(hostTournamentCreate.contains("mapDefaultLongitude"));
        assertFalse(hostTournamentCreate.contains("${pageContext.request.contextPath}"));
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
        assertTrue(script.contains("window.MatchPointLocationPicker"));
        assertTrue(script.contains("data-latitude-input"));
        assertTrue(script.contains("location-picker:change"));
        assertTrue(script.contains("data-location-zoom-in"));
        assertTrue(script.contains("data-location-zoom-out"));
        assertTrue(script.contains("data-location-picker"));
        assertFalse(script.contains("data-location-current"));
        assertFalse(script.contains("map.locate"));
        assertFalse(script.contains("locationfound"));
        assertFalse(script.contains("isSecureContext"));
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
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(feedIndex.contains("/explore/location"));
        assertTrue(sortSelectTag.contains("data-sort-select=\"true\""));
        assertTrue(feedIndex.contains("data-explore-location-form=\"true\""));
        assertTrue(feedIndex.contains("data-explore-location-modal=\"true\""));
        assertTrue(feedIndex.contains("data-explore-location-confirm=\"true\""));
        assertTrue(feedIndex.contains("data-location-picker-deferred=\"true\""));
        assertTrue(feedIndex.contains("data-latitude-input=\"#explore-location-latitude\""));
        assertTrue(feedIndex.contains("feed.locationPicker.title"));
        assertFalse(feedIndex.contains("data-location-unavailable-message"));
        assertFalse(feedIndex.contains("location.current.unavailable"));
        assertTrue(feedIndex.contains("near-me-panel--hidden"));
        assertFalse(feedIndex.contains("data-explore-location-submit=\"true\""));
        assertTrue(feedIndex.contains("event.distanceLabel"));
        assertTrue(feedIndex.contains("sortOptions"));
        assertTrue(Files.exists(scriptPath));
        assertTrue(script.contains("navigator.geolocation"));
        assertTrue(script.contains("isSecureContext"));
        assertTrue(script.contains("PERMISSION_DENIED"));
        assertTrue(script.contains("openManualPicker"));
        assertTrue(script.contains(".sort-panel__item"));
        assertTrue(script.contains("locationAvailable === 'true'"));
        assertNotNull(english.getProperty("feed.locationPicker.title"));
        assertNotNull(spanish.getProperty("feed.locationPicker.title"));
        assertFalse(english.containsKey("location.current.unavailable"));
        assertFalse(spanish.containsKey("location.current.unavailable"));
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
    void submitGuardRestoresButtonStateWhenPageIsRestoredFromHistory() throws IOException {
        final String script = read("src/main/webapp/js/form-submit-guard.js");

        assertTrue(script.contains("window.addEventListener(\"pageshow\""));
        assertTrue(script.contains("delete form.dataset.submitting"));
        assertTrue(script.contains("restoreSubmitButtons(form)"));
        assertTrue(
                script.indexOf("disableSubmitButtons(form)")
                        < script.indexOf("updateLoadingLabel(form)"));
    }

    @Test
    void matchDetailUsesHostActionCardInsteadOfOverflowMenuForHostActions() throws IOException {
        final String detailView = read("src/main/webapp/WEB-INF/views/matches/detail.jsp");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(detailView.contains("host-action-card"));
        assertTrue(detailView.contains("event.host.action.editSeries"));
        assertTrue(detailView.contains("event.host.action.cancelSeries"));
        assertFalse(detailView.contains("<ui:overflowMenu"));
        assertFalse(detailView.contains("host.manage"));
        assertFalse(detailView.contains("overflow-menu__item--danger"));
        assertTrue(detailView.contains("hostSeriesEditPath"));
        assertTrue(detailView.contains("hostSeriesCancelPath"));
        assertEquals("Edit series", english.getProperty("event.host.action.editSeries"));
        assertEquals("Cancel series", english.getProperty("event.host.action.cancelSeries"));
        assertEquals("Editar serie", spanish.getProperty("event.host.action.editSeries"));
        assertEquals("Cancelar serie", spanish.getProperty("event.host.action.cancelSeries"));
    }

    @Test
    void tournamentDetailIncludesLocalizedHostCancelAction() throws IOException {
        final String detailView = read("src/main/webapp/WEB-INF/views/tournaments/detail.jsp");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(detailView.contains("editTournamentPath"));
        assertTrue(detailView.contains("tournament.host.edit"));
        assertTrue(detailView.contains("cancelTournamentPath"));
        assertTrue(detailView.contains("tournament.host.cancel"));
        assertTrue(detailView.contains("tournament.host.cancel.loading"));
        assertTrue(detailView.contains("closeRegistrationPath"));
        assertTrue(detailView.contains("tournament.host.closeRegistration"));
        assertTrue(detailView.contains("tournament.detail.registrationWindow.startsAt"));
        assertTrue(detailView.contains("tournament.detail.registrationWindow.endsAt"));
        assertTrue(detailView.contains("tournamentPage.closeRegistrationDisabled"));
        assertTrue(detailView.contains("tournamentPage.closeRegistrationDisabledMessage"));
        assertTrue(
                detailView.contains(
                        "variant=\"primary\" disabled=\"${tournamentPage.closeRegistrationDisabled}\""));
        assertTrue(detailView.contains("booking-panel__notice--info"));
        assertTrue(detailView.contains("variant=\"danger\""));
        assertTrue(
                detailView.indexOf("closeRegistrationPath")
                        < detailView.indexOf("editTournamentPath"));
        assertTrue(
                detailView.indexOf("editTournamentPath")
                        < detailView.indexOf("cancelTournamentPath"));
        assertEquals("Edit tournament", english.getProperty("tournament.host.edit"));
        assertEquals("Cancel tournament", english.getProperty("tournament.host.cancel"));
        assertEquals(
                "Close registration", english.getProperty("tournament.host.closeRegistration"));
        assertEquals(
                "Not enough players to close registration. Wait for more players or cancel the tournament.",
                english.getProperty("tournament.host.closeRegistration.unavailable"));
        assertEquals("Tournament updated.", english.getProperty("tournament.host.edit.success"));
        assertEquals(
                "Tournament cancelled.", english.getProperty("tournament.host.cancel.success"));
        assertEquals("Editar torneo", spanish.getProperty("tournament.host.edit"));
        assertEquals("Cancelar torneo", spanish.getProperty("tournament.host.cancel"));
        assertEquals(
                "Cerrar inscripci\u00f3n",
                spanish.getProperty("tournament.host.closeRegistration"));
        assertEquals(
                "No hay suficientes jugadores para cerrar la inscripci\u00f3n. Esper\u00e1 a que se sumen m\u00e1s jugadores o cancel\u00e1 el torneo.",
                spanish.getProperty("tournament.host.closeRegistration.unavailable"));
        assertEquals("Torneo actualizado.", spanish.getProperty("tournament.host.edit.success"));
        assertEquals("Torneo cancelado.", spanish.getProperty("tournament.host.cancel.success"));
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
                read("src/main/webapp/WEB-INF/views/host/participation/aggregate-requests.jsp");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(requestView.contains("host.requests.all.description"));
        assertTrue(requestView.contains("host.requests.seriesBadge"));
        assertEquals("Join requests", english.getProperty("nav.host.joinRequests"));
        assertEquals("Recurring dates", english.getProperty("host.requests.seriesBadge"));
        assertEquals("Solicitudes de ingreso", spanish.getProperty("nav.host.joinRequests"));
        assertEquals("Fechas recurrentes", spanish.getProperty("host.requests.seriesBadge"));
    }

    @Test
    void hostInvitesIncludesLocalizedSeriesInviteOption() throws IOException {
        final String inviteView = read("src/main/webapp/WEB-INF/views/matches/detail.jsp");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(inviteView.contains("hostSeriesInviteAvailable"));
        assertTrue(inviteView.contains("name=\"inviteSeries\""));
        assertTrue(inviteView.contains("host.invites.inviteSeries"));
        assertEquals(
                "Invite to all dates in this series",
                english.getProperty("host.invites.inviteSeries"));
        assertEquals(
                "Invitar a todas las fechas de esta serie",
                spanish.getProperty("host.invites.inviteSeries"));
    }

    @Test
    void matchDetailHostManagementListsUseScopedLabelsAndOptionalProfileLinks() throws IOException {
        final String detailView = read("src/main/webapp/WEB-INF/views/matches/detail.jsp");

        assertTrue(detailView.contains("aria-labelledby=\"pending-requests-title\""));
        assertTrue(detailView.contains("aria-labelledby=\"pending-invitations-title\""));
        assertTrue(detailView.contains("<c:when test=\"${not empty req.profileHref}\">"));
        assertTrue(detailView.contains("<c:when test=\"${not empty invite.profileHref}\">"));
    }

    @Test
    void matchDetailPaginatesRecurringSchedule() throws IOException {
        final String detailView = read("src/main/webapp/WEB-INF/views/matches/detail.jsp");
        final String eventDetailCss = read("src/main/webapp/css/event-detail.css");
        final Properties english = properties("src/main/resources/i18n/messages.properties");
        final Properties spanish = properties("src/main/resources/i18n/messages_es.properties");

        assertTrue(detailView.contains("recurrencePaginationItems"));
        assertTrue(detailView.contains("feed-pagination"));
        assertTrue(detailView.contains("recurrenceHasPreviousPage"));
        assertTrue(detailView.contains("recurrenceHasNextPage"));
        assertTrue(detailView.contains("<c:when test=\"${not empty occurrence.href}\">"));
        assertTrue(detailView.contains("recurrence-schedule__text"));
        assertTrue(eventDetailCss.contains(".recurrence-schedule__text"));
        assertTrue(detailView.contains("code=\"event.recurrence.pagination.aria\""));
        assertTrue(
                detailView.contains(
                        "<section class=\"feed-pagination\" aria-label=\"${recurrenceScheduleTitle}\">"));
        assertTrue(
                detailView.contains(
                        "<nav class=\"feed-pagination__nav\" aria-label=\"${recurrenceScheduleTitle}\">"));
        assertTrue(
                detailView.contains(
                        "<span class=\"feed-pagination__ellipsis\" aria-hidden=\"true\">${item.label}</span>"));
        assertFalse(detailView.contains("aria-label=\"\""));
        assertEquals("Previous", english.getProperty("pagination.previous"));
        assertEquals("Next", english.getProperty("pagination.next"));
        assertEquals("Anterior", spanish.getProperty("pagination.previous"));
        assertEquals("Siguiente", spanish.getProperty("pagination.next"));
        assertNotNull(english.getProperty("event.recurrence.pagination.aria"));
        assertNotNull(spanish.getProperty("event.recurrence.pagination.aria"));
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
        assertTrue(toggleTag.contains("iconOnly"));
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
