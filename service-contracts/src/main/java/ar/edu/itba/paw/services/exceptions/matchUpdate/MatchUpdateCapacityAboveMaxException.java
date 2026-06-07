package ar.edu.itba.paw.services.exceptions.matchUpdate;

public class MatchUpdateCapacityAboveMaxException extends MatchUpdateException {
    public MatchUpdateCapacityAboveMaxException() {
        super("capacityAboveMax");
    }
}
