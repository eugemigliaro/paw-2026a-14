package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationAlreadyAssignedException
        extends TournamentRegistrationException {
    public TournamentRegistrationAlreadyAssignedException() {
        super("alreadyAssigned");
    }
}
