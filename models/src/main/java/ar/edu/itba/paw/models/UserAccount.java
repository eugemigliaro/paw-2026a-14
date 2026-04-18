package ar.edu.itba.paw.models;

import java.time.Instant;

public class UserAccount {

    private final Long id;
    private final String email;
    private final String username;
    private final String passwordHash;
    private final UserRole role;
    private final Instant emailVerifiedAt;

    public UserAccount(
            final Long id,
            final String email,
            final String username,
            final String passwordHash,
            final UserRole role,
            final Instant emailVerifiedAt) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }
}
