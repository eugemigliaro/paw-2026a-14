package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationNotOpenException extends TournamentRegistrationException {
    public TournamentRegistrationNotOpenException() {
        super("notOpen");
    }
}
