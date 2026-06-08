package ar.edu.itba.paw.models.exceptions.tournamentBracket;

public class TournamentBracketNotInProgressException extends TournamentBracketException {
    public TournamentBracketNotInProgressException() {
        super("notInProgress");
    }
}
