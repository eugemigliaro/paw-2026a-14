package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import ar.itba.edu.paw.persistence.UserDao;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    @Autowired
    public UserServiceImpl(final UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public User createUser(final String email, final String password, final String username) {
        return userDao.createUser(email, password, username);
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return userDao.findByEmail(email);
    }

    @Override
    public Optional<User> findById(final Long id) {
        return userDao.findById(id);
    }
}
