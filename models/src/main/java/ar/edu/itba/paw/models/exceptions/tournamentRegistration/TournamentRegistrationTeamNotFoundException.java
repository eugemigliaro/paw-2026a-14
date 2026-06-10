package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationTeamNotFoundException extends TournamentRegistrationException {
    public TournamentRegistrationTeamNotFoundException() {
        super("teamNotFound");
    }
}
