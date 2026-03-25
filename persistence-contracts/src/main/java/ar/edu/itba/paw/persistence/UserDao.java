package ar.itba.edu.paw.persistence;

import ar.edu.itba.paw.models.User;
import java.util.Optional;

public interface UserDao {

    User createUser(final String email, final String password, final String username);

    Optional<User> findByEmail(final String email);

    Optional<User> findById(final Long id);
}
