package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.AppealDecision;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import ar.edu.itba.paw.models.types.ReportReason;
import ar.edu.itba.paw.models.types.ReportResolution;
import ar.edu.itba.paw.models.types.ReportStatus;
import ar.edu.itba.paw.models.types.ReportTargetType;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.persistence.ModerationReportDao;
import ar.edu.itba.paw.persistence.PlayerReviewDao;
import ar.edu.itba.paw.persistence.UserBanDao;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.exceptions.moderation.ModerationException;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
    @Mock private MatchService matchService;

    private RecordingMailDispatchService mailDispatchService;
    private ModerationService moderationService;

    @BeforeEach
    public void setUp() {
        mailDispatchService = new RecordingMailDispatchService();
        moderationService =
                new ModerationServiceImpl(
                        userBanDao,
                        moderationReportDao,
                        userDao,
                        matchDao,
                        matchParticipantDao,
                        playerReviewDao,
                        mailDispatchService,
                        matchService,
                        messageSource(),
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    private void usePlayerReviewDao(final PlayerReviewDao playerReviewDao) {
        moderationService =
                new ModerationServiceImpl(
                        userBanDao,
                        moderationReportDao,
                        userDao,
                        matchDao,
                        matchParticipantDao,
                        playerReviewDao,
                        mailDispatchService,
                        matchService,
                        messageSource(),
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    private void useUserBanDao(final UserBanDao userBanDao) {
        moderationService =
                new ModerationServiceImpl(
                        userBanDao,
                        moderationReportDao,
                        userDao,
                        matchDao,
                        matchParticipantDao,
                        playerReviewDao,
                        mailDispatchService,
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
        final Instant expectedBannedUntil = FIXED_NOW.plusSeconds(7L * 24L * 3600L);

        Mockito.when(
                        matchService.findDashboardMatches(
                                Mockito.any(User.class),
                                Mockito.anyBoolean(),
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
                                Mockito.anyInt(),
                                Mockito.anyInt()))
                .thenReturn(new PaginatedResult<>(List.<Match>of(), 0, 1, 10));

        Mockito.when(moderationReportDao.findById(77L)).thenReturn(Optional.of(report));
        Mockito.when(
                        moderationReportDao.resolveReport(
                                Mockito.eq(77L),
                                Mockito.eq(UserUtils.getUser(99L)),
                                Mockito.eq(ReportResolution.USER_BANNED),
                                Mockito.anyString(),
                                Mockito.any(),
                                Mockito.eq(ReportStatus.RESOLVED)))
                .thenReturn(true);

        Mockito.when(userBanDao.createBan(report, expectedBannedUntil))
                .thenReturn(new UserBan(10L, report, expectedBannedUntil));

        Mockito.when(userDao.findById(88L)).thenReturn(Optional.of(UserUtils.getUser(88L)));

        final ModerationReport resolved =
                moderationService.resolveReport(
                        77L,
                        UserUtils.getUser(99L),
                        ReportResolution.USER_BANNED,
                        "Repeated abuse",
                        ReportStatus.RESOLVED,
                        7);

        Assertions.assertNotNull(resolved, "resolveReport must return the updated report");
        Assertions.assertNotNull(
                mailDispatchService.bannedUntil.get(0),
                "A ban expiry must have been supplied to the mail boundary");
        Assertions.assertTrue(
                mailDispatchService.bannedUntil.get(0).isAfter(FIXED_NOW),
                "Ban expiry must be strictly after the resolution instant");
    }

    @Test
    public void resolveReportWithUserBan_usesProvidedBanDuration() {
        final ModerationReport report = sampleUserReport();
        final Instant expectedBannedUntil = FIXED_NOW.plusSeconds(30L * 24L * 3600L);

        Mockito.when(
                        matchService.findDashboardMatches(
                                Mockito.any(User.class),
                                Mockito.anyBoolean(),
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
                                Mockito.anyInt(),
                                Mockito.anyInt()))
                .thenReturn(new PaginatedResult<>(List.<Match>of(), 0, 1, 10));

        Mockito.when(moderationReportDao.findById(77L)).thenReturn(Optional.of(report));
        Mockito.when(
                        moderationReportDao.resolveReport(
                                Mockito.eq(77L),
                                Mockito.eq(UserUtils.getUser(99L)),
                                Mockito.eq(ReportResolution.USER_BANNED),
                                Mockito.anyString(),
                                Mockito.any(),
                                Mockito.eq(ReportStatus.RESOLVED)))
                .thenReturn(true);
        Mockito.when(userBanDao.createBan(report, expectedBannedUntil))
                .thenReturn(new UserBan(10L, report, expectedBannedUntil));
        Mockito.when(userDao.findById(88L)).thenReturn(Optional.of(UserUtils.getUser(88L)));

        moderationService.resolveReport(
                77L,
                UserUtils.getUser(99L),
                ReportResolution.USER_BANNED,
                "Repeated abuse",
                ReportStatus.RESOLVED,
                30);

        Assertions.assertEquals(
                expectedBannedUntil,
                mailDispatchService.bannedUntil.get(0),
                "The submitted ban duration must determine the ban expiry");
    }

    @Test
    public void resolveReportWithUserBan_banIsLinkedToSourceReport() {
        final ModerationReport report = sampleUserReport();
        final RecordingUserBanDao recordingUserBanDao = new RecordingUserBanDao();
        useUserBanDao(recordingUserBanDao);

        Mockito.when(
                        matchService.findDashboardMatches(
                                Mockito.any(User.class),
                                Mockito.anyBoolean(),
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
                                Mockito.anyInt(),
                                Mockito.anyInt()))
                .thenReturn(new PaginatedResult<>(List.<Match>of(), 0, 1, 10));

        Mockito.when(moderationReportDao.findById(77L)).thenReturn(Optional.of(report));
        Mockito.when(
                        moderationReportDao.resolveReport(
                                Mockito.eq(77L),
                                Mockito.eq(UserUtils.getUser(99L)),
                                Mockito.eq(ReportResolution.USER_BANNED),
                                Mockito.anyString(),
                                Mockito.any(),
                                Mockito.eq(ReportStatus.RESOLVED)))
                .thenReturn(true);

        Mockito.when(userDao.findById(88L)).thenReturn(Optional.of(UserUtils.getUser(88L)));

        moderationService.resolveReport(
                77L,
                UserUtils.getUser(99L),
                ReportResolution.USER_BANNED,
                "Repeated abuse",
                ReportStatus.RESOLVED,
                7);

        Assertions.assertNotNull(
                recordingUserBanDao.createdBan, "A ban must be created for the resolved report");
        Assertions.assertSame(
                report,
                recordingUserBanDao.createdBan.getModerationReport(),
                "The ban must be linked to the originating report");
    }

    @Test
    public void resolveReviewReport_contentDeleted_softDeletesCorrectReview() {
        final ModerationReport report = sampleReviewReport();
        final String reason = "Inappropriate content";
        final PlayerReview review =
                new PlayerReview(
                        123L,
                        UserUtils.getUser(7L),
                        UserUtils.getUser(8L),
                        PlayerReviewReaction.DISLIKE,
                        "bad",
                        FIXED_NOW,
                        FIXED_NOW,
                        false,
                        null,
                        null,
                        null);
        usePlayerReviewDao(new ModeratedPlayerReviewDao(review));

        Mockito.when(moderationReportDao.findById(45L)).thenReturn(Optional.of(report));
        Mockito.when(
                        moderationReportDao.resolveReport(
                                Mockito.eq(45L),
                                Mockito.eq(UserUtils.getUser(99L)),
                                Mockito.eq(ReportResolution.CONTENT_DELETED),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.eq(ReportStatus.RESOLVED)))
                .thenReturn(true);

        final ModerationReport resolved =
                moderationService.resolveReport(
                        45L,
                        UserUtils.getUser(99L),
                        ReportResolution.CONTENT_DELETED,
                        reason,
                        ReportStatus.RESOLVED,
                        7);

        Assertions.assertNotNull(resolved);
        Assertions.assertEquals(
                ReportTargetType.REVIEW,
                resolved.getTargetType(),
                "The resolved report must remain tied to the reported review");
        Assertions.assertEquals(123L, resolved.getTargetId());
        Assertions.assertTrue(review.isDeleted());
        Assertions.assertEquals(UserUtils.getUser(99L), review.getDeletedBy());
        Assertions.assertEquals(reason, review.getDeleteReason());
    }

    @Test
    public void reportContent_throwsModerationException_whenReportAlreadyExistsForTarget() {
        Mockito.when(moderationReportDao.countActiveReportsByReporter(UserUtils.getUser(50L)))
                .thenReturn(0);
        Mockito.when(
                        moderationReportDao.existsReportForTarget(
                                UserUtils.getUser(50L), ReportTargetType.USER, 88L))
                .thenReturn(true);

        Assertions.assertThrows(
                ModerationException.class,
                () ->
                        moderationService.reportContent(
                                UserUtils.getUser(50L),
                                ReportTargetType.USER,
                                88L,
                                ReportReason.SPAM,
                                "Duplicate"));
    }

    @Test
    public void reportContent_throwsModerationException_whenActiveReportLimitReached() {
        Mockito.when(moderationReportDao.countActiveReportsByReporter(UserUtils.getUser(50L)))
                .thenReturn(3);

        Assertions.assertThrows(
                ModerationException.class,
                () ->
                        moderationService.reportContent(
                                UserUtils.getUser(50L),
                                ReportTargetType.USER,
                                88L,
                                ReportReason.SPAM,
                                "Too many"));
    }

    @Test
    public void markReportUnderReview_returnsUpdatedReport() {
        final ModerationReport report = sampleUserReport();
        Mockito.when(
                        moderationReportDao.markUnderReview(
                                Mockito.eq(77L), Mockito.eq(UserUtils.getUser(99L)), Mockito.any()))
                .thenReturn(true);
        Mockito.when(moderationReportDao.findById(77L)).thenReturn(Optional.of(report));

        final ModerationReport result =
                moderationService.markReportUnderReview(77L, UserUtils.getUser(99L));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(
                77L, result.getId(), "Returned report id must match the requested report id");
    }

    @Test
    public void markReportUnderReview_throwsModerationException_whenDaoReturnsFalse() {
        Mockito.when(
                        moderationReportDao.markUnderReview(
                                Mockito.eq(77L), Mockito.eq(UserUtils.getUser(99L)), Mockito.any()))
                .thenReturn(false);

        Assertions.assertThrows(
                ModerationException.class,
                () -> moderationService.markReportUnderReview(77L, UserUtils.getUser(99L)));
    }

    @Test
    public void appealReport_storesSuppliedAppealText() {
        final ModerationReport report = sampleUserReport(); // appealCount == 0

        Mockito.when(moderationReportDao.findById(77L)).thenReturn(Optional.of(report));
        Mockito.when(
                        moderationReportDao.appealReport(
                                Mockito.eq(77L), Mockito.eq("Please reconsider"), Mockito.any()))
                .thenReturn(true);

        final ModerationReport appealed = moderationService.appealReport(77L, "Please reconsider");

        Assertions.assertEquals(
                77L, appealed.getId(), "The appealed report must be returned after storage");
    }

    @Test
    public void appealReport_throwsModerationException_whenAppealLimitAlreadyReached() {
        final ModerationReport reportWithExistingAppeal =
                new ModerationReport(
                        77L,
                        UserUtils.getUser(50L),
                        ReportTargetType.USER,
                        88L,
                        ReportReason.HARASSMENT,
                        "details",
                        ReportStatus.RESOLVED,
                        ReportResolution.USER_BANNED,
                        "reason",
                        UserUtils.getUser(99L),
                        FIXED_NOW,
                        "previous appeal",
                        (short) 1 /* appealCount >= 1 */,
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
    public void softDeleteReview_returnsTrueForExistingReviewModerationAction() {
        final PlayerReview review =
                new PlayerReview(
                        1L,
                        UserUtils.getUser(1L),
                        UserUtils.getUser(2L),
                        PlayerReviewReaction.LIKE,
                        "Good",
                        FIXED_NOW,
                        FIXED_NOW,
                        false,
                        null,
                        null,
                        null);
        usePlayerReviewDao(new ModeratedPlayerReviewDao(review));

        final boolean result =
                moderationService.softDeleteReview(
                        UserUtils.getUser(1L),
                        UserUtils.getUser(2L),
                        "SPAM",
                        UserUtils.getUser(3L));

        Assertions.assertTrue(result, "An existing review moderation action should succeed");
        Assertions.assertTrue(review.isDeleted());
        Assertions.assertEquals(UserUtils.getUser(3L), review.getDeletedBy());
        Assertions.assertEquals("SPAM", review.getDeleteReason());
    }

    @Test
    public void restoreReview_returnsTrueForRestorableReview() {
        final PlayerReview review =
                new PlayerReview(
                        1L,
                        UserUtils.getUser(1L),
                        UserUtils.getUser(2L),
                        PlayerReviewReaction.LIKE,
                        "Good",
                        FIXED_NOW,
                        FIXED_NOW,
                        true,
                        FIXED_NOW,
                        UserUtils.getUser(3L),
                        "SPAM");
        usePlayerReviewDao(new ModeratedPlayerReviewDao(review));

        final boolean result =
                moderationService.restoreReview(UserUtils.getUser(1L), UserUtils.getUser(2L));

        Assertions.assertTrue(result, "A restorable review should be restored");
        Assertions.assertFalse(review.isDeleted());
        Assertions.assertNull(review.getDeletedBy());
        Assertions.assertNull(review.getDeleteReason());
    }

    @Test
    public void softDeleteMatch_returnsTrueForExistingMatchModerationAction() {
        Mockito.when(matchDao.softDeleteMatch(10L, UserUtils.getUser(99L), "Reason"))
                .thenReturn(true);

        final boolean result =
                moderationService.softDeleteMatch(10L, UserUtils.getUser(99L), "Reason");

        Assertions.assertTrue(result, "An existing match moderation action should succeed");
    }

    @Test
    public void finalizeReportAppeal_sendsUnbanEmailToStoredUser() {
        final Long userId = 88L;
        User user = new User(userId, null, null, null, null, null, null, UserLanguages.SPANISH);

        LocaleContextHolder.setLocale(Locale.ENGLISH);
        try {
            Mockito.when(userDao.findById(userId)).thenReturn(Optional.of(user));

            final ModerationReport report =
                    new ModerationReport(
                            77L,
                            UserUtils.getUser(50L),
                            ReportTargetType.USER,
                            userId,
                            ReportReason.HARASSMENT,
                            "details",
                            ReportStatus.RESOLVED,
                            ReportResolution.USER_BANNED,
                            "reason",
                            UserUtils.getUser(99L),
                            FIXED_NOW,
                            "appeal",
                            (short) 1,
                            FIXED_NOW,
                            null,
                            null,
                            null,
                            FIXED_NOW,
                            FIXED_NOW);

            Mockito.when(moderationReportDao.findById(77L)).thenReturn(Optional.of(report));
            Mockito.when(userBanDao.findActiveBanForUser(Mockito.eq(user), Mockito.any()))
                    .thenReturn(Optional.empty());
            Mockito.when(
                            moderationReportDao.finalizeAppeal(
                                    Mockito.eq(77L),
                                    Mockito.eq(UserUtils.getUser(99L)),
                                    Mockito.eq(AppealDecision.LIFTED),
                                    Mockito.any()))
                    .thenReturn(true);

            moderationService.finalizeReportAppeal(
                    77L, UserUtils.getUser(99L), AppealDecision.LIFTED);

            Assertions.assertEquals(List.of("unban"), mailDispatchService.actions);
            Assertions.assertEquals(List.of(user), mailDispatchService.users);
        } finally {
            org.springframework.context.i18n.LocaleContextHolder.resetLocaleContext();
        }
    }

    private static ModerationReport sampleUserReport() {
        return new ModerationReport(
                77L,
                UserUtils.getUser(50L),
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
                (short) 0,
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
                UserUtils.getUser(50L),
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
                (short) 0,
                null,
                null,
                null,
                null,
                FIXED_NOW,
                FIXED_NOW);
    }

    private static class ModeratedPlayerReviewDao implements PlayerReviewDao {

        private final PlayerReview review;

        private ModeratedPlayerReviewDao(final PlayerReview review) {
            this.review = review;
        }

        @Override
        public PlayerReview upsertReview(
                final User reviewer,
                final User reviewed,
                final PlayerReviewReaction reaction,
                final String comment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean softDeleteReview(final User reviewer, final User reviewed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean softDeleteReview(
                final User reviewer,
                final User reviewed,
                final User deletedBy,
                final String deleteReason) {
            if (!matches(reviewer, reviewed) || review.isDeleted()) {
                return false;
            }
            review.setDeleted(true);
            review.setDeletedAt(FIXED_NOW);
            review.setDeletedBy(deletedBy);
            review.setDeleteReason(deleteReason);
            return true;
        }

        @Override
        public boolean restoreReview(final User reviewer, final User reviewed) {
            if (!matches(reviewer, reviewed) || !review.isDeleted()) {
                return false;
            }
            review.setDeleted(false);
            review.setDeletedAt(null);
            review.setDeletedBy(null);
            review.setDeleteReason(null);
            return true;
        }

        @Override
        public Optional<PlayerReview> findByPair(final User reviewer, final User reviewed) {
            return matches(reviewer, reviewed) && !review.isDeleted()
                    ? Optional.of(review)
                    : Optional.empty();
        }

        @Override
        public Optional<PlayerReview> findByIdIncludingDeleted(final Long reviewId) {
            return review.getId().equals(reviewId) ? Optional.of(review) : Optional.empty();
        }

        @Override
        public Optional<PlayerReview> findById(final Long reviewId) {
            return review.getId().equals(reviewId) && !review.isDeleted()
                    ? Optional.of(review)
                    : Optional.empty();
        }

        @Override
        public ar.edu.itba.paw.models.PlayerReviewSummary getSummaryForUser(final User reviewed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int countReviewsForUser(
                final User reviewed, final ar.edu.itba.paw.models.query.PlayerReviewFilter filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<PlayerReview> findReviewsForUser(
                final User reviewed,
                final ar.edu.itba.paw.models.query.PlayerReviewFilter filter,
                final int limit,
                final int offset) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean canReview(final User reviewer, final User reviewed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Long> findReviewableUserIds(final User reviewer) {
            throw new UnsupportedOperationException();
        }

        private boolean matches(final User reviewer, final User reviewed) {
            return review.getReviewer().equals(reviewer) && review.getReviewed().equals(reviewed);
        }
    }

    private static class RecordingUserBanDao implements UserBanDao {

        private UserBan createdBan;

        @Override
        public UserBan createBan(
                final ModerationReport moderationReport, final Instant bannedUntil) {
            createdBan = new UserBan(10L, moderationReport, bannedUntil);
            return createdBan;
        }

        @Override
        public Optional<UserBan> findById(final Long banId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<UserBan> findLatestBanForUser(final User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<UserBan> findActiveBanForUser(final User user, final Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void upliftBan(final Long banId) {
            throw new UnsupportedOperationException();
        }
    }

    private static class RecordingMailDispatchService implements MailDispatchService {

        private final List<String> actions = new ArrayList<>();
        private final List<User> users = new ArrayList<>();
        private final List<Instant> bannedUntil = new ArrayList<>();

        @Override
        public void sendBanNotice(final User user, final Instant bannedUntil, final String reason) {
            actions.add("ban");
            users.add(user);
            this.bannedUntil.add(bannedUntil);
        }

        @Override
        public void sendUnbanNotice(final User user) {
            actions.add("unban");
            users.add(user);
        }
    }
}
