package ar.edu.itba.paw.services.exceptions.match;

public class MatchSeriesStartedException extends MatchException {
    public MatchSeriesStartedException() {
        super("join.error.seriesStarted");
    }
}
