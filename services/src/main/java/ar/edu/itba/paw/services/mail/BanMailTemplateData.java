package ar.edu.itba.paw.services.mail;

import java.time.Instant;
import java.util.Locale;

public class BanMailTemplateData {

    private final String recipientEmail;
    private final String username;
    private final Instant bannedUntil;
    private final String reason;
    private final String loginUrl;
    private final Locale locale;

    public BanMailTemplateData(
            final String recipientEmail,
            final String username,
            final Instant bannedUntil,
            final String reason,
            final String loginUrl,
            final Locale locale) {
        this.recipientEmail = recipientEmail;
        this.username = username;
        this.bannedUntil = bannedUntil;
        this.reason = reason;
        this.loginUrl = loginUrl;
        this.locale = locale;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getUsername() {
        return username;
    }

    public Instant getBannedUntil() {
        return bannedUntil;
    }

    public String getReason() {
        return reason;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public Locale getLocale() {
        return locale;
    }
}
