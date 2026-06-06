package ar.edu.itba.paw.services.exceptions.matchUpdate;

public class MatchUpdateInvalidScheduleException extends MatchUpdateException {
    public MatchUpdateInvalidScheduleException() {
        super("match.schedule.error.invalid");
    }
}
