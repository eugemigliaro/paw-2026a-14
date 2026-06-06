package ar.edu.itba.paw.services.exceptions.tournamentLifecycle;

public class TournamentLifecycleInvalidTeamSizeException extends TournamentLifecycleException {
    public TournamentLifecycleInvalidTeamSizeException() {
        super("exception.tournament.invalidTeamSize");
    }
}
