package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.persistence.UserDao;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class AdminBootstrapServiceImplTest {

    @Mock private UserDao userDao;
    @Mock private Clock clock;

    private PasswordEncoder passwordEncoder;
    private AdminBootstrapServiceImpl service;
    private static final Instant FIXED_NOW = Instant.parse("2026-04-27T15:00:00Z");

    @BeforeEach
    public void setUp() {
        Mockito.lenient().when(clock.instant()).thenReturn(FIXED_NOW);
        Mockito.lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    public void bootstrapSkipsWhenConfigurationContainsUnresolvedPlaceholder() {
        Mockito.when(userDao.findAccountByEmail("${admin.bootstrap.email}"))
                .thenReturn(java.util.Optional.empty());
        service =
                new AdminBootstrapServiceImpl(
                        userDao,
                        clock,
                        "${admin.bootstrap.email}",
                        "admin",
                        "Admin",
                        "User",
                        "hash",
                        passwordEncoder);

        service.bootstrapFromConfiguration();

        Assertions.assertTrue(userDao.findAccountByEmail("${admin.bootstrap.email}").isEmpty());
    }

    @Test
    public void bootstrapCreatesAdminWhenConfigurationIsPresent() {
        Mockito.when(userDao.findAccountByEmail("admin@test.com"))
                .thenReturn(
                        Optional.of(
                                new UserAccount(
                                        1L,
                                        "admin@test.com",
                                        "adminUser",
                                        "Admin",
                                        "User",
                                        null,
                                        null,
                                        passwordEncoder.encode("password"),
                                        UserRole.ADMIN_MOD,
                                        FIXED_NOW)));

        service =
                new AdminBootstrapServiceImpl(
                        userDao,
                        clock,
                        "admin@test.com",
                        "adminUser",
                        "Admin",
                        "User",
                        "password",
                        passwordEncoder);

        service.bootstrapFromConfiguration();

        Optional<UserAccount> created = userDao.findAccountByEmail("admin@test.com");

        Assertions.assertTrue(created.isPresent());
        Assertions.assertEquals("admin@test.com", created.get().getEmail());
        Assertions.assertEquals("adminUser", created.get().getUsername());
        Assertions.assertEquals("Admin", created.get().getName());
        Assertions.assertEquals("User", created.get().getLastName());
        Assertions.assertNull(created.get().getPhone());
        Assertions.assertTrue(passwordEncoder.matches("password", created.get().getPasswordHash()));
        Assertions.assertEquals(UserRole.ADMIN_MOD, created.get().getRole());
        Assertions.assertEquals(FIXED_NOW, created.get().getEmailVerifiedAt());
    }
}
