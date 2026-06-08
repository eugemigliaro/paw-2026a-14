package ar.edu.itba.paw.models.exceptions.verificationFailure;

public class VerificationFailureExpiredException extends VerificationFailureException {
    public VerificationFailureExpiredException() {
        super("expired");
    }
}
