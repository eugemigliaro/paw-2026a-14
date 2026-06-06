package ar.edu.itba.paw.services.exceptions.registration;

public class LastNameInvalidException extends AccountRegistrationException {
    public LastNameInvalidException() {
        super("auth.registration.error.lastNameInvalid");
    }
}
