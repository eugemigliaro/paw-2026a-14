package ar.edu.itba.paw.models;

public class MatchReservationPayload {

    private Long matchId;

    public MatchReservationPayload() {
        // Jackson constructor.
    }

    public MatchReservationPayload(final Long matchId) {
        this.matchId = matchId;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(final Long matchId) {
        this.matchId = matchId;
    }
}
