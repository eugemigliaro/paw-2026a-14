package ar.edu.itba.paw.models.exceptions.userSportRating;

public class UserSportRatingSportNotRatedException extends UserSportRatingException {
    public UserSportRatingSportNotRatedException() {
        super("sportNotRated");
    }
}
