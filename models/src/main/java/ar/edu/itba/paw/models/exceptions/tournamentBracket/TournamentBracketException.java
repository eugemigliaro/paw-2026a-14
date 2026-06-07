package ar.edu.itba.paw.models.exceptions.tournamentBracket;

import ar.edu.itba.paw.models.exceptions.DomainException;

public class TournamentBracketException extends DomainException {
    public TournamentBracketException(final String message) {
        super(message);
    }
}
