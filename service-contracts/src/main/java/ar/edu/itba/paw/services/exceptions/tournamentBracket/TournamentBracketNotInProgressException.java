package ar.edu.itba.paw.services.exceptions.tournamentBracket;

public class TournamentBracketNotInProgressException extends TournamentBracketException {
    public TournamentBracketNotInProgressException() {
        super("exception.tournament.bracket.notInProgress");
    }
}
