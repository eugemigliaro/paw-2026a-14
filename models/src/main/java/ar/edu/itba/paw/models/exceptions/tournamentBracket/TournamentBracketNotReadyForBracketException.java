package ar.edu.itba.paw.models.exceptions.tournamentBracket;

public class TournamentBracketNotReadyForBracketException extends TournamentBracketException {
    public TournamentBracketNotReadyForBracketException() {
        super("notReady");
    }
}
