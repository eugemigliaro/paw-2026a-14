package ar.edu.itba.paw.models.exceptions.tournamentLifecycle;

import ar.edu.itba.paw.models.exceptions.DomainException;

public class TournamentLifecycleException extends DomainException {

    public TournamentLifecycleException(final String message) {
        super(message);
    }
}
