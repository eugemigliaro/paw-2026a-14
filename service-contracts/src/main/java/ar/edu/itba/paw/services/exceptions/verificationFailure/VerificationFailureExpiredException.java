package ar.edu.itba.paw.services.exceptions.verificationFailure;

public class VerificationFailureExpiredException extends VerificationFailureException {
    public VerificationFailureExpiredException() {
        super("expired");
    }
}
