package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.services.exceptions.registration.AccountRegistrationException;
import ar.edu.itba.paw.services.exceptions.registration.LastNameInvalidException;
import ar.edu.itba.paw.services.exceptions.registration.NameInvalidException;
import ar.edu.itba.paw.services.exceptions.registration.PhoneInvalidException;
import ar.edu.itba.paw.services.exceptions.registration.UsernameInvalidException;
import ar.edu.itba.paw.services.exceptions.registration.UsernameTakenException;
import ar.edu.itba.paw.services.internal.UserDataService;
import java.io.IOException;
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
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]{3,50}$");

    private final UserDataService userDataService;
    private final ImageService imageService;
    private final MessageSource messageSource;

    @Autowired
    public UserServiceImpl(
            final UserDataService userDataService,
            final ImageService imageService,
            final MessageSource messageSource) {
        this.userDataService = Objects.requireNonNull(userDataService);
        this.imageService = Objects.requireNonNull(imageService);
        this.messageSource = Objects.requireNonNull(messageSource);
    }

    @Override
    @Transactional
    public User createUser(final String email, final String username) {
        return userDataService.createUser(email, username);
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return userDataService.findByEmail(email);
    }

    @Override
    public Optional<User> findById(final Long id) {
        return userDataService.findById(id);
    }

    @Override
    public List<User> findByIds(final Collection<Long> ids) {
        return userDataService.findByIds(ids);
    }

    @Override
    public Optional<User> findByUsername(final String username) {
        return userDataService.findByUsername(username);
    }

    @Override
    @Transactional
    public User updateProfile(
            final User user,
            final String username,
            final String name,
            final String lastName,
            final String phone,
            final ImageUpload profileImage)
            throws IOException {
        final Locale locale = currentLocale();
        nonNullUser(user);

        final String normalizedUsername = normalizeUsername(user, username, locale);
        final String normalizedName = normalizeRequiredText(name, 150, "name", locale);
        final String normalizedLastName = normalizeRequiredText(lastName, 150, "lastName", locale);
        final String normalizedPhone = normalizePhone(phone, locale);
        ImageMetadata profileImageMetadata = imageService.resolveImageMetadata(profileImage);
        if (profileImageMetadata == null) {
            profileImageMetadata = user.getProfileImageMetadata();
        }

        final Optional<User> userWithUsername = userDataService.findByUsername(normalizedUsername);
        if (userWithUsername.isPresent() && !userWithUsername.get().getId().equals(user.getId())) {
            throw new UsernameTakenException(message("profile.edit.error.usernameTaken", locale));
        }

        try {
            userDataService.updateProfile(
                    user.getId(),
                    normalizedUsername,
                    normalizedName,
                    normalizedLastName,
                    normalizedPhone,
                    profileImageMetadata);
        } catch (final DataIntegrityViolationException exception) {
            if (userDataService
                    .findByUsername(normalizedUsername)
                    .filter(u -> !u.getId().equals(user.getId()))
                    .isPresent()) {
                throw new UsernameTakenException(
                        message("profile.edit.error.usernameTaken", locale));
            }
            throw exception;
        }

        return new User(
                user.getId(),
                user.getEmail(),
                normalizedUsername,
                normalizedName,
                normalizedLastName,
                normalizedPhone,
                profileImageMetadata,
                user.getPreferredLanguage());
    }

    @Override
    @Transactional
    public void updatePreferredLanguage(final User user, final String preferredLanguage) {
        nonNullUser(user);
        userDataService.updatePreferredLanguage(
                user.getId(), UserLanguages.normalizeLanguage(preferredLanguage));
    }

    private String normalizeUsername(
            final User existingUser, final String username, final Locale locale) {
        if (username == null) {
            throw new UsernameInvalidException(
                    message("profile.edit.error.usernameInvalid", locale));
        }

        final String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (existingUser.getUsername() != null
                && existingUser.getUsername().trim().equalsIgnoreCase(normalized)) {
            return existingUser.getUsername();
        }
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new UsernameInvalidException(
                    message("profile.edit.error.usernameInvalid", locale));
        }
        return normalized;
    }

    private void nonNullUser(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
    }

    private String normalizeRequiredText(
            final String value, final int maxLength, final String fieldCode, final Locale locale) {
        if (value == null) {
            if ("name".equals(fieldCode)) {
                throw new NameInvalidException(message("profile.edit.error.nameInvalid", locale));
            } else if ("lastName".equals(fieldCode)) {
                throw new LastNameInvalidException(
                        message("profile.edit.error.lastNameInvalid", locale));
            }
            throw new AccountRegistrationException(
                    message("profile.edit.error." + fieldCode + "Invalid", locale));
        }

        final String normalized = value.trim();
        if (normalized.isBlank() || normalized.length() > maxLength) {
            if ("name".equals(fieldCode)) {
                throw new NameInvalidException(message("profile.edit.error.nameInvalid", locale));
            } else if ("lastName".equals(fieldCode)) {
                throw new LastNameInvalidException(
                        message("profile.edit.error.lastNameInvalid", locale));
            }
            throw new AccountRegistrationException(
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
            throw new PhoneInvalidException(message("profile.edit.error.phoneInvalid", locale));
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
