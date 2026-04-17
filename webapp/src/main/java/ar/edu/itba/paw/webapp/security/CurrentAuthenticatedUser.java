package ar.edu.itba.paw.webapp.security;

import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentAuthenticatedUser {

    private CurrentAuthenticatedUser() {
        // Static access helper.
    }

    public static Optional<AuthenticatedUserPrincipal> get() {
        final Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal)) {
            return Optional.empty();
        }

        return Optional.of((AuthenticatedUserPrincipal) authentication.getPrincipal());
    }
}
