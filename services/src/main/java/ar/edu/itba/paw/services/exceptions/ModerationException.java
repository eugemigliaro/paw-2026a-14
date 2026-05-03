package ar.edu.itba.paw.services.exceptions;

public class ModerationException extends RuntimeException {

    private final String code;

    public ModerationException(final String code, final String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
