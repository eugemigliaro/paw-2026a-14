package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MatchParticipantJdbcDao implements MatchParticipantDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchParticipantJdbcDao.class);
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
        LOGGER.debug("Attempting reservation insert matchId={} userId={}", matchId, userId);
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
                                + " AND m.join_policy = 'direct'"
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
        if (insertedRows != 1) {
            LOGGER.debug("Reservation insert rejected matchId={} userId={}", matchId, userId);
        }
        return insertedRows == 1;
    }

    @Override
    public int createSeriesReservationsIfSpace(
            final Long seriesId, final Long userId, final Instant startsAfter) {
        LOGGER.debug(
                "Attempting series reservation insert seriesId={} userId={} startsAfter={}",
                seriesId,
                userId,
                startsAfter);
        final Timestamp startsAfterTimestamp = Timestamp.from(startsAfter);
        final int restoredRows =
                jdbcTemplate.update(
                        "UPDATE match_participants"
                                + " SET status = 'joined', joined_at = CURRENT_TIMESTAMP"
                                + " WHERE user_id = ?"
                                + " AND status = 'cancelled'"
                                + " AND match_id IN ("
                                + " SELECT m.id"
                                + " FROM matches m"
                                + " LEFT JOIN match_participants active"
                                + " ON active.match_id = m.id"
                                + " AND active.status IN ('joined', 'checked_in')"
                                + " WHERE m.series_id = ?"
                                + " AND m.visibility = 'public'"
                                + " AND m.join_policy = 'direct'"
                                + " AND m.status = 'open'"
                                + " AND m.starts_at > ?"
                                + " GROUP BY m.id, m.max_players"
                                + " HAVING COUNT(active.id) < MAX(m.max_players)"
                                + ")",
                        userId,
                        seriesId,
                        startsAfterTimestamp);

        final int insertedRows =
                jdbcTemplate.update(
                        "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                                + " SELECT m.id, ?, 'joined', CURRENT_TIMESTAMP"
                                + " FROM matches m"
                                + " LEFT JOIN match_participants mp"
                                + " ON mp.match_id = m.id"
                                + " AND mp.status IN ('joined', 'checked_in')"
                                + " WHERE m.series_id = ?"
                                + " AND m.visibility = 'public'"
                                + " AND m.join_policy = 'direct'"
                                + " AND m.status = 'open'"
                                + " AND m.starts_at > ?"
                                + " AND NOT EXISTS ("
                                + " SELECT 1 FROM match_participants existing"
                                + " WHERE existing.match_id = m.id AND existing.user_id = ?)"
                                + " GROUP BY m.id, m.max_players"
                                + " HAVING COUNT(mp.id) < MAX(m.max_players)",
                        userId,
                        seriesId,
                        startsAfterTimestamp,
                        userId);
        final int reservedRows = restoredRows + insertedRows;
        if (reservedRows == 0) {
            LOGGER.debug(
                    "Series reservation insert rejected seriesId={} userId={}", seriesId, userId);
        }
        return reservedRows;
    }

    @Override
    public int cancelFutureSeriesReservations(
            final Long seriesId, final Long userId, final Instant startsAfter) {
        LOGGER.debug(
                "Attempting future series reservation cancellation seriesId={} userId={} startsAfter={}",
                seriesId,
                userId,
                startsAfter);
        final int updatedRows =
                jdbcTemplate.update(
                        "UPDATE match_participants"
                                + " SET status = 'cancelled'"
                                + " WHERE user_id = ?"
                                + " AND status IN ('joined', 'checked_in')"
                                + " AND match_id IN ("
                                + " SELECT id FROM matches"
                                + " WHERE series_id = ?"
                                + " AND starts_at > ?"
                                + ")",
                        userId,
                        seriesId,
                        Timestamp.from(startsAfter));
        if (updatedRows == 0) {
            LOGGER.debug(
                    "Future series reservation cancellation found no rows seriesId={} userId={}",
                    seriesId,
                    userId);
        }
        return updatedRows;
    }

    @Override
    public List<User> findConfirmedParticipants(final Long matchId) {
        return jdbcTemplate.query(
                "SELECT u.id, u.email, u.username, u.name, u.last_name, u.phone, u.profile_image_id"
                        + " FROM match_participants mp"
                        + " JOIN users u ON u.id = mp.user_id"
                        + " WHERE mp.match_id = ?"
                        + " AND mp.status IN ('joined', 'checked_in')"
                        + " ORDER BY mp.joined_at ASC, u.username ASC",
                (rs, rowNum) ->
                        new User(
                                rs.getLong("id"),
                                rs.getString("email"),
                                rs.getString("username"),
                                rs.getString("name"),
                                rs.getString("last_name"),
                                rs.getString("phone"),
                                rs.getObject("profile_image_id") == null
                                        ? null
                                        : rs.getLong("profile_image_id")),
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
        final int restoredRows =
                jdbcTemplate.update(
                        "UPDATE match_participants"
                                + " SET status = 'pending_approval', joined_at = CURRENT_TIMESTAMP"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status IN ('cancelled', 'declined_invite')",
                        matchId,
                        userId);

        if (restoredRows == 1) {
            return true;
        }

        final int insertedRows =
                jdbcTemplate.update(
                        "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                                + " SELECT ?, ?, 'pending_approval', CURRENT_TIMESTAMP"
                                + " FROM users u"
                                + " WHERE u.id = ?"
                                + " AND NOT EXISTS ("
                                + "   SELECT 1 FROM match_participants"
                                + "   WHERE match_id = ? AND user_id = ?)",
                        matchId,
                        userId,
                        userId,
                        matchId,
                        userId);
        return insertedRows == 1;
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

    @Override
    public boolean inviteUser(final Long matchId, final Long userId) {
        final int restoredRows =
                jdbcTemplate.update(
                        "UPDATE match_participants"
                                + " SET status = 'invited', joined_at = CURRENT_TIMESTAMP"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status IN ('cancelled', 'declined_invite')",
                        matchId,
                        userId);

        if (restoredRows == 1) {
            return true;
        }

        final int insertedRows =
                jdbcTemplate.update(
                        "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                                + " SELECT ?, ?, 'invited', CURRENT_TIMESTAMP"
                                + " FROM users u"
                                + " WHERE u.id = ?"
                                + " AND NOT EXISTS ("
                                + "   SELECT 1 FROM match_participants"
                                + "   WHERE match_id = ? AND user_id = ?)",
                        matchId,
                        userId,
                        userId,
                        matchId,
                        userId);
        return insertedRows == 1;
    }

    @Override
    public boolean hasInvitation(final Long matchId, final Long userId) {
        final Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status = 'invited'",
                        Integer.class,
                        matchId,
                        userId);
        return count != null && count > 0;
    }

    @Override
    public boolean acceptInvite(final Long matchId, final Long userId) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE match_participants SET status = 'joined'"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status = 'invited'",
                        matchId,
                        userId);
        return rows == 1;
    }

    @Override
    public boolean declineInvite(final Long matchId, final Long userId) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE match_participants SET status = 'declined_invite'"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status = 'invited'",
                        matchId,
                        userId);
        return rows == 1;
    }

    @Override
    public List<User> findInvitedUsers(final Long matchId) {
        return jdbcTemplate.query(
                "SELECT u.id, u.email, u.username"
                        + " FROM match_participants mp"
                        + " JOIN users u ON u.id = mp.user_id"
                        + " WHERE mp.match_id = ?"
                        + " AND mp.status = 'invited'"
                        + " ORDER BY mp.joined_at ASC, u.username ASC",
                (rs, rowNum) ->
                        new User(rs.getLong("id"), rs.getString("email"), rs.getString("username")),
                matchId);
    }

    @Override
    public List<User> findDeclinedInvitees(final Long matchId) {
        return jdbcTemplate.query(
                "SELECT u.id, u.email, u.username"
                        + " FROM match_participants mp"
                        + " JOIN users u ON u.id = mp.user_id"
                        + " WHERE mp.match_id = ?"
                        + " AND mp.status = 'declined_invite'"
                        + " ORDER BY mp.joined_at ASC, u.username ASC",
                (rs, rowNum) ->
                        new User(rs.getLong("id"), rs.getString("email"), rs.getString("username")),
                matchId);
    }

    @Override
    public List<Long> findInvitedMatchIds(final Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT match_id FROM match_participants"
                        + " WHERE user_id = ? AND status = 'invited'"
                        + " ORDER BY joined_at ASC",
                Long.class,
                userId);
    }
}
