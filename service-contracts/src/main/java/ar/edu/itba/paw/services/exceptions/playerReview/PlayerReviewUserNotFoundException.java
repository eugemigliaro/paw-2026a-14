package ar.edu.itba.paw.services.exceptions.playerReview;

public class PlayerReviewUserNotFoundException extends PlayerReviewException {
    public PlayerReviewUserNotFoundException() {
        super("exception.playerReview.user.notFound");
    }
}
