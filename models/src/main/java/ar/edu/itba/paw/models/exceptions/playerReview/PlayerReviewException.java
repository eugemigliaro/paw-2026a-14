package ar.edu.itba.paw.models.exceptions.playerReview;

import ar.edu.itba.paw.models.exceptions.DomainException;

public class PlayerReviewException extends DomainException {

    public PlayerReviewException(final String message) {
        super(message);
    }
}
