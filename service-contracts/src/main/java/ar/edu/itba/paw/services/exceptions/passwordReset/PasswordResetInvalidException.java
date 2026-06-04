package ar.edu.itba.paw.services.exceptions.passwordReset;

public class PasswordResetInvalidException extends PasswordResetException {
    public PasswordResetInvalidException(final String message) {
        super(message);
    }
}
