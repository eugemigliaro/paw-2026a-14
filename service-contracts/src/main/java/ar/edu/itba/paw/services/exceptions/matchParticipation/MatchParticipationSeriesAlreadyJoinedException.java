package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationSeriesAlreadyJoinedException extends MatchParticipationException {
    public MatchParticipationSeriesAlreadyJoinedException() {
        super("seriesAlreadyJoined");
    }
}
