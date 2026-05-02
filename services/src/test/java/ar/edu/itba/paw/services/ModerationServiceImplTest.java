package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.AppealDecision;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.ReportReason;
import ar.edu.itba.paw.models.ReportResolution;
import ar.edu.itba.paw.models.ReportStatus;
import ar.edu.itba.paw.models.ReportTargetType;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.persistence.ModerationReportDao;
import ar.edu.itba.paw.persistence.PlayerReviewDao;
import ar.edu.itba.paw.persistence.UserBanDao;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.MailProperties;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;

@ExtendWith(MockitoExtension.class)
public class ModerationServiceImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-20T12:00:00Z");

    @Mock private UserBanDao userBanDao;
    @Mock private ModerationReportDao moderationReportDao;
    @Mock private UserDao userDao;
    @Mock private MatchDao matchDao;
    @Mock private MatchParticipantDao matchParticipantDao;
    @Mock private PlayerReviewDao playerReviewDao;
    @Mock private MailDispatchService mailDispatchService;
    @Mock private MailProperties mailProperties;
    @Mock private ThymeleafMailTemplateRenderer templateRenderer;
    @Mock private MatchService matchService;

    private ModerationService moderationService;

    @BeforeEach
    public void setUp() {
        moderationService =
                new ModerationServiceImpl(
                        userBanDao,
                        moderationReportDao,
                        userDao,
                        matchDao,
                        matchParticipantDao,
                        playerReviewDao,
                        mailDispatchService,
                        mailProperties,
                        templateRenderer,
                        matchService,
                        messageSource(),
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    private static MessageSource messageSource() {
        final StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("ban.period.unknown", Locale.getDefault(), "Unknown");
        messageSource.addMessage("ban.period.7d", Locale.getDefault(), "7 days");
        messageSource.addMessage("ban.period.14d", Locale.getDefault(), "14 days");
        messageSource.addMessage("ban.period.30d", Locale.getDefault(), "30 days");
        messageSource.addMessage("ban.period.permanent", Locale.getDefault(), "Permanent");
        messageSource.addMessage(
                "moderation.action.defaultReason", Locale.getDefault(), "Moderation action reason");
        return messageSource;
    }

    @Test
    public void resolveReportWithUserBanCreatesUserBanAndResolvesReport() {
        final ModerationReport report = sampleUserReport();
        final AtomicLong capturedReportId = new AtomicLong(-1L);
        final AtomicReference<Instant> capturedBannedUntil = new AtomicReference<>();

        Mockito.when(mailProperties.getBaseUrl()).thenReturn("https://matchpoint.test");
        Mockito.when(
                        matchService.findJoinedMatches(
                                Mockito.anyLong(),
                                Mockito.anyBoolean(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.anyInt(),
                                Mockito.anyInt()))
                .thenReturn(new PaginatedResult<>(List.<Match>of(), 0, 1, 10));
        Mockito.when(
                        matchService.findHostedMatches(
                                Mockito.anyLong(),
                                Mockito.anyBoolean(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.anyInt(),
                                Mockito.anyInt()))
                .thenReturn(new PaginatedResult<>(List.<Match>of(), 0, 1, 10));

        Mockito.when(moderationReportDao.findById(77L)).thenReturn(Optional.of(report));
        Mockito.when(
                        moderationReportDao.resolveReport(
                                Mockito.eq(77L),
                                Mockito.eq(99L),
                                Mockito.eq(ReportResolution.USER_BANNED),
                                Mockito.anyString(),
                                Mockito.any(),
                                Mockito.eq(ReportStatus.RESOLVED)))
                .thenReturn(true);

        Mockito.when(userBanDao.createBan(Mockito.anyLong(), Mockito.any()))
                .thenAnswer(
                        invocation -> {
                            capturedReportId.set(invocation.getArgument(0));
                            capturedBannedUntil.set(invocation.getArgument(1));
                            return new UserBan(
                                    10L, invocation.getArgument(0), invocation.getArgument(1));
                        });

        Mockito.when(userDao.findAccountById(88L))
                .thenReturn(
                        Optional.of(
                                new UserAccount(
                                        88L,
                                        "reported@test.com",
                                        "reported",
                                        "{bcrypt}hash",
                                        UserRole.USER,
                                        FIXED_NOW)));

        final ModerationReport resolved =
                moderationService.resolveReport(
                        77L,
                        99L,
                        ReportResolution.USER_BANNED,
                        "Repeated abuse",
                        ReportStatus.RESOLVED);

        Assertions.assertNotNull(resolved);
        Assertions.assertEquals(77L, capturedReportId.get());
        Assertions.assertNotNull(capturedBannedUntil.get());
        Assertions.assertTrue(capturedBannedUntil.get().isAfter(FIXED_NOW));
    }

    @Test
    public void resolveReviewContentDeleteAppliesReviewSoftDelete() {
        final ModerationReport report = sampleReviewReport();
        final String reason = "Inappropriate content";

        Mockito.when(moderationReportDao.findById(45L)).thenReturn(Optional.of(report));
        Mockito.when(
                        moderationReportDao.resolveReport(
                                Mockito.eq(45L),
                                Mockito.eq(99L),
                                Mockito.eq(ReportResolution.CONTENT_DELETED),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.eq(ReportStatus.RESOLVED)))
                .thenReturn(true);

        Mockito.when(playerReviewDao.findByIdIncludingDeleted(123L))
                .thenReturn(
                        Optional.of(
                                new ar.edu.itba.paw.models.PlayerReview(
                                        123L,
                                        7L,
                                        8L,
                                        ar.edu.itba.paw.models.PlayerReviewReaction.DISLIKE,
                                        "bad",
                                        FIXED_NOW,
                                        FIXED_NOW,
                                        null)));

        Mockito.when(
                        playerReviewDao.softDeleteReview(
                                Mockito.anyLong(),
                                Mockito.anyLong(),
                                Mockito.anyLong(),
                                Mockito.anyString()))
                .thenReturn(true);

        final ModerationReport resolved =
                moderationService.resolveReport(
                        45L, 99L, ReportResolution.CONTENT_DELETED, reason, ReportStatus.RESOLVED);

        Assertions.assertNotNull(resolved);
        Mockito.verify(playerReviewDao).softDeleteReview(7L, 8L, 99L, reason);
    }

    @Test
    public void findReportsByReporterReturnsReportsFromDao() {
        final List<ModerationReport> expectedReports = List.of(sampleUserReport());
        Mockito.when(moderationReportDao.findReportsByReporter(50L, List.of(), List.of()))
                .thenReturn(expectedReports);

        final List<ModerationReport> reports =
                moderationService.findReportsByReporter(50L, List.of(), List.of());

        Assertions.assertEquals(expectedReports, reports);
    }

    @Test
    public void findActiveReportsIncludesReportsFromDao() {
        final ModerationReport report = sampleUserReport();
        Mockito.when(moderationReportDao.findReports()).thenReturn(List.of(report));

        final List<ModerationReport> reports = moderationService.findReports();

        Assertions.assertEquals(1, reports.size());
        Assertions.assertEquals(77L, reports.get(0).getId());
    }

    @Test
    public void reportContentEnforcesLimit() {
        Mockito.when(moderationReportDao.countActiveReportsByReporter(50L)).thenReturn(3);

        Assertions.assertThrows(
                ar.edu.itba.paw.services.exceptions.ModerationException.class,
                () ->
                        moderationService.reportContent(
                                50L, ReportTargetType.USER, 88L, ReportReason.SPAM, "Too many"));
    }

    @Test
    public void markReportUnderReviewDelegatesToDao() {
        final ModerationReport report = sampleUserReport();
        Mockito.when(
                        moderationReportDao.markUnderReview(
                                Mockito.eq(77L), Mockito.eq(99L), Mockito.any()))
                .thenReturn(true);
        Mockito.when(moderationReportDao.findById(77L)).thenReturn(Optional.of(report));

        final ModerationReport result = moderationService.markReportUnderReview(77L, 99L);

        Assertions.assertEquals(report, result);
        Mockito.verify(moderationReportDao)
                .markUnderReview(Mockito.eq(77L), Mockito.eq(99L), Mockito.any());
    }

    @Test
    public void appealReportDelegatesToDao() {
        final ModerationReport report = sampleUserReport();
        Mockito.when(moderationReportDao.findById(77L)).thenReturn(Optional.of(report));
        Mockito.when(
                        moderationReportDao.appealReport(
                                Mockito.eq(77L), Mockito.anyString(), Mockito.any()))
                .thenReturn(true);

        moderationService.appealReport(77L, "Appeal");

        Mockito.verify(moderationReportDao)
                .appealReport(Mockito.eq(77L), Mockito.eq("Appeal"), Mockito.any());
    }

    @Test
    public void finalizeReportAppealDelegatesToDao() {
        final ModerationReport report = sampleUserReport();
        final UserAccount account =
                new UserAccount(88L, "test@test.com", "test", "hash", UserRole.USER, FIXED_NOW);

        Mockito.when(moderationReportDao.findById(77L)).thenReturn(Optional.of(report));
        Mockito.when(userDao.findAccountById(88L)).thenReturn(Optional.of(account));
        Mockito.when(mailProperties.getBaseUrl()).thenReturn("https://test.com");
        Mockito.when(userBanDao.findActiveBanForUser(Mockito.eq(88L), Mockito.any()))
                .thenReturn(Optional.empty());

        Mockito.when(
                        moderationReportDao.finalizeAppeal(
                                Mockito.eq(77L),
                                Mockito.eq(99L),
                                Mockito.eq(AppealDecision.LIFTED),
                                Mockito.any()))
                .thenReturn(true);

        moderationService.finalizeReportAppeal(77L, 99L, AppealDecision.LIFTED);

        Mockito.verify(moderationReportDao)
                .finalizeAppeal(
                        Mockito.eq(77L),
                        Mockito.eq(99L),
                        Mockito.eq(AppealDecision.LIFTED),
                        Mockito.any());
        Mockito.verify(mailDispatchService).dispatch(Mockito.eq("test@test.com"), Mockito.any());
    }

    @Test
    public void softDeleteReviewDelegatesToDao() {
        moderationService.softDeleteReview(1L, 2L, "SPAM", 3L);
        Mockito.verify(playerReviewDao).softDeleteReview(1L, 2L, 3L, "SPAM");
    }

    @Test
    public void restoreReviewDelegatesToDao() {
        moderationService.restoreReview(1L, 2L);
        Mockito.verify(playerReviewDao).restoreReview(1L, 2L);
    }

    @Test
    public void softDeleteMatchDelegatesToDao() {
        moderationService.softDeleteMatch(10L, 99L, "Reason");
        Mockito.verify(matchDao).softDeleteMatch(10L, 99L, "Reason");
    }

    @Test
    public void sendUnbanEmailUsesCurrentLocale() {
        final Long userId = 88L;
        final Locale spanishLocale = Locale.of("es");
        final UserAccount account =
                new UserAccount(userId, "test@test.com", "test", "hash", UserRole.USER, FIXED_NOW);

        org.springframework.context.i18n.LocaleContextHolder.setLocale(spanishLocale);
        try {
            Mockito.when(userDao.findAccountById(userId)).thenReturn(Optional.of(account));
            Mockito.when(mailProperties.getBaseUrl()).thenReturn("https://test.com");

            // We use a report to trigger the uplift which calls sendUnbanEmail
            final ModerationReport report =
                    new ModerationReport(
                            77L,
                            50L,
                            ReportTargetType.USER,
                            userId,
                            ReportReason.HARASSMENT,
                            "details",
                            ReportStatus.RESOLVED,
                            ReportResolution.USER_BANNED,
                            "reason",
                            99L,
                            FIXED_NOW,
                            "appeal",
                            1,
                            FIXED_NOW,
                            null,
                            null,
                            null,
                            FIXED_NOW,
                            FIXED_NOW);

            Mockito.when(moderationReportDao.findById(77L)).thenReturn(Optional.of(report));
            Mockito.when(userBanDao.findActiveBanForUser(Mockito.eq(userId), Mockito.any()))
                    .thenReturn(Optional.empty());
            Mockito.when(
                            moderationReportDao.finalizeAppeal(
                                    Mockito.eq(77L),
                                    Mockito.eq(99L),
                                    Mockito.eq(AppealDecision.LIFTED),
                                    Mockito.any()))
                    .thenReturn(true);

            moderationService.finalizeReportAppeal(77L, 99L, AppealDecision.LIFTED);

            Mockito.verify(templateRenderer)
                    .renderUnbanNotification(
                            Mockito.argThat(data -> spanishLocale.equals(data.getLocale())));
        } finally {
            org.springframework.context.i18n.LocaleContextHolder.resetLocaleContext();
        }
    }

    private static ModerationReport sampleUserReport() {
        return new ModerationReport(
                77L,
                50L,
                ReportTargetType.USER,
                88L,
                ReportReason.HARASSMENT,
                "details",
                ReportStatus.UNDER_REVIEW,
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                null,
                FIXED_NOW,
                FIXED_NOW);
    }

    private static ModerationReport sampleReviewReport() {
        return new ModerationReport(
                45L,
                50L,
                ReportTargetType.REVIEW,
                123L,
                ReportReason.AGGRESSIVE_LANGUAGE,
                "details",
                ReportStatus.UNDER_REVIEW,
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                null,
                FIXED_NOW,
                FIXED_NOW);
    }
}
