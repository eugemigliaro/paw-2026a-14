package ar.edu.itba.paw.models.exceptions.playerReview;

import ar.edu.itba.paw.models.exceptions.NotFoundException;

public class PlayerReviewNotFoundException extends NotFoundException {
    public PlayerReviewNotFoundException() {
        super("notFound");
    }
}
