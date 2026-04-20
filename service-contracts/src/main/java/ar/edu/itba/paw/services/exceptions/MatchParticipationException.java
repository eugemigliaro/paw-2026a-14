package ar.edu.itba.paw.services.exceptions;

public class MatchParticipationException extends RuntimeException {

    private final String code;

    public MatchParticipationException(final String code, final String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
