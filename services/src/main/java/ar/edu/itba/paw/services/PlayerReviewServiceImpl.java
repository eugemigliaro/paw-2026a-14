package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.PlayerReviewFilter;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import ar.edu.itba.paw.persistence.PlayerReviewDao;
import ar.edu.itba.paw.services.exceptions.ModerationException;
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
            final User reviewer,
            final User reviewed,
            final PlayerReviewReaction reaction,
            final String comment) {
        validateReviewRequest(reviewer, reviewed, reaction);
        final String normalizedComment = normalizeComment(comment);
        return playerReviewDao.upsertReview(reviewer, reviewed, reaction, normalizedComment);
    }

    @Override
    @Transactional
    public void deleteReview(final User reviewer, final User reviewed) {
        nonNullUser(reviewer);
        nonNullUser(reviewed);
        if (!playerReviewDao.softDeleteReview(reviewer, reviewed)) {
            throw new PlayerReviewException(
                    PlayerReviewException.NOT_FOUND, "Player review not found.");
        }
    }

    @Override
    public Optional<PlayerReview> findReviewByPair(final User reviewer, final User reviewed) {
        nonNullUser(reviewer);
        nonNullUser(reviewed);
        return playerReviewDao.findByPair(reviewer, reviewed);
    }

    @Override
    public Optional<PlayerReview> findReviewByIdIncludingDeleted(final Long reviewId) {
        return playerReviewDao.findByIdIncludingDeleted(reviewId);
    }

    @Override
    public PlayerReviewSummary findSummaryForUser(final User reviewed) {
        nonNullUser(reviewed);
        return playerReviewDao.getSummaryForUser(reviewed);
    }

    @Override
    public PaginatedResult<PlayerReview> findReviewsForUser(
            final User reviewed,
            final PlayerReviewFilter filter,
            final int page,
            final int pageSize) {
        nonNullUser(reviewed);
        final int safePage = page > 0 ? page : 1;
        final int safePageSize = pageSize > 0 ? pageSize : DEFAULT_RECENT_LIMIT;
        final PlayerReviewFilter safeFilter = filter == null ? PlayerReviewFilter.BOTH : filter;
        final int totalCount = playerReviewDao.countReviewsForUser(reviewed, safeFilter);
        final int totalPages =
                totalCount == 0 ? 1 : (int) Math.ceil((double) totalCount / safePageSize);
        final int clampedPage = Math.min(safePage, totalPages);
        final int offset = (clampedPage - 1) * safePageSize;
        final List<PlayerReview> items =
                playerReviewDao.findReviewsForUser(reviewed, safeFilter, safePageSize, offset);
        return new PaginatedResult<>(items, totalCount, clampedPage, safePageSize);
    }

    @Override
    public boolean canReview(final User reviewer, final User reviewed) {
        return reviewer != null
                && reviewed != null
                && !reviewer.equals(reviewed)
                && playerReviewDao.canReview(reviewer, reviewed);
    }

    @Override
    public Set<Long> findReviewableUserIds(final User reviewer) {
        if (reviewer == null) {
            return Set.of();
        }
        return new HashSet<>(playerReviewDao.findReviewableUserIds(reviewer));
    }

    private void nonNullUser(final User user) {
        if (user == null) {
            throw new ModerationException("invalid_report", "Reporter user is required.");
        }
    }

    private void validateReviewRequest(
            final User reviewer, final User reviewed, final PlayerReviewReaction reaction) {
        nonNullUser(reviewer);
        nonNullUser(reviewed);
        if (reaction == null) {
            throw new PlayerReviewException(
                    PlayerReviewException.INVALID_REACTION, "Review reaction is required.");
        }
        if (reviewer != null && reviewer.getId().equals(reviewed.getId())) {
            throw new PlayerReviewException(
                    PlayerReviewException.SELF_REVIEW, "Users cannot review themselves.");
        }
        if (!canReview(reviewer, reviewed)) {
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
