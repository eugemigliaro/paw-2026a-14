package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.EmailActionRequest;
import ar.edu.itba.paw.models.types.EmailActionStatus;
import ar.edu.itba.paw.models.types.EmailActionType;
import java.time.Clock;
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
public class EmailActionRequestJpaDao implements EmailActionRequestDao {

    @PersistenceContext private EntityManager em;

    private final Clock clock;

    public EmailActionRequestJpaDao(final Clock clock) {
        this.clock = clock;
    }

    @Override
    @Transactional
    public EmailActionRequest create(
            final EmailActionType actionType,
            final String email,
            final Long userId,
            final String tokenHash,
            final String payloadJson,
            final Instant expiresAt) {
        final Instant now = Instant.now(clock);
        final EmailActionRequest emailActionRequest =
                new EmailActionRequest(
                        null,
                        actionType,
                        email,
                        userId,
                        tokenHash,
                        payloadJson,
                        EmailActionStatus.PENDING,
                        expiresAt,
                        null,
                        now,
                        now);

        em.persist(emailActionRequest);
        em.flush();

        return emailActionRequest;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmailActionRequest> findByTokenHash(final String tokenHash) {
        return findEntityByTokenHash(tokenHash, null);
    }

    @Override
    @Transactional
    public Optional<EmailActionRequest> findByTokenHashForUpdate(final String tokenHash) {
        return findEntityByTokenHash(tokenHash, LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    @Transactional
    public void updateStatus(
            final Long id,
            final EmailActionStatus status,
            final Long userId,
            final Instant consumedAt) {
        final EmailActionRequest entity = em.find(EmailActionRequest.class, id);
        if (entity != null) {
            entity.setStatus(status);
            entity.setUserId(userId);
            entity.setConsumedAt(consumedAt);
            entity.setUpdatedAt(Instant.now(clock));
        }
    }

    @Override
    @Transactional
    public void expirePendingByEmailAndActionType(
            final EmailActionType actionType, final String email, final Instant consumedAt) {
        final List<EmailActionRequest> pendingRequests =
                em.createQuery(
                                "FROM EmailActionRequest e "
                                        + "WHERE e.actionType = :actionType "
                                        + "AND e.email = :email "
                                        + "AND e.status = :pendingStatus",
                                EmailActionRequest.class)
                        .setParameter("actionType", actionType)
                        .setParameter("email", email)
                        .setParameter("pendingStatus", EmailActionStatus.PENDING)
                        .getResultList();

        pendingRequests.forEach(
                request -> {
                    request.setStatus(EmailActionStatus.EXPIRED);
                    request.setConsumedAt(consumedAt);
                    request.setUpdatedAt(consumedAt);
                });
        em.flush();
    }

    private Optional<EmailActionRequest> findEntityByTokenHash(
            final String tokenHash, final LockModeType lockModeType) {
        final TypedQuery<EmailActionRequest> query =
                em.createQuery(
                                "FROM EmailActionRequest e WHERE e.tokenHash = :tokenHash",
                                EmailActionRequest.class)
                        .setParameter("tokenHash", tokenHash);
        if (lockModeType != null) {
            query.setLockMode(lockModeType);
        }

        final List<EmailActionRequest> result = query.getResultList();
        return result.stream().findFirst();
    }
}
