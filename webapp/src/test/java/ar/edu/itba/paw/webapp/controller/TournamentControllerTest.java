package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.services.TournamentRegistrationService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TournamentControllerTest {

    private static final Instant NOW = Instant.parse("2026-05-21T12:00:00Z");

    private MockMvc mockMvc;
    private TournamentService tournamentService;
    private TournamentRegistrationService tournamentRegistrationService;
    private User host;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tournamentService = Mockito.mock(TournamentService.class);
        tournamentRegistrationService = Mockito.mock(TournamentRegistrationService.class);
        host = UserUtils.getUser(7L);

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new TournamentController(
                                        tournamentService,
                                        tournamentRegistrationService,
                                        messageSource()))
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicDetailRendersRegistrationTournament() throws Exception {
        // 1. Arrange
        Mockito.when(tournamentService.findPublicTournament(77L))
                .thenReturn(Optional.of(tournament(77L, TournamentStatus.REGISTRATION)));
        Mockito.when(tournamentRegistrationService.findSoloEntry(Mockito.eq(77L), Mockito.isNull()))
                .thenReturn(Optional.empty());
        Mockito.when(tournamentRegistrationService.findUserTeam(Mockito.eq(77L), Mockito.isNull()))
                .thenReturn(Optional.empty());

        // 2. Exercise + 3. Assert
        mockMvc.perform(get("/tournaments/77").locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name("tournaments/detail"))
                .andExpect(model().attributeExists("tournamentPage"))
                .andExpect(
                        model().attribute(
                                        "tournamentPage",
                                        Matchers.hasProperty(
                                                "title", Matchers.is("City Padel Cup"))))
                .andExpect(
                        model().attribute(
                                        "tournamentPage",
                                        Matchers.hasProperty("canJoinSolo", Matchers.is(false))))
                .andExpect(
                        model().attribute(
                                        "tournamentPage",
                                        Matchers.hasProperty(
                                                "requiresLoginToJoin", Matchers.is(true))));
    }

    @Test
    void loggedOutJoinIsDenied() throws Exception {
        // 1. Arrange

        // 2. Exercise + 3. Assert
        mockMvc.perform(post("/tournaments/77/solo-entry")).andExpect(status().isUnauthorized());
    }

    @Test
    void loggedInJoinRedirectsWithFlashState() throws Exception {
        // 1. Arrange
        final User player = UserUtils.getUser(9L);
        AuthenticationUtils.authenticateUser(player, "{bcrypt}hash", UserRole.USER, true);
        Mockito.when(tournamentRegistrationService.joinSolo(77L, player))
                .thenReturn(
                        new TournamentSoloEntry(
                                300L,
                                tournament(77L, TournamentStatus.REGISTRATION),
                                player,
                                TournamentSoloEntryStatus.IN_POOL,
                                null,
                                NOW,
                                null));

        // 2. Exercise + 3. Assert
        mockMvc.perform(post("/tournaments/77/solo-entry"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tournaments/77"))
                .andExpect(
                        flash().attribute(
                                        "tournamentNoticeCode", "tournament.registration.joined"));
    }

    @Test
    void loggedInLeaveRedirectsWithFlashState() throws Exception {
        // 1. Arrange
        AuthenticationUtils.authenticateUser(
                UserUtils.getUser(9L), "{bcrypt}hash", UserRole.USER, true);

        // 2. Exercise + 3. Assert
        mockMvc.perform(post("/tournaments/77/solo-entry/leave"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tournaments/77"))
                .andExpect(
                        flash().attribute("tournamentNoticeCode", "tournament.registration.left"));
    }

    private Tournament tournament(final Long id, final TournamentStatus status) {
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
                NOW,
                NOW);
    }

    private static MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}
