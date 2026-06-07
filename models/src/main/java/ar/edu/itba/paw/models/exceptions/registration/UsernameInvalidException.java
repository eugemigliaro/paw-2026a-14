package ar.edu.itba.paw.models.exceptions.registration;

public class UsernameInvalidException extends AccountRegistrationException {
    public UsernameInvalidException() {
        super("usernameInvalid");
    }
}
