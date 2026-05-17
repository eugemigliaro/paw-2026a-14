package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.types.UserRole;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserDao {

    User createUser(final String email, final String username);

    UserAccount createAccount(
            String email,
            String username,
            String name,
            String lastName,
            String phone,
            String preferredLanguage,
            String passwordHash,
            UserRole role,
            Instant emailVerifiedAt);

    Optional<User> findByEmail(final String email);

    Optional<UserAccount> findAccountByEmail(String email);

    Optional<User> findById(final Long id);

    List<User> findByIds(Collection<Long> ids);

    Optional<UserAccount> findAccountById(Long id);

    Optional<User> findByUsername(final String username);

    void updateProfile(
            Long id,
            String username,
            String name,
            String lastName,
            String phone,
            ImageMetadata profileImageMetadata);

    void updateProfileImage(Long id, ImageMetadata profileImageMetadata);

    void updatePasswordHash(Long id, String passwordHash);

    void markEmailVerified(Long id, Instant emailVerifiedAt);

    void updatePreferredLanguage(Long id, String preferredLanguage);
}
