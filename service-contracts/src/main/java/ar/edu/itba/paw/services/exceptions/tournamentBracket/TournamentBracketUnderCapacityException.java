package ar.edu.itba.paw.services.exceptions.tournamentBracket;

public class TournamentBracketUnderCapacityException extends TournamentBracketException {
    public TournamentBracketUnderCapacityException() {
        super("underCapacity");
    }
}
