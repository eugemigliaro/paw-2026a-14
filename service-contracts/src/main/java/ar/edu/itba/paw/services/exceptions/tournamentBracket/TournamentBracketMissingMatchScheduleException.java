package ar.edu.itba.paw.services.exceptions.tournamentBracket;

public class TournamentBracketMissingMatchScheduleException extends TournamentBracketException {
    public TournamentBracketMissingMatchScheduleException() {
        super("exception.match.scheduleRequired");
    }
}
