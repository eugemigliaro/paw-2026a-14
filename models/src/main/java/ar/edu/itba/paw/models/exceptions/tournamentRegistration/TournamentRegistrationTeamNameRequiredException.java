package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationTeamNameRequiredException
        extends TournamentRegistrationException {
    public TournamentRegistrationTeamNameRequiredException() {
        super("teamNameRequired");
    }
}
