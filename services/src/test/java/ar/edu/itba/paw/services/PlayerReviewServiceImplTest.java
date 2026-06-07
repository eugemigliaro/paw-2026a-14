package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.playerReview.*;
import ar.edu.itba.paw.models.query.PlayerReviewFilter;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import ar.edu.itba.paw.persistence.PlayerReviewDao;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PlayerReviewServiceImplTest {

    @Mock private PlayerReviewDao playerReviewDao;

    private PlayerReviewServiceImpl playerReviewService;

    @BeforeEach
    public void setUp() {
        playerReviewService = new PlayerReviewServiceImpl(playerReviewDao);
    }

    @Test
    public void testSubmitReviewCreatesValidReview() {
        final PlayerReview persisted =
                review(1L, 2L, 3L, PlayerReviewReaction.LIKE, "Great teammate", null);
        Mockito.when(playerReviewDao.canReview(UserUtils.getUser(2L), UserUtils.getUser(3L)))
                .thenReturn(true);
        Mockito.when(
                        playerReviewDao.upsertReview(
                                UserUtils.getUser(2L),
                                UserUtils.getUser(3L),
                                PlayerReviewReaction.LIKE,
                                "Great teammate"))
                .thenReturn(persisted);

        final PlayerReview review =
                playerReviewService.submitReview(
                        UserUtils.getUser(2L),
                        UserUtils.getUser(3L),
                        PlayerReviewReaction.LIKE,
                        "  Great teammate  ");

        Assertions.assertEquals(persisted, review);
        Assertions.assertEquals("Great teammate", review.getComment());
    }

    @Test
    public void testSubmitReviewAllowsOnceEverEditableBehavior() {
        final PlayerReview updated = review(1L, 2L, 3L, PlayerReviewReaction.DISLIKE, "Late", null);
        Mockito.when(playerReviewDao.canReview(UserUtils.getUser(2L), UserUtils.getUser(3L)))
                .thenReturn(true);
        Mockito.when(
                        playerReviewDao.upsertReview(
                                UserUtils.getUser(2L),
                                UserUtils.getUser(3L),
                                PlayerReviewReaction.DISLIKE,
                                "Late"))
                .thenReturn(updated);

        final PlayerReview review =
                playerReviewService.submitReview(
                        UserUtils.getUser(2L),
                        UserUtils.getUser(3L),
                        PlayerReviewReaction.DISLIKE,
                        "Late");

        Assertions.assertEquals(1L, review.getId());
        Assertions.assertEquals(PlayerReviewReaction.DISLIKE, review.getReaction());
    }

    @Test
    public void testSubmitReviewRejectsSelfReview() {
        Assertions.assertThrows(
                PlayerReviewSelfReviewException.class,
                () ->
                        playerReviewService.submitReview(
                                UserUtils.getUser(2L),
                                UserUtils.getUser(2L),
                                PlayerReviewReaction.LIKE,
                                "Great"));
    }

    @Test
    public void testSubmitReviewRejectsIneligibleUsers() {
        Mockito.when(playerReviewDao.canReview(UserUtils.getUser(2L), UserUtils.getUser(3L)))
                .thenReturn(false);

        Assertions.assertThrows(
                PlayerReviewNotEligibleException.class,
                () ->
                        playerReviewService.submitReview(
                                UserUtils.getUser(2L),
                                UserUtils.getUser(3L),
                                PlayerReviewReaction.LIKE,
                                "Great"));
    }

    @Test
    public void testSubmitReviewRejectsMissingReaction() {
        Assertions.assertThrows(
                PlayerReviewInvalidReactionException.class,
                () ->
                        playerReviewService.submitReview(
                                UserUtils.getUser(2L), UserUtils.getUser(3L), null, "Great"));
    }

    @Test
    public void testSubmitReviewStoresBlankCommentAsNull() {
        final PlayerReview persisted = review(1L, 2L, 3L, PlayerReviewReaction.LIKE, null, null);
        Mockito.when(playerReviewDao.canReview(UserUtils.getUser(2L), UserUtils.getUser(3L)))
                .thenReturn(true);
        Mockito.when(
                        playerReviewDao.upsertReview(
                                UserUtils.getUser(2L),
                                UserUtils.getUser(3L),
                                PlayerReviewReaction.LIKE,
                                null))
                .thenReturn(persisted);

        final PlayerReview review =
                playerReviewService.submitReview(
                        UserUtils.getUser(2L),
                        UserUtils.getUser(3L),
                        PlayerReviewReaction.LIKE,
                        "   ");

        Assertions.assertNull(review.getComment());
    }

    @Test
    public void testSubmitReviewRejectsTooLongComment() {
        Mockito.when(playerReviewDao.canReview(UserUtils.getUser(2L), UserUtils.getUser(3L)))
                .thenReturn(true);
        final String tooLongComment = "a".repeat(PlayerReviewService.MAX_COMMENT_LENGTH + 1);

        Assertions.assertThrows(
                PlayerReviewCommentTooLongException.class,
                () ->
                        playerReviewService.submitReview(
                                UserUtils.getUser(2L),
                                UserUtils.getUser(3L),
                                PlayerReviewReaction.LIKE,
                                tooLongComment));
    }

    @Test
    public void testDeleteReviewRemovesExistingReviewFromLookup() {
        final PlayerReview persisted =
                review(1L, 2L, 3L, PlayerReviewReaction.LIKE, "Great teammate", null);
        playerReviewService = new PlayerReviewServiceImpl(new StatefulPlayerReviewDao(persisted));

        playerReviewService.deleteReview(UserUtils.getUser(2L), UserUtils.getUser(3L));

        Assertions.assertTrue(
                playerReviewService
                        .findReviewByPair(UserUtils.getUser(2L), UserUtils.getUser(3L))
                        .isEmpty());
    }

    @Test
    public void testDeleteReviewRejectsMissingReview() {
        Mockito.when(playerReviewDao.softDeleteReview(UserUtils.getUser(2L), UserUtils.getUser(3L)))
                .thenReturn(false);

        Assertions.assertThrows(
                PlayerReviewNotFoundException.class,
                () ->
                        playerReviewService.deleteReview(
                                UserUtils.getUser(2L), UserUtils.getUser(3L)));
    }

    @Test
    public void testFindMethodsDelegateToDaoResults() {
        final PlayerReview review = review(1L, 2L, 3L, PlayerReviewReaction.LIKE, "Great", null);
        final PlayerReviewSummary summary = new PlayerReviewSummary(3L, 1, 0, 1);
        Mockito.when(playerReviewDao.findByPair(UserUtils.getUser(2L), UserUtils.getUser(3L)))
                .thenReturn(Optional.of(review));
        Mockito.when(playerReviewDao.getSummaryForUser(UserUtils.getUser(3L))).thenReturn(summary);

        Assertions.assertTrue(
                playerReviewService
                        .findReviewByPair(UserUtils.getUser(2L), UserUtils.getUser(3L))
                        .isPresent());
        Assertions.assertEquals(
                summary, playerReviewService.findSummaryForUser(UserUtils.getUser(3L)));
    }

    @Test
    public void testFindReviewsForUserReturnsRequestedPage() {
        final PlayerReview review = review(2L, 4L, 3L, PlayerReviewReaction.LIKE, "Second", null);
        Mockito.when(
                        playerReviewDao.countReviewsForUser(
                                UserUtils.getUser(3L), PlayerReviewFilter.BOTH))
                .thenReturn(21);
        Mockito.when(
                        playerReviewDao.findReviewsForUser(
                                UserUtils.getUser(3L), PlayerReviewFilter.BOTH, 10, 10))
                .thenReturn(List.of(review));

        final PaginatedResult<PlayerReview> result =
                playerReviewService.findReviewsForUser(
                        UserUtils.getUser(3L), PlayerReviewFilter.BOTH, 2, 10);

        Assertions.assertEquals(2, result.getPage());
        Assertions.assertEquals(3, result.getTotalPages());
        Assertions.assertEquals(21, result.getTotalCount());
        Assertions.assertEquals(List.of(review), result.getItems());
    }

    @Test
    public void testFindReviewsForUserNormalizesInvalidPageAndPageSize() {
        final PlayerReview review = review(1L, 2L, 3L, PlayerReviewReaction.LIKE, "Great", null);
        Mockito.when(
                        playerReviewDao.countReviewsForUser(
                                UserUtils.getUser(3L), PlayerReviewFilter.POSITIVE))
                .thenReturn(1);
        Mockito.when(
                        playerReviewDao.findReviewsForUser(
                                UserUtils.getUser(3L), PlayerReviewFilter.POSITIVE, 10, 0))
                .thenReturn(List.of(review));

        final PaginatedResult<PlayerReview> result =
                playerReviewService.findReviewsForUser(
                        UserUtils.getUser(3L), PlayerReviewFilter.POSITIVE, -2, 0);

        Assertions.assertEquals(1, result.getPage());
        Assertions.assertEquals(10, result.getPageSize());
        Assertions.assertEquals(List.of(review), result.getItems());
    }

    @Test
    public void testFindReviewsForUserClampsPagePastTotal() {
        final PlayerReview review = review(3L, 5L, 3L, PlayerReviewReaction.DISLIKE, "Third", null);
        Mockito.when(
                        playerReviewDao.countReviewsForUser(
                                UserUtils.getUser(3L), PlayerReviewFilter.BAD))
                .thenReturn(25);
        Mockito.when(
                        playerReviewDao.findReviewsForUser(
                                UserUtils.getUser(3L), PlayerReviewFilter.BAD, 10, 20))
                .thenReturn(List.of(review));

        final PaginatedResult<PlayerReview> result =
                playerReviewService.findReviewsForUser(
                        UserUtils.getUser(3L), PlayerReviewFilter.BAD, 9, 10);

        Assertions.assertEquals(3, result.getPage());
        Assertions.assertEquals(3, result.getTotalPages());
        Assertions.assertEquals(List.of(review), result.getItems());
    }

    @Test
    public void testFindReviewableUserIdsReturnsDaoIdsAsSet() {
        Mockito.when(playerReviewDao.findReviewableUserIds(UserUtils.getUser(2L)))
                .thenReturn(List.of(3L, 4L, 3L));

        final Set<Long> reviewableUserIds =
                playerReviewService.findReviewableUserIds(UserUtils.getUser(2L));

        Assertions.assertEquals(Set.of(3L, 4L), reviewableUserIds);
    }

    @Test
    public void testFindReviewableUserIdsRejectsMissingReviewer() {
        final Set<Long> reviewableUserIds = playerReviewService.findReviewableUserIds(null);

        Assertions.assertTrue(reviewableUserIds.isEmpty());
    }

    private static PlayerReview review(
            final Long id,
            final Long reviewerUserId,
            final Long reviewedUserId,
            final PlayerReviewReaction reaction,
            final String comment,
            final Instant deletedAt) {
        final User reviewer = Mockito.mock(User.class);
        final User reviewed = Mockito.mock(User.class);
        Mockito.lenient().when(reviewer.getId()).thenReturn(reviewerUserId);
        Mockito.lenient().when(reviewed.getId()).thenReturn(reviewedUserId);

        return new PlayerReview(
                id,
                reviewer,
                reviewed,
                reaction,
                comment,
                Instant.parse("2026-04-01T18:00:00Z"),
                Instant.parse("2026-04-01T18:00:00Z"),
                deletedAt != null,
                deletedAt,
                null,
                null);
    }

    private static class StatefulPlayerReviewDao implements PlayerReviewDao {

        private final PlayerReview review;

        private StatefulPlayerReviewDao(final PlayerReview review) {
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
            if (matches(reviewer, reviewed) && !review.isDeleted()) {
                review.setDeleted(true);
                return true;
            }
            return false;
        }

        @Override
        public boolean softDeleteReview(
                final User reviewer,
                final User reviewed,
                final User deletedBy,
                final String deleteReason) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean restoreReview(final User reviewer, final User reviewed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PlayerReview> findByPair(final User reviewer, final User reviewed) {
            return matches(reviewer, reviewed) && !review.isDeleted()
                    ? Optional.of(review)
                    : Optional.empty();
        }

        @Override
        public Optional<PlayerReview> findByIdIncludingDeleted(final Long reviewId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PlayerReview> findById(final Long reviewId) {
            return review.getId().equals(reviewId) && !review.isDeleted()
                    ? Optional.of(review)
                    : Optional.empty();
        }

        @Override
        public PlayerReviewSummary getSummaryForUser(final User reviewed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int countReviewsForUser(final User reviewed, final PlayerReviewFilter filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<PlayerReview> findReviewsForUser(
                final User reviewed,
                final PlayerReviewFilter filter,
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
            return review.getReviewer().getId().equals(reviewer.getId())
                    && review.getReviewed().getId().equals(reviewed.getId());
        }
    }
}
