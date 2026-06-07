package ar.edu.itba.paw.services.exceptions.matchUpdate;

public class MatchUpdateNotEditableException extends MatchUpdateException {
    public MatchUpdateNotEditableException() {
        super("notEditable");
    }
}
