package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;

public final class SecurityControllerUtils {

    private static final SecurityContextRepository SECURITY_CONTEXT_REPOSITORY =
            new HttpSessionSecurityContextRepository();

    private SecurityControllerUtils() {}

    public static AuthenticatedUserPrincipal requireAuthenticatedPrincipal() {
        return CurrentAuthenticatedUser.get()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public static User requireAuthenticatedUser() {
        return requireAuthenticatedPrincipal().getUser();
    }

    public static void refreshAuthentication(final User user) {
        final Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        final AuthenticatedUserPrincipal principal =
                SecurityControllerUtils.requireAuthenticatedPrincipal();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUserPrincipal(user, principal.getRole()),
                                authentication.getCredentials(),
                                authentication.getAuthorities()));
    }

    public static void authenticateVerifiedAccount(
            final UserAccount account,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        final Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUserPrincipal(account),
                        null,
                        authoritiesFor(account.getRole()));

        final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        SECURITY_CONTEXT_REPOSITORY.saveContext(securityContext, request, response);
    }

    private static List<GrantedAuthority> authoritiesFor(final UserRole role) {
        if (role != null && role.isAdmin()) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN_MOD"),
                    new SimpleGrantedAuthority("ROLE_USER"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public static User currentUserOrNull() {
        return CurrentAuthenticatedUser.get().map(AuthenticatedUserPrincipal::getUser).orElse(null);
    }
}
