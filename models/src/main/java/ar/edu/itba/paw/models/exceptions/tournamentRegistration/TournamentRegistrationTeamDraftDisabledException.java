package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationTeamDraftDisabledException
        extends TournamentRegistrationException {
    public TournamentRegistrationTeamDraftDisabledException() {
        super("teamDraftDisabled");
    }
}
