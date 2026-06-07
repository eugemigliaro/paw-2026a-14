package ar.edu.itba.paw.models.exceptions.matchUpdate;

public class MatchUpdateNotEditableException extends MatchUpdateException {
    public MatchUpdateNotEditableException() {
        super("notEditable");
    }
}
