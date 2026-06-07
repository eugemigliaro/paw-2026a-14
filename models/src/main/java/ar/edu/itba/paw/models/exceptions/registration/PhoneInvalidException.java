package ar.edu.itba.paw.models.exceptions.registration;

public class PhoneInvalidException extends AccountRegistrationException {
    public PhoneInvalidException() {
        super("phoneInvalid");
    }
}
