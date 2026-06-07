package ar.edu.itba.paw.models.exceptions.verificationFailure;

public class VerificationFailureInvalidActionException extends VerificationFailureException {
    public VerificationFailureInvalidActionException() {
        super("accountUnavailable");
    }
}
