package ar.edu.itba.paw.services.exceptions.tournamentLifecycle;

public class TournamentLifecycleNotCancellableException extends TournamentLifecycleException {
    public TournamentLifecycleNotCancellableException() {
        super("tournament.lifecycle.error.notCancellable");
    }
}
