package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import java.time.Instant;
import java.util.Optional;

public interface UserDao {

    User createUser(final String email, final String username);

    UserAccount createAccount(
            String email,
            String username,
            String name,
            String lastName,
            String phone,
            String passwordHash,
            UserRole role,
            Instant emailVerifiedAt);

    Optional<User> findByEmail(final String email);

    Optional<UserAccount> findAccountByEmail(String email);

    Optional<User> findById(final Long id);

    Optional<UserAccount> findAccountById(Long id);

    Optional<User> findByUsername(final String username);

    void updatePasswordHash(Long id, String passwordHash);

    void markEmailVerified(Long id, Instant emailVerifiedAt);
}
