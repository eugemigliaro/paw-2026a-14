package ar.edu.itba.paw.webapp.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.services.AccountAuthService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class AccountUserDetailsServiceTest {

    @Test
    void loadUserByUsernameReturnsPrincipalForVerifiedPasswordAccount() {
        // 1. Arrange
        final AccountAuthService accountAuthService = Mockito.mock(AccountAuthService.class);
        final UserAccount account =
                new UserAccount(
                        8L,
                        "player@test.com",
                        "player_one",
                        "Player",
                        "One",
                        null,
                        null,
                        "{bcrypt}hash",
                        UserRole.ADMIN_MOD,
                        Instant.parse("2026-04-10T18:00:00Z"),
                        UserLanguages.DEFAULT_LANGUAGE);
        Mockito.when(accountAuthService.findAccountByEmail("player@test.com"))
                .thenReturn(Optional.of(account));
        final AccountUserDetailsService service = new AccountUserDetailsService(accountAuthService);

        // 2. Exercise
        final AuthenticatedUserPrincipal principal =
                (AuthenticatedUserPrincipal) service.loadUserByUsername("  PLAYER@Test.com  ");

        // 3. Assert
        assertEquals("player@test.com", principal.getUsername());
        assertEquals("player_one", principal.getName());
        assertEquals("{bcrypt}hash", principal.getPassword());
        assertTrue(principal.isEnabled());
        assertTrue(
                principal.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN_MOD".equals(authority.getAuthority())));
    }

    @Test
    void loadUserByUsernameRejectsUnverifiedAccount() {
        // 1. Arrange
        final AccountAuthService accountAuthService = Mockito.mock(AccountAuthService.class);
        Mockito.when(accountAuthService.findAccountByEmail("pending@test.com"))
                .thenReturn(
                        Optional.of(
                                new UserAccount(
                                        9L,
                                        "pending@test.com",
                                        "pending",
                                        null,
                                        null,
                                        null,
                                        null,
                                        "{bcrypt}hash",
                                        UserRole.USER,
                                        null,
                                        UserLanguages.DEFAULT_LANGUAGE)));
        final AccountUserDetailsService service = new AccountUserDetailsService(accountAuthService);

        // 2. Exercise + 3. Assert
        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("pending@test.com"));
    }

    @Test
    void loadUserByUsernameRejectsAccountWithoutPassword() {
        // 1. Arrange
        final AccountAuthService accountAuthService = Mockito.mock(AccountAuthService.class);
        Mockito.when(accountAuthService.findAccountByEmail("legacy@test.com"))
                .thenReturn(
                        Optional.of(
                                new UserAccount(
                                        10L,
                                        "legacy@test.com",
                                        "legacy",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        UserRole.USER,
                                        Instant.parse("2026-04-10T18:00:00Z"),
                                        UserLanguages.DEFAULT_LANGUAGE)));
        final AccountUserDetailsService service = new AccountUserDetailsService(accountAuthService);

        // 2. Exercise + 3. Assert
        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("legacy@test.com"));
    }
}
