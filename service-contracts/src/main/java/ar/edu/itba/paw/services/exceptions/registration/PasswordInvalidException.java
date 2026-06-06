package ar.edu.itba.paw.services.exceptions.registration;

public class PasswordInvalidException extends AccountRegistrationException {
    public PasswordInvalidException() {
        super("auth.registration.error.passwordInvalid");
    }
}
