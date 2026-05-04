package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.UserRole;
import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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
                        UserLanguages.SPANISH,
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
        Assertions.assertEquals(UserLanguages.SPANISH, persisted.getPreferredLanguage());
    }

    @Test
    public void testCreateAccountSupportsUnverifiedUsersWithoutLegacyDefaults() {
        userDao.createAccount(
                "pending@test.com",
                "pending_user",
                "Jamie",
                "Rivera",
                "+1 555 123 4567",
                UserLanguages.DEFAULT_LANGUAGE,
                "{bcrypt}encoded",
                UserRole.USER,
                null);

        final UserAccount persisted =
                userDao.findAccountByEmail("pending@test.com").orElseThrow(AssertionError::new);

        Assertions.assertNull(persisted.getEmailVerifiedAt());
        Assertions.assertEquals(UserRole.USER, persisted.getRole());
        Assertions.assertEquals(UserLanguages.DEFAULT_LANGUAGE, persisted.getPreferredLanguage());
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
                        UserLanguages.DEFAULT_LANGUAGE,
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
                        UserLanguages.DEFAULT_LANGUAGE,
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
                        UserLanguages.DEFAULT_LANGUAGE,
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
        Assertions.assertEquals(UserLanguages.DEFAULT_LANGUAGE, persisted.getPreferredLanguage());
    }

    @Test
    public void testUpdatePreferredLanguageUpdatesExistingAccount() {
        final UserAccount account =
                userDao.createAccount(
                        "locale@test.com",
                        "locale_user",
                        "Jamie",
                        "Rivera",
                        "+1 555 123 4567",
                        UserLanguages.DEFAULT_LANGUAGE,
                        "{bcrypt}hash",
                        UserRole.USER,
                        Instant.now());

        userDao.updatePreferredLanguage(account.getId(), UserLanguages.SPANISH);

        final User persisted = userDao.findById(account.getId()).orElseThrow(AssertionError::new);

        Assertions.assertEquals(UserLanguages.SPANISH, persisted.getPreferredLanguage());
    }

    @Test
    public void testFindByIdsReturnsMatchingUsers() {
        final User first = userDao.createUser("first@test.com", "first_user");
        final User second = userDao.createUser("second@test.com", "second_user");
        userDao.createUser("third@test.com", "third_user");

        final List<User> result = userDao.findByIds(List.of(first.getId(), second.getId()));

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.stream().anyMatch(user -> user.getId().equals(first.getId())));
        Assertions.assertTrue(
                result.stream().anyMatch(user -> user.getId().equals(second.getId())));
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
                        UserLanguages.DEFAULT_LANGUAGE,
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
        Assertions.assertEquals(UserLanguages.DEFAULT_LANGUAGE, persisted.getPreferredLanguage());
        Assertions.assertTrue(
                jdbcTemplate.queryForObject(
                                "SELECT email_verified_at FROM users WHERE id = ?",
                                Timestamp.class,
                                user.getId())
                        != null);
    }

    @Test
    public void shouldFindByEmail_WhenUserExists() {
        final String email = "findme@test.com";
        final User created = userDao.createUser(email, "findme_user");

        final var result = userDao.findByEmail(email);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(created.getId(), result.get().getId());
        Assertions.assertEquals(email, result.get().getEmail());

        final Long countInDb =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM users WHERE email = ?", Long.class, email);
        Assertions.assertEquals(1L, countInDb, "User should exist in database with exact email");
    }

    @Test
    public void shouldFindByEmail_WhenUserNotFound() {
        final String nonExistentEmail = "notfound@test.com";

        final var result = userDao.findByEmail(nonExistentEmail);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void shouldFindByEmail_IsCaseSensitive() {
        final String email = "CaseSensitive@test.com";
        userDao.createUser(email, "case_user");

        final var resultExact = userDao.findByEmail(email);
        userDao.findByEmail(email.toLowerCase());
        userDao.findByEmail(email.toUpperCase());

        Assertions.assertTrue(resultExact.isPresent(), "Exact case match should find user");
    }

    @Test
    public void shouldFindById_WhenUserExists() {
        final User created = userDao.createUser("byid@test.com", "byid_user");

        final var result = userDao.findById(created.getId());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(created.getId(), result.get().getId());
        Assertions.assertEquals(created.getEmail(), result.get().getEmail());
        Assertions.assertEquals(created.getUsername(), result.get().getUsername());

        final Long countInDb =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM users WHERE id = ?", Long.class, created.getId());
        Assertions.assertEquals(1L, countInDb, "User should exist in database");
    }

    @Test
    public void shouldFindById_WhenUserNotFound() {
        final Long nonExistentId = 999999L;

        final var result = userDao.findById(nonExistentId);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void shouldFindById_WithMultipleUsers_ReturnsExactUser() {
        final User user1 = userDao.createUser("user1@test.com", "user1");
        final User user2 = userDao.createUser("user2@test.com", "user2");
        final User user3 = userDao.createUser("user3@test.com", "user3");

        final var result = userDao.findById(user2.getId());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(user2.getId(), result.get().getId());
        Assertions.assertEquals("user2@test.com", result.get().getEmail());
        Assertions.assertNotEquals(user1.getId(), result.get().getId());
        Assertions.assertNotEquals(user3.getId(), result.get().getId());
    }

    @Test
    public void shouldFindByUsername_WhenUserExists() {
        final String username = "findbyname";
        final User created = userDao.createUser("byname@test.com", username);

        final var result = userDao.findByUsername(username);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(created.getId(), result.get().getId());
        Assertions.assertEquals(username, result.get().getUsername());

        final Long countInDb =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM users WHERE username = ?", Long.class, username);
        Assertions.assertEquals(1L, countInDb, "User should exist in database with exact username");
    }

    @Test
    public void shouldFindByUsername_WhenUserNotFound() {
        final String nonExistentUsername = "notexist";

        final var result = userDao.findByUsername(nonExistentUsername);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void shouldFindByUsername_WithMultipleUsers_ReturnsExactUser() {
        final User user1 = userDao.createUser("u1@test.com", "alice");
        final User user2 = userDao.createUser("u2@test.com", "bob");
        final User user3 = userDao.createUser("u3@test.com", "charlie");

        final var result = userDao.findByUsername("bob");

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(user2.getId(), result.get().getId());
        Assertions.assertEquals("bob", result.get().getUsername());
        Assertions.assertNotEquals(user1.getId(), result.get().getId());
        Assertions.assertNotEquals(user3.getId(), result.get().getId());
    }
}
