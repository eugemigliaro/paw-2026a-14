package ar.edu.itba.paw.services.exceptions.match;

import ar.edu.itba.paw.services.exceptions.ForbiddenException;

public class MatchForbiddenActionException extends ForbiddenException {
    public MatchForbiddenActionException() {
        super("exception.match.forbidden");
    }
}
