package ar.edu.itba.paw.services.exceptions.tournamentBracket;

import ar.edu.itba.paw.services.exceptions.DomainException;

public class TournamentBracketException extends DomainException {
    public TournamentBracketException(final String message) {
        super(message);
    }
}
