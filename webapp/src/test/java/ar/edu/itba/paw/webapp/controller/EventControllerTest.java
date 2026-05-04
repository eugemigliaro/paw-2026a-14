package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.paw.models.EventJoinPolicy;
import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.EventVisibility;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EventControllerTest {

    private MockMvc mockMvc;
    private MatchService matchService;
    private MatchReservationService matchReservationService;
    private MatchParticipationService matchParticipationService;
    private PlayerReviewService playerReviewService;
    private UserService userService;
    private MessageSource messageSource;
    private Clock clock;

    @BeforeEach
    void setUp() {
        matchService = Mockito.mock(MatchService.class);
        matchReservationService = Mockito.mock(MatchReservationService.class);
        matchParticipationService = Mockito.mock(MatchParticipationService.class);
        playerReviewService = Mockito.mock(PlayerReviewService.class);
        userService = Mockito.mock(UserService.class);
        messageSource = Mockito.mock(MessageSource.class);
        clock = Mockito.mock(Clock.class);

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new EventController(
                                        matchService,
                                        matchReservationService,
                                        matchParticipationService,
                                        playerReviewService,
                                        userService,
                                        messageSource,
                                        clock))
                        .build();
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
        authenticateUser(2L);
        final Match match =
                new Match(
                        Long.valueOf(42),
                        Sport.FOOTBALL,
                        Long.valueOf(1),
                        "Address",
                        "Title",
                        "Desc",
                        Instant.now().plusSeconds(3600),
                        Instant.now().plusSeconds(4600),
                        10,
                        BigDecimal.ZERO,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        0,
                        null);
        Mockito.when(matchService.findMatchById(42L)).thenReturn(Optional.of(match));

        mockMvc.perform(post("/matches/42/reservations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42?reservation=confirmed"));
    }

    private static void authenticateUser(final Long userId) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUserPrincipal(
                                        new UserAccount(
                                                userId,
                                                "user@test.com",
                                                "user",
                                                "{bcrypt}hash",
                                                UserRole.USER,
                                                Instant.parse("2026-04-10T10:00:00Z"))),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
