package ar.edu.itba.paw.services.exceptions.tournamentBracket;

public class TournamentBracketNotReadyForBracketException extends TournamentBracketException {
    public TournamentBracketNotReadyForBracketException() {
        super("exception.tournament.bracket.notReadyForGeneration");
    }
}
