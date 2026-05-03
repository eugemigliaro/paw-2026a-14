package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.exceptions.AccountRegistrationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]{3,50}$");

    private final UserDao userDao;
    private final ImageService imageService;
    private final MessageSource messageSource;

    @Autowired
    public UserServiceImpl(
            final UserDao userDao,
            final ImageService imageService,
            final MessageSource messageSource) {
        this.userDao = Objects.requireNonNull(userDao);
        this.imageService = Objects.requireNonNull(imageService);
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    public User createUser(final String email, final String username) {
        return userDao.createUser(email, username);
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return userDao.findByEmail(email);
    }

    @Override
    public Optional<User> findById(final Long id) {
        return userDao.findById(id);
    }

    @Override
    public List<User> findByIds(final Collection<Long> ids) {
        return userDao.findByIds(ids);
    }

    @Override
    public Optional<User> findByUsername(final String username) {
        return userDao.findByUsername(username);
    }

    @Override
    @Transactional
    public User updateProfile(
            final Long id,
            final String username,
            final String name,
            final String lastName,
            final String phone,
            final String profileImageContentType,
            final long profileImageContentLength,
            final InputStream profileImageContentStream)
            throws IOException {
        final Locale locale = currentLocale();
        final User existingUser =
                userDao.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

        final String normalizedUsername = normalizeUsername(existingUser, username, locale);
        final String normalizedName = normalizeRequiredText(name, 150, "name", locale);
        final String normalizedLastName = normalizeRequiredText(lastName, 150, "lastName", locale);
        final String normalizedPhone = normalizePhone(phone, locale);
        final Long profileImageId =
                resolveProfileImageId(
                        existingUser,
                        profileImageContentType,
                        profileImageContentLength,
                        profileImageContentStream);

        final Optional<User> userWithUsername = userDao.findByUsername(normalizedUsername);
        if (userWithUsername.isPresent() && !userWithUsername.get().getId().equals(id)) {
            throw new AccountRegistrationException(
                    "username_taken", message("profile.edit.error.usernameTaken", locale));
        }

        try {
            userDao.updateProfile(
                    id,
                    normalizedUsername,
                    normalizedName,
                    normalizedLastName,
                    normalizedPhone,
                    profileImageId);
        } catch (final DataIntegrityViolationException exception) {
            if (userDao.findByUsername(normalizedUsername)
                    .filter(user -> !user.getId().equals(id))
                    .isPresent()) {
                throw new AccountRegistrationException(
                        "username_taken", message("profile.edit.error.usernameTaken", locale));
            }
            throw exception;
        }

        return new User(
                existingUser.getId(),
                existingUser.getEmail(),
                normalizedUsername,
                normalizedName,
                normalizedLastName,
                normalizedPhone,
                profileImageId);
    }

    @Override
    @Transactional
    public User updateProfileImage(
            final Long id,
            final String contentType,
            final long contentLength,
            final InputStream contentStream)
            throws IOException {
        final User existingUser =
                userDao.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));
        final Long profileImageId = imageService.store(contentType, contentLength, contentStream);
        userDao.updateProfileImage(id, profileImageId);
        return new User(
                existingUser.getId(),
                existingUser.getEmail(),
                existingUser.getUsername(),
                existingUser.getName(),
                existingUser.getLastName(),
                existingUser.getPhone(),
                profileImageId);
    }

    private Long resolveProfileImageId(
            final User existingUser,
            final String profileImageContentType,
            final long profileImageContentLength,
            final InputStream profileImageContentStream)
            throws IOException {
        if (profileImageContentStream == null || profileImageContentLength <= 0) {
            return existingUser.getProfileImageId();
        }

        return imageService.store(
                profileImageContentType, profileImageContentLength, profileImageContentStream);
    }

    private String normalizeUsername(
            final User existingUser, final String username, final Locale locale) {
        if (username == null) {
            throw new AccountRegistrationException(
                    "username_invalid", message("profile.edit.error.usernameInvalid", locale));
        }

        final String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (existingUser.getUsername() != null
                && existingUser.getUsername().trim().equalsIgnoreCase(normalized)) {
            return existingUser.getUsername();
        }
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new AccountRegistrationException(
                    "username_invalid", message("profile.edit.error.usernameInvalid", locale));
        }
        return normalized;
    }

    private String normalizeRequiredText(
            final String value, final int maxLength, final String fieldCode, final Locale locale) {
        if (value == null) {
            throw new AccountRegistrationException(
                    fieldCode + "_invalid",
                    message("profile.edit.error." + fieldCode + "Invalid", locale));
        }

        final String normalized = value.trim();
        if (normalized.isBlank() || normalized.length() > maxLength) {
            throw new AccountRegistrationException(
                    fieldCode + "_invalid",
                    message("profile.edit.error." + fieldCode + "Invalid", locale));
        }
        return normalized;
    }

    private String normalizePhone(final String phone, final Locale locale) {
        if (phone == null) {
            return null;
        }

        final String normalized = phone.trim();
        if (normalized.isBlank()) {
            return null;
        }

        if (normalized.length() > 50 || !normalized.matches("^[0-9+()\\-\\s]{6,50}$")) {
            throw new AccountRegistrationException(
                    "phone_invalid", message("profile.edit.error.phoneInvalid", locale));
        }
        return normalized;
    }

    private String message(final String code, final Locale locale) {
        return messageSource.getMessage(
                Objects.requireNonNull(code), null, code, Objects.requireNonNull(locale));
    }

    private static Locale currentLocale() {
        final Locale locale = LocaleContextHolder.getLocale();
        return locale == null ? Locale.ENGLISH : locale;
    }
}
