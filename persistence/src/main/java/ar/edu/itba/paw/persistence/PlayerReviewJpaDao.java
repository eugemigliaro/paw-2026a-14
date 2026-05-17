package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.User;
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

@Repository
public class PlayerReviewJpaDao implements PlayerReviewDao {

    @PersistenceContext private EntityManager em;

    @Override
    public PlayerReview upsertReview(
            final User reviewer,
            final User reviewed,
            final PlayerReviewReaction reaction,
            final String comment) {
        lockUsersForPair(reviewer.getId(), reviewed.getId());

        final Instant now = Instant.now();
        final Optional<PlayerReview> existing =
                findEntityByPairIncludingDeleted(
                        reviewer.getId(), reviewed.getId(), LockModeType.PESSIMISTIC_WRITE);

        if (existing.isPresent()) {
            applyUpsertValues(existing.get(), reaction, comment, now);
        } else {
            final PlayerReview review =
                    new PlayerReview(
                            null, reviewer, reviewed, reaction, comment, now, now, false, null,
                            null, null);
            em.persist(review);
        }

        return findByPair(reviewer, reviewed)
                .orElseThrow(() -> new IllegalStateException("Player review was not persisted"));
    }

    @Override
    public boolean softDeleteReview(final User reviewer, final User reviewed) {
        return softDeleteReview(reviewer, reviewed, null, null);
    }

    @Override
    public boolean softDeleteReview(
            final User reviewer, final User reviewed, final User deletedBy, final String reason) {
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
                        .setParameter("deletedBy", deletedBy)
                        .setParameter("reason", reason)
                        .setParameter("reviewerUserId", reviewer.getId())
                        .setParameter("reviewedUserId", reviewed.getId())
                        .executeUpdate();
        return rows == 1;
    }

    @Override
    public boolean restoreReview(final User reviewer, final User reviewed) {
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
                        .setParameter("reviewerUserId", reviewer.getId())
                        .setParameter("reviewedUserId", reviewed.getId())
                        .executeUpdate();
        return rows == 1;
    }

    @Override
    public Optional<PlayerReview> findByPair(final User reviewer, final User reviewed) {
        final List<PlayerReview> projections =
                em.createQuery(
                                "From PlayerReview pr"
                                        + " WHERE pr.reviewer.id = :reviewerUserId"
                                        + " AND pr.reviewed.id = :reviewedUserId"
                                        + " AND pr.deleted = FALSE",
                                PlayerReview.class)
                        .setParameter("reviewerUserId", reviewer.getId())
                        .setParameter("reviewedUserId", reviewed.getId())
                        .getResultList();
        return projections.stream().findFirst();
    }

    @Override
    public Optional<PlayerReview> findByIdIncludingDeleted(final Long reviewId) {
        return Optional.of(em.find(PlayerReview.class, reviewId));
    }

    @Override
    public PlayerReviewSummary getSummaryForUser(final User reviewed) {
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
                        .setParameter("reviewedUserId", reviewed.getId())
                        .setParameter("likeReaction", PlayerReviewReaction.LIKE)
                        .setParameter("dislikeReaction", PlayerReviewReaction.DISLIKE)
                        .getSingleResult();

        return new PlayerReviewSummary(
                reviewed.getId(),
                ((Number) counts[0]).longValue(),
                ((Number) counts[1]).longValue(),
                ((Number) counts[2]).longValue());
    }

    @Override
    public int countReviewsForUser(final User reviewed, final PlayerReviewFilter filter) {
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
        query.setParameter("reviewedUserId", reviewed.getId());
        reaction.ifPresent(value -> query.setParameter("reaction", value));

        return query.getSingleResult().intValue();
    }

    @Override
    public List<PlayerReview> findReviewsForUser(
            final User reviewed,
            final PlayerReviewFilter filter,
            final int limit,
            final int offset) {
        final PlayerReviewFilter safeFilter = filter == null ? PlayerReviewFilter.BOTH : filter;
        final Optional<PlayerReviewReaction> reaction = safeFilter.getReaction();

        final StringBuilder jpql =
                new StringBuilder(
                        "From PlayerReview pr"
                                + " WHERE pr.reviewed.id = :reviewedUserId"
                                + " AND pr.deleted = FALSE");

        if (reaction.isPresent()) {
            jpql.append(" AND pr.reaction = :reaction");
        }

        jpql.append(" ORDER BY pr.updatedAt DESC, pr.id DESC");

        final TypedQuery<PlayerReview> query = em.createQuery(jpql.toString(), PlayerReview.class);
        query.setParameter("reviewedUserId", reviewed.getId());
        reaction.ifPresent(value -> query.setParameter("reaction", value));
        query.setMaxResults(limit);
        query.setFirstResult(offset);
        return query.getResultList();
    }

    @Override
    public boolean canReview(final User reviewer, final User reviewed) {
        if (reviewer == null || reviewer.equals(reviewed)) {
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
                        .setParameter("reviewerUserId", reviewer.getId())
                        .setParameter("reviewedUserId", reviewed.getId())
                        .setParameter(
                                "participantStatuses",
                                List.of(ParticipantStatus.JOINED, ParticipantStatus.CHECKED_IN))
                        .setParameter("completedStatus", EventStatus.COMPLETED)
                        .setParameter("openStatus", EventStatus.OPEN)
                        .getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public List<Long> findReviewableUserIds(final User reviewer) {
        if (reviewer == null) {
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
                .setParameter("reviewerUserId", reviewer.getId())
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
}
