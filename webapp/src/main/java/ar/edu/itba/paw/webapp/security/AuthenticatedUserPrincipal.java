package ar.edu.itba.paw.webapp.security;

import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;

public class AuthenticatedUserPrincipal implements Principal, Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String email;
    private final String username;
    private final UserRole role;

    public AuthenticatedUserPrincipal(final UserAccount account) {
        this.userId = account.getId();
        this.email = account.getEmail();
        this.username = account.getUsername();
        this.role = account.getRole();
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String getName() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }
}
