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
import ar.edu.itba.paw.services.mail.VerificationMailTemplateData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ActionVerificationServiceImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T18:00:00Z");
    private static final DateTimeFormatter END_TIME_FORMATTER =
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.US);

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
                        new ObjectMapper(),
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
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
        Assertions.assertEquals("/matches/10?reservation=confirmed", result.getRedirectUrl());
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
        final Instant startsAt = FIXED_NOW.plusSeconds(7200);
        final Instant endsAt = FIXED_NOW.plusSeconds(10800);
        final CreateMatchRequest createRequest =
                new CreateMatchRequest(
                        null,
                        "Club Address",
                        "Host Event",
                        "Description",
                        startsAt,
                        endsAt,
                        10,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        "public",
                        "open",
                        null);
        final AtomicReference<VerificationMailTemplateData> capturedTemplateData =
                new AtomicReference<>();

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
                .thenAnswer(
                        invocation -> {
                            capturedTemplateData.set(invocation.getArgument(0));
                            return new MailContent("subject", "<p>html</p>", "text");
                        });

        final VerificationRequestResult result =
                actionVerificationService.requestMatchCreation(createRequest, "host@test.com");

        Assertions.assertEquals("host@test.com", result.getEmail());
        Assertions.assertNotNull(capturedTemplateData.get());
        Assertions.assertTrue(
                capturedTemplateData.get().getDetails().stream()
                        .anyMatch(
                                detail ->
                                        "End time".equals(detail.getLabel())
                                                && END_TIME_FORMATTER
                                                        .format(
                                                                endsAt.atZone(
                                                                        ZoneId.systemDefault()))
                                                        .equals(detail.getValue())));
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
        Assertions.assertEquals("/matches/55", result.getRedirectUrl());
    }

    @Test
    public void testConfirmMatchCreationPreservesEndTimeWhenPresent() {
        final long startsAtEpochMillis = 1775858400000L;
        final long endsAtEpochMillis = 1775865600000L;
        final EmailActionRequest request =
                new EmailActionRequest(
                        31L,
                        EmailActionType.MATCH_CREATION,
                        "host@test.com",
                        null,
                        "token-hash",
                        "{\"hostUserId\":null,\"address\":\"Club Address\",\"title\":\"Host Event\",\"description\":\"Description\",\"startsAtEpochMillis\":"
                                + startsAtEpochMillis
                                + ",\"endsAtEpochMillis\":"
                                + endsAtEpochMillis
                                + ",\"maxPlayers\":10,\"pricePerPlayer\":0,\"sport\":\"padel\",\"visibility\":\"public\",\"status\":\"open\"}",
                        EmailActionStatus.PENDING,
                        FIXED_NOW.plusSeconds(24 * 3600L),
                        null,
                        FIXED_NOW,
                        FIXED_NOW);
        final User user = new User(5L, "host@test.com", "host-player");
        final AtomicReference<CreateMatchRequest> capturedRequest = new AtomicReference<>();

        Mockito.when(emailActionRequestDao.findByTokenHashForUpdate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(request));
        Mockito.when(mvpIdentityService.resolveOrCreateByEmail("host@test.com")).thenReturn(user);
        Mockito.when(matchService.createMatch(ArgumentMatchers.any(CreateMatchRequest.class)))
                .thenAnswer(
                        invocation -> {
                            final CreateMatchRequest createRequest = invocation.getArgument(0);
                            capturedRequest.set(createRequest);
                            return new Match(
                                    55L,
                                    createRequest.getSport(),
                                    createRequest.getHostUserId(),
                                    createRequest.getAddress(),
                                    createRequest.getTitle(),
                                    createRequest.getDescription(),
                                    createRequest.getStartsAt(),
                                    createRequest.getEndsAt(),
                                    createRequest.getMaxPlayers(),
                                    createRequest.getPricePerPlayer(),
                                    createRequest.getVisibility(),
                                    createRequest.getStatus(),
                                    0,
                                    createRequest.getBannerImageId());
                        });

        final VerificationConfirmationResult result =
                actionVerificationService.confirm("raw-token");

        Assertions.assertEquals(5L, result.getUserId());
        Assertions.assertEquals(
                Instant.ofEpochMilli(endsAtEpochMillis), capturedRequest.get().getEndsAt());
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
}
