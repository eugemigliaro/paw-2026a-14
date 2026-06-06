package ar.edu.itba.paw.services.exceptions.tournamentRegistration;

public class TournamentRegistrationSoloPoolFullException extends TournamentRegistrationException {
    public TournamentRegistrationSoloPoolFullException() {
        super("exception.tournament.registration.soloPoolFull");
    }
}
