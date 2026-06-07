package ar.edu.itba.paw.services.exceptions.verificationFailure;

public class VerificationFailureInvalidActionException extends VerificationFailureException {
    public VerificationFailureInvalidActionException() {
        super("accountUnavailable");
    }
}
