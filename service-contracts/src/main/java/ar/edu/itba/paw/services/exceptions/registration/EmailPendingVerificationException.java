package ar.edu.itba.paw.services.exceptions.registration;

public class EmailPendingVerificationException extends AccountRegistrationException {
    public EmailPendingVerificationException(final String message) {
        super(message);
    }
}
