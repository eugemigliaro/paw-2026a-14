package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.exceptions.AccountRegistrationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
    @Mock private ImageService imageService;
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
    public void testUpdateProfileUpdatesExistingUser() throws IOException {
        final User existingUser =
                new User(2L, "old@test.com", "old_user", "Old", "Name", "+1 111 111 1111", 14L);
        Mockito.when(userDao.findById(2L)).thenReturn(Optional.of(existingUser));
        Mockito.when(userDao.findByUsername("new_user")).thenReturn(Optional.empty());

        final User result =
                userService.updateProfile(
                        2L, "new_user", "Jamie", "Rivera", "+1 555 123 4567", null, 0L, null);

        Assertions.assertEquals("old@test.com", result.getEmail());
        Assertions.assertEquals("new_user", result.getUsername());
        Assertions.assertEquals("Jamie", result.getName());
        Assertions.assertEquals("Rivera", result.getLastName());
        Assertions.assertEquals("+1 555 123 4567", result.getPhone());
        Assertions.assertEquals(14L, result.getProfileImageId());
    }

    @Test
    public void testUpdateProfileRejectsTakenUsername() {
        final User existingUser =
                new User(2L, "old@test.com", "old_user", "Old", "Name", "+1 111 111 1111", null);
        final User conflictingUser =
                new User(3L, "other@test.com", "new_user", "Other", "User", null, null);
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
                () ->
                        userService.updateProfile(
                                2L, "new_user", "Jamie", "Rivera", "", null, 0L, null));
    }

    @Test
    public void testUpdateProfileImageStoresImageAndReturnsUpdatedUser() throws IOException {
        final User existingUser =
                new User(2L, "old@test.com", "old_user", "Old", "Name", "+1 111 111 1111", null);
        Mockito.when(userDao.findById(2L)).thenReturn(Optional.of(existingUser));
        Mockito.when(imageService.store(Mockito.eq("image/png"), Mockito.eq(3L), Mockito.any()))
                .thenReturn(99L);

        final User result =
                userService.updateProfileImage(
                        2L, "image/png", 3L, new ByteArrayInputStream(new byte[] {1, 2, 3}));

        Assertions.assertEquals(99L, result.getProfileImageId());
        Assertions.assertEquals("old_user", result.getUsername());
    }

    @Test
    public void testUpdateProfileRejectsInvalidUsername() {
        final User existingUser = new User(1L, "test@test.com", "valid");
        Mockito.when(userDao.findById(1L)).thenReturn(Optional.of(existingUser));

        Assertions.assertThrows(
                AccountRegistrationException.class,
                () -> userService.updateProfile(1L, "a", "Name", "Last", null, null, 0, null));
    }

    @Test
    public void testUpdateProfileRejectsEmptyName() {
        final User existingUser = new User(1L, "test@test.com", "valid");
        Mockito.when(userDao.findById(1L)).thenReturn(Optional.of(existingUser));

        Assertions.assertThrows(
                AccountRegistrationException.class,
                () -> userService.updateProfile(1L, "valid", " ", "Last", null, null, 0, null));
    }

    @Test
    public void testUpdateProfileRejectsInvalidPhone() {
        final User existingUser = new User(1L, "test@test.com", "valid");
        Mockito.when(userDao.findById(1L)).thenReturn(Optional.of(existingUser));

        Assertions.assertThrows(
                AccountRegistrationException.class,
                () -> userService.updateProfile(1L, "valid", "Name", "Last", "abc", null, 0, null));
    }

    @Test
    public void testUpdateProfileWithNewImage() throws IOException {
        final User existingUser = new User(1L, "test@test.com", "valid");
        Mockito.when(userDao.findById(1L)).thenReturn(Optional.of(existingUser));
        Mockito.when(imageService.store(Mockito.any(), Mockito.anyLong(), Mockito.any()))
                .thenReturn(100L);

        final User result =
                userService.updateProfile(
                        1L,
                        "valid",
                        "Name",
                        "Last",
                        null,
                        "image/png",
                        10,
                        new ByteArrayInputStream(new byte[10]));

        Assertions.assertEquals(100L, result.getProfileImageId());
        Mockito.verify(userDao)
                .updateProfile(
                        Mockito.eq(1L),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.eq(100L));
    }
}
