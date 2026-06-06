package ar.edu.itba.paw.services.exceptions.registration;

import ar.edu.itba.paw.services.exceptions.DomainException;

public class AccountRegistrationException extends DomainException {

    public AccountRegistrationException(final String message) {
        super(message);
    }
}
