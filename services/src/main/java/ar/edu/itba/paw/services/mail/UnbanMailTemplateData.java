package ar.edu.itba.paw.services.mail;

import java.util.Locale;

public class UnbanMailTemplateData {

    private final String recipientEmail;
    private final String username;
    private final String loginUrl;
    private final Locale locale;

    public UnbanMailTemplateData(
            final String recipientEmail,
            final String username,
            final String loginUrl,
            final Locale locale) {
        this.recipientEmail = recipientEmail;
        this.username = username;
        this.loginUrl = loginUrl;
        this.locale = locale;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getUsername() {
        return username;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public Locale getLocale() {
        return locale;
    }
}
