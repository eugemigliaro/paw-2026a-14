package ar.edu.itba.paw.services.exceptions;

import ar.edu.itba.paw.services.TournamentJoinFailureReason;

public class TournamentRegistrationException extends RuntimeException {

    private final TournamentJoinFailureReason reason;

    public TournamentRegistrationException(
            final TournamentJoinFailureReason reason, final String message) {
        super(message);
        this.reason = reason;
    }

    public TournamentJoinFailureReason getReason() {
        return reason;
    }
}
