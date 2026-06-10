package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationTeamNameTakenException extends TournamentRegistrationException {
    public TournamentRegistrationTeamNameTakenException() {
        super("teamNameTaken");
    }
}
