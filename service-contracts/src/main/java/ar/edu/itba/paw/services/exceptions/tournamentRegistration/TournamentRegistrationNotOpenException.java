package ar.edu.itba.paw.services.exceptions.tournamentRegistration;

public class TournamentRegistrationNotOpenException extends TournamentRegistrationException {
    public TournamentRegistrationNotOpenException() {
        super("notOpen");
    }
}
