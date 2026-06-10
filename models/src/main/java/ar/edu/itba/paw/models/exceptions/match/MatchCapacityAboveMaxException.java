package ar.edu.itba.paw.models.exceptions.match;

public class MatchCapacityAboveMaxException extends MatchException {
    public MatchCapacityAboveMaxException() {
        super("capacityAboveMax");
    }
}
