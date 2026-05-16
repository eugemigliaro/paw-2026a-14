package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.UserRole;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class UserJpaDao implements UserDao {

    @PersistenceContext private EntityManager em;

    @Override
    public User createUser(final String email, final String username) {
        final User user =
                new User(
                        null,
                        email,
                        username,
                        null,
                        null,
                        null,
                        null,
                        UserLanguages.DEFAULT_LANGUAGE);

        em.persist(user);

        return user;
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

        final Instant now = Instant.now();
        final UserAccount userAccount =
                new UserAccount(
                        null,
                        email,
                        username,
                        name,
                        lastName,
                        phone,
                        null,
                        passwordHash,
                        role,
                        emailVerifiedAt,
                        preferredLanguage,
                        now,
                        now);

        em.persist(userAccount);

        return userAccount;
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        final TypedQuery<User> query =
                em.createQuery("FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", email);

        return query.getResultList().stream().findAny();
    }

    @Override
    public Optional<UserAccount> findAccountByEmail(final String email) {
        final TypedQuery<UserAccount> query =
                em.createQuery("FROM UserAccount u WHERE u.email = :email", UserAccount.class);
        query.setParameter("email", email);

        return query.getResultList().stream().findAny();
    }

    @Override
    public Optional<User> findById(final Long id) {
        return Optional.ofNullable(em.find(User.class, id));
    }

    @Override
    public List<User> findByIds(final Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        final TypedQuery<User> query = em.createQuery("FROM User u WHERE u.id IN :ids", User.class);
        query.setParameter("ids", ids);

        return query.getResultList().stream().toList();
    }

    @Override
    public Optional<UserAccount> findAccountById(final Long id) {
        return Optional.ofNullable(em.find(UserAccount.class, id));
    }

    @Override
    public Optional<User> findByUsername(final String username) {
        final TypedQuery<User> query =
                em.createQuery("FROM User u WHERE u.username = :username", User.class);
        query.setParameter("username", username);

        return query.getResultList().stream().findAny();
    }

    @Override
    public void updateProfile(
            final Long id,
            final String username,
            final String name,
            final String lastName,
            final String phone,
            final ImageMetadata profileImageMetadata) {
        final UserAccount user = em.find(UserAccount.class, id);
        if (user != null) {
            user.setUsername(username);
            user.setName(name);
            user.setLastName(lastName);
            user.setPhone(phone);
            user.setProfileImageMetadata(profileImageMetadata);
            user.setUpdatedAt(Instant.now());
        }
    }

    @Override
    public void updateProfileImage(final Long id, final ImageMetadata profileImageMetadata) {
        final UserAccount user = em.find(UserAccount.class, id);
        if (user != null) {
            user.setProfileImageMetadata(profileImageMetadata);
            user.setUpdatedAt(Instant.now());
        }
    }

    @Override
    public void updatePasswordHash(final Long id, final String passwordHash) {
        final UserAccount user = em.find(UserAccount.class, id);
        if (user != null) {
            user.setPasswordHash(passwordHash);
            user.setUpdatedAt(Instant.now());
        }
    }

    @Override
    public void markEmailVerified(final Long id, final Instant emailVerifiedAt) {
        final UserAccount user = em.find(UserAccount.class, id);
        if (user != null) {
            user.setEmailVerifiedAt(emailVerifiedAt);
            user.setUpdatedAt(Instant.now());
        }
    }

    @Override
    public void updatePreferredLanguage(final Long id, final String preferredLanguage) {
        final UserAccount user = em.find(UserAccount.class, id);
        if (user != null) {
            user.setPreferredLanguage(UserLanguages.normalizeLanguage(preferredLanguage));
            user.setUpdatedAt(Instant.now());
        }
    }
}
