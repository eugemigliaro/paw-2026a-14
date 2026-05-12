package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.query.PlayerReviewFilter;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.ParticipantStatus;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PlayerReviewJpaDao implements PlayerReviewDao {

    private static final String REVIEW_PROJECTION_JPQL =
            "SELECT new ar.edu.itba.paw.persistence.PlayerReviewProjection("
                    + "pr.id,"
                    + " pr.reviewer.id, pr.reviewer.username,"
                    + " pr.reviewed.id, pr.reviewed.username,"
                    + " pr.reaction, pr.comment, pr.createdAt, pr.updatedAt,"
                    + " pr.deleted, pr.deletedAt, pr.deletedBy.id, pr.deleteReason)"
                    + " FROM PlayerReview pr";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public PlayerReview upsertReview(
            final Long reviewerUserId,
            final Long reviewedUserId,
            final PlayerReviewReaction reaction,
            final String comment) {
        lockUsersForPair(reviewerUserId, reviewedUserId);

        final Instant now = Instant.now();
        final Optional<PlayerReview> existing =
                findEntityByPairIncludingDeleted(
                        reviewerUserId, reviewedUserId, LockModeType.PESSIMISTIC_WRITE);

        if (existing.isPresent()) {
            applyUpsertValues(existing.get(), reaction, comment, now);
        } else {
            final PlayerReview review =
                    new PlayerReview(
                            null,
                            em.getReference(UserAccount.class, reviewerUserId),
                            em.getReference(UserAccount.class, reviewedUserId),
                            reaction,
                            comment,
                            now,
                            now,
                            false,
                            null,
                            null,
                            null);
            em.persist(review);
        }

        return findByPair(reviewerUserId, reviewedUserId)
                .orElseThrow(() -> new IllegalStateException("Player review was not persisted"));
    }

    @Override
    @Transactional
    public boolean softDeleteReview(final Long reviewerUserId, final Long reviewedUserId) {
        return softDeleteReview(reviewerUserId, reviewedUserId, null, null);
    }

    @Override
    @Transactional
    public boolean softDeleteReview(
            final Long reviewerUserId,
            final Long reviewedUserId,
            final Long deletedByUserId,
            final String reason) {
        final Instant now = Instant.now();
        final int rows =
                em.createQuery(
                                "UPDATE PlayerReview pr"
                                        + " SET pr.deleted = TRUE, pr.deletedAt = :now,"
                                        + " pr.deletedBy = :deletedBy, pr.deleteReason = :reason,"
                                        + " pr.updatedAt = :now"
                                        + " WHERE pr.reviewer.id = :reviewerUserId"
                                        + " AND pr.reviewed.id = :reviewedUserId"
                                        + " AND pr.deletedAt IS NULL")
                        .setParameter("now", now)
                        .setParameter("deletedBy", userReferenceOrNull(deletedByUserId))
                        .setParameter("reason", reason)
                        .setParameter("reviewerUserId", reviewerUserId)
                        .setParameter("reviewedUserId", reviewedUserId)
                        .executeUpdate();
        clearPersistenceContextAfterBulkUpdate(rows);
        return rows == 1;
    }

    @Override
    @Transactional
    public boolean restoreReview(final Long reviewerUserId, final Long reviewedUserId) {
        final Instant now = Instant.now();
        final int rows =
                em.createQuery(
                                "UPDATE PlayerReview pr"
                                        + " SET pr.deleted = FALSE, pr.deletedAt = NULL,"
                                        + " pr.deletedBy = NULL, pr.deleteReason = NULL,"
                                        + " pr.updatedAt = :now"
                                        + " WHERE pr.reviewer.id = :reviewerUserId"
                                        + " AND pr.reviewed.id = :reviewedUserId"
                                        + " AND pr.deleted = TRUE")
                        .setParameter("now", now)
                        .setParameter("reviewerUserId", reviewerUserId)
                        .setParameter("reviewedUserId", reviewedUserId)
                        .executeUpdate();
        clearPersistenceContextAfterBulkUpdate(rows);
        return rows == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlayerReview> findByPair(final Long reviewerUserId, final Long reviewedUserId) {
        final List<PlayerReviewProjection> projections =
                em.createQuery(
                                REVIEW_PROJECTION_JPQL
                                        + " WHERE pr.reviewer.id = :reviewerUserId"
                                        + " AND pr.reviewed.id = :reviewedUserId"
                                        + " AND pr.deleted = FALSE",
                                PlayerReviewProjection.class)
                        .setParameter("reviewerUserId", reviewerUserId)
                        .setParameter("reviewedUserId", reviewedUserId)
                        .getResultList();
        return projections.stream().findFirst().map(PlayerReviewProjection::toPlayerReview);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlayerReview> findByIdIncludingDeleted(final Long reviewId) {
        final List<PlayerReviewProjection> projections =
                em.createQuery(
                                REVIEW_PROJECTION_JPQL + " WHERE pr.id = :reviewId",
                                PlayerReviewProjection.class)
                        .setParameter("reviewId", reviewId)
                        .getResultList();
        return projections.stream().findFirst().map(PlayerReviewProjection::toPlayerReview);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerReviewSummary getSummaryForUser(final Long reviewedUserId) {
        final Object[] counts =
                em.createQuery(
                                "SELECT"
                                        + " COALESCE(SUM(CASE WHEN pr.reaction = :likeReaction THEN 1 ELSE 0 END), 0),"
                                        + " COALESCE(SUM(CASE WHEN pr.reaction = :dislikeReaction THEN 1 ELSE 0 END), 0),"
                                        + " COUNT(pr.id)"
                                        + " FROM PlayerReview pr"
                                        + " WHERE pr.reviewed.id = :reviewedUserId"
                                        + " AND pr.deleted = FALSE",
                                Object[].class)
                        .setParameter("reviewedUserId", reviewedUserId)
                        .setParameter("likeReaction", PlayerReviewReaction.LIKE)
                        .setParameter("dislikeReaction", PlayerReviewReaction.DISLIKE)
                        .getSingleResult();

        return new PlayerReviewSummary(
                reviewedUserId,
                ((Number) counts[0]).longValue(),
                ((Number) counts[1]).longValue(),
                ((Number) counts[2]).longValue());
    }

    @Override
    @Transactional(readOnly = true)
    public int countReviewsForUser(final Long reviewedUserId, final PlayerReviewFilter filter) {
        final PlayerReviewFilter safeFilter = filter == null ? PlayerReviewFilter.BOTH : filter;
        final Optional<PlayerReviewReaction> reaction = safeFilter.getReaction();

        final StringBuilder jpql =
                new StringBuilder(
                        "SELECT COUNT(pr.id) FROM PlayerReview pr"
                                + " WHERE pr.reviewed.id = :reviewedUserId"
                                + " AND pr.deleted = FALSE");

        if (reaction.isPresent()) {
            jpql.append(" AND pr.reaction = :reaction");
        }

        final TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
        query.setParameter("reviewedUserId", reviewedUserId);
        reaction.ifPresent(value -> query.setParameter("reaction", value));

        return query.getSingleResult().intValue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerReview> findReviewsForUser(
            final Long reviewedUserId,
            final PlayerReviewFilter filter,
            final int limit,
            final int offset) {
        final PlayerReviewFilter safeFilter = filter == null ? PlayerReviewFilter.BOTH : filter;
        final Optional<PlayerReviewReaction> reaction = safeFilter.getReaction();

        final StringBuilder jpql =
                new StringBuilder(
                        REVIEW_PROJECTION_JPQL
                                + " WHERE pr.reviewed.id = :reviewedUserId"
                                + " AND pr.deleted = FALSE");

        if (reaction.isPresent()) {
            jpql.append(" AND pr.reaction = :reaction");
        }

        jpql.append(" ORDER BY pr.updatedAt DESC, pr.id DESC");

        final TypedQuery<PlayerReviewProjection> query =
                em.createQuery(jpql.toString(), PlayerReviewProjection.class);
        query.setParameter("reviewedUserId", reviewedUserId);
        reaction.ifPresent(value -> query.setParameter("reaction", value));
        query.setMaxResults(limit);
        query.setFirstResult(offset);
        final List<PlayerReviewProjection> projections = query.getResultList();
        return projections.stream().map(PlayerReviewProjection::toPlayerReview).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canReview(final Long reviewerUserId, final Long reviewedUserId) {
        if (reviewerUserId == null || reviewerUserId.equals(reviewedUserId)) {
            return false;
        }

        final Long count =
                em.createQuery(
                                "SELECT COUNT(reviewer.id)"
                                        + " FROM MatchParticipant reviewer"
                                        + " JOIN MatchParticipant reviewed"
                                        + " ON reviewed.match.id = reviewer.match.id"
                                        + " JOIN reviewer.match m"
                                        + " WHERE reviewer.user.id = :reviewerUserId"
                                        + " AND reviewer.status IN :participantStatuses"
                                        + " AND reviewed.user.id = :reviewedUserId"
                                        + " AND reviewed.status IN :participantStatuses"
                                        + " AND (m.status = :completedStatus"
                                        + " OR (m.status = :openStatus"
                                        + " AND COALESCE(m.endsAt, m.startsAt) <= CURRENT_TIMESTAMP))",
                                Long.class)
                        .setParameter("reviewerUserId", reviewerUserId)
                        .setParameter("reviewedUserId", reviewedUserId)
                        .setParameter(
                                "participantStatuses",
                                List.of(ParticipantStatus.JOINED, ParticipantStatus.CHECKED_IN))
                        .setParameter("completedStatus", EventStatus.COMPLETED)
                        .setParameter("openStatus", EventStatus.OPEN)
                        .getSingleResult();
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findReviewableUserIds(final Long reviewerUserId) {
        if (reviewerUserId == null) {
            return List.of();
        }

        return em.createQuery(
                        "SELECT DISTINCT reviewed.user.id"
                                + " FROM MatchParticipant reviewer"
                                + " JOIN MatchParticipant reviewed"
                                + " ON reviewed.match.id = reviewer.match.id"
                                + " JOIN reviewer.match m"
                                + " WHERE reviewer.user.id = :reviewerUserId"
                                + " AND reviewer.status IN :participantStatuses"
                                + " AND reviewed.user.id <> :reviewerUserId"
                                + " AND reviewed.status IN :participantStatuses"
                                + " AND (m.status = :completedStatus"
                                + " OR (m.status = :openStatus"
                                + " AND COALESCE(m.endsAt, m.startsAt) <= CURRENT_TIMESTAMP))",
                        Long.class)
                .setParameter("reviewerUserId", reviewerUserId)
                .setParameter(
                        "participantStatuses",
                        List.of(ParticipantStatus.JOINED, ParticipantStatus.CHECKED_IN))
                .setParameter("completedStatus", EventStatus.COMPLETED)
                .setParameter("openStatus", EventStatus.OPEN)
                .getResultList();
    }

    private void applyUpsertValues(
            final PlayerReview review,
            final PlayerReviewReaction reaction,
            final String comment,
            final Instant now) {
        review.setReaction(reaction);
        review.setComment(comment);
        review.setUpdatedAt(now);
        review.setDeleted(false);
        review.setDeletedAt(null);
        review.setDeletedBy(null);
        review.setDeleteReason(null);
    }

    private Optional<PlayerReview> findEntityByPairIncludingDeleted(
            final Long reviewerUserId, final Long reviewedUserId, final LockModeType lockModeType) {
        final TypedQuery<PlayerReview> query =
                em.createQuery(
                                "FROM PlayerReview pr"
                                        + " WHERE pr.reviewer.id = :reviewerUserId"
                                        + " AND pr.reviewed.id = :reviewedUserId",
                                PlayerReview.class)
                        .setParameter("reviewerUserId", reviewerUserId)
                        .setParameter("reviewedUserId", reviewedUserId);

        if (lockModeType != null) {
            query.setLockMode(lockModeType);
        }

        final List<PlayerReview> reviews = query.getResultList();
        return reviews.stream().findFirst();
    }

    private void lockUsersForPair(final Long reviewerUserId, final Long reviewedUserId) {
        if (reviewerUserId == null || reviewedUserId == null) {
            return;
        }

        final Long firstUserId = reviewerUserId <= reviewedUserId ? reviewerUserId : reviewedUserId;
        final Long secondUserId =
                reviewerUserId <= reviewedUserId ? reviewedUserId : reviewerUserId;

        em.createQuery("SELECT u.id FROM UserAccount u WHERE u.id = :userId", Long.class)
                .setParameter("userId", firstUserId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        if (!firstUserId.equals(secondUserId)) {
            em.createQuery("SELECT u.id FROM UserAccount u WHERE u.id = :userId", Long.class)
                    .setParameter("userId", secondUserId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();
        }
    }

    private UserAccount userReferenceOrNull(final Long userId) {
        if (userId == null) {
            return null;
        }
        return em.getReference(UserAccount.class, userId);
    }

    private void clearPersistenceContextAfterBulkUpdate(final int updatedRows) {
        if (updatedRows > 0) {
            em.clear();
        }
    }
}
