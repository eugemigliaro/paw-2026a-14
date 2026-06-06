package ar.edu.itba.paw.services.exceptions.registration;

public class NameInvalidException extends AccountRegistrationException {
    public NameInvalidException() {
        super("auth.registration.error.nameInvalid");
    }
}
