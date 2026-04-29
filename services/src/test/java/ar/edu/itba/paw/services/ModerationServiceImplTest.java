package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.ReportReason;
import ar.edu.itba.paw.models.ReportResolution;
import ar.edu.itba.paw.models.ReportStatus;
import ar.edu.itba.paw.models.ReportTargetType;
import ar.edu.itba.paw.models.ReviewDeleteReason;
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
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    public void resolveUserBanReportCreatesUserBanAndResolvesReport() {
        final ModerationReport report = sampleUserReport();
        final AtomicLong capturedBanUserId = new AtomicLong(-1L);
        final AtomicReference<Instant> capturedBannedUntil = new AtomicReference<>();
        final AtomicReference<String> capturedBanReason = new AtomicReference<>();
        Mockito.when(mailProperties.getBaseUrl()).thenReturn("https://matchpoint.test");
        Mockito.when(
                        matchService.findJoinedMatches(
                                Mockito.anyLong(),
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
                                Mockito.any(),
                                Mockito.anyInt(),
                                Mockito.anyInt()))
                .thenReturn(new PaginatedResult<>(List.<Match>of(), 0, 1, 10));
        Mockito.when(
                        matchService.findHostedMatches(
                                Mockito.anyLong(),
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
        Mockito.when(moderationReportDao.findById(77L))
                .thenReturn(Optional.of(report))
                .thenReturn(Optional.of(report));
        Mockito.when(
                        userBanDao.createBan(
                                Mockito.anyLong(),
                                Mockito.anyLong(),
                                Mockito.any(),
                                Mockito.anyString()))
                .thenAnswer(
                        invocation -> {
                            capturedBanUserId.set(invocation.getArgument(0));
                            capturedBannedUntil.set(invocation.getArgument(2));
                            capturedBanReason.set(invocation.getArgument(3));
                            return new UserBan(
                                    10L,
                                    invocation.getArgument(0),
                                    invocation.getArgument(1),
                                    invocation.getArgument(3),
                                    invocation.getArgument(2),
                                    FIXED_NOW,
                                    null,
                                    0,
                                    null,
                                    null,
                                    null,
                                    null);
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
                moderationService.resolveUserBanReport(
                        77L, 99L, "Repeated abuse", 14, ReportStatus.RESOLVED);

        Assertions.assertNotNull(resolved);
        Assertions.assertEquals(88L, capturedBanUserId.get());
        Assertions.assertNotNull(capturedBannedUntil.get());
        Assertions.assertTrue(capturedBannedUntil.get().isAfter(FIXED_NOW));
        Assertions.assertEquals("Repeated abuse", capturedBanReason.get());
    }

    @Test
    public void resolveReviewContentDeleteAppliesReviewSoftDelete() {
        final ModerationReport report = sampleReviewReport();
        final AtomicReference<ReviewDeleteReason> capturedReason = new AtomicReference<>();
        final AtomicLong capturedReviewer = new AtomicLong(-1L);
        final AtomicLong capturedReviewed = new AtomicLong(-1L);
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
        Mockito.when(moderationReportDao.findById(45L))
                .thenReturn(Optional.of(report))
                .thenReturn(Optional.of(report));
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
                                Mockito.any(),
                                Mockito.anyLong()))
                .thenAnswer(
                        invocation -> {
                            capturedReviewer.set(invocation.getArgument(0));
                            capturedReviewed.set(invocation.getArgument(1));
                            capturedReason.set(invocation.getArgument(2));
                            return true;
                        });

        final ModerationReport resolved =
                moderationService.resolveReport(
                        45L,
                        99L,
                        ReportResolution.CONTENT_DELETED,
                        "Offensive review",
                        ReportStatus.RESOLVED);

        Assertions.assertNotNull(resolved);
        Assertions.assertEquals(7L, capturedReviewer.get());
        Assertions.assertEquals(8L, capturedReviewed.get());
        Assertions.assertEquals(ReviewDeleteReason.INAPPROPRIATE_CONTENT, capturedReason.get());
    }

    @Test
    public void findReportsByReporterReturnsReportsFromDao() {
        final List<ModerationReport> expectedReports = List.of(sampleUserReport());
        Mockito.when(moderationReportDao.findReportsByReporter(50L)).thenReturn(expectedReports);

        final List<ModerationReport> reports = moderationService.findReportsByReporter(50L);

        Assertions.assertEquals(expectedReports, reports);
    }

    @Test
    public void banUserCreatesBanAndSendsEmail() {
        final Instant bannedUntil = FIXED_NOW.plusSeconds(3600);
        final UserAccount account =
                new UserAccount(88L, "test@test.com", "test", "hash", UserRole.USER, FIXED_NOW);
        Mockito.when(userDao.findAccountById(88L)).thenReturn(Optional.of(account));
        Mockito.when(mailProperties.getBaseUrl()).thenReturn("https://test.com");
        Mockito.when(userBanDao.createBan(88L, 99L, bannedUntil, "Reason")).thenReturn(null);
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
                .thenReturn(new PaginatedResult<>(List.of(), 0, 1, 10));
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
                .thenReturn(new PaginatedResult<>(List.of(), 0, 1, 10));

        moderationService.banUser(88L, 99L, bannedUntil, "Reason");

        Mockito.verify(userBanDao).createBan(88L, 99L, bannedUntil, "Reason");
        Mockito.verify(mailDispatchService).dispatch(Mockito.eq("test@test.com"), Mockito.any());
    }

    @Test
    public void appealBanDelegatesToDao() {
        final UserBan ban =
                new UserBan(
                        10L,
                        88L,
                        99L,
                        "Reason",
                        FIXED_NOW.plusSeconds(3600),
                        FIXED_NOW,
                        null,
                        0,
                        null,
                        null,
                        null,
                        null);
        Mockito.when(userBanDao.findLatestBanForUser(88L)).thenReturn(Optional.of(ban));
        Mockito.when(userBanDao.appealBan(Mockito.eq(10L), Mockito.anyString(), Mockito.any()))
                .thenReturn(true);

        moderationService.appealBan(10L, 88L, "My appeal");

        Mockito.verify(userBanDao)
                .appealBan(Mockito.eq(10L), Mockito.eq("My appeal"), Mockito.any());
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

        moderationService.appealReport(77L, 50L, "Appeal");

        Mockito.verify(moderationReportDao)
                .appealReport(Mockito.eq(77L), Mockito.eq("Appeal"), Mockito.any());
    }

    @Test
    public void finalizeReportAppealDelegatesToDao() {
        final ModerationReport report = sampleUserReport();
        Mockito.when(moderationReportDao.findById(77L)).thenReturn(Optional.of(report));
        Mockito.when(
                        moderationReportDao.finalizeAppeal(
                                Mockito.eq(77L),
                                Mockito.eq(99L),
                                Mockito.eq(ReportResolution.WARNING),
                                Mockito.any()))
                .thenReturn(true);

        moderationService.finalizeReportAppeal(77L, 99L, ReportResolution.WARNING);

        Mockito.verify(moderationReportDao)
                .finalizeAppeal(
                        Mockito.eq(77L),
                        Mockito.eq(99L),
                        Mockito.eq(ReportResolution.WARNING),
                        Mockito.any());
    }

    @Test
    public void softDeleteReviewDelegatesToDao() {
        moderationService.softDeleteReview(1L, 2L, ReviewDeleteReason.SPAM, 3L);
        Mockito.verify(playerReviewDao).softDeleteReview(1L, 2L, ReviewDeleteReason.SPAM, 3L);
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

    private static ModerationReport sampleUserReport() {
        return new ModerationReport(
                77L,
                50L,
                ReportTargetType.USER,
                88L,
                "user:88",
                ReportReason.HARASSMENT,
                "details",
                ReportStatus.UNDER_REVIEW,
                null,
                null,
                99L,
                FIXED_NOW,
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
                "review:123",
                ReportReason.AGGRESSIVE_LANGUAGE,
                "details",
                ReportStatus.UNDER_REVIEW,
                null,
                null,
                99L,
                FIXED_NOW,
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
