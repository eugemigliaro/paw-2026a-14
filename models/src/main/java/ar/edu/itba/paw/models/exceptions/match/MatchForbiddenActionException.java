package ar.edu.itba.paw.models.exceptions.match;

import ar.edu.itba.paw.models.exceptions.ForbiddenException;

public class MatchForbiddenActionException extends ForbiddenException {
    public MatchForbiddenActionException() {
        super("forbidden");
    }
}
