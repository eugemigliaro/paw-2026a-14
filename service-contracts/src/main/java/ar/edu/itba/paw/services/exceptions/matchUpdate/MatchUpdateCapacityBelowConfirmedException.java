package ar.edu.itba.paw.services.exceptions.matchUpdate;

public class MatchUpdateCapacityBelowConfirmedException extends MatchUpdateException {
    public MatchUpdateCapacityBelowConfirmedException() {
        super("match.update.error.capacityBelowConfirmed");
    }
}
