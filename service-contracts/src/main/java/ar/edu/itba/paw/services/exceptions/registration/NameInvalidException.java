package ar.edu.itba.paw.services.exceptions.registration;

public class NameInvalidException extends AccountRegistrationException {
    public NameInvalidException(final String message) {
        super(message);
    }
}
