package ar.edu.itba.paw.models.exceptions.tournament;

import ar.edu.itba.paw.models.exceptions.DomainException;

public class TournamentInvalidRequestException extends DomainException {
    public TournamentInvalidRequestException() {
        super("invalidRequest");
    }
}
