package ar.edu.itba.paw.services.exceptions.tournamentRegistration;

public class TournamentRegistrationSoloSignupDisabledException
        extends TournamentRegistrationException {
    public TournamentRegistrationSoloSignupDisabledException() {
        super("soloDisabled");
    }
}
