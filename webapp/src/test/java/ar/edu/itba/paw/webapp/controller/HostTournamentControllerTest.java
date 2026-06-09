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
import ar.edu.itba.paw.models.exceptions.tournament.TournamentForbiddenActionException;
import ar.edu.itba.paw.models.exceptions.tournamentBracket.TournamentBracketNotGeneratedException;
import ar.edu.itba.paw.models.exceptions.tournamentLifecycle.TournamentLifecycleNotCancellableException;
import ar.edu.itba.paw.models.exceptions.tournamentRegistration.TournamentRegistrationUnderCapacityException;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.services.CreateTournamentRequest;
import ar.edu.itba.paw.services.TournamentBracketService;
import ar.edu.itba.paw.services.TournamentBracketView;
import ar.edu.itba.paw.services.TournamentMatchScheduleRequest;
import ar.edu.itba.paw.services.TournamentRegistrationService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.services.UpdateTournamentRequest;
import ar.edu.itba.paw.webapp.config.converters.StringToSportConverter;
import ar.edu.itba.paw.webapp.config.converters.StringToTournamentPairingStrategyConverter;
import ar.edu.itba.paw.webapp.exception.AccessExceptionHandler;
import ar.edu.itba.paw.webapp.form.BracketPublishForm;
import ar.edu.itba.paw.webapp.form.CreateTournamentForm;
import ar.edu.itba.paw.webapp.security.annotation.CurrentUserArgumentResolver;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HostTournamentControllerTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-05-21T12:00:00Z");

    private MockMvc mockMvc;
    private TournamentService tournamentService;
    private TournamentRegistrationService tournamentRegistrationService;
    private TournamentBracketService tournamentBracketService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tournamentService = Mockito.mock(TournamentService.class);
        tournamentRegistrationService = Mockito.mock(TournamentRegistrationService.class);
        tournamentBracketService = Mockito.mock(TournamentBracketService.class);

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HostTournamentController(
                                        tournamentService,
                                        tournamentRegistrationService,
                                        tournamentBracketService,
                                        true,
                                        "/assets/tiles/{z}/{x}/{y}.png",
                                        "Local Buenos Aires map tiles",
                                        -34.6037,
                                        -58.3816,
                                        14))
                        .setConversionService(conversionService())
                        .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                        .setControllerAdvice(
                                new AccessExceptionHandler(Mockito.mock(MessageSource.class)))
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCreateTournamentIncludesMapPickerConfig() throws Exception {
        // Arrange + exercise + test
        mockMvc.perform(get("/tournaments/new").locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name("host/tournaments/create"))
                .andExpect(model().attribute("pageTitleCode", "page.title.hostTournamentCreate"))
                .andExpect(model().attribute("formTitleCode", "tournament.create.title"))
                .andExpect(model().attribute("formAction", "/tournaments"))
                .andExpect(model().attribute("submitLabelCode", "tournament.form.submit.create"))
                .andExpect(model().attribute("mapPickerEnabled", true))
                .andExpect(model().attribute("mapTileUrlTemplate", "/assets/tiles/{z}/{x}/{y}.png"))
                .andExpect(model().attribute("mapAttribution", "Local Buenos Aires map tiles"))
                .andExpect(model().attribute("mapDefaultZoom", 14));
    }

    @Test
    void getLegacyCreateTournamentRedirectsToCanonicalRoute() throws Exception {
        // Arrange + exercise + test
        mockMvc.perform(get("/host/tournaments/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tournaments/new"));
    }

    @Test
    void postCreateWithValidFormRedirectsToTournamentDetail() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(
                        tournamentService.createTournament(
                                Mockito.any(User.class),
                                Mockito.argThat(
                                        HostTournamentControllerTest::isValidCreateRequest)))
                .thenReturn(tournament(99L, host, TournamentStatus.REGISTRATION));

        // 2. Exercise + 3. Assert
        mockMvc.perform(validCreatePost())
                .andExpect(status().isSeeOther())
                .andExpect(redirectedUrl("/tournaments/99"));
    }

    @Test
    void postCreateWithoutTimezoneUsesArgentinaFallback() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(
                        tournamentService.createTournament(
                                Mockito.any(User.class),
                                Mockito.argThat(
                                        HostTournamentControllerTest
                                                ::isCreateRequestWithArgentinaFallback)))
                .thenReturn(tournament(99L, host, TournamentStatus.REGISTRATION));

        // 2. Exercise + 3. Assert
        mockMvc.perform(createPostWithoutTimezone("City Padel Cup"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tournaments/99"));
    }

    @Test
    void postCreateWithTeamDraftOnlyRedirectsToTournamentDetail() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(
                        tournamentService.createTournament(
                                Mockito.any(User.class),
                                Mockito.argThat(
                                        HostTournamentControllerTest
                                                ::isTeamDraftOnlyCreateRequest)))
                .thenReturn(tournament(99L, host, TournamentStatus.REGISTRATION));

        // 2. Exercise + 3. Assert
        mockMvc.perform(createPost("City Padel Cup", false, true))
                .andExpect(status().isSeeOther())
                .andExpect(redirectedUrl("/tournaments/99"));
    }

    @Test
    void postCreateWithInvalidFormReturnsForm() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(7L), "{bcrypt}hash", UserRole.USER, true);
        failIfTournamentCreationIsAttempted();

        // 2. Exercise + 3. Assert
        mockMvc.perform(createPost(""))
                .andExpect(status().isOk())
                .andExpect(view().name("host/tournaments/create"))
                .andExpect(model().attributeHasFieldErrors("createTournamentForm", "title"));
    }

    @Test
    void postCreateWithNoJoinModeReturnsForm() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(7L), "{bcrypt}hash", UserRole.USER, true);
        failIfTournamentCreationIsAttempted();

        // 2. Exercise + 3. Assert
        mockMvc.perform(createPost("City Padel Cup", false, false))
                .andExpect(status().isOk())
                .andExpect(view().name("host/tournaments/create"))
                .andExpect(
                        model().attributeHasFieldErrors("createTournamentForm", "allowSoloSignup"));
    }

    @Test
    void postCreateWithUnsupportedTeamSizeForSportReturnsForm() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(7L), "{bcrypt}hash", UserRole.USER, true);
        failIfTournamentCreationIsAttempted();

        // 2. Exercise + 3. Assert
        mockMvc.perform(
                        createPost(
                                "City Football Cup", Sport.FOOTBALL.getDbValue(), "2", true, true))
                .andExpect(status().isOk())
                .andExpect(view().name("host/tournaments/create"))
                .andExpect(model().attributeHasFieldErrors("createTournamentForm", "teamSize"));
    }

    @Test
    void postCreateWithOtherSportReturnsForm() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(7L), "{bcrypt}hash", UserRole.USER, true);
        failIfTournamentCreationIsAttempted();

        // 2. Exercise + 3. Assert
        mockMvc.perform(createPost("City Football Cup", Sport.OTHER.getDbValue(), "1", true, true))
                .andExpect(status().isOk())
                .andExpect(view().name("host/tournaments/create"))
                .andExpect(model().attributeHasFieldErrors("createTournamentForm", "sport"));
    }

    @Test
    void postCloseRegistrationByNonHostReturnsForbidden() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(9L), "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(
                        tournamentRegistrationService.closeRegistration(
                                Mockito.eq(77L), Mockito.any(User.class)))
                .thenThrow(new TournamentForbiddenActionException());

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
                .thenThrow(new TournamentRegistrationUnderCapacityException());

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
        Mockito.when(
                        tournamentService.findEditableTournamentForHost(
                                Mockito.eq(77L), Mockito.any()))
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
        Mockito.when(
                        tournamentService.findEditableTournamentForHost(
                                Mockito.eq(77L), Mockito.any()))
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
        Mockito.when(
                        tournamentService.findEditableTournamentForHost(
                                Mockito.eq(77L), Mockito.any()))
                .thenReturn(Optional.of(tournament(77L, host, TournamentStatus.REGISTRATION)));
        Mockito.when(
                        tournamentService.update(
                                Mockito.eq(77L),
                                Mockito.any(User.class),
                                Mockito.argThat(
                                        HostTournamentControllerTest::isValidUpdateRequest)))
                .thenReturn(tournament(77L, host, TournamentStatus.REGISTRATION));

        // 2. Exercise + 3. Assert
        mockMvc.perform(editPost(77L, "Updated City Cup"))
                .andExpect(status().isSeeOther())
                .andExpect(redirectedUrl("/tournaments/77"));
    }

    @Test
    void postCreateWithStartBeforeRegistrationCloseReturnsForm() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(7L), "{bcrypt}hash", UserRole.USER, true);
        failIfTournamentCreationIsAttempted();

        // 2. Exercise + 3. Assert
        mockMvc.perform(
                        createPostWithSchedule(
                                "City Padel Cup", "2030-04-09", "20:00", "2030-04-10", "21:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/tournaments/create"))
                .andExpect(model().attributeHasFieldErrors("createTournamentForm", "startTime"));
    }

    @Test
    void postEditTournamentByNonHostReturnsNotFound() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(9L), "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(
                        tournamentService.findEditableTournamentForHost(
                                Mockito.eq(77L), Mockito.any()))
                .thenReturn(Optional.empty());

        // 2. Exercise + 3. Assert
        mockMvc.perform(editPost(77L, "Updated City Cup")).andExpect(status().isNotFound());
    }

    @Test
    void postEditWithOtherSportReturnsForm() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(
                        tournamentService.findEditableTournamentForHost(
                                Mockito.eq(77L), Mockito.any()))
                .thenReturn(Optional.of(tournament(77L, host, TournamentStatus.REGISTRATION)));
        failIfTournamentCreationIsAttempted();

        // 2. Exercise + 3. Assert
        mockMvc.perform(
                        post("/host/tournaments/77/edit")
                                .locale(Locale.ENGLISH)
                                .param("title", "Title")
                                .param("sport", Sport.OTHER.getDbValue())
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
                                .param("_allowTeamDraft", "on"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/tournaments/create"))
                .andExpect(model().attributeHasFieldErrors("createTournamentForm", "sport"));
    }

    @Test
    void postCancelTournamentByHostRedirectsToTournamentDetail() throws Exception {
        // 1. Arrange
        final User host = UserUtils.getUser(7L);
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(
                        tournamentService.cancel(
                                Mockito.eq(77L),
                                Mockito.any(User.class),
                                Mockito.eq("Host cancelled tournament")))
                .thenReturn(tournament(77L, host, TournamentStatus.CANCELLED));

        // 2. Exercise + 3. Assert
        mockMvc.perform(post("/host/tournaments/77/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tournaments/77"));
    }

    @Test
    void postCancelTournamentByNonHostReturnsForbidden() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(9L), "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(
                        tournamentService.cancel(
                                Mockito.eq(77L), Mockito.any(User.class), Mockito.anyString()))
                .thenThrow(new TournamentForbiddenActionException());

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
                .thenThrow(new TournamentLifecycleNotCancellableException());

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
                        .andExpect(model().attributeExists("bracketView"))
                        .andExpect(model().attribute("bracketGenerated", true))
                        .andReturn();
        final Object teams = result.getModelAndView().getModel().get("bracketTeams");
        final Object membersByTeamId =
                result.getModelAndView().getModel().get("bracketMembersByTeamId");
        Assertions.assertEquals(List.of(firstTeam, secondTeam), teams);
        Assertions.assertEquals(List.of("user11", "user12"), ((Map<?, ?>) membersByTeamId).get(1L));
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
                .thenThrow(new TournamentBracketNotGeneratedException());
        Mockito.when(tournamentBracketService.listTeamsForSetup(77L, host))
                .thenReturn(List.of(firstTeam, secondTeam));
        Mockito.when(tournamentRegistrationService.listTeamMembers(77L)).thenReturn(teamMembers);

        // 2. Exercise + 3. Assert
        final var result = mockMvc.perform(get("/host/tournaments/77/bracket/setup")).andReturn();

        Assertions.assertEquals(false, result.getModelAndView().getModel().get("bracketGenerated"));
        Assertions.assertEquals(
                List.of(firstTeam, secondTeam),
                result.getModelAndView().getModel().get("bracketTeams"));
        final Object membersByTeamId =
                result.getModelAndView().getModel().get("bracketMembersByTeamId");
        Assertions.assertEquals(List.of("user11", "user12"), ((Map<?, ?>) membersByTeamId).get(1L));
        Assertions.assertEquals(List.of("user13"), ((Map<?, ?>) membersByTeamId).get(2L));
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
        final LocalDate expectedDate = LocalDate.now(PlatformTime.ZONE).plusDays(1);

        // 2. Exercise
        final var result = mockMvc.perform(get("/host/tournaments/77/bracket/setup")).andReturn();

        // 3. Assert
        final BracketPublishForm bracketPublishForm =
                (BracketPublishForm) result.getModelAndView().getModel().get("bracketPublishForm");
        Assertions.assertNotNull(bracketPublishForm);
        Assertions.assertEquals(
                expectedDate, bracketPublishForm.getSchedules().get(0).getStartDate());
        Assertions.assertEquals(
                LocalTime.of(18, 0), bracketPublishForm.getSchedules().get(0).getStartTime());
        Assertions.assertEquals(
                expectedDate, bracketPublishForm.getSchedules().get(0).getEndDate());
        Assertions.assertNull(bracketPublishForm.getSchedules().get(0).getEndTime());
        Assertions.assertEquals(
                expectedDate, bracketPublishForm.getSchedules().get(1).getStartDate());
        Assertions.assertEquals(
                LocalTime.of(19, 0), bracketPublishForm.getSchedules().get(1).getStartTime());
        Assertions.assertEquals(
                expectedDate, bracketPublishForm.getSchedules().get(1).getEndDate());
        Assertions.assertNull(bracketPublishForm.getSchedules().get(1).getEndTime());
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
                                Mockito.eq(77L),
                                Mockito.eq(host),
                                Mockito.argThat(
                                        HostTournamentControllerTest::isValidPublishSchedule)))
                .thenReturn(tournament(77L, host, TournamentStatus.IN_PROGRESS));

        // 2. Exercise + 3. Assert
        mockMvc.perform(validPublishPost())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tournaments/77"));
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
    }

    private static boolean isValidCreateRequest(final CreateTournamentRequest request) {
        return request != null
                && Sport.PADEL == request.getSport()
                && LocalDate.of(2030, 4, 10).equals(request.getStartDate())
                && LocalTime.of(18, 0).equals(request.getStartTime())
                && LocalDate.of(2030, 4, 10).equals(request.getEndDate())
                && LocalTime.of(21, 0).equals(request.getEndTime())
                && request.getBracketSize() == 8
                && request.getTeamSize() == 1
                && request.isAllowSoloSignup()
                && request.isAllowTeamDraft();
    }

    private static boolean isCreateRequestWithArgentinaFallback(
            final CreateTournamentRequest request) {
        return request != null
                && LocalDate.of(2030, 4, 1).equals(request.getRegistrationOpensDate())
                && LocalTime.of(9, 0).equals(request.getRegistrationOpensTime())
                && LocalDate.of(2030, 4, 9).equals(request.getRegistrationClosesDate())
                && LocalTime.of(20, 0).equals(request.getRegistrationClosesTime());
    }

    private static boolean isTeamDraftOnlyCreateRequest(final CreateTournamentRequest request) {
        return request != null && !request.isAllowSoloSignup() && request.isAllowTeamDraft();
    }

    private void failIfTournamentCreationIsAttempted() {
        Mockito.doThrow(new AssertionError("Invalid tournament forms must not create events"))
                .when(tournamentService)
                .createTournament(Mockito.any(User.class), Mockito.any());
    }

    private static boolean isValidUpdateRequest(final UpdateTournamentRequest request) {
        return request != null
                && "Updated City Cup".equals(request.getTitle())
                && Sport.PADEL == request.getSport()
                && request.getBracketSize() == 16
                && request.getTeamSize() == 2;
    }

    private static boolean isValidPublishSchedule(
            final List<TournamentMatchScheduleRequest> schedules) {
        return schedules != null
                && schedules.size() == 2
                && schedules.get(0).getMatchId() == 10L
                && "Downtown Club".equals(schedules.get(0).getAddress())
                && schedules.get(1).getMatchId() == 11L;
    }

    private static MockHttpServletRequestBuilder validCreatePost() {
        return createPost("City Padel Cup");
    }

    private static MockHttpServletRequestBuilder editPost(
            final long tournamentId, final String title) {
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

    private static MockHttpServletRequestBuilder createPost(final String title) {
        return createPost(title, true, true);
    }

    private static MockHttpServletRequestBuilder createPostWithoutTimezone(final String title) {
        return post("/tournaments")
                .locale(Locale.ENGLISH)
                .param("title", title)
                .param("sport", Sport.PADEL.getDbValue())
                .param("description", "Open city tournament")
                .param("address", "Downtown Club")
                .param("registrationOpensDate", "2030-04-01")
                .param("registrationOpensTime", "09:00")
                .param("registrationClosesDate", "2030-04-09")
                .param("registrationClosesTime", "20:00")
                .param("startDate", "2030-04-10")
                .param("startTime", "18:00")
                .param("endDate", "2030-04-10")
                .param("endTime", "21:00")
                .param("bracketSize", "8")
                .param("teamSize", "1")
                .param("pricePerPlayer", "10.00")
                .param("allowSoloSignup", "true")
                .param("allowTeamDraft", "true");
    }

    private static MockHttpServletRequestBuilder createPost(
            final String title, final boolean allowSoloSignup, final boolean allowTeamDraft) {
        return createPost(title, Sport.PADEL.getDbValue(), "1", allowSoloSignup, allowTeamDraft);
    }

    private static MockHttpServletRequestBuilder createPost(
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

    private static MockHttpServletRequestBuilder createPostWithSchedule(
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

    private static MockHttpServletRequestBuilder createPostWithSchedule(
            final String title,
            final String sport,
            final String teamSize,
            final boolean allowSoloSignup,
            final boolean allowTeamDraft,
            final String startDate,
            final String startTime,
            final String endDate,
            final String endTime) {
        final MockHttpServletRequestBuilder builder =
                post("/tournaments")
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

    private static MockHttpServletRequestBuilder validPublishPost() {
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
                .param("schedules[0].matchNumber", "1")
                .param("schedules[1].startDate", "2030-04-10")
                .param("schedules[1].startTime", "18:00")
                .param("schedules[1].endDate", "2030-04-10")
                .param("schedules[1].endTime", "19:00")
                .param("schedules[1].address", "Downtown Club")
                .param("schedules[1].latitude", "-34.6")
                .param("schedules[1].longitude", "-58.4")
                .param("schedules[1].matchId", "11")
                .param("schedules[1].roundNumber", "1")
                .param("schedules[1].matchNumber", "2");
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

    private static DefaultFormattingConversionService conversionService() {
        final DefaultFormattingConversionService conversionService =
                new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToSportConverter());
        conversionService.addConverter(new StringToTournamentPairingStrategyConverter());
        return conversionService;
    }
}
