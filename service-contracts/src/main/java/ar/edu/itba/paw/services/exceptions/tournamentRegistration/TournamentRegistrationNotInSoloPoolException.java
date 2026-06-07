package ar.edu.itba.paw.services.exceptions.tournamentRegistration;

public class TournamentRegistrationNotInSoloPoolException extends TournamentRegistrationException {
    public TournamentRegistrationNotInSoloPoolException() {
        super("notInSoloPool");
    }
}
