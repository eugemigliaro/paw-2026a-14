package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewFilter;
import ar.edu.itba.paw.models.PlayerReviewReaction;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.persistence.PlayerReviewDao;
import ar.edu.itba.paw.services.exceptions.PlayerReviewException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlayerReviewServiceImpl implements PlayerReviewService {

    private static final int DEFAULT_RECENT_LIMIT = 10;
    private final PlayerReviewDao playerReviewDao;

    @Autowired
    public PlayerReviewServiceImpl(final PlayerReviewDao playerReviewDao) {
        this.playerReviewDao = playerReviewDao;
    }

    @Override
    @Transactional
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
    @Transactional
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
    public Optional<PlayerReview> findReviewByIdIncludingDeleted(final Long reviewId) {
        return playerReviewDao.findByIdIncludingDeleted(reviewId);
    }

    @Override
    public PlayerReviewSummary findSummaryForUser(final Long reviewedUserId) {
        return playerReviewDao.getSummaryForUser(reviewedUserId);
    }

    @Override
    public PaginatedResult<PlayerReview> findReviewsForUser(
            final Long reviewedUserId,
            final PlayerReviewFilter filter,
            final int page,
            final int pageSize) {
        final int safePage = page > 0 ? page : 1;
        final int safePageSize = pageSize > 0 ? pageSize : DEFAULT_RECENT_LIMIT;
        final PlayerReviewFilter safeFilter = filter == null ? PlayerReviewFilter.BOTH : filter;
        final int totalCount = playerReviewDao.countReviewsForUser(reviewedUserId, safeFilter);
        final int totalPages =
                totalCount == 0 ? 1 : (int) Math.ceil((double) totalCount / safePageSize);
        final int clampedPage = Math.min(safePage, totalPages);
        final int offset = (clampedPage - 1) * safePageSize;
        final List<PlayerReview> items =
                playerReviewDao.findReviewsForUser(
                        reviewedUserId, safeFilter, safePageSize, offset);
        return new PaginatedResult<>(items, totalCount, clampedPage, safePageSize);
    }

    @Override
    public boolean canReview(final Long reviewerUserId, final Long reviewedUserId) {
        return reviewerUserId != null
                && reviewedUserId != null
                && !reviewerUserId.equals(reviewedUserId)
                && playerReviewDao.canReview(reviewerUserId, reviewedUserId);
    }

    @Override
    public Set<Long> findReviewableUserIds(final Long reviewerUserId) {
        if (reviewerUserId == null) {
            return Set.of();
        }
        return new HashSet<>(playerReviewDao.findReviewableUserIds(reviewerUserId));
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
