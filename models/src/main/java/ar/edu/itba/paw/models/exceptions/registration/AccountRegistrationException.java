package ar.edu.itba.paw.models.exceptions.registration;

import ar.edu.itba.paw.models.exceptions.DomainException;

public class AccountRegistrationException extends DomainException {

    public AccountRegistrationException(final String message) {
        super(message);
    }
}
