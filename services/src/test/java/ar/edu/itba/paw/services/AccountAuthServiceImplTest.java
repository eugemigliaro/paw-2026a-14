package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EmailActionRequest;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.EmailActionStatus;
import ar.edu.itba.paw.models.types.EmailActionType;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.persistence.EmailActionRequestDao;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.exceptions.AccountRegistrationException;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.MailMode;
import ar.edu.itba.paw.services.mail.MailProperties;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import ar.edu.itba.paw.services.mail.VerificationMailTemplateData;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
    @Mock private MailDispatchService mailDispatchService;
    @Mock private ThymeleafMailTemplateRenderer templateRenderer;

    private PasswordEncoder passwordEncoder;
    private AccountAuthServiceImpl accountAuthService;

    @BeforeEach
    public void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
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
                        templateRenderer,
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
        final AtomicReference<String> capturedPasswordHash = new AtomicReference<>();
        final AtomicReference<VerificationMailTemplateData> capturedTemplateData =
                new AtomicReference<>();
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
                                ArgumentMatchers.eq("+1 555 123 4567"),
                                ArgumentMatchers.eq(UserLanguages.ENGLISH),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.eq(UserRole.USER),
                                ArgumentMatchers.isNull()))
                .thenAnswer(
                        invocation -> {
                            capturedPasswordHash.set(invocation.getArgument(6));
                            return createdAccount;
                        });
        Mockito.when(
                        emailActionRequestDao.create(
                                ArgumentMatchers.eq(EmailActionType.ACCOUNT_VERIFICATION),
                                ArgumentMatchers.eq("new@test.com"),
                                ArgumentMatchers.eq(9L),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.eq("{}"),
                                ArgumentMatchers.eq(FIXED_NOW.plusSeconds(24 * 3600L))))
                .thenReturn(
                        new EmailActionRequest(
                                20L,
                                EmailActionType.ACCOUNT_VERIFICATION,
                                "new@test.com",
                                9L,
                                "token-hash",
                                "{}",
                                EmailActionStatus.PENDING,
                                FIXED_NOW.plusSeconds(24 * 3600L),
                                null,
                                FIXED_NOW,
                                FIXED_NOW));
        Mockito.when(templateRenderer.renderActionMail(ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            capturedTemplateData.set(invocation.getArgument(0));
                            return new MailContent("subject", "<p>html</p>", "text");
                        });

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
        Assertions.assertNotNull(capturedPasswordHash.get());
        Assertions.assertTrue(passwordEncoder.matches("Password123!", capturedPasswordHash.get()));
        Assertions.assertNotNull(capturedTemplateData.get());
        Assertions.assertTrue(
                capturedTemplateData.get().getConfirmationUrl().contains("/verifications/"));
        Assertions.assertEquals(Locale.ENGLISH, capturedTemplateData.get().getLocale());
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

        final AccountRegistrationException exception =
                Assertions.assertThrows(
                        AccountRegistrationException.class,
                        () ->
                                accountAuthService.register(
                                        new RegisterAccountRequest(
                                                "player@test.com",
                                                "player",
                                                "Jamie",
                                                "Rivera",
                                                "+1 555 123 4567",
                                                "Password123!")));

        Assertions.assertEquals("email_taken", exception.getCode());
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

        final AccountRegistrationException exception =
                Assertions.assertThrows(
                        AccountRegistrationException.class,
                        () ->
                                accountAuthService.register(
                                        new RegisterAccountRequest(
                                                "pending@test.com",
                                                "pending",
                                                "Jamie",
                                                "Rivera",
                                                "+1 555 123 4567",
                                                "Password123!")));

        Assertions.assertEquals("email_pending_verification", exception.getCode());
    }

    @Test
    public void testRegisterRejectsTakenUsername() {
        Mockito.when(userDao.findAccountByEmail("new@test.com")).thenReturn(Optional.empty());
        Mockito.when(userDao.findByUsername("taken_name"))
                .thenReturn(
                        Optional.of(
                                new ar.edu.itba.paw.models.User(
                                        7L, "other@test.com", "taken_name")));

        final AccountRegistrationException exception =
                Assertions.assertThrows(
                        AccountRegistrationException.class,
                        () ->
                                accountAuthService.register(
                                        new RegisterAccountRequest(
                                                "new@test.com",
                                                "taken_name",
                                                "Jamie",
                                                "Rivera",
                                                "+1 555 123 4567",
                                                "Password123!")));

        Assertions.assertEquals("username_taken", exception.getCode());
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
                                ArgumentMatchers.eq(9L),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.eq("{}"),
                                ArgumentMatchers.eq(FIXED_NOW.plusSeconds(24 * 3600L))))
                .thenReturn(
                        new EmailActionRequest(
                                20L,
                                EmailActionType.ACCOUNT_VERIFICATION,
                                "new@test.com",
                                9L,
                                "token-hash",
                                "{}",
                                EmailActionStatus.PENDING,
                                FIXED_NOW.plusSeconds(24 * 3600L),
                                null,
                                FIXED_NOW,
                                FIXED_NOW));
        Mockito.when(templateRenderer.renderActionMail(ArgumentMatchers.any()))
                .thenReturn(new MailContent("subject", "<p>html</p>", "text"));

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
        final AtomicReference<VerificationMailTemplateData> capturedTemplateData =
                new AtomicReference<>();
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
                                ArgumentMatchers.eq(4L),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.eq("{}"),
                                ArgumentMatchers.eq(FIXED_NOW.plusSeconds(24 * 3600L))))
                .thenReturn(
                        new EmailActionRequest(
                                21L,
                                EmailActionType.ACCOUNT_VERIFICATION,
                                "pending@test.com",
                                4L,
                                "token-hash",
                                "{}",
                                EmailActionStatus.PENDING,
                                FIXED_NOW.plusSeconds(24 * 3600L),
                                null,
                                FIXED_NOW,
                                FIXED_NOW));
        Mockito.when(templateRenderer.renderActionMail(ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            capturedTemplateData.set(invocation.getArgument(0));
                            return new MailContent("subject", "<p>html</p>", "text");
                        });

        final Optional<VerificationRequestResult> result =
                accountAuthService.resendVerification("pending@test.com");

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("pending@test.com", result.orElseThrow().getEmail());
        Assertions.assertEquals(Locale.of("es"), capturedTemplateData.get().getLocale());
    }

    @Test
    public void testConfirmVerificationMarksAccountAsVerified() {
        final AtomicReference<Instant> verificationTimestamp = new AtomicReference<>();
        final AtomicReference<EmailActionStatus> updatedStatus = new AtomicReference<>();
        final EmailActionRequest request =
                new EmailActionRequest(
                        31L,
                        EmailActionType.ACCOUNT_VERIFICATION,
                        "verify@test.com",
                        5L,
                        "token-hash",
                        "{}",
                        EmailActionStatus.PENDING,
                        FIXED_NOW.plusSeconds(24 * 3600L),
                        null,
                        FIXED_NOW,
                        FIXED_NOW);

        Mockito.when(emailActionRequestDao.findByTokenHashForUpdate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(request));
        Mockito.when(userDao.findAccountById(5L))
                .thenReturn(
                        Optional.of(
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
                                        UserLanguages.DEFAULT_LANGUAGE)));
        Mockito.doAnswer(
                        invocation -> {
                            verificationTimestamp.set(invocation.getArgument(1));
                            return null;
                        })
                .when(userDao)
                .markEmailVerified(ArgumentMatchers.eq(5L), ArgumentMatchers.any());
        Mockito.doAnswer(
                        invocation -> {
                            updatedStatus.set(invocation.getArgument(1));
                            return null;
                        })
                .when(emailActionRequestDao)
                .updateStatus(
                        ArgumentMatchers.eq(31L),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.eq(5L),
                        ArgumentMatchers.any());

        final VerificationConfirmationResult result =
                accountAuthService.confirmVerification("raw-token");

        Assertions.assertEquals(5L, result.getUserId());
        Assertions.assertEquals("/", result.getRedirectUrl());
        Assertions.assertTrue(result.getAccount().isPresent());
        Assertions.assertEquals(5L, result.getAccount().orElseThrow().getId());
        Assertions.assertEquals(EmailActionStatus.COMPLETED, updatedStatus.get());
        Assertions.assertEquals(FIXED_NOW, verificationTimestamp.get());
    }

    @Test
    public void testRequestPasswordResetCreatesPendingRequestForVerifiedAccount() {
        final AtomicReference<VerificationMailTemplateData> capturedTemplateData =
                new AtomicReference<>();
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
                                ArgumentMatchers.eq(6L),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.eq("{}"),
                                ArgumentMatchers.eq(FIXED_NOW.plusSeconds(24 * 3600L))))
                .thenReturn(
                        new EmailActionRequest(
                                40L,
                                EmailActionType.PASSWORD_RESET,
                                "legacy@test.com",
                                6L,
                                "token-hash",
                                "{}",
                                EmailActionStatus.PENDING,
                                FIXED_NOW.plusSeconds(24 * 3600L),
                                null,
                                FIXED_NOW,
                                FIXED_NOW));
        Mockito.when(templateRenderer.renderActionMail(ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            capturedTemplateData.set(invocation.getArgument(0));
                            return new MailContent("subject", "<p>html</p>", "text");
                        });

        final Optional<VerificationRequestResult> result =
                accountAuthService.requestPasswordReset("legacy@test.com");

        Assertions.assertTrue(result.isPresent());
        Assertions.assertNotNull(capturedTemplateData.get());
        Assertions.assertTrue(
                capturedTemplateData.get().getConfirmationUrl().contains("/password-reset/"));
        Assertions.assertEquals(Locale.of("es"), capturedTemplateData.get().getLocale());
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
                        8L,
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
        final AtomicReference<String> capturedPasswordHash = new AtomicReference<>();
        final AtomicReference<EmailActionStatus> updatedStatus = new AtomicReference<>();
        final EmailActionRequest request =
                new EmailActionRequest(
                        60L,
                        EmailActionType.PASSWORD_RESET,
                        "player@test.com",
                        9L,
                        "token-hash",
                        "{}",
                        EmailActionStatus.PENDING,
                        FIXED_NOW.plusSeconds(24 * 3600L),
                        null,
                        FIXED_NOW,
                        FIXED_NOW);

        Mockito.when(emailActionRequestDao.findByTokenHashForUpdate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(request));
        Mockito.when(userDao.findAccountById(9L))
                .thenReturn(
                        Optional.of(
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
                                        UserLanguages.DEFAULT_LANGUAGE)));
        Mockito.doAnswer(
                        invocation -> {
                            capturedPasswordHash.set(invocation.getArgument(1));
                            return null;
                        })
                .when(userDao)
                .updatePasswordHash(ArgumentMatchers.eq(9L), ArgumentMatchers.anyString());
        Mockito.doAnswer(
                        invocation -> {
                            updatedStatus.set(invocation.getArgument(1));
                            return null;
                        })
                .when(emailActionRequestDao)
                .updateStatus(
                        ArgumentMatchers.eq(60L),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.eq(9L),
                        ArgumentMatchers.any());

        final VerificationConfirmationResult result =
                accountAuthService.resetPassword("raw-token", "EvenBetter123!");

        Assertions.assertEquals(9L, result.getUserId());
        Assertions.assertEquals("/login?reset=1", result.getRedirectUrl());
        Assertions.assertEquals(EmailActionStatus.COMPLETED, updatedStatus.get());
        Assertions.assertNotNull(capturedPasswordHash.get());
        Assertions.assertTrue(
                passwordEncoder.matches("EvenBetter123!", capturedPasswordHash.get()));
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
}
