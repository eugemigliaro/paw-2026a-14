package ar.edu.itba.paw.services.exceptions.tournament;

import ar.edu.itba.paw.services.exceptions.ForbiddenException;

public class TournamentForbiddenActionException extends ForbiddenException {
    public TournamentForbiddenActionException() {
        super("exception.tournament.forbidden");
    }
}
