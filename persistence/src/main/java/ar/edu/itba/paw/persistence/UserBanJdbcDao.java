package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.BanAppealDecision;
import ar.edu.itba.paw.models.UserBan;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class UserBanJdbcDao implements UserBanDao {

    @NonNull
    private static final RowMapper<UserBan> USER_BAN_ROW_MAPPER =
            (ResultSet rs, int rowNum) ->
                    new UserBan(
                            rs.getLong("id"),
                            rs.getLong("user_id"),
                            rs.getLong("banned_by_user_id"),
                            rs.getString("reason"),
                            toInstant(rs.getTimestamp("banned_until")),
                            toInstant(rs.getTimestamp("created_at")),
                            rs.getString("appeal_reason"),
                            rs.getInt("appeal_count"),
                            toInstant(rs.getTimestamp("appealed_at")),
                            toInstant(rs.getTimestamp("appeal_resolved_at")),
                            rs.getObject("appeal_resolved_by_user_id") == null
                                    ? null
                                    : rs.getLong("appeal_resolved_by_user_id"),
                            BanAppealDecision.fromDbValue(rs.getString("appeal_decision"))
                                    .orElse(null));

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public UserBanJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("user_bans")
                        .usingColumns(
                                "user_id",
                                "banned_by_user_id",
                                "reason",
                                "banned_until",
                                "created_at",
                                "updated_at",
                                "appeal_count")
                        .usingGeneratedKeyColumns("id");
    }

    @Override
    public UserBan createBan(
            final Long userId,
            final Long bannedByUserId,
            final Instant bannedUntil,
            final String reason) {
        final Instant now = Instant.now();
        final Map<String, Object> values = new HashMap<>();
        values.put("user_id", userId);
        values.put("banned_by_user_id", bannedByUserId);
        values.put("reason", reason);
        values.put("banned_until", Timestamp.from(bannedUntil));
        values.put("created_at", Timestamp.from(now));
        values.put("updated_at", Timestamp.from(now));
        values.put("appeal_count", 0);

        final Number key = jdbcInsert.executeAndReturnKey(values);

        return new UserBan(
                key.longValue(),
                userId,
                bannedByUserId,
                reason,
                bannedUntil,
                now,
                null,
                0,
                null,
                null,
                null,
                null);
    }

    @Override
    public Optional<UserBan> findLatestBanForUser(final Long userId) {
        return jdbcTemplate
                .query(
                        "SELECT id, user_id, banned_by_user_id, reason, banned_until, created_at,"
                                + " appeal_reason, appeal_count, appealed_at, appeal_resolved_at,"
                                + " appeal_resolved_by_user_id, appeal_decision"
                                + " FROM user_bans WHERE user_id = ?"
                                + " ORDER BY created_at DESC, id DESC LIMIT 1",
                        USER_BAN_ROW_MAPPER,
                        userId)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<UserBan> findById(final Long banId) {
        return jdbcTemplate
                .query(
                        "SELECT id, user_id, banned_by_user_id, reason, banned_until, created_at,"
                                + " appeal_reason, appeal_count, appealed_at, appeal_resolved_at,"
                                + " appeal_resolved_by_user_id, appeal_decision"
                                + " FROM user_bans WHERE id = ?",
                        USER_BAN_ROW_MAPPER,
                        banId)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<UserBan> findActiveBanForUser(final Long userId, final Instant now) {
        return jdbcTemplate
                .query(
                        "SELECT id, user_id, banned_by_user_id, reason, banned_until, created_at,"
                                + " appeal_reason, appeal_count, appealed_at, appeal_resolved_at,"
                                + " appeal_resolved_by_user_id, appeal_decision"
                                + " FROM user_bans"
                                + " WHERE user_id = ?"
                                + " AND banned_until > ?"
                                + " AND (appeal_decision IS NULL OR appeal_decision <> 'lifted')"
                                + " ORDER BY created_at DESC, id DESC LIMIT 1",
                        USER_BAN_ROW_MAPPER,
                        userId,
                        Timestamp.from(now))
                .stream()
                .findFirst();
    }

    @Override
    public List<UserBan> findPendingAppeals() {
        return jdbcTemplate.query(
                "SELECT id, user_id, banned_by_user_id, reason, banned_until, created_at,"
                        + " appeal_reason, appeal_count, appealed_at, appeal_resolved_at,"
                        + " appeal_resolved_by_user_id, appeal_decision"
                        + " FROM user_bans WHERE appeal_count = 1 AND appeal_resolved_at IS NULL"
                        + " ORDER BY appealed_at DESC, id DESC",
                USER_BAN_ROW_MAPPER);
    }

    @Override
    public boolean appealBan(
            final Long banId, final String appealReason, final Instant appealedAt) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE user_bans"
                                + " SET appeal_reason = ?, appeal_count = 1, appealed_at = ?,"
                                + " updated_at = CURRENT_TIMESTAMP"
                                + " WHERE id = ? AND appeal_count = 0",
                        appealReason,
                        Timestamp.from(appealedAt),
                        banId);
        return rows == 1;
    }

    @Override
    public boolean resolveAppeal(
            final Long banId,
            final Long adminUserId,
            final BanAppealDecision decision,
            final Instant resolvedAt) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE user_bans"
                                + " SET appeal_resolved_by_user_id = ?, appeal_resolved_at = ?,"
                                + " appeal_decision = ?, updated_at = CURRENT_TIMESTAMP"
                                + " WHERE id = ? AND appeal_count = 1"
                                + " AND appeal_resolved_at IS NULL",
                        adminUserId,
                        Timestamp.from(resolvedAt),
                        new SqlParameterValue(Types.OTHER, decision.getDbValue()),
                        banId);
        return rows == 1;
    }

    @Override
    public List<UserBan> findBansForUser(final Long userId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, banned_by_user_id, reason, banned_until, created_at,"
                        + " appeal_reason, appeal_count, appealed_at, appeal_resolved_at,"
                        + " appeal_resolved_by_user_id, appeal_decision"
                        + " FROM user_bans WHERE user_id = ?"
                        + " ORDER BY created_at DESC, id DESC",
                USER_BAN_ROW_MAPPER,
                userId);
    }

    private static Instant toInstant(final Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
