package ar.edu.itba.paw.services.exceptions.registration;

public class PhoneInvalidException extends AccountRegistrationException {
    public PhoneInvalidException(final String message) {
        super(message);
    }
}
