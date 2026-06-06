package ar.edu.itba.paw.services.exceptions.playerReview;

public class PlayerReviewCommentTooLongException extends PlayerReviewException {
    public PlayerReviewCommentTooLongException() {
        super("exception.field.lengthExcedeed");
    }
}
