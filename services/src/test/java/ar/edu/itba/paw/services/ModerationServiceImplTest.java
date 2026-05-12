package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.AppealDecision;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewReaction;
import ar.edu.itba.paw.models.ReportReason;
import ar.edu.itba.paw.models.ReportResolution;
import ar.edu.itba.paw.models.ReportStatus;
import ar.edu.itba.paw.models.ReportTargetType;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.persistence.ModerationReportDao;
import ar.edu.itba.paw.persistence.PlayerReviewDao;
import ar.edu.itba.paw.persistence.UserBanDao;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.exceptions.ModerationException;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.MailProperties;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import ar.edu.itba.paw.services.mail.UnbanMailTemplateData;
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
    public void resolveReportWithUserBan_banExpiryIsInTheFuture() {
        final ModerationReport report = sampleUserReport();
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

        Mockito.when(userBanDao.createBan(Mockito.any(ModerationReport.class), Mockito.any()))
                .thenAnswer(
                        invocation -> {
                            capturedBannedUntil.set(invocation.getArgument(1));
                            return new UserBan(
                                    10L,
                                    (ModerationReport) invocation.getArgument(0),
                                    invocation.getArgument(1));
                        });

        Mockito.when(userDao.findAccountById(88L))
                .thenReturn(
                        Optional.of(
                                new UserAccount(
                                        88L,
                                        "reported@test.com",
                                        "reported",
                                        null,
                                        null,
                                        null,
                                        null,
                                        "{bcrypt}hash",
                                        UserRole.USER,
                                        Instant.parse("2026-04-10T10:00:00Z"),
                                        UserLanguages.DEFAULT_LANGUAGE)));

        final ModerationReport resolved =
                moderationService.resolveReport(
                        77L,
                        99L,
                        ReportResolution.USER_BANNED,
                        "Repeated abuse",
                        ReportStatus.RESOLVED);

        Assertions.assertNotNull(resolved, "resolveReport must return the updated report");
        Assertions.assertNotNull(
                capturedBannedUntil.get(), "A ban expiry must have been supplied to the DAO");
        Assertions.assertTrue(
                capturedBannedUntil.get().isAfter(FIXED_NOW),
                "Ban expiry must be strictly after the resolution instant");
    }

    @Test
    public void resolveReportWithUserBan_banIsLinkedToSourceReport() {
        final ModerationReport report = sampleUserReport();
        final AtomicLong capturedLinkedReportId = new AtomicLong(-1L);

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

        Mockito.when(userBanDao.createBan(Mockito.any(ModerationReport.class), Mockito.any()))
                .thenAnswer(
                        invocation -> {
                            final ModerationReport rep = invocation.getArgument(0);
                            capturedLinkedReportId.set(rep.getId());
                            return new UserBan(10L, rep, invocation.getArgument(1));
                        });

        Mockito.when(userDao.findAccountById(88L))
                .thenReturn(
                        Optional.of(
                                new UserAccount(
                                        88L,
                                        "reported@test.com",
                                        "reported",
                                        null,
                                        null,
                                        null,
                                        null,
                                        "{bcrypt}hash",
                                        UserRole.USER,
                                        Instant.parse("2026-04-10T10:00:00Z"),
                                        UserLanguages.DEFAULT_LANGUAGE)));

        moderationService.resolveReport(
                77L, 99L, ReportResolution.USER_BANNED, "Repeated abuse", ReportStatus.RESOLVED);

        Assertions.assertEquals(
                77L,
                capturedLinkedReportId.get(),
                "The ban must be linked to the originating report id");
    }

    @Test
    public void resolveReviewReport_contentDeleted_softDeletesCorrectReview() {
        final ModerationReport report = sampleReviewReport();
        final String reason = "Inappropriate content";

        final AtomicLong capturedReviewerUserId = new AtomicLong(-1L);
        final AtomicLong capturedReviewedUserId = new AtomicLong(-1L);
        final AtomicReference<String> capturedReason = new AtomicReference<>();

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
                                new PlayerReview(
                                        123L,
                                        7L,
                                        8L,
                                        PlayerReviewReaction.DISLIKE,
                                        "bad",
                                        FIXED_NOW,
                                        FIXED_NOW,
                                        null)));

        Mockito.when(
                        playerReviewDao.softDeleteReview(
                                Mockito.anyLong(), Mockito.anyLong(),
                                Mockito.anyLong(), Mockito.anyString()))
                .thenAnswer(
                        invocation -> {
                            capturedReviewerUserId.set(invocation.getArgument(0));
                            capturedReviewedUserId.set(invocation.getArgument(1));
                            capturedReason.set(invocation.getArgument(3));
                            return true;
                        });

        final ModerationReport resolved =
                moderationService.resolveReport(
                        45L, 99L, ReportResolution.CONTENT_DELETED, reason, ReportStatus.RESOLVED);

        Assertions.assertNotNull(resolved);
        Assertions.assertEquals(
                7L,
                capturedReviewerUserId.get(),
                "Soft-delete must target the reviewer identified by the stored review");
        Assertions.assertEquals(
                8L,
                capturedReviewedUserId.get(),
                "Soft-delete must target the reviewed user identified by the stored review");
        Assertions.assertEquals(
                reason,
                capturedReason.get(),
                "The resolution reason must be forwarded to the soft-delete call");
    }

    @Test
    public void findReportsByReporter_returnsExactlyTheReportsFromDao() {
        final List<ModerationReport> expected = List.of(sampleUserReport());
        Mockito.when(moderationReportDao.findReportsByReporter(50L, List.of(), List.of()))
                .thenReturn(expected);

        final List<ModerationReport> actual =
                moderationService.findReportsByReporter(50L, List.of(), List.of());

        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void findReports_returnsAllReportsReturnedByDao() {
        final ModerationReport report = sampleUserReport();
        Mockito.when(moderationReportDao.findReports()).thenReturn(List.of(report));

        final List<ModerationReport> reports = moderationService.findReports();

        Assertions.assertEquals(1, reports.size());
        Assertions.assertEquals(77L, reports.get(0).getId());
    }

    @Test
    public void reportContent_throwsModerationException_whenActiveReportLimitReached() {
        Mockito.when(moderationReportDao.countActiveReportsByReporter(50L)).thenReturn(3);

        Assertions.assertThrows(
                ModerationException.class,
                () ->
                        moderationService.reportContent(
                                50L, ReportTargetType.USER, 88L, ReportReason.SPAM, "Too many"));
    }

    @Test
    public void markReportUnderReview_returnsUpdatedReport() {
        final ModerationReport report = sampleUserReport();
        Mockito.when(
                        moderationReportDao.markUnderReview(
                                Mockito.eq(77L), Mockito.eq(99L), Mockito.any()))
                .thenReturn(true);
        Mockito.when(moderationReportDao.findById(77L)).thenReturn(Optional.of(report));

        final ModerationReport result = moderationService.markReportUnderReview(77L, 99L);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(
                77L, result.getId(), "Returned report id must match the requested report id");
    }

    @Test
    public void markReportUnderReview_throwsModerationException_whenDaoReturnsFalse() {
        Mockito.when(
                        moderationReportDao.markUnderReview(
                                Mockito.eq(77L), Mockito.eq(99L), Mockito.any()))
                .thenReturn(false);

        Assertions.assertThrows(
                ModerationException.class, () -> moderationService.markReportUnderReview(77L, 99L));
    }

    @Test
    public void appealReport_storesSuppliedAppealText() {
        final ModerationReport report = sampleUserReport(); // appealCount == 0
        final AtomicReference<String> capturedAppealText = new AtomicReference<>();

        Mockito.when(moderationReportDao.findById(77L)).thenReturn(Optional.of(report));
        Mockito.when(
                        moderationReportDao.appealReport(
                                Mockito.eq(77L), Mockito.anyString(), Mockito.any()))
                .thenAnswer(
                        invocation -> {
                            capturedAppealText.set(invocation.getArgument(1));
                            return true;
                        });

        moderationService.appealReport(77L, "Please reconsider");

        Assertions.assertEquals(
                "Please reconsider",
                capturedAppealText.get(),
                "The appeal text provided by the caller must reach the DAO unchanged");
    }

    @Test
    public void appealReport_throwsModerationException_whenAppealLimitAlreadyReached() {
        final ModerationReport reportWithExistingAppeal =
                new ModerationReport(
                        77L,
                        50L,
                        ReportTargetType.USER,
                        88L,
                        ReportReason.HARASSMENT,
                        "details",
                        ReportStatus.RESOLVED,
                        ReportResolution.USER_BANNED,
                        "reason",
                        99L,
                        FIXED_NOW,
                        "previous appeal",
                        1 /* appealCount >= 1 */,
                        FIXED_NOW,
                        null,
                        null,
                        null,
                        FIXED_NOW,
                        FIXED_NOW);

        Mockito.when(moderationReportDao.findById(77L))
                .thenReturn(Optional.of(reportWithExistingAppeal));

        Assertions.assertThrows(
                ModerationException.class,
                () -> moderationService.appealReport(77L, "Second appeal attempt"),
                "A second appeal must be rejected regardless of its content");
    }

    @Test
    public void softDeleteReview_forwardsAllArgumentsToDao() {
        final AtomicLong capturedReviewer = new AtomicLong();
        final AtomicLong capturedReviewed = new AtomicLong();
        final AtomicLong capturedDeletedBy = new AtomicLong();
        final AtomicReference<String> capturedReason = new AtomicReference<>();

        Mockito.when(
                        playerReviewDao.softDeleteReview(
                                Mockito.anyLong(), Mockito.anyLong(),
                                Mockito.anyLong(), Mockito.anyString()))
                .thenAnswer(
                        invocation -> {
                            capturedReviewer.set(invocation.getArgument(0));
                            capturedReviewed.set(invocation.getArgument(1));
                            capturedDeletedBy.set(invocation.getArgument(2));
                            capturedReason.set(invocation.getArgument(3));
                            return true;
                        });

        final boolean result = moderationService.softDeleteReview(1L, 2L, "SPAM", 3L);

        Assertions.assertTrue(result, "Should propagate the DAO's return value");
        Assertions.assertEquals(1L, capturedReviewer.get(), "reviewerUserId must be forwarded");
        Assertions.assertEquals(2L, capturedReviewed.get(), "reviewedUserId must be forwarded");
        Assertions.assertEquals(3L, capturedDeletedBy.get(), "deletedByUserId must be forwarded");
        Assertions.assertEquals("SPAM", capturedReason.get(), "reason must be forwarded");
    }

    @Test
    public void restoreReview_forwardsUserIdsToDao() {
        final AtomicLong capturedReviewer = new AtomicLong();
        final AtomicLong capturedReviewed = new AtomicLong();

        Mockito.when(playerReviewDao.restoreReview(Mockito.anyLong(), Mockito.anyLong()))
                .thenAnswer(
                        invocation -> {
                            capturedReviewer.set(invocation.getArgument(0));
                            capturedReviewed.set(invocation.getArgument(1));
                            return true;
                        });

        moderationService.restoreReview(1L, 2L);

        Assertions.assertEquals(1L, capturedReviewer.get(), "reviewerUserId must be forwarded");
        Assertions.assertEquals(2L, capturedReviewed.get(), "reviewedUserId must be forwarded");
    }

    @Test
    public void softDeleteMatch_forwardsAllArgumentsToDao() {
        final AtomicLong capturedMatchId = new AtomicLong();
        final AtomicLong capturedDeletedBy = new AtomicLong();
        final AtomicReference<String> capturedReason = new AtomicReference<>();

        Mockito.when(
                        matchDao.softDeleteMatch(
                                Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString()))
                .thenAnswer(
                        invocation -> {
                            capturedMatchId.set(invocation.getArgument(0));
                            capturedDeletedBy.set(invocation.getArgument(1));
                            capturedReason.set(invocation.getArgument(2));
                            return true;
                        });

        moderationService.softDeleteMatch(10L, 99L, "Reason");

        Assertions.assertEquals(10L, capturedMatchId.get(), "matchId must be forwarded");
        Assertions.assertEquals(99L, capturedDeletedBy.get(), "deletedByUserId must be forwarded");
        Assertions.assertEquals("Reason", capturedReason.get(), "reason must be forwarded");
    }

    @Test
    public void finalizeReportAppeal_usesStoredUserLocaleForUnbanEmailTemplate() {
        final Long userId = 88L;
        final Locale spanishLocale = Locale.of("es");
        final UserAccount account =
                new UserAccount(
                        userId,
                        "test@test.com",
                        "test",
                        null,
                        null,
                        null,
                        null,
                        "hash",
                        UserRole.USER,
                        FIXED_NOW,
                        UserLanguages.SPANISH);
        final AtomicReference<Locale> capturedLocale = new AtomicReference<>();

        org.springframework.context.i18n.LocaleContextHolder.setLocale(Locale.ENGLISH);
        try {
            Mockito.when(userDao.findAccountById(userId)).thenReturn(Optional.of(account));
            Mockito.when(mailProperties.getBaseUrl()).thenReturn("https://test.com");

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
                                    Mockito.eq(77L), Mockito.eq(99L),
                                    Mockito.eq(AppealDecision.LIFTED), Mockito.any()))
                    .thenReturn(true);

            Mockito.when(templateRenderer.renderUnbanNotification(Mockito.any()))
                    .thenAnswer(
                            invocation -> {
                                final UnbanMailTemplateData data = invocation.getArgument(0);
                                capturedLocale.set(data.getLocale());
                                return Mockito.mock(MailContent.class);
                            });

            moderationService.finalizeReportAppeal(77L, 99L, AppealDecision.LIFTED);

            Assertions.assertEquals(
                    spanishLocale,
                    capturedLocale.get(),
                    "The stored user locale must be used when rendering the unban email");
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
