package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewFilter;
import ar.edu.itba.paw.models.PlayerReviewReaction;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.persistence.PlayerReviewDao;
import ar.edu.itba.paw.services.exceptions.PlayerReviewException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
        Mockito.when(playerReviewDao.canReview(2L, 3L)).thenReturn(true);
        Mockito.when(
                        playerReviewDao.upsertReview(
                                2L, 3L, PlayerReviewReaction.LIKE, "Great teammate"))
                .thenReturn(persisted);

        final PlayerReview review =
                playerReviewService.submitReview(
                        2L, 3L, PlayerReviewReaction.LIKE, "  Great teammate  ");

        Assertions.assertEquals(persisted, review);
        Assertions.assertEquals("Great teammate", review.getComment());
    }

    @Test
    public void testSubmitReviewAllowsOnceEverEditableBehavior() {
        final PlayerReview updated = review(1L, 2L, 3L, PlayerReviewReaction.DISLIKE, "Late", null);
        Mockito.when(playerReviewDao.canReview(2L, 3L)).thenReturn(true);
        Mockito.when(playerReviewDao.upsertReview(2L, 3L, PlayerReviewReaction.DISLIKE, "Late"))
                .thenReturn(updated);

        final PlayerReview review =
                playerReviewService.submitReview(2L, 3L, PlayerReviewReaction.DISLIKE, "Late");

        Assertions.assertEquals(1L, review.getId());
        Assertions.assertEquals(PlayerReviewReaction.DISLIKE, review.getReaction());
    }

    @Test
    public void testSubmitReviewRejectsSelfReview() {
        final PlayerReviewException exception =
                Assertions.assertThrows(
                        PlayerReviewException.class,
                        () ->
                                playerReviewService.submitReview(
                                        2L, 2L, PlayerReviewReaction.LIKE, "Great"));

        Assertions.assertEquals(PlayerReviewException.SELF_REVIEW, exception.getCode());
    }

    @Test
    public void testSubmitReviewRejectsIneligibleUsers() {
        Mockito.when(playerReviewDao.canReview(2L, 3L)).thenReturn(false);

        final PlayerReviewException exception =
                Assertions.assertThrows(
                        PlayerReviewException.class,
                        () ->
                                playerReviewService.submitReview(
                                        2L, 3L, PlayerReviewReaction.LIKE, "Great"));

        Assertions.assertEquals(PlayerReviewException.NOT_ELIGIBLE, exception.getCode());
    }

    @Test
    public void testSubmitReviewRejectsMissingReaction() {
        final PlayerReviewException exception =
                Assertions.assertThrows(
                        PlayerReviewException.class,
                        () -> playerReviewService.submitReview(2L, 3L, null, "Great"));

        Assertions.assertEquals(PlayerReviewException.INVALID_REACTION, exception.getCode());
    }

    @Test
    public void testSubmitReviewStoresBlankCommentAsNull() {
        final PlayerReview persisted = review(1L, 2L, 3L, PlayerReviewReaction.LIKE, null, null);
        Mockito.when(playerReviewDao.canReview(2L, 3L)).thenReturn(true);
        Mockito.when(playerReviewDao.upsertReview(2L, 3L, PlayerReviewReaction.LIKE, null))
                .thenReturn(persisted);

        final PlayerReview review =
                playerReviewService.submitReview(2L, 3L, PlayerReviewReaction.LIKE, "   ");

        Assertions.assertNull(review.getComment());
    }

    @Test
    public void testSubmitReviewRejectsTooLongComment() {
        Mockito.when(playerReviewDao.canReview(2L, 3L)).thenReturn(true);
        final String tooLongComment = "a".repeat(PlayerReviewService.MAX_COMMENT_LENGTH + 1);

        final PlayerReviewException exception =
                Assertions.assertThrows(
                        PlayerReviewException.class,
                        () ->
                                playerReviewService.submitReview(
                                        2L, 3L, PlayerReviewReaction.LIKE, tooLongComment));

        Assertions.assertEquals(PlayerReviewException.COMMENT_TOO_LONG, exception.getCode());
    }

    @Test
    public void testDeleteReviewRejectsMissingReview() {
        Mockito.when(playerReviewDao.softDeleteReview(2L, 3L)).thenReturn(false);

        final PlayerReviewException exception =
                Assertions.assertThrows(
                        PlayerReviewException.class,
                        () -> playerReviewService.deleteReview(2L, 3L));

        Assertions.assertEquals(PlayerReviewException.NOT_FOUND, exception.getCode());
    }

    @Test
    public void testFindMethodsDelegateToDaoResults() {
        final PlayerReview review = review(1L, 2L, 3L, PlayerReviewReaction.LIKE, "Great", null);
        final PlayerReviewSummary summary = new PlayerReviewSummary(3L, 1, 0, 1);
        Mockito.when(playerReviewDao.findByPair(2L, 3L)).thenReturn(Optional.of(review));
        Mockito.when(playerReviewDao.getSummaryForUser(3L)).thenReturn(summary);
        Mockito.when(playerReviewDao.findRecentReviewsForUser(3L, PlayerReviewFilter.BOTH, 10, 0))
                .thenReturn(List.of(review));

        Assertions.assertTrue(playerReviewService.findReviewByPair(2L, 3L).isPresent());
        Assertions.assertEquals(summary, playerReviewService.findSummaryForUser(3L));
        Assertions.assertEquals(
                1,
                playerReviewService
                        .findRecentReviewsForUser(3L, PlayerReviewFilter.BOTH, 0, -10)
                        .size());
    }

    @Test
    public void testFindRecentReviewsNormalizesFilterLimitAndOffset() {
        final PlayerReview review = review(1L, 2L, 3L, PlayerReviewReaction.LIKE, "Great", null);
        Mockito.when(
                        playerReviewDao.findRecentReviewsForUser(
                                3L, PlayerReviewFilter.POSITIVE, 10, 0))
                .thenReturn(List.of(review));

        final List<PlayerReview> reviews =
                playerReviewService.findRecentReviewsForUser(
                        3L, PlayerReviewFilter.POSITIVE, 0, -10);

        Assertions.assertEquals(1, reviews.size());
        Assertions.assertEquals(PlayerReviewReaction.LIKE, reviews.get(0).getReaction());
    }

    @Test
    public void testFindReviewsForUserReturnsRequestedPage() {
        final PlayerReview review = review(2L, 4L, 3L, PlayerReviewReaction.LIKE, "Second", null);
        Mockito.when(playerReviewDao.countReviewsForUser(3L, PlayerReviewFilter.BOTH))
                .thenReturn(21);
        Mockito.when(playerReviewDao.findRecentReviewsForUser(3L, PlayerReviewFilter.BOTH, 10, 10))
                .thenReturn(List.of(review));

        final PaginatedResult<PlayerReview> result =
                playerReviewService.findReviewsForUser(3L, PlayerReviewFilter.BOTH, 2, 10);

        Assertions.assertEquals(2, result.getPage());
        Assertions.assertEquals(3, result.getTotalPages());
        Assertions.assertEquals(21, result.getTotalCount());
        Assertions.assertEquals(List.of(review), result.getItems());
    }

    @Test
    public void testFindReviewsForUserNormalizesInvalidPageAndPageSize() {
        final PlayerReview review = review(1L, 2L, 3L, PlayerReviewReaction.LIKE, "Great", null);
        Mockito.when(playerReviewDao.countReviewsForUser(3L, PlayerReviewFilter.POSITIVE))
                .thenReturn(1);
        Mockito.when(
                        playerReviewDao.findRecentReviewsForUser(
                                3L, PlayerReviewFilter.POSITIVE, 10, 0))
                .thenReturn(List.of(review));

        final PaginatedResult<PlayerReview> result =
                playerReviewService.findReviewsForUser(3L, PlayerReviewFilter.POSITIVE, -2, 0);

        Assertions.assertEquals(1, result.getPage());
        Assertions.assertEquals(10, result.getPageSize());
        Assertions.assertEquals(List.of(review), result.getItems());
    }

    @Test
    public void testFindReviewsForUserClampsPagePastTotal() {
        final PlayerReview review = review(3L, 5L, 3L, PlayerReviewReaction.DISLIKE, "Third", null);
        Mockito.when(playerReviewDao.countReviewsForUser(3L, PlayerReviewFilter.BAD))
                .thenReturn(25);
        Mockito.when(playerReviewDao.findRecentReviewsForUser(3L, PlayerReviewFilter.BAD, 10, 20))
                .thenReturn(List.of(review));

        final PaginatedResult<PlayerReview> result =
                playerReviewService.findReviewsForUser(3L, PlayerReviewFilter.BAD, 9, 10);

        Assertions.assertEquals(3, result.getPage());
        Assertions.assertEquals(3, result.getTotalPages());
        Assertions.assertEquals(List.of(review), result.getItems());
    }

    private static PlayerReview review(
            final Long id,
            final Long reviewerUserId,
            final Long reviewedUserId,
            final PlayerReviewReaction reaction,
            final String comment,
            final Instant deletedAt) {
        return new PlayerReview(
                id,
                reviewerUserId,
                reviewedUserId,
                reaction,
                comment,
                Instant.parse("2026-04-01T18:00:00Z"),
                Instant.parse("2026-04-01T18:00:00Z"),
                deletedAt);
    }
}
