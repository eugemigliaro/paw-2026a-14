package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewReaction;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class PlayerReviewJdbcDao implements PlayerReviewDao {

    private static final String COMPLETED_MATCH_SQL =
            "(m.status = 'completed'"
                    + " OR (m.status = 'open'"
                    + " AND COALESCE(m.ends_at, m.starts_at) <= CURRENT_TIMESTAMP))";

    @NonNull
    private static final RowMapper<PlayerReview> PLAYER_REVIEW_ROW_MAPPER =
            (ResultSet rs, int rowNum) ->
                    new PlayerReview(
                            rs.getLong("id"),
                            rs.getLong("reviewer_user_id"),
                            rs.getLong("reviewed_user_id"),
                            PlayerReviewReaction.fromDbValue(rs.getString("reaction"))
                                    .orElseThrow(
                                            () ->
                                                    new IllegalStateException(
                                                            "Unknown player review reaction")),
                            rs.getString("comment"),
                            toInstant(rs.getTimestamp("created_at")),
                            toInstant(rs.getTimestamp("updated_at")),
                            toInstant(rs.getTimestamp("deleted_at")));

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PlayerReviewJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public PlayerReview upsertReview(
            final Long reviewerUserId,
            final Long reviewedUserId,
            final PlayerReviewReaction reaction,
            final String comment) {
        final Optional<PlayerReview> existing =
                findByPairIncludingDeleted(reviewerUserId, reviewedUserId);

        if (existing.isPresent()) {
            jdbcTemplate.update(
                    "UPDATE player_reviews"
                            + " SET reaction = ?, comment = ?,"
                            + " updated_at = CURRENT_TIMESTAMP, deleted_at = NULL"
                            + " WHERE reviewer_user_id = ? AND reviewed_user_id = ?",
                    reactionParameter(reaction),
                    comment,
                    reviewerUserId,
                    reviewedUserId);
        } else {
            jdbcTemplate.update(
                    "INSERT INTO player_reviews"
                            + " (reviewer_user_id, reviewed_user_id, reaction, comment,"
                            + " created_at, updated_at)"
                            + " VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    reviewerUserId,
                    reviewedUserId,
                    reactionParameter(reaction),
                    comment);
        }

        return findByPair(reviewerUserId, reviewedUserId)
                .orElseThrow(() -> new IllegalStateException("Player review was not persisted"));
    }

    @Override
    public boolean softDeleteReview(final Long reviewerUserId, final Long reviewedUserId) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE player_reviews"
                                + " SET deleted_at = CURRENT_TIMESTAMP,"
                                + " updated_at = CURRENT_TIMESTAMP"
                                + " WHERE reviewer_user_id = ? AND reviewed_user_id = ?"
                                + " AND deleted_at IS NULL",
                        reviewerUserId,
                        reviewedUserId);
        return rows == 1;
    }

    @Override
    public Optional<PlayerReview> findByPair(final Long reviewerUserId, final Long reviewedUserId) {
        return jdbcTemplate
                .query(
                        "SELECT id, reviewer_user_id, reviewed_user_id,"
                                + " reaction, comment, created_at, updated_at, deleted_at"
                                + " FROM player_reviews"
                                + " WHERE reviewer_user_id = ? AND reviewed_user_id = ?"
                                + " AND deleted_at IS NULL",
                        PLAYER_REVIEW_ROW_MAPPER,
                        reviewerUserId,
                        reviewedUserId)
                .stream()
                .findFirst();
    }

    @Override
    public PlayerReviewSummary getSummaryForUser(final Long reviewedUserId) {
        return jdbcTemplate.queryForObject(
                "SELECT ? AS reviewed_user_id,"
                        + " COALESCE(SUM(CASE WHEN reaction = 'like' THEN 1 ELSE 0 END), 0)"
                        + " AS like_count,"
                        + " COALESCE(SUM(CASE WHEN reaction = 'dislike' THEN 1 ELSE 0 END), 0)"
                        + " AS dislike_count,"
                        + " COUNT(id) AS review_count"
                        + " FROM player_reviews"
                        + " WHERE reviewed_user_id = ? AND deleted_at IS NULL",
                (rs, rowNum) ->
                        new PlayerReviewSummary(
                                rs.getLong("reviewed_user_id"),
                                rs.getLong("like_count"),
                                rs.getLong("dislike_count"),
                                rs.getLong("review_count")),
                reviewedUserId,
                reviewedUserId);
    }

    @Override
    public List<PlayerReview> findRecentReviewsForUser(
            final Long reviewedUserId, final int limit, final int offset) {
        return jdbcTemplate.query(
                "SELECT id, reviewer_user_id, reviewed_user_id,"
                        + " reaction, comment, created_at, updated_at, deleted_at"
                        + " FROM player_reviews"
                        + " WHERE reviewed_user_id = ? AND deleted_at IS NULL"
                        + " ORDER BY updated_at DESC, id DESC"
                        + " LIMIT ? OFFSET ?",
                PLAYER_REVIEW_ROW_MAPPER,
                reviewedUserId,
                limit,
                offset);
    }

    @Override
    public boolean canReview(final Long reviewerUserId, final Long reviewedUserId) {
        if (reviewerUserId == null || reviewerUserId.equals(reviewedUserId)) {
            return false;
        }

        final Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*)"
                                + " FROM matches m"
                                + " JOIN match_participants reviewer"
                                + " ON reviewer.match_id = m.id"
                                + " AND reviewer.user_id = ?"
                                + " AND reviewer.status IN ('joined', 'checked_in')"
                                + " JOIN match_participants reviewed"
                                + " ON reviewed.match_id = m.id"
                                + " AND reviewed.user_id = ?"
                                + " AND reviewed.status IN ('joined', 'checked_in')"
                                + " WHERE "
                                + COMPLETED_MATCH_SQL,
                        Integer.class,
                        reviewerUserId,
                        reviewedUserId);
        return count != null && count > 0;
    }

    private Optional<PlayerReview> findByPairIncludingDeleted(
            final Long reviewerUserId, final Long reviewedUserId) {
        return jdbcTemplate
                .query(
                        "SELECT id, reviewer_user_id, reviewed_user_id,"
                                + " reaction, comment, created_at, updated_at, deleted_at"
                                + " FROM player_reviews"
                                + " WHERE reviewer_user_id = ? AND reviewed_user_id = ?",
                        PLAYER_REVIEW_ROW_MAPPER,
                        reviewerUserId,
                        reviewedUserId)
                .stream()
                .findFirst();
    }

    private static SqlParameterValue reactionParameter(final PlayerReviewReaction reaction) {
        return new SqlParameterValue(Types.OTHER, reaction.getDbValue());
    }

    private static Instant toInstant(final Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
