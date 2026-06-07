package ar.edu.itba.paw.models.exceptions.registration;

public class UsernameTakenException extends AccountRegistrationException {
    public UsernameTakenException() {
        super("usernameTaken");
    }
}
