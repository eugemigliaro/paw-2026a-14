package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.services.CreateTournamentRequest;
import ar.edu.itba.paw.services.TournamentJoinFailureReason;
import ar.edu.itba.paw.services.TournamentRegistrationService;
import ar.edu.itba.paw.services.TournamentService;
import ar.edu.itba.paw.services.exceptions.TournamentRegistrationException;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
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
    private AtomicReference<CreateTournamentRequest> createdRequest;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tournamentService = Mockito.mock(TournamentService.class);
        tournamentRegistrationService = Mockito.mock(TournamentRegistrationService.class);
        createdRequest = new AtomicReference<>();

        final MessageSource messageSource = messageSource();
        final Clock clock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HostTournamentController(
                                        tournamentService,
                                        tournamentRegistrationService,
                                        messageSource,
                                        clock))
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
        Assertions.assertEquals(8, createdRequest.get().getBracketSize());
        Assertions.assertEquals(1, createdRequest.get().getTeamSize());
        Assertions.assertTrue(createdRequest.get().isAllowSoloSignup());
        Assertions.assertFalse(createdRequest.get().isAllowTeamDraft());
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

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            validCreatePost() {
        return createPost("City Padel Cup");
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            createPost(final String title) {
        return post("/host/tournaments")
                .locale(Locale.ENGLISH)
                .param("title", title)
                .param("sport", "padel")
                .param("description", "Open city tournament")
                .param("address", "Downtown Club")
                .param("startDate", "2030-04-10")
                .param("startTime", "18:00")
                .param("endDate", "2030-04-10")
                .param("endTime", "21:00")
                .param("registrationOpensDate", "2030-04-01")
                .param("registrationOpensTime", "09:00")
                .param("registrationClosesDate", "2030-04-09")
                .param("registrationClosesTime", "20:00")
                .param("bracketSize", "8")
                .param("teamSize", "1")
                .param("pricePerPlayer", "10.00")
                .param("allowSoloSignup", "true")
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
