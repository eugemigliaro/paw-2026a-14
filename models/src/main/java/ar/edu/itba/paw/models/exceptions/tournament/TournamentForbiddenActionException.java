package ar.edu.itba.paw.models.exceptions.tournament;

import ar.edu.itba.paw.models.exceptions.ForbiddenException;

public class TournamentForbiddenActionException extends ForbiddenException {
    public TournamentForbiddenActionException() {
        super("forbidden");
    }
}
