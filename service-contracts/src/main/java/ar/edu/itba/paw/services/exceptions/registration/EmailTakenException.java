package ar.edu.itba.paw.services.exceptions.registration;

public class EmailTakenException extends AccountRegistrationException {
    public EmailTakenException(final String message) {
        super(message);
    }
}
