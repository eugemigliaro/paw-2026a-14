package ar.edu.itba.paw.models.exceptions.userSportRating;

import ar.edu.itba.paw.models.exceptions.DomainException;

public class UserSportRatingException extends DomainException {
    public UserSportRatingException(final String message) {
        super(message);
    }
}
