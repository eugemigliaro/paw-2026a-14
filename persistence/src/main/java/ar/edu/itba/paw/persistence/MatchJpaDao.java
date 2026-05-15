package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSeries;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.query.EventTimeFilter;
import ar.edu.itba.paw.models.query.MatchSort;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.ParticipantStatus;
import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class MatchJpaDao implements MatchDao {

    private static final List<ParticipantStatus> ACTIVE_PARTICIPANT_STATUSES =
            List.of(
                    ParticipantStatus.JOINED,
                    ParticipantStatus.CHECKED_IN,
                    ParticipantStatus.INVITED);
    private static final List<ParticipantStatus> JOINED_PARTICIPANT_STATUSES =
            List.of(ParticipantStatus.JOINED, ParticipantStatus.CHECKED_IN);

    @PersistenceContext private EntityManager em;

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
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy,
            final EventStatus status,
            final Long bannerImageId,
            final Double latitude,
            final Double longitude,
            final Long seriesId,
            final Integer seriesOccurrenceIndex) {
        final Instant now = Instant.now();
        final Match match =
                new Match(
                        null,
                        sport,
                        hostUserId,
                        address,
                        latitude,
                        longitude,
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
                        bannerImageId,
                        seriesId,
                        seriesOccurrenceIndex);
        match.setHost(em.getReference(UserAccount.class, hostUserId));
        if (seriesId != null) {
            match.setSeries(em.getReference(MatchSeries.class, seriesId));
        }
        match.setCreatedAt(now);
        match.setUpdatedAt(now);

        em.persist(match);

        return match;
    }

    @Override
    public Long createMatchSeries(
            final Long hostUserId,
            final String frequency,
            final Instant startsAt,
            final Instant endsAt,
            final String timezone,
            final LocalDate untilDate,
            final Integer occurrenceCount) {
        final Instant now = Instant.now();
        final MatchSeries series =
                new MatchSeries(
                        hostUserId,
                        frequency,
                        startsAt,
                        endsAt,
                        timezone,
                        untilDate,
                        occurrenceCount,
                        now,
                        now);
        series.setHost(em.getReference(UserAccount.class, hostUserId));

        em.persist(series);

        return series.getId();
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
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy,
            final EventStatus status,
            final Long bannerImageId,
            final Double latitude,
            final Double longitude) {
        final Match match = em.find(Match.class, matchId);

        if (match == null || !match.getHostUserId().equals(hostUserId)) {
            return false;
        }

        match.setAddress(address);
        match.setTitle(title);
        match.setDescription(description);
        match.setStartsAt(startsAt);
        match.setEndsAt(endsAt);
        match.setMaxPlayers(maxPlayers);
        match.setPricePerPlayer(pricePerPlayer);
        match.setSport(sport);
        match.setVisibility(visibility);
        match.setJoinPolicy(joinPolicy);
        match.setStatus(status);
        match.setBannerImageId(bannerImageId);
        match.setLatitude(latitude);
        match.setLongitude(longitude);
        match.setUpdatedAt(Instant.now());
        return true;
    }

    @Override
    public boolean cancelMatch(final Long matchId, final Long hostUserId) {
        final Match match = em.find(Match.class, matchId);

        if (match == null || !match.getHostUserId().equals(hostUserId)) {
            return false;
        }

        match.setStatus(EventStatus.CANCELLED);
        match.setUpdatedAt(Instant.now());
        return true;
    }

    @Override
    public boolean softDeleteMatch(
            final Long matchId, final Long deletedByUserId, final String deleteReason) {
        final Match match = em.find(Match.class, matchId);

        if (match == null) {
            return false;
        }

        final Instant now = Instant.now();

        match.setStatus(EventStatus.CANCELLED);
        match.setDeleted(true);
        match.setDeletedAt(Instant.now());
        match.setDeletedByUserId(deletedByUserId);
        match.setDeleteReason(deleteReason);
        match.setUpdatedAt(now);
        return true;
    }

    @Override
    public boolean restoreMatch(final Long matchId) {
        final Match match = em.find(Match.class, matchId);

        if (match == null) {
            return false;
        }

        final Instant now = Instant.now();

        match.setStatus(EventStatus.CANCELLED);
        match.setDeleted(false);
        match.setDeletedAt(null);
        match.setDeletedByUserId(null);
        match.setDeleteReason(null);
        match.setUpdatedAt(now);
        return true;
    }

    @Override
    public Optional<Match> findById(final Long matchId) {
        return findProjected("m.id = :matchId", Map.of("matchId", matchId)).stream().findFirst();
    }

    @Override
    public Optional<Match> findPublicMatchById(final Long matchId) {
        return findProjected(
                        "m.id = :matchId"
                                + " AND m.visibility = :publicVisibility"
                                + " AND m.status = :openStatus"
                                + " AND COALESCE(m.endsAt, m.startsAt) > CURRENT_TIMESTAMP",
                        Map.of(
                                "matchId",
                                matchId,
                                "publicVisibility",
                                EventVisibility.PUBLIC,
                                "openStatus",
                                EventStatus.OPEN))
                .stream()
                .findFirst();
    }

    @Override
    public List<Match> findSeriesOccurrences(final Long seriesId) {
        final String where = "m.series.id = :seriesId";
        final Map<String, Object> params = Map.of("seriesId", seriesId);
        final TypedQuery<MatchProjection> query =
                em.createQuery(
                        projectedSelect()
                                + " WHERE "
                                + where
                                + " ORDER BY m.startsAt ASC, m.seriesOccurrenceIndex ASC",
                        MatchProjection.class);
        setCommonParams(query);
        setParams(query, params);
        final Instant now = Instant.now();
        return query.getResultList().stream().map(projection -> projection.toMatch(now)).toList();
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
            final Double latitude,
            final Double longitude,
            final int offset,
            final int limit) {
        final QueryParts parts = new QueryParts();
        parts.where.add("m.visibility IN :visibility");
        parts.params.put("visibility", List.of(EventVisibility.PUBLIC));
        appendStatusFilter(parts, List.of(EventStatus.OPEN));
        parts.where.add("m.deleted = FALSE");
        parts.where.add(openSpotsExpression() + " >= 1");
        appendFilters(
                parts,
                query,
                sports,
                timeFilter,
                startsAtFrom,
                startsAtTo,
                zoneId,
                minPrice,
                maxPrice,
                Boolean.TRUE);
        return findPage(parts, sort, Boolean.TRUE, latitude, longitude, offset, limit);
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
        final QueryParts parts = new QueryParts();
        parts.where.add("m.visibility IN :visibility");
        parts.params.put("visibility", List.of(EventVisibility.PUBLIC));
        appendStatusFilter(parts, List.of(EventStatus.OPEN));
        parts.where.add("m.deleted = FALSE");
        parts.where.add(openSpotsExpression() + " >= 1");
        appendFilters(
                parts,
                query,
                sports,
                timeFilter,
                startsAtFrom,
                startsAtTo,
                zoneId,
                minPrice,
                maxPrice,
                Boolean.TRUE);
        return countMatches(parts);
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
        final QueryParts parts = new QueryParts();
        parts.where.add("m.host.id = :hostUserId");
        parts.params.put("hostUserId", hostUserId);
        parts.where.add("m.deleted = FALSE");
        appendManagedFilters(parts, visibility, statuses);
        appendFilters(
                parts,
                query,
                sports,
                timeFilter,
                startsAtFrom,
                startsAtTo,
                zoneId,
                minPrice,
                maxPrice,
                upcoming);
        return findPage(parts, sort, upcoming, null, null, offset, limit);
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
        final QueryParts parts = new QueryParts();
        parts.where.add("m.host.id = :hostUserId");
        parts.params.put("hostUserId", hostUserId);
        parts.where.add("m.deleted = FALSE");
        appendManagedFilters(parts, visibility, statuses);
        appendFilters(
                parts,
                query,
                sports,
                timeFilter,
                startsAtFrom,
                startsAtTo,
                zoneId,
                minPrice,
                maxPrice,
                upcoming);
        return countMatches(parts);
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
        final QueryParts parts = joinedParts(userId);
        appendManagedFilters(parts, visibility, statuses);
        appendFilters(
                parts,
                query,
                sports,
                timeFilter,
                startsAtFrom,
                startsAtTo,
                zoneId,
                minPrice,
                maxPrice,
                upcoming);
        return findPage(parts, sort, upcoming, null, null, offset, limit);
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
        final QueryParts parts = joinedParts(userId);
        appendManagedFilters(parts, visibility, statuses);
        appendFilters(
                parts,
                query,
                sports,
                timeFilter,
                startsAtFrom,
                startsAtTo,
                zoneId,
                minPrice,
                maxPrice,
                upcoming);
        return countMatches(parts);
    }

    private List<Match> findPage(
            final QueryParts parts,
            final MatchSort sort,
            final Boolean upcoming,
            final Double latitude,
            final Double longitude,
            final int offset,
            final int limit) {
        final String where = whereClause(parts);
        final TypedQuery<Long> idQuery =
                em.createQuery(
                                "SELECT m.id FROM Match m JOIN m.host hu"
                                        + where
                                        + orderBy(sort, upcoming, latitude, longitude),
                                Long.class)
                        .setFirstResult(offset)
                        .setMaxResults(limit);
        setCommonParams(idQuery);
        setParams(idQuery, parts.params);
        if (sort == MatchSort.DISTANCE) {
            setDistanceParams(idQuery, latitude, longitude);
        }

        final List<Long> ids = idQuery.getResultList();
        if (ids.isEmpty()) {
            return List.of();
        }

        final TypedQuery<MatchProjection> matchesQuery =
                em.createQuery(projectedSelect() + " WHERE m.id IN :ids", MatchProjection.class);
        setCommonParams(matchesQuery);
        matchesQuery.setParameter("ids", ids);

        final Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            order.put(ids.get(i), i);
        }
        final Instant now = Instant.now();
        return matchesQuery.getResultList().stream()
                .map(projection -> projection.toMatch(now))
                .sorted(Comparator.comparingInt(match -> order.get(match.getId())))
                .toList();
    }

    private int countMatches(final QueryParts parts) {
        final TypedQuery<Long> countQuery =
                em.createQuery(
                        "SELECT COUNT(m.id) FROM Match m JOIN m.host hu" + whereClause(parts),
                        Long.class);
        setCommonParams(countQuery);
        setParams(countQuery, parts.params);
        return countQuery.getSingleResult().intValue();
    }

    private List<Match> findProjected(final String where, final Map<String, Object> params) {
        final TypedQuery<MatchProjection> query =
                em.createQuery(projectedSelect() + " WHERE " + where, MatchProjection.class);
        setCommonParams(query);
        setParams(query, params);
        final Instant now = Instant.now();
        return query.getResultList().stream().map(projection -> projection.toMatch(now)).toList();
    }

    private static QueryParts joinedParts(final Long userId) {
        final QueryParts parts = new QueryParts();
        parts.where.add("m.deleted = FALSE");
        parts.where.add(
                "EXISTS (SELECT 1 FROM MatchParticipant me"
                        + " WHERE me.match = m"
                        + " AND me.user.id = :userId"
                        + " AND me.status IN :joinedStatuses)");
        parts.params.put("userId", userId);
        parts.params.put("joinedStatuses", JOINED_PARTICIPANT_STATUSES);
        return parts;
    }

    private static void appendManagedFilters(
            final QueryParts parts,
            final List<EventVisibility> visibility,
            final List<EventStatus> statuses) {
        if (visibility != null && !visibility.isEmpty()) {
            parts.where.add("m.visibility IN :visibility");
            parts.params.put("visibility", visibility);
        }
        if (statuses != null && !statuses.isEmpty()) {
            appendStatusFilter(parts, statuses);
        }
    }

    private static void appendStatusFilter(
            final QueryParts parts, final List<EventStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return;
        }

        final List<String> clauses = new ArrayList<>();
        final List<EventStatus> directStatuses = new ArrayList<>();
        if (statuses.contains(EventStatus.OPEN)) {
            clauses.add(
                    "(m.status = :openStatus"
                            + " AND COALESCE(m.endsAt, m.startsAt) > CURRENT_TIMESTAMP)");
            parts.params.put("openStatus", EventStatus.OPEN);
        }
        if (statuses.contains(EventStatus.COMPLETED)) {
            clauses.add(
                    "(m.status = :completedStatus"
                            + " OR (m.status = :completedOpenStatus"
                            + " AND COALESCE(m.endsAt, m.startsAt) <= CURRENT_TIMESTAMP))");
            parts.params.put("completedStatus", EventStatus.COMPLETED);
            parts.params.put("completedOpenStatus", EventStatus.OPEN);
        }
        statuses.stream()
                .filter(status -> status != EventStatus.OPEN && status != EventStatus.COMPLETED)
                .forEach(directStatuses::add);
        if (!directStatuses.isEmpty()) {
            clauses.add("m.status IN :directStatuses");
            parts.params.put("directStatuses", directStatuses);
        }
        parts.where.add("(" + String.join(" OR ", clauses) + ")");
    }

    private static void appendFilters(
            final QueryParts parts,
            final String query,
            final List<Sport> sports,
            final EventTimeFilter timeFilter,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final ZoneId zoneId,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final Boolean upcoming) {
        appendSearchFilter(parts, query);
        if (sports != null && !sports.isEmpty()) {
            parts.where.add("m.sport IN :sports");
            parts.params.put("sports", sports);
        }
        appendDateRangeFilter(parts, startsAtFrom, startsAtTo);
        if (startsAtFrom == null && startsAtTo == null) {
            appendTimeFilter(parts, timeFilter, zoneId, upcoming);
        }
        if (minPrice != null) {
            parts.where.add("m.pricePerPlayer >= :minPrice");
            parts.params.put("minPrice", minPrice);
        }
        if (maxPrice != null) {
            parts.where.add("m.pricePerPlayer <= :maxPrice");
            parts.params.put("maxPrice", maxPrice);
        }
        if (upcoming != null) {
            parts.where.add(
                    Boolean.TRUE.equals(upcoming)
                            ? "m.startsAt >= CURRENT_TIMESTAMP"
                            : "m.startsAt < CURRENT_TIMESTAMP");
        }
    }

    private static void appendSearchFilter(final QueryParts parts, final String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        parts.where.add(
                "(LOWER(m.title) LIKE :query"
                        + " OR LOWER(COALESCE(m.description, '')) LIKE :query"
                        + " OR LOWER(COALESCE(m.address, '')) LIKE :query"
                        + " OR LOWER(CAST(m.sport AS string)) LIKE :query"
                        + " OR LOWER(COALESCE(hu.name, '')) LIKE :query"
                        + " OR LOWER(COALESCE(hu.lastName, '')) LIKE :query"
                        + " OR LOWER(COALESCE(hu.username, '')) LIKE :query"
                        + " OR LOWER(CONCAT(COALESCE(hu.name, ''), ' ', COALESCE(hu.lastName, ''))) LIKE :query"
                        + " OR LOWER(CONCAT(COALESCE(hu.lastName, ''), ' ', COALESCE(hu.name, ''))) LIKE :query)");
        parts.params.put("query", "%" + query.trim().toLowerCase() + "%");
    }

    private static void appendDateRangeFilter(
            final QueryParts parts, final Instant startsAtFrom, final Instant startsAtTo) {
        if (startsAtFrom != null) {
            parts.where.add("m.startsAt >= :startsAtFrom");
            parts.params.put("startsAtFrom", startsAtFrom);
        }
        if (startsAtTo != null) {
            parts.where.add("m.startsAt < :startsAtTo");
            parts.params.put("startsAtTo", startsAtTo);
        }
    }

    private static void appendTimeFilter(
            final QueryParts parts,
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
            parts.where.add("m.startsAt >= :timeStart");
            parts.params.put("timeStart", ZonedDateTime.now(safeZoneId).toInstant());
            return;
        }
        final TimeRange range =
                Boolean.FALSE.equals(upcoming)
                        ? buildPastTimeRange(timeFilter, safeZoneId)
                        : buildTimeRange(timeFilter, safeZoneId);
        parts.where.add("m.startsAt >= :timeStart AND m.startsAt < :timeEnd");
        parts.params.put("timeStart", range.start());
        parts.params.put("timeEnd", range.end());
    }

    private static String whereClause(final QueryParts parts) {
        return parts.where.isEmpty() ? "" : " WHERE " + String.join(" AND ", parts.where);
    }

    private static String projectedSelect() {
        return "SELECT NEW ar.edu.itba.paw.persistence.MatchProjection("
                + "m.id, m.sport, m.host.id, m.address, m.latitude, m.longitude, "
                + "m.title, m.description, m.startsAt, m.endsAt, m.maxPlayers, "
                + "m.pricePerPlayer, m.visibility, m.joinPolicy, m.status, "
                + joinedPlayersExpression()
                + ", m.bannerImageId, m.series.id, m.seriesOccurrenceIndex, "
                + "m.deleted, m.deletedAt, m.deletedByUserId, m.deleteReason)"
                + " FROM Match m";
    }

    private static String joinedPlayersExpression() {
        return "(SELECT COUNT(mp.id) FROM MatchParticipant mp"
                + " WHERE mp.match = m AND mp.status IN :activeStatuses)";
    }

    private static String openSpotsExpression() {
        return "(m.maxPlayers - " + joinedPlayersExpression() + ")";
    }

    private static String orderBy(
            final MatchSort sort,
            final Boolean upcoming,
            final Double latitude,
            final Double longitude) {
        final MatchSort safeSort = sort == null ? MatchSort.SOONEST : sort;
        if (safeSort == MatchSort.DISTANCE && latitude != null && longitude != null) {
            return " ORDER BY CASE WHEN m.latitude IS NULL OR m.longitude IS NULL THEN 1 ELSE 0 END ASC,"
                    + " ((m.latitude - :latitude) * (m.latitude - :latitude))"
                    + " + ((m.longitude - :longitude) * :cosLatitude * (m.longitude - :longitude) * :cosLatitude) ASC,"
                    + " m.startsAt ASC, m.id ASC";
        }
        if (Boolean.FALSE.equals(upcoming)) {
            return switch (safeSort) {
                case PRICE_LOW ->
                        " ORDER BY COALESCE(m.pricePerPlayer, 0) ASC, m.startsAt DESC, m.id DESC";
                case SPOTS_DESC ->
                        " ORDER BY " + openSpotsExpression() + " DESC, m.startsAt DESC, m.id DESC";
                default -> " ORDER BY m.startsAt DESC, m.id DESC";
            };
        }
        return switch (safeSort) {
            case PRICE_LOW ->
                    " ORDER BY COALESCE(m.pricePerPlayer, 0) ASC, m.startsAt ASC, m.id ASC";
            case SPOTS_DESC ->
                    " ORDER BY " + openSpotsExpression() + " DESC, m.startsAt ASC, m.id ASC";
            default -> " ORDER BY m.startsAt ASC, m.id ASC";
        };
    }

    private static void setCommonParams(final Query query) {
        setParameterIfPresent(query, "activeStatuses", ACTIVE_PARTICIPANT_STATUSES);
    }

    private static void setParams(final Query query, final Map<String, Object> params) {
        params.forEach(query::setParameter);
    }

    private static void setDistanceParams(
            final Query query, final Double latitude, final Double longitude) {
        if (latitude == null || longitude == null) {
            return;
        }
        setParameterIfPresent(query, "latitude", latitude);
        setParameterIfPresent(query, "longitude", longitude);
        setParameterIfPresent(query, "cosLatitude", Math.cos(Math.toRadians(latitude)));
    }

    private static void setParameterIfPresent(
            final Query query, final String name, final Object value) {
        final boolean present =
                query.getParameters().stream()
                        .anyMatch(parameter -> name.equals(parameter.getName()));
        if (present) {
            query.setParameter(name, value);
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
        if (timeFilter == EventTimeFilter.TODAY) {
            return new TimeRange(
                    now.toInstant(), today.plusDays(1).atStartOfDay(zoneId).toInstant());
        }
        return new TimeRange(now.toInstant(), today.plusDays(1).atStartOfDay(zoneId).toInstant());
    }

    private static TimeRange buildPastTimeRange(
            final EventTimeFilter timeFilter, final ZoneId zoneId) {
        final ZonedDateTime now = ZonedDateTime.now(zoneId);
        if (timeFilter == EventTimeFilter.TODAY) {
            return new TimeRange(
                    now.toLocalDate().atStartOfDay(zoneId).toInstant(), now.toInstant());
        }
        return new TimeRange(now.minusDays(7).toInstant(), now.toInstant());
    }

    private static final class QueryParts {
        private final List<String> where = new ArrayList<>();
        private final Map<String, Object> params = new HashMap<>();
    }

    private record TimeRange(Instant start, Instant end) {}
}
