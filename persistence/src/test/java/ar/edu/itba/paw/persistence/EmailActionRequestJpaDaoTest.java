package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.EmailActionRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EmailActionStatus;
import ar.edu.itba.paw.models.types.EmailActionType;
import java.time.Instant;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class EmailActionRequestJpaDaoTest {

    @Autowired private EmailActionRequestDao emailActionRequestDao;

    @PersistenceContext private EntityManager em;

    private User user;

    @BeforeEach
    public void setUp() {
        em.createNativeQuery(
                        "INSERT INTO users (id, username, email, created_at, updated_at) "
                                + "VALUES (1, 'testuser', 'user@test.com', "
                                + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .executeUpdate();

        user = em.find(User.class, 1L);

        flushAndClear();
    }

    @Test
    public void shouldCreateEmailActionRequest_WithAllFields() {
        final String email = "new@test.com";
        final String tokenHash = "hash-unique-123";
        final String payloadJson = "{\"matchId\": 42}";
        final Instant expiresAt = Instant.parse("2026-05-15T10:30:00Z");

        final EmailActionRequest created =
                emailActionRequestDao.create(
                        EmailActionType.MATCH_RESERVATION,
                        email,
                        user,
                        tokenHash,
                        payloadJson,
                        expiresAt);

        Assertions.assertNotNull(created.getId());
        Assertions.assertEquals(EmailActionType.MATCH_RESERVATION, created.getActionType());
        Assertions.assertEquals(email, created.getEmail());
        Assertions.assertEquals(user.getId(), created.getUser().getId());
        Assertions.assertEquals(tokenHash, created.getTokenHash());
        Assertions.assertEquals(payloadJson, created.getPayloadJson());
        Assertions.assertEquals(EmailActionStatus.PENDING, created.getStatus());
        Assertions.assertEquals(expiresAt, created.getExpiresAt());
        Assertions.assertNull(created.getConsumedAt());
        Assertions.assertEquals(1L, countRequests());
        assertPersistedRequest(
                created.getId(),
                EmailActionType.MATCH_RESERVATION,
                email,
                user,
                tokenHash,
                EmailActionStatus.PENDING,
                null);
    }

    @Test
    public void shouldCreateEmailActionRequest_WithoutUserId() {
        final String email = "unverified@test.com";
        final String tokenHash = "token-no-user";
        final Instant expiresAt = Instant.parse("2026-05-16T12:00:00Z");

        final EmailActionRequest created =
                emailActionRequestDao.create(
                        EmailActionType.ACCOUNT_VERIFICATION,
                        email,
                        null,
                        tokenHash,
                        "{}",
                        expiresAt);

        Assertions.assertNotNull(created.getId());
        Assertions.assertNull(created.getUser());
        Assertions.assertEquals(1L, countRequests());
        assertPersistedRequest(
                created.getId(),
                EmailActionType.ACCOUNT_VERIFICATION,
                email,
                null,
                tokenHash,
                EmailActionStatus.PENDING,
                null);
    }

    @Test
    public void shouldFindByTokenHash_WhenRequestExists() {
        final String tokenHash = "unique-token-hash";
        final EmailActionRequest created =
                emailActionRequestDao.create(
                        EmailActionType.MATCH_RESERVATION,
                        "find@test.com",
                        user,
                        tokenHash,
                        "{\"matchId\": 100}",
                        Instant.parse("2026-05-18T14:00:00Z"));
        flushAndClear();

        final Optional<EmailActionRequest> found = emailActionRequestDao.findByTokenHash(tokenHash);

        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(created.getId(), found.get().getId());
        Assertions.assertEquals(tokenHash, found.get().getTokenHash());
        Assertions.assertEquals(EmailActionStatus.PENDING, found.get().getStatus());
        Assertions.assertEquals(1L, countRequests());
        assertPersistedRequest(
                created.getId(),
                EmailActionType.MATCH_RESERVATION,
                "find@test.com",
                user,
                tokenHash,
                EmailActionStatus.PENDING,
                null);
    }

    @Test
    public void shouldFindByTokenHash_WhenRequestNotFound() {
        final String nonExistentTokenHash = "non-existent-token";

        final Optional<EmailActionRequest> found =
                emailActionRequestDao.findByTokenHash(nonExistentTokenHash);

        Assertions.assertTrue(found.isEmpty());
        Assertions.assertEquals(0L, countRequests());
    }

    @Test
    public void shouldFindByTokenHashForUpdate_WhenRequestExists() {
        final String tokenHash = "token-for-update";
        final EmailActionRequest created =
                emailActionRequestDao.create(
                        EmailActionType.ACCOUNT_VERIFICATION,
                        "update@test.com",
                        null,
                        tokenHash,
                        "{}",
                        Instant.parse("2026-05-20T11:00:00Z"));
        flushAndClear();

        final Optional<EmailActionRequest> found =
                emailActionRequestDao.findByTokenHashForUpdate(tokenHash);

        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(created.getId(), found.get().getId());
        Assertions.assertEquals(tokenHash, found.get().getTokenHash());
        Assertions.assertEquals(1L, countRequests());
        assertPersistedRequest(
                created.getId(),
                EmailActionType.ACCOUNT_VERIFICATION,
                "update@test.com",
                null,
                tokenHash,
                EmailActionStatus.PENDING,
                null);
    }

    @Test
    public void shouldUpdateStatus_ToCompleted() {
        final EmailActionRequest created =
                emailActionRequestDao.create(
                        EmailActionType.MATCH_RESERVATION,
                        "complete@test.com",
                        user,
                        "token-to-complete",
                        "{\"matchId\": 50}",
                        Instant.parse("2026-05-21T13:00:00Z"));
        final Instant consumedAt = Instant.parse("2026-05-21T13:15:00Z");

        emailActionRequestDao.updateStatus(
                created.getId(), EmailActionStatus.COMPLETED, user, consumedAt);

        final Optional<EmailActionRequest> updated =
                emailActionRequestDao.findByTokenHash("token-to-complete");
        Assertions.assertTrue(updated.isPresent());
        Assertions.assertEquals(EmailActionStatus.COMPLETED, updated.get().getStatus());
        Assertions.assertEquals(user.getId(), updated.get().getUser().getId());
        Assertions.assertEquals(consumedAt, updated.get().getConsumedAt());
        Assertions.assertEquals(1L, countRequests());
        assertPersistedRequest(
                created.getId(),
                EmailActionType.MATCH_RESERVATION,
                "complete@test.com",
                user,
                "token-to-complete",
                EmailActionStatus.COMPLETED,
                consumedAt);
    }

    @Test
    public void shouldUpdateStatus_CanSetUserIdToNull() {
        final EmailActionRequest created =
                emailActionRequestDao.create(
                        EmailActionType.ACCOUNT_VERIFICATION,
                        "null-user@test.com",
                        user,
                        "token-null-user",
                        "{}",
                        Instant.parse("2026-05-23T12:00:00Z"));
        final Instant consumedAt = Instant.parse("2026-05-23T13:00:00Z");

        emailActionRequestDao.updateStatus(
                created.getId(), EmailActionStatus.EXPIRED, null, consumedAt);

        final Optional<EmailActionRequest> updated =
                emailActionRequestDao.findByTokenHash("token-null-user");
        Assertions.assertTrue(updated.isPresent());
        Assertions.assertNull(updated.get().getUser());
        Assertions.assertEquals(EmailActionStatus.EXPIRED, updated.get().getStatus());
        Assertions.assertEquals(1L, countRequests());
        assertPersistedRequest(
                created.getId(),
                EmailActionType.ACCOUNT_VERIFICATION,
                "null-user@test.com",
                null,
                "token-null-user",
                EmailActionStatus.EXPIRED,
                consumedAt);
    }

    @Test
    public void shouldExpirePendingByEmailAndActionType_OnlyMatchingRequests() {
        final String targetEmail = "expire-test@test.com";
        final EmailActionRequest verification =
                emailActionRequestDao.create(
                        EmailActionType.ACCOUNT_VERIFICATION,
                        targetEmail,
                        null,
                        "token-verify",
                        "{}",
                        Instant.parse("2026-05-24T10:00:00Z"));
        final EmailActionRequest passwordReset =
                emailActionRequestDao.create(
                        EmailActionType.PASSWORD_RESET,
                        targetEmail,
                        user,
                        "token-reset",
                        "{}",
                        Instant.parse("2026-05-24T10:00:00Z"));
        final EmailActionRequest otherEmail =
                emailActionRequestDao.create(
                        EmailActionType.ACCOUNT_VERIFICATION,
                        "other@test.com",
                        null,
                        "token-other",
                        "{}",
                        Instant.parse("2026-05-24T10:00:00Z"));
        final Instant consumedAt = Instant.parse("2026-05-24T11:00:00Z");

        emailActionRequestDao.expirePendingByEmailAndActionType(
                EmailActionType.ACCOUNT_VERIFICATION, targetEmail, consumedAt);

        Assertions.assertEquals(3L, countRequests());
        assertPersistedRequest(
                verification.getId(),
                EmailActionType.ACCOUNT_VERIFICATION,
                targetEmail,
                null,
                "token-verify",
                EmailActionStatus.EXPIRED,
                consumedAt);
        assertPersistedRequest(
                passwordReset.getId(),
                EmailActionType.PASSWORD_RESET,
                targetEmail,
                user,
                "token-reset",
                EmailActionStatus.PENDING,
                null);
        assertPersistedRequest(
                otherEmail.getId(),
                EmailActionType.ACCOUNT_VERIFICATION,
                "other@test.com",
                null,
                "token-other",
                EmailActionStatus.PENDING,
                null);
    }

    @Test
    public void shouldExpirePendingByEmailAndActionType_DoesNotExpireAlreadyExpired() {
        final String email = "already-expired@test.com";
        final EmailActionRequest request =
                emailActionRequestDao.create(
                        EmailActionType.ACCOUNT_VERIFICATION,
                        email,
                        null,
                        "token-pre-expired",
                        "{}",
                        Instant.parse("2026-05-27T10:00:00Z"));
        final Instant firstConsumedAt = Instant.parse("2026-05-27T10:30:00Z");
        emailActionRequestDao.updateStatus(
                request.getId(), EmailActionStatus.EXPIRED, null, firstConsumedAt);

        emailActionRequestDao.expirePendingByEmailAndActionType(
                EmailActionType.ACCOUNT_VERIFICATION, email, Instant.parse("2026-05-27T11:00:00Z"));

        Assertions.assertEquals(1L, countRequests());
        assertPersistedRequest(
                request.getId(),
                EmailActionType.ACCOUNT_VERIFICATION,
                email,
                null,
                "token-pre-expired",
                EmailActionStatus.EXPIRED,
                firstConsumedAt);
    }

    private long countRequests() {
        flushAndClear();
        return ((Number)
                        em.createNativeQuery("SELECT COUNT(*) FROM email_action_requests")
                                .getSingleResult())
                .longValue();
    }

    private void assertPersistedRequest(
            final Long id,
            final EmailActionType actionType,
            final String email,
            final User user,
            final String tokenHash,
            final EmailActionStatus status,
            final Instant consumedAt) {
        flushAndClear();
        final EmailActionRequest persisted = em.find(EmailActionRequest.class, id);
        Assertions.assertNotNull(persisted);
        Assertions.assertEquals(actionType, persisted.getActionType());
        Assertions.assertEquals(email, persisted.getEmail());
        if (user == null) {
            Assertions.assertNull(persisted.getUser());
        } else {
            Assertions.assertNotNull(persisted.getUser());
            Assertions.assertEquals(user.getId(), persisted.getUser().getId());
        }
        Assertions.assertEquals(tokenHash, persisted.getTokenHash());
        Assertions.assertEquals(status, persisted.getStatus());
        Assertions.assertEquals(status.getDbValue(), persistedStatus(id));
        Assertions.assertEquals(consumedAt, persisted.getConsumedAt());
    }

    private String persistedStatus(final Long id) {
        return (String)
                em.createNativeQuery("SELECT status FROM email_action_requests WHERE id = :id")
                        .setParameter("id", id)
                        .getSingleResult();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
