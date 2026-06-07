package ar.edu.itba.paw.models.exceptions.registration;

public class PasswordInvalidException extends AccountRegistrationException {
    public PasswordInvalidException() {
        super("passwordInvalid");
    }
}
