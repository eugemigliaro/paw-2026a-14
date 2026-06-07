package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EmailActionRequest;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.EmailActionStatus;
import ar.edu.itba.paw.models.types.EmailActionType;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.persistence.EmailActionRequestDao;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.exceptions.passwordReset.PasswordResetInvalidException;
import ar.edu.itba.paw.services.exceptions.registration.AccountRegistrationException;
import ar.edu.itba.paw.services.exceptions.registration.EmailPendingVerificationException;
import ar.edu.itba.paw.services.exceptions.registration.EmailTakenException;
import ar.edu.itba.paw.services.exceptions.registration.LastNameInvalidException;
import ar.edu.itba.paw.services.exceptions.registration.NameInvalidException;
import ar.edu.itba.paw.services.exceptions.registration.PasswordInvalidException;
import ar.edu.itba.paw.services.exceptions.registration.PhoneInvalidException;
import ar.edu.itba.paw.services.exceptions.registration.UsernameInvalidException;
import ar.edu.itba.paw.services.exceptions.registration.UsernameTakenException;
import ar.edu.itba.paw.services.exceptions.verificationFailure.VerificationFailureAlreadyUsedException;
import ar.edu.itba.paw.services.exceptions.verificationFailure.VerificationFailureExpiredException;
import ar.edu.itba.paw.services.exceptions.verificationFailure.VerificationFailureInvalidActionException;
import ar.edu.itba.paw.services.exceptions.verificationFailure.VerificationFailureNotFoundException;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.MailProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Autowired
    public AccountAuthServiceImpl(
            final UserDao userDao,
            final EmailActionRequestDao emailActionRequestDao,
            final MailProperties mailProperties,
            final MailDispatchService mailDispatchService,
            final PasswordEncoder passwordEncoder,
            final Clock clock) {
        this.userDao = Objects.requireNonNull(userDao);
        this.emailActionRequestDao = Objects.requireNonNull(emailActionRequestDao);
        this.mailProperties = Objects.requireNonNull(mailProperties);
        this.mailDispatchService = Objects.requireNonNull(mailDispatchService);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional
    public VerificationRequestResult register(final RegisterAccountRequest request) {
        final Locale locale = currentLocale();
        final String normalizedEmail = normalizeEmail(request.getEmail());
        final String normalizedUsername = normalizeUsername(request.getUsername(), locale);
        final String normalizedName = normalizeRequiredText(request.getName(), 150, "name", locale);
        final String normalizedLastName =
                normalizeRequiredText(request.getLastName(), 150, "lastName", locale);
        final String normalizedPhone = normalizeRequiredPhone(request.getPhone(), locale);
        validatePassword(request.getPassword(), locale);

        final Optional<UserAccount> existingAccount = userDao.findAccountByEmail(normalizedEmail);
        if (existingAccount.isPresent()) {
            if (existingAccount.get().isEmailVerified()) {
                throw new EmailTakenException("An account with that email already exists");
            }
            throw new EmailPendingVerificationException(
                    "That email is already registered but still pending verification");
        }

        if (userDao.findByUsername(normalizedUsername).isPresent()) {
            throw new UsernameTakenException("That username is already in use");
        }

        try {
            final UserAccount createdAccount =
                    userDao.createAccount(
                            normalizedEmail,
                            normalizedUsername,
                            normalizedName,
                            normalizedLastName,
                            normalizedPhone,
                            UserLanguages.fromLocale(locale),
                            passwordEncoder.encode(request.getPassword()),
                            UserRole.USER,
                            null);
            return createAccountVerificationRequest(createdAccount, locale);
        } catch (final DataIntegrityViolationException exception) {
            if (userDao.findAccountByEmail(normalizedEmail).isPresent()) {
                throw new EmailTakenException("An account with that email already exists");
            }
            if (userDao.findByUsername(normalizedUsername).isPresent()) {
                throw new UsernameTakenException("That username is already in use");
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
        return Optional.of(
                createAccountVerificationRequest(
                        account.get(), recipientLocale(account.get(), locale)));
    }

    @Override
    @Transactional
    public VerificationPreview getVerificationPreview(final String rawToken) {
        final Locale locale = currentLocale();
        final EmailActionRequest request =
                getRequiredPendingRequest(
                        rawToken, EmailActionType.ACCOUNT_VERIFICATION, false, locale);
        return new VerificationPreview(request.getEmail(), request.getExpiresAt());
    }

    @Override
    @Transactional
    public VerificationConfirmationResult confirmVerification(final String rawToken) {
        final Locale locale = currentLocale();
        final EmailActionRequest request =
                getRequiredPendingRequest(
                        rawToken, EmailActionType.ACCOUNT_VERIFICATION, true, locale);
        final UserAccount account = getRequiredAccount(request, locale, false);
        final Instant now = Instant.now(clock);

        if (!account.isEmailVerified()) {
            userDao.markEmailVerified(account.getId(), now);
        }

        emailActionRequestDao.updateStatus(
                request.getId(), EmailActionStatus.COMPLETED, account.toUser(), now);

        final UserAccount verifiedAccount =
                account.isEmailVerified()
                        ? account
                        : userDao.findAccountById(account.getId()).orElse(account);
        return new VerificationConfirmationResult(verifiedAccount);
    }

    @Override
    @Transactional
    public Optional<VerificationRequestResult> requestPasswordReset(final String email) {
        final Locale locale = currentLocale();
        final Optional<UserAccount> account = userDao.findAccountByEmail(normalizeEmail(email));
        if (account.isEmpty() || !account.get().isEmailVerified()) {
            return Optional.empty();
        }
        return Optional.of(
                createPasswordResetRequest(account.get(), recipientLocale(account.get(), locale)));
    }

    @Override
    @Transactional
    public PasswordResetPreview getPasswordResetPreview(final String rawToken) {
        final Locale locale = currentLocale();
        final EmailActionRequest request =
                getRequiredPendingRequest(rawToken, EmailActionType.PASSWORD_RESET, false, locale);
        getRequiredAccount(request, locale, true);
        return new PasswordResetPreview(request.getEmail(), request.getExpiresAt());
    }

    @Override
    @Transactional
    public VerificationConfirmationResult resetPassword(
            final String rawToken, final String newPassword) {
        final Locale locale = currentLocale();
        validateResetPassword(newPassword, locale);

        final EmailActionRequest request =
                getRequiredPendingRequest(rawToken, EmailActionType.PASSWORD_RESET, true, locale);
        final UserAccount account = getRequiredAccount(request, locale, true);
        final Instant now = Instant.now(clock);

        userDao.updatePasswordHash(account.getId(), passwordEncoder.encode(newPassword));
        emailActionRequestDao.updateStatus(
                request.getId(), EmailActionStatus.COMPLETED, account.toUser(), now);

        return new VerificationConfirmationResult(account.getId());
    }

    @Override
    @Transactional(readOnly = true)
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
                locale);
    }

    private VerificationRequestResult createPendingRequest(
            final UserAccount account,
            final EmailActionType actionType,
            final String rawToken,
            final String confirmationUrl,
            final Locale locale) {
        final Instant now = Instant.now(clock);
        final Instant expiresAt = now.plusSeconds(mailProperties.getVerificationTtlHours() * 3600L);
        final String tokenHash = hashToken(rawToken);

        emailActionRequestDao.expirePendingByEmailAndActionType(
                actionType, account.getEmail(), now);
        emailActionRequestDao.create(
                actionType,
                account.getEmail(),
                account.toUser(),
                tokenHash,
                EMPTY_PAYLOAD_JSON,
                expiresAt);

        if (actionType == EmailActionType.PASSWORD_RESET) {
            mailDispatchService.sendPasswordReset(account, confirmationUrl, expiresAt, locale);
        } else {
            mailDispatchService.sendAccountVerification(
                    account, confirmationUrl, expiresAt, locale);
        }
        return new VerificationRequestResult(account.getEmail(), expiresAt);
    }

    private EmailActionRequest getRequiredPendingRequest(
            final String rawToken,
            final EmailActionType expectedActionType,
            final boolean forUpdate,
            final Locale locale) {
        final String tokenHash = hashToken(rawToken);
        final EmailActionRequest request =
                (forUpdate
                                ? emailActionRequestDao.findByTokenHashForUpdate(tokenHash)
                                : emailActionRequestDao.findByTokenHash(tokenHash))
                        .orElseThrow(
                                () ->
                                        new VerificationFailureNotFoundException(
                                                "That verification link is invalid or no longer exists"));

        if (request.getStatus() == EmailActionStatus.COMPLETED
                || request.getStatus() == EmailActionStatus.FAILED) {
            throw new VerificationFailureAlreadyUsedException(
                    "This account verification can no longer be completed");
        }

        final Instant now = Instant.now(clock);
        if (request.getStatus() == EmailActionStatus.EXPIRED || request.isExpired(now)) {
            emailActionRequestDao.updateStatus(
                    request.getId(), EmailActionStatus.EXPIRED, request.getUser(), now);
            throw new VerificationFailureExpiredException("That verification link has expired");
        }

        if (request.getActionType() != expectedActionType) {
            throw new VerificationFailureInvalidActionException(
                    "This password reset link can no longer be used");
        }

        return request;
    }

    private UserAccount getRequiredAccount(
            final EmailActionRequest request,
            final Locale locale,
            final boolean requireVerifiedAccount) {
        final Optional<UserAccount> account =
                request.getUser().getId() == null
                        ? userDao.findAccountByEmail(request.getEmail())
                        : userDao.findAccountById(request.getUser().getId());

        if (account.isEmpty()) {
            throw invalidateRequest(
                    request, "This account verification can no longer be completed");
        }

        if (requireVerifiedAccount && !account.get().isEmailVerified()) {
            throw invalidateRequest(
                    request, "This account verification can no longer be completed");
        }

        return account.get();
    }

    private VerificationFailureInvalidActionException invalidateRequest(
            final EmailActionRequest request, final String message) {
        emailActionRequestDao.updateStatus(
                request.getId(), EmailActionStatus.FAILED, null, Instant.now(clock));
        return new VerificationFailureInvalidActionException(message);
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
            throw new UsernameInvalidException(
                    "Use 3 to 50 lowercase letters, numbers, or underscores for your username");
        }

        final String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new UsernameInvalidException(
                    "Use 3 to 50 lowercase letters, numbers, or underscores for your username");
        }
        return normalized;
    }

    private void validatePassword(final String password, final Locale locale) {
        if (isPasswordLengthInvalid(password)) {
            throw new PasswordInvalidException("Use 8 to 72 characters");
        }
    }

    private void validateResetPassword(final String password, final Locale locale) {
        if (isPasswordLengthInvalid(password)) {
            throw new PasswordResetInvalidException("Use 8 to 72 characters");
        }
    }

    private String normalizeRequiredText(
            final String value, final int maxLength, final String fieldCode, final Locale locale) {
        if (value == null) {
            if ("name".equals(fieldCode)) {
                throw new NameInvalidException("Enter a valid first name");
            } else if ("lastName".equals(fieldCode)) {
                throw new LastNameInvalidException("Enter a valid last name");
            }
            throw new AccountRegistrationException("Invalid field found");
        }

        final String normalized = value.trim();
        if (normalized.isBlank() || normalized.length() > maxLength) {
            if ("name".equals(fieldCode)) {
                throw new NameInvalidException("Enter a valid first name");
            } else if ("lastName".equals(fieldCode)) {
                throw new LastNameInvalidException("Enter a valid last name");
            }
            throw new AccountRegistrationException("Invalid field found");
        }
        return normalized;
    }

    private String normalizeRequiredPhone(final String phone, final Locale locale) {
        if (phone == null) {
            return null;
        }

        final String normalized = phone.trim();
        if (normalized.isBlank()) {
            return null;
        }

        if (normalized.length() > 50) {
            throw new PhoneInvalidException("Enter a valid phone number");
        }

        if (!normalized.matches("^[0-9+()\\-\\s]{6,50}$")) {
            throw new PhoneInvalidException("Enter a valid phone number");
        }
        return normalized;
    }

    private static boolean isPasswordLengthInvalid(final String password) {
        return password == null
                || password.isBlank()
                || password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH;
    }

    private static Locale currentLocale() {
        return resolvedLocale(LocaleContextHolder.getLocale());
    }

    private static Locale recipientLocale(final UserAccount account, final Locale fallbackLocale) {
        if (account == null) {
            return resolvedLocale(fallbackLocale);
        }
        return UserLanguages.toLocale(account.getPreferredLanguage());
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
