package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationTournamentFullException extends TournamentRegistrationException {
    public TournamentRegistrationTournamentFullException() {
        super("tournamentFull");
    }
}
