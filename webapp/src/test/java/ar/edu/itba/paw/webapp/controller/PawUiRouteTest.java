package ar.edu.itba.paw.webapp.controller;

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
import ar.edu.itba.paw.services.MvpIdentityService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.VerificationConfirmationResult;
import ar.edu.itba.paw.services.VerificationFailureException;
import ar.edu.itba.paw.services.VerificationFailureReason;
import ar.edu.itba.paw.services.VerificationPreview;
import ar.edu.itba.paw.services.VerificationPreviewDetail;
import ar.edu.itba.paw.services.VerificationRequestResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class PawUiRouteTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T00:00:00Z");

    private MockMvc mockMvc;
    private AtomicReference<String> lastSportsFilter;

    @BeforeEach
    void setUp() {
        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        final MessageSource messageSource = messageSource();
        lastSportsFilter = new AtomicReference<>();

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
                        "Fast 5v5",
                        Instant.parse("2026-04-07T19:00:00Z"),
                        Instant.parse("2026-04-07T20:30:00Z"),
                        10,
                        BigDecimal.ZERO,
                        "public",
                        "open",
                        4,
                        null);
        final Match completedMatch =
                new Match(
                        44L,
                        Sport.BASKETBALL,
                        7L,
                        "South Sports Center",
                        "Weekend Basketball",
                        "Completed tournament",
                        Instant.parse("2026-04-03T19:00:00Z"),
                        Instant.parse("2026-04-03T21:00:00Z"),
                        10,
                        BigDecimal.ZERO,
                        "public",
                        "completed",
                        10,
                        null);
        final Match cancelledFutureMatch =
                new Match(
                        45L,
                        Sport.TENNIS,
                        7L,
                        "City Tennis Club",
                        "Sunday Tennis",
                        "Cancelled due to weather",
                        Instant.parse("2026-04-08T12:00:00Z"),
                        Instant.parse("2026-04-08T14:00:00Z"),
                        6,
                        BigDecimal.TEN,
                        "public",
                        "cancelled",
                        2,
                        null);

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
                    public PaginatedResult<Match> findHostedMatches(
                            final Long hostUserId, final int page, final int pageSize) {
                        return new PaginatedResult<>(
                                List.of(realMatch, footballMatch, completedMatch), 3, 1, pageSize);
                    }

                    @Override
                    public PaginatedResult<Match> findFinishedHostedMatches(
                            final Long hostUserId, final int page, final int pageSize) {
                        return new PaginatedResult<>(List.of(completedMatch), 1, 1, pageSize);
                    }

                    @Override
                    public PaginatedResult<Match> findPastJoinedMatches(
                            final Long userId, final int page, final int pageSize) {
                        return new PaginatedResult<>(List.of(completedMatch), 1, 1, pageSize);
                    }

                    @Override
                    public PaginatedResult<Match> findUpcomingJoinedMatches(
                            final Long userId, final int page, final int pageSize) {
                        return new PaginatedResult<>(
                                List.of(realMatch, cancelledFutureMatch), 2, 1, pageSize);
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
                            final java.math.BigDecimal minPrice,
                            final java.math.BigDecimal maxPrice) {
                        lastSportsFilter.set(sport);
                        return new PaginatedResult<>(
                                List.of(realMatch, footballMatch), 2, 1, pageSize);
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

        final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));

        final MvpIdentityService mvpIdentityService =
                new MvpIdentityService() {
                    @Override
                    public Optional<User> findExistingByEmail(final String email) {
                        return Optional.of(new User(7L, email, "host-player"));
                    }

                    @Override
                    public User resolveOrCreateByEmail(final String email) {
                        return new User(7L, email, "host-player");
                    }
                };

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new FeedController(matchService, messageSource),
                                new EventController(
                                        matchService,
                                        userService,
                                        actionVerificationService,
                                        messageSource),
                                new HostController(
                                        actionVerificationService,
                                        imageService,
                                        fixedClock,
                                        messageSource),
                                new MatchDashboardController(
                                        matchService, mvpIdentityService, messageSource),
                                new ErrorPageController(messageSource),
                                new VerificationController(
                                        actionVerificationService, messageSource))
                        .setViewResolvers(viewResolver)
                        .setLocaleResolver(localeResolver())
                        .addInterceptors(localeChangeInterceptor())
                        .build();
    }

    @Test
    void getFeedRouteRendersFeedPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"))
                .andExpect(model().attributeExists("shell"))
                .andExpect(model().attributeExists("feedPage"));
    }

    @Test
    void getFeedRouteWithSpanishLocaleLocalizesShellAndCards() throws Exception {
        mockMvc.perform(get("/").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"))
                .andExpect(
                        model().attribute(
                                        "shell",
                                        Matchers.hasProperty(
                                                "hostAction",
                                                Matchers.hasProperty(
                                                        "label",
                                                        Matchers.is("Cambiar a Organizador")))))
                .andExpect(
                        model().attribute(
                                        "feedPage",
                                        Matchers.hasProperty(
                                                "title",
                                                Matchers.is(
                                                        "Encontr\u00e1 tu pr\u00f3ximo partido."))));
    }

    @Test
    void getFeedRouteWithRepeatedSportParamsPassesCommaSeparatedToService() throws Exception {
        mockMvc.perform(get("/").param("sport", "padel").param("sport", "football"))
                .andExpect(status().isOk());

        assert lastSportsFilter.get() != null
                : "expected MatchService to be called with a non-null sport filter";
        assert lastSportsFilter.get().contains("padel")
                        && lastSportsFilter.get().contains("football")
                : "expected sport filter to include both selected sports, was "
                        + lastSportsFilter.get();
    }

    @Test
    void getFeedRouteWithCommaSeparatedSportParamAcceptsMultipleSports() throws Exception {
        mockMvc.perform(get("/").param("sport", "padel,tennis"))
                .andExpect(status().isOk())
                .andExpect(
                        model().attribute(
                                        "selectedSports",
                                        Matchers.containsInAnyOrder("padel", "tennis")));
    }

    @Test
    void getFeedRouteWithMinAndMaxPricePropagatesToModel() throws Exception {
        mockMvc.perform(get("/").param("minPrice", "5").param("maxPrice", "25"))
                .andExpect(status().isOk())
                .andExpect(
                        model().attribute(
                                        "selectedMinPrice",
                                        Matchers.comparesEqualTo(new BigDecimal("5"))))
                .andExpect(
                        model().attribute(
                                        "selectedMaxPrice",
                                        Matchers.comparesEqualTo(new BigDecimal("25"))));
    }

    @Test
    void getRealMatchDetailsRouteRendersMatchPage() throws Exception {
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
    void postVerificationConfirmRedirectsToMatch() throws Exception {
        mockMvc.perform(post("/verifications/abc123/confirm"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42?reservation=confirmed"));
    }

    @Test
    void getRemovedMockMatchRouteReturnsNotFound() throws Exception {
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
                                .param("eventDate", "2026-04-10")
                                .param("eventTime", "18:00")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification/check-email"))
                .andExpect(model().attributeExists("title"))
                .andExpect(model().attributeExists("summary"));
    }

    @Test
    void postHostPublishWithSpanishLocaleLocalizesConfirmationCopyAndDate() throws Exception {
        mockMvc.perform(
                        post("/host/matches/new")
                                .param("lang", "es")
                                .param("email", "host@test.com")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("eventDate", "2026-04-10")
                                .param("eventTime", "18:00")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification/check-email"))
                .andExpect(model().attribute("title", "Revis\u00e1 tu email"))
                .andExpect(model().attribute("eyebrow", "Publicaci\u00f3n de evento solicitada"))
                .andExpect(model().attribute("actionLabel", "Volver a crear evento"))
                .andExpect(
                        model().attribute(
                                        "expiresAtLabel",
                                        Matchers.containsStringIgnoringCase("abr")));
    }

    @Test
    void postHostPublishWithEndTimeBeforeStartTimeRerendersFormWithError() throws Exception {
        mockMvc.perform(
                        post("/host/matches/new")
                                .param("email", "host@test.com")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("eventDate", "2026-04-10")
                                .param("eventTime", "18:00")
                                .param("endTime", "17:00")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "endTime"));
    }

    @Test
    void getHostAllMatchesRouteRendersDashboardPage() throws Exception {
        mockMvc.perform(get("/host/matches").param("email", "host@test.com").locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name("host/all-matches"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attributeExists("listTitle"));
    }

    @Test
    void getHostFinishedMatchesRouteRendersFinishedPage() throws Exception {
        mockMvc.perform(
                        get("/host/matches/finished")
                                .param("email", "host@test.com")
                                .locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name("host/finished-matches"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attributeExists("listTitle"));
    }

    @Test
    void getPlayerPastMatchesRouteRendersPastPage() throws Exception {
        mockMvc.perform(get("/player/matches/past").param("email", "player@test.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("player/past-matches"))
                .andExpect(model().attributeExists("events"));
    }

    @Test
    void getPlayerUpcomingMatchesRouteRendersUpcomingPage() throws Exception {
        mockMvc.perform(get("/player/matches/upcoming").param("email", "player@test.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("player/upcoming-matches"))
                .andExpect(model().attributeExists("events"));
    }

    @Test
    void getHostAllMatchesRouteWithSpanishLocaleLocalizesHeader() throws Exception {
        mockMvc.perform(get("/host/matches").param("email", "host@test.com").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/all-matches"))
                .andExpect(model().attribute("listTitle", "Panel de eventos organizados"));
    }

    private static MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    private static SessionLocaleResolver localeResolver() {
        final SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        return localeResolver;
    }

    private static LocaleChangeInterceptor localeChangeInterceptor() {
        final LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        return localeChangeInterceptor;
    }
}
