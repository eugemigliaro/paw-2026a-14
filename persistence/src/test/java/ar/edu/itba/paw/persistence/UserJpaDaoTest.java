package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.UserRole;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.Assertions;
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
public class UserJpaDaoTest {

    private static final String PASSWORD_HASH = "{bcrypt}encoded";
    private static final Instant VERIFIED_AT = Instant.parse("2026-04-17T18:00:00Z");

    @Autowired private UserDao userDao;
    @Autowired private ImageDao imageDao;

    @PersistenceContext private EntityManager entityManager;

    @Test
    public void testCreateUserWhenUserDoesNotExist() {
        final String email = "test@example.com";
        final String username = "testuser";

        final User user = userDao.createUser(email, username);

        Assertions.assertNotNull(user);
        Assertions.assertEquals(email, user.getEmail());
        Assertions.assertEquals(username, user.getUsername());
        Assertions.assertEquals(1, countUsers());

        final UserAccount inDb = findPersistedUser(user.getId());
        Assertions.assertEquals(email, inDb.getEmail());
        Assertions.assertEquals(username, inDb.getUsername());
        Assertions.assertNull(inDb.getName());
        Assertions.assertNull(inDb.getLastName());
        Assertions.assertNull(inDb.getPhone());
        Assertions.assertNull(inDb.getPasswordHash());
        Assertions.assertEquals(UserRole.USER, inDb.getRole());
        Assertions.assertNull(inDb.getEmailVerifiedAt());
        Assertions.assertEquals(UserLanguages.DEFAULT_LANGUAGE, inDb.getPreferredLanguage());
        Assertions.assertNotNull(inDb.getCreatedAt());
        Assertions.assertNotNull(inDb.getUpdatedAt());
    }

    @Test
    public void testCreateAccountStoresPasswordRoleAndVerificationState() {
        final String email = "auth@test.com";

        final UserAccount account =
                userDao.createAccount(
                        email,
                        "auth_user",
                        "Jamie",
                        "Rivera",
                        "+1 555 123 4567",
                        UserLanguages.SPANISH,
                        PASSWORD_HASH,
                        UserRole.ADMIN_MOD,
                        VERIFIED_AT);

        Assertions.assertEquals(1, countUsers());

        final UserAccount persisted =
                userDao.findAccountByEmail(email).orElseThrow(AssertionError::new);
        Assertions.assertEquals(account.getId(), persisted.getId());
        Assertions.assertEquals("Jamie", persisted.getName());
        Assertions.assertEquals("Rivera", persisted.getLastName());
        Assertions.assertEquals("+1 555 123 4567", persisted.getPhone());
        Assertions.assertEquals(PASSWORD_HASH, persisted.getPasswordHash());
        Assertions.assertEquals(UserRole.ADMIN_MOD, persisted.getRole());
        Assertions.assertEquals(VERIFIED_AT, persisted.getEmailVerifiedAt());
        Assertions.assertEquals(UserLanguages.SPANISH, persisted.getPreferredLanguage());

        final UserAccount inDb = findPersistedUser(account.getId());
        Assertions.assertEquals(email, inDb.getEmail());
        Assertions.assertEquals("auth_user", inDb.getUsername());
        Assertions.assertEquals("Jamie", inDb.getName());
        Assertions.assertEquals("Rivera", inDb.getLastName());
        Assertions.assertEquals("+1 555 123 4567", inDb.getPhone());
        Assertions.assertEquals(PASSWORD_HASH, inDb.getPasswordHash());
        Assertions.assertEquals(UserRole.ADMIN_MOD, inDb.getRole());
        Assertions.assertEquals(VERIFIED_AT, inDb.getEmailVerifiedAt());
        Assertions.assertEquals(UserLanguages.SPANISH, inDb.getPreferredLanguage());
    }

