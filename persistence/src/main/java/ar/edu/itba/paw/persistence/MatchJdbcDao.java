package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.EventTimeFilter;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSort;
import ar.edu.itba.paw.models.Sport;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

@Repository
public class MatchJdbcDao implements MatchDao {

    private static final String BASE_FROM =
            " FROM matches m"
                    + " LEFT JOIN match_participants mp"
                    + " ON mp.match_id = m.id"
                    + " AND mp.status IN ('joined', 'checked_in') ";

    private static final String BASE_GROUP_BY =
            " GROUP BY m.id" + " HAVING MAX(m.max_players) > COUNT(mp.id) ";

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
                        rs.getString("status"),
                        rs.getInt("joined_players"));
            };

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public MatchJdbcDao(final DataSource dataSource) {
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
            final java.time.Instant startsAt,
            final java.time.Instant endsAt,
            final int maxPlayers,
            final BigDecimal pricePerPlayer,
            final Sport sport,
            final String visibility,
            final String status) {
        final Map<String, Object> values = new HashMap<>();
        values.put("host_user_id", hostUserId);
        values.put("address", address);
        values.put("title", title);
        values.put("description", description);
        values.put("starts_at", Timestamp.from(startsAt));
        values.put("ends_at", endsAt == null ? null : Timestamp.from(endsAt));
        values.put("max_players", maxPlayers);
        values.put("price_per_player", pricePerPlayer);
        values.put("sport", sport.getDbValue());
        values.put("visibility", visibility);
        values.put("status", status);
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
                status,
                0);
    }

    @Override
    public List<Match> findPublicMatches(
            final String query,
            final Sport sport,
            final EventTimeFilter timeFilter,
            final MatchSort sort,
            final ZoneId zoneId,
            final int offset,
            final int limit) {
        final StringBuilder sql = new StringBuilder();
        final List<Object> params = new ArrayList<>();

        sql.append("SELECT m.*, COUNT(mp.id) AS joined_players");
        sql.append(BASE_FROM);
        appendFilters(sql, params, query, sport, timeFilter, zoneId);
        sql.append(BASE_GROUP_BY);
        appendSort(sql, sort);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), MATCH_ROW_MAPPER, params.toArray());
    }

    @Override
    public int countPublicMatches(
            final String query,
            final Sport sport,
            final EventTimeFilter timeFilter,
            final ZoneId zoneId) {
        final StringBuilder sql = new StringBuilder();
        final List<Object> params = new ArrayList<>();

        sql.append("SELECT COUNT(*) FROM (");
        sql.append("SELECT m.id");
        sql.append(BASE_FROM);
        appendFilters(sql, params, query, sport, timeFilter, zoneId);
        sql.append(BASE_GROUP_BY);
        sql.append(") filtered_matches");

        return jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    private static void appendFilters(
            final StringBuilder sql,
            final List<Object> params,
            final String query,
            final Sport sport,
            final EventTimeFilter timeFilter,
            final ZoneId zoneId) {
        sql.append(" WHERE m.visibility = 'public' AND m.status = 'open'");

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (LOWER(m.title) LIKE ? OR LOWER(COALESCE(m.description, '')) LIKE ?)");
            final String queryPattern = "%" + query.trim().toLowerCase() + "%";
            params.add(queryPattern);
            params.add(queryPattern);
        }

        if (sport != null) {
            sql.append(" AND CAST(m.sport AS VARCHAR(30)) = ?");
            params.add(sport.getDbValue());
        }

        if (timeFilter != null) {
            if (timeFilter == EventTimeFilter.ALL) {
                final Timestamp now = Timestamp.from(ZonedDateTime.now(zoneId).toInstant());
                sql.append(" AND m.starts_at >= ?");
                params.add(now);
            } else {
                final TimeRange timeRange = buildTimeRange(timeFilter, zoneId);
                sql.append(" AND m.starts_at >= ? AND m.starts_at < ?");
                params.add(Timestamp.from(timeRange.start()));
                params.add(Timestamp.from(timeRange.end()));
            }
        }
    }

    private static void appendSort(final StringBuilder sql, final MatchSort sort) {
        final MatchSort safeSort = sort == null ? MatchSort.SOONEST : sort;

        switch (safeSort) {
            case PRICE_LOW:
                sql.append(" ORDER BY COALESCE(m.price_per_player, 0) ASC, m.starts_at ASC");
                break;
            case SPOTS_DESC:
                sql.append(" ORDER BY (MAX(m.max_players) - COUNT(mp.id)) DESC, m.starts_at ASC");
                break;
            case SOONEST:
            default:
                sql.append(" ORDER BY m.starts_at ASC");
                break;
        }
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

        final ZonedDateTime start = today.atStartOfDay(zoneId);
        return new TimeRange(start.toInstant(), start.plusDays(1).toInstant());
    }

    private record TimeRange(java.time.Instant start, java.time.Instant end) {}
}
