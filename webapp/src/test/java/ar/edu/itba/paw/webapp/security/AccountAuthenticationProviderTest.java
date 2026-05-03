package ar.edu.itba.paw.webapp.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.services.AccountAuthService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

class AccountAuthenticationProviderTest {

    @Test
    void authenticateReturnsPrincipalAndAuthoritiesForVerifiedAccount() {
        final AccountAuthService accountAuthService = Mockito.mock(AccountAuthService.class);
        final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        final MessageSource messageSource = Mockito.mock(MessageSource.class);
        final UserAccount account =
                new UserAccount(
                        3L,
                        "player@test.com",
                        "player_one",
                        "{bcrypt}hash",
                        UserRole.ADMIN_MOD,
                        Instant.parse("2026-04-10T18:00:00Z"));

        Mockito.when(accountAuthService.findAccountByEmail("player@test.com"))
                .thenReturn(Optional.of(account));
        Mockito.when(passwordEncoder.matches("Password123!", "{bcrypt}hash")).thenReturn(true);

        final AccountAuthenticationProvider provider =
                new AccountAuthenticationProvider(
                        accountAuthService, passwordEncoder, messageSource);

        final Authentication authentication =
                provider.authenticate(
                        new UsernamePasswordAuthenticationToken("player@test.com", "Password123!"));

        assertTrue(authentication.isAuthenticated());
        assertEquals("player_one", authentication.getName());
        assertTrue(
                authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN_MOD".equals(authority.getAuthority())));
        assertTrue(
                authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority())));
    }

    @Test
    void authenticateRejectsUnverifiedAccounts() {
        final AccountAuthService accountAuthService = Mockito.mock(AccountAuthService.class);
        final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        final MessageSource messageSource = Mockito.mock(MessageSource.class);

        Mockito.when(accountAuthService.findAccountByEmail("pending@test.com"))
                .thenReturn(
                        Optional.of(
                                new UserAccount(
                                        4L,
                                        "pending@test.com",
                                        "pending",
                                        "{bcrypt}hash",
                                        UserRole.USER,
                                        null)));

        final AccountAuthenticationProvider provider =
                new AccountAuthenticationProvider(
                        accountAuthService, passwordEncoder, messageSource);

        assertThrows(
                EmailNotVerifiedAuthenticationException.class,
                () ->
                        provider.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                        "pending@test.com", "Password123!")));
    }

    @Test
    void authenticateRejectsAccountsWithoutPasswordHash() {
        final AccountAuthService accountAuthService = Mockito.mock(AccountAuthService.class);
        final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        final MessageSource messageSource = Mockito.mock(MessageSource.class);

        Mockito.when(accountAuthService.findAccountByEmail("legacy@test.com"))
                .thenReturn(
                        Optional.of(
                                new UserAccount(
                                        5L,
                                        "legacy@test.com",
                                        "legacy",
                                        null,
                                        UserRole.USER,
                                        Instant.parse("2026-04-10T18:00:00Z"))));

        final AccountAuthenticationProvider provider =
                new AccountAuthenticationProvider(
                        accountAuthService, passwordEncoder, messageSource);

        assertThrows(
                PasswordSetupRequiredAuthenticationException.class,
                () ->
                        provider.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                        "legacy@test.com", "Password123!")));
    }

    @Test
    void authenticateRejectsInvalidCredentials() {
        final AccountAuthService accountAuthService = Mockito.mock(AccountAuthService.class);
        final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        final MessageSource messageSource = Mockito.mock(MessageSource.class);
        final UserAccount account =
                new UserAccount(
                        6L,
                        "player@test.com",
                        "player_one",
                        "{bcrypt}hash",
                        UserRole.USER,
                        Instant.parse("2026-04-10T18:00:00Z"));

        Mockito.when(accountAuthService.findAccountByEmail("player@test.com"))
                .thenReturn(Optional.of(account));
        Mockito.when(passwordEncoder.matches("WrongPassword!", "{bcrypt}hash")).thenReturn(false);

        final AccountAuthenticationProvider provider =
                new AccountAuthenticationProvider(
                        accountAuthService, passwordEncoder, messageSource);

        assertThrows(
                BadCredentialsException.class,
                () ->
                        provider.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                        "player@test.com", "WrongPassword!")));
    }
}
