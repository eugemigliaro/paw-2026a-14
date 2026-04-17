package ar.edu.itba.paw.webapp.security;

import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.services.AccountAuthService;
import java.util.List;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
                        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!account.isEmailVerified()) {
            throw new EmailNotVerifiedAuthenticationException("Email verification required");
        }

        if (!account.hasPassword()) {
            throw new PasswordSetupRequiredAuthenticationException(
                    "Password setup required before login");
        }

        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(account), null, authoritiesFor(account.getRole()));
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static String normalizeEmail(final String email) {
        if (email == null || email.isBlank()) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static List<GrantedAuthority> authoritiesFor(final UserRole role) {
        if (role == UserRole.ADMIN_MOD) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN_MOD"),
                    new SimpleGrantedAuthority("ROLE_USER"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
