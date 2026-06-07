package ar.edu.itba.paw.models.exceptions.verificationFailure;

import ar.edu.itba.paw.models.exceptions.NotFoundException;

public class VerificationFailureNotFoundException extends NotFoundException {
    public VerificationFailureNotFoundException() {
        super("notFound");
    }
}
