package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
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

    @Autowired @NonNull private DataSource dataSource;

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
                        "Jamie",
                        "Rivera",
                        "+1 555 123 4567",
                        "{bcrypt}encoded",
                        UserRole.ADMIN_MOD,
                        verifiedAt);

        final UserAccount persisted =
                userDao.findAccountByEmail("auth@test.com").orElseThrow(AssertionError::new);

        Assertions.assertEquals(account.getId(), persisted.getId());
        Assertions.assertEquals("Jamie", persisted.getName());
        Assertions.assertEquals("Rivera", persisted.getLastName());
        Assertions.assertEquals("+1 555 123 4567", persisted.getPhone());
        Assertions.assertEquals("{bcrypt}encoded", persisted.getPasswordHash());
        Assertions.assertEquals(UserRole.ADMIN_MOD, persisted.getRole());
        Assertions.assertEquals(verifiedAt, persisted.getEmailVerifiedAt());
    }

    @Test
    public void testCreateAccountSupportsUnverifiedUsersWithoutLegacyDefaults() {
        userDao.createAccount(
                "pending@test.com",
                "pending_user",
                "Jamie",
                "Rivera",
                "+1 555 123 4567",
                "{bcrypt}encoded",
                UserRole.USER,
                null);

        final UserAccount persisted =
                userDao.findAccountByEmail("pending@test.com").orElseThrow(AssertionError::new);

        Assertions.assertNull(persisted.getEmailVerifiedAt());
        Assertions.assertEquals(UserRole.USER, persisted.getRole());
    }

    @Test
    public void testUpdatePasswordHashUpdatesExistingAccount() {
        final UserAccount account =
                userDao.createAccount(
                        "reset@test.com",
                        "reset_user",
                        "Jamie",
                        "Rivera",
                        "+1 555 123 4567",
                        null,
                        UserRole.USER,
                        Instant.now());

        userDao.updatePasswordHash(account.getId(), "{bcrypt}newhash");

        final UserAccount persisted =
                userDao.findAccountById(account.getId()).orElseThrow(AssertionError::new);

        Assertions.assertEquals("{bcrypt}newhash", persisted.getPasswordHash());
    }

    @Test
    public void testMarkEmailVerifiedUpdatesExistingAccount() {
        final UserAccount account =
                userDao.createAccount(
                        "verify@test.com",
                        "verify_user",
                        "Jamie",
                        "Rivera",
                        "+1 555 123 4567",
                        "{bcrypt}hash",
                        UserRole.USER,
                        null);
        final Instant verifiedAt = Instant.parse("2026-04-18T12:00:00Z");

        userDao.markEmailVerified(account.getId(), verifiedAt);

        final UserAccount persisted =
                userDao.findAccountById(account.getId()).orElseThrow(AssertionError::new);

        Assertions.assertEquals(verifiedAt, persisted.getEmailVerifiedAt());
    }

    @Test
    public void testUpdateProfileUpdatesEditableFields() {
        final UserAccount account =
                userDao.createAccount(
                        "profile@test.com",
                        "profile_user",
                        "Jamie",
                        "Rivera",
                        "+1 555 123 4567",
                        "{bcrypt}hash",
                        UserRole.USER,
                        Instant.now());

        userDao.updateProfile(account.getId(), "updated_user", "Taylor", "Morgan", null, null);

        final User persisted = userDao.findById(account.getId()).orElseThrow(AssertionError::new);

        Assertions.assertEquals("profile@test.com", persisted.getEmail());
        Assertions.assertEquals("updated_user", persisted.getUsername());
        Assertions.assertEquals("Taylor", persisted.getName());
        Assertions.assertEquals("Morgan", persisted.getLastName());
        Assertions.assertNull(persisted.getPhone());
    }

    @Test
    public void testUpdateProfileImageStoresProfileImageId() throws Exception {
        final UserAccount account =
                userDao.createAccount(
                        "avatar@test.com",
                        "avatar_user",
                        "Jamie",
                        "Rivera",
                        "+1 555 123 4567",
                        "{bcrypt}hash",
                        UserRole.USER,
                        Instant.now());
        final ImageDao imageDao = new ImageJdbcDao(dataSource);
        final Long imageId =
                imageDao.create("image/png", 4L, new ByteArrayInputStream(new byte[] {1, 2, 3, 4}));

        userDao.updateProfileImage(account.getId(), imageId);

        final User persisted = userDao.findById(account.getId()).orElseThrow(AssertionError::new);

        Assertions.assertEquals(imageId, persisted.getProfileImageId());
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
