package ar.edu.itba.paw.services.exceptions.passwordReset;

public class PasswordResetException extends RuntimeException {

    public PasswordResetException(final String message) {
        super(message);
    }
}
