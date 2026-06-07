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
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.services.CreateMatchRequest;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.UpdateMatchRequest;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.exceptions.matchCancelation.MatchCancellationForbiddenException;
import ar.edu.itba.paw.services.exceptions.matchCancelation.MatchCancellationNotFoundException;
import ar.edu.itba.paw.services.exceptions.matchUpdate.MatchUpdateCapacityBelowConfirmedException;
import ar.edu.itba.paw.services.exceptions.matchUpdate.MatchUpdateForbiddenException;
import ar.edu.itba.paw.services.exceptions.matchUpdate.MatchUpdateNotEditableException;
import ar.edu.itba.paw.services.exceptions.matchUpdate.MatchUpdateNotFoundException;
import ar.edu.itba.paw.services.exceptions.matchUpdate.MatchUpdateNotRecurringException;
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
import ar.edu.itba.paw.webapp.security.annotation.CurrentUserArgumentResolver;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.MatchUtils;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import ar.edu.itba.paw.webapp.validation.UserEmailValidator;
import ar.edu.itba.paw.webapp.validation.UsernameValidator;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
                validator(messageSource, userEmailValidator, usernameValidator);

        realMatch =
                new Match(
                        42L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Downtown Club",
                        -34.61,
                        -58.38,
                        "Sunrise Padel",
                        "Friendly\\n doubles session",
                        Instant.parse("2026-04-06T10:00:00Z"),
                        Instant.parse("2026-04-06T12:00:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        2,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null);
        completedMatch =
                new Match(
                        44L,
                        Sport.BASKETBALL,
                        UserUtils.getUser(7L),
                        "South Sports Center",
                        null,
                        null,
                        "Weekend Basketball",
                        "Completed tournament",
                        Instant.parse("2026-04-03T19:00:00Z"),
                        Instant.parse("2026-04-03T21:00:00Z"),
                        10,
                        BigDecimal.ZERO,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.COMPLETED,
                        10,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null);
        cancelledFutureMatch =
                new Match(
                        45L,
                        Sport.TENNIS,
                        UserUtils.getUser(7L),
                        "City Tennis Club",
                        null,
                        null,
                        "Sunday Tennis",
                        "Cancelled due to weather",
                        Instant.parse("2026-04-08T12:00:00Z"),
                        Instant.parse("2026-04-08T14:00:00Z"),
                        6,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.CANCELLED,
                        2,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null);
        privateInviteOnlyMatch =
                new Match(
                        51L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Members Club",
                        null,
                        null,
                        "Invite Night Padel",
                        "Private doubles session",
                        Instant.parse("2026-04-10T21:00:00Z"),
                        Instant.parse("2026-04-10T22:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        2,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null);
        recurringSecondOccurrence =
                new Match(
                        47L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Downtown Club",
                        null,
                        null,
                        "Weekly Padel",
                        "Friendly recurring session",
                        Instant.parse("2026-04-16T18:00:00Z"),
                        Instant.parse("2026-04-16T19:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        0,
                        null,
                        MatchUtils.getMatchSeries(600L, UserUtils.getUser(7L)),
                        2,
                        false,
                        null,
                        null,
                        null);

        configureMatchServiceStubs();

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HostController(
                                        matchService, messageSource, false, "", "", 0, 0, 0))
                        .setViewResolvers(viewResolver)
                        .setLocaleResolver(localeResolver())
                        .addInterceptors(localeChangeInterceptor())
                        .defaultRequest(get("/").locale(Locale.ENGLISH))
                        .setValidator(validator)
                        .setConversionService(formattingConversionServiceWithSportConverter())
                        .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void configureMatchServiceStubs() {
        // createMatch: capture request, return a canned Match with id 43.
        Mockito.when(matchService.createMatch(Mockito.any(CreateMatchRequest.class)))
                .thenAnswer(
                        invocation -> {
                            final CreateMatchRequest request = invocation.getArgument(0);
                            final EventJoinPolicy joinPolicy =
                                    EventVisibility.PRIVATE.equals(request.getVisibility())
                                            ? EventJoinPolicy.INVITE_ONLY
                                            : request.getJoinPolicy();
                            return new Match(
                                    43L,
                                    request.getSport(),
                                    request.getHost(),
                                    request.getAddress(),
                                    null,
                                    null,
                                    request.getTitle(),
                                    request.getDescription(),
                                    PlatformTime.toInstant(
                                            request.getStartDate(), request.getStartTime()),
                                    PlatformTime.toInstant(
                                            request.getEndDate(), request.getEndTime()),
                                    request.getMaxPlayers(),
                                    request.getPricePerPlayer(),
                                    request.getVisibility(),
                                    joinPolicy,
                                    request.getStatus(),
                                    0,
                                    null,
                                    request.isRecurring()
                                            ? MatchUtils.getMatchSeries(700L, request.getHost())
                                            : null,
                                    request.isRecurring() ? 1 : null,
                                    false,
                                    null,
                                    null,
                                    null);
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
                                throw new MatchUpdateNotFoundException("Missing match");
                            }
                            if (actingUser.getId() != 7L) {
                                throw new MatchUpdateForbiddenException("Forbidden");
                            }
                            if (match.getStatus() == EventStatus.COMPLETED
                                    || match.getStatus() == EventStatus.CANCELLED) {
                                throw new MatchUpdateNotEditableException("Not editable");
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
                                throw new MatchUpdateNotRecurringException("Not recurring");
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
                                throw new MatchUpdateNotFoundException("Missing match");
                            }
                            if (actingUser.getId() != 7L) {
                                throw new MatchUpdateForbiddenException("Forbidden");
                            }
                            if (request.getMaxPlayers() < 2) {
                                throw new MatchUpdateCapacityBelowConfirmedException(
                                        "Capacity too low");
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
                                throw new MatchUpdateNotFoundException("Missing match");
                            }
                            if (actingUser.getId() != 7L) {
                                throw new MatchUpdateForbiddenException("Forbidden");
                            }
                            if (request.getMaxPlayers() < 2) {
                                throw new MatchUpdateCapacityBelowConfirmedException(
                                        "Capacity too low");
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
                                throw new MatchCancellationNotFoundException("Missing match");
                            }
                            if (actingUser.getId() != 7L) {
                                throw new MatchCancellationForbiddenException("Forbidden");
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
                                throw new MatchCancellationNotFoundException("Missing match");
                            }
                            if (actingUser.getId() != 7L) {
                                throw new MatchCancellationForbiddenException("Forbidden");
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
        return new Match(
                matchId,
                request.getSport(),
                actingUser,
                request.getAddress(),
                null,
                null,
                request.getTitle(),
                request.getDescription(),
                PlatformTime.toInstant(request.getStartDate(), request.getStartTime()),
                PlatformTime.toInstant(request.getEndDate(), request.getEndTime()),
                request.getMaxPlayers(),
                request.getPricePerPlayer(),
                request.getVisibility(),
                joinPolicy,
                request.getStatus(),
                0,
                null,
                null,
                null,
                false,
                null,
                null,
                null);
    }

    @Test
    void postHostPublishWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
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
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postHostPublishCreatesAndRedirectsForAuthenticatedUsers() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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

        final CreateMatchRequest request = captureCreateMatchRequest();
        Assertions.assertNotNull(request);
        Assertions.assertFalse(request.isRecurring());
        Assertions.assertEquals(LocalDate.parse("2099-04-10"), request.getStartDate());
        Assertions.assertEquals(LocalTime.parse("18:00"), request.getStartTime());
        Assertions.assertEquals(LocalDate.parse("2099-04-10"), request.getEndDate());
        Assertions.assertEquals(LocalTime.parse("19:30"), request.getEndTime());
    }

    @Test
    void postHostPublishPassesValidCoordinatesToService() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
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

        final CreateMatchRequest request = captureCreateMatchRequest();
        Assertions.assertNotNull(request);
        Assertions.assertEquals(-34.61, request.getLatitude());
        Assertions.assertEquals(-58.38, request.getLongitude());
    }

    @Test
    void postHostPublishRejectsPartialCoordinates() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
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

        Mockito.verify(matchService, Mockito.never())
                .createMatch(Mockito.any(CreateMatchRequest.class));
    }

    @Test
    void postHostPublishRejectsOutOfRangeCoordinates() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
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

        Mockito.verify(matchService, Mockito.never())
                .createMatch(Mockito.any(CreateMatchRequest.class));
    }

    @Test
    void postHostPublishCreatesRecurringMatchWithOccurrenceCount() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
                                .param("recurring", "true")
                                .param("recurrenceFrequency", "weekly")
                                .param("recurrenceEndMode", "occurrence_count")
                                .param("recurrenceOccurrenceCount", "3")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/43"));

        final CreateMatchRequest request = captureCreateMatchRequest();
        Assertions.assertNotNull(request);
        Assertions.assertTrue(request.isRecurring());
        Assertions.assertEquals("weekly", request.getRecurrence().getFrequency().getDbValue());
        Assertions.assertEquals(3, request.getRecurrence().getOccurrenceCount());
    }

    @Test
    void postHostPublishCreatesRecurringMatchWithUntilDate() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
                                .param("recurring", "true")
                                .param("recurrenceFrequency", "weekly")
                                .param("recurrenceEndMode", "until_date")
                                .param("recurrenceUntilDate", "2099-04-24")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/43"));

        final CreateMatchRequest request = captureCreateMatchRequest();
        Assertions.assertNotNull(request);
        Assertions.assertTrue(request.isRecurring());
        Assertions.assertEquals("until_date", request.getRecurrence().getEndMode().getDbValue());
        Assertions.assertEquals(LocalDate.of(2099, 4, 24), request.getRecurrence().getUntilDate());
    }

    @Test
    void postHostPublishRejectsInvalidRecurrenceFrequency() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
                        post("/host/matches/new")
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
                        post("/host/matches/new")
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
    void getHostEditRouteForNonHostReturnsNotFound() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/host/matches/42/edit")).andExpect(status().isNotFound());
    }

    @Test
    void getHostEditRouteForCompletedMatchReturnsNotFound() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches/44/edit")).andExpect(status().isNotFound());
    }

    @Test
    void getHostEditRouteForCancelledMatchReturnsNotFound() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches/45/edit")).andExpect(status().isNotFound());
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
                .andExpect(model().attribute("formTitle", "Edit recurring dates"))
                .andExpect(
                        model().attribute(
                                        "createEventForm",
                                        Matchers.hasProperty(
                                                "title", Matchers.is("Weekly Padel"))));
    }

    @Test
    void getHostSeriesEditRouteForSingleEventReturnsNotFound() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches/42/series/edit")).andExpect(status().isNotFound());
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

        final UpdateMatchRequest request = captureSeriesUpdateRequest();
        Assertions.assertEquals("Updated Weekly Padel", request.getTitle());
    }

    @Test
    void postHostEditPersistsPrivateVisibilityAsInviteOnly() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

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

        final Match updatedMatch =
                buildUpdatedMatch(42L, UserUtils.getUser(7L), captureUpdateRequest());
        Assertions.assertNotNull(updatedMatch);
        Assertions.assertEquals(EventVisibility.PRIVATE, updatedMatch.getVisibility());
        Assertions.assertEquals(EventJoinPolicy.INVITE_ONLY, updatedMatch.getJoinPolicy());
    }

    @Test
    void postHostEditPersistsPublicVisibilityAndJoinPolicyFromPrivateMatch() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

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

        final Match updatedMatch =
                buildUpdatedMatch(51L, UserUtils.getUser(7L), captureUpdateRequest());
        Assertions.assertNotNull(updatedMatch);
        Assertions.assertEquals(EventVisibility.PUBLIC, updatedMatch.getVisibility());
        Assertions.assertEquals(EventJoinPolicy.DIRECT, updatedMatch.getJoinPolicy());
    }

    @Test
    void postHostEditRejectsPublicInviteOnlyPolicyFromStaleFormState() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

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

        Mockito.verify(matchService, Mockito.never())
                .updateMatch(
                        Mockito.anyLong(),
                        Mockito.any(User.class),
                        Mockito.any(UpdateMatchRequest.class));
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
    void postHostCancelForNonHostReturnsNotFound() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/host/matches/42/cancel")).andExpect(status().isNotFound());
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

    private CreateMatchRequest captureCreateMatchRequest() {
        final ArgumentCaptor<CreateMatchRequest> captor =
                ArgumentCaptor.forClass(CreateMatchRequest.class);
        Mockito.verify(matchService).createMatch(captor.capture());
        return captor.getValue();
    }

    private UpdateMatchRequest captureUpdateRequest() {
        final ArgumentCaptor<UpdateMatchRequest> captor =
                ArgumentCaptor.forClass(UpdateMatchRequest.class);
        Mockito.verify(matchService)
                .updateMatch(Mockito.anyLong(), Mockito.any(User.class), captor.capture());
        return captor.getValue();
    }

    private UpdateMatchRequest captureSeriesUpdateRequest() {
        final ArgumentCaptor<UpdateMatchRequest> captor =
                ArgumentCaptor.forClass(UpdateMatchRequest.class);
        Mockito.verify(matchService)
                .updateSeriesFromOccurrence(
                        Mockito.anyLong(), Mockito.any(User.class), captor.capture());
        return captor.getValue();
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

    @SuppressWarnings("unchecked")
    private static LocalValidatorFactoryBean validator(
            final MessageSource messageSource,
            final UserEmailValidator userEmailValidator,
            final UsernameValidator usernameValidator) {
        final ConstraintValidatorFactory customConstraintFactory =
                new ConstraintValidatorFactory() {
                    @Override
                    public <T extends ConstraintValidator<?, ?>> T getInstance(final Class<T> key) {
                        if (key == UserEmailValidator.class) {
                            return (T) userEmailValidator;
                        } else if (key == UsernameValidator.class) {
                            return (T) usernameValidator;
                        }
                        try {
                            return key.getDeclaredConstructor().newInstance();
                        } catch (final Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void releaseInstance(final ConstraintValidator<?, ?> instance) {}
                };

        final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        validator.setConstraintValidatorFactory(customConstraintFactory);
        validator.afterPropertiesSet();
        return validator;
    }
}
