package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.UserDao;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class MvpIdentityServiceImpl implements MvpIdentityService {

    private final UserDao userDao;

    @Autowired
    public MvpIdentityServiceImpl(final UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public Optional<User> findExistingByEmail(final String email) {
        return userDao.findByEmail(normalizeEmail(email));
    }

    @Override
    public User resolveOrCreateByEmail(final String email) {
        final String normalizedEmail = normalizeEmail(email);
        return userDao.findByEmail(normalizedEmail).orElseGet(() -> createUser(normalizedEmail));
    }

    private User createUser(final String email) {
        final String baseUsername = sanitizeUsername(email);
        int suffix = 0;

        while (true) {
            final String candidate =
                    suffix == 0 ? baseUsername : truncateUsername(baseUsername, suffix) + suffix;

            if (userDao.findByUsername(candidate).isPresent()) {
                suffix++;
                continue;
            }

            try {
                return userDao.createUser(email, candidate);
            } catch (final DataIntegrityViolationException exception) {
                final Optional<User> existingUser = userDao.findByEmail(email);
                if (existingUser.isPresent()) {
                    return existingUser.get();
                }
                suffix++;
            }
        }
    }

    private static String normalizeEmail(final String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email cannot be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String sanitizeUsername(final String email) {
        final String localPart = email.split("@", 2)[0].toLowerCase(Locale.ROOT);
        final String sanitized = localPart.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return sanitized.isBlank() ? "player" : truncateUsername(sanitized, 0);
    }

    private static String truncateUsername(final String username, final int suffix) {
        final int maxLength = 50;
        final int reserved = suffix == 0 ? 0 : String.valueOf(suffix).length();
        final int limit = Math.max(1, maxLength - reserved);
        return username.length() <= limit ? username : username.substring(0, limit);
    }
}
