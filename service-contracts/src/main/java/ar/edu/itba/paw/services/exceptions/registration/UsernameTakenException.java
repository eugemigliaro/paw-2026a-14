package ar.edu.itba.paw.services.exceptions.registration;

public class UsernameTakenException extends AccountRegistrationException {
    public UsernameTakenException(final String message) {
        super(message);
    }
}
