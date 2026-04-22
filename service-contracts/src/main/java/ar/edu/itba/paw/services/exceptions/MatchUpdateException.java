package ar.edu.itba.paw.services.exceptions;

import ar.edu.itba.paw.services.MatchUpdateFailureReason;

public class MatchUpdateException extends RuntimeException {

    private final MatchUpdateFailureReason reason;

    public MatchUpdateException(final MatchUpdateFailureReason reason, final String message) {
        super(message);
        this.reason = reason;
    }

    public MatchUpdateFailureReason getReason() {
        return reason;
    }
}
