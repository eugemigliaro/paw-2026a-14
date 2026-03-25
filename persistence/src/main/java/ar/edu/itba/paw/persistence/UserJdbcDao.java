package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import ar.itba.edu.paw.persistence.UserDao;
import org.springframework.stereotype.Repository;

@Repository
public class UserJdbcDao implements UserDao {

    @Override
    public User createUser(final String email, final String password, final String username) {
        return new User(email, password, username);
    }
}
