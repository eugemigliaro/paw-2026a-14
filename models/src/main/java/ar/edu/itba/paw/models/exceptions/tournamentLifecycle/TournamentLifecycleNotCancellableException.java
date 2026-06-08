package ar.edu.itba.paw.models.exceptions.tournamentLifecycle;

public class TournamentLifecycleNotCancellableException extends TournamentLifecycleException {
    public TournamentLifecycleNotCancellableException() {
        super("notCancellable");
    }
}
