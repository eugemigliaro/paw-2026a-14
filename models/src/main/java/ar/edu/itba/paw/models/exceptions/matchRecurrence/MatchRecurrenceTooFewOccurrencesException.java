package ar.edu.itba.paw.models.exceptions.matchRecurrence;

public class MatchRecurrenceTooFewOccurrencesException extends MatchRecurrenceException {
    public MatchRecurrenceTooFewOccurrencesException() {
        super("tooFewOccurrences");
    }
}
