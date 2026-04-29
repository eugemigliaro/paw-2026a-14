package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewFilter;
import ar.edu.itba.paw.models.PlayerReviewReaction;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.persistence.PlayerReviewDao;
import ar.edu.itba.paw.services.exceptions.PlayerReviewException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlayerReviewServiceImpl implements PlayerReviewService {

    private static final int DEFAULT_RECENT_LIMIT = 10;
    private static final int MAX_RECENT_LIMIT = 50;

    private final PlayerReviewDao playerReviewDao;

    @Autowired
    public PlayerReviewServiceImpl(final PlayerReviewDao playerReviewDao) {
        this.playerReviewDao = playerReviewDao;
    }

    @Override
    public PlayerReview submitReview(
            final Long reviewerUserId,
            final Long reviewedUserId,
            final PlayerReviewReaction reaction,
            final String comment) {
        validateReviewRequest(reviewerUserId, reviewedUserId, reaction);
        final String normalizedComment = normalizeComment(comment);
        return playerReviewDao.upsertReview(
                reviewerUserId, reviewedUserId, reaction, normalizedComment);
    }

    @Override
    public void deleteReview(final Long reviewerUserId, final Long reviewedUserId) {
        if (!playerReviewDao.softDeleteReview(reviewerUserId, reviewedUserId)) {
            throw new PlayerReviewException(
                    PlayerReviewException.NOT_FOUND, "Player review not found.");
        }
    }

    @Override
    public Optional<PlayerReview> findReviewByPair(
            final Long reviewerUserId, final Long reviewedUserId) {
        return playerReviewDao.findByPair(reviewerUserId, reviewedUserId);
    }

    @Override
    public PlayerReviewSummary findSummaryForUser(final Long reviewedUserId) {
        return playerReviewDao.getSummaryForUser(reviewedUserId);
    }

    @Override
    public List<PlayerReview> findRecentReviewsForUser(
            final Long reviewedUserId,
            final PlayerReviewFilter filter,
            final int limit,
            final int offset) {
        final int safeLimit = limit <= 0 ? DEFAULT_RECENT_LIMIT : Math.min(limit, MAX_RECENT_LIMIT);
        final int safeOffset = Math.max(offset, 0);
        final PlayerReviewFilter safeFilter = filter == null ? PlayerReviewFilter.BOTH : filter;
        return playerReviewDao.findRecentReviewsForUser(
                reviewedUserId, safeFilter, safeLimit, safeOffset);
    }

    @Override
    public boolean canReview(final Long reviewerUserId, final Long reviewedUserId) {
        return reviewerUserId != null
                && reviewedUserId != null
                && !reviewerUserId.equals(reviewedUserId)
                && playerReviewDao.canReview(reviewerUserId, reviewedUserId);
    }

    private void validateReviewRequest(
            final Long reviewerUserId,
            final Long reviewedUserId,
            final PlayerReviewReaction reaction) {
        if (reaction == null) {
            throw new PlayerReviewException(
                    PlayerReviewException.INVALID_REACTION, "Review reaction is required.");
        }
        if (reviewerUserId != null && reviewerUserId.equals(reviewedUserId)) {
            throw new PlayerReviewException(
                    PlayerReviewException.SELF_REVIEW, "Users cannot review themselves.");
        }
        if (!canReview(reviewerUserId, reviewedUserId)) {
            throw new PlayerReviewException(
                    PlayerReviewException.NOT_ELIGIBLE,
                    "Users must share a completed match to review each other.");
        }
    }

    private static String normalizeComment(final String comment) {
        if (comment == null) {
            return null;
        }
        final String normalized = comment.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > MAX_COMMENT_LENGTH) {
            throw new PlayerReviewException(
                    PlayerReviewException.COMMENT_TOO_LONG, "Review comment is too long.");
        }
        return normalized;
    }
}
