package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PlayerReview;
import java.util.Optional;

public class PlayerReviewProfileState {

    public enum LockedReason {
        NONE,
        ANONYMOUS,
        SELF,
        NOT_ELIGIBLE
    }

    private final Optional<PlayerReview> viewerReview;
    private final boolean canSubmit;
    private final LockedReason lockedReason;

    public PlayerReviewProfileState(
            final Optional<PlayerReview> viewerReview,
            final boolean canSubmit,
            final LockedReason lockedReason) {
        this.viewerReview = viewerReview == null ? Optional.empty() : viewerReview;
        this.canSubmit = canSubmit;
        this.lockedReason = lockedReason == null ? LockedReason.NONE : lockedReason;
    }

    public static PlayerReviewProfileState anonymous() {
        return new PlayerReviewProfileState(Optional.empty(), false, LockedReason.ANONYMOUS);
    }

    public Optional<PlayerReview> getViewerReview() {
        return viewerReview;
    }

    public boolean canSubmit() {
        return canSubmit;
    }

    public LockedReason getLockedReason() {
        return lockedReason;
    }

    public boolean showLockedMessage() {
        return lockedReason == LockedReason.ANONYMOUS || lockedReason == LockedReason.NOT_ELIGIBLE;
    }
}
