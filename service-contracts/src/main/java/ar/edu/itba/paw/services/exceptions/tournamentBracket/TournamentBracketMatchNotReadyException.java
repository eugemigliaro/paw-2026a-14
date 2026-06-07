package ar.edu.itba.paw.services.exceptions.tournamentBracket;

public class TournamentBracketMatchNotReadyException extends TournamentBracketException {
    public TournamentBracketMatchNotReadyException() {
        super("matchNotReady");
    }
}
