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
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
public class MvpIdentityServiceImplTest {

    @InjectMocks private MvpIdentityServiceImpl mvpIdentityService;

    @Mock private UserDao userDao;

    @Test
    public void testResolveOrCreateByEmailReturnsExistingUser() {
        final User user = new User(1L, "existing@test.com", "existing");
        Mockito.when(userDao.findByEmail("existing@test.com")).thenReturn(Optional.of(user));

        final User result = mvpIdentityService.resolveOrCreateByEmail("existing@test.com");

        Assertions.assertEquals(user, result);
        Mockito.verify(userDao, Mockito.never())
                .createUser(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testResolveOrCreateByEmailCreatesUserWithGeneratedUsername() {
        final User user = new User(2L, "new.user@test.com", "new_user");
        Mockito.when(userDao.findByEmail("new.user@test.com")).thenReturn(Optional.empty());
        Mockito.when(userDao.findByUsername("new_user")).thenReturn(Optional.empty());
        Mockito.when(userDao.createUser("new.user@test.com", "new_user")).thenReturn(user);

        final User result = mvpIdentityService.resolveOrCreateByEmail("New.User@Test.com");

        Assertions.assertEquals(user, result);
        Mockito.verify(userDao).createUser("new.user@test.com", "new_user");
    }

    @Test
    public void testResolveOrCreateByEmailUsesSuffixWhenUsernameExists() {
        final User user = new User(3L, "player@test.com", "player1");
        Mockito.when(userDao.findByEmail("player@test.com")).thenReturn(Optional.empty());
        Mockito.when(userDao.findByUsername("player"))
                .thenReturn(Optional.of(new User(2L, "other@test.com", "player")));
        Mockito.when(userDao.findByUsername("player1")).thenReturn(Optional.empty());
        Mockito.when(userDao.createUser("player@test.com", "player1")).thenReturn(user);

        final User result = mvpIdentityService.resolveOrCreateByEmail("player@test.com");

        Assertions.assertEquals(user, result);
        Mockito.verify(userDao).createUser("player@test.com", "player1");
    }

    @Test
    public void testResolveOrCreateByEmailReturnsExistingUserAfterConcurrentInsert() {
        final User existingUser = new User(4L, "race@test.com", "race");
        Mockito.when(userDao.findByEmail("race@test.com"))
                .thenReturn(Optional.empty(), Optional.of(existingUser));
        Mockito.when(userDao.findByUsername("race")).thenReturn(Optional.empty());
        Mockito.when(userDao.createUser("race@test.com", "race"))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        final User result = mvpIdentityService.resolveOrCreateByEmail("race@test.com");

        Assertions.assertEquals(existingUser, result);
    }
}
