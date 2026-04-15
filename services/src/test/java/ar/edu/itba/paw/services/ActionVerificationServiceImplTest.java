package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EmailActionRequest;
import ar.edu.itba.paw.models.EmailActionStatus;
import ar.edu.itba.paw.models.EmailActionType;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.EmailActionRequestDao;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.services.exceptions.MatchReservationException;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.MailMode;
import ar.edu.itba.paw.services.mail.MailProperties;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;

@ExtendWith(MockitoExtension.class)
public class ActionVerificationServiceImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T18:00:00Z");

    @Mock private EmailActionRequestDao emailActionRequestDao;
    @Mock private MatchDao matchDao;
    @Mock private MatchService matchService;
    @Mock private MvpIdentityService mvpIdentityService;
    @Mock private MatchReservationService matchReservationService;
    @Mock private MailDispatchService mailDispatchService;
    @Mock private ThymeleafMailTemplateRenderer templateRenderer;

    private ActionVerificationServiceImpl actionVerificationService;

    @BeforeEach
    public void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        actionVerificationService =
                new ActionVerificationServiceImpl(
                        emailActionRequestDao,
                        matchDao,
                        matchService,
                        mvpIdentityService,
                        matchReservationService,
                        new MailProperties(
                                MailMode.LOG,
                                "http://localhost:8080",
                                "no-reply@matchpoint.local",
                                "",
                                587,
                                "",
                                "",
                                false,
                                true,
                                24),
                        mailDispatchService,
                        templateRenderer,
                        messageSource(),
                        new ObjectMapper(),
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @AfterEach
    public void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    public void testRequestMatchReservationCreatesPendingRequestAndSendsMail() {
        final Match match = createMatch(10L, "Morning Padel", 0);
        Mockito.when(matchDao.findPublicMatchById(10L)).thenReturn(Optional.of(match));
        Mockito.when(mvpIdentityService.findExistingByEmail("player@test.com"))
                .thenReturn(Optional.empty());
        Mockito.when(
                        emailActionRequestDao.create(
                                ArgumentMatchers.eq(EmailActionType.MATCH_RESERVATION),
                                ArgumentMatchers.eq("player@test.com"),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.eq(FIXED_NOW.plusSeconds(24 * 3600L))))
                .thenReturn(
                        new EmailActionRequest(
                                20L,
                                EmailActionType.MATCH_RESERVATION,
                                "player@test.com",
                                null,
                                "token-hash",
                                "{\"matchId\":10}",
                                EmailActionStatus.PENDING,
                                FIXED_NOW.plusSeconds(24 * 3600L),
                                null,
                                FIXED_NOW,
                                FIXED_NOW));
        Mockito.when(templateRenderer.renderReservationConfirmation(ArgumentMatchers.any()))
                .thenReturn(new MailContent("subject", "<p>html</p>", "text"));

        final VerificationRequestResult result =
                actionVerificationService.requestMatchReservation(10L, "player@test.com");

        Assertions.assertEquals("player@test.com", result.getEmail());
        Assertions.assertEquals(FIXED_NOW.plusSeconds(24 * 3600L), result.getExpiresAt());
    }

    @Test
    public void testConfirmCreatesUserAndCompletesReservation() {
        final EmailActionRequest request = pendingRequest("{\"matchId\":10}");
        final Match match = createMatch(10L, "Morning Padel", 0);
        final User user = new User(5L, "player@test.com", "player");

        Mockito.when(emailActionRequestDao.findByTokenHashForUpdate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(request));
        Mockito.when(matchDao.findPublicMatchById(10L)).thenReturn(Optional.of(match));
        Mockito.when(mvpIdentityService.resolveOrCreateByEmail("player@test.com")).thenReturn(user);

        final VerificationConfirmationResult result =
                actionVerificationService.confirm("raw-token");

        Assertions.assertEquals(5L, result.getUserId());
        Assertions.assertEquals("/events/10?reservation=confirmed", result.getRedirectUrl());
    }

    @Test
    public void testPreviewExpiredTokenMarksRequestAsExpired() {
        final EmailActionRequest request =
                new EmailActionRequest(
                        7L,
                        EmailActionType.MATCH_RESERVATION,
                        "player@test.com",
                        null,
                        "token-hash",
                        "{\"matchId\":10}",
                        EmailActionStatus.PENDING,
                        FIXED_NOW.minusSeconds(1),
                        null,
                        FIXED_NOW.minusSeconds(3600),
                        FIXED_NOW.minusSeconds(3600));

        Mockito.when(emailActionRequestDao.findByTokenHash(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(request));

        final VerificationFailureException exception =
                Assertions.assertThrows(
                        VerificationFailureException.class,
                        () -> actionVerificationService.getPreview("raw-token"));

        Assertions.assertEquals(VerificationFailureReason.EXPIRED, exception.getReason());
    }

    @Test
    public void testRequestMatchCreationCreatesPendingRequestAndSendsMail() {
        final CreateMatchRequest createRequest =
                new CreateMatchRequest(
                        null,
                        "Club Address",
                        "Host Event",
                        "Description",
                        FIXED_NOW.plusSeconds(7200),
                        null,
                        10,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        "public",
                        "open",
                        null);

        Mockito.when(mvpIdentityService.findExistingByEmail("host@test.com"))
                .thenReturn(Optional.empty());
        Mockito.when(
                        emailActionRequestDao.create(
                                ArgumentMatchers.eq(EmailActionType.MATCH_CREATION),
                                ArgumentMatchers.eq("host@test.com"),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.eq(FIXED_NOW.plusSeconds(24 * 3600L))))
                .thenReturn(
                        new EmailActionRequest(
                                30L,
                                EmailActionType.MATCH_CREATION,
                                "host@test.com",
                                null,
                                "token-hash",
                                "{}",
                                EmailActionStatus.PENDING,
                                FIXED_NOW.plusSeconds(24 * 3600L),
                                null,
                                FIXED_NOW,
                                FIXED_NOW));
        Mockito.when(templateRenderer.renderReservationConfirmation(ArgumentMatchers.any()))
                .thenReturn(new MailContent("subject", "<p>html</p>", "text"));

        final VerificationRequestResult result =
                actionVerificationService.requestMatchCreation(createRequest, "host@test.com");

        Assertions.assertEquals("host@test.com", result.getEmail());
    }

    @Test
    public void testGetPreviewUsesSpanishLocaleForReservationContent() {
        final EmailActionRequest request = pendingRequest("{\"matchId\":10}");
        final Match match = createMatch(10L, "Morning Padel", 0);
        LocaleContextHolder.setLocale(new Locale("es"));

        Mockito.when(emailActionRequestDao.findByTokenHash(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(request));
        Mockito.when(matchDao.findPublicMatchById(10L)).thenReturn(Optional.of(match));

        final VerificationPreview preview = actionVerificationService.getPreview("raw-token");

        Assertions.assertEquals("Confirm\u00e1 tu reserva para Morning Padel", preview.getTitle());
        Assertions.assertEquals("Confirmar reserva", preview.getConfirmLabel());
        Assertions.assertEquals("Deporte", preview.getDetails().get(0).getLabel());
        Assertions.assertEquals("P\u00e1del", preview.getDetails().get(0).getValue());
        Assertions.assertTrue(
                preview.getDetails().get(2).getValue().toLowerCase(Locale.ROOT).contains("abr"));
    }

    @Test
    public void testConfirmMatchCreationPublishesEventAndRedirects() {
        final EmailActionRequest request =
                new EmailActionRequest(
                        31L,
                        EmailActionType.MATCH_CREATION,
                        "host@test.com",
                        null,
                        "token-hash",
                        "{\"hostUserId\":null,\"address\":\"Club Address\",\"title\":\"Host Event\",\"description\":\"Description\",\"startsAtEpochMillis\":1775858400000,\"endsAtEpochMillis\":null,\"maxPlayers\":10,\"pricePerPlayer\":0,\"sport\":\"padel\",\"visibility\":\"public\",\"status\":\"open\"}",
                        EmailActionStatus.PENDING,
                        FIXED_NOW.plusSeconds(24 * 3600L),
                        null,
                        FIXED_NOW,
                        FIXED_NOW);
        final User user = new User(5L, "host@test.com", "host-player");
        final Match createdMatch =
                new Match(
                        55L,
                        Sport.PADEL,
                        user.getId(),
                        "Club Address",
                        "Host Event",
                        "Description",
                        Instant.ofEpochMilli(1775858400000L),
                        null,
                        10,
                        BigDecimal.ZERO,
                        "public",
                        "open",
                        0,
                        null);

        // Arrange
        Mockito.when(emailActionRequestDao.findByTokenHashForUpdate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(request));
        Mockito.when(mvpIdentityService.resolveOrCreateByEmail("host@test.com")).thenReturn(user);
        Mockito.when(matchService.createMatch(ArgumentMatchers.any(CreateMatchRequest.class)))
                .thenReturn(createdMatch);

        // Exercise
        final VerificationConfirmationResult result =
                actionVerificationService.confirm("raw-token");

        // Assert
        Assertions.assertEquals(5L, result.getUserId());
        Assertions.assertEquals("/events/55", result.getRedirectUrl());
    }

    @Test
    public void testConfirmWhenReservationCanNoLongerBeCompletedMarksRequestAsFailed() {
        final EmailActionRequest request = pendingRequest("{\"matchId\":10}");
        final Match match = createMatch(10L, "Morning Padel", 1);
        final User user = new User(5L, "player@test.com", "player");

        Mockito.when(emailActionRequestDao.findByTokenHashForUpdate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(request));
        Mockito.when(matchDao.findPublicMatchById(10L)).thenReturn(Optional.of(match));
        Mockito.when(mvpIdentityService.resolveOrCreateByEmail("player@test.com")).thenReturn(user);
        Mockito.doThrow(
                        new MatchReservationException(
                                "full",
                                "The event filled up before the reservation could be confirmed."))
                .when(matchReservationService)
                .reserveSpot(10L, 5L);

        final VerificationFailureException exception =
                Assertions.assertThrows(
                        VerificationFailureException.class,
                        () -> actionVerificationService.confirm("raw-token"));

        Assertions.assertEquals(VerificationFailureReason.INVALID_ACTION, exception.getReason());
    }

    private static Match createMatch(final Long id, final String title, final int joinedPlayers) {
        return new Match(
                id,
                Sport.PADEL,
                1L,
                "Club Address",
                title,
                "Description",
                FIXED_NOW.plusSeconds(7200),
                FIXED_NOW.plusSeconds(10800),
                8,
                BigDecimal.TEN,
                "public",
                "open",
                joinedPlayers,
                null);
    }

    private static EmailActionRequest pendingRequest(final String payloadJson) {
        return new EmailActionRequest(
                7L,
                EmailActionType.MATCH_RESERVATION,
                "player@test.com",
                null,
                "token-hash",
                payloadJson,
                EmailActionStatus.PENDING,
                FIXED_NOW.plusSeconds(24 * 3600L),
                null,
                FIXED_NOW.minusSeconds(60),
                FIXED_NOW.minusSeconds(60));
    }

    private static MessageSource messageSource() {
        final StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);

        messageSource.addMessage(
                "verification.preview.reservation.title",
                Locale.ENGLISH,
                "Confirm your reservation for {0}");
        messageSource.addMessage(
                "verification.preview.reservation.summary",
                Locale.ENGLISH,
                "Use this one-time confirmation to reserve your spot in the event.");
        messageSource.addMessage(
                "verification.preview.reservation.confirm", Locale.ENGLISH, "Confirm reservation");
        messageSource.addMessage(
                "verification.preview.creation.title",
                Locale.ENGLISH,
                "Confirm your event publication");
        messageSource.addMessage(
                "verification.preview.creation.summary",
                Locale.ENGLISH,
                "Use this one-time confirmation to publish your event.");
        messageSource.addMessage(
                "verification.preview.creation.confirm",
                Locale.ENGLISH,
                "Confirm event publication");
        messageSource.addMessage("verification.preview.detail.sport", Locale.ENGLISH, "Sport");
        messageSource.addMessage("verification.preview.detail.title", Locale.ENGLISH, "Title");
        messageSource.addMessage("verification.preview.detail.venue", Locale.ENGLISH, "Venue");
        messageSource.addMessage(
                "verification.preview.detail.schedule", Locale.ENGLISH, "Schedule");
        messageSource.addMessage("verification.preview.detail.price", Locale.ENGLISH, "Price");
        messageSource.addMessage(
                "verification.preview.detail.capacity", Locale.ENGLISH, "Capacity");
        messageSource.addMessage(
                "verification.preview.detail.spotsLeft", Locale.ENGLISH, "Spots left");
        messageSource.addMessage("sport.padel", Locale.ENGLISH, "Padel");
        messageSource.addMessage("price.free", Locale.ENGLISH, "Free");
        messageSource.addMessage("price.tbd", Locale.ENGLISH, "Price TBD");
        messageSource.addMessage("price.amount", Locale.ENGLISH, "${0}");
        messageSource.addMessage(
                "verification.message.reservationUnavailable",
                Locale.ENGLISH,
                "This event is no longer available for reservation.");
        messageSource.addMessage(
                "verification.message.eventFull", Locale.ENGLISH, "This event is already full.");
        messageSource.addMessage(
                "verification.message.alreadyReserved",
                Locale.ENGLISH,
                "This email already has a confirmed reservation for the event.");
        messageSource.addMessage(
                "verification.message.notFound",
                Locale.ENGLISH,
                "That verification link is invalid or no longer exists.");
        messageSource.addMessage(
                "verification.message.alreadyUsed",
                Locale.ENGLISH,
                "That verification link was already used.");
        messageSource.addMessage(
                "verification.message.expired",
                Locale.ENGLISH,
                "That verification link has expired.");
        messageSource.addMessage(
                "verification.message.reservationConfirmed",
                Locale.ENGLISH,
                "Your reservation is now confirmed.");
        messageSource.addMessage(
                "verification.message.eventPublished",
                Locale.ENGLISH,
                "Your event is now published.");
        messageSource.addMessage(
                "reservation.error.notFound",
                Locale.ENGLISH,
                "This event is no longer available for reservation.");
        messageSource.addMessage(
                "reservation.error.closed",
                Locale.ENGLISH,
                "Reservations for this event are closed.");
        messageSource.addMessage(
                "reservation.error.started", Locale.ENGLISH, "This event has already started.");
        messageSource.addMessage(
                "reservation.error.alreadyJoined",
                Locale.ENGLISH,
                "This email already has a confirmed reservation for the event.");
        messageSource.addMessage(
                "reservation.error.fullBeforeConfirm",
                Locale.ENGLISH,
                "The event filled up before the reservation could be confirmed.");

        final Locale spanish = new Locale("es");
        messageSource.addMessage(
                "verification.preview.reservation.title",
                spanish,
                "Confirm\u00e1 tu reserva para {0}");
        messageSource.addMessage(
                "verification.preview.reservation.summary",
                spanish,
                "Usá esta confirmación única para reservar tu lugar en el evento.");
        messageSource.addMessage(
                "verification.preview.reservation.confirm", spanish, "Confirmar reserva");
        messageSource.addMessage(
                "verification.preview.creation.title",
                spanish,
                "Confirmá la publicación de tu evento");
        messageSource.addMessage(
                "verification.preview.creation.summary",
                spanish,
                "Usá esta confirmación única para publicar tu evento.");
        messageSource.addMessage(
                "verification.preview.creation.confirm", spanish, "Confirmar publicación");
        messageSource.addMessage("verification.preview.detail.sport", spanish, "Deporte");
        messageSource.addMessage("verification.preview.detail.title", spanish, "Título");
        messageSource.addMessage("verification.preview.detail.venue", spanish, "Lugar");
        messageSource.addMessage("verification.preview.detail.schedule", spanish, "Horario");
        messageSource.addMessage("verification.preview.detail.price", spanish, "Precio");
        messageSource.addMessage("verification.preview.detail.capacity", spanish, "Capacidad");
        messageSource.addMessage(
                "verification.preview.detail.spotsLeft", spanish, "Lugares disponibles");
        messageSource.addMessage("sport.padel", spanish, "P\u00e1del");
        messageSource.addMessage("price.free", spanish, "Gratis");
        messageSource.addMessage("price.tbd", spanish, "Precio a confirmar");
        messageSource.addMessage("price.amount", spanish, "${0}");
        messageSource.addMessage(
                "verification.message.reservationUnavailable",
                spanish,
                "Este evento ya no está disponible para reserva.");
        messageSource.addMessage(
                "verification.message.eventFull", spanish, "Este evento ya está completo.");
        messageSource.addMessage(
                "verification.message.alreadyReserved",
                spanish,
                "Este email ya tiene una reserva confirmada para el evento.");
        messageSource.addMessage(
                "verification.message.notFound",
                spanish,
                "Ese enlace de verificación es inválido o ya no existe.");
        messageSource.addMessage(
                "verification.message.alreadyUsed",
                spanish,
                "Ese enlace de verificación ya fue usado.");
        messageSource.addMessage(
                "verification.message.expired", spanish, "Ese enlace de verificación expiró.");
        messageSource.addMessage(
                "verification.message.reservationConfirmed",
                spanish,
                "Tu reserva ahora está confirmada.");
        messageSource.addMessage(
                "verification.message.eventPublished", spanish, "Tu evento ahora está publicado.");
        messageSource.addMessage(
                "reservation.error.notFound",
                spanish,
                "Este evento ya no está disponible para reserva.");
        messageSource.addMessage(
                "reservation.error.closed",
                spanish,
                "Las reservas para este evento están cerradas.");
        messageSource.addMessage("reservation.error.started", spanish, "Este evento ya empezó.");
        messageSource.addMessage(
                "reservation.error.alreadyJoined",
                spanish,
                "Este email ya tiene una reserva confirmada para el evento.");
        messageSource.addMessage(
                "reservation.error.fullBeforeConfirm",
                spanish,
                "El evento se llenó antes de que pudiera confirmarse la reserva.");

        return messageSource;
    }
}
