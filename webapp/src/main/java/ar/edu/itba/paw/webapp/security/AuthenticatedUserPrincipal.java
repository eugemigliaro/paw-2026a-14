package ar.edu.itba.paw.webapp.security;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.types.UserRole;
import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;

public class AuthenticatedUserPrincipal implements Principal, Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private final User user;
    private final String email;
    private final String username;
    private final UserRole role;

    public AuthenticatedUserPrincipal(final UserAccount account) {
        this.user = account.toUser();
        this.email = account.getEmail();
        this.username = account.getUsername();
        this.role = account.getRole();
    }

    public AuthenticatedUserPrincipal(final User user, final UserRole role) {
        this.user = user;
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.role = role;
    }

    public User getUser() {
        return user;
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
