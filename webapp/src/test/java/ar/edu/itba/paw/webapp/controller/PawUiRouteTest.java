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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class PawUiRouteTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-10T18:00:00Z");

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();

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
                        return new PaginatedResult<>(List.of(realMatch), 1, 1, 12);
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
                                "/events/42?reservation=confirmed",
                                List.of(new VerificationPreviewDetail("Venue", "Downtown Club")));
                    }

                    @Override
                    public VerificationConfirmationResult confirm(final String rawToken) {
                        if ("invalid".equals(rawToken)) {
                            throw new VerificationFailureException(
                                    VerificationFailureReason.NOT_FOUND, "Missing link");
                        }
                        return new VerificationConfirmationResult(
                                9L, "/events/42?reservation=confirmed", "done");
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
                                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC)),
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
                .andExpect(model().attributeExists("feedPage"));
    }

    @Test
    void getFeedRoutePreservesMultipleSelectedSports() throws Exception {
        mockMvc.perform(get("/").param("sport", "football", "tennis"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"))
                .andExpect(model().attribute("selectedSports", Matchers.contains("football", "tennis")));
    }

    @Test
    void getRealEventDetailsRouteRendersEventPage() throws Exception {
        mockMvc.perform(get("/events/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/detail"))
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
        mockMvc.perform(post("/events/42/reservations").param("email", "player@test.com"))
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
                .andExpect(redirectedUrl("/events/42?reservation=confirmed"));
    }

    @Test
    void getRemovedMockEventRouteReturnsNotFound() throws Exception {
        mockMvc.perform(get("/events/sunrise-padel-championship")).andExpect(status().isNotFound());
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
                        post("/host/events/new")
                                .param("email", "host@test.com")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("eventDate", "2026-04-11")
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
                        post("/host/events/new")
                                .param("email", "host@test.com")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("eventDate", "2020-04-10")
                                .param("eventTime", "18:00")
                                .param("endTime", "20:00")
                                .param("timezone", "UTC")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-event"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "eventDate"));
    }

    @Test
    void postHostPublishWithPastTimeTodayShowsValidationError() throws Exception {
        mockMvc.perform(
                        post("/host/events/new")
                                .param("email", "host@test.com")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("eventDate", "2026-04-10")
                                .param("eventTime", "17:00")
                                .param("endTime", "19:00")
                                .param("timezone", "UTC")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-event"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "eventTime"));
    }

    @Test
    void postHostPublishWithEndTimeBeforeStartTimeShowsValidationError() throws Exception {
        mockMvc.perform(
                        post("/host/events/new")
                                .param("email", "host@test.com")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("eventDate", "2026-04-11")
                                .param("eventTime", "18:00")
                                .param("endTime", "17:30")
                                .param("timezone", "UTC")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-event"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "endTime"));
    }
}
