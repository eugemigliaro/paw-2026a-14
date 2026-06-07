package ar.edu.itba.paw.models.exceptions.matchUpdate;

public class MatchUpdateCapacityBelowConfirmedException extends MatchUpdateException {
    public MatchUpdateCapacityBelowConfirmedException() {
        super("capacityBelowConfirmed");
    }
}
