package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.UserSportRating;

public class UserSportRatingLookupResult {

    private final UserSportRating rating;
    private final boolean created;

    public UserSportRatingLookupResult(final UserSportRating rating, final boolean created) {
        this.rating = rating;
        this.created = created;
    }

    public UserSportRating getRating() {
        return rating;
    }

    public boolean isCreated() {
        return created;
    }
}
