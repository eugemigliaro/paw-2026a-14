package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.webapp.config.converters.StringToEventStatusConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToEventTypeConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToEventVisibilityConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToMatchSortConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToSportConverter;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.EventCardViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FeedPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FilterGroupViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.SelectOptionViewModel;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class FeedControllerTournamentTest {

    private MatchService matchService;
    private TournamentService tournamentService;
    private MockMvc mockMvc;
    private AtomicBoolean matchSearchCalled;
    private AtomicBoolean tournamentSearchCalled;

    @BeforeEach
    void setUp() {
        matchService = Mockito.mock(MatchService.class);
        tournamentService = Mockito.mock(TournamentService.class);
        final MatchParticipationService matchParticipationService =
                Mockito.mock(MatchParticipationService.class);
        final MatchReservationService matchReservationService =
                Mockito.mock(MatchReservationService.class);
        final MessageSource messageSource = messageSource();

        matchSearchCalled = new AtomicBoolean(false);
        tournamentSearchCalled = new AtomicBoolean(false);

        Mockito.when(
                        matchService.searchPublicMatches(
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.<List<Sport>>any(),
                                ArgumentMatchers.nullable(LocalDate.class),
                                ArgumentMatchers.nullable(LocalDate.class),
                                ArgumentMatchers.nullable(EventSort.class),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.nullable(BigDecimal.class),
                                ArgumentMatchers.nullable(BigDecimal.class),
                                ArgumentMatchers.nullable(Double.class),
                                ArgumentMatchers.nullable(Double.class)))
                .thenAnswer(
                        invocation -> {
                            matchSearchCalled.set(true);
                            return new PaginatedResult<>(
                                    List.of(match(42L, "Sunrise Padel")),
                                    1,
                                    1,
                                    invocation.getArgument(6));
                        });
        Mockito.when(
                        tournamentService.searchPublicTournaments(
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.<List<Sport>>any(),
                                ArgumentMatchers.nullable(LocalDate.class),
                                ArgumentMatchers.nullable(LocalDate.class),
                                ArgumentMatchers.nullable(EventSort.class),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.nullable(BigDecimal.class),
                                ArgumentMatchers.nullable(BigDecimal.class),
                                ArgumentMatchers.nullable(Double.class),
                                ArgumentMatchers.nullable(Double.class)))
                .thenAnswer(
                        invocation -> {
                            tournamentSearchCalled.set(true);
                            return new PaginatedResult<>(
                                    List.of(tournament(77L, "City Cup")),
                                    13,
                                    invocation.getArgument(5),
                                    invocation.getArgument(6));
                        });

        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new FeedController(
                                        matchService,
                                        matchParticipationService,
                                        matchReservationService,
                                        tournamentService,
                                        messageSource,
                                        false,
                                        "",
                                        "",
                                        0,
                                        0,
                                        0))
                        .setViewResolvers(viewResolver)
                        .setConversionService(conversionService())
                        .defaultRequest(get("/").locale(Locale.ENGLISH))
                        .build();
    }

    @Test
    void defaultFeedStillUsesMatchSearch() throws Exception {
        final MvcResult result =
                mockMvc.perform(get("/"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("feed/index"))
                        .andExpect(model().attribute("selectedType", "match"))
                        .andReturn();

        final FeedPageViewModel feedPage =
                (FeedPageViewModel) result.getModelAndView().getModel().get("feedPage");

        Assertions.assertTrue(matchSearchCalled.get());
        Assertions.assertFalse(tournamentSearchCalled.get());
        Assertions.assertEquals("/matches/42", feedPage.getFeaturedEvents().get(0).getHref());
    }

    @Test
    void tournamentFeedUsesTournamentSearchAndBuildsTournamentCards() throws Exception {
        final MvcResult result =
                mockMvc.perform(
                                get("/").param("type", "tournament")
                                        .param("q", "cup")
                                        .param("sport", "padel")
                                        .param("sort", "price")
                                        .param("page", "2")
                                        .param("minPrice", "5")
                                        .param("maxPrice", "20"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("feed/index"))
                        .andExpect(model().attribute("selectedType", "tournament"))
                        .andReturn();

        final FeedPageViewModel feedPage =
                (FeedPageViewModel) result.getModelAndView().getModel().get("feedPage");
        final EventCardViewModel card = feedPage.getFeaturedEvents().get(0);

        Assertions.assertFalse(matchSearchCalled.get());
        Assertions.assertTrue(tournamentSearchCalled.get());
        Assertions.assertEquals("/tournaments/77", card.getHref());
        Assertions.assertEquals("Tournament", card.getBadge());
        Assertions.assertEquals("Registration", card.getLevel());
        Assertions.assertTrue(
                feedPage.getPaginationItems().stream()
                        .filter(item -> item.getHref() != null)
                        .allMatch(item -> item.getHref().contains("type=tournament")));
    }

    @Test
    void tournamentFeedShowsEventTypeFilterAndOmitsSpotsSortInSpanish() throws Exception {
        final MvcResult result =
                mockMvc.perform(
                                get("/").param("type", "tournament")
                                        .locale(Locale.forLanguageTag("es")))
                        .andExpect(status().isOk())
                        .andReturn();

        final FeedPageViewModel feedPage =
                (FeedPageViewModel) result.getModelAndView().getModel().get("feedPage");
        final List<SelectOptionViewModel> sortOptions = sortOptions(result);
        final FilterGroupViewModel eventTypeGroup =
                feedPage.getFilterGroups().stream()
                        .filter(group -> "Tipo de evento".equals(group.getTitle()))
                        .findFirst()
                        .orElseThrow();

        Assertions.assertEquals(2, eventTypeGroup.getOptions().size());
        Assertions.assertTrue(hasActiveOption(eventTypeGroup, "Torneos"));
        Assertions.assertTrue(
                sortOptions.stream()
                        .noneMatch(
                                option ->
                                        "M\u00e1s lugares disponibles".equals(option.getLabel())));
    }

    @Test
    void postExploreLocationWithTournamentTypePreservesTournamentFeedMode() throws Exception {
        mockMvc.perform(
                        post("/explore/location")
                                .param("latitude", "-34.61")
                                .param("longitude", "-58.38")
                                .param("type", "tournament"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?sort=distance&type=tournament"));
    }

    private static boolean hasActiveOption(final FilterGroupViewModel group, final String label) {
        return group.getOptions().stream()
                .anyMatch(option -> label.equals(option.getLabel()) && option.isActive());
    }

    @SuppressWarnings("unchecked")
    private static List<SelectOptionViewModel> sortOptions(final MvcResult result) {
        return (List<SelectOptionViewModel>) result.getModelAndView().getModel().get("sortOptions");
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
        return conversionService;
    }

    private static Match match(final long id, final String title) {
        return new Match(
                id,
                Sport.PADEL,
                user(7L, "host"),
                "Downtown Club",
                -34.61,
                -58.38,
                title,
                "Friendly doubles session",
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
    }

    private static Tournament tournament(final long id, final String title) {
        return new Tournament(
                id,
                user(8L, "host-tournament"),
                Sport.PADEL,
                title,
                "Competitive city tournament",
                "Central Club",
                -34.60,
                -58.38,
                Instant.parse("2026-04-12T10:00:00Z"),
                Instant.parse("2026-04-12T14:00:00Z"),
                BigDecimal.TEN,
                null,
                TournamentFormat.SINGLE_ELIMINATION,
                8,
                2,
                true,
                false,
                Instant.parse("2026-04-05T10:00:00Z"),
                Instant.parse("2026-04-10T10:00:00Z"),
                TournamentStatus.REGISTRATION,
                Instant.parse("2026-04-05T10:00:00Z"),
                Instant.parse("2026-04-05T10:00:00Z"));
    }

    private static User user(final long id, final String username) {
        return new User(id, username + "@example.com", username, "Host", "User", null, null, "en");
    }
}
