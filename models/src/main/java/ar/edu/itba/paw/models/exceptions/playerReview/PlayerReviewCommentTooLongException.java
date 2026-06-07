package ar.edu.itba.paw.models.exceptions.playerReview;

public class PlayerReviewCommentTooLongException extends PlayerReviewException {
    public PlayerReviewCommentTooLongException() {
        super("commentTooLong");
    }
}
