package ar.edu.itba.paw.models.exceptions.pagination;

import ar.edu.itba.paw.models.exceptions.DomainException;

public class InvalidPaginationException extends DomainException {
    public InvalidPaginationException(final String message) {
        super(message);
    }
}
