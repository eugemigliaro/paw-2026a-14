package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.query.PlayerReviewFilter;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import java.util.List;
import java.util.Optional;

public interface PlayerReviewDao {

    PlayerReview upsertReview(
            Long reviewerUserId,
            Long reviewedUserId,
            PlayerReviewReaction reaction,
            String comment);

    boolean softDeleteReview(Long reviewerUserId, Long reviewedUserId);

    boolean softDeleteReview(
            Long reviewerUserId, Long reviewedUserId, Long deletedByUserId, String deleteReason);

    boolean restoreReview(Long reviewerUserId, Long reviewedUserId);

    Optional<PlayerReview> findByPair(Long reviewerUserId, Long reviewedUserId);

    Optional<PlayerReview> findByIdIncludingDeleted(Long reviewId);

    PlayerReviewSummary getSummaryForUser(Long reviewedUserId);

    int countReviewsForUser(Long reviewedUserId, PlayerReviewFilter filter);

    List<PlayerReview> findReviewsForUser(
            Long reviewedUserId, PlayerReviewFilter filter, int limit, int offset);

    boolean canReview(Long reviewerUserId, Long reviewedUserId);

    List<Long> findReviewableUserIds(Long reviewerUserId);
}
