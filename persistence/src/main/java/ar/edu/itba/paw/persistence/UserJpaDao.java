package ar.edu.itba.paw.persistence;

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
        final Instant now = Instant.now();
        final UserAccount userAccount =
                new UserAccount(
                        null,
                        email,
                        username,
                        null,
                        null,
                        null,
                        null,
                        null,
                        UserRole.USER,
                        null,
                        UserLanguages.DEFAULT_LANGUAGE,
                        now,
                        now);

        em.persist(userAccount);
        em.flush();

        return toUser(userAccount);
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
        em.flush();

        return userAccount;
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        final TypedQuery<UserAccount> query =
                em.createQuery("FROM UserAccount u WHERE u.email = :email", UserAccount.class);
        query.setParameter("email", email);

        return query.getResultList().stream().map(this::toUser).findAny();
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
        return Optional.ofNullable(em.find(UserAccount.class, id)).map(this::toUser);
    }

    @Override
    public List<User> findByIds(final Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        final TypedQuery<UserAccount> query =
                em.createQuery("FROM UserAccount u WHERE u.id IN :ids", UserAccount.class);
        query.setParameter("ids", ids);

        return query.getResultList().stream().map(this::toUser).toList();
    }

    @Override
    public Optional<UserAccount> findAccountById(final Long id) {
        return Optional.ofNullable(em.find(UserAccount.class, id));
    }

    @Override
    public Optional<User> findByUsername(final String username) {
        final TypedQuery<UserAccount> query =
                em.createQuery(
                        "FROM UserAccount u WHERE u.username = :username", UserAccount.class);
        query.setParameter("username", username);

        return query.getResultList().stream().map(this::toUser).findAny();
    }

    @Override
    public void updateProfile(
            final Long id,
            final String username,
            final String name,
            final String lastName,
            final String phone,
            final Long profileImageId) {
        final UserAccount user = em.find(UserAccount.class, id);
        if (user != null) {
            user.setUsername(username);
            user.setName(name);
            user.setLastName(lastName);
            user.setPhone(phone);
            user.setProfileImageId(profileImageId);
            user.setUpdatedAt(Instant.now());
        }
    }

    @Override
    public void updateProfileImage(final Long id, final Long profileImageId) {
        final UserAccount user = em.find(UserAccount.class, id);
        if (user != null) {
            user.setProfileImageId(profileImageId);
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

    // Convert a UserAccount entity to a User DTO.
    private User toUser(final UserAccount account) {
        return new User(
                account.getId(),
                account.getEmail(),
                account.getUsername(),
                account.getName(),
                account.getLastName(),
                account.getPhone(),
                account.getProfileImageId(),
                account.getPreferredLanguage());
    }
}
