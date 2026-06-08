package ar.edu.itba.paw.services;

public class MatchInvitationResult {

    private final boolean seriesInvitation;

    public MatchInvitationResult(final boolean seriesInvitation) {
        this.seriesInvitation = seriesInvitation;
    }

    public static MatchInvitationResult singleMatch() {
        return new MatchInvitationResult(false);
    }

    public static MatchInvitationResult series() {
        return new MatchInvitationResult(true);
    }

    public boolean isSeriesInvitation() {
        return seriesInvitation;
    }
}