    @Test
    public void testCreateAccountSupportsUnverifiedUsersWithoutLegacyDefaults() {
        final String email = "pending@test.com";

        final UserAccount account =
                userDao.createAccount(
                        email,
                        "pending_user",
                        "Jamie",
                        "Rivera",
                        "+1 555 123 4567",
                        UserLanguages.DEFAULT_LANGUAGE,
                        PASSWORD_HASH,
                        UserRole.USER,
                        null);

        Assertions.assertEquals(1, countUsers());

        final UserAccount persisted =
                userDao.findAccountByEmail(email).orElseThrow(AssertionError::new);
        Assertions.assertNull(persisted.getEmailVerifiedAt());
        Assertions.assertEquals(UserRole.USER, persisted.getRole());
        Assertions.assertEquals(UserLanguages.DEFAULT_LANGUAGE, persisted.getPreferredLanguage());

        final UserAccount inDb = findPersistedUser(account.getId());
        Assertions.assertEquals(email, inDb.getEmail());
        Assertions.assertEquals("pending_user", inDb.getUsername());
        Assertions.assertEquals(PASSWORD_HASH, inDb.getPasswordHash());
        Assertions.assertNull(inDb.getEmailVerifiedAt());
        Assertions.assertEquals(UserRole.USER, inDb.getRole());
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
                        VERIFIED_AT);

        userDao.updatePasswordHash(account.getId(), "{bcrypt}newhash");

