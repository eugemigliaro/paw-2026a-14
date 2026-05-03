package ar.edu.itba.paw.models;

public class PlayerReviewSummary {

    private final Long reviewedUserId;
    private final long likeCount;
    private final long dislikeCount;
    private final long reviewCount;

    public PlayerReviewSummary(
            final Long reviewedUserId,
            final long likeCount,
            final long dislikeCount,
            final long reviewCount) {
        this.reviewedUserId = reviewedUserId;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.reviewCount = reviewCount;
    }

    public Long getReviewedUserId() {
        return reviewedUserId;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public long getDislikeCount() {
        return dislikeCount;
    }

    public long getReviewCount() {
        return reviewCount;
    }
}
