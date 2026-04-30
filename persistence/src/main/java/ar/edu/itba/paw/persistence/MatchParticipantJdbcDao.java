package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MatchParticipantJdbcDao implements MatchParticipantDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchParticipantJdbcDao.class);
    private final JdbcTemplate jdbcTemplate;
    private final boolean supportsOnConflictDoNothing;

    @Autowired
    public MatchParticipantJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.supportsOnConflictDoNothing = supportsOnConflictDoNothing(dataSource);
    }

    private static boolean supportsOnConflictDoNothing(final DataSource dataSource) {
        try (final Connection connection = dataSource.getConnection()) {
            return "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        } catch (final SQLException e) {
            LOGGER.debug(
                    "Could not determine database product for participant conflict handling", e);
            return false;
        }
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
    public List<Long> findActiveFutureReservationMatchIdsForSeries(
            final Long seriesId, final Long userId, final Instant startsAfter) {
        return jdbcTemplate.queryForList(
                "SELECT mp.match_id"
                        + " FROM match_participants mp"
                        + " JOIN matches m ON m.id = mp.match_id"
                        + " WHERE m.series_id = ?"
                        + " AND m.starts_at > ?"
                        + " AND mp.user_id = ?"
                        + " AND mp.status IN ('joined', 'checked_in')"
                        + " ORDER BY m.starts_at ASC, m.id ASC",
                Long.class,
                seriesId,
                Timestamp.from(startsAfter),
                userId);
    }

    @Override
    public List<Long> findPendingFutureRequestMatchIdsForSeries(
            final Long seriesId, final Long userId, final Instant startsAfter) {
        return jdbcTemplate.queryForList(
                "SELECT mp.match_id"
                        + " FROM match_participants mp"
                        + " JOIN matches m ON m.id = mp.match_id"
                        + " WHERE m.series_id = ?"
                        + " AND m.starts_at > ?"
                        + " AND mp.user_id = ?"
                        + " AND mp.status = 'pending_approval'"
                        + " ORDER BY m.starts_at ASC, m.id ASC",
                Long.class,
                seriesId,
                Timestamp.from(startsAfter),
                userId);
    }

    @Override
    public boolean createReservationIfSpace(final Long matchId, final Long userId) {
        LOGGER.debug("Attempting reservation insert matchId={} userId={}", matchId, userId);
        final int restoredRows =
                jdbcTemplate.update(
                        "UPDATE match_participants"
                                + " SET status = 'joined', joined_at = CURRENT_TIMESTAMP"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status NOT IN ('joined', 'checked_in')"
                                + " AND match_id IN ("
                                + " SELECT m.id"
                                + " FROM matches m"
                                + " LEFT JOIN match_participants active"
                                + " ON active.match_id = m.id"
                                + " AND active.status IN ('joined', 'checked_in')"
                                + " WHERE m.id = ?"
                                + " AND ("
                                + " (m.visibility = 'public' AND m.join_policy = 'direct')"
                                + " OR m.host_user_id = ?"
                                + " )"
                                + " AND m.status = 'open'"
                                + " AND m.starts_at > CURRENT_TIMESTAMP"
                                + " GROUP BY m.id, m.max_players"
                                + " HAVING COUNT(active.id) < MAX(m.max_players)"
                                + ")",
                        matchId,
                        userId,
                        matchId,
                        userId);
        if (restoredRows == 1) {
            return true;
        }

        final int insertedRows;
        try {
            insertedRows =
                    jdbcTemplate.update(
                            "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                                    + " SELECT m.id, ?, 'joined', CURRENT_TIMESTAMP"
                                    + " FROM matches m"
                                    + " LEFT JOIN match_participants mp"
                                    + " ON mp.match_id = m.id"
                                    + " AND mp.status IN ('joined', 'checked_in')"
                                    + " WHERE m.id = ?"
                                    + " AND ("
                                    + " (m.visibility = 'public' AND m.join_policy = 'direct')"
                                    + " OR m.host_user_id = ?"
                                    + " )"
                                    + " AND m.status = 'open'"
                                    + " AND m.starts_at > CURRENT_TIMESTAMP"
                                    + " AND NOT EXISTS ("
                                    + " SELECT 1 FROM match_participants existing"
                                    + " WHERE existing.match_id = m.id AND existing.user_id = ?)"
                                    + " GROUP BY m.id, m.max_players"
                                    + " HAVING COUNT(mp.id) < MAX(m.max_players)",
                            userId,
                            matchId,
                            userId,
                            userId);
        } catch (final DuplicateKeyException e) {
            LOGGER.debug(
                    "Reservation insert hit duplicate participant row matchId={} userId={}",
                    matchId,
                    userId);
            return false;
        }
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
                                + " AND status NOT IN ('joined', 'checked_in')"
                                + " AND match_id IN ("
                                + " SELECT m.id"
                                + " FROM matches m"
                                + " LEFT JOIN match_participants active"
                                + " ON active.match_id = m.id"
                                + " AND active.status IN ('joined', 'checked_in')"
                                + " WHERE m.series_id = ?"
                                + " AND ("
                                + " (m.visibility = 'public' AND m.join_policy = 'direct')"
                                + " OR m.host_user_id = ?"
                                + " )"
                                + " AND m.status = 'open'"
                                + " AND m.starts_at > ?"
                                + " GROUP BY m.id, m.max_players"
                                + " HAVING COUNT(active.id) < MAX(m.max_players)"
                                + ")",
                        userId,
                        seriesId,
                        userId,
                        startsAfterTimestamp);

        final String insertSql =
                "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                        + " SELECT m.id, ?, 'joined', CURRENT_TIMESTAMP"
                        + " FROM matches m"
                        + " LEFT JOIN match_participants mp"
                        + " ON mp.match_id = m.id"
                        + " AND mp.status IN ('joined', 'checked_in')"
                        + " WHERE m.series_id = ?"
                        + " AND ("
                        + " (m.visibility = 'public' AND m.join_policy = 'direct')"
                        + " OR m.host_user_id = ?"
                        + " )"
                        + " AND m.status = 'open'"
                        + " AND m.starts_at > ?"
                        + " AND NOT EXISTS ("
                        + " SELECT 1 FROM match_participants existing"
                        + " WHERE existing.match_id = m.id AND existing.user_id = ?)"
                        + " GROUP BY m.id, m.max_players"
                        + " HAVING COUNT(mp.id) < MAX(m.max_players)"
                        + (supportsOnConflictDoNothing
                                ? " ON CONFLICT (match_id, user_id) DO NOTHING"
                                : "");
        int insertedRows = 0;
        try {
            insertedRows =
                    jdbcTemplate.update(
                            insertSql, userId, seriesId, userId, startsAfterTimestamp, userId);
        } catch (final DuplicateKeyException e) {
            LOGGER.debug(
                    "Series reservation insert hit duplicate participant row seriesId={} userId={}",
                    seriesId,
                    userId);
        }
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
    public boolean createSeriesJoinRequestIfSpace(final Long matchId, final Long userId) {
        LOGGER.debug("Attempting series join request insert matchId={} userId={}", matchId, userId);
        final int upgradedRows =
                jdbcTemplate.update(
                        "UPDATE match_participants"
                                + " SET series_request = TRUE, joined_at = CURRENT_TIMESTAMP"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status = 'pending_approval'"
                                + " AND series_request = FALSE"
                                + " AND match_id IN ("
                                + eligibleApprovalRequiredMatchSql()
                                + ")",
                        matchId,
                        userId,
                        matchId);
        if (upgradedRows == 1) {
            return true;
        }

        final int restoredRows =
                jdbcTemplate.update(
                        "UPDATE match_participants"
                                + " SET status = 'pending_approval',"
                                + " series_request = TRUE, joined_at = CURRENT_TIMESTAMP"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status IN ('cancelled', 'declined_invite')"
                                + " AND match_id IN ("
                                + eligibleApprovalRequiredMatchSql()
                                + ")",
                        matchId,
                        userId,
                        matchId);
        if (restoredRows == 1) {
            return true;
        }

        final int insertedRows =
                jdbcTemplate.update(
                        "INSERT INTO match_participants"
                                + " (match_id, user_id, status, joined_at, series_request)"
                                + " SELECT m.id, ?, 'pending_approval', CURRENT_TIMESTAMP, TRUE"
                                + " FROM matches m"
                                + " JOIN users u ON u.id = ?"
                                + " LEFT JOIN match_participants active"
                                + " ON active.match_id = m.id"
                                + " AND active.status IN ('joined', 'checked_in', 'invited')"
                                + " WHERE m.id = ?"
                                + " AND m.visibility = 'public'"
                                + " AND m.join_policy = 'approval_required'"
                                + " AND m.status = 'open'"
                                + " AND m.starts_at > CURRENT_TIMESTAMP"
                                + " AND NOT EXISTS ("
                                + " SELECT 1 FROM match_participants existing"
                                + " WHERE existing.match_id = m.id AND existing.user_id = ?)"
                                + " GROUP BY m.id, m.max_players"
                                + " HAVING COUNT(active.id) < MAX(m.max_players)",
                        userId,
                        userId,
                        matchId,
                        userId);
        if (insertedRows != 1) {
            LOGGER.debug(
                    "Series join request insert rejected matchId={} userId={}", matchId, userId);
        }
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
    public List<PendingJoinRequest> findPendingRequestsForHost(final Long hostUserId) {
        return jdbcTemplate.query(
                "SELECT m.id, m.sport, m.host_user_id, m.address, m.title, m.description,"
                        + " m.starts_at, m.ends_at, m.max_players, m.price_per_player,"
                        + " m.visibility, m.join_policy, m.status, m.banner_image_id,"
                        + " m.series_id, m.series_occurrence_index,"
                        + " COALESCE(active.joined_players, 0) AS joined_players,"
                        + " u.id AS request_user_id, u.email AS request_user_email,"
                        + " u.username AS request_username, u.name AS request_name,"
                        + " u.last_name AS request_last_name, u.phone AS request_phone,"
                        + " u.profile_image_id AS request_profile_image_id,"
                        + " mp.series_request"
                        + " FROM match_participants mp"
                        + " JOIN matches m ON m.id = mp.match_id"
                        + " JOIN users u ON u.id = mp.user_id"
                        + " LEFT JOIN ("
                        + " SELECT match_id, COUNT(*) AS joined_players"
                        + " FROM match_participants"
                        + " WHERE status IN ('joined', 'checked_in', 'invited')"
                        + " GROUP BY match_id"
                        + " ) active ON active.match_id = m.id"
                        + " WHERE m.host_user_id = ?"
                        + " AND m.join_policy = 'approval_required'"
                        + " AND mp.status = 'pending_approval'"
                        + " ORDER BY m.starts_at ASC, mp.joined_at ASC, u.username ASC",
                (rs, rowNum) ->
                        new PendingJoinRequest(
                                mapPendingRequestMatch(rs),
                                mapPendingRequestUser(rs),
                                rs.getBoolean("series_request")),
                hostUserId);
    }

    @Override
    public boolean approveRequest(final Long matchId, final Long userId) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE match_participants SET status = 'joined', series_request = FALSE"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status = 'pending_approval'",
                        matchId,
                        userId);
        return rows == 1;
    }

    @Override
    public int approveSeriesJoinRequest(
            final Long seriesId, final Long userId, final Instant startsAfter) {
        LOGGER.debug(
                "Attempting series join request approval seriesId={} userId={} startsAfter={}",
                seriesId,
                userId,
                startsAfter);
        final Timestamp startsAfterTimestamp = Timestamp.from(startsAfter);
        final int updatedRows =
                jdbcTemplate.update(
                        "UPDATE match_participants"
                                + " SET status = 'joined', series_request = FALSE,"
                                + " joined_at = CURRENT_TIMESTAMP"
                                + " WHERE user_id = ?"
                                + " AND status NOT IN ('joined', 'checked_in')"
                                + " AND match_id IN ("
                                + eligibleApprovalRequiredSeriesMatchSql()
                                + ")",
                        userId,
                        seriesId,
                        startsAfterTimestamp);
        final int insertedRows =
                jdbcTemplate.update(
                        "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                                + " SELECT m.id, ?, 'joined', CURRENT_TIMESTAMP"
                                + " FROM matches m"
                                + " LEFT JOIN match_participants active"
                                + " ON active.match_id = m.id"
                                + " AND active.status IN ('joined', 'checked_in', 'invited')"
                                + " WHERE m.series_id = ?"
                                + " AND m.visibility = 'public'"
                                + " AND m.join_policy = 'approval_required'"
                                + " AND m.status = 'open'"
                                + " AND m.starts_at > ?"
                                + " AND NOT EXISTS ("
                                + " SELECT 1 FROM match_participants existing"
                                + " WHERE existing.match_id = m.id AND existing.user_id = ?)"
                                + " GROUP BY m.id, m.max_players"
                                + " HAVING COUNT(active.id) < MAX(m.max_players)",
                        userId,
                        seriesId,
                        startsAfterTimestamp,
                        userId);
        final int approvedRows = updatedRows + insertedRows;
        if (approvedRows == 0) {
            LOGGER.debug(
                    "Series join request approval rejected seriesId={} userId={}",
                    seriesId,
                    userId);
        } else {
            jdbcTemplate.update(
                    "UPDATE match_participants"
                            + " SET status = 'cancelled', series_request = FALSE"
                            + " WHERE user_id = ?"
                            + " AND status = 'pending_approval'"
                            + " AND series_request = TRUE"
                            + " AND match_id IN ("
                            + " SELECT id FROM matches"
                            + " WHERE series_id = ?"
                            + " AND starts_at > ?"
                            + ")",
                    userId,
                    seriesId,
                    startsAfterTimestamp);
        }
        return approvedRows;
    }

    @Override
    public boolean isSeriesJoinRequest(final Long matchId, final Long userId) {
        final Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM match_participants"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status = 'pending_approval'"
                                + " AND series_request = TRUE",
                        Integer.class,
                        matchId,
                        userId);
        return count != null && count > 0;
    }

    @Override
    public boolean hasPendingSeriesRequest(final Long seriesId, final Long userId) {
        final Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*)"
                                + " FROM match_participants mp"
                                + " JOIN matches m ON m.id = mp.match_id"
                                + " WHERE m.series_id = ?"
                                + " AND mp.user_id = ?"
                                + " AND mp.status = 'pending_approval'"
                                + " AND mp.series_request = TRUE",
                        Integer.class,
                        seriesId,
                        userId);
        return count != null && count > 0;
    }

    @Override
    public boolean rejectRequest(final Long matchId, final Long userId) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE match_participants SET status = 'cancelled', series_request = FALSE"
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
                        "UPDATE match_participants SET status = 'cancelled', series_request = FALSE"
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
        return inviteUser(matchId, userId, false);
    }

    @Override
    public boolean inviteUser(
            final Long matchId, final Long userId, final boolean seriesInvitation) {
        final int restoredRows =
                jdbcTemplate.update(
                        "UPDATE match_participants"
                                + " SET status = 'invited', series_request = ?,"
                                + " joined_at = CURRENT_TIMESTAMP"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status IN ('cancelled', 'declined_invite')",
                        seriesInvitation,
                        matchId,
                        userId);

        if (restoredRows == 1) {
            return true;
        }

        final int insertedRows =
                jdbcTemplate.update(
                        "INSERT INTO match_participants"
                                + " (match_id, user_id, status, joined_at, series_request)"
                                + " SELECT ?, ?, 'invited', CURRENT_TIMESTAMP, ?"
                                + " FROM users u"
                                + " WHERE u.id = ?"
                                + " AND NOT EXISTS ("
                                + "   SELECT 1 FROM match_participants"
                                + "   WHERE match_id = ? AND user_id = ?)",
                        matchId,
                        userId,
                        seriesInvitation,
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
    public boolean isSeriesInvitation(final Long matchId, final Long userId) {
        final Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*)"
                                + " FROM match_participants mp"
                                + " JOIN matches m ON m.id = mp.match_id"
                                + " WHERE mp.match_id = ?"
                                + " AND mp.user_id = ?"
                                + " AND mp.status = 'invited'"
                                + " AND m.series_id IS NOT NULL"
                                + " AND ("
                                + " mp.series_request = TRUE"
                                + " OR EXISTS ("
                                + " SELECT 1"
                                + " FROM match_participants other_mp"
                                + " JOIN matches other_m ON other_m.id = other_mp.match_id"
                                + " WHERE other_mp.user_id = mp.user_id"
                                + " AND other_mp.status = 'invited'"
                                + " AND other_m.series_id = m.series_id"
                                + " AND other_mp.match_id <> mp.match_id"
                                + " )"
                                + " )",
                        Integer.class,
                        matchId,
                        userId);
        return count != null && count > 0;
    }

    @Override
    public boolean acceptInvite(final Long matchId, final Long userId) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE match_participants SET status = 'joined', series_request = FALSE"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status = 'invited'",
                        matchId,
                        userId);
        return rows == 1;
    }

    @Override
    public int acceptSeriesInvite(
            final Long seriesId, final Long userId, final Instant startsAfter) {
        return jdbcTemplate.update(
                "UPDATE match_participants"
                        + " SET status = 'joined', series_request = FALSE"
                        + " WHERE user_id = ?"
                        + " AND status = 'invited'"
                        + " AND match_id IN ("
                        + " SELECT id FROM matches"
                        + " WHERE series_id = ?"
                        + " AND status = 'open'"
                        + " AND starts_at > ?"
                        + ")",
                userId,
                seriesId,
                Timestamp.from(startsAfter));
    }

    @Override
    public boolean declineInvite(final Long matchId, final Long userId) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE match_participants"
                                + " SET status = 'declined_invite', series_request = FALSE"
                                + " WHERE match_id = ? AND user_id = ?"
                                + " AND status = 'invited'",
                        matchId,
                        userId);
        return rows == 1;
    }

    @Override
    public int declineSeriesInvite(final Long seriesId, final Long userId) {
        return jdbcTemplate.update(
                "UPDATE match_participants"
                        + " SET status = 'declined_invite', series_request = FALSE"
                        + " WHERE user_id = ?"
                        + " AND status = 'invited'"
                        + " AND match_id IN ("
                        + " SELECT id FROM matches"
                        + " WHERE series_id = ?"
                        + ")",
                userId,
                seriesId);
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
                "SELECT match_id"
                        + " FROM ("
                        + " SELECT mp.match_id, mp.joined_at, m.starts_at"
                        + " FROM match_participants mp"
                        + " JOIN matches m ON m.id = mp.match_id"
                        + " WHERE mp.user_id = ?"
                        + " AND mp.status = 'invited'"
                        + " AND m.series_id IS NULL"
                        + " UNION ALL"
                        + " SELECT mp.match_id, mp.joined_at, m.starts_at"
                        + " FROM match_participants mp"
                        + " JOIN matches m ON m.id = mp.match_id"
                        + " WHERE mp.user_id = ?"
                        + " AND mp.status = 'invited'"
                        + " AND m.series_id IS NOT NULL"
                        + " AND NOT EXISTS ("
                        + " SELECT 1"
                        + " FROM match_participants earlier"
                        + " JOIN matches em ON em.id = earlier.match_id"
                        + " WHERE earlier.user_id = mp.user_id"
                        + " AND earlier.status = 'invited'"
                        + " AND earlier.series_request = TRUE"
                        + " AND em.series_id = m.series_id"
                        + " AND (em.starts_at < m.starts_at"
                        + " OR (em.starts_at = m.starts_at AND em.id < m.id))"
                        + " )"
                        + " ) invited_matches"
                        + " ORDER BY joined_at ASC, starts_at ASC, match_id ASC",
                Long.class,
                userId,
                userId);
    }

    private static String eligibleApprovalRequiredMatchSql() {
        return " SELECT m.id"
                + " FROM matches m"
                + " LEFT JOIN match_participants active"
                + " ON active.match_id = m.id"
                + " AND active.status IN ('joined', 'checked_in', 'invited')"
                + " WHERE m.id = ?"
                + " AND m.visibility = 'public'"
                + " AND m.join_policy = 'approval_required'"
                + " AND m.status = 'open'"
                + " AND m.starts_at > CURRENT_TIMESTAMP"
                + " GROUP BY m.id, m.max_players"
                + " HAVING COUNT(active.id) < MAX(m.max_players)";
    }

    private static String eligibleApprovalRequiredSeriesMatchSql() {
        return " SELECT m.id"
                + " FROM matches m"
                + " LEFT JOIN match_participants active"
                + " ON active.match_id = m.id"
                + " AND active.status IN ('joined', 'checked_in', 'invited')"
                + " WHERE m.series_id = ?"
                + " AND m.visibility = 'public'"
                + " AND m.join_policy = 'approval_required'"
                + " AND m.status = 'open'"
                + " AND m.starts_at > ?"
                + " GROUP BY m.id, m.max_players"
                + " HAVING COUNT(active.id) < MAX(m.max_players)";
    }

    private static Match mapPendingRequestMatch(final ResultSet rs) throws java.sql.SQLException {
        final Timestamp startsAt = rs.getTimestamp("starts_at");
        final Timestamp endsAt = rs.getTimestamp("ends_at");
        final BigDecimal price = rs.getBigDecimal("price_per_player");
        return new Match(
                rs.getLong("id"),
                Sport.fromDbValue(rs.getString("sport")).orElse(Sport.FOOTBALL),
                rs.getLong("host_user_id"),
                rs.getString("address"),
                rs.getString("title"),
                rs.getString("description"),
                startsAt.toInstant(),
                endsAt == null ? null : endsAt.toInstant(),
                rs.getInt("max_players"),
                price,
                rs.getString("visibility"),
                rs.getString("join_policy"),
                rs.getString("status"),
                rs.getInt("joined_players"),
                rs.getObject("banner_image_id") == null ? null : rs.getLong("banner_image_id"),
                rs.getObject("series_id") == null ? null : rs.getLong("series_id"),
                rs.getObject("series_occurrence_index") == null
                        ? null
                        : rs.getInt("series_occurrence_index"));
    }

    private static User mapPendingRequestUser(final ResultSet rs) throws java.sql.SQLException {
        return new User(
                rs.getLong("request_user_id"),
                rs.getString("request_user_email"),
                rs.getString("request_username"),
                rs.getString("request_name"),
                rs.getString("request_last_name"),
                rs.getString("request_phone"),
                rs.getObject("request_profile_image_id") == null
                        ? null
                        : rs.getLong("request_profile_image_id"));
    }
}
