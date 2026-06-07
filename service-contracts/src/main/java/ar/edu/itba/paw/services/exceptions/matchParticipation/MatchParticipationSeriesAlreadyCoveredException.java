package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationSeriesAlreadyCoveredException extends MatchParticipationException {
    public MatchParticipationSeriesAlreadyCoveredException() {
        super("seriesAlreadyCovered");
    }
}
