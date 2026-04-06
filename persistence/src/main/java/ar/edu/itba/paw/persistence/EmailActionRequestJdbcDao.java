package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.EmailActionRequest;
import ar.edu.itba.paw.models.EmailActionStatus;
import ar.edu.itba.paw.models.EmailActionType;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
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
            (ResultSet rs, int rowNum) ->
                    new EmailActionRequest(
                            rs.getLong("id"),
                            EmailActionType.fromDbValue(rs.getString("action_type"))
                                    .orElse(EmailActionType.MATCH_RESERVATION),
                            rs.getString("email"),
                            rs.getObject("user_id") == null ? null : rs.getLong("user_id"),
                            rs.getString("token_hash"),
                            rs.getString("payload_json"),
                            EmailActionStatus.fromDbValue(rs.getString("status"))
                                    .orElse(EmailActionStatus.PENDING),
                            rs.getTimestamp("expires_at").toInstant(),
                            toInstant(rs.getTimestamp("consumed_at")),
                            rs.getTimestamp("created_at").toInstant(),
                            rs.getTimestamp("updated_at").toInstant());

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public EmailActionRequestJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("email_action_requests")
                        .usingGeneratedKeyColumns("id");
    }

    @Override
    public EmailActionRequest create(
            final EmailActionType actionType,
            final String email,
            final Long userId,
            final String tokenHash,
            final String payloadJson,
            final Instant expiresAt) {
        final Timestamp now = Timestamp.from(Instant.now());
        final Map<String, Object> values = new HashMap<>();
        values.put("action_type", new SqlParameterValue(Types.OTHER, actionType.getDbValue()));
        values.put("email", email);
        values.put("user_id", userId);
        values.put("token_hash", tokenHash);
        values.put("payload_json", payloadJson);
        values.put("status", new SqlParameterValue(Types.OTHER, EmailActionStatus.PENDING.getDbValue()));
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
                Timestamp.from(Instant.now()),
                id);
    }

    private static Instant toInstant(final Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
