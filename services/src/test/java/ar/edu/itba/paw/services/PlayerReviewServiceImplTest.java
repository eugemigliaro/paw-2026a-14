package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PlayerReview;
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
                review(1L, 2L, 3L, 10L, PlayerReviewReaction.LIKE, "Great teammate", null);
        Mockito.when(playerReviewDao.canReview(2L, 3L, 10L)).thenReturn(true);
        Mockito.when(
                        playerReviewDao.upsertReview(
                                2L, 3L, 10L, PlayerReviewReaction.LIKE, "Great teammate"))
                .thenReturn(persisted);

        final PlayerReview review =
                playerReviewService.submitReview(
                        2L, 3L, 10L, PlayerReviewReaction.LIKE, "  Great teammate  ");

        Assertions.assertEquals(persisted, review);
        Assertions.assertEquals("Great teammate", review.getComment());
    }

    @Test
    public void testSubmitReviewAllowsOnceEverEditableBehavior() {
        final PlayerReview updated =
                review(1L, 2L, 3L, 11L, PlayerReviewReaction.DISLIKE, "Late", null);
        Mockito.when(playerReviewDao.canReview(2L, 3L, 11L)).thenReturn(true);
        Mockito.when(
                        playerReviewDao.upsertReview(
                                2L, 3L, 11L, PlayerReviewReaction.DISLIKE, "Late"))
                .thenReturn(updated);

        final PlayerReview review =
                playerReviewService.submitReview(2L, 3L, 11L, PlayerReviewReaction.DISLIKE, "Late");

        Assertions.assertEquals(1L, review.getId());
        Assertions.assertEquals(11L, review.getOriginMatchId());
        Assertions.assertEquals(PlayerReviewReaction.DISLIKE, review.getReaction());
    }

    @Test
    public void testSubmitReviewRejectsSelfReview() {
        final PlayerReviewException exception =
                Assertions.assertThrows(
                        PlayerReviewException.class,
                        () ->
                                playerReviewService.submitReview(
                                        2L, 2L, 10L, PlayerReviewReaction.LIKE, "Great"));

        Assertions.assertEquals(PlayerReviewException.SELF_REVIEW, exception.getCode());
    }

    @Test
    public void testSubmitReviewRejectsIneligibleUsers() {
        Mockito.when(playerReviewDao.canReview(2L, 3L, 10L)).thenReturn(false);

        final PlayerReviewException exception =
                Assertions.assertThrows(
                        PlayerReviewException.class,
                        () ->
                                playerReviewService.submitReview(
                                        2L, 3L, 10L, PlayerReviewReaction.LIKE, "Great"));

        Assertions.assertEquals(PlayerReviewException.NOT_ELIGIBLE, exception.getCode());
    }

    @Test
    public void testSubmitReviewRejectsMissingReaction() {
        final PlayerReviewException exception =
                Assertions.assertThrows(
                        PlayerReviewException.class,
                        () -> playerReviewService.submitReview(2L, 3L, 10L, null, "Great"));

        Assertions.assertEquals(PlayerReviewException.INVALID_REACTION, exception.getCode());
    }

    @Test
    public void testSubmitReviewStoresBlankCommentAsNull() {
        final PlayerReview persisted =
                review(1L, 2L, 3L, 10L, PlayerReviewReaction.LIKE, null, null);
        Mockito.when(playerReviewDao.canReview(2L, 3L, 10L)).thenReturn(true);
        Mockito.when(playerReviewDao.upsertReview(2L, 3L, 10L, PlayerReviewReaction.LIKE, null))
                .thenReturn(persisted);

        final PlayerReview review =
                playerReviewService.submitReview(2L, 3L, 10L, PlayerReviewReaction.LIKE, "   ");

        Assertions.assertNull(review.getComment());
    }

    @Test
    public void testSubmitReviewRejectsTooLongComment() {
        Mockito.when(playerReviewDao.canReview(2L, 3L, 10L)).thenReturn(true);
        final String tooLongComment = "a".repeat(PlayerReviewService.MAX_COMMENT_LENGTH + 1);

        final PlayerReviewException exception =
                Assertions.assertThrows(
                        PlayerReviewException.class,
                        () ->
                                playerReviewService.submitReview(
                                        2L, 3L, 10L, PlayerReviewReaction.LIKE, tooLongComment));

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
        final PlayerReview review =
                review(1L, 2L, 3L, 10L, PlayerReviewReaction.LIKE, "Great", null);
        final PlayerReviewSummary summary = new PlayerReviewSummary(3L, 1, 0, 1);
        Mockito.when(playerReviewDao.findByPair(2L, 3L)).thenReturn(Optional.of(review));
        Mockito.when(playerReviewDao.getSummaryForUser(3L)).thenReturn(summary);
        Mockito.when(playerReviewDao.findRecentReviewsForUser(3L, 10, 0))
                .thenReturn(List.of(review));

        Assertions.assertTrue(playerReviewService.findReviewByPair(2L, 3L).isPresent());
        Assertions.assertEquals(summary, playerReviewService.findSummaryForUser(3L));
        Assertions.assertEquals(1, playerReviewService.findRecentReviewsForUser(3L, 0, -10).size());
    }

    private static PlayerReview review(
            final Long id,
            final Long reviewerUserId,
            final Long reviewedUserId,
            final Long originMatchId,
            final PlayerReviewReaction reaction,
            final String comment,
            final Instant deletedAt) {
        return new PlayerReview(
                id,
                reviewerUserId,
                reviewedUserId,
                originMatchId,
                reaction,
                comment,
                Instant.parse("2026-04-01T18:00:00Z"),
                Instant.parse("2026-04-01T18:00:00Z"),
                deletedAt);
    }
}
