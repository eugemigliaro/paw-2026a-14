package ar.edu.itba.paw.webapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventFilter;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventType;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.ParticipantStatus;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.webapp.config.converters.StringToEventCategoryConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToEventFilterConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToEventStatusConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToEventTypeConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToEventVisibilityConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToMatchSortConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToSportConverter;
import ar.edu.itba.paw.webapp.form.SearchForm;
import ar.edu.itba.paw.webapp.security.annotation.CurrentUserArgumentResolver;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.MatchListControlsViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.SelectOptionViewModel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class MatchDashboardControllerTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T00:00:00Z");

    private MatchDashboardController controller;
    private MockMvc mockMvc;
    private boolean currentUserHasReservation;
    private boolean currentUserHasSeriesJoinRequest;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        currentUserHasReservation = false;
        currentUserHasSeriesJoinRequest = false;

        final Match realMatch =
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
        final Match completedMatch =
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
        final Match cancelledFutureMatch =
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
        final Match pendingFutureMatch =
                new Match(
                        56L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Downtown Club",
                        null,
                        null,
                        "Approval Future Padel",
                        "Future session with host approval",
                        Instant.parse("2030-04-09T20:00:00Z"),
                        Instant.parse("2030-04-09T21:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        1,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null);

        final MatchService matchService = Mockito.mock(MatchService.class);
        Mockito.when(
                        matchService.findDashboardMatches(
                                ArgumentMatchers.any(User.class),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.nullable(String.class),
                                ArgumentMatchers.<List<Sport>>any(),
                                ArgumentMatchers.<List<EventStatus>>any(),
                                ArgumentMatchers.nullable(java.time.LocalDate.class),
                                ArgumentMatchers.nullable(java.time.LocalDate.class),
                                ArgumentMatchers.nullable(BigDecimal.class),
                                ArgumentMatchers.nullable(BigDecimal.class),
                                ArgumentMatchers.nullable(EventSort.class),
                                ArgumentMatchers.<List<ParticipantStatus>>any(),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.anyInt()))
                .thenAnswer(
                        invocation -> {
                            final Boolean upcoming = invocation.getArgument(1);
                            final Boolean includeHosted = invocation.getArgument(2);
                            final List<EventStatus> statuses = invocation.getArgument(5);
                            final List<ParticipantStatus> participantStatuses =
                                    invocation.getArgument(11);
                            final int page = invocation.getArgument(12);
                            final int pageSize = invocation.getArgument(13);

                            final List<Match> matches = new ArrayList<>();

                            if (Boolean.TRUE.equals(includeHosted)) {
                                if (statuses != null && statuses.contains(EventStatus.COMPLETED)) {
                                    matches.add(completedMatch);
                                } else {
                                    matches.add(realMatch);
                                }
                            }

                            if (participantStatuses != null
                                    && participantStatuses.contains(
                                            ParticipantStatus.PENDING_APPROVAL)) {
                                matches.add(pendingFutureMatch);
                            }

                            if (Boolean.TRUE.equals(upcoming)) {
                                matches.add(realMatch);
                                matches.add(cancelledFutureMatch);
                            } else {
                                matches.add(completedMatch);
                            }

                            return new PaginatedResult<>(matches, matches.size(), page, pageSize);
                        });

        final MatchReservationService matchReservationService =
                Mockito.mock(MatchReservationService.class);
        Mockito.when(
                        matchReservationService.hasActiveReservation(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            final Long matchId = invocation.getArgument(0);
                            final User user = invocation.getArgument(1);
                            if (user == null) {
                                return false;
                            }
                            return currentUserHasReservation
                                    && (user.getId() == 9L || user.getId() == 7L)
                                    && (matchId == 42L || matchId == 51L);
                        });
        Mockito.when(
                        matchReservationService.findActiveFutureReservationMatchIdsForSeries(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenReturn(Set.of());

        final MatchParticipationService matchParticipationService =
                Mockito.mock(MatchParticipationService.class);
        Mockito.when(
                        matchParticipationService.hasPendingRequest(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenReturn(false);
        Mockito.when(
                        matchParticipationService.hasPendingSeriesRequest(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenReturn(false);
        Mockito.when(
                        matchParticipationService.findPendingFutureRequestMatchIdsForSeries(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            final Long seriesId = invocation.getArgument(0);
                            final User user = invocation.getArgument(1);
                            if (currentUserHasSeriesJoinRequest
                                    && user != null
                                    && user.getId() == 9L
                                    && seriesId == 700L) {
                                return Set.of(52L, 53L);
                            }
                            return Set.of();
                        });
        Mockito.when(
                        matchParticipationService.hasInvitation(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenReturn(false);

        final Tournament hostedTournament =
                new Tournament(
                        70L,
                        UserUtils.getUser(9L),
                        Sport.PADEL,
                        "Hosted Tournament",
                        "Tournament description",
                        "Tournament Club",
                        null,
                        null,
                        null,
                        null,
                        BigDecimal.TEN,
                        null,
                        TournamentFormat.SINGLE_ELIMINATION,
                        8,
                        1,
                        true,
                        false,
                        Instant.parse("2026-04-01T10:00:00Z"),
                        Instant.parse("2026-04-10T10:00:00Z"),
                        TournamentStatus.REGISTRATION,
                        FIXED_NOW,
                        FIXED_NOW);
        final TournamentService tournamentService = Mockito.mock(TournamentService.class);
        Mockito.when(
                        tournamentService.findDashboardTournaments(
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                ArgumentMatchers.nullable(String.class),
                                ArgumentMatchers.<List<Sport>>any(),
                                ArgumentMatchers.nullable(java.time.LocalDate.class),
                                ArgumentMatchers.nullable(java.time.LocalDate.class),
                                ArgumentMatchers.nullable(EventSort.class),
                                Mockito.anyInt(),
                                Mockito.anyInt(),
                                ArgumentMatchers.nullable(BigDecimal.class),
                                ArgumentMatchers.nullable(BigDecimal.class),
                                ArgumentMatchers.nullable(Double.class),
                                ArgumentMatchers.nullable(Double.class)))
                .thenReturn(new PaginatedResult<>(List.of(hostedTournament), 1, 1, 12));

        final MessageSource messageSource = messageSource();

        controller =
                new MatchDashboardController(
                        matchService,
                        matchParticipationService,
                        matchReservationService,
                        tournamentService,
                        messageSource);

        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setViewResolvers(viewResolver)
                        .setLocaleResolver(localeResolver())
                        .addInterceptors(localeChangeInterceptor())
                        .defaultRequest(get("/").locale(Locale.ENGLISH))
                        .setConversionService(conversionService())
                        .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void showEventsPageWithInvalidBindingReturnsControlledResponse() {
        AuthenticationUtils.authenticateUser(1L);

        final SearchForm searchForm = new SearchForm();
        searchForm.setQ("bad!");
        searchForm.setType(EventType.MATCH);
        searchForm.setMinPrice(BigDecimal.ONE);

        final BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(searchForm, "searchForm");
        bindingResult.rejectValue("q", "Pattern");

        try {
            controller.showEventsPage(
                    UserUtils.getUser(1L), searchForm, bindingResult, Locale.ENGLISH);
            fail("Expected a bad request response");
        } catch (final ResponseStatusException exception) {
            assertEquals(400, exception.getStatus().value());
        }
    }

    @Test
    void getEventsRouteRendersEventsPage() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/list"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attribute("pageTitleCode", "page.title.events"));
    }

    @Test
    void getEventsRouteBindsSubmittedPageParameter() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/events").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/list"))
                .andExpect(model().attribute("pageNumber", 2));
    }

    @Test
    void getEventsRouteRendersSortLinksAgainstEventsPath() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        final MvcResult result =
                mockMvc.perform(get("/events")).andExpect(status().isOk()).andReturn();

        final MatchListControlsViewModel listControls =
                (MatchListControlsViewModel)
                        result.getModelAndView().getModel().get("listControls");
        final SelectOptionViewModel firstSortOption = listControls.getSortOptions().get(0);
        Assertions.assertNull(firstSortOption.getHref());
        Assertions.assertTrue(firstSortOption.getParams().containsKey("sort"));
    }

    @Test
    void getEventsRouteRejectsInvalidPageParameter() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/events").param("page", "0")).andExpect(status().isBadRequest());
    }

    @Test
    void getEventsRouteBindsSportSelectionsAsTypedEnums() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        final MvcResult result =
                mockMvc.perform(
                                get("/events")
                                        .param("sport", "padel")
                                        .param("sport", "tennis")
                                        .param("sort", "price")
                                        .param("status", "open")
                                        .param("status", "completed")
                                        .param("visibility", "public")
                                        .param("visibility", "invite_only"))
                        .andExpect(status().isOk())
                        .andReturn();

        final SearchForm searchForm =
                (SearchForm) result.getModelAndView().getModel().get("searchForm");
        Assertions.assertEquals(List.of(Sport.PADEL, Sport.TENNIS), searchForm.getSport());
        Assertions.assertEquals(EventSort.PRICE_LOW, searchForm.getSort());
        Assertions.assertEquals(
                List.of(EventStatus.OPEN, EventStatus.COMPLETED), searchForm.getStatus());
        Assertions.assertEquals(
                List.of(EventVisibility.PUBLIC, EventVisibility.INVITE_ONLY),
                searchForm.getVisibility());
    }

    @Test
    void getEventsRoutePreservesPastFilterAsLowercaseQueryParam() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        final MvcResult result =
                mockMvc.perform(get("/events").param("filter", "past"))
                        .andExpect(status().isOk())
                        .andReturn();

        final SearchForm searchForm =
                (SearchForm) result.getModelAndView().getModel().get("searchForm");
        Assertions.assertEquals(EventFilter.PAST, searchForm.getFilter());
    }

    @Test
    void getEventsRouteRendersHostedTournamentsWhenTournamentTypeSelected() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        final MvcResult result =
                mockMvc.perform(
                                get("/events")
                                        .param("type", "tournament")
                                        .param("startDate", "2099-04-10")
                                        .param("endDate", "2099-04-11"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("events/list"))
                        .andExpect(model().attribute("selectedType", "tournament"))
                        .andReturn();

        final SearchForm searchForm =
                (SearchForm) result.getModelAndView().getModel().get("searchForm");
        Assertions.assertTrue(searchForm.getCategory().isEmpty());

        final List<Object> events = getEventsModel(result);
        Assertions.assertTrue(
                events.stream()
                        .filter(Tournament.class::isInstance)
                        .map(Tournament.class::cast)
                        .anyMatch(event -> "Hosted Tournament".equals(event.getTitle())));
    }

    @Test
    void getEventsRouteDoesNotIncludePendingCategoryByDefault() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");
        currentUserHasSeriesJoinRequest = true;

        final MvcResult result =
                mockMvc.perform(get("/events"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("events/list"))
                        .andReturn();

        final List<Object> events = getEventsModel(result);
        Assertions.assertTrue(
                events.stream()
                        .filter(Match.class::isInstance)
                        .map(Match.class::cast)
                        .noneMatch(event -> "Approval Future Padel".equals(event.getTitle())));
    }

    @Test
    void getEventsRouteIncludesPendingOnlyWhenPendingCategorySelected() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");
        currentUserHasSeriesJoinRequest = true;

        final MvcResult result =
                mockMvc.perform(get("/events").param("category", "pending"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("events/list"))
                        .andReturn();

        final List<Object> events = getEventsModel(result);
        Assertions.assertTrue(
                events.stream()
                        .filter(Match.class::isInstance)
                        .map(Match.class::cast)
                        .anyMatch(event -> "Approval Future Padel".equals(event.getTitle())));
    }

    @Test
    void getEventsRouteShowsHostedAndGoingBadgesTogether() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");
        currentUserHasReservation = true;

        final MvcResult result =
                mockMvc.perform(get("/events").param("category", "hosted"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("events/list"))
                        .andReturn();

        final List<Object> events = getEventsModel(result);
        final Match hostedEvent =
                events.stream()
                        .filter(Match.class::isInstance)
                        .map(Match.class::cast)
                        .filter(event -> "Sunrise Padel".equals(event.getTitle()))
                        .findFirst()
                        .orElseThrow(AssertionError::new);
        final Map<Long, List<String>> relationshipBadgeCodes = eventRelationshipBadgeCodes(result);
        Assertions.assertTrue(relationshipBadgeCodes.get(hostedEvent.getId()).contains("my_event"));
        Assertions.assertTrue(relationshipBadgeCodes.get(hostedEvent.getId()).contains("going"));
    }

    @SuppressWarnings("unchecked")
    private List<Object> getEventsModel(final MvcResult result) {
        return (List<Object>) result.getModelAndView().getModel().get("events");
    }

    @SuppressWarnings("unchecked")
    private Map<Long, List<String>> eventRelationshipBadgeCodes(final MvcResult result) {
        return (Map<Long, List<String>>)
                result.getModelAndView().getModel().get("eventRelationshipBadgeCodes");
    }

    private static MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    private static DefaultFormattingConversionService conversionService() {
        final DefaultFormattingConversionService conversionService =
                new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToSportConverter());
        conversionService.addConverter(new StringToEventStatusConverter());
        conversionService.addConverter(new StringToEventVisibilityConverter());
        conversionService.addConverter(new StringToMatchSortConverter());
        conversionService.addConverter(new StringToEventTypeConverter());
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
