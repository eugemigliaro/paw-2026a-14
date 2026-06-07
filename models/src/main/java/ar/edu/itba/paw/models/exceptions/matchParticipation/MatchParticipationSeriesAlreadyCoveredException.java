package ar.edu.itba.paw.models.exceptions.matchParticipation;

public class MatchParticipationSeriesAlreadyCoveredException extends MatchParticipationException {
    public MatchParticipationSeriesAlreadyCoveredException() {
        super("seriesAlreadyCovered");
    }
}
