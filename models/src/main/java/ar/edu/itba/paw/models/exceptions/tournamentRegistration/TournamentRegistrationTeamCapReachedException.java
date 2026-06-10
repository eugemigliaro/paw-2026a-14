package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationTeamCapReachedException extends TournamentRegistrationException {
    public TournamentRegistrationTeamCapReachedException() {
        super("teamCapReached");
    }
}
