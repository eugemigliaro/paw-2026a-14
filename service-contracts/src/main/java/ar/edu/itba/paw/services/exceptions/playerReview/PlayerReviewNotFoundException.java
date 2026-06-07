package ar.edu.itba.paw.services.exceptions.playerReview;

import ar.edu.itba.paw.services.exceptions.NotFoundException;

public class PlayerReviewNotFoundException extends NotFoundException {
    public PlayerReviewNotFoundException() {
        super("notFound");
    }
}
