package ar.edu.itba.paw.services.exceptions;

import ar.edu.itba.paw.services.MatchCancellationFailureReason;

public class MatchCancellationException extends RuntimeException {

    private final MatchCancellationFailureReason reason;

    public MatchCancellationException(
            final MatchCancellationFailureReason reason, final String message) {
        super(message);
        this.reason = reason;
    }

    public MatchCancellationFailureReason getReason() {
        return reason;
    }
}
