package ar.edu.itba.paw.models.exceptions.matchUpdate;

public class MatchUpdateCapacityAboveMaxException extends MatchUpdateException {
    public MatchUpdateCapacityAboveMaxException() {
        super("capacityAboveMax");
    }
}
