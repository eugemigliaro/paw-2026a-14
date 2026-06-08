package ar.edu.itba.paw.models.exceptions.match;

public class MatchSeriesClosedException extends MatchException {
    public MatchSeriesClosedException() {
        super("seriesClosed");
    }
}
