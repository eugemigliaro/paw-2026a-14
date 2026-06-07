package ar.edu.itba.paw.services.exceptions.tournamentRegistration;

public class TournamentRegistrationAlreadyAssignedException
        extends TournamentRegistrationException {
    public TournamentRegistrationAlreadyAssignedException() {
        super("alreadyAssigned");
    }
}
