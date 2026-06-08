package ar.edu.itba.paw.models.exceptions.tournamentBracket;

public class TournamentBracketMissingMatchScheduleException extends TournamentBracketException {
    public TournamentBracketMissingMatchScheduleException() {
        super("missingMatchSchedule");
    }
}
