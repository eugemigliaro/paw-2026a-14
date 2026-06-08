package ar.edu.itba.paw.models.exceptions.matchParticipation;

public class MatchParticipationSeriesAlreadyJoinedException extends MatchParticipationException {
    public MatchParticipationSeriesAlreadyJoinedException() {
        super("seriesAlreadyJoined");
    }
}
