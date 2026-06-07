package ar.edu.itba.paw.services.exceptions.tournamentLifecycle;

public class TournamentLifecycleNotEditableException extends TournamentLifecycleException {
    public TournamentLifecycleNotEditableException() {
        super("notEditable");
    }
}
