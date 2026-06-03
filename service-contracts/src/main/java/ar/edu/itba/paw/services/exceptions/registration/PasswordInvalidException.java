package ar.edu.itba.paw.services.exceptions.registration;

public class PasswordInvalidException extends AccountRegistrationException {
    public PasswordInvalidException(final String message) {
        super(message);
    }
}
