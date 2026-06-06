package ar.edu.itba.paw.services.exceptions.tournamentBracket;

public class TournamentBracketInvalidRoundOrderException extends TournamentBracketException {
    public TournamentBracketInvalidRoundOrderException() {
        super("exception.tournament.bracket.invalidRoundOrder");
    }
}
