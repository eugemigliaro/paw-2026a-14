package ar.edu.itba.paw.models;

import java.time.Instant;

public class PlayerReview {

    private final Long id;
    private final Long reviewerUserId;
    private final Long reviewedUserId;
    private final Long originMatchId;
    private final PlayerReviewReaction reaction;
    private final String comment;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;

    public PlayerReview(
            final Long id,
            final Long reviewerUserId,
            final Long reviewedUserId,
            final Long originMatchId,
            final PlayerReviewReaction reaction,
            final String comment,
            final Instant createdAt,
            final Instant updatedAt,
            final Instant deletedAt) {
        this.id = id;
        this.reviewerUserId = reviewerUserId;
        this.reviewedUserId = reviewedUserId;
        this.originMatchId = originMatchId;
        this.reaction = reaction;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getReviewerUserId() {
        return reviewerUserId;
    }

    public Long getReviewedUserId() {
        return reviewedUserId;
    }

    public Long getOriginMatchId() {
        return originMatchId;
    }

    public PlayerReviewReaction getReaction() {
        return reaction;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
