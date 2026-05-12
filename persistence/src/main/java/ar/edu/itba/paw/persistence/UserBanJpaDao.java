package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.AppealDecision;
import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.ReportTargetType;
import ar.edu.itba.paw.models.UserBan;
import java.time.Instant;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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
        final TypedQuery<UserBan> query =
                em.createQuery(
                                "FROM UserBan ub "
                                        + "WHERE ub.moderationReport.targetType = :type "
                                        + "AND ub.moderationReport.targetId = :userId "
                                        + "ORDER BY ub.id DESC",
                                UserBan.class)
                        .setParameter("userId", userId)
                        .setParameter("type", ReportTargetType.USER);
        query.setMaxResults(1);
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserBan> findActiveBanForUser(final Long userId, final Instant now) {
        final TypedQuery<UserBan> query =
                em.createQuery(
                                "FROM UserBan ub "
                                        + "WHERE ub.moderationReport.targetType = :type "
                                        + "AND ub.moderationReport.targetId = :userId "
                                        + "AND ub.bannedUntil > :now "
                                        + "AND (ub.moderationReport.appealDecision IS NULL OR ub.moderationReport.appealDecision <> :appealDecision) "
                                        + "ORDER BY ub.id DESC",
                                UserBan.class)
                        .setParameter("userId", userId)
                        .setParameter("now", now)
                        .setParameter("type", ReportTargetType.USER)
                        .setParameter("appealDecision", AppealDecision.LIFTED);
        query.setMaxResults(1);
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void upliftBan(final Long id) {
        final UserBan ban = em.find(UserBan.class, id);
        if (ban != null) {
            ban.setBannedUntil(Instant.now());
            em.merge(ban);
        }
    }
}
