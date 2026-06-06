package ar.edu.itba.paw.services.exceptions.moderation;

import ar.edu.itba.paw.services.exceptions.DomainException;

public class ModerationException extends DomainException {

    public ModerationException(final String message) {
        super(message);
    }
}
