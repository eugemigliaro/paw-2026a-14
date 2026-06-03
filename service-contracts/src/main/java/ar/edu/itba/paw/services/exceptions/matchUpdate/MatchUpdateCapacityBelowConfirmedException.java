package ar.edu.itba.paw.services.exceptions.matchUpdate;

public class MatchUpdateCapacityBelowConfirmedException extends MatchUpdateException {

    public MatchUpdateCapacityBelowConfirmedException(final String message) {
        super(message);
    }
}
