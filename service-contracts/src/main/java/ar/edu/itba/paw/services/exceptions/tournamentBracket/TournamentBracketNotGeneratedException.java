package ar.edu.itba.paw.services.exceptions.tournamentBracket;

public class TournamentBracketNotGeneratedException extends TournamentBracketException {
    public TournamentBracketNotGeneratedException() {
        super("exception.tournament.bracket.notGenerated");
    }
}
