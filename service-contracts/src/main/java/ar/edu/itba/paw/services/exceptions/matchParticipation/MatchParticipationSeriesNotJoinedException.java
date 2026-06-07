package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationSeriesNotJoinedException extends MatchParticipationException {
    public MatchParticipationSeriesNotJoinedException() {
        super("seriesNotJoined");
    }
}
