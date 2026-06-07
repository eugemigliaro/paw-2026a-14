package ar.edu.itba.paw.services.exceptions.tournamentBracket;

public class TournamentBracketWinnerNotInMatchException extends TournamentBracketException {
    public TournamentBracketWinnerNotInMatchException() {
        super("winnerNotInMatch");
    }
}
