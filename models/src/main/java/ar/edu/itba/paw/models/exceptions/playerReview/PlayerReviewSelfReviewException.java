package ar.edu.itba.paw.models.exceptions.playerReview;

public class PlayerReviewSelfReviewException extends PlayerReviewException {
    public PlayerReviewSelfReviewException() {
        super("selfReview");
    }
}
