package ar.edu.itba.paw.services;

import java.time.Instant;

public class PasswordResetPreview {

    private final String email;
    private final Instant expiresAt;

    public PasswordResetPreview(final String email, final Instant expiresAt) {
        this.email = email;
        this.expiresAt = expiresAt;
    }

    public String getEmail() {
        return email;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
