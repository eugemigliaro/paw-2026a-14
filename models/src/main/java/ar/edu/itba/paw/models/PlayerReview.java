package ar.edu.itba.paw.models;

import java.time.Instant;

public class PlayerReview {

    private final Long id;
    private final Long reviewerUserId;
    private final Long reviewedUserId;
    private final PlayerReviewReaction reaction;
    private final String comment;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final boolean deleted;
    private final Instant deletedAt;
    private final Long deletedByUserId;
    private final String deleteReason;

    public PlayerReview(
            final Long id,
            final Long reviewerUserId,
            final Long reviewedUserId,
            final PlayerReviewReaction reaction,
            final String comment,
            final Instant createdAt,
            final Instant updatedAt,
            final Instant deletedAt) {
        this(
                id,
                reviewerUserId,
                reviewedUserId,
                reaction,
                comment,
                createdAt,
                updatedAt,
                deletedAt != null,
                deletedAt,
                null,
                null);
    }

    public PlayerReview(
            final Long id,
            final Long reviewerUserId,
            final Long reviewedUserId,
            final PlayerReviewReaction reaction,
            final String comment,
            final Instant createdAt,
            final Instant updatedAt,
            final boolean deleted,
            final Instant deletedAt,
            final Long deletedByUserId,
            final String deleteReason) {
        this.id = id;
        this.reviewerUserId = reviewerUserId;
        this.reviewedUserId = reviewedUserId;
        this.reaction = reaction;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.deletedByUserId = deletedByUserId;
        this.deleteReason = deleteReason;
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

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Long getDeletedByUserId() {
        return deletedByUserId;
    }

    public String getDeleteReason() {
        return deleteReason;
    }
}
