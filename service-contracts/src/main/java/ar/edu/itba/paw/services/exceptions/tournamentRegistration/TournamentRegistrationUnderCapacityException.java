package ar.edu.itba.paw.services.exceptions.tournamentRegistration;

public class TournamentRegistrationUnderCapacityException extends TournamentRegistrationException {

    public TournamentRegistrationUnderCapacityException(final String message) {
        super(message);
    }
}
