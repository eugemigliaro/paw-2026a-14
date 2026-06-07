package ar.edu.itba.paw.models.exceptions.tournamentBracket;

public class TournamentBracketAlreadyGeneratedException extends TournamentBracketException {
    public TournamentBracketAlreadyGeneratedException() {
        super("alreadyGenerated");
    }
}
