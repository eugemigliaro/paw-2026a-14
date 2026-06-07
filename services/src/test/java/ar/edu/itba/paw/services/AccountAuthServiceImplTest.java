package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EmailActionRequest;
import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.exceptions.registration.*;
import ar.edu.itba.paw.models.types.EmailActionStatus;
import ar.edu.itba.paw.models.types.EmailActionType;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.persistence.EmailActionRequestDao;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.MailMode;
import ar.edu.itba.paw.services.mail.MailProperties;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class AccountAuthServiceImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-10T18:00:00Z");

    @Mock private UserDao userDao;
    @Mock private EmailActionRequestDao emailActionRequestDao;

    private RecordingMailDispatchService mailDispatchService;
    private PasswordEncoder passwordEncoder;
    private AccountAuthServiceImpl accountAuthService;

    @BeforeEach
    public void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        mailDispatchService = new RecordingMailDispatchService();
        passwordEncoder = new BCryptPasswordEncoder();
        accountAuthService =
                new AccountAuthServiceImpl(
                        userDao,
                        emailActionRequestDao,
                        new MailProperties(
                                MailMode.LOG,
                                "http://localhost:8080",
                                "no-reply@matchpoint.local",
                                "",
                                587,
                                "",
                                "",
                                false,
                                true,
                                24),
                        mailDispatchService,
                        messageSource(),
                        passwordEncoder,
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @AfterEach
    public void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    public void testRegisterCreatesUnverifiedAccountAndSendsVerificationMail() {
        final FakeUserDao fakeUserDao = new FakeUserDao();
        final FakeEmailActionRequestDao fakeEmailActionRequestDao = new FakeEmailActionRequestDao();
        accountAuthService = accountAuthService(fakeUserDao, fakeEmailActionRequestDao);

        final VerificationRequestResult result =
                accountAuthService.register(
                        new RegisterAccountRequest(
                                "New@Test.com",
                                "new_user",
                                "Jamie",
                                "Rivera",
                                "+1 555 123 4567",
                                "Password123!"));

        Assertions.assertEquals("new@test.com", result.getEmail());
        Assertions.assertEquals(FIXED_NOW.plusSeconds(24 * 3600L), result.getExpiresAt());
        final UserAccount createdAccount =
                fakeUserDao.findAccountByEmail("new@test.com").orElseThrow();
        Assertions.assertEquals("new_user", createdAccount.getUsername());
        Assertions.assertEquals("Jamie", createdAccount.getName());
        Assertions.assertEquals("Rivera", createdAccount.getLastName());
        Assertions.assertEquals("+1 555 123 4567", createdAccount.getPhone());
        Assertions.assertEquals(UserLanguages.ENGLISH, createdAccount.getPreferredLanguage());
        Assertions.assertEquals(UserRole.USER, createdAccount.getRole());
        Assertions.assertNull(createdAccount.getEmailVerifiedAt());
        Assertions.assertTrue(
                passwordEncoder.matches("Password123!", createdAccount.getPasswordHash()));
        Assertions.assertEquals(1, fakeEmailActionRequestDao.requests.size());
        Assertions.assertEquals(
                EmailActionType.ACCOUNT_VERIFICATION,
                fakeEmailActionRequestDao.requests.get(0).getActionType());
        Assertions.assertEquals(
                EmailActionStatus.PENDING, fakeEmailActionRequestDao.requests.get(0).getStatus());
        Assertions.assertEquals(List.of("account-verification"), mailDispatchService.actions);
        Assertions.assertEquals(List.of("new@test.com"), mailDispatchService.recipients);
        Assertions.assertTrue(mailDispatchService.urls.get(0).contains("/verifications/"));
        Assertions.assertEquals(Locale.ENGLISH, mailDispatchService.locales.get(0));
    }

    @Test
    public void testRegisterRejectsExistingVerifiedEmail() {
        Mockito.when(userDao.findAccountByEmail("player@test.com"))
                .thenReturn(
                        Optional.of(
                                new UserAccount(
                                        1L,
                                        "player@test.com",
                                        "player",
                                        null,
                                        null,
                                        null,
                                        null,
                                        "{bcrypt}hash",
                                        UserRole.USER,
                                        FIXED_NOW.minusSeconds(300),
                                        UserLanguages.ENGLISH)));

        Assertions.assertThrows(
                EmailTakenException.class,
                () ->
                        accountAuthService.register(
                                new RegisterAccountRequest(
                                        "player@test.com",
                                        "player",
                                        "Jamie",
                                        "Rivera",
                                        "+1 555 123 4567",
                                        "Password123!")));
    }

    @Test
    public void testRegisterRejectsExistingUnverifiedEmail() {
        Mockito.when(userDao.findAccountByEmail("pending@test.com"))
                .thenReturn(
                        Optional.of(
                                new UserAccount(
                                        2L,
                                        "pending@test.com",
                                        "pending",
                                        null,
                                        null,
                                        null,
                                        null,
                                        "{bcrypt}hash",
                                        UserRole.USER,
                                        null,
                                        UserLanguages.ENGLISH)));

        Assertions.assertThrows(
                EmailPendingVerificationException.class,
                () ->
                        accountAuthService.register(
                                new RegisterAccountRequest(
                                        "pending@test.com",
                                        "pending",
                                        "Jamie",
                                        "Rivera",
                                        "+1 555 123 4567",
                                        "Password123!")));
    }

    @Test
    public void testRegisterRejectsTakenUsername() {
        Mockito.when(userDao.findAccountByEmail("new@test.com")).thenReturn(Optional.empty());
        final User userWithTakenName = UserUtils.getUser(7L);
        Mockito.when(userDao.findByUsername("user7")).thenReturn(Optional.of(userWithTakenName));

        Assertions.assertThrows(
                UsernameTakenException.class,
                () ->
                        accountAuthService.register(
                                new RegisterAccountRequest(
                                        "new@test.com",
                                        "user7",
                                        "Jamie",
                                        "Rivera",
                                        "+1 555 123 4567",
                                        "Password123!")));
    }

    @Test
    public void testRegisterAllowsMissingPhone() {
        final UserAccount createdAccount =
                new UserAccount(
                        9L,
                        "new@test.com",
                        "new_user",
                        null,
                        null,
                        null,
                        null,
                        "{bcrypt}hash",
                        UserRole.USER,
                        null,
                        UserLanguages.ENGLISH);

        Mockito.when(userDao.findAccountByEmail("new@test.com")).thenReturn(Optional.empty());
        Mockito.when(userDao.findByUsername("new_user")).thenReturn(Optional.empty());
        Mockito.when(
                        userDao.createAccount(
                                ArgumentMatchers.eq("new@test.com"),
                                ArgumentMatchers.eq("new_user"),
                                ArgumentMatchers.eq("Jamie"),
                                ArgumentMatchers.eq("Rivera"),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.eq(UserLanguages.ENGLISH),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.eq(UserRole.USER),
                                ArgumentMatchers.isNull()))
                .thenReturn(createdAccount);
        Mockito.when(
                        emailActionRequestDao.create(
                                ArgumentMatchers.eq(EmailActionType.ACCOUNT_VERIFICATION),
                                ArgumentMatchers.eq("new@test.com"),
                                ArgumentMatchers.any(User.class),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.eq("{}"),
                                ArgumentMatchers.eq(FIXED_NOW.plusSeconds(24 * 3600L))))
                .thenReturn(
                        new EmailActionRequest(
                                20L,
                                EmailActionType.ACCOUNT_VERIFICATION,
                                "new@test.com",
                                UserUtils.getUser(9L),
                                "token-hash",
                                "{}",
                                EmailActionStatus.PENDING,
                                FIXED_NOW.plusSeconds(24 * 3600L),
                                null,
                                FIXED_NOW,
                                FIXED_NOW));
        final VerificationRequestResult result =
                accountAuthService.register(
                        new RegisterAccountRequest(
                                "new@test.com", "new_user", "Jamie", "Rivera", "", "Password123!"));

        Assertions.assertEquals("new@test.com", result.getEmail());
    }

    @Test
    public void testResendVerificationReturnsEmptyForVerifiedAccount() {
        Mockito.when(userDao.findAccountByEmail("verified@test.com"))
                .thenReturn(
                        Optional.of(
                                new UserAccount(
                                        3L,
                                        "verified@test.com",
                                        "verified",
                                        null,
                                        null,
                                        null,
                                        null,
                                        "{bcrypt}hash",
                                        UserRole.USER,
                                        FIXED_NOW.minusSeconds(120),
                                        UserLanguages.ENGLISH)));

        final Optional<VerificationRequestResult> result =
                accountAuthService.resendVerification("verified@test.com");

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testResendVerificationCreatesFreshTokenForUnverifiedAccount() {
        final UserAccount pendingAccount =
                new UserAccount(
                        4L,
                        "pending@test.com",
                        "pending",
                        null,
                        null,
                        null,
                        null,
                        "{bcrypt}hash",
                        UserRole.USER,
                        null,
                        UserLanguages.SPANISH);
        Mockito.when(userDao.findAccountByEmail("pending@test.com"))
                .thenReturn(Optional.of(pendingAccount));
        Mockito.when(
                        emailActionRequestDao.create(
                                ArgumentMatchers.eq(EmailActionType.ACCOUNT_VERIFICATION),
                                ArgumentMatchers.eq("pending@test.com"),
                                ArgumentMatchers.any(User.class),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.eq("{}"),
                                ArgumentMatchers.eq(FIXED_NOW.plusSeconds(24 * 3600L))))
                .thenReturn(
                        new EmailActionRequest(
                                21L,
                                EmailActionType.ACCOUNT_VERIFICATION,
                                "pending@test.com",
                                pendingAccount.toUser(),
                                "token-hash",
                                "{}",
                                EmailActionStatus.PENDING,
                                FIXED_NOW.plusSeconds(24 * 3600L),
                                null,
                                FIXED_NOW,
                                FIXED_NOW));
        final Optional<VerificationRequestResult> result =
                accountAuthService.resendVerification("pending@test.com");

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("pending@test.com", result.orElseThrow().getEmail());
        Assertions.assertEquals(List.of("account-verification"), mailDispatchService.actions);
        Assertions.assertEquals(List.of("pending@test.com"), mailDispatchService.recipients);
        Assertions.assertTrue(mailDispatchService.urls.get(0).contains("/verifications/"));
        Assertions.assertEquals(Locale.of("es"), mailDispatchService.locales.get(0));
    }

    @Test
    public void testConfirmVerificationMarksAccountAsVerified() {
        final FakeUserDao fakeUserDao = new FakeUserDao();
        final FakeEmailActionRequestDao fakeEmailActionRequestDao = new FakeEmailActionRequestDao();
        final UserAccount account =
                new UserAccount(
                        5L,
                        "verify@test.com",
                        "verified_user",
                        null,
                        null,
                        null,
                        null,
                        "{bcrypt}hash",
                        UserRole.USER,
                        null,
                        UserLanguages.DEFAULT_LANGUAGE);
        final EmailActionRequest request =
                new EmailActionRequest(
                        31L,
                        EmailActionType.ACCOUNT_VERIFICATION,
                        "verify@test.com",
                        UserUtils.getUser(5L),
                        hashToken("raw-token"),
                        "{}",
                        EmailActionStatus.PENDING,
                        FIXED_NOW.plusSeconds(24 * 3600L),
                        null,
                        FIXED_NOW,
                        FIXED_NOW);
        fakeUserDao.accounts.add(account);
        fakeEmailActionRequestDao.requests.add(request);
        accountAuthService = accountAuthService(fakeUserDao, fakeEmailActionRequestDao);

        final VerificationConfirmationResult result =
                accountAuthService.confirmVerification("raw-token");

        Assertions.assertEquals(5L, result.getUserId());
        Assertions.assertTrue(result.getAccount().isPresent());
        Assertions.assertEquals(5L, result.getAccount().orElseThrow().getId());
        Assertions.assertEquals(EmailActionStatus.COMPLETED, request.getStatus());
        Assertions.assertEquals(FIXED_NOW, request.getConsumedAt());
        Assertions.assertEquals(FIXED_NOW, account.getEmailVerifiedAt());
    }

    @Test
    public void testRequestPasswordResetCreatesPendingRequestForVerifiedAccount() {
        final UserAccount account =
                new UserAccount(
                        6L,
                        "legacy@test.com",
                        "legacy",
                        null,
                        null,
                        null,
                        null,
                        null,
                        UserRole.USER,
                        FIXED_NOW,
                        UserLanguages.SPANISH);

        Mockito.when(userDao.findAccountByEmail("legacy@test.com"))
                .thenReturn(Optional.of(account));
        Mockito.when(
                        emailActionRequestDao.create(
                                ArgumentMatchers.eq(EmailActionType.PASSWORD_RESET),
                                ArgumentMatchers.eq("legacy@test.com"),
                                ArgumentMatchers.any(User.class),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.eq("{}"),
                                ArgumentMatchers.eq(FIXED_NOW.plusSeconds(24 * 3600L))))
                .thenReturn(
                        new EmailActionRequest(
                                40L,
                                EmailActionType.PASSWORD_RESET,
                                "legacy@test.com",
                                UserUtils.getUser(6L),
                                "token-hash",
                                "{}",
                                EmailActionStatus.PENDING,
                                FIXED_NOW.plusSeconds(24 * 3600L),
                                null,
                                FIXED_NOW,
                                FIXED_NOW));
        final Optional<VerificationRequestResult> result =
                accountAuthService.requestPasswordReset("legacy@test.com");

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(List.of("password-reset"), mailDispatchService.actions);
        Assertions.assertEquals(List.of("legacy@test.com"), mailDispatchService.recipients);
        Assertions.assertTrue(mailDispatchService.urls.get(0).contains("/password-reset/"));
        Assertions.assertEquals(Locale.of("es"), mailDispatchService.locales.get(0));
    }

    @Test
    public void testRequestPasswordResetReturnsEmptyForUnverifiedAccount() {
        Mockito.when(userDao.findAccountByEmail("pending@test.com"))
                .thenReturn(
                        Optional.of(
                                new UserAccount(
                                        7L,
                                        "pending@test.com",
                                        "pending",
                                        null,
                                        null,
                                        null,
                                        null,
                                        "{bcrypt}hash",
                                        UserRole.USER,
                                        null,
                                        UserLanguages.DEFAULT_LANGUAGE)));

        final Optional<VerificationRequestResult> result =
                accountAuthService.requestPasswordReset("pending@test.com");

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetPasswordResetPreviewReturnsEmailAndExpiry() {
        final EmailActionRequest request =
                new EmailActionRequest(
                        50L,
                        EmailActionType.PASSWORD_RESET,
                        "player@test.com",
                        UserUtils.getUser(8L),
                        "token-hash",
                        "{}",
                        EmailActionStatus.PENDING,
                        FIXED_NOW.plusSeconds(24 * 3600L),
                        null,
                        FIXED_NOW,
                        FIXED_NOW);

        Mockito.when(emailActionRequestDao.findByTokenHash(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(request));
        Mockito.when(userDao.findAccountById(8L))
                .thenReturn(
                        Optional.of(
                                new UserAccount(
                                        8L,
                                        "player@test.com",
                                        "player",
                                        null,
                                        null,
                                        null,
                                        null,
                                        "{bcrypt}hash",
                                        UserRole.USER,
                                        FIXED_NOW.minusSeconds(10),
                                        UserLanguages.DEFAULT_LANGUAGE)));

        final PasswordResetPreview preview =
                accountAuthService.getPasswordResetPreview("raw-token");

        Assertions.assertEquals("player@test.com", preview.getEmail());
        Assertions.assertEquals(FIXED_NOW.plusSeconds(24 * 3600L), preview.getExpiresAt());
    }

    @Test
    public void testResetPasswordUpdatesHashAndCompletesRequest() {
        final FakeUserDao fakeUserDao = new FakeUserDao();
        final FakeEmailActionRequestDao fakeEmailActionRequestDao = new FakeEmailActionRequestDao();
        final UserAccount account =
                new UserAccount(
                        9L,
                        "player@test.com",
                        "player",
                        null,
                        null,
                        null,
                        null,
                        null,
                        UserRole.USER,
                        FIXED_NOW.minusSeconds(60),
                        UserLanguages.DEFAULT_LANGUAGE);
        final EmailActionRequest request =
                new EmailActionRequest(
                        60L,
                        EmailActionType.PASSWORD_RESET,
                        "player@test.com",
                        UserUtils.getUser(9L),
                        hashToken("raw-token"),
                        "{}",
                        EmailActionStatus.PENDING,
                        FIXED_NOW.plusSeconds(24 * 3600L),
                        null,
                        FIXED_NOW,
                        FIXED_NOW);
        fakeUserDao.accounts.add(account);
        fakeEmailActionRequestDao.requests.add(request);
        accountAuthService = accountAuthService(fakeUserDao, fakeEmailActionRequestDao);

        final VerificationConfirmationResult result =
                accountAuthService.resetPassword("raw-token", "EvenBetter123!");

        Assertions.assertEquals(9L, result.getUserId());
        Assertions.assertEquals(EmailActionStatus.COMPLETED, request.getStatus());
        Assertions.assertEquals(FIXED_NOW, request.getConsumedAt());
        Assertions.assertTrue(passwordEncoder.matches("EvenBetter123!", account.getPasswordHash()));
    }

    private AccountAuthServiceImpl accountAuthService(
            final UserDao userDao, final EmailActionRequestDao emailActionRequestDao) {
        return new AccountAuthServiceImpl(
                userDao,
                emailActionRequestDao,
                new MailProperties(
                        MailMode.LOG,
                        "http://localhost:8080",
                        "no-reply@matchpoint.local",
                        "",
                        587,
                        "",
                        "",
                        false,
                        true,
                        24),
                mailDispatchService,
                messageSource(),
                passwordEncoder,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    private static MessageSource messageSource() {
        final StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage(
                "auth.registration.error.emailTaken",
                Locale.ENGLISH,
                "An account with that email already exists.");
        messageSource.addMessage(
                "auth.registration.error.emailPending",
                Locale.ENGLISH,
                "That email is already registered but still pending verification.");
        messageSource.addMessage(
                "auth.registration.error.usernameTaken",
                Locale.ENGLISH,
                "That username is already in use.");
        messageSource.addMessage(
                "auth.registration.error.usernameInvalid",
                Locale.ENGLISH,
                "Use 3 to 50 lowercase letters, numbers, or underscores for your username.");
        messageSource.addMessage(
                "auth.registration.error.nameInvalid", Locale.ENGLISH, "Enter a valid first name.");
        messageSource.addMessage(
                "auth.registration.error.lastNameInvalid",
                Locale.ENGLISH,
                "Enter a valid last name.");
        messageSource.addMessage(
                "auth.registration.error.phoneInvalid",
                Locale.ENGLISH,
                "Enter a valid phone number.");
        messageSource.addMessage(
                "auth.registration.error.passwordInvalid",
                Locale.ENGLISH,
                "Use a password between 8 and 72 characters.");
        messageSource.addMessage(
                "verification.preview.account.title",
                Locale.ENGLISH,
                "Verify your Match Point account");
        messageSource.addMessage(
                "verification.preview.account.summary",
                Locale.ENGLISH,
                "Use this one-time confirmation to activate your account and sign in.");
        messageSource.addMessage(
                "verification.preview.account.confirm", Locale.ENGLISH, "Verify account");
        messageSource.addMessage(
                "verification.message.notFound",
                Locale.ENGLISH,
                "That verification link is invalid or no longer exists.");
        messageSource.addMessage(
                "verification.message.alreadyUsed",
                Locale.ENGLISH,
                "That verification link was already used.");
        messageSource.addMessage(
                "verification.message.expired",
                Locale.ENGLISH,
                "That verification link has expired.");
        messageSource.addMessage(
                "verification.message.accountVerified",
                Locale.ENGLISH,
                "Your email is now verified. You can sign in.");
        messageSource.addMessage(
                "verification.message.accountUnavailable",
                Locale.ENGLISH,
                "This account verification can no longer be completed.");
        messageSource.addMessage("passwordReset.mail.title", Locale.ENGLISH, "Reset your password");
        messageSource.addMessage(
                "passwordReset.mail.summary",
                Locale.ENGLISH,
                "Use this secure link to choose a new password for your Match Point account.");
        messageSource.addMessage(
                "passwordReset.preview.title", Locale.ENGLISH, "Choose a new password");
        messageSource.addMessage(
                "passwordReset.preview.summary",
                Locale.ENGLISH,
                "Use this secure link to set a new password for your account.");
        messageSource.addMessage(
                "passwordReset.message.unavailable",
                Locale.ENGLISH,
                "This password reset link can no longer be used.");
        messageSource.addMessage(
                "passwordReset.message.completed",
                Locale.ENGLISH,
                "Your password was updated successfully.");
        return messageSource;
    }

    private static String hashToken(final String rawToken) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static class RecordingMailDispatchService implements MailDispatchService {

        private final List<String> actions = new ArrayList<>();
        private final List<String> recipients = new ArrayList<>();
        private final List<String> urls = new ArrayList<>();
        private final List<Locale> locales = new ArrayList<>();

        @Override
        public void sendAccountVerification(
                final UserAccount account,
                final String confirmationUrl,
                final Instant expiresAt,
                final Locale locale) {
            actions.add("account-verification");
            recipients.add(account.getEmail());
            urls.add(confirmationUrl);
            locales.add(locale);
        }

        @Override
        public void sendPasswordReset(
                final UserAccount account,
                final String resetUrl,
                final Instant expiresAt,
                final Locale locale) {
            actions.add("password-reset");
            recipients.add(account.getEmail());
            urls.add(resetUrl);
            locales.add(locale);
        }
    }

    private static class FakeUserDao implements UserDao {

        private final List<UserAccount> accounts = new ArrayList<>();
        private long nextAccountId = 9L;

        @Override
        public User createUser(final String email, final String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserAccount createAccount(
                final String email,
                final String username,
                final String name,
                final String lastName,
                final String phone,
                final String preferredLanguage,
                final String passwordHash,
                final UserRole role,
                final Instant emailVerifiedAt) {
            final UserAccount account =
                    new UserAccount(
                            nextAccountId++,
                            email,
                            username,
                            name,
                            lastName,
                            phone,
                            null,
                            passwordHash,
                            role,
                            emailVerifiedAt,
                            preferredLanguage);
            accounts.add(account);
            return account;
        }

        @Override
        public Optional<User> findByEmail(final String email) {
            return findAccountByEmail(email).map(UserAccount::toUser);
        }

        @Override
        public Optional<UserAccount> findAccountByEmail(final String email) {
            return accounts.stream()
                    .filter(account -> account.getEmail().equals(email))
                    .findFirst();
        }

        @Override
        public Optional<User> findById(final Long id) {
            return findAccountById(id).map(UserAccount::toUser);
        }

        @Override
        public List<User> findByIds(final Collection<Long> ids) {
            return accounts.stream()
                    .filter(account -> ids.contains(account.getId()))
                    .map(UserAccount::toUser)
                    .toList();
        }

        @Override
        public Optional<UserAccount> findAccountById(final Long id) {
            return accounts.stream().filter(account -> account.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<User> findByUsername(final String username) {
            return accounts.stream()
                    .filter(account -> account.getUsername().equals(username))
                    .findFirst()
                    .map(UserAccount::toUser);
        }

        @Override
        public void updateProfile(
                final Long id,
                final String username,
                final String name,
                final String lastName,
                final String phone,
                final ImageMetadata profileImageMetadata) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateProfileImage(final Long id, final ImageMetadata profileImageMetadata) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updatePasswordHash(final Long id, final String passwordHash) {
            findAccountById(id).orElseThrow().setPasswordHash(passwordHash);
        }

        @Override
        public void markEmailVerified(final Long id, final Instant emailVerifiedAt) {
            findAccountById(id).orElseThrow().setEmailVerifiedAt(emailVerifiedAt);
        }

        @Override
        public void updatePreferredLanguage(final Long id, final String preferredLanguage) {
            findAccountById(id).orElseThrow().setPreferredLanguage(preferredLanguage);
        }
    }

    private static class FakeEmailActionRequestDao implements EmailActionRequestDao {

        private final List<EmailActionRequest> requests = new ArrayList<>();
        private long nextRequestId = 20L;

        @Override
        public EmailActionRequest create(
                final EmailActionType actionType,
                final String email,
                final User user,
                final String tokenHash,
                final String payloadJson,
                final Instant expiresAt) {
            final EmailActionRequest request =
                    new EmailActionRequest(
                            nextRequestId++,
                            actionType,
                            email,
                            user,
                            tokenHash,
                            payloadJson,
                            EmailActionStatus.PENDING,
                            expiresAt,
                            null,
                            FIXED_NOW,
                            FIXED_NOW);
            requests.add(request);
            return request;
        }

        @Override
        public Optional<EmailActionRequest> findByTokenHash(final String tokenHash) {
            return requests.stream()
                    .filter(request -> request.getTokenHash().equals(tokenHash))
                    .filter(request -> request.getStatus() == EmailActionStatus.PENDING)
                    .findFirst();
        }

        @Override
        public Optional<EmailActionRequest> findByTokenHashForUpdate(final String tokenHash) {
            return findByTokenHash(tokenHash);
        }

        @Override
        public void updateStatus(
                final Long id,
                final EmailActionStatus status,
                final User user,
                final Instant consumedAt) {
            final EmailActionRequest request =
                    requests.stream()
                            .filter(candidate -> candidate.getId().equals(id))
                            .findFirst()
                            .orElseThrow();
            request.setStatus(status);
            request.setUser(user);
            request.setConsumedAt(consumedAt);
            request.setUpdatedAt(consumedAt);
        }

        @Override
        public void expirePendingByEmailAndActionType(
                final EmailActionType actionType, final String email, final Instant consumedAt) {
            requests.stream()
                    .filter(request -> request.getActionType() == actionType)
                    .filter(request -> request.getEmail().equals(email))
                    .filter(request -> request.getStatus() == EmailActionStatus.PENDING)
                    .forEach(
                            request -> {
                                request.setStatus(EmailActionStatus.EXPIRED);
                                request.setConsumedAt(consumedAt);
                                request.setUpdatedAt(consumedAt);
                            });
        }
    }
}
