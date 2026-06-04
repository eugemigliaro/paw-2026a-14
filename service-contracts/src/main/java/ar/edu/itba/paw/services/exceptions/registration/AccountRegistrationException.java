package ar.edu.itba.paw.services.exceptions.registration;

public class AccountRegistrationException extends RuntimeException {

    public AccountRegistrationException(final String message) {
        super(message);
    }
}
