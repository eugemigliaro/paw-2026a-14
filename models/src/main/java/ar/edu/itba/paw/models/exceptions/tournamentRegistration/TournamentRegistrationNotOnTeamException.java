package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationNotOnTeamException extends TournamentRegistrationException {
    public TournamentRegistrationNotOnTeamException() {
        super("notOnTeam");
    }
}
