package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.EmailActionRequest;
import ar.edu.itba.paw.models.EmailActionStatus;
import ar.edu.itba.paw.models.EmailActionType;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class EmailActionRequestJdbcDaoTest {

    @Autowired private EmailActionRequestDao emailActionRequestDao;
    @Autowired private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, created_at, updated_at)"
                        + " VALUES (1, 'testuser', 'user@test.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
    }

    @Test
    public void shouldCreateEmailActionRequest_WithAllFields() {
        final String email = "new@test.com";
        final Long userId = 1L;
        final String tokenHash = "hash-unique-123";
        final String payloadJson = "{\"matchId\": 42}";
        final Instant expiresAt = Instant.parse("2026-05-15T10:30:00Z");

        final EmailActionRequest created =
                emailActionRequestDao.create(
                        EmailActionType.MATCH_RESERVATION,
                        email,
                        userId,
                        tokenHash,
                        payloadJson,
                        expiresAt);

        Assertions.assertNotNull(created.getId());
        Assertions.assertEquals(EmailActionType.MATCH_RESERVATION, created.getActionType());
        Assertions.assertEquals(email, created.getEmail());
        Assertions.assertEquals(userId, created.getUserId());
        Assertions.assertEquals(EmailActionStatus.PENDING, created.getStatus());

        final Long countInDb =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM email_action_requests WHERE id = ? AND token_hash = ? AND status = ?",
                        Long.class,
                        created.getId(),
                        tokenHash,
                        "pending");
        Assertions.assertEquals(1L, countInDb, "Request should be persisted in database");
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
        Assertions.assertNull(created.getUserId());

        final Long userIdFromDb =
                jdbcTemplate.queryForObject(
                        "SELECT user_id FROM email_action_requests WHERE id = ?",
                        (rs, rowNum) ->
                                rs.getObject("user_id") == null ? null : rs.getLong("user_id"),
                        created.getId());
        Assertions.assertNull(userIdFromDb, "user_id should be NULL in database");
    }

    @Test
    public void shouldCreateEmailActionRequest_WithDifferentActionTypes() {
        final Instant expiresAt = Instant.parse("2026-05-17T08:00:00Z");

        final EmailActionRequest verificationRequest =
                emailActionRequestDao.create(
                        EmailActionType.ACCOUNT_VERIFICATION,
                        "verify@test.com",
                        null,
                        "token-verify",
                        "{}",
                        expiresAt);

        final EmailActionRequest passwordResetRequest =
                emailActionRequestDao.create(
                        EmailActionType.PASSWORD_RESET,
                        "reset@test.com",
                        1L,
                        "token-reset",
                        "{}",
                        expiresAt);

        final String verifyType =
                jdbcTemplate.queryForObject(
                        "SELECT action_type FROM email_action_requests WHERE id = ?",
                        String.class,
                        verificationRequest.getId());
        final String resetType =
                jdbcTemplate.queryForObject(
                        "SELECT action_type FROM email_action_requests WHERE id = ?",
                        String.class,
                        passwordResetRequest.getId());

        Assertions.assertEquals("account_verification", verifyType);
        Assertions.assertEquals("password_reset", resetType);
    }

    @Test
    public void shouldFindByTokenHash_WhenRequestExists() {
        final String tokenHash = "unique-token-hash";
        final EmailActionRequest created =
                emailActionRequestDao.create(
                        EmailActionType.MATCH_RESERVATION,
                        "find@test.com",
                        1L,
                        tokenHash,
                        "{\"matchId\": 100}",
                        Instant.parse("2026-05-18T14:00:00Z"));

        final var found = emailActionRequestDao.findByTokenHash(tokenHash);

        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(created.getId(), found.get().getId());
        Assertions.assertEquals(tokenHash, found.get().getTokenHash());
        Assertions.assertEquals(EmailActionStatus.PENDING, found.get().getStatus());
    }

    @Test
    public void shouldFindByTokenHash_WhenRequestNotFound() {
        final String nonExistentTokenHash = "non-existent-token";

        final var found = emailActionRequestDao.findByTokenHash(nonExistentTokenHash);

        Assertions.assertTrue(found.isEmpty());
    }

    @Test
    public void shouldFindByTokenHash_OnlyReturnsExactMatch() {
        final String tokenHash1 = "token-aaa";
        final String tokenHash2 = "token-bbb";

        emailActionRequestDao.create(
                EmailActionType.ACCOUNT_VERIFICATION,
                "user1@test.com",
                null,
                tokenHash1,
                "{}",
                Instant.parse("2026-05-19T09:00:00Z"));

        emailActionRequestDao.create(
                EmailActionType.PASSWORD_RESET,
                "user2@test.com",
                1L,
                tokenHash2,
                "{}",
                Instant.parse("2026-05-19T10:00:00Z"));

        final var result1 = emailActionRequestDao.findByTokenHash(tokenHash1);
        final var result2 = emailActionRequestDao.findByTokenHash(tokenHash2);

        Assertions.assertTrue(result1.isPresent());
        Assertions.assertEquals("user1@test.com", result1.get().getEmail());

        Assertions.assertTrue(result2.isPresent());
        Assertions.assertEquals("user2@test.com", result2.get().getEmail());
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

        final var found = emailActionRequestDao.findByTokenHashForUpdate(tokenHash);

        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(created.getId(), found.get().getId());
        Assertions.assertEquals(tokenHash, found.get().getTokenHash());
    }

    @Test
    public void shouldFindByTokenHashForUpdate_WhenRequestNotFound() {
        final String nonExistentTokenHash = "non-existent-update-token";

        final var found = emailActionRequestDao.findByTokenHashForUpdate(nonExistentTokenHash);

        Assertions.assertTrue(found.isEmpty());
    }

    @Test
    public void shouldUpdateStatus_ToCompleted() {
        final EmailActionRequest created =
                emailActionRequestDao.create(
                        EmailActionType.MATCH_RESERVATION,
                        "complete@test.com",
                        1L,
                        "token-to-complete",
                        "{\"matchId\": 50}",
                        Instant.parse("2026-05-21T13:00:00Z"));

        final Instant consumedAt = Instant.parse("2026-05-21T13:15:00Z");

        emailActionRequestDao.updateStatus(
                created.getId(), EmailActionStatus.COMPLETED, 1L, consumedAt);

        final var updated = emailActionRequestDao.findByTokenHash("token-to-complete");

        Assertions.assertTrue(updated.isPresent());
        Assertions.assertEquals(EmailActionStatus.COMPLETED, updated.get().getStatus());
        Assertions.assertEquals(1L, updated.get().getUserId());
        Assertions.assertEquals(consumedAt, updated.get().getConsumedAt());

        final String statusInDb =
                jdbcTemplate.queryForObject(
                        "SELECT status FROM email_action_requests WHERE id = ?",
                        String.class,
                        created.getId());
        Assertions.assertEquals("completed", statusInDb);
    }

    @Test
    public void shouldUpdateStatus_WithoutChangingOtherFields() {
        final String originalTokenHash = "token-immutable";
        final String originalEmail = "immutable@test.com";
        final EmailActionRequest created =
                emailActionRequestDao.create(
                        EmailActionType.PASSWORD_RESET,
                        originalEmail,
                        1L,
                        originalTokenHash,
                        "{\"reset\": true}",
                        Instant.parse("2026-05-22T15:00:00Z"));

        emailActionRequestDao.updateStatus(
                created.getId(),
                EmailActionStatus.EXPIRED,
                null,
                Instant.parse("2026-05-22T16:00:00Z"));

        final var updated = emailActionRequestDao.findByTokenHash(originalTokenHash);
        Assertions.assertTrue(updated.isPresent());
        Assertions.assertEquals(originalEmail, updated.get().getEmail());
        Assertions.assertEquals(EmailActionType.PASSWORD_RESET, updated.get().getActionType());
    }

    @Test
    public void shouldUpdateStatus_CanSetUserIdToNull() {
        final EmailActionRequest created =
                emailActionRequestDao.create(
                        EmailActionType.ACCOUNT_VERIFICATION,
                        "null-user@test.com",
                        1L,
                        "token-null-user",
                        "{}",
                        Instant.parse("2026-05-23T12:00:00Z"));

        emailActionRequestDao.updateStatus(
                created.getId(),
                EmailActionStatus.EXPIRED,
                null,
                Instant.parse("2026-05-23T13:00:00Z"));

        final Long userIdFromDb =
                jdbcTemplate.queryForObject(
                        "SELECT user_id FROM email_action_requests WHERE id = ?",
                        (rs, rowNum) ->
                                rs.getObject("user_id") == null ? null : rs.getLong("user_id"),
                        created.getId());
        Assertions.assertNull(userIdFromDb);
    }

    @Test
    public void shouldExpirePendingByEmailAndActionType_OnlyMatchingRequests() {
        final String targetEmail = "expire-test@test.com";

        emailActionRequestDao.create(
                EmailActionType.ACCOUNT_VERIFICATION,
                targetEmail,
                null,
                "token-verify",
                "{}",
                Instant.parse("2026-05-24T10:00:00Z"));

        emailActionRequestDao.create(
                EmailActionType.PASSWORD_RESET,
                targetEmail,
                1L,
                "token-reset",
                "{}",
                Instant.parse("2026-05-24T10:00:00Z"));

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

        final var expiredRequest = emailActionRequestDao.findByTokenHash("token-verify");
        Assertions.assertTrue(expiredRequest.isPresent());
        Assertions.assertEquals(EmailActionStatus.EXPIRED, expiredRequest.get().getStatus());

        final var resetRequest = emailActionRequestDao.findByTokenHash("token-reset");
        Assertions.assertTrue(resetRequest.isPresent());
        Assertions.assertEquals(EmailActionStatus.PENDING, resetRequest.get().getStatus());

        final var otherRequest = emailActionRequestDao.findByTokenHash("token-other");
        Assertions.assertTrue(otherRequest.isPresent());
        Assertions.assertEquals(EmailActionStatus.PENDING, otherRequest.get().getStatus());
    }

    @Test
    public void shouldExpirePendingByEmailAndActionType_NoMatchesDoesNothing() {
        emailActionRequestDao.create(
                EmailActionType.PASSWORD_RESET,
                "no-match@test.com",
                1L,
                "token-no-match",
                "{}",
                Instant.parse("2026-05-25T09:00:00Z"));

        emailActionRequestDao.expirePendingByEmailAndActionType(
                EmailActionType.ACCOUNT_VERIFICATION,
                "no-match@test.com",
                Instant.parse("2026-05-25T10:00:00Z"));

        final var request = emailActionRequestDao.findByTokenHash("token-no-match");
        Assertions.assertTrue(request.isPresent());
        Assertions.assertEquals(EmailActionStatus.PENDING, request.get().getStatus());
    }

    @Test
    public void shouldExpirePendingByEmailAndActionType_MultipleMatchingRequests() {
        final String email = "multi@test.com";

        for (int i = 0; i < 3; i++) {
            emailActionRequestDao.create(
                    EmailActionType.ACCOUNT_VERIFICATION,
                    email,
                    null,
                    "token-multi-" + i,
                    "{}",
                    Instant.parse("2026-05-26T10:00:00Z"));
        }

        final Instant consumedAt = Instant.parse("2026-05-26T11:00:00Z");

        emailActionRequestDao.expirePendingByEmailAndActionType(
                EmailActionType.ACCOUNT_VERIFICATION, email, consumedAt);

        for (int i = 0; i < 3; i++) {
            final var request = emailActionRequestDao.findByTokenHash("token-multi-" + i);
            Assertions.assertTrue(request.isPresent());
            Assertions.assertEquals(
                    EmailActionStatus.EXPIRED,
                    request.get().getStatus(),
                    "All matching requests should be expired");
        }
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

        emailActionRequestDao.updateStatus(
                request.getId(),
                EmailActionStatus.EXPIRED,
                null,
                Instant.parse("2026-05-27T10:30:00Z"));

        emailActionRequestDao.expirePendingByEmailAndActionType(
                EmailActionType.ACCOUNT_VERIFICATION, email, Instant.parse("2026-05-27T11:00:00Z"));

        final var result = emailActionRequestDao.findByTokenHash("token-pre-expired");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(EmailActionStatus.EXPIRED, result.get().getStatus());
    }
}
