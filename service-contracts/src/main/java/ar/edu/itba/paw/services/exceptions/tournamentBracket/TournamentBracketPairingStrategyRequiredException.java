package ar.edu.itba.paw.services.exceptions.tournamentBracket;

public class TournamentBracketPairingStrategyRequiredException extends TournamentBracketException {
    public TournamentBracketPairingStrategyRequiredException() {
        super("tournament.bracket.error.pairingStrategyRequired");
    }
}
