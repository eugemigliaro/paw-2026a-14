package ar.edu.itba.paw.services.exceptions.tournamentRegistration;

import ar.edu.itba.paw.services.exceptions.DomainException;

public class TournamentRegistrationException extends DomainException {

    public TournamentRegistrationException(final String message) {
        super(message);
    }
}
