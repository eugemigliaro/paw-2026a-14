package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationNotInSoloPoolException extends TournamentRegistrationException {
    public TournamentRegistrationNotInSoloPoolException() {
        super("notInSoloPool");
    }
}
