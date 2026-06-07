package ar.edu.itba.paw.models.exceptions.tournamentLifecycle;

public class TournamentLifecycleNotEditableException extends TournamentLifecycleException {
    public TournamentLifecycleNotEditableException() {
        super("notEditable");
    }
}
