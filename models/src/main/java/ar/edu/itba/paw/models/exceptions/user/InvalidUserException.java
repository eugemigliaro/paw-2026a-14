package ar.edu.itba.paw.models.exceptions.user;

import ar.edu.itba.paw.models.exceptions.ForbiddenException;

public class InvalidUserException extends ForbiddenException {
    public InvalidUserException() {
        super("forbidden");
    }
}
