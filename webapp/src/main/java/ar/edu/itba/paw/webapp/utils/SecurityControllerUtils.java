package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

public final class SecurityControllerUtils {

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

    public static User currentUserOrNull() {
        return CurrentAuthenticatedUser.get().map(AuthenticatedUserPrincipal::getUser).orElse(null);
    }
}
