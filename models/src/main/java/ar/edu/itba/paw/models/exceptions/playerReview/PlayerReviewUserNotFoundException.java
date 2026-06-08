package ar.edu.itba.paw.models.exceptions.playerReview;

public class PlayerReviewUserNotFoundException extends PlayerReviewException {
    public PlayerReviewUserNotFoundException() {
        super("userNotFound");
    }
}
