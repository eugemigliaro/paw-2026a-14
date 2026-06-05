package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.persistence.UserDao;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserDataServiceImpl implements UserDataService {

    private final UserDao userDao;

    public UserDataServiceImpl(final UserDao userDao) {
        this.userDao = Objects.requireNonNull(userDao);
    }

    @Override
    public User createUser(final String email, final String username) {
        return userDao.createUser(email, username);
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
        return userDao.createAccount(
                email,
                username,
                name,
                lastName,
                phone,
                preferredLanguage,
                passwordHash,
                role,
                emailVerifiedAt);
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return userDao.findByEmail(email);
    }

    @Override
    public Optional<UserAccount> findAccountByEmail(final String email) {
        return userDao.findAccountByEmail(email);
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
    public Optional<UserAccount> findAccountById(final Long id) {
        return userDao.findAccountById(id);
    }

    @Override
    public Optional<User> findByUsername(final String username) {
        return userDao.findByUsername(username);
    }

    @Override
    public void updateProfile(
            final Long id,
            final String username,
            final String name,
            final String lastName,
            final String phone,
            final ImageMetadata profileImageMetadata) {
        userDao.updateProfile(id, username, name, lastName, phone, profileImageMetadata);
    }

    @Override
    public void updatePasswordHash(final Long id, final String passwordHash) {
        userDao.updatePasswordHash(id, passwordHash);
    }

    @Override
    public void markEmailVerified(final Long id, final Instant emailVerifiedAt) {
        userDao.markEmailVerified(id, emailVerifiedAt);
    }

    @Override
    public void updatePreferredLanguage(final Long id, final String preferredLanguage) {
        userDao.updatePreferredLanguage(id, preferredLanguage);
    }
}
