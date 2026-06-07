package ar.edu.itba.paw.models.exceptions.playerReview;

public class PlayerReviewNotEligibleException extends PlayerReviewException {
    public PlayerReviewNotEligibleException() {
        super("notEligible");
    }
}
