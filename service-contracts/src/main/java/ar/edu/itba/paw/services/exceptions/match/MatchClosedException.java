package ar.edu.itba.paw.services.exceptions.match;

public class MatchClosedException extends MatchException {
    public MatchClosedException() {
        super("closed");
    }
}
