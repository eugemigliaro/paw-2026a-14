package ar.edu.itba.paw.models.exceptions.tournamentLifecycle;

public class TournamentLifecycleInvalidTeamSizeException extends TournamentLifecycleException {
    public TournamentLifecycleInvalidTeamSizeException() {
        super("invalidTeamSize");
    }
}
