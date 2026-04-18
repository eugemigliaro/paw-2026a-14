package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.EmailActionRequest;
import ar.edu.itba.paw.models.EmailActionStatus;
import ar.edu.itba.paw.models.EmailActionType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

@Repository
public class EmailActionRequestJdbcDao implements EmailActionRequestDao {

    private static final RowMapper<EmailActionRequest> EMAIL_ACTION_REQUEST_ROW_MAPPER =
            (rs, rowNum) -> mapRow(rs);

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;
    private final Clock clock;

    @Autowired
    public EmailActionRequestJdbcDao(final DataSource dataSource, final Clock clock) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("email_action_requests")
                        .usingGeneratedKeyColumns("id");
        this.clock = clock;
    }

    @Override
    public EmailActionRequest create(
            final EmailActionType actionType,
            final String email,
            final Long userId,
            final String tokenHash,
            final String payloadJson,
            final Instant expiresAt) {
        final Timestamp now = Timestamp.from(Instant.now(clock));
        final Map<String, Object> values = new HashMap<>();
        values.put("action_type", new SqlParameterValue(Types.OTHER, actionType.getDbValue()));
        values.put("email", email);
        values.put("user_id", userId);
        values.put("token_hash", tokenHash);
        values.put("payload_json", payloadJson);
        values.put(
                "status",
                new SqlParameterValue(Types.OTHER, EmailActionStatus.PENDING.getDbValue()));
        values.put("expires_at", Timestamp.from(expiresAt));
        values.put("consumed_at", null);
        values.put("created_at", now);
        values.put("updated_at", now);

        final Number id = jdbcInsert.executeAndReturnKey(values);

        return new EmailActionRequest(
                id.longValue(),
                actionType,
                email,
                userId,
                tokenHash,
                payloadJson,
                EmailActionStatus.PENDING,
                expiresAt,
                null,
                now.toInstant(),
                now.toInstant());
    }

    @Override
    public Optional<EmailActionRequest> findByTokenHash(final String tokenHash) {
        return jdbcTemplate
                .query(
                        "SELECT * FROM email_action_requests WHERE token_hash = ?",
                        EMAIL_ACTION_REQUEST_ROW_MAPPER,
                        tokenHash)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<EmailActionRequest> findByTokenHashForUpdate(final String tokenHash) {
        return jdbcTemplate
                .query(
                        "SELECT * FROM email_action_requests WHERE token_hash = ? FOR UPDATE",
                        EMAIL_ACTION_REQUEST_ROW_MAPPER,
                        tokenHash)
                .stream()
                .findFirst();
    }

    @Override
    public void updateStatus(
            final Long id,
            final EmailActionStatus status,
            final Long userId,
            final Instant consumedAt) {
        jdbcTemplate.update(
                "UPDATE email_action_requests"
                        + " SET status = ?, user_id = ?, consumed_at = ?, updated_at = ?"
                        + " WHERE id = ?",
                new SqlParameterValue(Types.OTHER, status.getDbValue()),
                userId,
                consumedAt == null ? null : Timestamp.from(consumedAt),
                Timestamp.from(Instant.now(clock)),
                id);
    }

    @Override
    public void expirePendingByEmailAndActionType(
            final EmailActionType actionType, final String email, final Instant consumedAt) {
        jdbcTemplate.update(
                "UPDATE email_action_requests"
                        + " SET status = ?, consumed_at = ?, updated_at = ?"
                        + " WHERE action_type = ? AND email = ? AND status = ?",
                new SqlParameterValue(Types.OTHER, EmailActionStatus.EXPIRED.getDbValue()),
                Timestamp.from(consumedAt),
                Timestamp.from(consumedAt),
                new SqlParameterValue(Types.OTHER, actionType.getDbValue()),
                email,
                new SqlParameterValue(Types.OTHER, EmailActionStatus.PENDING.getDbValue()));
    }

    private static EmailActionRequest mapRow(final ResultSet rs) throws SQLException {
        final String actionTypeRaw = rs.getString("action_type");
        final String statusRaw = rs.getString("status");
        return new EmailActionRequest(
                rs.getLong("id"),
                EmailActionType.fromDbValue(actionTypeRaw)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Unknown email_action_type: " + actionTypeRaw)),
                rs.getString("email"),
                rs.getObject("user_id") == null ? null : rs.getLong("user_id"),
                rs.getString("token_hash"),
                rs.getString("payload_json"),
                EmailActionStatus.fromDbValue(statusRaw)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Unknown email_action_status: " + statusRaw)),
                rs.getTimestamp("expires_at").toInstant(),
                toInstant(rs.getTimestamp("consumed_at")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private static Instant toInstant(final Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
