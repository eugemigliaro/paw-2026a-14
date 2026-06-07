package ar.edu.itba.paw.services.exceptions.tournamentRegistration;

public class TournamentRegistrationUnderCapacityException extends TournamentRegistrationException {
    public TournamentRegistrationUnderCapacityException() {
        super("underCapacity");
    }
}
