package ar.edu.itba.paw.services.exceptions.verificationFailure;

public class VerificationFailureAlreadyUsedException extends VerificationFailureException {

    public VerificationFailureAlreadyUsedException(final String message) {
        super(message);
    }
}
