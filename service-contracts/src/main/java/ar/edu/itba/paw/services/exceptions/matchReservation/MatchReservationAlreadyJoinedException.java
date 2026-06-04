package ar.edu.itba.paw.services.exceptions.matchReservation;

public class MatchReservationAlreadyJoinedException extends MatchReservationException {
    public MatchReservationAlreadyJoinedException(final String message) {
        super(message);
    }
}
