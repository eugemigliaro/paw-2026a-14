package ar.edu.itba.paw.webapp.security;

import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.services.AccountAuthService;
import java.util.Locale;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class AccountUserDetailsService implements UserDetailsService {

    private final AccountAuthService accountAuthService;

    public AccountUserDetailsService(final AccountAuthService accountAuthService) {
        this.accountAuthService = accountAuthService;
    }

    @Override
    public UserDetails loadUserByUsername(final String email) throws UsernameNotFoundException {
        final String normalizedEmail = normalizeEmail(email);
        final UserAccount account =
                accountAuthService
                        .findAccountByEmail(normalizedEmail)
                        .filter(UserAccount::isEmailVerified)
                        .filter(UserAccount::hasPassword)
                        .orElseThrow(
                                () ->
                                        new UsernameNotFoundException(
                                                "Account not found for remember-me login"));

        return new AuthenticatedUserPrincipal(account);
    }

    private static String normalizeEmail(final String email) {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("Account not found for remember-me login");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
