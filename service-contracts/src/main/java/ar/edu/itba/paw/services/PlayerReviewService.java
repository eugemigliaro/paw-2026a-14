package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.PlayerReviewFilter;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import java.util.Optional;
import java.util.Set;

public interface PlayerReviewService {

    int MAX_COMMENT_LENGTH = 1000;

    PlayerReview submitReview(
            User reviewer, User reviewed, PlayerReviewReaction reaction, String comment);

    void deleteReview(User reviewer, User reviewed);

    Optional<PlayerReview> findReviewByPair(User reviewer, User reviewed);

    Optional<PlayerReview> findReviewByIdIncludingDeleted(Long reviewId);

    PlayerReviewSummary findSummaryForUser(User reviewed);

    PaginatedResult<PlayerReview> findReviewsForUser(
            User reviewed, PlayerReviewFilter filter, int page, int pageSize);

    boolean canReview(User reviewer, User reviewed);

    Set<Long> findReviewableUserIds(User reviewer);
}
