package ar.edu.itba.paw.models.exceptions.matchParticipation;

public class MatchParticipationSeriesNotJoinedException extends MatchParticipationException {
    public MatchParticipationSeriesNotJoinedException() {
        super("seriesNotJoined");
    }
}
