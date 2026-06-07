package ar.edu.itba.paw.services.exceptions.playerReview;

public class PlayerReviewNotEligibleException extends PlayerReviewException {
    public PlayerReviewNotEligibleException() {
        super("notEligible");
    }
}
