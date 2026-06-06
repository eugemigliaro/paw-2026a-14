package ar.edu.itba.paw.services.exceptions.playerReview;

import ar.edu.itba.paw.services.exceptions.DomainException;

public class PlayerReviewException extends DomainException {

    public PlayerReviewException(final String message) {
        super(message);
    }
}
