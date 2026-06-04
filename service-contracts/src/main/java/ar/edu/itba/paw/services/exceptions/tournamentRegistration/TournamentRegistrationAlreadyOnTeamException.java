package ar.edu.itba.paw.services.exceptions.tournamentRegistration;

public class TournamentRegistrationAlreadyOnTeamException extends TournamentRegistrationException {

    public TournamentRegistrationAlreadyOnTeamException(final String message) {
        super(message);
    }
}
