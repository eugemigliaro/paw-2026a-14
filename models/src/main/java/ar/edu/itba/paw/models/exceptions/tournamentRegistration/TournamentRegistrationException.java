package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

import ar.edu.itba.paw.models.exceptions.DomainException;

public class TournamentRegistrationException extends DomainException {

    public TournamentRegistrationException(final String message) {
        super(message);
    }
}
