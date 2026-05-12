package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.types.AppealDecision;
import ar.edu.itba.paw.models.types.ReportTargetType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UserBanJpaDao implements UserBanDao {

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public UserBan createBan(final ModerationReport moderationReport, final Instant bannedUntil) {
        final UserBan ban = new UserBan(null, moderationReport, bannedUntil);

        em.persist(ban);
        em.flush();

        return ban;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserBan> findById(final Long id) {
        final UserBan userBan = em.find(UserBan.class, id);
        return Optional.ofNullable(userBan);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserBan> findLatestBanForUser(final Long userId) {
        return findFirstBan(
                "FROM UserBan ub "
                        + "WHERE ub.moderationReport.targetType = :targetType "
                        + "AND ub.moderationReport.targetId = :userId "
                        + "ORDER BY ub.id DESC",
                userId,
                ReportTargetType.USER,
                null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserBan> findActiveBanForUser(final Long userId, final Instant now) {
        return findFirstBan(
                "FROM UserBan ub "
                        + "WHERE ub.moderationReport.targetType = :targetType "
                        + "AND ub.moderationReport.targetId = :userId "
                        + "AND ub.bannedUntil > :now "
                        + "AND (ub.moderationReport.appealDecision IS NULL "
                        + "OR ub.moderationReport.appealDecision <> :liftedAppealDecision) "
                        + "ORDER BY ub.id DESC",
                userId,
                ReportTargetType.USER,
                now);
    }

    @Override
    @Transactional
    public void upliftBan(final Long id) {
        final UserBan ban = em.find(UserBan.class, id);
        if (ban != null) {
            ban.setBannedUntil(Instant.now());
        }
    }

    private Optional<UserBan> findFirstBan(
            final String jpql,
            final Long userId,
            final ReportTargetType targetType,
            final Instant now) {
        final TypedQuery<UserBan> query =
                em.createQuery(jpql, UserBan.class)
                        .setParameter("userId", userId)
                        .setParameter("targetType", targetType);
        if (now != null) {
            query.setParameter("now", now)
                    .setParameter("liftedAppealDecision", AppealDecision.LIFTED);
        }
        query.setMaxResults(1);

        final List<UserBan> result = query.getResultList();
        return result.stream().findFirst();
    }
}
