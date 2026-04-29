package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewFilter;
import ar.edu.itba.paw.models.PlayerReviewReaction;
import ar.edu.itba.paw.models.PlayerReviewSummary;
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

    PaginatedResult<PlayerReview> findReviewsForUser(
            Long reviewedUserId, PlayerReviewFilter filter, int page, int pageSize);

    boolean canReview(Long reviewerUserId, Long reviewedUserId);
}
