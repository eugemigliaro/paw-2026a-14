package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewFilter;
import ar.edu.itba.paw.models.PlayerReviewReaction;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import java.util.List;
import java.util.Optional;

public interface PlayerReviewService {

    int MAX_COMMENT_LENGTH = 1000;

    PlayerReview submitReview(
            Long reviewerUserId,
            Long reviewedUserId,
            PlayerReviewReaction reaction,
            String comment);

    void deleteReview(Long reviewerUserId, Long reviewedUserId);

    Optional<PlayerReview> findReviewByPair(Long reviewerUserId, Long reviewedUserId);

    PlayerReviewSummary findSummaryForUser(Long reviewedUserId);

    default List<PlayerReview> findRecentReviewsForUser(
            final Long reviewedUserId, final int limit, final int offset) {
        return findRecentReviewsForUser(reviewedUserId, PlayerReviewFilter.BOTH, limit, offset);
    }

    List<PlayerReview> findRecentReviewsForUser(
            Long reviewedUserId, PlayerReviewFilter filter, int limit, int offset);

    PaginatedResult<PlayerReview> findReviewsForUser(
            Long reviewedUserId, PlayerReviewFilter filter, int page, int pageSize);

    boolean canReview(Long reviewerUserId, Long reviewedUserId);
}
