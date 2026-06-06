package ar.edu.itba.paw.services.exceptions.tournamentLifecycle;

import ar.edu.itba.paw.services.exceptions.DomainException;

public class TournamentLifecycleException extends DomainException {

    public TournamentLifecycleException(final String message) {
        super(message);
    }
}
