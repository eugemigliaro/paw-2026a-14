package ar.edu.itba.paw.models.exceptions;

public class NotFoundException extends RuntimeException {

    public NotFoundException(final String message) {
        super(message);
    }
}
