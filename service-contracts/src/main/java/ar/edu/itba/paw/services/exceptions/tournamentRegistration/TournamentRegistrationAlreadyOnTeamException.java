package ar.edu.itba.paw.services.exceptions.tournamentRegistration;

public class TournamentRegistrationAlreadyOnTeamException extends TournamentRegistrationException {
    public TournamentRegistrationAlreadyOnTeamException() {
        super("exception.tournament.registration.alreadyOnTeam");
    }
}
