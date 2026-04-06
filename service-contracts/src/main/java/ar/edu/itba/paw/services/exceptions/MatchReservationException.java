package ar.edu.itba.paw.services.exceptions;

public class MatchReservationException extends RuntimeException {

    private final String code;

    public MatchReservationException(final String code, final String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
