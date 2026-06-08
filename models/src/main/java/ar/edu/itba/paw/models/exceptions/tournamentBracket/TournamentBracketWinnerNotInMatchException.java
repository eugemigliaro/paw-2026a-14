package ar.edu.itba.paw.models.exceptions.tournamentBracket;

public class TournamentBracketWinnerNotInMatchException extends TournamentBracketException {
    public TournamentBracketWinnerNotInMatchException() {
        super("winnerNotInMatch");
    }
}
