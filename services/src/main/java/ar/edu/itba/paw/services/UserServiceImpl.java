package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.exceptions.registration.LastNameInvalidException;
import ar.edu.itba.paw.models.exceptions.registration.NameInvalidException;
import ar.edu.itba.paw.models.exceptions.registration.PhoneInvalidException;
import ar.edu.itba.paw.models.exceptions.registration.UsernameInvalidException;
import ar.edu.itba.paw.models.exceptions.registration.UsernameTakenException;
import ar.edu.itba.paw.persistence.UserDao;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]{3,50}$");

    private final UserDao userDao;
    private final ImageService imageService;

    @Autowired
    public UserServiceImpl(final UserDao userDao, final ImageService imageService) {
        this.userDao = Objects.requireNonNull(userDao);
        this.imageService = Objects.requireNonNull(imageService);
    }

    @Override
    @Transactional
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
        final String normalizedName = normalizeName(name, 150);
        final String normalizedLastName = normalizeLastName(lastName, 150);
        final String normalizedPhone = normalizePhone(phone, locale);
        ImageMetadata profileImageMetadata = imageService.resolveImageMetadata(profileImage);
        if (profileImageMetadata == null) {
            profileImageMetadata = user.getProfileImageMetadata();
        }

        final Optional<User> userWithUsername = userDao.findByUsername(normalizedUsername);
        if (userWithUsername.isPresent() && !userWithUsername.get().getId().equals(user.getId())) {
            throw new UsernameTakenException();
        }

        userDao.updateProfile(
                user.getId(),
                normalizedUsername,
                normalizedName,
                normalizedLastName,
                normalizedPhone,
                profileImageMetadata);

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
        userDao.updatePreferredLanguage(
                user.getId(), UserLanguages.normalizeLanguage(preferredLanguage));
    }

    private String normalizeUsername(
            final User existingUser, final String username, final Locale locale) {
        if (username == null) {
            throw new UsernameInvalidException();
        }

        final String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (existingUser.getUsername() != null
                && existingUser.getUsername().trim().equalsIgnoreCase(normalized)) {
            return existingUser.getUsername();
        }
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new UsernameInvalidException();
        }
        return normalized;
    }

    private void nonNullUser(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("exception.user.notNull");
        }
    }

    private String normalizeName(final String name, final int maxLength) {
        if (name == null) {
            throw new NameInvalidException();
        }

        final String normalized = name.trim();
        if (normalized.isBlank() || normalized.length() > maxLength) {
            throw new NameInvalidException();
        }

        return normalized;
    }

    private String normalizeLastName(final String lastName, final int maxLength) {
        if (lastName == null) {
            throw new LastNameInvalidException();
        }

        final String normalized = lastName.trim();
        if (normalized.isBlank() || normalized.length() > maxLength) {
            throw new LastNameInvalidException();
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
            throw new PhoneInvalidException();
        }
        return normalized;
    }

    private static Locale currentLocale() {
        final Locale locale = LocaleContextHolder.getLocale();
        return locale == null ? Locale.ENGLISH : locale;
    }
}
