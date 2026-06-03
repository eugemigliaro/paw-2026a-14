package ar.edu.itba.paw.services.exceptions.tournamentLifecycle;

public class TournamentLifecycleAlreadyCompletedException extends TournamentLifecycleException {

    public TournamentLifecycleAlreadyCompletedException(final String message) {
        super(message);
    }
}