        Assertions.assertEquals(1, countUsers());
        final UserAccount persisted = findPersistedUser(account.getId());
        Assertions.assertEquals("{bcrypt}newhash", persisted.getPasswordHash());
        Assertions.assertEquals("reset@test.com", persisted.getEmail());
        Assertions.assertEquals("reset_user", persisted.getUsername());
        Assertions.assertEquals(UserRole.USER, persisted.getRole());
        Assertions.assertEquals(VERIFIED_AT, persisted.getEmailVerifiedAt());
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
                        PASSWORD_HASH,
                        UserRole.USER,
                        null);
        final Instant verifiedAt = Instant.parse("2026-04-18T12:00:00Z");

        userDao.markEmailVerified(account.getId(), verifiedAt);

        Assertions.assertEquals(1, countUsers());
        final UserAccount persisted = findPersistedUser(account.getId());
        Assertions.assertEquals(verifiedAt, persisted.getEmailVerifiedAt());
        Assertions.assertEquals(PASSWORD_HASH, persisted.getPasswordHash());
        Assertions.assertEquals("verify@test.com", persisted.getEmail());
        Assertions.assertEquals("verify_user", persisted.getUsername());
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
                        PASSWORD_HASH,
                        UserRole.USER,
                        VERIFIED_AT);

        userDao.updateProfile(account.getId(), "updated_user", "Taylor", "Morgan", null, null);

        Assertions.assertEquals(1, countUsers());

        final User persisted = userDao.findById(account.getId()).orElseThrow(AssertionError::new);
        Assertions.assertEquals("profile@test.com", persisted.getEmail());
        Assertions.assertEquals("updated_user", persisted.getUsername());
        Assertions.assertEquals("Taylor", persisted.getName());
        Assertions.assertEquals("Morgan", persisted.getLastName());
        Assertions.assertNull(persisted.getPhone());
        Assertions.assertEquals(UserLanguages.DEFAULT_LANGUAGE, persisted.getPreferredLanguage());

        final UserAccount inDb = findPersistedUser(account.getId());
        Assertions.assertEquals("profile@test.com", inDb.getEmail());
        Assertions.assertEquals("updated_user", inDb.getUsername());
        Assertions.assertEquals("Taylor", inDb.getName());
        Assertions.assertEquals("Morgan", inDb.getLastName());
        Assertions.assertNull(inDb.getPhone());
        Assertions.assertNull(inDb.getProfileImageId());
        Assertions.assertEquals(PASSWORD_HASH, inDb.getPasswordHash());
        Assertions.assertEquals(VERIFIED_AT, inDb.getEmailVerifiedAt());
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
                        PASSWORD_HASH,
                        UserRole.USER,
                        VERIFIED_AT);

        userDao.updatePreferredLanguage(account.getId(), UserLanguages.SPANISH);

        Assertions.assertEquals(1, countUsers());

        final User persisted = userDao.findById(account.getId()).orElseThrow(AssertionError::new);
        Assertions.assertEquals(UserLanguages.SPANISH, persisted.getPreferredLanguage());

        final UserAccount inDb = findPersistedUser(account.getId());
        Assertions.assertEquals(UserLanguages.SPANISH, inDb.getPreferredLanguage());
        Assertions.assertEquals("locale@test.com", inDb.getEmail());
        Assertions.assertEquals("locale_user", inDb.getUsername());
    }

    @Test
    public void testFindByIdsReturnsMatchingUsers() {
        final User first = userDao.createUser("first@test.com", "first_user");
        final User second = userDao.createUser("second@test.com", "second_user");
        final User third = userDao.createUser("third@test.com", "third_user");
        flushAndClear();

        final List<User> result = userDao.findByIds(List.of(first.getId(), second.getId()));

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.stream().anyMatch(user -> user.getId().equals(first.getId())));
        Assertions.assertTrue(
                result.stream().anyMatch(user -> user.getId().equals(second.getId())));
        Assertions.assertTrue(
                result.stream().noneMatch(user -> user.getId().equals(third.getId())));
        Assertions.assertEquals(3, countUsers());
        assertPersistedUser(first.getId(), "first@test.com", "first_user");
        assertPersistedUser(second.getId(), "second@test.com", "second_user");
        assertPersistedUser(third.getId(), "third@test.com", "third_user");
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
                        PASSWORD_HASH,
                        UserRole.USER,
                        VERIFIED_AT);
        final Long imageId =
                imageDao.create("image/png", 4L, new ByteArrayInputStream(new byte[] {1, 2, 3, 4}));

        userDao.updateProfileImage(account.getId(), imageId);

        Assertions.assertEquals(1, countUsers());

        final User persisted = userDao.findById(account.getId()).orElseThrow(AssertionError::new);
        Assertions.assertEquals(imageId, persisted.getProfileImageId());

        final UserAccount inDb = findPersistedUser(account.getId());
        Assertions.assertEquals(imageId, inDb.getProfileImageId());
        Assertions.assertEquals("avatar@test.com", inDb.getEmail());
        Assertions.assertEquals("avatar_user", inDb.getUsername());
        Assertions.assertEquals(PASSWORD_HASH, inDb.getPasswordHash());
    }

    @Test
    public void testLegacyCreateUserMarksUserAsUnverifiedByDefault() {
        final String email = "legacy@test.com";

        final User user = userDao.createUser(email, "legacy_user");

        Assertions.assertEquals(1, countUsers());

        final UserAccount persisted =
                userDao.findAccountById(user.getId()).orElseThrow(AssertionError::new);
        Assertions.assertEquals(UserRole.USER, persisted.getRole());
        Assertions.assertNull(persisted.getEmailVerifiedAt());
        Assertions.assertNull(persisted.getPasswordHash());
        Assertions.assertEquals(UserLanguages.DEFAULT_LANGUAGE, persisted.getPreferredLanguage());

        final UserAccount inDb = findPersistedUser(user.getId());
        Assertions.assertEquals(email, inDb.getEmail());
        Assertions.assertEquals("legacy_user", inDb.getUsername());
        Assertions.assertEquals(UserRole.USER, inDb.getRole());
        Assertions.assertNull(inDb.getEmailVerifiedAt());
        Assertions.assertNull(inDb.getPasswordHash());
        Assertions.assertEquals(UserLanguages.DEFAULT_LANGUAGE, inDb.getPreferredLanguage());
    }

    @Test
    public void shouldFindByEmail_WhenUserExists() {
        final String email = "findme@test.com";
        final User created = userDao.createUser(email, "findme_user");
        flushAndClear();

        final var result = userDao.findByEmail(email);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(created.getId(), result.get().getId());
        Assertions.assertEquals(email, result.get().getEmail());
        Assertions.assertEquals(1, countUsers());
        assertPersistedUser(created.getId(), email, "findme_user");
    }

    @Test
    public void shouldFindByEmail_WhenUserNotFound() {
        final String nonExistentEmail = "notfound@test.com";

        final var result = userDao.findByEmail(nonExistentEmail);

        Assertions.assertTrue(result.isEmpty());
        Assertions.assertEquals(0, countUsers());
    }

    @Test
    public void shouldFindById_WhenUserExists() {
        final User created = userDao.createUser("byid@test.com", "byid_user");
        flushAndClear();

        final var result = userDao.findById(created.getId());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(created.getId(), result.get().getId());
        Assertions.assertEquals(created.getEmail(), result.get().getEmail());
        Assertions.assertEquals(created.getUsername(), result.get().getUsername());
        Assertions.assertEquals(1, countUsers());
        assertPersistedUser(created.getId(), "byid@test.com", "byid_user");
    }

    @Test
    public void shouldFindById_WhenUserNotFound() {
        final Long nonExistentId = 999999L;

        final var result = userDao.findById(nonExistentId);

        Assertions.assertTrue(result.isEmpty());
        Assertions.assertEquals(0, countUsers());
    }

    @Test
    public void shouldFindById_WithMultipleUsers_ReturnsExactUser() {
        final User user1 = userDao.createUser("user1@test.com", "user1");
        final User user2 = userDao.createUser("user2@test.com", "user2");
        final User user3 = userDao.createUser("user3@test.com", "user3");
        flushAndClear();

        final var result = userDao.findById(user2.getId());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(user2.getId(), result.get().getId());
        Assertions.assertEquals("user2@test.com", result.get().getEmail());
        Assertions.assertNotEquals(user1.getId(), result.get().getId());
        Assertions.assertNotEquals(user3.getId(), result.get().getId());
        Assertions.assertEquals(3, countUsers());
        assertPersistedUser(user1.getId(), "user1@test.com", "user1");
        assertPersistedUser(user2.getId(), "user2@test.com", "user2");
        assertPersistedUser(user3.getId(), "user3@test.com", "user3");
    }

    @Test
    public void shouldFindByUsername_WhenUserExists() {
        final String username = "findbyname";
        final User created = userDao.createUser("byname@test.com", username);
        flushAndClear();

        final var result = userDao.findByUsername(username);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(created.getId(), result.get().getId());
        Assertions.assertEquals(username, result.get().getUsername());
        Assertions.assertEquals(1, countUsers());
        assertPersistedUser(created.getId(), "byname@test.com", username);
    }

    @Test
    public void shouldFindByUsername_WhenUserNotFound() {
        final String nonExistentUsername = "notexist";

        final var result = userDao.findByUsername(nonExistentUsername);

        Assertions.assertTrue(result.isEmpty());
        Assertions.assertEquals(0, countUsers());
    }

    @Test
    public void shouldFindByUsername_WithMultipleUsers_ReturnsExactUser() {
        final User user1 = userDao.createUser("u1@test.com", "alice");
        final User user2 = userDao.createUser("u2@test.com", "bob");
        final User user3 = userDao.createUser("u3@test.com", "charlie");
        flushAndClear();

        final var result = userDao.findByUsername("bob");

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(user2.getId(), result.get().getId());
        Assertions.assertEquals("bob", result.get().getUsername());
        Assertions.assertNotEquals(user1.getId(), result.get().getId());
        Assertions.assertNotEquals(user3.getId(), result.get().getId());
        Assertions.assertEquals(3, countUsers());
        assertPersistedUser(user1.getId(), "u1@test.com", "alice");
        assertPersistedUser(user2.getId(), "u2@test.com", "bob");
        assertPersistedUser(user3.getId(), "u3@test.com", "charlie");
    }

    private long countUsers() {
        flushAndClear();
        return entityManager
                .createQuery("SELECT COUNT(userAccount) FROM UserAccount userAccount", Long.class)
                .getSingleResult();
    }

    private void assertPersistedUser(final Long userId, final String email, final String username) {
        final UserAccount inDb = findPersistedUser(userId);
        Assertions.assertEquals(email, inDb.getEmail());
        Assertions.assertEquals(username, inDb.getUsername());
    }

    private UserAccount findPersistedUser(final Long userId) {
        flushAndClear();
        final UserAccount inDb = entityManager.find(UserAccount.class, userId);
        Assertions.assertNotNull(inDb, "User row must exist in the database");
        return inDb;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
