package ar.edu.itba.paw.services.exceptions.verificationFailure;

import ar.edu.itba.paw.services.exceptions.DomainException;

public class VerificationFailureException extends DomainException {

    public VerificationFailureException(final String message) {
        super(message);
    }
}
