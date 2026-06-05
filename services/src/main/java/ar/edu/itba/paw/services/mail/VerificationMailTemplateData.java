package ar.edu.itba.paw.services.mail;

import ar.edu.itba.paw.services.VerificationPreviewDetail;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public class VerificationMailTemplateData {

    private final String title;
    private final String summary;
    private final String recipientEmail;
    private final String confirmationUrl;
    private final Instant expiresAt;
    private final List<VerificationPreviewDetail> details;
    private final Locale locale;

    public VerificationMailTemplateData(
            final String title,
            final String summary,
            final String recipientEmail,
            final String confirmationUrl,
            final Instant expiresAt,
            final List<VerificationPreviewDetail> details,
            final Locale locale) {
        this.title = title;
        this.summary = summary;
        this.recipientEmail = recipientEmail;
        this.confirmationUrl = confirmationUrl;
        this.expiresAt = expiresAt;
        this.details = details;
        this.locale = locale;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getConfirmationUrl() {
        return confirmationUrl;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public List<VerificationPreviewDetail> getDetails() {
        return details;
    }

    public Locale getLocale() {
        return locale;
    }
}
