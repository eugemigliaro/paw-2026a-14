package ar.edu.itba.paw.services.exceptions.playerReview;

public class PlayerReviewInvalidReactionException extends PlayerReviewException {
    public PlayerReviewInvalidReactionException() {
        super("exception.playerReview.reaction.notFound");
    }
}
