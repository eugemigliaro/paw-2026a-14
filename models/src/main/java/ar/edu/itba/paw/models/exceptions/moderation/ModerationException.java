package ar.edu.itba.paw.models.exceptions.moderation;

import ar.edu.itba.paw.models.exceptions.DomainException;

public class ModerationException extends DomainException {

    public ModerationException(final String message) {
        super(message);
    }
}
