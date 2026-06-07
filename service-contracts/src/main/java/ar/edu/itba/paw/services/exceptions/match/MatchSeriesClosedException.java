package ar.edu.itba.paw.services.exceptions.match;

public class MatchSeriesClosedException extends MatchException {
    public MatchSeriesClosedException() {
        super("seriesClosed");
    }
}
