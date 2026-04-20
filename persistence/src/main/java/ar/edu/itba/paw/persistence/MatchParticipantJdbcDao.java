package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MatchParticipantJdbcDao implements MatchParticipantDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MatchParticipantJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public boolean hasActiveReservation(final Long matchId, final Long userId) {
        final Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status IN ('joined', 'checked_in')",
                        Integer.class,
                        matchId,
                        userId);
        return count != null && count > 0;
    }

    @Override
    public boolean createReservationIfSpace(final Long matchId, final Long userId) {
        final int insertedRows =
                jdbcTemplate.update(
                        "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                                + " SELECT m.id, ?, 'joined', CURRENT_TIMESTAMP"
                                + " FROM matches m"
                                + " LEFT JOIN match_participants mp"
                                + " ON mp.match_id = m.id"
                                + " AND mp.status IN ('joined', 'checked_in')"
                                + " WHERE m.id = ?"
                                + " AND m.visibility = 'public'"
                                + " AND m.status = 'open'"
                                + " AND m.starts_at > CURRENT_TIMESTAMP"
                                + " AND NOT EXISTS ("
                                + " SELECT 1 FROM match_participants existing"
                                + " WHERE existing.match_id = m.id AND existing.user_id = ?"
                                + " AND existing.status IN ('joined', 'checked_in'))"
                                + " GROUP BY m.id, m.max_players"
                                + " HAVING COUNT(mp.id) < MAX(m.max_players)",
                        userId,
                        matchId,
                        userId);
        return insertedRows == 1;
    }

    @Override
    public List<User> findConfirmedParticipants(final Long matchId) {
        return jdbcTemplate.query(
                "SELECT u.id, u.email, u.username"
                        + " FROM match_participants mp"
                        + " JOIN users u ON u.id = mp.user_id"
                        + " WHERE mp.match_id = ?"
                        + " AND mp.status IN ('joined', 'checked_in')"
                        + " ORDER BY mp.joined_at ASC, u.username ASC",
                (rs, rowNum) ->
                        new User(rs.getLong("id"), rs.getString("email"), rs.getString("username")),
                matchId);
    }

    @Override
    public boolean hasPendingRequest(final Long matchId, final Long userId) {
        final Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status = 'pending_approval'",
                        Integer.class,
                        matchId,
                        userId);
        return count != null && count > 0;
    }

    @Override
    public boolean createJoinRequest(final Long matchId, final Long userId) {
        final int rows =
                jdbcTemplate.update(
                        "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                                + " SELECT ?, ?, 'pending_approval', CURRENT_TIMESTAMP"
                                + " WHERE NOT EXISTS ("
                                + "   SELECT 1 FROM match_participants"
                                + "   WHERE match_id = ? AND user_id = ?"
                                + "   AND status IN ('joined', 'checked_in', 'pending_approval'))",
                        matchId,
                        userId,
                        matchId,
                        userId);
        return rows == 1;
    }

    @Override
    public List<User> findPendingRequests(final Long matchId) {
        return jdbcTemplate.query(
                "SELECT u.id, u.email, u.username"
                        + " FROM match_participants mp"
                        + " JOIN users u ON u.id = mp.user_id"
                        + " WHERE mp.match_id = ?"
                        + " AND mp.status = 'pending_approval'"
                        + " ORDER BY mp.joined_at ASC, u.username ASC",
                (rs, rowNum) ->
                        new User(rs.getLong("id"), rs.getString("email"), rs.getString("username")),
                matchId);
    }

    @Override
    public boolean approveRequest(final Long matchId, final Long userId) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE match_participants SET status = 'joined'"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status = 'pending_approval'",
                        matchId,
                        userId);
        return rows == 1;
    }

    @Override
    public boolean rejectRequest(final Long matchId, final Long userId) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE match_participants SET status = 'cancelled'"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status = 'pending_approval'",
                        matchId,
                        userId);
        return rows == 1;
    }

    @Override
    public boolean removeParticipant(final Long matchId, final Long userId) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE match_participants SET status = 'cancelled'"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status IN ('joined', 'checked_in')",
                        matchId,
                        userId);
        return rows == 1;
    }

    @Override
    public boolean cancelJoinRequest(final Long matchId, final Long userId) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE match_participants SET status = 'cancelled'"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status = 'pending_approval'",
                        matchId,
                        userId);
        return rows == 1;
    }

    @Override
    public List<Long> findPendingMatchIds(final Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT match_id FROM match_participants"
                        + " WHERE user_id = ? AND status = 'pending_approval'"
                        + " ORDER BY joined_at ASC",
                Long.class,
                userId);
    }
}
