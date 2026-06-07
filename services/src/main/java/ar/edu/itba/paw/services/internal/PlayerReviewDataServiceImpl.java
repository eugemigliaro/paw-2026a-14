package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.PlayerReviewFilter;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import ar.edu.itba.paw.persistence.PlayerReviewDao;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PlayerReviewDataServiceImpl implements PlayerReviewDataService {

    private final PlayerReviewDao playerReviewDao;

    public PlayerReviewDataServiceImpl(final PlayerReviewDao playerReviewDao) {
        this.playerReviewDao = Objects.requireNonNull(playerReviewDao);
    }

    @Override
    public PlayerReview upsertReview(
            final User reviewer,
            final User reviewed,
            final PlayerReviewReaction reaction,
            final String comment) {
        return playerReviewDao.upsertReview(reviewer, reviewed, reaction, comment);
    }

    @Override
    public boolean softDeleteReview(final User reviewer, final User reviewed) {
        return playerReviewDao.softDeleteReview(reviewer, reviewed);
    }

    @Override
    public boolean softDeleteReview(
            final User reviewer,
            final User reviewed,
            final User deletedBy,
            final String deleteReason) {
        return playerReviewDao.softDeleteReview(reviewer, reviewed, deletedBy, deleteReason);
    }

    @Override
    public boolean restoreReview(final User reviewer, final User reviewed) {
        return playerReviewDao.restoreReview(reviewer, reviewed);
    }

    @Override
    public Optional<PlayerReview> findByPair(final User reviewer, final User reviewed) {
        return playerReviewDao.findByPair(reviewer, reviewed);
    }

    @Override
    public Optional<PlayerReview> findByIdIncludingDeleted(final Long reviewId) {
        return playerReviewDao.findByIdIncludingDeleted(reviewId);
    }

    @Override
    public Optional<PlayerReview> findById(final Long reviewId) {
        return playerReviewDao.findById(reviewId);
    }

    @Override
    public PlayerReviewSummary getSummaryForUser(final User reviewed) {
        return playerReviewDao.getSummaryForUser(reviewed);
    }

    @Override
    public int countReviewsForUser(final User reviewed, final PlayerReviewFilter filter) {
        return playerReviewDao.countReviewsForUser(reviewed, filter);
    }

    @Override
    public List<PlayerReview> findReviewsForUser(
            final User reviewed,
            final PlayerReviewFilter filter,
            final int limit,
            final int offset) {
        return playerReviewDao.findReviewsForUser(reviewed, filter, limit, offset);
    }

    @Override
    public boolean canReview(final User reviewer, final User reviewed) {
        return playerReviewDao.canReview(reviewer, reviewed);
    }

    @Override
    public List<Long> findReviewableUserIds(final User reviewer) {
        return playerReviewDao.findReviewableUserIds(reviewer);
    }
}
