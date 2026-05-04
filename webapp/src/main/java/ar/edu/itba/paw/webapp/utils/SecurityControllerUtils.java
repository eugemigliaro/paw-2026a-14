package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class SecurityControllerUtils {

    private SecurityControllerUtils() {}

    public static AuthenticatedUserPrincipal requireAuthenticatedPrincipal() {
        return CurrentAuthenticatedUser.get()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public static long requireAuthenticatedUserId() {
        return requireAuthenticatedPrincipal().getUserId();
    }

    public static Long currentUserIdOrNull() {
        return CurrentAuthenticatedUser.get()
                .map(AuthenticatedUserPrincipal::getUserId)
                .orElse(null);
    }
}
