package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewFilter;
import ar.edu.itba.paw.models.PlayerReviewReaction;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import java.util.List;
import java.util.Optional;

public interface PlayerReviewDao {

    PlayerReview upsertReview(
            Long reviewerUserId,
            Long reviewedUserId,
            PlayerReviewReaction reaction,
            String comment);

    boolean softDeleteReview(Long reviewerUserId, Long reviewedUserId);

    Optional<PlayerReview> findByPair(Long reviewerUserId, Long reviewedUserId);

    PlayerReviewSummary getSummaryForUser(Long reviewedUserId);

    int countReviewsForUser(Long reviewedUserId, PlayerReviewFilter filter);

    List<PlayerReview> findReviewsForUser(
            Long reviewedUserId, PlayerReviewFilter filter, int limit, int offset);

    boolean canReview(Long reviewerUserId, Long reviewedUserId);
}
