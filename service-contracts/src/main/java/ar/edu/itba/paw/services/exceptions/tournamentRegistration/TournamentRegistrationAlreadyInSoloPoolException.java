package ar.edu.itba.paw.services.exceptions.tournamentRegistration;

public class TournamentRegistrationAlreadyInSoloPoolException
        extends TournamentRegistrationException {

    public TournamentRegistrationAlreadyInSoloPoolException(final String message) {
        super(message);
    }
}
