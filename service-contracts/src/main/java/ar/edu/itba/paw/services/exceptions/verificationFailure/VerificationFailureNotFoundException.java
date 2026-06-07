package ar.edu.itba.paw.services.exceptions.verificationFailure;

import ar.edu.itba.paw.services.exceptions.NotFoundException;

public class VerificationFailureNotFoundException extends NotFoundException {
    public VerificationFailureNotFoundException() {
        super("notFound");
    }
}
