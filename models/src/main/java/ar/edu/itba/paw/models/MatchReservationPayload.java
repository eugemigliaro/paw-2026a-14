package ar.edu.itba.paw.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MatchReservationPayload {

    private final Long matchId;

    @JsonCreator
    public MatchReservationPayload(@JsonProperty("matchId") final Long matchId) {
        this.matchId = matchId;
    }

    public Long getMatchId() {
        return matchId;
    }
}
