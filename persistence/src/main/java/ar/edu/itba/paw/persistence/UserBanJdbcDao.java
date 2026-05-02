package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.UserBan;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class UserBanJdbcDao implements UserBanDao {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @NonNull
    private final RowMapper<UserBan> USER_BAN_ROW_MAPPER =
            (rs, rowNum) ->
                    new UserBan(
                            rs.getLong("id"),
                            rs.getLong("moderation_report_id"),
                            toInstant(rs.getTimestamp("banned_until")));

    @Autowired
    public UserBanJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("user_bans")
                        .usingGeneratedKeyColumns("id");
    }

    @Override
    public UserBan createBan(final Long moderationReportId, final Instant bannedUntil) {
        final Map<String, Object> values = new HashMap<>();
        values.put("moderation_report_id", moderationReportId);
        values.put("banned_until", Timestamp.from(bannedUntil));

        final Long id = jdbcInsert.executeAndReturnKey(values).longValue();

        return new UserBan(id, moderationReportId, bannedUntil);
    }

    @Override
    public Optional<UserBan> findLatestBanForUser(final Long userId) {
        return jdbcTemplate
                .query(
                        "SELECT ub.id, ub.moderation_report_id, ub.banned_until "
                                + "FROM user_bans ub "
                                + "JOIN moderation_reports mr ON ub.moderation_report_id = mr.id "
                                + "WHERE mr.target_type = 'user' AND mr.target_id = ? "
                                + "ORDER BY ub.id DESC LIMIT 1",
                        USER_BAN_ROW_MAPPER,
                        userId)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<UserBan> findById(final Long banId) {
        return jdbcTemplate
                .query(
                        "SELECT id, moderation_report_id, banned_until "
                                + "FROM user_bans WHERE id = ?",
                        USER_BAN_ROW_MAPPER,
                        banId)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<UserBan> findActiveBanForUser(final Long userId, final Instant now) {
        return jdbcTemplate
                .query(
                        "SELECT ub.id, ub.moderation_report_id, ub.banned_until "
                                + "FROM user_bans ub "
                                + "JOIN moderation_reports mr ON ub.moderation_report_id = mr.id "
                                + "WHERE mr.target_type = 'user' "
                                + "AND mr.target_id = ? "
                                + "AND ub.banned_until > ? "
                                + "AND (mr.appeal_decision IS NULL OR mr.appeal_decision <> 'lifted') "
                                + "ORDER BY ub.id DESC LIMIT 1",
                        USER_BAN_ROW_MAPPER,
                        userId,
                        Timestamp.from(now))
                .stream()
                .findFirst();
    }

    @Override
    public void upliftBan(final Long banId) {
        jdbcTemplate.update("UPDATE user_bans SET banned_until = NOW() WHERE id = ?", banId);
    }

    private static Instant toInstant(final Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
