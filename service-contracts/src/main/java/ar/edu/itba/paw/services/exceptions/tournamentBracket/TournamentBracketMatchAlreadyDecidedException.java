package ar.edu.itba.paw.services.exceptions.tournamentBracket;

public class TournamentBracketMatchAlreadyDecidedException extends TournamentBracketException {
    public TournamentBracketMatchAlreadyDecidedException() {
        super("matchAlreadyDecided");
    }
}
