package ar.edu.itba.paw.services.exceptions.tournamentRegistration;

public class TournamentRegistrationAlreadyAssignedException
        extends TournamentRegistrationException {

    public TournamentRegistrationAlreadyAssignedException(final String message) {
        super(message);
    }
}
