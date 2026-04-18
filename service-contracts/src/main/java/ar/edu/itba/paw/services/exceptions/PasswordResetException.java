package ar.edu.itba.paw.services.exceptions;

public class PasswordResetException extends RuntimeException {

    private final String code;

    public PasswordResetException(final String code, final String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
