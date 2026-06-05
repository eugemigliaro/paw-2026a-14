package ar.edu.itba.paw.services.mail;

import java.time.Instant;
import java.util.Locale;

public class TournamentLifecycleMailTemplateData {

    private final String recipientEmail;
    private final String tournamentTitle;
    private final String sportLabel;
    private final String statusLabel;
    private final String matchLabel;
    private final String winnerName;
    private final String loserName;
    private final String championName;
    private final String address;
    private final Instant startsAt;
    private final Locale locale;

    public TournamentLifecycleMailTemplateData(
            final String recipientEmail,
            final String tournamentTitle,
            final String sportLabel,
            final String statusLabel,
            final String matchLabel,
            final String winnerName,
            final String loserName,
            final String championName,
            final String address,
            final Instant startsAt,
            final Locale locale) {
        this.recipientEmail = recipientEmail;
        this.tournamentTitle = tournamentTitle;
        this.sportLabel = sportLabel;
        this.statusLabel = statusLabel;
        this.matchLabel = matchLabel;
        this.winnerName = winnerName;
        this.loserName = loserName;
        this.championName = championName;
        this.address = address;
        this.startsAt = startsAt;
        this.locale = locale;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getTournamentTitle() {
        return tournamentTitle;
    }

    public String getSportLabel() {
        return sportLabel;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getMatchLabel() {
        return matchLabel;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public String getLoserName() {
        return loserName;
    }

    public String getChampionName() {
        return championName;
    }

    public String getAddress() {
        return address;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Locale getLocale() {
        return locale;
    }
}
