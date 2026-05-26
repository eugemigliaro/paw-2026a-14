package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import java.time.Instant;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthenticationUtils {
    public static void authenticateUser(
            User user,
            String passwordHash,
            UserRole role,
            boolean isVerified,
            Object credentials,
            List<SimpleGrantedAuthority> authorities) {
        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(
                        new UserAccount(
                                user.getId(),
                                user.getEmail(),
                                user.getUsername(),
                                user.getName(),
                                user.getLastName(),
                                user.getPhone(),
                                null,
                                passwordHash,
                                role,
                                isVerified ? Instant.now().minusSeconds(10) : null,
                                UserLanguages.DEFAULT_LANGUAGE));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, credentials, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void authenticateUser(
            User user, String passwordHash, UserRole role, boolean isVerified) {
        authenticateUser(
                user,
                passwordHash,
                role,
                isVerified,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public static void authenticateUser(Long id, String username, String email) {
        final User user = new User(id, email, username, null, null, null, null, null);

        authenticateUser(user, "hashedPassword", UserRole.USER, true);
    }

    public static void authenticateUser(Long id) {
        authenticateUser(UserUtils.getUser(id), "hashedPassword", UserRole.USER, true);
    }

    public static void authenticateAdmin(Long id, Object credentials) {
        authenticateUser(
                UserUtils.getUser(id),
                "hashedPassword",
                UserRole.ADMIN_MOD,
                true,
                credentials,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN_MOD")));
    }
}
