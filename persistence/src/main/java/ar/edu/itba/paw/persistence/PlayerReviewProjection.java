package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import java.time.Instant;

public record PlayerReviewProjection(
        Long id,
        Long reviewerId,
        String reviewerUsername,
        Long reviewedId,
        String reviewedUsername,
        PlayerReviewReaction reaction,
        String comment,
        Instant createdAt,
        Instant updatedAt,
        Boolean deleted,
        Instant deletedAt,
        Long deletedById,
        String deleteReason) {

    PlayerReviewProjection(
            final long id,
            final Long reviewerId,
            final String reviewerUsername,
            final Long reviewedId,
            final String reviewedUsername,
            final PlayerReviewReaction reaction,
            final String comment,
            final Instant createdAt,
            final Instant updatedAt,
            final Boolean deleted,
            final Instant deletedAt,
            final Long deletedById,
            final String deleteReason) {
        this(
                Long.valueOf(id),
                reviewerId,
                reviewerUsername,
                reviewedId,
                reviewedUsername,
                reaction,
                comment,
                createdAt,
                updatedAt,
                deleted,
                deletedAt,
                deletedById,
                deleteReason);
    }

    PlayerReview toPlayerReview() {
        final UserAccount reviewer =
                new UserAccount(
                        reviewerId,
                        null,
                        reviewerUsername,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        final UserAccount reviewed =
                new UserAccount(
                        reviewedId,
                        null,
                        reviewedUsername,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        final UserAccount deletedBy =
                deletedById == null
                        ? null
                        : new UserAccount(
                                deletedById,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null);

        final boolean deletedVal = deleted != null && deleted;

        return new PlayerReview(
                id,
                reviewer,
                reviewed,
                reaction,
                comment,
                createdAt,
                updatedAt,
                deletedVal,
                deletedAt,
                deletedBy,
                deleteReason);
    }
}
