package ar.edu.itba.paw.models.exceptions.matchRecurrence;

public class MatchRecurrenceTooManyOccurrencesException extends MatchRecurrenceException {
    public MatchRecurrenceTooManyOccurrencesException() {
        super("tooManyOccurrences");
    }
}
