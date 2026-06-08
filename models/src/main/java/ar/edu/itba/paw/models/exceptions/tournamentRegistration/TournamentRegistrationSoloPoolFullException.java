package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationSoloPoolFullException extends TournamentRegistrationException {
    public TournamentRegistrationSoloPoolFullException() {
        super("soloPoolFull");
    }
}
