package ar.edu.itba.paw.services.exceptions;

import ar.edu.itba.paw.services.TournamentLifecycleFailureReason;

public class TournamentLifecycleException extends RuntimeException {

    private final TournamentLifecycleFailureReason reason;

    public TournamentLifecycleException(
            final TournamentLifecycleFailureReason reason, final String message) {
        super(message);
        this.reason = reason;
    }

    public TournamentLifecycleFailureReason getReason() {
        return reason;
    }
}
