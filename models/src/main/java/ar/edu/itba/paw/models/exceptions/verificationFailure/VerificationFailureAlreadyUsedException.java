package ar.edu.itba.paw.models.exceptions.verificationFailure;

public class VerificationFailureAlreadyUsedException extends VerificationFailureException {
    public VerificationFailureAlreadyUsedException() {
        super("accountUnavailable");
    }
}
