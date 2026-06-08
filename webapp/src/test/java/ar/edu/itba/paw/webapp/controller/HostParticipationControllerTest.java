package ar.edu.itba.paw.webapp.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.match.MatchClosedException;
import ar.edu.itba.paw.models.exceptions.match.MatchForbiddenActionException;
import ar.edu.itba.paw.models.exceptions.match.MatchStartedException;
import ar.edu.itba.paw.services.MatchInvitationResult;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.exception.AccessExceptionHandler;
import ar.edu.itba.paw.webapp.exception.PasswordResetExceptionHandler;
import ar.edu.itba.paw.webapp.exception.VerificationExceptionHandler;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.validation.UserEmailValidator;
import java.util.Locale;
import java.util.Optional;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class HostParticipationControllerTest {

    private MockMvc mockMvc;
    private MatchParticipationService matchParticipationService;
    private MessageSource messageSource;
    private UserService userService;

    @BeforeEach
    void setUp() {
        matchParticipationService = Mockito.mock(MatchParticipationService.class);
        messageSource = Mockito.mock(MessageSource.class);
        userService = Mockito.mock(UserService.class);

        UserEmailValidator userEmailValidator = new UserEmailValidator(userService);

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HostParticipationController(
                                        matchParticipationService, userService, messageSource))
                        .setValidator(validator(userEmailValidator))
                        .setControllerAdvice(
                                new AccessExceptionHandler(),
                                new PasswordResetExceptionHandler(messageSource),
                                new VerificationExceptionHandler(messageSource))
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getHostJoinRequestsRouteRendersAggregateRequestsPage() throws Exception {
        AuthenticationUtils.authenticateUser(7L);
        when(matchParticipationService.findPendingRequestsForHost(Mockito.any()))
                .thenReturn(java.util.List.of());

        mockMvc.perform(get("/host/requests"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/participation/aggregate-requests"))
                .andExpect(model().attribute("aggregateRequests", true))
                .andExpect(model().attributeExists("pendingRequests"))
                .andExpect(model().attribute("matchesUrl", "/events"));
    }

    @Test
    void approveRequestRedirectsWithSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(1L);

        final User requestedUser = Mockito.mock(User.class);
        when(userService.findById(9L)).thenReturn(Optional.of(requestedUser));

        mockMvc.perform(post("/host/matches/42/requests/9/approve"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42"))
                .andExpect(flash().attribute("hostAction", "requestApproved"));
    }

    @Test
    void approveRequestMapsClosedError() throws Exception {
        AuthenticationUtils.authenticateUser(1L);

        final User requestedUser = Mockito.mock(User.class);

        when(userService.findById(9L)).thenReturn(Optional.of(requestedUser));
        when(messageSource.getMessage(
                        Mockito.eq("event.host.requests.error.closed"),
                        Mockito.isNull(),
                        Mockito.<Locale>any()))
                .thenReturn("This event is closed.");
        Mockito.doThrow(new MatchClosedException())
                .when(matchParticipationService)
                .approveRequest(
                        Mockito.eq(42L), Mockito.any(User.class), Mockito.eq(requestedUser));

        mockMvc.perform(post("/host/matches/42/requests/9/approve"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42"))
                .andExpect(flash().attribute("hostActionTarget", "requests"))
                .andExpect(flash().attribute("hostActionError", "This event is closed."));
    }

    @Test
    void approveRequestByNonHostReturnsForbiddenFromService() throws Exception {
        AuthenticationUtils.authenticateUser(9L);

        final User requestedUser = Mockito.mock(User.class);

        when(userService.findById(9L)).thenReturn(Optional.of(requestedUser));
        Mockito.doThrow(new MatchForbiddenActionException())
                .when(matchParticipationService)
                .approveRequest(
                        Mockito.eq(42L), Mockito.any(User.class), Mockito.eq(requestedUser));

        mockMvc.perform(post("/host/matches/42/requests/9/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectRequestRedirectsWithSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(1L);

        final User requestedUser = Mockito.mock(User.class);
        when(userService.findById(9L)).thenReturn(Optional.of(requestedUser));

        mockMvc.perform(post("/host/matches/42/requests/9/reject"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42"))
                .andExpect(flash().attribute("hostAction", "requestRejected"));
    }

    @Test
    void inviteUserRedirectsWithSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(1L);

        final User requestedUser = Mockito.mock(User.class);
        when(userService.findByEmail("test@test.com")).thenReturn(Optional.of(requestedUser));
        when(matchParticipationService.inviteUserWithResult(
                        Mockito.eq(42L),
                        Mockito.any(User.class),
                        Mockito.eq("test@test.com"),
                        Mockito.eq(false)))
                .thenReturn(MatchInvitationResult.singleMatch());

        mockMvc.perform(post("/host/matches/42/invites").param("email", "test@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42"))
                .andExpect(flash().attribute("hostAction", "inviteSent"));
    }

    @Test
    void removeParticipantRedirectsWithSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(1L);

        final User requestedUser = Mockito.mock(User.class);
        when(userService.findById(9L)).thenReturn(Optional.of(requestedUser));

        mockMvc.perform(post("/host/matches/42/participants/9/remove"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42"))
                .andExpect(flash().attribute("hostAction", "participantRemoved"));
    }

    @Test
    void removeParticipantMapsStartedError() throws Exception {
        AuthenticationUtils.authenticateUser(1L);

        final User requestedUser = Mockito.mock(User.class);
        when(userService.findById(9L)).thenReturn(Optional.of(requestedUser));
        when(messageSource.getMessage(
                        Mockito.eq("event.host.participants.error.started"),
                        Mockito.isNull(),
                        Mockito.<Locale>any()))
                .thenReturn("This event has already started.");
        Mockito.doThrow(new MatchStartedException())
                .when(matchParticipationService)
                .removeParticipant(
                        Mockito.eq(42L), Mockito.any(User.class), Mockito.eq(requestedUser));

        mockMvc.perform(post("/host/matches/42/participants/9/remove"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42"))
                .andExpect(flash().attribute("hostActionTarget", "participants"))
                .andExpect(flash().attribute("hostActionError", "This event has already started."));
    }

    @Test
    void showRosterByNonHostReturnsForbiddenFromService() throws Exception {
        AuthenticationUtils.authenticateUser(9L);
        Mockito.when(
                        matchParticipationService.findConfirmedParticipants(
                                Mockito.eq(42L), Mockito.any(User.class)))
                .thenThrow(new MatchForbiddenActionException());

        mockMvc.perform(get("/host/matches/42/participants")).andExpect(status().isForbidden());
    }

    private LocalValidatorFactoryBean validator(UserEmailValidator userEmailValidator) {
        ConstraintValidatorFactory customConstraintFactory =
                new ConstraintValidatorFactory() {
                    @Override
                    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                        if (key == UserEmailValidator.class) {
                            return (T) userEmailValidator;
                        }
                        try {
                            return key.getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void releaseInstance(ConstraintValidator<?, ?> instance) {}
                }; // TODO: find a better way to inject the custom validator without having to
        // reimplement the whole factory

        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        factoryBean.setConstraintValidatorFactory(customConstraintFactory);
        factoryBean.afterPropertiesSet();
        return factoryBean;
    }
}
