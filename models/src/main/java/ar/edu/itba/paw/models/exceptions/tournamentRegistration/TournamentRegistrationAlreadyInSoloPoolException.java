package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationAlreadyInSoloPoolException
        extends TournamentRegistrationException {
    public TournamentRegistrationAlreadyInSoloPoolException() {
        super("alreadyInSoloPool");
    }
}
