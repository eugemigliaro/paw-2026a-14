package ar.edu.itba.paw.services.exceptions.match;

public class MatchSeriesFullException extends MatchException {
    public MatchSeriesFullException() {
        super("host.invites.error.seriesFull");
    }
}
