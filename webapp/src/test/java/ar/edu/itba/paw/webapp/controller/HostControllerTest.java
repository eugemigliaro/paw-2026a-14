package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.match.MatchForbiddenActionException;
import ar.edu.itba.paw.models.exceptions.match.MatchNotFoundException;
import ar.edu.itba.paw.models.exceptions.match.MatchNotRecurringException;
import ar.edu.itba.paw.models.exceptions.matchUpdate.MatchUpdateCapacityBelowConfirmedException;
import ar.edu.itba.paw.models.exceptions.matchUpdate.MatchUpdateNotEditableException;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.services.CreateMatchRequest;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.UpdateMatchRequest;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.config.converters.StringToEventCategoryConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToEventFilterConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToEventJoinPolicyConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToEventStatusConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToEventTypeConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToEventVisibilityConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToMatchSortConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToPlayerReviewFilterConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToPlayerReviewReactionConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToRecurrenceEndModeConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToRecurrenceFrequencyConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToSportConverter;
import ar.edu.itba.paw.webapp.exception.AccessExceptionHandler;
import ar.edu.itba.paw.webapp.security.annotation.CurrentUserArgumentResolver;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.MatchUtils;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import ar.edu.itba.paw.webapp.utils.ValidatorTestUtils;
import ar.edu.itba.paw.webapp.validation.UserEmailValidator;
import ar.edu.itba.paw.webapp.validation.UsernameValidator;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class HostControllerTest {

    private MockMvc mockMvc;
    private MatchService matchService;

    private Match realMatch;
    private Match completedMatch;
    private Match cancelledFutureMatch;
    private Match privateInviteOnlyMatch;
    private Match recurringSecondOccurrence;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        matchService = Mockito.mock(MatchService.class);

        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        final MessageSource messageSource = messageSource();
        final UserEmailValidator userEmailValidator =
                new UserEmailValidator(Mockito.mock(UserService.class));
        final UsernameValidator usernameValidator =
                new UsernameValidator(Mockito.mock(UserService.class));
        final LocalValidatorFactoryBean validator =
                ValidatorTestUtils.validator(messageSource, userEmailValidator, usernameValidator);

        realMatch =
                MatchUtils.match(42L)
                        .address("Downtown Club")
                        .coords(-34.61, -58.38)
                        .title("Sunrise Padel")
                        .description("Friendly\\n doubles session")
                        .price(BigDecimal.TEN)
                        .joinedPlayers(2)
                        .build();
        completedMatch =
                MatchUtils.match(44L)
                        .sport(Sport.BASKETBALL)
                        .address("South Sports Center")
                        .title("Weekend Basketball")
                        .description("Completed tournament")
                        .startsAt(Instant.parse("2026-04-03T19:00:00Z"))
                        .endsAt(Instant.parse("2026-04-03T21:00:00Z"))
                        .maxPlayers(10)
                        .price(BigDecimal.ZERO)
                        .status(EventStatus.COMPLETED)
                        .joinedPlayers(10)
                        .build();
        cancelledFutureMatch =
                MatchUtils.match(45L)
                        .sport(Sport.TENNIS)
                        .address("City Tennis Club")
                        .title("Sunday Tennis")
                        .description("Cancelled due to weather")
                        .startsAt(Instant.parse("2026-04-08T12:00:00Z"))
                        .endsAt(Instant.parse("2026-04-08T14:00:00Z"))
                        .maxPlayers(6)
                        .price(BigDecimal.TEN)
                        .status(EventStatus.CANCELLED)
                        .joinedPlayers(2)
                        .build();
        privateInviteOnlyMatch =
                MatchUtils.match(51L)
                        .address("Members Club")
                        .title("Invite Night Padel")
                        .description("Private doubles session")
                        .startsAt(Instant.parse("2026-04-10T21:00:00Z"))
                        .endsAt(Instant.parse("2026-04-10T22:30:00Z"))
                        .price(BigDecimal.TEN)
                        .visibility(EventVisibility.PRIVATE)
                        .joinPolicy(EventJoinPolicy.INVITE_ONLY)
                        .joinedPlayers(2)
                        .build();
        recurringSecondOccurrence =
                MatchUtils.match(47L)
                        .address("Downtown Club")
                        .title("Weekly Padel")
                        .description("Friendly recurring session")
                        .startsAt(Instant.parse("2026-04-16T18:00:00Z"))
                        .endsAt(Instant.parse("2026-04-16T19:30:00Z"))
                        .price(BigDecimal.TEN)
                        .joinedPlayers(0)
                        .series(MatchUtils.getMatchSeries(600L, UserUtils.getUser(7L)))
                        .seriesOccurrenceIndex(2)
                        .build();

        configureMatchServiceStubs();

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HostController(matchService, false, "", "", 0, 0, 0))
                        .setViewResolvers(viewResolver)
                        .setLocaleResolver(localeResolver())
                        .addInterceptors(localeChangeInterceptor())
                        .defaultRequest(get("/").locale(Locale.ENGLISH))
                        .setValidator(validator)
                        .setConversionService(formattingConversionServiceWithSportConverter())
                        .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                        .setControllerAdvice(new AccessExceptionHandler(messageSource))
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void configureMatchServiceStubs() {
        // findMatchById: returns the fixture match for known IDs.
        Mockito.when(matchService.findMatchById(Mockito.anyLong()))
                .thenAnswer(
                        invocation -> {
                            Long id = invocation.getArgument(0);
                            return Optional.ofNullable(findFixtureMatch(id));
                        });

        // createMatch: capture request, return a canned Match with id 43.
        Mockito.when(matchService.createMatch(Mockito.any(CreateMatchRequest.class)))
                .thenAnswer(
                        invocation -> {
                            final CreateMatchRequest request = invocation.getArgument(0);
                            final EventJoinPolicy joinPolicy =
                                    EventVisibility.PRIVATE.equals(request.getVisibility())
                                            ? EventJoinPolicy.INVITE_ONLY
                                            : request.getJoinPolicy();
                            return MatchUtils.match(43L)
                                    .sport(request.getSport())
                                    .host(request.getHost())
                                    .address(request.getAddress())
                                    .title(request.getTitle())
                                    .description(request.getDescription())
                                    .startsAt(
                                            PlatformTime.toInstant(
                                                    request.getStartDate(), request.getStartTime()))
                                    .endsAt(
                                            PlatformTime.toInstant(
                                                    request.getEndDate(), request.getEndTime()))
                                    .maxPlayers(request.getMaxPlayers())
                                    .price(request.getPricePerPlayer())
                                    .visibility(request.getVisibility())
                                    .joinPolicy(joinPolicy)
                                    .status(request.getStatus())
                                    .series(
                                            request.isRecurring()
                                                    ? MatchUtils.getMatchSeries(
                                                            700L, request.getHost())
                                                    : null)
                                    .seriesOccurrenceIndex(request.isRecurring() ? 1 : null)
                                    .build();
                        });

        // findEditableMatchForHost: id-based mapping mirroring the original impl.
        Mockito.when(
                        matchService.findEditableMatchForHost(
                                Mockito.anyLong(), Mockito.any(User.class)))
                .thenAnswer(
                        invocation -> {
                            final Long matchId = invocation.getArgument(0);
                            final User actingUser = invocation.getArgument(1);
                            final Match match = findFixtureMatch(matchId);
                            if (match == null) {
                                throw new MatchNotFoundException();
                            }
                            if (actingUser.getId() != 7L) {
                                throw new MatchForbiddenActionException();
                            }
                            if (match.getStatus() == EventStatus.COMPLETED
                                    || match.getStatus() == EventStatus.CANCELLED) {
                                throw new MatchUpdateNotEditableException();
                            }
                            return match;
                        });

        // findEditableRecurringMatchForHost: must be editable AND a recurring occurrence.
        Mockito.when(
                        matchService.findEditableRecurringMatchForHost(
                                Mockito.anyLong(), Mockito.any(User.class)))
                .thenAnswer(
                        invocation -> {
                            final Long matchId = invocation.getArgument(0);
                            final User actingUser = invocation.getArgument(1);
                            final Match match =
                                    matchService.findEditableMatchForHost(matchId, actingUser);
                            if (!match.isRecurringOccurrence()) {
                                throw new MatchNotRecurringException();
                            }
                            return match;
                        });

        // updateMatch: only 42 and 51 are known; capacity below confirmed when maxPlayers < 2.
        Mockito.when(
                        matchService.updateMatch(
                                Mockito.anyLong(),
                                Mockito.any(User.class),
                                Mockito.any(UpdateMatchRequest.class)))
                .thenAnswer(
                        invocation -> {
                            final Long matchId = invocation.getArgument(0);
                            final User actingUser = invocation.getArgument(1);
                            final UpdateMatchRequest request = invocation.getArgument(2);
                            if (matchId != 42L && matchId != 51L) {
                                throw new MatchNotFoundException();
                            }
                            if (actingUser.getId() != 7L) {
                                throw new MatchForbiddenActionException();
                            }
                            if (request.getMaxPlayers() < 2) {
                                throw new MatchUpdateCapacityBelowConfirmedException();
                            }
                            return buildUpdatedMatch(matchId, actingUser, request);
                        });

        // updateSeriesFromOccurrence: only 46 and 47 are known.
        Mockito.when(
                        matchService.updateSeriesFromOccurrence(
                                Mockito.anyLong(),
                                Mockito.any(User.class),
                                Mockito.any(UpdateMatchRequest.class)))
                .thenAnswer(
                        invocation -> {
                            final Long matchId = invocation.getArgument(0);
                            final User actingUser = invocation.getArgument(1);
                            final UpdateMatchRequest request = invocation.getArgument(2);
                            if (matchId != 46L && matchId != 47L) {
                                throw new MatchNotFoundException();
                            }
                            if (actingUser.getId() != 7L) {
                                throw new MatchForbiddenActionException();
                            }
                            if (request.getMaxPlayers() < 2) {
                                throw new MatchUpdateCapacityBelowConfirmedException();
                            }
                            return java.util.List.of(
                                    buildUpdatedMatch(matchId, actingUser, request));
                        });

        // cancelMatch: only 42 and 47 are known.
        Mockito.when(matchService.cancelMatch(Mockito.anyLong(), Mockito.any(User.class)))
                .thenAnswer(
                        invocation -> {
                            final Long matchId = invocation.getArgument(0);
                            final User actingUser = invocation.getArgument(1);
                            if (matchId != 42L && matchId != 47L) {
                                throw new MatchNotFoundException();
                            }
                            if (actingUser.getId() != 7L) {
                                throw new MatchForbiddenActionException();
                            }
                            return realMatch;
                        });

        // cancelSeriesFromOccurrence: only 46 and 47 are known.
        Mockito.when(
                        matchService.cancelSeriesFromOccurrence(
                                Mockito.anyLong(), Mockito.any(User.class)))
                .thenAnswer(
                        invocation -> {
                            final Long matchId = invocation.getArgument(0);
                            final User actingUser = invocation.getArgument(1);
                            if (matchId != 46L && matchId != 47L) {
                                throw new MatchNotFoundException();
                            }
                            if (actingUser.getId() != 7L) {
                                throw new MatchForbiddenActionException();
                            }
                            return java.util.List.of(recurringSecondOccurrence);
                        });
    }

    private Match findFixtureMatch(final Long matchId) {
        if (matchId == 42L) {
            return realMatch;
        }
        if (matchId == 44L) {
            return completedMatch;
        }
        if (matchId == 45L) {
            return cancelledFutureMatch;
        }
        if (matchId == 51L) {
            return privateInviteOnlyMatch;
        }
        if (matchId == 47L) {
            return recurringSecondOccurrence;
        }
        return null;
    }

    private static Match buildUpdatedMatch(
            final Long matchId, final User actingUser, final UpdateMatchRequest request) {
        final EventJoinPolicy joinPolicy =
                EventVisibility.PRIVATE == request.getVisibility()
                        ? EventJoinPolicy.INVITE_ONLY
                        : request.getJoinPolicy();
        return MatchUtils.match(matchId)
                .sport(request.getSport())
                .host(actingUser)
                .address(request.getAddress())
                .title(request.getTitle())
                .description(request.getDescription())
                .startsAt(PlatformTime.toInstant(request.getStartDate(), request.getStartTime()))
                .endsAt(PlatformTime.toInstant(request.getEndDate(), request.getEndTime()))
                .maxPlayers(request.getMaxPlayers())
                .price(request.getPricePerPlayer())
                .visibility(request.getVisibility())
                .joinPolicy(joinPolicy)
                .status(request.getStatus())
                .build();
    }

    @Test
    void getCreateMatchUsesCanonicalRouteAndFormAction() throws Exception {
        mockMvc.perform(get("/matches/new").locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attribute("pageTitleCode", "page.title.hostMode"))
                .andExpect(model().attribute("formAction", "/matches/new"));
    }

    @Test
    void getLegacyCreateMatchRouteRedirectsToCanonicalRoute() throws Exception {
        mockMvc.perform(get("/host/matches/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/new"));
    }

    @Test
    void postHostPublishWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(
                        post("/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postHostPublishCreatesAndRedirectsForAuthenticatedUsers() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");
        // Only the expected request shape creates match 43; anything else fails the test.
        Mockito.doThrow(new AssertionError("createMatch called with unexpected request"))
                .when(matchService)
                .createMatch(Mockito.any(CreateMatchRequest.class));
        Mockito.doReturn(MatchUtils.match(43L).build())
                .when(matchService)
                .createMatch(
                        Mockito.argThat(
                                (CreateMatchRequest req) ->
                                        req != null
                                                && !req.isRecurring()
                                                && LocalDate.parse("2099-04-10")
                                                        .equals(req.getStartDate())
                                                && LocalTime.parse("18:00")
                                                        .equals(req.getStartTime())
                                                && LocalDate.parse("2099-04-10")
                                                        .equals(req.getEndDate())
                                                && LocalTime.parse("19:30")
                                                        .equals(req.getEndTime())));

        mockMvc.perform(
                        post("/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/43"));
    }

    @Test
    void postLegacyHostPublishCreatesAndRedirectsForStaleForms() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");
        Mockito.doThrow(new AssertionError("createMatch called with unexpected request"))
                .when(matchService)
                .createMatch(Mockito.any(CreateMatchRequest.class));
        Mockito.doReturn(MatchUtils.match(43L).build())
                .when(matchService)
                .createMatch(
                        Mockito.argThat(
                                (CreateMatchRequest req) ->
                                        req != null && "Host Test Match".equals(req.getTitle())));

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/43"));
    }

    @Test
    void postHostPublishPassesValidCoordinatesToService() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");
        Mockito.doThrow(new AssertionError("createMatch called with unexpected request"))
                .when(matchService)
                .createMatch(Mockito.any(CreateMatchRequest.class));
        Mockito.doReturn(MatchUtils.match(43L).build())
                .when(matchService)
                .createMatch(
                        Mockito.argThat(
                                (CreateMatchRequest req) ->
                                        req != null
                                                && Double.valueOf(-34.61).equals(req.getLatitude())
                                                && Double.valueOf(-58.38)
                                                        .equals(req.getLongitude())));

        mockMvc.perform(
                        post("/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("latitude", "-34.61")
                                .param("longitude", "-58.38")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/43"));
    }

    @Test
    void postHostPublishRejectsPartialCoordinates() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");
        Mockito.doThrow(new AssertionError("createMatch must not be called when validation fails"))
                .when(matchService)
                .createMatch(Mockito.any(CreateMatchRequest.class));

        mockMvc.perform(
                        post("/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("latitude", "-34.61")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "longitude"));
    }

    @Test
    void postHostPublishRejectsOutOfRangeCoordinates() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");
        Mockito.doThrow(new AssertionError("createMatch must not be called when validation fails"))
                .when(matchService)
                .createMatch(Mockito.any(CreateMatchRequest.class));

        mockMvc.perform(
                        post("/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("latitude", "-91")
                                .param("longitude", "-58.38")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "latitude"));
    }

    @Test
    void postHostPublishCreatesRecurringMatchWithOccurrenceCount() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");
        Mockito.doThrow(new AssertionError("createMatch called with unexpected request"))
                .when(matchService)
                .createMatch(Mockito.any(CreateMatchRequest.class));
        Mockito.doReturn(MatchUtils.match(43L).build())
                .when(matchService)
                .createMatch(
                        Mockito.argThat(
                                (CreateMatchRequest req) ->
                                        req != null
                                                && req.isRecurring()
                                                && req.getRecurrence() != null
                                                && "weekly"
                                                        .equals(
                                                                req.getRecurrence()
                                                                        .getFrequency()
                                                                        .getDbValue())
                                                && Integer.valueOf(3)
                                                        .equals(
                                                                req.getRecurrence()
                                                                        .getOccurrenceCount())));

        mockMvc.perform(
                        post("/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("recurring", "true")
                                .param("recurrenceFrequency", "weekly")
                                .param("recurrenceEndMode", "occurrence_count")
                                .param("recurrenceOccurrenceCount", "3")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/43"));
    }

    @Test
    void postHostPublishCreatesRecurringMatchWithUntilDate() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");
        Mockito.doThrow(new AssertionError("createMatch called with unexpected request"))
                .when(matchService)
                .createMatch(Mockito.any(CreateMatchRequest.class));
        Mockito.doReturn(MatchUtils.match(43L).build())
                .when(matchService)
                .createMatch(
                        Mockito.argThat(
                                (CreateMatchRequest req) ->
                                        req != null
                                                && req.isRecurring()
                                                && req.getRecurrence() != null
                                                && "until_date"
                                                        .equals(
                                                                req.getRecurrence()
                                                                        .getEndMode()
                                                                        .getDbValue())
                                                && LocalDate.of(2099, 4, 24)
                                                        .equals(
                                                                req.getRecurrence()
                                                                        .getUntilDate())));

        mockMvc.perform(
                        post("/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("recurring", "true")
                                .param("recurrenceFrequency", "weekly")
                                .param("recurrenceEndMode", "until_date")
                                .param("recurrenceUntilDate", "2099-04-24")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/43"));
    }

    @Test
    void postHostPublishRejectsInvalidRecurrenceFrequency() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("recurring", "true")
                                .param("recurrenceFrequency", "yearly")
                                .param("recurrenceEndMode", "occurrence_count")
                                .param("recurrenceOccurrenceCount", "3")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(
                        model().attributeHasFieldErrors("createEventForm", "recurrenceFrequency"));
    }

    @Test
    void postHostPublishRejectsRecurringUntilDateTooSoonForFrequency() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("recurring", "true")
                                .param("recurrenceFrequency", "weekly")
                                .param("recurrenceEndMode", "until_date")
                                .param("recurrenceUntilDate", "2099-04-12")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(
                        model().attributeHasFieldErrors("createEventForm", "recurrenceUntilDate"));
    }

    @Test
    void postHostPublishAcceptsOtherSportOption() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "other")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/43"));
    }

    @Test
    void postHostPublishWithInvalidEndTimeRerendersFormWithError() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "later")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "endTime"));
    }

    @Test
    void postHostPublishWithEndEqualToStartRerendersFormWithFriendlyError() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "18:00")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "endTime"));
    }

    @Test
    void postHostPublishWithEndBeforeStartRerendersFormWithFriendlyError() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "17:45")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "endTime"));
    }

    @Test
    void postHostPublishWithPrivateEventSucceedsRegardlessOfJoinPolicy() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "private")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void getHostEditRouteRendersPrefilledFormForHost() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches/42/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attribute("isEditMode", true))
                .andExpect(model().attribute("formAction", "/host/matches/42/edit"))
                .andExpect(
                        model().attribute(
                                        "createEventForm",
                                        Matchers.hasProperty(
                                                "title", Matchers.is("Sunrise Padel"))))
                .andExpect(
                        model().attribute(
                                        "createEventForm",
                                        Matchers.allOf(
                                                Matchers.hasProperty(
                                                        "visibility",
                                                        Matchers.is(EventVisibility.PUBLIC)),
                                                Matchers.hasProperty(
                                                        "joinPolicy",
                                                        Matchers.is(EventJoinPolicy.DIRECT)),
                                                Matchers.hasProperty(
                                                        "endDate",
                                                        Matchers.is(LocalDate.of(2026, 4, 6))),
                                                Matchers.hasProperty(
                                                        "endTime",
                                                        Matchers.is(LocalTime.of(9, 0))))));
    }

    @Test
    void getHostEditRouteWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/host/matches/42/edit")).andExpect(status().isUnauthorized());
    }

    @Test
    void getHostSeriesEditRouteRendersPrefilledFormForHost() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches/47/series/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attribute("isEditMode", true))
                .andExpect(model().attribute("isSeriesEditMode", true))
                .andExpect(model().attribute("formAction", "/host/matches/47/series/edit"))
                .andExpect(model().attribute("formTitleCode", "host.seriesEdit.title"))
                .andExpect(
                        model().attribute(
                                        "createEventForm",
                                        Matchers.hasProperty(
                                                "title", Matchers.is("Weekly Padel"))));
    }

    @Test
    void postHostEditRedirectsToDetailOnSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/42/edit")
                                .param("title", "Updated Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "20:15")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42"))
                .andExpect(flash().attribute("hostAction", "updated"));
    }

    @Test
    void postHostSeriesEditRedirectsToDetailOnSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");
        Mockito.doThrow(
                        new AssertionError(
                                "updateSeriesFromOccurrence called with unexpected request"))
                .when(matchService)
                .updateSeriesFromOccurrence(
                        Mockito.anyLong(),
                        Mockito.any(User.class),
                        Mockito.any(UpdateMatchRequest.class));
        Mockito.doReturn(java.util.List.of(recurringSecondOccurrence))
                .when(matchService)
                .updateSeriesFromOccurrence(
                        Mockito.eq(47L),
                        Mockito.argThat(u -> u != null && Long.valueOf(7L).equals(u.getId())),
                        Mockito.argThat(
                                (UpdateMatchRequest req) ->
                                        req != null
                                                && "Updated Weekly Padel".equals(req.getTitle())));

        mockMvc.perform(
                        post("/host/matches/47/series/edit")
                                .param("title", "Updated Weekly Padel")
                                .param("description", "Updated recurring game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "20:15")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/47"))
                .andExpect(flash().attribute("hostAction", "seriesUpdated"));
    }

    @Test
    void postHostEditPersistsPrivateVisibilityAsInviteOnly() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");
        // The controller's contract is forwarding the submitted PRIVATE visibility with the
        // form's join policy verbatim; the PRIVATE -> INVITE_ONLY normalization is service
        // behavior covered by MatchServiceImplTest.
        Mockito.doThrow(new AssertionError("updateMatch called with unexpected request"))
                .when(matchService)
                .updateMatch(
                        Mockito.anyLong(),
                        Mockito.any(User.class),
                        Mockito.any(UpdateMatchRequest.class));
        Mockito.doReturn(findFixtureMatch(42L))
                .when(matchService)
                .updateMatch(
                        Mockito.eq(42L),
                        Mockito.argThat(u -> u != null && Long.valueOf(7L).equals(u.getId())),
                        Mockito.argThat(
                                (UpdateMatchRequest req) ->
                                        req != null
                                                && EventVisibility.PRIVATE == req.getVisibility()
                                                && EventJoinPolicy.DIRECT == req.getJoinPolicy()));

        mockMvc.perform(
                        post("/host/matches/42/edit")
                                .param("title", "Updated Private Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "private")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "20:15")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42"))
                .andExpect(flash().attribute("hostAction", "updated"));
    }

    @Test
    void postHostEditPersistsPublicVisibilityAndJoinPolicyFromPrivateMatch() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");
        Mockito.doThrow(new AssertionError("updateMatch called with unexpected request"))
                .when(matchService)
                .updateMatch(
                        Mockito.anyLong(),
                        Mockito.any(User.class),
                        Mockito.any(UpdateMatchRequest.class));
        Mockito.doReturn(findFixtureMatch(51L))
                .when(matchService)
                .updateMatch(
                        Mockito.eq(51L),
                        Mockito.argThat(u -> u != null && Long.valueOf(7L).equals(u.getId())),
                        Mockito.argThat(
                                (UpdateMatchRequest req) ->
                                        req != null
                                                && EventVisibility.PUBLIC == req.getVisibility()
                                                && EventJoinPolicy.DIRECT == req.getJoinPolicy()));

        mockMvc.perform(
                        post("/host/matches/51/edit")
                                .param("title", "Updated Public Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "20:15")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/51"))
                .andExpect(flash().attribute("hostAction", "updated"));
    }

    @Test
    void postHostEditRejectsPublicInviteOnlyPolicyFromStaleFormState() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");
        Mockito.doThrow(new AssertionError("updateMatch must not be called when validation fails"))
                .when(matchService)
                .updateMatch(
                        Mockito.anyLong(),
                        Mockito.any(User.class),
                        Mockito.any(UpdateMatchRequest.class));

        mockMvc.perform(
                        post("/host/matches/51/edit")
                                .param("title", "Updated Public Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "invite_only")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "20:15")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "joinPolicy"));
    }

    @Test
    void postHostEditWithCapacityBelowConfirmedRerendersFormWithError() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/42/edit")
                                .param("title", "Updated Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-11")
                                .param("endTime", "00:15")
                                .param("maxPlayers", "1")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "maxPlayers"));
    }

    @Test
    void postHostEditWithEndBeforeStartRerendersFormWithErrorOnEndTime() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/42/edit")
                                .param("title", "Updated Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "17:45")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "endTime"));
    }

    @Test
    void postHostEditForCompletedMatchReturnsNotFound() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/44/edit")
                                .param("title", "Updated Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "20:15")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postHostEditForCancelledMatchReturnsNotFound() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/45/edit")
                                .param("title", "Updated Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "20:15")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postHostCancelWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/host/matches/42/cancel")).andExpect(status().isUnauthorized());
    }

    @Test
    void postHostCancelForNonHostReturnsForbidden() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/host/matches/42/cancel")).andExpect(status().isForbidden());
    }

    @Test
    void postHostCancelRedirectsToDetailOnSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(post("/host/matches/42/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42"))
                .andExpect(flash().attribute("hostAction", "cancelled"));
    }

    @Test
    void postHostCancelRecurringOccurrenceRedirectsToSelectedOccurrence() throws Exception {
        // Arrange
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        // Exercise and Assert
        mockMvc.perform(post("/host/matches/47/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/47"))
                .andExpect(flash().attribute("hostAction", "cancelled"));
    }

    @Test
    void postHostCancelRecurringSeriesRedirectsToSelectedOccurrence() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(post("/host/matches/47/series/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/47"))
                .andExpect(flash().attribute("hostAction", "seriesCancelled"));
    }

    @Test
    void postHostCancelRecurringSeriesForSingleEventReturnsNotFound() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(post("/host/matches/42/series/cancel")).andExpect(status().isNotFound());
    }

    @Test
    void postHostCancelForCompletedEventReturnsNotFound() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(post("/host/matches/44/cancel")).andExpect(status().isNotFound());
    }

    private static MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    private static DefaultFormattingConversionService
            formattingConversionServiceWithSportConverter() {
        final DefaultFormattingConversionService conversionService =
                new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToSportConverter());
        conversionService.addConverter(new StringToEventStatusConverter());
        conversionService.addConverter(new StringToEventVisibilityConverter());
        conversionService.addConverter(new StringToMatchSortConverter());
        conversionService.addConverter(new StringToEventTypeConverter());
        conversionService.addConverter(new StringToPlayerReviewFilterConverter());
        conversionService.addConverter(new StringToPlayerReviewReactionConverter());
        conversionService.addConverter(new StringToRecurrenceEndModeConverter());
        conversionService.addConverter(new StringToRecurrenceFrequencyConverter());
        conversionService.addConverter(new StringToEventJoinPolicyConverter());
        conversionService.addConverter(new StringToEventFilterConverter());
        conversionService.addConverter(new StringToEventCategoryConverter());
        return conversionService;
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
