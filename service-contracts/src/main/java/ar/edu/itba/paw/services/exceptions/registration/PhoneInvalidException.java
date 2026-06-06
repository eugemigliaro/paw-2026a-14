package ar.edu.itba.paw.services.exceptions.registration;

public class PhoneInvalidException extends AccountRegistrationException {
    public PhoneInvalidException() {
        super("auth.registration.error.phoneInvalid");
    }
}
