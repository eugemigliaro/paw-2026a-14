package ar.edu.itba.paw.models.exceptions.tournamentBracket;

public class TournamentBracketMatchNotReadyException extends TournamentBracketException {
    public TournamentBracketMatchNotReadyException() {
        super("matchNotReady");
    }
}
