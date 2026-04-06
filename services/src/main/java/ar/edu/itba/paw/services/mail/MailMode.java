package ar.edu.itba.paw.services.mail;

import java.util.Locale;

public enum MailMode {
    LOG,
    SMTP;

    public static MailMode fromProperty(final String value) {
        if (value == null || value.isBlank()) {
            return LOG;
        }

        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "smtp":
                return SMTP;
            case "log":
            default:
                return LOG;
        }
    }
}
