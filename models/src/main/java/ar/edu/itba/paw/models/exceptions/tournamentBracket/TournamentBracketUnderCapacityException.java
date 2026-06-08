package ar.edu.itba.paw.models.exceptions.tournamentBracket;

public class TournamentBracketUnderCapacityException extends TournamentBracketException {
    public TournamentBracketUnderCapacityException() {
        super("underCapacity");
    }
}
