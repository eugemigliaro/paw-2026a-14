package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationSoloSignupDisabledException
        extends TournamentRegistrationException {
    public TournamentRegistrationSoloSignupDisabledException() {
        super("soloDisabled");
    }
}
