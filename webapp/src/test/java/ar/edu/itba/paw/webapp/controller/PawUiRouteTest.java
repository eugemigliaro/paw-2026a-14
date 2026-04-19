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
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.PasswordResetPreview;
import ar.edu.itba.paw.services.RegisterAccountRequest;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.VerificationConfirmationResult;
import ar.edu.itba.paw.services.VerificationFailureException;
import ar.edu.itba.paw.services.VerificationFailureReason;
import ar.edu.itba.paw.services.VerificationPreview;
import ar.edu.itba.paw.services.VerificationPreviewDetail;
import ar.edu.itba.paw.services.VerificationRequestResult;
import ar.edu.itba.paw.services.exceptions.MatchReservationException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FilterGroupViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.MatchListControlsViewModel;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class PawUiRouteTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T00:00:00Z");

    private MockMvc mockMvc;
    private AtomicReference<String> lastSportsFilter;
    private AtomicReference<Long> lastReservedMatchId;
    private AtomicReference<Long> lastReservedUserId;
    private AtomicReference<MatchReservationException> reservationFailure;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        final MessageSource messageSource = messageSource();

        lastSportsFilter = new AtomicReference<>();
        lastReservedMatchId = new AtomicReference<>();
        lastReservedUserId = new AtomicReference<>();
        reservationFailure = new AtomicReference<>();

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
                    public Optional<Match> findMatchById(final Long matchId) {
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
                            final Long hostUserId,
                            final Boolean upcoming,
                            final String query,
                            final String sport,
                            final String visibility,
                            final String status,
                            final String startDate,
                            final String endDate,
                            final java.math.BigDecimal minPrice,
                            final java.math.BigDecimal maxPrice,
                            final String sort,
                            final String timezone,
                            final int page,
                            final int pageSize) {
                        final List<Match> items =
                                status != null && status.contains("completed")
                                        ? List.of(completedMatch)
                                        : List.of(realMatch);
                        return new PaginatedResult<>(items, items.size(), 1, pageSize);
                    }

                    @Override
                    public PaginatedResult<Match> findJoinedMatches(
                            final Long userId,
                            final Boolean upcoming,
                            final String query,
                            final String sport,
                            final String visibility,
                            final String status,
                            final String startDate,
                            final String endDate,
                            final java.math.BigDecimal minPrice,
                            final java.math.BigDecimal maxPrice,
                            final String sort,
                            final String timezone,
                            final int page,
                            final int pageSize) {
                        final List<Match> items =
                                Boolean.FALSE.equals(upcoming)
                                        ? List.of(completedMatch)
                                        : List.of(realMatch, cancelledFutureMatch);
                        return new PaginatedResult<>(items, items.size(), 1, pageSize);
                    }

                    @Override
                    public PaginatedResult<Match> searchPublicMatches(
                            final String query,
                            final String sport,
                            final String startDate,
                            final String endDate,
                            final String sort,
                            final int page,
                            final int pageSize,
                            final String timezone,
                            final BigDecimal minPrice,
                            final BigDecimal maxPrice) {
                        lastSportsFilter.set(sport);
                        return new PaginatedResult<>(
                                List.of(realMatch, footballMatch), 2, 1, pageSize);
                    }
                };

        final MatchReservationService matchReservationService =
                new MatchReservationService() {
                    @Override
                    public boolean hasActiveReservation(final Long matchId, final Long userId) {
                        return false;
                    }

                    @Override
                    public void reserveSpot(final Long matchId, final Long userId) {
                        final MatchReservationException failure = reservationFailure.get();
                        if (failure != null) {
                            throw failure;
                        }

                        lastReservedMatchId.set(matchId);
                        lastReservedUserId.set(userId);
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
                        if ("host-player".equals(username)) {
                            return Optional.of(new User(9L, "host@test.com", "host-player"));
                        }
                        return Optional.empty();
                    }
                };

        final AccountAuthService accountAuthService =
                new AccountAuthService() {
                    @Override
                    public VerificationRequestResult register(
                            final RegisterAccountRequest request) {
                        return new VerificationRequestResult(
                                request.getEmail(), Instant.parse("2026-04-06T18:00:00Z"));
                    }

                    @Override
                    public Optional<VerificationRequestResult> resendVerification(
                            final String email) {
                        return Optional.empty();
                    }

                    @Override
                    public VerificationPreview getVerificationPreview(final String rawToken) {
                        if ("invalid".equals(rawToken)) {
                            throw new VerificationFailureException(
                                    VerificationFailureReason.NOT_FOUND, "Missing link");
                        }

                        return new VerificationPreview(
                                "Verify your Match Point account",
                                "Confirm your email address to activate the account.",
                                "player@test.com",
                                Instant.parse("2026-04-06T18:00:00Z"),
                                "Verify account",
                                "/login?verified=1",
                                List.of(
                                        new VerificationPreviewDetail(
                                                "Username", "player-account")));
                    }

                    @Override
                    public VerificationConfirmationResult confirmVerification(
                            final String rawToken) {
                        if ("invalid".equals(rawToken)) {
                            throw new VerificationFailureException(
                                    VerificationFailureReason.NOT_FOUND, "Missing link");
                        }

                        return new VerificationConfirmationResult(9L, "/login?verified=1", "done");
                    }

                    @Override
                    public Optional<VerificationRequestResult> requestPasswordReset(
                            final String email) {
                        return Optional.empty();
                    }

                    @Override
                    public PasswordResetPreview getPasswordResetPreview(final String rawToken) {
                        return new PasswordResetPreview(
                                "player@test.com", Instant.parse("2026-04-06T18:00:00Z"));
                    }

                    @Override
                    public VerificationConfirmationResult resetPassword(
                            final String rawToken, final String newPassword) {
                        return new VerificationConfirmationResult(
                                9L, "/login?reset=1", "Password reset");
                    }

                    @Override
                    public Optional<UserAccount> findAccountByEmail(final String email) {
                        return Optional.of(
                                new UserAccount(
                                        9L,
                                        email,
                                        "host-player",
                                        "{bcrypt}hash",
                                        UserRole.USER,
                                        FIXED_NOW));
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

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new FeedController(matchService, messageSource),
                                new EventController(
                                        matchService,
                                        matchReservationService,
                                        userService,
                                        messageSource),
                                new PublicProfileController(userService, messageSource),
                                new AccountController(messageSource),
                                new HostController(
                                        matchService, imageService, fixedClock, messageSource),
                                new MatchDashboardController(matchService, messageSource),
                                new ErrorPageController(messageSource),
                                new VerificationController(accountAuthService, messageSource))
                        .setViewResolvers(viewResolver)
                        .setLocaleResolver(localeResolver())
                        .addInterceptors(localeChangeInterceptor())
                        .setConversionService(new DefaultFormattingConversionService())
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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
                                        Matchers.hasProperty("hostAction", Matchers.nullValue())))
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

        Assertions.assertNotNull(lastSportsFilter.get());
        Assertions.assertTrue(lastSportsFilter.get().contains("padel"));
        Assertions.assertTrue(lastSportsFilter.get().contains("football"));
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
    void getRealMatchDetailsRouteRendersMatchPageForAnonymousUsers() throws Exception {
        mockMvc.perform(get("/matches/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attributeExists("reservationRequestPath"))
                .andExpect(model().attribute("reservationRequiresLogin", true))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty(
                                                "aboutParagraphs",
                                                Matchers.contains("Friendly\n doubles session"))))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty(
                                                "hostProfileHref",
                                                Matchers.is("/users/host-player"))))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty(
                                                "participants",
                                                Matchers.contains(
                                                        Matchers.hasProperty(
                                                                "profileHref",
                                                                Matchers.is("/users/first-player")),
                                                        Matchers.hasProperty(
                                                                "profileHref",
                                                                Matchers.is(
                                                                        "/users/second-player"))))));
    }

    @Test
    void getRealMatchDetailsRouteForAuthenticatedUsersEnablesDirectReservation() throws Exception {
        authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("reservationRequiresLogin", false));
    }

    @Test
    void postReservationRequestWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/matches/42/reservations")).andExpect(status().isUnauthorized());
    }

    @Test
    void postReservationRequestAsAuthenticatedUserRedirectsToConfirmedEvent() throws Exception {
        authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/42/reservations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42?reservation=confirmed"));

        Assertions.assertEquals(42L, lastReservedMatchId.get());
        Assertions.assertEquals(9L, lastReservedUserId.get());
    }

    @Test
    void postReservationRequestWithSpanishLocaleLocalizesReservationErrors() throws Exception {
        authenticateUser(9L, "player@test.com", "player-account");
        reservationFailure.set(new MatchReservationException("already_joined", "Already reserved"));

        mockMvc.perform(post("/matches/42/reservations").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("reservationRequiresLogin", false))
                .andExpect(
                        model().attribute(
                                        "reservationError",
                                        "Tu cuenta ya tiene una reserva confirmada para este evento."));
    }

    @Test
    void getVerificationPreviewRendersConfirmPage() throws Exception {
        mockMvc.perform(get("/verifications/abc123"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification/confirm"))
                .andExpect(model().attributeExists("preview"));
    }

    @Test
    void postVerificationConfirmRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/verifications/abc123/confirm"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?verified=1"));
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
    void postHostPublishWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postHostPublishCreatesAndRedirectsForAuthenticatedUsers() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/43"));
    }

    @Test
    void postHostPublishWithEndTimeBeforeStartTimeRerendersFormWithError() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("eventDate", "2099-04-10")
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
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/list"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attributeExists("listTitle"));
    }

    @Test
    void getHostFinishedMatchesRouteRendersFinishedPage() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches/finished").locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/list"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attributeExists("listTitle"));
    }

    @Test
    void getHostFinishedMatchesDefaultsDateRangeAndNoLegacyTimeOption() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");
        final String today = LocalDate.now(ZoneId.systemDefault()).toString();

        final MvcResult result =
                mockMvc.perform(get("/host/matches/finished").locale(Locale.ENGLISH))
                        .andExpect(status().isOk())
                        .andExpect(view().name("matches/list"))
                        .andExpect(
                                model().attribute("selectedStartDateValue", Matchers.nullValue()))
                        .andExpect(model().attribute("selectedEndDateValue", today))
                        .andReturn();

        assertNoTomorrowTimeOption(
                (MatchListControlsViewModel)
                        result.getModelAndView().getModel().get("listControls"));
    }

    @Test
    void getPlayerPastMatchesRouteRendersPastPage() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/player/matches/past"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/list"))
                .andExpect(model().attributeExists("events"));
    }

    @Test
    void getPlayerPastMatchesDefaultsDateRangeAndNoLegacyTimeOption() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");
        final String today = LocalDate.now(ZoneId.systemDefault()).toString();

        final MvcResult result =
                mockMvc.perform(get("/player/matches/past").locale(Locale.ENGLISH))
                        .andExpect(status().isOk())
                        .andExpect(view().name("matches/list"))
                        .andExpect(
                                model().attribute("selectedStartDateValue", Matchers.nullValue()))
                        .andExpect(model().attribute("selectedEndDateValue", today))
                        .andReturn();

        assertNoTomorrowTimeOption(
                (MatchListControlsViewModel)
                        result.getModelAndView().getModel().get("listControls"));
    }

    @Test
    void getPlayerUpcomingMatchesRouteRendersUpcomingPage() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");
        final String today = LocalDate.now(ZoneId.systemDefault()).toString();

        mockMvc.perform(get("/player/matches/upcoming"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/list"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attribute("selectedStartDateValue", today))
                .andExpect(model().attribute("selectedEndDateValue", Matchers.nullValue()));
    }

    @Test
    void getAccountRouteRendersPrivateAccountPageForAuthenticatedUsers() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/account"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/index"))
                .andExpect(model().attribute("username", "host-player"))
                .andExpect(model().attribute("email", "host@test.com"))
                .andExpect(model().attributeExists("shell"));
    }

    @Test
    void getPublicProfileRouteRendersPublicProfileForAnonymousUsers() throws Exception {
        mockMvc.perform(get("/users/host-player"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attributeExists("profilePage"))
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty(
                                                "username", Matchers.is("host-player"))))
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty(
                                                "email", Matchers.is("host@test.com"))));
    }

    @Test
    void getPublicProfileRouteWithSpanishLocaleLocalizesPageCopy() throws Exception {
        mockMvc.perform(get("/users/host-player").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attribute("profileTitle", "Perfil p\u00fablico"))
                .andExpect(model().attribute("profileUsernameLabel", "Usuario"));
    }

    @Test
    void getUnknownPublicProfileRouteReturnsNotFound() throws Exception {
        mockMvc.perform(get("/users/missing-player")).andExpect(status().isNotFound());
    }

    @Test
    void getHostAllMatchesRouteWithSpanishLocaleLocalizesHeader() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/list"))
                .andExpect(model().attribute("listTitle", "Panel de eventos organizados"));
    }

    private void authenticateUser(final Long userId, final String email, final String username) {
        final AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(
                        new UserAccount(
                                userId, email, username, "{bcrypt}hash", UserRole.USER, FIXED_NOW));
        final UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static void assertNoTomorrowTimeOption(final MatchListControlsViewModel listControls) {
        final boolean hasTomorrowOption =
                listControls.getFilterGroups().stream()
                        .map(FilterGroupViewModel::getOptions)
                        .flatMap(List::stream)
                        .anyMatch(
                                option ->
                                        option.getHref() != null
                                                && option.getHref().contains("time=tomorrow"));

        Assertions.assertFalse(hasTomorrowOption);
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
