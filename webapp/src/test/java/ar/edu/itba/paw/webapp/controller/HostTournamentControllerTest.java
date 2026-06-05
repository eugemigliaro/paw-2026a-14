package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.services.CreateTournamentRequest;
import ar.edu.itba.paw.services.TournamentBracketFailureReason;
import ar.edu.itba.paw.services.TournamentBracketService;
import ar.edu.itba.paw.services.TournamentBracketView;
import ar.edu.itba.paw.services.TournamentJoinFailureReason;
import ar.edu.itba.paw.services.TournamentLifecycleFailureReason;
import ar.edu.itba.paw.services.TournamentMatchScheduleRequest;
import ar.edu.itba.paw.services.TournamentRegistrationService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.services.UpdateTournamentRequest;
import ar.edu.itba.paw.services.exceptions.TournamentBracketException;
import ar.edu.itba.paw.services.exceptions.TournamentLifecycleException;
import ar.edu.itba.paw.services.exceptions.TournamentRegistrationException;
import ar.edu.itba.paw.webapp.config.converters.StringToSportConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToTournamentPairingStrategyConverter;
import ar.edu.itba.paw.webapp.form.CreateTournamentForm;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import ar.edu.itba.paw.webapp.viewmodel.TournamentBracketViewModel;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
import org.springframework.test.web.servlet.MvcResult;
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
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HostTournamentController(
                                        tournamentService,
                                        tournamentRegistrationService,
                                        tournamentBracketService,
                                        messageSource,
                                        true,
                                        "/assets/tiles/{z}/{x}/{y}.png",
                                        "Local Buenos Aires map tiles",
                                        -34.6037,
                                        -58.3816,
                                        14))
                        .setValidator(validator(messageSource))
                        .setConversionService(conversionService())
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCreateTournamentIncludesMapPickerConfig() throws Exception {
        // Arrange + exercise + test
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
        Assertions.assertEquals(LocalDate.of(2030, 4, 10), createdRequest.get().getStartDate());
        Assertions.assertEquals(LocalTime.of(18, 0), createdRequest.get().getStartTime());
        Assertions.assertEquals(LocalDate.of(2030, 4, 10), createdRequest.get().getEndDate());
        Assertions.assertEquals(LocalTime.of(21, 0), createdRequest.get().getEndTime());
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
        mockMvc.perform(
                        createPost(
                                "City Football Cup", Sport.FOOTBALL.getDbValue(), "2", true, true))
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
    void postCloseRegistrationUnderCapacityRedirectsWithError() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(7L), "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(
                        tournamentRegistrationService.closeRegistration(
                                Mockito.eq(77L), Mockito.any(User.class)))
                .thenThrow(
                        new TournamentRegistrationException(
                                TournamentJoinFailureReason.UNDER_CAPACITY, "Not enough players"));

        // 2. Exercise + 3. Assert
        mockMvc.perform(post("/host/tournaments/77/close-registration"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tournaments/77"))
                .andExpect(
                        flash().attribute(
                                        "tournamentErrorCode",
                                        "tournament.registration.error.underCapacity"));
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
    void getEditTournamentWithLegacyEmptyScheduleReturnsBlankScheduleFields() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(tournamentService.findTournamentForHost(Mockito.eq(77L), Mockito.any()))
                .thenReturn(
                        Optional.of(
                                tournament(77L, host, TournamentStatus.REGISTRATION, null, null)));

        // 2. Exercise
        final MvcResult result =
                mockMvc.perform(get("/host/tournaments/77/edit").locale(Locale.ENGLISH))
                        .andExpect(status().isOk())
                        .andExpect(view().name("host/tournaments/create"))
                        .andExpect(model().attribute("isEditMode", true))
                        .andReturn();

        // 3. Assert
        final CreateTournamentForm form =
                (CreateTournamentForm)
                        result.getModelAndView().getModel().get("createTournamentForm");
        Assertions.assertNull(form.getStartDate());
        Assertions.assertNull(form.getStartTime());
        Assertions.assertNull(form.getEndDate());
        Assertions.assertNull(form.getEndTime());
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
        Assertions.assertEquals(LocalDate.of(2030, 4, 10), updatedRequest.get().getStartDate());
        Assertions.assertEquals(LocalTime.of(18, 0), updatedRequest.get().getStartTime());
        Assertions.assertEquals(LocalDate.of(2030, 4, 10), updatedRequest.get().getEndDate());
        Assertions.assertEquals(LocalTime.of(21, 0), updatedRequest.get().getEndTime());
        Assertions.assertEquals(16, updatedRequest.get().getBracketSize());
        Assertions.assertEquals(2, updatedRequest.get().getTeamSize());
    }

    @Test
    void postCreateWithStartBeforeRegistrationCloseReturnsForm() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(7L), "{bcrypt}hash", UserRole.USER, true);

        // 2. Exercise + 3. Assert
        mockMvc.perform(
                        createPostWithSchedule(
                                "City Padel Cup", "2030-04-09", "20:00", "2030-04-10", "21:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/tournaments/create"))
                .andExpect(model().attributeHasFieldErrors("createTournamentForm", "startTime"));
        Assertions.assertNull(createdRequest.get());
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
        final List<TournamentTeamMember> teamMembers =
                List.of(
                        member(firstTeam, UserUtils.getUser(11L)),
                        member(firstTeam, UserUtils.getUser(12L)),
                        member(secondTeam, UserUtils.getUser(13L)));
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
                                match,
                                teamMembers));

        // 2. Exercise + 3. Assert
        final var result =
                mockMvc.perform(get("/host/tournaments/77/bracket/setup"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("host/tournaments/bracket-setup"))
                        .andExpect(model().attributeExists("bracketPage"))
                        .andReturn();
        final TournamentBracketViewModel bracketPage =
                (TournamentBracketViewModel) result.getModelAndView().getModel().get("bracketPage");
        Assertions.assertNotNull(bracketPage);
        Assertions.assertEquals(2, bracketPage.getTeamRosters().size());
        Assertions.assertEquals(
                "user11, user12", bracketPage.getTeamRosters().get(0).getMembersLabel());
    }

    @Test
    void getBracketSetupForHostShowsTeamsWhenBracketIsNotGenerated() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        final Tournament tournament = tournament(77L, host, TournamentStatus.BRACKET_SETUP);
        final TournamentTeam firstTeam = team(1L, tournament, "Team One");
        final TournamentTeam secondTeam = team(2L, tournament, "Team Two");
        final List<TournamentTeamMember> teamMembers =
                List.of(
                        member(firstTeam, UserUtils.getUser(11L)),
                        member(firstTeam, UserUtils.getUser(12L)),
                        member(secondTeam, UserUtils.getUser(13L)));
        Mockito.when(tournamentService.findTournamentForHost(77L, host))
                .thenReturn(java.util.Optional.of(tournament));
        Mockito.when(tournamentBracketService.getBracket(77L, host))
                .thenThrow(
                        new TournamentBracketException(
                                TournamentBracketFailureReason.BRACKET_NOT_GENERATED,
                                "Not generated"));
        Mockito.when(tournamentBracketService.listTeamsForSetup(77L, host))
                .thenReturn(List.of(firstTeam, secondTeam));
        Mockito.when(tournamentRegistrationService.listTeamMembers(77L)).thenReturn(teamMembers);

        // 2. Exercise + 3. Assert
        final var result = mockMvc.perform(get("/host/tournaments/77/bracket/setup")).andReturn();

        final TournamentBracketViewModel bracketPage =
                (TournamentBracketViewModel) result.getModelAndView().getModel().get("bracketPage");
        Assertions.assertNotNull(bracketPage);
        Assertions.assertEquals(2, bracketPage.getTeamRosters().size());
        Assertions.assertEquals("Team One", bracketPage.getTeamRosters().get(0).getTeamName());
        Assertions.assertEquals(
                "user11, user12", bracketPage.getTeamRosters().get(0).getMembersLabel());
        Assertions.assertEquals("Team Two", bracketPage.getTeamRosters().get(1).getTeamName());
        Assertions.assertEquals("user13", bracketPage.getTeamRosters().get(1).getMembersLabel());
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
        final String expectedDate = LocalDate.now(PlatformTime.ZONE).plusDays(1).toString();

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
                                List.of(
                                        match.getTeamA(),
                                        match.getTeamB(),
                                        match2.getTeamA(),
                                        match2.getTeamB()),
                                List.of(match, match2),
                                null,
                                match));
        Mockito.when(tournamentService.findTournamentForHost(77L, host))
                .thenReturn(Optional.of(tournament));
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
        Mockito.when(tournamentService.findTournamentForHost(77L, host))
                .thenReturn(java.util.Optional.of(tournament));

        // 2. Exercise + 3. Assert
        mockMvc.perform(post("/host/tournaments/77/bracket/publish"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/tournaments/bracket-setup"));
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
                .param("sport", Sport.PADEL.getDbValue())
                .param("description", "Updated tournament")
                .param("address", "Updated Club")
                .param("registrationOpensDate", "2030-04-01")
                .param("registrationOpensTime", "09:00")
                .param("registrationClosesDate", "2030-04-09")
                .param("registrationClosesTime", "20:00")
                .param("startDate", "2030-04-10")
                .param("startTime", "18:00")
                .param("endDate", "2030-04-10")
                .param("endTime", "21:00")
                .param("bracketSize", "16")
                .param("teamSize", "2")
                .param("pricePerPlayer", "15.00")
                .param("allowSoloSignup", "true")
                .param("_allowTeamDraft", "on");
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
        return createPost(title, Sport.PADEL.getDbValue(), "1", allowSoloSignup, allowTeamDraft);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            createPost(
                    final String title,
                    final String sport,
                    final String teamSize,
                    final boolean allowSoloSignup,
                    final boolean allowTeamDraft) {
        return createPostWithSchedule(
                title,
                sport,
                teamSize,
                allowSoloSignup,
                allowTeamDraft,
                "2030-04-10",
                "18:00",
                "2030-04-10",
                "21:00");
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            createPostWithSchedule(
                    final String title,
                    final String startDate,
                    final String startTime,
                    final String endDate,
                    final String endTime) {
        return createPostWithSchedule(
                title,
                Sport.PADEL.getDbValue(),
                "1",
                true,
                true,
                startDate,
                startTime,
                endDate,
                endTime);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            createPostWithSchedule(
                    final String title,
                    final String sport,
                    final String teamSize,
                    final boolean allowSoloSignup,
                    final boolean allowTeamDraft,
                    final String startDate,
                    final String startTime,
                    final String endDate,
                    final String endTime) {
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
                        .param("startDate", startDate)
                        .param("startTime", startTime)
                        .param("endDate", endDate)
                        .param("endTime", endTime)
                        .param("bracketSize", "8")
                        .param("teamSize", teamSize)
                        .param("pricePerPlayer", "10.00");
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
                .param("schedules[0].startDate", "2030-04-10")
                .param("schedules[0].startTime", "18:00")
                .param("schedules[0].endDate", "2030-04-10")
                .param("schedules[0].endTime", "19:00")
                .param("schedules[0].address", "Downtown Club")
                .param("schedules[0].latitude", "-34.6")
                .param("schedules[0].longitude", "-58.4")
                .param("schedules[0].matchId", "10")
                .param("schedules[0].roundNumber", "1")
                .param("schedules[0].roundLabel", "Round 1")
                .param("schedules[0].matchLabel", "Match 1")
                .param("schedules[1].startDate", "2030-04-10")
                .param("schedules[1].startTime", "18:00")
                .param("schedules[1].endDate", "2030-04-10")
                .param("schedules[1].endTime", "19:00")
                .param("schedules[1].address", "Downtown Club")
                .param("schedules[1].latitude", "-34.6")
                .param("schedules[1].longitude", "-58.4")
                .param("schedules[1].matchId", "11")
                .param("schedules[1].roundNumber", "1")
                .param("schedules[1].roundLabel", "Round 1")
                .param("schedules[1].matchLabel", "Match 2");
    }

    private static Tournament tournament(
            final Long id, final User host, final TournamentStatus status) {
        return tournament(
                id,
                host,
                status,
                Instant.parse("2030-04-10T18:00:00Z"),
                Instant.parse("2030-04-10T21:00:00Z"));
    }

    private static Tournament tournament(
            final Long id,
            final User host,
            final TournamentStatus status,
            final Instant startsAt,
            final Instant endsAt) {
        return new Tournament(
                id,
                host,
                Sport.PADEL,
                "City Padel Cup",
                "Open city tournament",
                "Downtown Club",
                null,
                null,
                startsAt,
                endsAt,
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

    private static TournamentTeamMember member(final TournamentTeam team, final User user) {
        return new TournamentTeamMember(null, team, user, false, FIXED_NOW);
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

    private static DefaultFormattingConversionService conversionService() {
        final DefaultFormattingConversionService conversionService =
                new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToSportConverter());
        conversionService.addConverter(new StringToTournamentPairingStrategyConverter());
        return conversionService;
    }

    private static LocalValidatorFactoryBean validator(final MessageSource messageSource) {
        final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        validator.afterPropertiesSet();
        return validator;
    }
}
