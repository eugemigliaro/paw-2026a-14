package ar.edu.itba.paw.services;

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
