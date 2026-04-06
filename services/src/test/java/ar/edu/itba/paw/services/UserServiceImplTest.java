package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.UserDao;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @InjectMocks private UserServiceImpl userService;

    @Mock private UserDao userDao;

    @Test
    public void testFindByIdWhenUserExists() {
        final User user = new User(1L, "test", "test");
        Mockito.when(userDao.findById(1L)).thenReturn(Optional.of(user));

        final Optional<User> result = userService.findById(1L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1L, result.get().getId());
    }

    @Test
    public void testFindByIdWhenUserNotExists() {
        Mockito.when(userDao.findById(Mockito.anyLong())).thenReturn(Optional.empty());

        final Optional<User> result = userService.findById(1L);

        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testCreateUserWhenUserDoesNotExist() {
        final User user = new User(1L, "test", "test");
        Mockito.when(userDao.createUser(Mockito.anyString(), Mockito.anyString())).thenReturn(user);

        final User result = userService.createUser("test", "test");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1L, result.getId());
    }

    @Test
    public void testFindByUsernameDelegates() {
        final User user = new User(2L, "player@test.com", "player");
        Mockito.when(userDao.findByUsername("player")).thenReturn(Optional.of(user));

        final Optional<User> result = userService.findByUsername("player");

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("player", result.get().getUsername());
    }
}
