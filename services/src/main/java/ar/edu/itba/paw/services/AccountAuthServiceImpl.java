package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EmailActionRequest;
import ar.edu.itba.paw.models.EmailActionStatus;
import ar.edu.itba.paw.models.EmailActionType;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.persistence.EmailActionRequestDao;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.exceptions.AccountRegistrationException;
import ar.edu.itba.paw.services.exceptions.PasswordResetException;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.MailProperties;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import ar.edu.itba.paw.services.mail.VerificationMailTemplateData;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountAuthServiceImpl implements AccountAuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]{3,50}$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;
    private static final String EMPTY_PAYLOAD_JSON = "{}";

    private final UserDao userDao;
    private final EmailActionRequestDao emailActionRequestDao;
    private final MailProperties mailProperties;
    private final MailDispatchService mailDispatchService;
    private final ThymeleafMailTemplateRenderer templateRenderer;
    private final MessageSource messageSource;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Autowired
    public AccountAuthServiceImpl(
            final UserDao userDao,
            final EmailActionRequestDao emailActionRequestDao,
            final MailProperties mailProperties,
            final MailDispatchService mailDispatchService,
            final ThymeleafMailTemplateRenderer templateRenderer,
            final MessageSource messageSource,
            final PasswordEncoder passwordEncoder,
            final Clock clock) {
        this.userDao = userDao;
        this.emailActionRequestDao = emailActionRequestDao;
        this.mailProperties = mailProperties;
        this.mailDispatchService = mailDispatchService;
        this.templateRenderer = templateRenderer;
        this.messageSource = messageSource;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public VerificationRequestResult register(final RegisterAccountRequest request) {
        final Locale locale = currentLocale();
        final String normalizedEmail = normalizeEmail(request.getEmail());
        final String normalizedUsername = normalizeUsername(request.getUsername(), locale);
        validatePassword(request.getPassword(), locale);

        final Optional<UserAccount> existingAccount = userDao.findAccountByEmail(normalizedEmail);
        if (existingAccount.isPresent()) {
            if (existingAccount.get().isEmailVerified()) {
                throw new AccountRegistrationException(
                        "email_taken", message("auth.registration.error.emailTaken", locale));
            }
            throw new AccountRegistrationException(
                    "email_pending_verification",
                    message("auth.registration.error.emailPending", locale));
        }

        if (userDao.findByUsername(normalizedUsername).isPresent()) {
            throw new AccountRegistrationException(
                    "username_taken", message("auth.registration.error.usernameTaken", locale));
        }

        try {
            final UserAccount createdAccount =
                    userDao.createAccount(
                            normalizedEmail,
                            normalizedUsername,
                            passwordEncoder.encode(request.getPassword()),
                            UserRole.USER,
                            null);
            return createAccountVerificationRequest(createdAccount, locale);
        } catch (final DataIntegrityViolationException exception) {
            if (userDao.findAccountByEmail(normalizedEmail).isPresent()) {
                throw new AccountRegistrationException(
                        "email_taken", message("auth.registration.error.emailTaken", locale));
            }
            if (userDao.findByUsername(normalizedUsername).isPresent()) {
                throw new AccountRegistrationException(
                        "username_taken", message("auth.registration.error.usernameTaken", locale));
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public Optional<VerificationRequestResult> resendVerification(final String email) {
        final Locale locale = currentLocale();
        final Optional<UserAccount> account = userDao.findAccountByEmail(normalizeEmail(email));
        if (account.isEmpty() || account.get().isEmailVerified()) {
            return Optional.empty();
        }
        return Optional.of(createAccountVerificationRequest(account.get(), locale));
    }

    @Override
    public VerificationPreview getVerificationPreview(final String rawToken) {
        final Locale locale = currentLocale();
        final EmailActionRequest request =
                getRequiredPendingRequest(
                        rawToken,
                        EmailActionType.ACCOUNT_VERIFICATION,
                        false,
                        locale,
                        "verification.message.accountUnavailable");
        return new VerificationPreview(
                message("verification.preview.account.title", locale),
                message("verification.preview.account.summary", locale),
                request.getEmail(),
                request.getExpiresAt(),
                message("verification.preview.account.confirm", locale),
                "/login?verified=1",
                List.of());
    }

    @Override
    @Transactional
    public VerificationConfirmationResult confirmVerification(final String rawToken) {
        final Locale locale = currentLocale();
        final EmailActionRequest request =
                getRequiredPendingRequest(
                        rawToken,
                        EmailActionType.ACCOUNT_VERIFICATION,
                        true,
                        locale,
                        "verification.message.accountUnavailable");
        final UserAccount account =
                getRequiredAccount(
                        request, locale, "verification.message.accountUnavailable", false);
        final Instant now = Instant.now(clock);

        if (!account.isEmailVerified()) {
            userDao.markEmailVerified(account.getId(), now);
        }

        emailActionRequestDao.updateStatus(
                request.getId(), EmailActionStatus.COMPLETED, account.getId(), now);

        return new VerificationConfirmationResult(
                account.getId(),
                "/login?verified=1",
                message("verification.message.accountVerified", locale));
    }

    @Override
    @Transactional
    public Optional<VerificationRequestResult> requestPasswordReset(final String email) {
        final Locale locale = currentLocale();
        final Optional<UserAccount> account = userDao.findAccountByEmail(normalizeEmail(email));
        if (account.isEmpty() || !account.get().isEmailVerified()) {
            return Optional.empty();
        }
        return Optional.of(createPasswordResetRequest(account.get(), locale));
    }

    @Override
    public PasswordResetPreview getPasswordResetPreview(final String rawToken) {
        final Locale locale = currentLocale();
        final EmailActionRequest request =
                getRequiredPendingRequest(
                        rawToken,
                        EmailActionType.PASSWORD_RESET,
                        false,
                        locale,
                        "passwordReset.message.unavailable");
        getRequiredAccount(request, locale, "passwordReset.message.unavailable", true);
        return new PasswordResetPreview(request.getEmail(), request.getExpiresAt());
    }

    @Override
    @Transactional
    public VerificationConfirmationResult resetPassword(
            final String rawToken, final String newPassword) {
        final Locale locale = currentLocale();
        validateResetPassword(newPassword, locale);

        final EmailActionRequest request =
                getRequiredPendingRequest(
                        rawToken,
                        EmailActionType.PASSWORD_RESET,
                        true,
                        locale,
                        "passwordReset.message.unavailable");
        final UserAccount account =
                getRequiredAccount(request, locale, "passwordReset.message.unavailable", true);
        final Instant now = Instant.now(clock);

        userDao.updatePasswordHash(account.getId(), passwordEncoder.encode(newPassword));
        emailActionRequestDao.updateStatus(
                request.getId(), EmailActionStatus.COMPLETED, account.getId(), now);

        return new VerificationConfirmationResult(
                account.getId(),
                "/login?reset=1",
                message("passwordReset.message.completed", locale));
    }

    @Override
    public Optional<UserAccount> findAccountByEmail(final String email) {
        return userDao.findAccountByEmail(normalizeEmail(email));
    }

    private VerificationRequestResult createAccountVerificationRequest(
            final UserAccount account, final Locale locale) {
        final String rawToken = generateToken();
        return createPendingRequest(
                account,
                EmailActionType.ACCOUNT_VERIFICATION,
                rawToken,
                buildVerificationUrl(rawToken, locale),
                message("verification.preview.account.title", locale),
                message("verification.preview.account.summary", locale),
                locale);
    }

    private VerificationRequestResult createPasswordResetRequest(
            final UserAccount account, final Locale locale) {
        final String rawToken = generateToken();
        return createPendingRequest(
                account,
                EmailActionType.PASSWORD_RESET,
                rawToken,
                buildPasswordResetUrl(rawToken, locale),
                message("passwordReset.mail.title", locale),
                message("passwordReset.mail.summary", locale),
                locale);
    }

    private VerificationRequestResult createPendingRequest(
            final UserAccount account,
            final EmailActionType actionType,
            final String rawToken,
            final String confirmationUrl,
            final String title,
            final String summary,
            final Locale locale) {
        final Instant now = Instant.now(clock);
        final Instant expiresAt = now.plusSeconds(mailProperties.getVerificationTtlHours() * 3600L);
        final String tokenHash = hashToken(rawToken);

        emailActionRequestDao.expirePendingByEmailAndActionType(
                actionType, account.getEmail(), now);
        emailActionRequestDao.create(
                actionType,
                account.getEmail(),
                account.getId(),
                tokenHash,
                EMPTY_PAYLOAD_JSON,
                expiresAt);

        final MailContent mailContent =
                templateRenderer.renderActionMail(
                        new VerificationMailTemplateData(
                                title,
                                summary,
                                account.getEmail(),
                                confirmationUrl,
                                expiresAt,
                                List.of(),
                                locale));
        mailDispatchService.dispatch(account.getEmail(), mailContent);
        return new VerificationRequestResult(account.getEmail(), expiresAt);
    }

    private EmailActionRequest getRequiredPendingRequest(
            final String rawToken,
            final EmailActionType expectedActionType,
            final boolean forUpdate,
            final Locale locale,
            final String invalidActionCode) {
        final String tokenHash = hashToken(rawToken);
        final EmailActionRequest request =
                (forUpdate
                                ? emailActionRequestDao.findByTokenHashForUpdate(tokenHash)
                                : emailActionRequestDao.findByTokenHash(tokenHash))
                        .orElseThrow(
                                () ->
                                        new VerificationFailureException(
                                                VerificationFailureReason.NOT_FOUND,
                                                message("verification.message.notFound", locale)));

        if (request.getStatus() == EmailActionStatus.COMPLETED
                || request.getStatus() == EmailActionStatus.FAILED) {
            throw new VerificationFailureException(
                    VerificationFailureReason.ALREADY_USED,
                    message("verification.message.alreadyUsed", locale));
        }

        final Instant now = Instant.now(clock);
        if (request.getStatus() == EmailActionStatus.EXPIRED || request.isExpired(now)) {
            emailActionRequestDao.updateStatus(
                    request.getId(), EmailActionStatus.EXPIRED, request.getUserId(), now);
            throw new VerificationFailureException(
                    VerificationFailureReason.EXPIRED,
                    message("verification.message.expired", locale));
        }

        if (request.getActionType() != expectedActionType) {
            throw new VerificationFailureException(
                    VerificationFailureReason.INVALID_ACTION, message(invalidActionCode, locale));
        }

        return request;
    }

    private UserAccount getRequiredAccount(
            final EmailActionRequest request,
            final Locale locale,
            final String invalidActionCode,
            final boolean requireVerifiedAccount) {
        final Optional<UserAccount> account =
                request.getUserId() == null
                        ? userDao.findAccountByEmail(request.getEmail())
                        : userDao.findAccountById(request.getUserId());

        if (account.isEmpty()) {
            throw invalidateRequest(
                    request, request.getUserId(), message(invalidActionCode, locale));
        }

        if (requireVerifiedAccount && !account.get().isEmailVerified()) {
            throw invalidateRequest(
                    request, account.get().getId(), message(invalidActionCode, locale));
        }

        return account.get();
    }

    private VerificationFailureException invalidateRequest(
            final EmailActionRequest request, final Long userId, final String message) {
        emailActionRequestDao.updateStatus(
                request.getId(), EmailActionStatus.FAILED, userId, Instant.now(clock));
        return new VerificationFailureException(VerificationFailureReason.INVALID_ACTION, message);
    }

    private String buildVerificationUrl(final String rawToken, final Locale locale) {
        return buildActionUrl("/verifications/" + rawToken, locale);
    }

    private String buildPasswordResetUrl(final String rawToken, final Locale locale) {
        return buildActionUrl("/password-reset/" + rawToken, locale);
    }

    private String buildActionUrl(final String path, final Locale locale) {
        final String baseUrl = stripTrailingSlash(mailProperties.getBaseUrl());
        final String languageTag = resolvedLocale(locale).getLanguage();
        return languageTag.isBlank() ? baseUrl + path : baseUrl + path + "?lang=" + languageTag;
    }

    private static String stripTrailingSlash(final String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String normalizeEmail(final String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email cannot be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(final String username, final Locale locale) {
        if (username == null) {
            throw new AccountRegistrationException(
                    "username_invalid", message("auth.registration.error.usernameInvalid", locale));
        }

        final String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new AccountRegistrationException(
                    "username_invalid", message("auth.registration.error.usernameInvalid", locale));
        }
        return normalized;
    }

    private void validatePassword(final String password, final Locale locale) {
        if (password == null
                || password.isBlank()
                || password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH) {
            throw new AccountRegistrationException(
                    "password_invalid", message("auth.registration.error.passwordInvalid", locale));
        }
    }

    private void validateResetPassword(final String password, final Locale locale) {
        if (password == null
                || password.isBlank()
                || password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH) {
            throw new PasswordResetException(
                    "password_invalid", message("auth.registration.error.passwordInvalid", locale));
        }
    }

    private String message(final String code, final Locale locale) {
        return message(code, null, locale);
    }

    private String message(final String code, final Object[] args, final Locale locale) {
        return messageSource.getMessage(code, args, code, locale);
    }

    private static Locale currentLocale() {
        return resolvedLocale(LocaleContextHolder.getLocale());
    }

    private static Locale resolvedLocale(final Locale locale) {
        return locale == null ? Locale.ENGLISH : locale;
    }

    private static String generateToken() {
        final byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
}
