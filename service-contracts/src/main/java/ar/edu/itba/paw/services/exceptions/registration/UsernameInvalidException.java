package ar.edu.itba.paw.services.exceptions.registration;

public class UsernameInvalidException extends AccountRegistrationException {
    public UsernameInvalidException() {
        super("auth.registration.error.usernameInvalid");
    }
}
