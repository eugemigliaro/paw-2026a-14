package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationUnderCapacityException extends TournamentRegistrationException {
    public TournamentRegistrationUnderCapacityException() {
        super("underCapacity");
    }
}
