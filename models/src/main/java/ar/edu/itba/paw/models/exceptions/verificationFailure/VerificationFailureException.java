package ar.edu.itba.paw.models.exceptions.verificationFailure;

import ar.edu.itba.paw.models.exceptions.DomainException;

public class VerificationFailureException extends DomainException {

    public VerificationFailureException(final String message) {
        super(message);
    }
}
