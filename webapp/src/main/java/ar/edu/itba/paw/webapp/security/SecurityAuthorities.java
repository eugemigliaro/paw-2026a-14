package ar.edu.itba.paw.webapp.security;

import ar.edu.itba.paw.models.types.UserRole;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class SecurityAuthorities {

    public static final String USER = "ROLE_USER";
    public static final String ADMIN_MOD = "ROLE_ADMIN_MOD";

    private SecurityAuthorities() {}

    public static List<GrantedAuthority> forRole(final UserRole role) {
        if (role != null && role.isAdmin()) {
            return List.of(new SimpleGrantedAuthority(ADMIN_MOD), new SimpleGrantedAuthority(USER));
        }
        return List.of(new SimpleGrantedAuthority(USER));
    }
}
