package ar.edu.itba.paw.services.mail;

import java.time.Instant;
import java.util.Locale;

public class MatchLifecycleMailTemplateData {

    private final String recipientEmail;
    private final String matchTitle;
    private final String address;
    private final Instant startsAt;
    private final Instant endsAt;
    private final String sportLabel;
    private final String statusLabel;
    private final String actorName;
    private final Locale locale;

    public MatchLifecycleMailTemplateData(
            final String recipientEmail,
            final String matchTitle,
            final String address,
            final Instant startsAt,
            final Instant endsAt,
            final String sportLabel,
            final String statusLabel,
            final Locale locale) {
        this(
                recipientEmail,
                matchTitle,
                address,
                startsAt,
                endsAt,
                sportLabel,
                statusLabel,
                null,
                locale);
    }

    public MatchLifecycleMailTemplateData(
            final String recipientEmail,
            final String matchTitle,
            final String address,
            final Instant startsAt,
            final Instant endsAt,
            final String sportLabel,
            final String statusLabel,
            final String actorName,
            final Locale locale) {
        this.recipientEmail = recipientEmail;
        this.matchTitle = matchTitle;
        this.address = address;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.sportLabel = sportLabel;
        this.statusLabel = statusLabel;
        this.actorName = actorName;
        this.locale = locale;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getMatchTitle() {
        return matchTitle;
    }

    public String getAddress() {
        return address;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public String getSportLabel() {
        return sportLabel;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getActorName() {
        return actorName;
    }

    public Locale getLocale() {
        return locale;
    }
}
