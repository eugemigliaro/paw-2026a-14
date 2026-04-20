package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.exceptions.AccountRegistrationException;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @InjectMocks private UserServiceImpl userService;

    @Mock private UserDao userDao;
    @Mock private MessageSource messageSource;

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

    @Test
    public void testUpdateProfileUpdatesExistingUser() {
        final User existingUser =
                new User(2L, "old@test.com", "old_user", "Old", "Name", "+1 111 111 1111");
        Mockito.when(userDao.findById(2L)).thenReturn(Optional.of(existingUser));
        Mockito.when(userDao.findByUsername("new_user")).thenReturn(Optional.empty());

        final User result =
                userService.updateProfile(2L, "new_user", "Jamie", "Rivera", "+1 555 123 4567");

        Assertions.assertEquals("old@test.com", result.getEmail());
        Assertions.assertEquals("new_user", result.getUsername());
        Assertions.assertEquals("Jamie", result.getName());
        Assertions.assertEquals("Rivera", result.getLastName());
        Assertions.assertEquals("+1 555 123 4567", result.getPhone());
    }

    @Test
    public void testUpdateProfileRejectsTakenUsername() {
        final User existingUser =
                new User(2L, "old@test.com", "old_user", "Old", "Name", "+1 111 111 1111");
        final User conflictingUser =
                new User(3L, "other@test.com", "new_user", "Other", "User", null);
        Mockito.when(userDao.findById(2L)).thenReturn(Optional.of(existingUser));
        Mockito.when(userDao.findByUsername("new_user")).thenReturn(Optional.of(conflictingUser));
        Mockito.when(
                        messageSource.getMessage(
                                Mockito.anyString(),
                                Mockito.isNull(),
                                Mockito.any(),
                                Mockito.any()))
                .thenAnswer(invocation -> invocation.getArgument(0, String.class));

        Assertions.assertThrows(
                AccountRegistrationException.class,
                () -> userService.updateProfile(2L, "new_user", "Jamie", "Rivera", ""));
    }
}
