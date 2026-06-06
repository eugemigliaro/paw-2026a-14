package ar.edu.itba.paw.services.exceptions.registration;

public class EmailInvalidException extends AccountRegistrationException {
    public EmailInvalidException() {
        super("auth.registration.error.emailInvalid");
    }
}
