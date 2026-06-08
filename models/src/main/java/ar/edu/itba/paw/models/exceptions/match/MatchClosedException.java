package ar.edu.itba.paw.models.exceptions.match;

public class MatchClosedException extends MatchException {
    public MatchClosedException() {
        super("closed");
    }
}
