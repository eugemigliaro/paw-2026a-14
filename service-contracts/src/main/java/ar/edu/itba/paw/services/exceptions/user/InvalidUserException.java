package ar.edu.itba.paw.services.exceptions.user;

import ar.edu.itba.paw.services.exceptions.ForbiddenException;

public class InvalidUserException extends ForbiddenException {
    public InvalidUserException() {
        super("forbidden");
    }
}
