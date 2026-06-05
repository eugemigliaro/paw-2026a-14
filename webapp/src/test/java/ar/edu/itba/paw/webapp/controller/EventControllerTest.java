package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.MatchUtils;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EventControllerTest {

    private MockMvc mockMvc;
    private MatchService matchService;
    private MatchReservationService matchReservationService;
    private MatchParticipationService matchParticipationService;
    private PlayerReviewService playerReviewService;
    private MessageSource messageSource;
    private Clock clock;

    private User host;

    @BeforeEach
    void setUp() {
        matchService = Mockito.mock(MatchService.class);
        matchReservationService = Mockito.mock(MatchReservationService.class);
        matchParticipationService = Mockito.mock(MatchParticipationService.class);
        playerReviewService = Mockito.mock(PlayerReviewService.class);
        messageSource = Mockito.mock(MessageSource.class);
        clock = Mockito.mock(Clock.class);

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new EventController(
                                        matchService,
                                        matchReservationService,
                                        matchParticipationService,
                                        playerReviewService,
                                        messageSource,
                                        clock))
                        .build();

        host =
                new User(
                        2L,
                        "host@test.com",
                        "hostUser",
                        "hostName",
                        "hostLastName",
                        null,
                        null,
                        UserLanguages.DEFAULT_LANGUAGE);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getEventReturnsNotFoundWhenMissing() throws Exception {
        Mockito.when(matchService.findMatchById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/matches/999")).andExpect(status().isNotFound());
    }

    @Test
    void postReservationRedirectsWithSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(host, "{bcrypt}hash", UserRole.USER, true);
        final Match match =
                MatchUtils.createMatchWithId(
                        42L, host.getId(), Sport.FOOTBALL, Instant.now().plusSeconds(3600), 10);
        Mockito.when(matchService.findMatchById(42L)).thenReturn(Optional.of(match));

        mockMvc.perform(post("/matches/42/reservations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42"))
                .andExpect(flash().attribute("reservationStatus", "confirmed"));
    }
}
