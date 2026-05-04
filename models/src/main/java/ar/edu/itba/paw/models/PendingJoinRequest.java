package ar.edu.itba.paw.models;

public class PendingJoinRequest {

    private final Match match;
    private final User user;
    private final boolean seriesRequest;

    public PendingJoinRequest(final Match match, final User user, final boolean seriesRequest) {
        this.match = match;
        this.user = user;
        this.seriesRequest = seriesRequest;
    }

    public Match getMatch() {
        return match;
    }

    public User getUser() {
        return user;
    }

    public boolean isSeriesRequest() {
        return seriesRequest;
    }

    @Override
    public String toString() {
        return "PendingJoinRequest{"
                + "matchId="
                + (match == null ? null : match.getId())
                + ", userId="
                + (user == null ? null : user.getId())
                + ", seriesRequest="
                + seriesRequest
                + '}';
    }
}
