package ar.edu.itba.paw.models;

import ar.edu.itba.paw.models.converters.PlayerReviewReactionConverter;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
        name = "player_reviews",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"reviewer_user_id", "reviewed_user_id"})
        })
public class PlayerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_reviews_id_seq")
    @SequenceGenerator(
            sequenceName = "player_reviews_id_seq",
            name = "player_reviews_id_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_user_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewed_user_id", nullable = false)
    private User reviewed;

    @Column(name = "reaction", length = 20, nullable = false)
    @Convert(converter = PlayerReviewReactionConverter.class)
    private PlayerReviewReaction reaction;

    @Column(name = "comment")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_user_id")
    private User deletedBy;

    @Column(name = "delete_reason")
    private String deleteReason;

    PlayerReview() {}

    public PlayerReview(
            final Long id,
            final User reviewer,
            final User reviewed,
            final PlayerReviewReaction reaction,
            final String comment,
            final Instant createdAt,
            final Instant updatedAt,
            final boolean deleted,
            final Instant deletedAt,
            final User deletedBy,
            final String deleteReason) {
        this.id = id;
        this.reviewer = reviewer;
        this.reviewed = reviewed;
        this.reaction = reaction;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy;
        this.deleteReason = deleteReason;
    }

    public Long getId() {
        return id;
    }

    public User getReviewer() {
        return reviewer;
    }

    public User getReviewed() {
        return reviewed;
    }

    public PlayerReviewReaction getReaction() {
        return reaction;
    }

    public void setReaction(final PlayerReviewReaction reaction) {
        this.reaction = reaction;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(final Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public User getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(final User deletedBy) {
        this.deletedBy = deletedBy;
    }

    public String getDeleteReason() {
        return deleteReason;
    }

    public void setDeleteReason(final String deleteReason) {
        this.deleteReason = deleteReason;
    }

    @Override
    public String toString() {
        return "PlayerReview{"
                + "id="
                + id
                + ", reviewerUserId="
                + (reviewer != null ? reviewer.getId() : "null")
                + ", reviewedUserId="
                + (reviewed != null ? reviewed.getId() : "null")
                + ", reaction="
                + reaction
                + ", createdAt="
                + createdAt
                + ", updatedAt="
                + updatedAt
                + ", deleted="
                + deleted
                + ", deletedAt="
                + deletedAt
                + ", deletedByUserId="
                + (deletedBy != null ? deletedBy.getId() : "null")
                + ", hasComment="
                + (comment != null && !comment.isBlank())
                + ", hasDeleteReason="
                + (deleteReason != null && !deleteReason.isBlank())
                + '}';
    }
}
