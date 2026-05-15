package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.PlayerReviewFilter;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import java.util.List;
import java.util.Optional;

public interface PlayerReviewDao {

    PlayerReview upsertReview(
            User reviewer, User reviewed, PlayerReviewReaction reaction, String comment);

    boolean softDeleteReview(User reviewer, User reviewed);

    boolean softDeleteReview(User reviewer, User reviewed, User deletedBy, String deleteReason);

    boolean restoreReview(User reviewer, User reviewed);

    Optional<PlayerReview> findByPair(User reviewer, User reviewed);

    Optional<PlayerReview> findByIdIncludingDeleted(Long reviewId);

    PlayerReviewSummary getSummaryForUser(User reviewed);

    int countReviewsForUser(User reviewed, PlayerReviewFilter filter);

    List<PlayerReview> findReviewsForUser(
            User reviewed, PlayerReviewFilter filter, int limit, int offset);

    boolean canReview(User reviewer, User reviewed);

    List<Long> findReviewableUserIds(User reviewer);
}
