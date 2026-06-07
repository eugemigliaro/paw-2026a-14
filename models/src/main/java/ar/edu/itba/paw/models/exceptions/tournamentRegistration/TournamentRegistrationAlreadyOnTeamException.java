package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationAlreadyOnTeamException extends TournamentRegistrationException {
    public TournamentRegistrationAlreadyOnTeamException() {
        super("alreadyOnTeam");
    }
}
