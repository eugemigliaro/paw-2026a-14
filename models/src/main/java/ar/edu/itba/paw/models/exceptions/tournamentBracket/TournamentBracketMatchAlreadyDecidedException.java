package ar.edu.itba.paw.models.exceptions.tournamentBracket;

public class TournamentBracketMatchAlreadyDecidedException extends TournamentBracketException {
    public TournamentBracketMatchAlreadyDecidedException() {
        super("matchAlreadyDecided");
    }
}
