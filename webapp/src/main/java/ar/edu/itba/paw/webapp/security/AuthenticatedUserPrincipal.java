package ar.edu.itba.paw.webapp.security;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.types.UserRole;
import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticatedUserPrincipal implements Principal, UserDetails, Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private final User user;
    private final String email;
    private final String username;
    private final String passwordHash;
    private final UserRole role;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUserPrincipal(final UserAccount account) {
        this.user = account.toUser();
        this.email = account.getEmail();
        this.username = account.getUsername();
        this.passwordHash = account.getPasswordHash();
        this.role = account.getRole();
        this.authorities = SecurityAuthorities.forRole(account.getRole());
    }

    public AuthenticatedUserPrincipal(final User user, final UserRole role) {
        this.user = user;
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.passwordHash = null;
        this.role = role;
        this.authorities = SecurityAuthorities.forRole(role);
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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return passwordHash != null && !passwordHash.isBlank();
    }
}
