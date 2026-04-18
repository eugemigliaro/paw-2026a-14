package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import java.sql.Timestamp;
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
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class UserJdbcDaoTest {

    @Autowired private UserDao userDao;

    @Autowired private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    public void testCreateUserWhenUserDoesNotExist() {
        final String email = "[EMAIL_ADDRESS]";
        final String username = "[USERNAME]";

        final User user = userDao.createUser(email, username);

        Assertions.assertNotNull(user);
        Assertions.assertEquals(email, user.getEmail());
        Assertions.assertEquals(username, user.getUsername());
        Assertions.assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "users"));
    }

    @Test
    public void testCreateAccountStoresPasswordRoleAndVerificationState() {
        final Instant verifiedAt = Instant.parse("2026-04-17T18:00:00Z");

        final UserAccount account =
                userDao.createAccount(
                        "auth@test.com",
                        "auth_user",
                        "{bcrypt}encoded",
                        UserRole.ADMIN_MOD,
                        verifiedAt);

        final UserAccount persisted =
                userDao.findAccountByEmail("auth@test.com").orElseThrow(AssertionError::new);

        Assertions.assertEquals(account.getId(), persisted.getId());
        Assertions.assertEquals("{bcrypt}encoded", persisted.getPasswordHash());
        Assertions.assertEquals(UserRole.ADMIN_MOD, persisted.getRole());
        Assertions.assertEquals(verifiedAt, persisted.getEmailVerifiedAt());
    }

    @Test
    public void testCreateAccountSupportsUnverifiedUsersWithoutLegacyDefaults() {
        userDao.createAccount(
                "pending@test.com", "pending_user", "{bcrypt}encoded", UserRole.USER, null);

        final UserAccount persisted =
                userDao.findAccountByEmail("pending@test.com").orElseThrow(AssertionError::new);

        Assertions.assertNull(persisted.getEmailVerifiedAt());
        Assertions.assertEquals(UserRole.USER, persisted.getRole());
    }

    @Test
    public void testUpdatePasswordHashUpdatesExistingAccount() {
        final UserAccount account =
                userDao.createAccount(
                        "reset@test.com", "reset_user", null, UserRole.USER, Instant.now());

        userDao.updatePasswordHash(account.getId(), "{bcrypt}newhash");

        final UserAccount persisted =
                userDao.findAccountById(account.getId()).orElseThrow(AssertionError::new);

        Assertions.assertEquals("{bcrypt}newhash", persisted.getPasswordHash());
    }

    @Test
    public void testMarkEmailVerifiedUpdatesExistingAccount() {
        final UserAccount account =
                userDao.createAccount(
                        "verify@test.com", "verify_user", "{bcrypt}hash", UserRole.USER, null);
        final Instant verifiedAt = Instant.parse("2026-04-18T12:00:00Z");

        userDao.markEmailVerified(account.getId(), verifiedAt);

        final UserAccount persisted =
                userDao.findAccountById(account.getId()).orElseThrow(AssertionError::new);

        Assertions.assertEquals(verifiedAt, persisted.getEmailVerifiedAt());
    }

    @Test
    public void testLegacyCreateUserMarksUserAsVerifiedWithDefaultRole() {
        final User user = userDao.createUser("legacy@test.com", "legacy_user");

        final UserAccount persisted =
                userDao.findAccountById(user.getId()).orElseThrow(AssertionError::new);

        Assertions.assertEquals(UserRole.USER, persisted.getRole());
        Assertions.assertNotNull(persisted.getEmailVerifiedAt());
        Assertions.assertNull(persisted.getPasswordHash());
        Assertions.assertTrue(
                jdbcTemplate.queryForObject(
                                "SELECT email_verified_at FROM users WHERE id = ?",
                                Timestamp.class,
                                user.getId())
                        != null);
    }
}
