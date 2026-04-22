package ar.edu.itba.paw.services.exceptions;

import ar.edu.itba.paw.services.VerificationFailureReason;

public class VerificationFailureException extends RuntimeException {

    private final VerificationFailureReason reason;

    public VerificationFailureException(
            final VerificationFailureReason reason, final String message) {
        super(message);
        this.reason = reason;
    }

    public VerificationFailureReason getReason() {
        return reason;
    }
}
