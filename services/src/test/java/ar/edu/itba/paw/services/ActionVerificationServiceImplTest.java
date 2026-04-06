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
import ar.edu.itba.paw.services.mail.MailMode;
import ar.edu.itba.paw.services.mail.MailProperties;
import ar.edu.itba.paw.services.mail.MailService;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
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

    @Mock private EmailActionRequestDao emailActionRequestDao;
    @Mock private MatchDao matchDao;
    @Mock private MvpIdentityService mvpIdentityService;
    @Mock private MatchReservationService matchReservationService;
    @Mock private MailService mailService;
    @Mock private ThymeleafMailTemplateRenderer templateRenderer;

    private ActionVerificationServiceImpl actionVerificationService;

    @BeforeEach
    public void setUp() {
        actionVerificationService =
                new ActionVerificationServiceImpl(
                        emailActionRequestDao,
                        matchDao,
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
                        mailService,
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
        Mockito.verify(mailService)
                .send(ArgumentMatchers.eq("player@test.com"), ArgumentMatchers.any());
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
        Mockito.verify(emailActionRequestDao)
                .updateStatus(7L, EmailActionStatus.COMPLETED, 5L, FIXED_NOW);
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
        Mockito.verify(emailActionRequestDao)
                .updateStatus(7L, EmailActionStatus.EXPIRED, null, FIXED_NOW);
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
        Mockito.verify(emailActionRequestDao)
                .updateStatus(7L, EmailActionStatus.FAILED, 5L, FIXED_NOW);
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
                joinedPlayers);
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
