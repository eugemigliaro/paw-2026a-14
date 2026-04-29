package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.EventTimeFilter;
import ar.edu.itba.paw.models.EventVisibility;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.Sport;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class MatchJdbcDao implements MatchDao {

    private static final String DERIVED_STATUS_SQL =
            "CASE"
                    + " WHEN m.status = 'open'"
                    + " AND COALESCE(m.ends_at, m.starts_at) <= CURRENT_TIMESTAMP"
                    + " THEN 'completed'"
                    + " ELSE CAST(m.status AS VARCHAR(30))"
                    + " END";

    private static final String MATCH_SELECT_WITH_JOINED_PLAYERS =
            "SELECT m.id, m.sport, m.host_user_id, m.address, m.title, m.description,"
                    + " m.starts_at, m.ends_at, m.max_players, m.price_per_player,"
                    + " m.visibility, m.join_policy, "
                    + DERIVED_STATUS_SQL
                    + " AS status, m.banner_image_id, m.deleted, m.deleted_at,"
                    + " m.deleted_by_user_id, m.delete_reason, COUNT(mp.id) AS joined_players";

    private static final String BASE_FROM =
            " FROM matches m"
                    + " LEFT JOIN match_participants mp"
                    + " ON mp.match_id = m.id"
                    + " AND mp.status IN ('joined', 'checked_in', 'invited') ";

    private static final String BASE_GROUP_BY = " GROUP BY m.id";

    @NonNull
    private static final RowMapper<Match> MATCH_ROW_MAPPER =
            (ResultSet rs, int rowNum) -> {
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
                        rs.getObject("banner_image_id") == null
                                ? null
                                : rs.getLong("banner_image_id"),
                        rs.getBoolean("deleted"),
                        toInstant(rs.getTimestamp("deleted_at")),
                        rs.getObject("deleted_by_user_id") == null
                                ? null
                                : rs.getLong("deleted_by_user_id"),
                        rs.getString("delete_reason"));
            };

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public MatchJdbcDao(@NonNull final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("matches")
                        .usingGeneratedKeyColumns("id");
    }

    @Override
    public Match createMatch(
            final Long hostUserId,
            final String address,
            final String title,
            final String description,
            final Instant startsAt,
            final Instant endsAt,
            final int maxPlayers,
            final BigDecimal pricePerPlayer,
            final Sport sport,
            final String visibility,
            final String joinPolicy,
            final String status,
            final Long bannerImageId) {
        final Map<String, Object> values = new HashMap<>();
        values.put("host_user_id", hostUserId);
        values.put("address", address);
        values.put("title", title);
        values.put("description", description);
        values.put("starts_at", Timestamp.from(startsAt));
        values.put("ends_at", endsAt == null ? null : Timestamp.from(endsAt));
        values.put("max_players", maxPlayers);
        values.put("price_per_player", pricePerPlayer);
        values.put("sport", new SqlParameterValue(Types.OTHER, sport.getDbValue()));
        values.put("visibility", new SqlParameterValue(Types.OTHER, visibility));
        values.put("join_policy", new SqlParameterValue(Types.OTHER, joinPolicy));
        values.put("status", new SqlParameterValue(Types.OTHER, status));
        values.put("banner_image_id", bannerImageId);
        values.put("deleted", Boolean.FALSE);
        values.put("created_at", new Timestamp(System.currentTimeMillis()));
        values.put("updated_at", new Timestamp(System.currentTimeMillis()));

        final Number id = jdbcInsert.executeAndReturnKey(values);

        return new Match(
                id.longValue(),
                sport,
                hostUserId,
                address,
                title,
                description,
                startsAt,
                endsAt,
                maxPlayers,
                pricePerPlayer,
                visibility,
                joinPolicy,
                status,
                0,
                bannerImageId);
    }

    @Override
    public boolean updateMatch(
            final Long matchId,
            final Long hostUserId,
            final String address,
            final String title,
            final String description,
            final Instant startsAt,
            final Instant endsAt,
            final int maxPlayers,
            final BigDecimal pricePerPlayer,
            final Sport sport,
            final String visibility,
            final String status,
            final Long bannerImageId) {
        final int updatedRows =
                jdbcTemplate.update(
                        "UPDATE matches"
                                + " SET address = ?, title = ?, description = ?, starts_at = ?,"
                                + " ends_at = ?, max_players = ?, price_per_player = ?, sport = ?,"
                                + " visibility = ?, status = ?, banner_image_id = ?,"
                                + " updated_at = CURRENT_TIMESTAMP"
                                + " WHERE id = ? AND host_user_id = ?",
                        address,
                        title,
                        description,
                        Timestamp.from(startsAt),
                        endsAt == null ? null : Timestamp.from(endsAt),
                        maxPlayers,
                        pricePerPlayer,
                        new SqlParameterValue(Types.OTHER, sport.getDbValue()),
                        new SqlParameterValue(Types.OTHER, visibility),
                        new SqlParameterValue(Types.OTHER, status),
                        bannerImageId,
                        matchId,
                        hostUserId);

        return updatedRows > 0;
    }

    @Override
    public boolean cancelMatch(final Long matchId, final Long hostUserId) {
        final int updatedRows =
                jdbcTemplate.update(
                        "UPDATE matches"
                                + " SET status = 'cancelled', updated_at = CURRENT_TIMESTAMP"
                                + " WHERE id = ? AND host_user_id = ?",
                        matchId,
                        hostUserId);

        return updatedRows > 0;
    }

    @Override
    public boolean softDeleteMatch(
            final Long matchId, final Long deletedByUserId, final String deleteReason) {
        final int updatedRows =
                jdbcTemplate.update(
                        "UPDATE matches"
                                + " SET status = 'cancelled', deleted = TRUE,"
                                + " deleted_at = CURRENT_TIMESTAMP, deleted_by_user_id = ?,"
                                + " delete_reason = ?, updated_at = CURRENT_TIMESTAMP"
                                + " WHERE id = ?",
                        deletedByUserId,
                        deleteReason,
                        matchId);

        return updatedRows > 0;
    }

    @Override
    public Optional<Match> findById(final Long matchId) {
        final String sql =
                MATCH_SELECT_WITH_JOINED_PLAYERS + BASE_FROM + " WHERE m.id = ? GROUP BY m.id";

        return jdbcTemplate.query(sql, MATCH_ROW_MAPPER, matchId).stream().findFirst();
    }

    @Override
    public Optional<Match> findPublicMatchById(final Long matchId) {
        final String sql =
                MATCH_SELECT_WITH_JOINED_PLAYERS
                        + BASE_FROM
                        + " WHERE m.id = ?"
                        + " AND m.visibility = 'public'"
                        + " AND "
                        + DERIVED_STATUS_SQL
                        + " = 'open'"
                        + " GROUP BY m.id";

        return jdbcTemplate.query(sql, MATCH_ROW_MAPPER, matchId).stream().findFirst();
    }

    @Override
    public List<Match> findPublicMatches(
            final String query,
            final List<Sport> sports,
            final EventTimeFilter timeFilter,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final MatchSort sort,
            final ZoneId zoneId,
            final int offset,
            final int limit) {
        final StringBuilder sql = new StringBuilder();
        final List<Object> params = new ArrayList<>();

        sql.append(MATCH_SELECT_WITH_JOINED_PLAYERS);
        sql.append(BASE_FROM);
        sql.append(" WHERE 1=1");
        appendFilters(
                sql,
                params,
                query,
                sports,
                timeFilter,
                startsAtFrom,
                startsAtTo,
                zoneId,
                minPrice,
                maxPrice,
                List.of(EventVisibility.PUBLIC),
                List.of(EventStatus.OPEN),
                Boolean.TRUE);
        sql.append(" AND m.deleted = FALSE");
        sql.append(BASE_GROUP_BY);
        appendOpenSpotsConstraint(sql);
        appendSort(sql, sort);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), MATCH_ROW_MAPPER, params.toArray());
    }

    @Override
    public int countPublicMatches(
            final String query,
            final List<Sport> sports,
            final EventTimeFilter timeFilter,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final ZoneId zoneId) {
        final StringBuilder sql = new StringBuilder();
        final List<Object> params = new ArrayList<>();

        sql.append("SELECT COUNT(*) FROM (");
        sql.append("SELECT m.id");
        sql.append(BASE_FROM);
        sql.append(" WHERE 1=1");
        appendFilters(
                sql,
                params,
                query,
                sports,
                timeFilter,
                startsAtFrom,
                startsAtTo,
                zoneId,
                minPrice,
                maxPrice,
                List.of(EventVisibility.PUBLIC),
                List.of(EventStatus.OPEN),
                Boolean.TRUE);
        sql.append(" AND m.deleted = FALSE");
        sql.append(BASE_GROUP_BY);
        appendOpenSpotsConstraint(sql);
        sql.append(") filtered_matches");

        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    @Override
    public List<Match> findHostedMatches(
            final Long hostUserId,
            final Boolean upcoming,
            final String query,
            final List<Sport> sports,
            final List<EventVisibility> visibility,
            final List<EventStatus> statuses,
            final EventTimeFilter timeFilter,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final MatchSort sort,
            final ZoneId zoneId,
            final int offset,
            final int limit) {
        final StringBuilder sql = new StringBuilder();
        final List<Object> params = new ArrayList<>();

        sql.append(MATCH_SELECT_WITH_JOINED_PLAYERS);
        sql.append(BASE_FROM);
        sql.append(" WHERE m.host_user_id = ?");
        params.add(hostUserId);
        appendFilters(
                sql,
                params,
                query,
                sports,
                timeFilter,
                startsAtFrom,
                startsAtTo,
                zoneId,
                minPrice,
                maxPrice,
                visibility,
                statuses,
                upcoming);
        sql.append(BASE_GROUP_BY);
        appendSort(sql, sort, upcoming);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), MATCH_ROW_MAPPER, params.toArray());
    }

    @Override
    public int countHostedMatches(
            final Long hostUserId,
            final Boolean upcoming,
            final String query,
            final List<Sport> sports,
            final List<EventVisibility> visibility,
            final List<EventStatus> statuses,
            final EventTimeFilter timeFilter,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final ZoneId zoneId) {
        final StringBuilder sql = new StringBuilder();
        final List<Object> params = new ArrayList<>();

        sql.append("SELECT COUNT(*) FROM (");
        sql.append("SELECT m.id");
        sql.append(BASE_FROM);
        sql.append(" WHERE m.host_user_id = ?");
        params.add(hostUserId);
        appendFilters(
                sql,
                params,
                query,
                sports,
                timeFilter,
                startsAtFrom,
                startsAtTo,
                zoneId,
                minPrice,
                maxPrice,
                visibility,
                statuses,
                upcoming);
        sql.append(BASE_GROUP_BY);
        sql.append(") filtered_matches");

        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    @Override
    public List<Match> findJoinedMatches(
            final Long userId,
            final Boolean upcoming,
            final String query,
            final List<Sport> sports,
            final List<EventVisibility> visibility,
            final List<EventStatus> statuses,
            final EventTimeFilter timeFilter,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final MatchSort sort,
            final ZoneId zoneId,
            final int offset,
            final int limit) {
        final StringBuilder sql = new StringBuilder();
        final List<Object> params = new ArrayList<>();

        sql.append(MATCH_SELECT_WITH_JOINED_PLAYERS);
        sql.append(BASE_FROM);
        sql.append(
                " INNER JOIN match_participants me"
                        + " ON me.match_id = m.id"
                        + " AND me.user_id = ?"
                        + " AND me.status IN ('joined', 'checked_in')");
        params.add(userId);
        sql.append(" WHERE 1=1");
        appendFilters(
                sql,
                params,
                query,
                sports,
                timeFilter,
                startsAtFrom,
                startsAtTo,
                zoneId,
                minPrice,
                maxPrice,
                visibility,
                statuses,
                upcoming);
        sql.append(BASE_GROUP_BY);
        appendSort(sql, sort, upcoming);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), MATCH_ROW_MAPPER, params.toArray());
    }

    @Override
    public int countJoinedMatches(
            final Long userId,
            final Boolean upcoming,
            final String query,
            final List<Sport> sports,
            final List<EventVisibility> visibility,
            final List<EventStatus> statuses,
            final EventTimeFilter timeFilter,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final ZoneId zoneId) {
        final StringBuilder sql = new StringBuilder();
        final List<Object> params = new ArrayList<>();

        sql.append("SELECT COUNT(*) FROM (");
        sql.append("SELECT m.id");
        sql.append(BASE_FROM);
        sql.append(
                " INNER JOIN match_participants me"
                        + " ON me.match_id = m.id"
                        + " AND me.user_id = ?"
                        + " AND me.status IN ('joined', 'checked_in')");
        params.add(userId);
        sql.append(" WHERE 1=1");
        appendFilters(
                sql,
                params,
                query,
                sports,
                timeFilter,
                startsAtFrom,
                startsAtTo,
                zoneId,
                minPrice,
                maxPrice,
                visibility,
                statuses,
                upcoming);
        sql.append(BASE_GROUP_BY);
        sql.append(") filtered_matches");

        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    private static void appendStatusFilter(
            final StringBuilder sql, final List<Object> params, final List<EventStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return;
        }

        sql.append(" AND ");
        sql.append(DERIVED_STATUS_SQL);
        sql.append(" IN (");
        sql.append(statuses.stream().map(status -> "?").collect(Collectors.joining(", ")));
        sql.append(")");
        params.addAll(statuses.stream().map(EventStatus::getValue).toList());
    }

    private static void appendVisibilityFilter(
            final StringBuilder sql,
            final List<Object> params,
            final List<EventVisibility> visibility) {
        if (visibility == null || visibility.isEmpty()) {
            return;
        }

        sql.append(" AND CAST(m.visibility AS VARCHAR(30)) IN (");
        sql.append(visibility.stream().map(v -> "?").collect(Collectors.joining(", ")));
        sql.append(")");
        params.addAll(visibility.stream().map(EventVisibility::getValue).toList());
    }

    private static void appendSearchFilter(
            final StringBuilder sql, final List<Object> params, final String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        sql.append(" AND (LOWER(m.title) LIKE ? OR LOWER(COALESCE(m.description, '')) LIKE ?)");
        final String queryPattern = "%" + query.trim().toLowerCase() + "%";
        params.add(queryPattern);
        params.add(queryPattern);
    }

    private static void appendSportFilter(
            final StringBuilder sql, final List<Object> params, final List<Sport> sports) {
        if (sports == null || sports.isEmpty()) {
            return;
        }

        sql.append(" AND CAST(m.sport AS VARCHAR(30)) IN (");
        for (int i = 0; i < sports.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
            params.add(sports.get(i).getDbValue());
        }
        sql.append(")");
    }

    private static void appendSort(final StringBuilder sql, final MatchSort sort) {
        final MatchSort safeSort = sort == null ? MatchSort.SOONEST : sort;

        switch (safeSort) {
            case PRICE_LOW:
                sql.append(" ORDER BY COALESCE(m.price_per_player, 0) ASC, m.starts_at ASC");
                break;
            case SPOTS_DESC:
                sql.append(
                        " ORDER BY (MAX(m.max_players) - COUNT(mp.id)) DESC, " + "m.starts_at ASC");
                break;
            case SOONEST:
            default:
                sql.append(" ORDER BY m.starts_at ASC");
                break;
        }
    }

    private static void appendSort(
            final StringBuilder sql, final MatchSort sort, final Boolean upcoming) {
        if (upcoming == null || upcoming) {
            appendSort(sql, sort);
            return;
        }

        final MatchSort safeSort = sort == null ? MatchSort.SOONEST : sort;
        switch (safeSort) {
            case PRICE_LOW:
                sql.append(" ORDER BY COALESCE(m.price_per_player, 0) ASC, m.starts_at DESC");
                break;
            case SPOTS_DESC:
                sql.append(
                        " ORDER BY (MAX(m.max_players) - COUNT(mp.id)) DESC, "
                                + "m.starts_at DESC");
                break;
            case SOONEST:
            default:
                sql.append(" ORDER BY m.starts_at DESC");
                break;
        }
    }

    private static void appendTimeFilter(
            final StringBuilder sql,
            final List<Object> params,
            final EventTimeFilter timeFilter,
            final ZoneId zoneId,
            final Boolean upcoming) {
        if (timeFilter == null || timeFilter == EventTimeFilter.ALL) {
            return;
        }

        if (Boolean.FALSE.equals(upcoming) && timeFilter == EventTimeFilter.TOMORROW) {
            return;
        }

        final ZoneId safeZoneId = zoneId == null ? ZoneId.systemDefault() : zoneId;

        if (timeFilter == EventTimeFilter.FUTURE) {
            final Timestamp now = Timestamp.from(ZonedDateTime.now(safeZoneId).toInstant());
            sql.append(" AND m.starts_at >= ?");
            params.add(now);
            return;
        }

        final TimeRange timeRange =
                Boolean.FALSE.equals(upcoming)
                        ? buildPastTimeRange(timeFilter, safeZoneId)
                        : buildTimeRange(timeFilter, safeZoneId);
        sql.append(" AND m.starts_at >= ? AND m.starts_at < ?");
        params.add(Timestamp.from(timeRange.start()));
        params.add(Timestamp.from(timeRange.end()));
    }

    private static void appendPriceFilter(
            final StringBuilder sql,
            final List<Object> params,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        if (minPrice != null) {
            sql.append(" AND m.price_per_player >= ?");
            params.add(minPrice);
        }

        if (maxPrice != null) {
            sql.append(" AND m.price_per_player <= ?");
            params.add(maxPrice);
        }
    }

    private static void appendUpcomingConstraint(final StringBuilder sql, final Boolean upcoming) {
        if (upcoming == null) {
            return;
        }

        sql.append(" AND m.starts_at ");
        sql.append(Boolean.TRUE.equals(upcoming) ? ">= CURRENT_TIMESTAMP" : "< CURRENT_TIMESTAMP");
    }

    private static void appendFilters(
            final StringBuilder sql,
            final List<Object> params,
            final String query,
            final List<Sport> sports,
            final EventTimeFilter timeFilter,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final ZoneId zoneId,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final List<EventVisibility> visibility,
            final List<EventStatus> status,
            final Boolean upcoming) {
        appendStatusFilter(sql, params, status);
        appendVisibilityFilter(sql, params, visibility);
        appendSearchFilter(sql, params, query);
        appendSportFilter(sql, params, sports);
        appendDateRangeFilter(sql, params, startsAtFrom, startsAtTo);
        if (startsAtFrom == null && startsAtTo == null) {
            appendTimeFilter(sql, params, timeFilter, zoneId, upcoming);
        }
        appendPriceFilter(sql, params, minPrice, maxPrice);
        appendUpcomingConstraint(sql, upcoming);
    }

    private static void appendDateRangeFilter(
            final StringBuilder sql,
            final List<Object> params,
            final Instant startsAtFrom,
            final Instant startsAtTo) {
        if (startsAtFrom != null) {
            sql.append(" AND m.starts_at >= ?");
            params.add(Timestamp.from(startsAtFrom));
        }

        if (startsAtTo != null) {
            sql.append(" AND m.starts_at < ?");
            params.add(Timestamp.from(startsAtTo));
        }
    }

    private static void appendOpenSpotsConstraint(final StringBuilder sql) {
        sql.append(" HAVING MAX(m.max_players) - COUNT(mp.id) >= 1");
    }

    private static TimeRange buildTimeRange(final EventTimeFilter timeFilter, final ZoneId zoneId) {
        final ZonedDateTime now = ZonedDateTime.now(zoneId);

        if (timeFilter == EventTimeFilter.WEEK) {
            return new TimeRange(now.toInstant(), now.plusDays(7).toInstant());
        }

        final LocalDate today = now.toLocalDate();
        if (timeFilter == EventTimeFilter.TOMORROW) {
            final ZonedDateTime start = today.plusDays(1).atStartOfDay(zoneId);
            return new TimeRange(start.toInstant(), start.plusDays(1).toInstant());
        }

        if (timeFilter == EventTimeFilter.TODAY) {
            final ZonedDateTime end = today.plusDays(1).atStartOfDay(zoneId);
            return new TimeRange(now.toInstant(), end.toInstant());
        }
        final ZonedDateTime startOfTomorrow = today.plusDays(1).atStartOfDay(zoneId);
        return new TimeRange(now.toInstant(), startOfTomorrow.toInstant());
    }

    private static TimeRange buildPastTimeRange(
            final EventTimeFilter timeFilter, final ZoneId zoneId) {
        final ZonedDateTime now = ZonedDateTime.now(zoneId);
        final LocalDate today = now.toLocalDate();

        if (timeFilter == EventTimeFilter.TODAY) {
            final ZonedDateTime start = today.atStartOfDay(zoneId);
            return new TimeRange(start.toInstant(), now.toInstant());
        }

        final ZonedDateTime start = now.minusDays(7);
        return new TimeRange(start.toInstant(), now.toInstant());
    }

    private static Instant toInstant(final Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record TimeRange(Instant start, Instant end) {}
}
