package ar.edu.itba.paw.services;

import java.time.Instant;
import java.util.List;

public class VerificationPreview {

    private final String title;
    private final String summary;
    private final String email;
    private final Instant expiresAt;
    private final String confirmLabel;
    private final String successRedirectUrl;
    private final List<VerificationPreviewDetail> details;

    public VerificationPreview(
            final String title,
            final String summary,
            final String email,
            final Instant expiresAt,
            final String confirmLabel,
            final String successRedirectUrl,
            final List<VerificationPreviewDetail> details) {
        this.title = title;
        this.summary = summary;
        this.email = email;
        this.expiresAt = expiresAt;
        this.confirmLabel = confirmLabel;
        this.successRedirectUrl = successRedirectUrl;
        this.details = details;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getEmail() {
        return email;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getConfirmLabel() {
        return confirmLabel;
    }

    public String getSuccessRedirectUrl() {
        return successRedirectUrl;
    }

    public List<VerificationPreviewDetail> getDetails() {
        return details;
    }
}
