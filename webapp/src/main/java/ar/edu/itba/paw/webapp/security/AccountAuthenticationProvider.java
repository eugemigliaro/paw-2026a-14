package ar.edu.itba.paw.webapp.security;

import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.services.AccountAuthService;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AccountAuthenticationProvider implements AuthenticationProvider {

    private final AccountAuthService accountAuthService;
    private final PasswordEncoder passwordEncoder;

    public AccountAuthenticationProvider(
            final AccountAuthService accountAuthService, final PasswordEncoder passwordEncoder) {
        this.accountAuthService = accountAuthService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(final Authentication authentication)
            throws AuthenticationException {
        final String email = normalizeEmail(authentication.getName());
        final String password =
                authentication.getCredentials() == null
                        ? ""
                        : authentication.getCredentials().toString();

        final UserAccount account =
                accountAuthService
                        .findAccountByEmail(email)
                        .orElseThrow(() -> new BadCredentialsException("invalid"));

        if (!account.isEmailVerified()) {
            throw new EmailNotVerifiedAuthenticationException();
        }

        if (!account.hasPassword()) {
            throw new PasswordSetupRequiredAuthenticationException();
        }

        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BadCredentialsException("invalid");
        }

        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(account),
                null,
                SecurityAuthorities.forRole(account.getRole()));
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private String normalizeEmail(final String email) {
        if (email == null || email.isBlank()) {
            throw new BadCredentialsException("invalid");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
