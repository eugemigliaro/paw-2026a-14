package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.services.CreateTournamentRequest;
import ar.edu.itba.paw.services.TournamentBracketService;
import ar.edu.itba.paw.services.TournamentBracketView;
import ar.edu.itba.paw.services.TournamentJoinFailureReason;
import ar.edu.itba.paw.services.TournamentLifecycleFailureReason;
import ar.edu.itba.paw.services.TournamentMatchScheduleRequest;
import ar.edu.itba.paw.services.TournamentRegistrationService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.services.UpdateTournamentRequest;
import ar.edu.itba.paw.services.exceptions.TournamentLifecycleException;
import ar.edu.itba.paw.services.exceptions.TournamentRegistrationException;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import ar.edu.itba.paw.webapp.viewmodel.TournamentBracketViewModel;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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

class HostTournamentControllerTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-05-21T12:00:00Z");

    private MockMvc mockMvc;
    private TournamentService tournamentService;
    private TournamentRegistrationService tournamentRegistrationService;
    private TournamentBracketService tournamentBracketService;
    private AtomicReference<CreateTournamentRequest> createdRequest;
    private AtomicReference<UpdateTournamentRequest> updatedRequest;
    private AtomicReference<String> cancelReason;
    private AtomicReference<List<TournamentMatchScheduleRequest>> publishedSchedules;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tournamentService = Mockito.mock(TournamentService.class);
        tournamentRegistrationService = Mockito.mock(TournamentRegistrationService.class);
        tournamentBracketService = Mockito.mock(TournamentBracketService.class);
        createdRequest = new AtomicReference<>();
        updatedRequest = new AtomicReference<>();
        cancelReason = new AtomicReference<>();
        publishedSchedules = new AtomicReference<>();

        final MessageSource messageSource = messageSource();
        final Clock clock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HostTournamentController(
                                        tournamentService,
                                        tournamentRegistrationService,
                                        tournamentBracketService,
                                        messageSource,
                                        clock,
                                        true,
                                        "/assets/tiles/{z}/{x}/{y}.png",
                                        "Local Buenos Aires map tiles",
                                        -34.6037,
                                        -58.3816,
                                        14))
                        .setValidator(validator(messageSource))
                        .setConversionService(new DefaultFormattingConversionService())
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCreateRequiresAuthenticatedUser() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/host/tournaments/new")).andExpect(status().isUnauthorized());
    }

    @Test
    void getCreateTournamentIncludesMapPickerConfig() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(7L), "{bcrypt}hash", UserRole.USER, true);

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/host/tournaments/new").locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name("host/tournaments/create"))
                .andExpect(model().attribute("mapPickerEnabled", true))
                .andExpect(model().attribute("mapTileUrlTemplate", "/assets/tiles/{z}/{x}/{y}.png"))
                .andExpect(model().attribute("mapAttribution", "Local Buenos Aires map tiles"))
                .andExpect(model().attribute("mapDefaultZoom", 14));
    }

    @Test
    void postCreateWithValidFormRedirectsToTournamentDetail() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(
                        tournamentService.createTournament(
                                Mockito.any(User.class),
                                Mockito.any(CreateTournamentRequest.class)))
                .thenAnswer(
                        invocation -> {
                            createdRequest.set(invocation.getArgument(1));
                            return tournament(99L, host, TournamentStatus.REGISTRATION);
                        });

        // 2. Exercise + 3. Assert
        mockMvc.perform(validCreatePost())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tournaments/99"));
        Assertions.assertNotNull(createdRequest.get());
        Assertions.assertEquals(Sport.PADEL, createdRequest.get().getSport());
        Assertions.assertNull(createdRequest.get().getStartsAt());
        Assertions.assertNull(createdRequest.get().getEndsAt());
        Assertions.assertEquals(8, createdRequest.get().getBracketSize());
        Assertions.assertEquals(1, createdRequest.get().getTeamSize());
        Assertions.assertTrue(createdRequest.get().isAllowSoloSignup());
        Assertions.assertTrue(createdRequest.get().isAllowTeamDraft());
    }

    @Test
    void postCreateWithTeamDraftOnlyRedirectsToTournamentDetail() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(
                        tournamentService.createTournament(
                                Mockito.any(User.class),
                                Mockito.any(CreateTournamentRequest.class)))
                .thenAnswer(
                        invocation -> {
                            createdRequest.set(invocation.getArgument(1));
                            return tournament(99L, host, TournamentStatus.REGISTRATION);
                        });

        // 2. Exercise + 3. Assert
        mockMvc.perform(createPost("City Padel Cup", false, true))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tournaments/99"));
        Assertions.assertNotNull(createdRequest.get());
        Assertions.assertFalse(createdRequest.get().isAllowSoloSignup());
        Assertions.assertTrue(createdRequest.get().isAllowTeamDraft());
    }

    @Test
    void postCreateWithInvalidFormReturnsForm() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(7L), "{bcrypt}hash", UserRole.USER, true);

        // 2. Exercise + 3. Assert
        mockMvc.perform(createPost(""))
                .andExpect(status().isOk())
                .andExpect(view().name("host/tournaments/create"))
                .andExpect(model().attributeHasFieldErrors("createTournamentForm", "title"));
        Assertions.assertNull(createdRequest.get());
    }

    @Test
    void postCreateWithNoJoinModeReturnsForm() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(7L), "{bcrypt}hash", UserRole.USER, true);

        // 2. Exercise + 3. Assert
        mockMvc.perform(createPost("City Padel Cup", false, false))
                .andExpect(status().isOk())
                .andExpect(view().name("host/tournaments/create"))
                .andExpect(
                        model().attributeHasFieldErrors("createTournamentForm", "allowSoloSignup"));
        Assertions.assertNull(createdRequest.get());
    }

    @Test
    void postCreateWithUnsupportedTeamSizeForSportReturnsForm() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(7L), "{bcrypt}hash", UserRole.USER, true);

        // 2. Exercise + 3. Assert
        mockMvc.perform(createPost("City Football Cup", "football", "2", true, true))
                .andExpect(status().isOk())
                .andExpect(view().name("host/tournaments/create"))
                .andExpect(model().attributeHasFieldErrors("createTournamentForm", "teamSize"));
        Assertions.assertNull(createdRequest.get());
    }

    @Test
    void postCloseRegistrationByNonHostReturnsForbidden() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(9L), "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(
                        tournamentRegistrationService.closeRegistration(
                                Mockito.eq(77L), Mockito.any(User.class)))
                .thenThrow(
                        new TournamentRegistrationException(
                                TournamentJoinFailureReason.FORBIDDEN, "Forbidden"));

        // 2. Exercise + 3. Assert
        mockMvc.perform(post("/host/tournaments/77/close-registration"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEditTournamentByHostReturnsPrefilledForm() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(tournamentService.findTournamentForHost(Mockito.eq(77L), Mockito.any()))
                .thenReturn(Optional.of(tournament(77L, host, TournamentStatus.REGISTRATION)));

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/host/tournaments/77/edit").locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name("host/tournaments/create"))
                .andExpect(model().attribute("isEditMode", true));
    }

    @Test
    void postEditTournamentWithValidFormRedirectsToTournamentDetail() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(tournamentService.findTournamentForHost(Mockito.eq(77L), Mockito.any()))
                .thenReturn(Optional.of(tournament(77L, host, TournamentStatus.REGISTRATION)));
        Mockito.when(
                        tournamentService.update(
                                Mockito.eq(77L),
                                Mockito.any(User.class),
                                Mockito.any(UpdateTournamentRequest.class)))
                .thenAnswer(
                        invocation -> {
                            updatedRequest.set(invocation.getArgument(2));
                            return tournament(77L, host, TournamentStatus.REGISTRATION);
                        });

        // 2. Exercise + 3. Assert
        mockMvc.perform(editPost(77L, "Updated City Cup"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tournaments/77"));
        Assertions.assertNotNull(updatedRequest.get());
        Assertions.assertEquals("Updated City Cup", updatedRequest.get().getTitle());
        Assertions.assertEquals(Sport.PADEL, updatedRequest.get().getSport());
        Assertions.assertEquals(16, updatedRequest.get().getBracketSize());
        Assertions.assertEquals(2, updatedRequest.get().getTeamSize());
    }

    @Test
    void postEditTournamentByNonHostReturnsNotFound() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(9L), "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(tournamentService.findTournamentForHost(Mockito.eq(77L), Mockito.any()))
                .thenReturn(Optional.empty());

        // 2. Exercise + 3. Assert
        mockMvc.perform(editPost(77L, "Updated City Cup")).andExpect(status().isNotFound());
    }

    @Test
    void postCancelTournamentByHostRedirectsToTournamentDetail() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(
                        tournamentService.cancel(
                                Mockito.eq(77L), Mockito.any(User.class), Mockito.anyString()))
                .thenAnswer(
                        invocation -> {
                            cancelReason.set(invocation.getArgument(2));
                            return tournament(77L, host, TournamentStatus.CANCELLED);
                        });

        // 2. Exercise + 3. Assert
        mockMvc.perform(post("/host/tournaments/77/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tournaments/77"));
        Assertions.assertEquals("Host cancelled tournament", cancelReason.get());
    }

    @Test
    void postCancelTournamentByNonHostReturnsForbidden() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(9L), "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(
                        tournamentService.cancel(
                                Mockito.eq(77L), Mockito.any(User.class), Mockito.anyString()))
                .thenThrow(
                        new TournamentLifecycleException(
                                TournamentLifecycleFailureReason.FORBIDDEN, "Forbidden"));

        // 2. Exercise + 3. Assert
        mockMvc.perform(post("/host/tournaments/77/cancel")).andExpect(status().isForbidden());
    }

    @Test
    void postCancelCompletedTournamentRedirectsWithError() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(7L), "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(
                        tournamentService.cancel(
                                Mockito.eq(77L), Mockito.any(User.class), Mockito.anyString()))
                .thenThrow(
                        new TournamentLifecycleException(
                                TournamentLifecycleFailureReason.NOT_CANCELLABLE,
                                "Not cancellable"));

        // 2. Exercise + 3. Assert
        mockMvc.perform(post("/host/tournaments/77/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tournaments/77"));
    }

    @Test
    void postGenerateBracketRedirectsToSetup() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);

        // 2. Exercise + 3. Assert
        mockMvc.perform(post("/host/tournaments/77/bracket/generate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/host/tournaments/77/bracket/setup"));
    }

    @Test
    void getBracketSetupForHostRendersGeneratedBracket() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        final Tournament tournament = tournament(77L, host, TournamentStatus.BRACKET_SETUP);
        final TournamentTeam firstTeam = team(1L, tournament, "Team One");
        final TournamentTeam secondTeam = team(2L, tournament, "Team Two");
        final TournamentMatch match =
                new TournamentMatch(
                        10L,
                        tournament,
                        1,
                        0,
                        firstTeam,
                        secondTeam,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        TournamentMatchStatus.PENDING,
                        null,
                        null,
                        FIXED_NOW,
                        FIXED_NOW);
        Mockito.when(tournamentService.findTournamentForHost(77L, host))
                .thenReturn(java.util.Optional.of(tournament));
        Mockito.when(tournamentBracketService.getBracket(77L, host))
                .thenReturn(
                        new TournamentBracketView(
                                tournament,
                                java.util.List.of(firstTeam, secondTeam),
                                java.util.List.of(match),
                                null,
                                match));

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/host/tournaments/77/bracket/setup"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/tournaments/bracket-setup"))
                .andExpect(model().attributeExists("bracketPage"));
    }

    @Test
    void getBracketSetupDefaultsUnsheduledMatchTimesByRound() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        final Tournament tournament = tournament(77L, host, TournamentStatus.BRACKET_SETUP);
        final TournamentMatch roundOneMatch = bracketMatch(10L, tournament, 1);
        final TournamentMatch roundTwoMatch = bracketMatch(11L, tournament, 2);
        Mockito.when(tournamentService.findTournamentForHost(77L, host))
                .thenReturn(java.util.Optional.of(tournament));
        Mockito.when(tournamentBracketService.getBracket(77L, host))
                .thenReturn(
                        new TournamentBracketView(
                                tournament,
                                java.util.List.of(
                                        roundOneMatch.getTeamA(),
                                        roundOneMatch.getTeamB(),
                                        roundTwoMatch.getTeamA(),
                                        roundTwoMatch.getTeamB()),
                                java.util.List.of(roundOneMatch, roundTwoMatch),
                                null,
                                roundOneMatch));
        final String expectedDate = LocalDate.now(ZoneId.systemDefault()).toString();

        // 2. Exercise
        final var result = mockMvc.perform(get("/host/tournaments/77/bracket/setup")).andReturn();

        // 3. Assert
        final TournamentBracketViewModel bracketPage =
                (TournamentBracketViewModel) result.getModelAndView().getModel().get("bracketPage");
        Assertions.assertNotNull(bracketPage);
        Assertions.assertEquals(
                expectedDate, bracketPage.getRounds().get(0).getMatches().get(0).getStartDate());
        Assertions.assertEquals(
                "18:00", bracketPage.getRounds().get(0).getMatches().get(0).getStartTime());
        Assertions.assertEquals(
                expectedDate, bracketPage.getRounds().get(0).getMatches().get(0).getEndDate());
        Assertions.assertEquals(
                "", bracketPage.getRounds().get(0).getMatches().get(0).getEndTime());
        Assertions.assertEquals(
                expectedDate, bracketPage.getRounds().get(1).getMatches().get(0).getStartDate());
        Assertions.assertEquals(
                "19:00", bracketPage.getRounds().get(1).getMatches().get(0).getStartTime());
        Assertions.assertEquals(
                expectedDate, bracketPage.getRounds().get(1).getMatches().get(0).getEndDate());
        Assertions.assertEquals(
                "", bracketPage.getRounds().get(1).getMatches().get(0).getEndTime());
    }

    @Test
    void postPublishBracketWithValidRoundOneScheduleRedirectsToDetail() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        final Tournament tournament = tournament(77L, host, TournamentStatus.BRACKET_SETUP);
        final TournamentMatch match = bracketMatch(10L, tournament);
        final TournamentMatch match2 = bracketMatch(11L, tournament);
        Mockito.when(tournamentBracketService.getBracket(77L, host))
                .thenReturn(
                        new TournamentBracketView(
                                tournament,
                                java.util.List.of(
                                        match.getTeamA(),
                                        match.getTeamB(),
                                        match2.getTeamA(),
                                        match2.getTeamB()),
                                java.util.List.of(match, match2),
                                null,
                                match));
        Mockito.when(
                        tournamentBracketService.publishBracket(
                                Mockito.eq(77L), Mockito.eq(host), Mockito.anyList()))
                .thenAnswer(
                        invocation -> {
                            publishedSchedules.set(invocation.getArgument(2));
                            return tournament(77L, host, TournamentStatus.IN_PROGRESS);
                        });

        // 2. Exercise + 3. Assert
        mockMvc.perform(validPublishPost())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tournaments/77"));
        Assertions.assertNotNull(publishedSchedules.get());
        Assertions.assertEquals(2, publishedSchedules.get().size());
        Assertions.assertEquals(10L, publishedSchedules.get().get(0).getMatchId());
        Assertions.assertEquals("Downtown Club", publishedSchedules.get().get(0).getAddress());
        Assertions.assertEquals(11L, publishedSchedules.get().get(1).getMatchId());
    }

    @Test
    void postPublishBracketWithMissingScheduleRedirectsToSetup() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        final Tournament tournament = tournament(77L, host, TournamentStatus.BRACKET_SETUP);
        final TournamentMatch match = bracketMatch(10L, tournament);
        Mockito.when(tournamentBracketService.getBracket(77L, host))
                .thenReturn(
                        new TournamentBracketView(
                                tournament,
                                java.util.List.of(match.getTeamA(), match.getTeamB()),
                                java.util.List.of(match),
                                null,
                                match));

        // 2. Exercise + 3. Assert
        mockMvc.perform(post("/host/tournaments/77/bracket/publish").param("tz", "UTC"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/host/tournaments/77/bracket/setup"));
        Assertions.assertNull(publishedSchedules.get());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            validCreatePost() {
        return createPost("City Padel Cup");
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            editPost(final long tournamentId, final String title) {
        return post("/host/tournaments/" + tournamentId + "/edit")
                .locale(Locale.ENGLISH)
                .param("title", title)
                .param("sport", "padel")
                .param("description", "Updated tournament")
                .param("address", "Updated Club")
                .param("registrationOpensDate", "2030-04-01")
                .param("registrationOpensTime", "09:00")
                .param("registrationClosesDate", "2030-04-09")
                .param("registrationClosesTime", "20:00")
                .param("bracketSize", "16")
                .param("teamSize", "2")
                .param("pricePerPlayer", "15.00")
                .param("allowSoloSignup", "true")
                .param("_allowTeamDraft", "on")
                .param("tz", "UTC");
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            createPost(final String title) {
        return createPost(title, true, true);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            createPost(
                    final String title,
                    final boolean allowSoloSignup,
                    final boolean allowTeamDraft) {
        return createPost(title, "padel", "1", allowSoloSignup, allowTeamDraft);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            createPost(
                    final String title,
                    final String sport,
                    final String teamSize,
                    final boolean allowSoloSignup,
                    final boolean allowTeamDraft) {
        final org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder =
                post("/host/tournaments")
                        .locale(Locale.ENGLISH)
                        .param("title", title)
                        .param("sport", sport)
                        .param("description", "Open city tournament")
                        .param("address", "Downtown Club")
                        .param("registrationOpensDate", "2030-04-01")
                        .param("registrationOpensTime", "09:00")
                        .param("registrationClosesDate", "2030-04-09")
                        .param("registrationClosesTime", "20:00")
                        .param("bracketSize", "8")
                        .param("teamSize", teamSize)
                        .param("pricePerPlayer", "10.00")
                        .param("tz", "UTC");
        if (allowSoloSignup) {
            builder.param("allowSoloSignup", "true");
        } else {
            builder.param("_allowSoloSignup", "on");
        }
        if (allowTeamDraft) {
            builder.param("allowTeamDraft", "true");
        } else {
            builder.param("_allowTeamDraft", "on");
        }
        return builder;
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            validPublishPost() {
        return post("/host/tournaments/77/bracket/publish")
                .param("startDate_10", "2030-04-10")
                .param("startTime_10", "18:00")
                .param("endDate_10", "2030-04-10")
                .param("endTime_10", "19:00")
                .param("address_10", "Downtown Club")
                .param("latitude_10", "-34.6")
                .param("longitude_10", "-58.4")
                .param("startDate_11", "2030-04-10")
                .param("startTime_11", "18:00")
                .param("endDate_11", "2030-04-10")
                .param("endTime_11", "19:00")
                .param("address_11", "Downtown Club")
                .param("latitude_11", "-34.6")
                .param("longitude_11", "-58.4")
                .param("tz", "UTC");
    }

    private static Tournament tournament(
            final Long id, final User host, final TournamentStatus status) {
        return new Tournament(
                id,
                host,
                Sport.PADEL,
                "City Padel Cup",
                "Open city tournament",
                "Downtown Club",
                null,
                null,
                Instant.parse("2030-04-10T18:00:00Z"),
                Instant.parse("2030-04-10T21:00:00Z"),
                BigDecimal.TEN,
                null,
                TournamentFormat.SINGLE_ELIMINATION,
                8,
                1,
                true,
                false,
                Instant.parse("2030-04-01T09:00:00Z"),
                Instant.parse("2030-04-09T20:00:00Z"),
                status,
                FIXED_NOW,
                FIXED_NOW);
    }

    private static TournamentTeam team(
            final Long id, final Tournament tournament, final String name) {
        return new TournamentTeam(
                id, tournament, name, TournamentTeamOrigin.SOLO_POOL, null, FIXED_NOW);
    }

    private static TournamentMatch bracketMatch(final Long id, final Tournament tournament) {
        return bracketMatch(id, tournament, 1);
    }

    private static TournamentMatch bracketMatch(
            final Long id, final Tournament tournament, final int roundNumber) {
        return new TournamentMatch(
                id,
                tournament,
                roundNumber,
                0,
                team(1L, tournament, "Team One"),
                team(2L, tournament, "Team Two"),
                null,
                null,
                null,
                null,
                null,
                null,
                TournamentMatchStatus.PENDING,
                null,
                null,
                FIXED_NOW,
                FIXED_NOW);
    }

    private static MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    private static LocalValidatorFactoryBean validator(final MessageSource messageSource) {
        final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        validator.afterPropertiesSet();
        return validator;
    }
}
