package ar.edu.itba.paw.services.exceptions.matchCancelation;

public class MatchCancellationForbiddenException extends MatchCancellationException {
    public MatchCancellationForbiddenException(final String message) {
        super(message);
    }
}
