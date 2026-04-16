package ar.edu.itba.paw.webapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ActionVerificationService;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.VerificationConfirmationResult;
import ar.edu.itba.paw.services.VerificationFailureException;
import ar.edu.itba.paw.services.VerificationFailureReason;
import ar.edu.itba.paw.services.VerificationPreview;
import ar.edu.itba.paw.services.VerificationPreviewDetail;
import ar.edu.itba.paw.services.VerificationRequestResult;
import ar.edu.itba.paw.webapp.viewmodel.WebViewModels.FeedPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.WebViewModels.FilterGroupViewModel;
import ar.edu.itba.paw.webapp.viewmodel.WebViewModels.FilterOptionViewModel;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.Matchers;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class WebRouteTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-10T18:00:00Z");
    private static final ZoneId TEST_ZONE = ZoneId.of("UTC");

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final InternalResourceViewResolver viewResolver = createViewResolver();
        final LocalValidatorFactoryBean validator = createValidator();

        final Match realMatch =
                new Match(
                        42L,
                        Sport.PADEL,
                        7L,
                        "Downtown Club",
                        "Sunrise Padel",
                        "Friendly\\n doubles session",
                        Instant.parse("2026-04-06T10:00:00Z"),
                        Instant.parse("2026-04-06T12:00:00Z"),
                        8,
                        BigDecimal.TEN,
                        "public",
                        "open",
                        2,
                        null);
        final Match footballMatch =
                new Match(
                        43L,
                        Sport.FOOTBALL,
                        8L,
                        "North Arena",
                        "Afterwork Football",
                        "Fast 5v5 session",
                        Instant.parse("2026-04-07T19:00:00Z"),
                        Instant.parse("2026-04-07T20:30:00Z"),
                        10,
                        BigDecimal.ZERO,
                        "public",
                        "open",
                        4,
                        null);
        final Match tennisMatch =
                new Match(
                        44L,
                        Sport.TENNIS,
                        9L,
                        "River Club",
                        "Sunset Tennis",
                        "Singles and doubles rotation",
                        Instant.parse("2026-04-08T18:00:00Z"),
                        Instant.parse("2026-04-08T20:00:00Z"),
                        6,
                        BigDecimal.valueOf(12),
                        "public",
                        "open",
                        2,
                        null);
        final List<Match> feedMatches = List.of(realMatch, footballMatch, tennisMatch);

        final MatchService matchService =
                new MatchService() {
                    @Override
                    public Match createMatch(
                            final ar.edu.itba.paw.services.CreateMatchRequest request) {
                        return new Match(
                                43L,
                                request.getSport(),
                                request.getHostUserId(),
                                request.getAddress(),
                                request.getTitle(),
                                request.getDescription(),
                                request.getStartsAt(),
                                request.getEndsAt(),
                                request.getMaxPlayers(),
                                request.getPricePerPlayer(),
                                request.getVisibility(),
                                request.getStatus(),
                                0,
                                null);
                    }

                    @Override
                    public Optional<Match> findPublicMatchById(final Long matchId) {
                        return matchId == 42L ? Optional.of(realMatch) : Optional.empty();
                    }

                    @Override
                    public List<User> findConfirmedParticipants(final Long matchId) {
                        return matchId == 42L
                                ? List.of(
                                        new User(2L, "first@test.com", "first-player"),
                                        new User(3L, "second@test.com", "second-player"))
                                : List.of();
                    }

                    @Override
                    public PaginatedResult<Match> searchPublicMatches(
                            final String query,
                            final String sport,
                            final String time,
                            final String sort,
                            final int page,
                            final int pageSize,
                            final String timezone) {
                        return searchPublicMatches(
                                query, sport, time, sort, page, pageSize, timezone, null, null);
                    }

                    @Override
                    public PaginatedResult<Match> searchPublicMatches(
                            final String query,
                            final String sport,
                            final String time,
                            final String sort,
                            final int page,
                            final int pageSize,
                            final String timezone,
                            final BigDecimal minPrice,
                            final BigDecimal maxPrice) {
                        return filterFeedMatchesForRouteTests(
                                feedMatches, sport, page, pageSize, minPrice, maxPrice);
                    }
                };

        final UserService userService =
                new UserService() {
                    @Override
                    public User createUser(final String email, final String username) {
                        return new User(9L, email, username);
                    }

                    @Override
                    public Optional<User> findByEmail(final String email) {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<User> findById(final Long id) {
                        return Optional.of(new User(id, "host@test.com", "host-player"));
                    }

                    @Override
                    public Optional<User> findByUsername(final String username) {
                        return Optional.empty();
                    }
                };

        final ActionVerificationService actionVerificationService =
                new ActionVerificationService() {
                    @Override
                    public VerificationRequestResult requestMatchReservation(
                            final Long matchId, final String email) {
                        return new VerificationRequestResult(
                                email, Instant.parse("2026-04-06T18:00:00Z"));
                    }

                    @Override
                    public VerificationRequestResult requestMatchCreation(
                            final ar.edu.itba.paw.services.CreateMatchRequest request,
                            final String email) {
                        return new VerificationRequestResult(
                                email, Instant.parse("2026-04-06T18:00:00Z"));
                    }

                    @Override
                    public VerificationPreview getPreview(final String rawToken) {
                        if ("invalid".equals(rawToken)) {
                            throw new VerificationFailureException(
                                    VerificationFailureReason.NOT_FOUND, "Missing link");
                        }
                        return new VerificationPreview(
                                "Confirm reservation",
                                "Finish the reservation.",
                                "player@test.com",
                                Instant.parse("2026-04-06T18:00:00Z"),
                                "Confirm reservation",
                                "/matches/42?reservation=confirmed",
                                List.of(new VerificationPreviewDetail("Venue", "Downtown Club")));
                    }

                    @Override
                    public VerificationConfirmationResult confirm(final String rawToken) {
                        if ("invalid".equals(rawToken)) {
                            throw new VerificationFailureException(
                                    VerificationFailureReason.NOT_FOUND, "Missing link");
                        }
                        return new VerificationConfirmationResult(
                                9L, "/matches/42?reservation=confirmed", "done");
                    }
                };

        final ImageService imageService =
                new ImageService() {
                    @Override
                    public Long store(
                            final String contentType,
                            final long contentLength,
                            final InputStream contentStream)
                            throws IOException {
                        return 500L;
                    }

                    @Override
                    public Optional<ar.edu.itba.paw.models.ImageMetadata> findMetadataById(
                            final Long imageId) {
                        return Optional.empty();
                    }

                    @Override
                    public boolean streamContentById(
                            final Long imageId, final OutputStream outputStream)
                            throws IOException {
                        return false;
                    }
                };

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new FeedController(matchService),
                                new EventController(
                                        matchService, userService, actionVerificationService),
                                new HostController(
                                        actionVerificationService,
                                        imageService,
                                        Clock.fixed(FIXED_NOW, TEST_ZONE)),
                                new ErrorPageController(),
                                new VerificationController(actionVerificationService))
                        .setViewResolvers(viewResolver)
                        .setValidator(validator)
                        .build();
    }

    @Test
    void getFeedRouteRendersFeedPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"))
                .andExpect(model().attributeExists("shell"))
                .andExpect(model().attributeExists("feedPage"))
                .andExpect(
                        model().attribute(
                                        "feedPage",
                                        Matchers.hasProperty(
                                                "filterGroups",
                                                Matchers.contains(
                                                        Matchers.hasProperty(
                                                                "title",
                                                                Matchers.equalTo("Sports")),
                                                        Matchers.hasProperty(
                                                                "title",
                                                                Matchers.equalTo("Time"))))));
    }

    @Test
    void getFeedRouteOmitsAnySportAndAnyTimeOptions() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"))
                .andExpect(
                        model().attribute(
                                        "feedPage",
                                        Matchers.hasProperty(
                                                "filterGroups",
                                                Matchers.contains(
                                                        Matchers.allOf(
                                                                Matchers.hasProperty(
                                                                        "title",
                                                                        Matchers.equalTo("Sports")),
                                                                Matchers.hasProperty(
                                                                        "options",
                                                                        Matchers.contains(
                                                                                Matchers
                                                                                        .hasProperty(
                                                                                                "label",
                                                                                                Matchers
                                                                                                        .equalTo(
                                                                                                                "Football")),
                                                                                Matchers
                                                                                        .hasProperty(
                                                                                                "label",
                                                                                                Matchers
                                                                                                        .equalTo(
                                                                                                                "Tennis")),
                                                                                Matchers
                                                                                        .hasProperty(
                                                                                                "label",
                                                                                                Matchers
                                                                                                        .equalTo(
                                                                                                                "Basketball")),
                                                                                Matchers
                                                                                        .hasProperty(
                                                                                                "label",
                                                                                                Matchers
                                                                                                        .equalTo(
                                                                                                                "Padel"))))),
                                                        Matchers.allOf(
                                                                Matchers.hasProperty(
                                                                        "title",
                                                                        Matchers.equalTo("Time")),
                                                                Matchers.hasProperty(
                                                                        "options",
                                                                        Matchers.contains(
                                                                                Matchers
                                                                                        .hasProperty(
                                                                                                "label",
                                                                                                Matchers
                                                                                                        .equalTo(
                                                                                                                "Today")),
                                                                                Matchers
                                                                                        .hasProperty(
                                                                                                "label",
                                                                                                Matchers
                                                                                                        .equalTo(
                                                                                                                "Tomorrow")),
                                                                                Matchers
                                                                                        .hasProperty(
                                                                                                "label",
                                                                                                Matchers
                                                                                                        .equalTo(
                                                                                                                "This week")))))))));
    }

    @Test
    void getFeedRoutePreservesMultipleSelectedSports() throws Exception {
        mockMvc.perform(get("/").param("sport", "football", "tennis"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"))
                .andExpect(
                        model().attribute(
                                        "selectedSports", Matchers.contains("football", "tennis")));
    }

    @Test
    void getFeedRoutePreservesCommaSeparatedSelectedSports() throws Exception {
        mockMvc.perform(get("/").param("sport", "football,tennis"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"))
                .andExpect(
                        model().attribute(
                                        "selectedSports", Matchers.contains("football", "tennis")));
    }

    @Test
    void getFeedRouteFiltersFeaturedEventsWhenMultipleSportsAreSelected() throws Exception {
        mockMvc.perform(get("/").param("sport", "football", "tennis"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"))
                .andExpect(
                        model().attribute(
                                        "feedPage",
                                        Matchers.hasProperty(
                                                "featuredEvents",
                                                Matchers.contains(
                                                        Matchers.hasProperty(
                                                                "sport",
                                                                Matchers.equalTo("Football")),
                                                        Matchers.hasProperty(
                                                                "sport",
                                                                Matchers.equalTo("Tennis"))))));
    }

    @Test
    void getFeedRouteBuildsToggleHrefForSelectedSport() throws Exception {
        mockMvc.perform(get("/").param("sport", "padel"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"))
                .andExpect(
                        result -> {
                            final FeedPageViewModel feedPage =
                                    (FeedPageViewModel)
                                            result.getModelAndView().getModel().get("feedPage");
                            final FilterOptionViewModel padelOption =
                                    findFilterOption(feedPage, "Sports", "Padel");

                            assertEquals(
                                    "/?q=&time=all&sort=soonest&page=1", padelOption.getHref());
                            assertTrue(padelOption.isActive());
                        });
    }

    @Test
    void getFeedRouteBuildsToggleHrefForSelectedTime() throws Exception {
        mockMvc.perform(get("/").param("time", "today"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"))
                .andExpect(
                        result -> {
                            final FeedPageViewModel feedPage =
                                    (FeedPageViewModel)
                                            result.getModelAndView().getModel().get("feedPage");
                            final FilterOptionViewModel todayOption =
                                    findFilterOption(feedPage, "Time", "Today");

                            assertEquals(
                                    "/?q=&time=all&sort=soonest&page=1", todayOption.getHref());
                            assertTrue(todayOption.isActive());
                        });
    }

    @Test
    void getFeedRoutePreservesPriceFilters() throws Exception {
        mockMvc.perform(get("/").param("minPrice", "10").param("maxPrice", "25"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"))
                .andExpect(
                        model().attribute(
                                        "selectedMinPrice",
                                        Matchers.comparesEqualTo(new BigDecimal("10"))))
                .andExpect(model().attribute("selectedMinPriceValue", "10"))
                .andExpect(
                        model().attribute(
                                        "selectedMaxPrice",
                                        Matchers.comparesEqualTo(new BigDecimal("25"))))
                .andExpect(model().attribute("selectedMaxPriceValue", "25"));
    }

    @Test
    void getFeedRouteForwardsPriceFiltersToExtendedSearchOverload() throws Exception {
        final AtomicReference<String> capturedSport = new AtomicReference<>();
        final AtomicReference<BigDecimal> capturedMinPrice = new AtomicReference<>();
        final AtomicReference<BigDecimal> capturedMaxPrice = new AtomicReference<>();
        final MatchService capturingMatchService =
                new MatchService() {
                    @Override
                    public Match createMatch(
                            final ar.edu.itba.paw.services.CreateMatchRequest request) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public Optional<Match> findPublicMatchById(final Long matchId) {
                        return Optional.empty();
                    }

                    @Override
                    public List<User> findConfirmedParticipants(final Long matchId) {
                        return List.of();
                    }

                    @Override
                    public PaginatedResult<Match> searchPublicMatches(
                            final String query,
                            final String sport,
                            final String time,
                            final String sort,
                            final int page,
                            final int pageSize,
                            final String timezone) {
                        throw new AssertionError(
                                "Expected controller to use the extended overload");
                    }

                    @Override
                    public PaginatedResult<Match> searchPublicMatches(
                            final String query,
                            final String sport,
                            final String time,
                            final String sort,
                            final int page,
                            final int pageSize,
                            final String timezone,
                            final BigDecimal minPrice,
                            final BigDecimal maxPrice) {
                        capturedSport.set(sport);
                        capturedMinPrice.set(minPrice);
                        capturedMaxPrice.set(maxPrice);
                        return new PaginatedResult<>(List.of(), 0, 1, 12);
                    }
                };
        final MockMvc feedOnlyMockMvc = createFeedMockMvc(capturingMatchService);

        feedOnlyMockMvc
                .perform(get("/").param("sport", "football", "tennis").param("minPrice", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"));

        assertEquals("football,tennis", capturedSport.get());
        assertEquals(0, new BigDecimal("10").compareTo(capturedMinPrice.get()));
        assertEquals(null, capturedMaxPrice.get());
    }

    @Test
    void getFeedRouteFallsBackWhenExtendedSearchOverloadIsUnavailableAtRuntime() throws Exception {
        final Match cheapFootball =
                new Match(
                        51L,
                        Sport.FOOTBALL,
                        7L,
                        "North Arena",
                        "Budget Football",
                        "Fast match",
                        Instant.parse("2026-04-11T18:00:00Z"),
                        Instant.parse("2026-04-11T19:00:00Z"),
                        10,
                        BigDecimal.ZERO,
                        "public",
                        "open",
                        4,
                        null);
        final Match premiumPadel =
                new Match(
                        52L,
                        Sport.PADEL,
                        8L,
                        "Downtown Club",
                        "Premium Padel",
                        "Competitive game",
                        Instant.parse("2026-04-11T20:00:00Z"),
                        Instant.parse("2026-04-11T21:30:00Z"),
                        8,
                        new BigDecimal("20"),
                        "public",
                        "open",
                        2,
                        null);
        final Match premiumTennis =
                new Match(
                        53L,
                        Sport.TENNIS,
                        9L,
                        "River Club",
                        "Premium Tennis",
                        "Evening session",
                        Instant.parse("2026-04-11T22:00:00Z"),
                        Instant.parse("2026-04-11T23:30:00Z"),
                        6,
                        new BigDecimal("12"),
                        "public",
                        "open",
                        2,
                        null);
        final MatchService fallbackMatchService =
                new MatchService() {
                    @Override
                    public Match createMatch(
                            final ar.edu.itba.paw.services.CreateMatchRequest request) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public Optional<Match> findPublicMatchById(final Long matchId) {
                        return Optional.empty();
                    }

                    @Override
                    public List<User> findConfirmedParticipants(final Long matchId) {
                        return List.of();
                    }

                    @Override
                    public PaginatedResult<Match> searchPublicMatches(
                            final String query,
                            final String sport,
                            final String time,
                            final String sort,
                            final int page,
                            final int pageSize,
                            final String timezone) {
                        return new PaginatedResult<>(
                                List.of(cheapFootball, premiumPadel, premiumTennis), 3, 1, 12);
                    }

                    @Override
                    public PaginatedResult<Match> searchPublicMatches(
                            final String query,
                            final String sport,
                            final String time,
                            final String sort,
                            final int page,
                            final int pageSize,
                            final String timezone,
                            final BigDecimal minPrice,
                            final BigDecimal maxPrice) {
                        throw new NoSuchMethodError(
                                "Simulated stale runtime MatchService contract");
                    }
                };
        final MockMvc feedOnlyMockMvc = createFeedMockMvc(fallbackMatchService);

        feedOnlyMockMvc
                .perform(get("/").param("sport", "football", "tennis").param("minPrice", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"))
                .andExpect(
                        model().attribute(
                                        "feedPage",
                                        Matchers.hasProperty(
                                                "featuredEvents",
                                                Matchers.contains(
                                                        Matchers.hasProperty(
                                                                "sport",
                                                                Matchers.equalTo("Tennis"))))));
    }

    @Test
    void getRealEventDetailsRouteRendersEventPage() throws Exception {
        mockMvc.perform(get("/matches/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attributeExists("reservationRequestPath"))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty(
                                                "aboutParagraphs",
                                                Matchers.contains("Friendly\n doubles session"))))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty("participants", Matchers.hasSize(2))));
    }

    @Test
    void postReservationRequestRendersCheckEmailPage() throws Exception {
        mockMvc.perform(post("/matches/42/reservations").param("email", "player@test.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification/check-email"))
                .andExpect(model().attributeExists("title"))
                .andExpect(model().attributeExists("summary"));
    }

    @Test
    void getVerificationPreviewRendersConfirmPage() throws Exception {
        mockMvc.perform(get("/verifications/abc123"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification/confirm"))
                .andExpect(model().attributeExists("preview"));
    }

    @Test
    void postVerificationConfirmRedirectsToEvent() throws Exception {
        mockMvc.perform(post("/verifications/abc123/confirm"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42?reservation=confirmed"));
    }

    @Test
    void getRemovedMockEventRouteReturnsNotFound() throws Exception {
        mockMvc.perform(get("/matches/sunrise-padel-championship"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRemovedComponentPreviewRouteReturnsNotFound() throws Exception {
        mockMvc.perform(get("/ui/components")).andExpect(status().isNotFound());
    }

    @Test
    void getInvalidVerificationRendersErrorPage() throws Exception {
        mockMvc.perform(get("/verifications/invalid"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification/error"))
                .andExpect(model().attributeExists("message"));
    }

    @Test
    void getNotFoundErrorRouteRenders404Page() throws Exception {
        mockMvc.perform(get("/errors/404"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("errors/not-found"))
                .andExpect(model().attributeExists("shell"));
    }

    @Test
    void postHostPublishCreatesAndRedirects() throws Exception {
        mockMvc.perform(
                        post("/host/matches/new")
                                .param("email", "host@test.com")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param(
                                        "eventDate",
                                        FIXED_NOW
                                                .atZone(TEST_ZONE)
                                                .toLocalDate()
                                                .plusDays(1)
                                                .toString())
                                .param("eventTime", "18:00")
                                .param("endTime", "20:00")
                                .param("timezone", "UTC")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification/check-email"))
                .andExpect(model().attributeExists("title"))
                .andExpect(model().attributeExists("summary"));
    }

    @Test
    void postHostPublishWithPastDateShowsValidationError() throws Exception {
        mockMvc.perform(
                        post("/host/matches/new")
                                .param("email", "host@test.com")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param(
                                        "eventDate",
                                        FIXED_NOW
                                                .atZone(TEST_ZONE)
                                                .toLocalDate()
                                                .minusDays(1)
                                                .toString())
                                .param("eventTime", "18:00")
                                .param("endTime", "20:00")
                                .param("timezone", "UTC")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "eventDate"));
    }

    @Test
    void postHostPublishWithPastTimeTodayShowsValidationError() throws Exception {
        mockMvc.perform(
                        post("/host/matches/new")
                                .param("email", "host@test.com")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param(
                                        "eventDate",
                                        FIXED_NOW.atZone(TEST_ZONE).toLocalDate().toString())
                                .param("eventTime", LocalTime.MIDNIGHT.toString())
                                .param("endTime", "19:00")
                                .param("timezone", "UTC")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "eventTime"));
    }

    @Test
    void postHostPublishWithEndTimeBeforeStartTimeShowsValidationError() throws Exception {
        mockMvc.perform(
                        post("/host/matches/new")
                                .param("email", "host@test.com")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param(
                                        "eventDate",
                                        FIXED_NOW
                                                .atZone(TEST_ZONE)
                                                .toLocalDate()
                                                .plusDays(1)
                                                .toString())
                                .param("eventTime", "18:00")
                                .param("endTime", "17:30")
                                .param("timezone", "UTC")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "endTime"));
    }

    private static FilterOptionViewModel findFilterOption(
            final FeedPageViewModel feedPage, final String groupTitle, final String optionLabel) {
        return findFilterGroup(feedPage, groupTitle).getOptions().stream()
                .filter(option -> optionLabel.equals(option.getLabel()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new AssertionError(
                                        "Expected option '%s' in group '%s'"
                                                .formatted(optionLabel, groupTitle)));
    }

    private static FilterGroupViewModel findFilterGroup(
            final FeedPageViewModel feedPage, final String groupTitle) {
        return feedPage.getFilterGroups().stream()
                .filter(group -> groupTitle.equals(group.getTitle()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new AssertionError(
                                        "Expected filter group '%s'".formatted(groupTitle)));
    }

    private static MockMvc createFeedMockMvc(final MatchService matchService) {
        return MockMvcBuilders.standaloneSetup(new FeedController(matchService))
                .setViewResolvers(createViewResolver())
                .setValidator(createValidator())
                .build();
    }

    private static InternalResourceViewResolver createViewResolver() {
        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        return viewResolver;
    }

    private static LocalValidatorFactoryBean createValidator() {
        final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.setConfigurationInitializer(
                configuration ->
                        configuration.clockProvider(() -> Clock.fixed(FIXED_NOW, TEST_ZONE)));
        validator.afterPropertiesSet();
        return validator;
    }

    private static PaginatedResult<Match> filterFeedMatchesForRouteTests(
            final List<Match> matches,
            final String sport,
            final int page,
            final int pageSize,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        final List<String> sports = parseSportFilters(sport);
        final List<Match> filteredMatches =
                matches.stream()
                        .filter(
                                match ->
                                        sports.isEmpty()
                                                || sports.contains(match.getSport().getDbValue()))
                        .filter(match -> matchesPrice(match, minPrice, maxPrice))
                        .toList();
        final int safePage = page > 0 ? page : 1;
        final int safePageSize = pageSize > 0 ? pageSize : 12;
        final int totalPages =
                Math.max(1, (filteredMatches.size() + safePageSize - 1) / safePageSize);
        final int clampedPage = Math.min(safePage, totalPages);
        final int fromIndex = Math.min((clampedPage - 1) * safePageSize, filteredMatches.size());
        final int toIndex = Math.min(fromIndex + safePageSize, filteredMatches.size());

        return new PaginatedResult<>(
                filteredMatches.subList(fromIndex, toIndex),
                filteredMatches.size(),
                clampedPage,
                safePageSize);
    }

    private static List<String> parseSportFilters(final String sport) {
        if (sport == null || sport.isBlank()) {
            return List.of();
        }

        final LinkedHashSet<String> parsedSports = new LinkedHashSet<>();
        for (final String rawSport : sport.split(",")) {
            if (rawSport == null || rawSport.isBlank()) {
                continue;
            }
            Sport.fromDbValue(rawSport.trim().toLowerCase(Locale.ROOT))
                    .map(Sport::getDbValue)
                    .ifPresent(parsedSports::add);
        }

        return List.copyOf(parsedSports);
    }

    private static boolean matchesPrice(
            final Match match, final BigDecimal minPrice, final BigDecimal maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return true;
        }

        if (match.getPricePerPlayer() == null) {
            return false;
        }

        if (minPrice != null && match.getPricePerPlayer().compareTo(minPrice) < 0) {
            return false;
        }

        return maxPrice == null || match.getPricePerPlayer().compareTo(maxPrice) <= 0;
    }
}
