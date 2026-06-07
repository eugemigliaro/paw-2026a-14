package ar.edu.itba.paw.models.exceptions.tournamentBracket;

public class TournamentBracketPairingStrategyRequiredException extends TournamentBracketException {
    public TournamentBracketPairingStrategyRequiredException() {
        super("pairingStrategyRequired");
    }
}
