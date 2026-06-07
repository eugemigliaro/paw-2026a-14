package ar.edu.itba.paw.services.exceptions.match;

public class MatchNotRecurringException extends MatchException {
    public MatchNotRecurringException() {
        super("notRecurring");
    }
}
