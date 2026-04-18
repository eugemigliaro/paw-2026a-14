package ar.edu.itba.paw.services.exceptions;

public class AccountRegistrationException extends RuntimeException {

    private final String code;

    public AccountRegistrationException(final String code, final String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
