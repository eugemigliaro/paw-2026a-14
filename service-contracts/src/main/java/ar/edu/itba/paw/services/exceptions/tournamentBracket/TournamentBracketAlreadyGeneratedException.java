package ar.edu.itba.paw.services.exceptions.tournamentBracket;

public class TournamentBracketAlreadyGeneratedException extends TournamentBracketException {
    public TournamentBracketAlreadyGeneratedException() {
        super("tournament.bracket.error.alreadyGenerated");
    }
}
