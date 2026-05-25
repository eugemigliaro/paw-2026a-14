package ar.edu.itba.paw.services.exceptions;

import ar.edu.itba.paw.services.TournamentBracketFailureReason;

public class TournamentBracketException extends RuntimeException {

    private final TournamentBracketFailureReason reason;

    public TournamentBracketException(
            final TournamentBracketFailureReason reason, final String message) {
        super(message);
        this.reason = reason;
    }

    public TournamentBracketFailureReason getReason() {
        return reason;
    }
}
