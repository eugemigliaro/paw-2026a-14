package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationSeriesAlreadyPendingException extends MatchParticipationException {
    public MatchParticipationSeriesAlreadyPendingException() {
        super("seriesAlreadyPending");
    }
}
