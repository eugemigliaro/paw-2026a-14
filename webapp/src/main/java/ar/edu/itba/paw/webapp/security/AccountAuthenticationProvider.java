package ar.edu.itba.paw.webapp.security;

import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.services.AccountAuthService;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
    private final MessageSource messageSource;

    public AccountAuthenticationProvider(
            final AccountAuthService accountAuthService,
            final PasswordEncoder passwordEncoder,
            final MessageSource messageSource) {
        this.accountAuthService = accountAuthService;
        this.passwordEncoder = passwordEncoder;
        this.messageSource = messageSource;
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
                        .orElseThrow(
                                () ->
                                        new BadCredentialsException(
                                                messageSource.getMessage(
                                                        "auth.invalid_credentials",
                                                        null,
                                                        LocaleContextHolder.getLocale())));

        if (!account.isEmailVerified()) {
            throw new EmailNotVerifiedAuthenticationException(
                    messageSource.getMessage(
                            "auth.email_not_verified", null, LocaleContextHolder.getLocale()));
        }

        if (!account.hasPassword()) {
            throw new PasswordSetupRequiredAuthenticationException(
                    messageSource.getMessage(
                            "auth.password_setup_required", null, LocaleContextHolder.getLocale()));
        }

        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BadCredentialsException(
                    messageSource.getMessage(
                            "auth.invalid_credentials", null, LocaleContextHolder.getLocale()));
        }

        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(account), null, authoritiesFor(account.getRole()));
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private String normalizeEmail(final String email) {
        if (email == null || email.isBlank()) {
            throw new BadCredentialsException(
                    messageSource.getMessage(
                            "auth.invalid_credentials", null, LocaleContextHolder.getLocale()));
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static List<GrantedAuthority> authoritiesFor(final UserRole role) {
        if (role != null && role.isAdmin()) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN_MOD"),
                    new SimpleGrantedAuthority("ROLE_USER"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
