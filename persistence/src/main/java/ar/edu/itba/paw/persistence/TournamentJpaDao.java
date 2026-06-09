package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class TournamentJpaDao implements TournamentDao {

    private static final List<TournamentStatus> PUBLIC_STATUSES =
            List.of(
                    TournamentStatus.REGISTRATION,
                    TournamentStatus.BRACKET_SETUP,
                    TournamentStatus.IN_PROGRESS,
                    TournamentStatus.COMPLETED,
                    TournamentStatus.CANCELLED);

    private static final List<TournamentStatus> PUBLIC_ACTIVE_STATUSES =
            List.of(
                    TournamentStatus.REGISTRATION,
                    TournamentStatus.BRACKET_SETUP,
                    TournamentStatus.IN_PROGRESS);

    @PersistenceContext private EntityManager em;

    @Override
    public Tournament create(
            final User host,
            final Sport sport,
            final String title,
            final String description,
            final String address,
            final Double latitude,
            final Double longitude,
            final Instant startsAt,
            final Instant endsAt,
            final BigDecimal pricePerPlayer,
            final ImageMetadata bannerImageMetadata,
            final TournamentFormat format,
            final int bracketSize,
            final int teamSize,
            final boolean allowSoloSignup,
            final boolean allowTeamDraft,
            final Instant registrationOpensAt,
            final Instant registrationClosesAt,
            final TournamentStatus status) {
        final Instant now = Instant.now();
        final Tournament tournament =
                new Tournament(
                        null,
                        host,
                        sport,
                        title,
                        description,
                        address,
                        latitude,
                        longitude,
                        startsAt,
                        endsAt,
                        pricePerPlayer,
                        bannerImageMetadata,
                        format,
                        bracketSize,
                        teamSize,
                        allowSoloSignup,
                        allowTeamDraft,
                        registrationOpensAt,
                        registrationClosesAt,
                        status,
                        now,
                        now);

        em.persist(tournament);
        return tournament;
    }

    @Override
    public Optional<Tournament> findById(final long tournamentId) {
        return Optional.ofNullable(em.find(Tournament.class, tournamentId));
    }

    @Override
    public Optional<Tournament> findPublicById(final long tournamentId) {
        return em.createQuery(
                        "FROM Tournament t"
                                + " WHERE t.id = :tournamentId"
                                + " AND t.deleted = FALSE"
                                + " AND t.status IN :statuses",
                        Tournament.class)
                .setParameter("tournamentId", tournamentId)
                .setParameter("statuses", PUBLIC_STATUSES)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Tournament> findPublicRegistrationOrLive(final int offset, final int limit) {
        return em.createQuery(
                        "FROM Tournament t"
                                + " WHERE t.deleted = FALSE"
                                + " AND t.status IN :statuses"
                                + " ORDER BY t.registrationClosesAt ASC, t.id ASC",
                        Tournament.class)
                .setParameter("statuses", PUBLIC_ACTIVE_STATUSES)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public List<Tournament> findPublicTournaments(
            final String query,
            final List<Sport> sports,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final EventSort sort,
            final Double latitude,
            final Double longitude,
            final int offset,
            final int limit) {
        final QueryParts parts = publicSearchParts();
        appendFilters(parts, query, sports, startsAtFrom, startsAtTo, minPrice, maxPrice);
        return findPage(parts, sort, latitude, longitude, offset, limit);
    }

    @Override
    public int countPublicTournaments(
            final String query,
            final List<Sport> sports,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        final QueryParts parts = publicSearchParts();
        appendFilters(parts, query, sports, startsAtFrom, startsAtTo, minPrice, maxPrice);
        return countTournaments(parts);
    }

    @Override
    public List<Tournament> findDashboardTournaments(
            final User user,
            final Boolean upcoming,
            final Boolean includeHosted,
            final String query,
            final List<Sport> sports,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final EventSort sort,
            final Double latitude,
            final Double longitude,
            final int offset,
            final int limit) {
        final QueryParts parts = dashboardSearchParts(user, upcoming, includeHosted);
        appendFilters(parts, query, sports, startsAtFrom, startsAtTo, minPrice, maxPrice);
        return findPage(parts, sort, latitude, longitude, offset, limit);
    }

    @Override
    public int countDashboardTournaments(
            final User host,
            final Boolean upcoming,
            final Boolean includeHosted,
            final String query,
            final List<Sport> sports,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        final QueryParts parts = dashboardSearchParts(host, upcoming, includeHosted);
        appendFilters(parts, query, sports, startsAtFrom, startsAtTo, minPrice, maxPrice);
        return countTournaments(parts);
    }

    @Override
    public Set<Long> findParticipatingTournamentIds(
            final User user, final List<Long> tournamentIds) {
        if (user == null
                || user.getId() == null
                || tournamentIds == null
                || tournamentIds.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(
                em.createQuery(
                                "SELECT DISTINCT t.id FROM Tournament t"
                                        + " WHERE t.id IN :tournamentIds"
                                        + " AND t.deleted = FALSE"
                                        + " AND (EXISTS ("
                                        + " SELECT tse.id FROM TournamentSoloEntry tse"
                                        + " WHERE tse.tournament = t"
                                        + " AND tse.user = :user"
                                        + " AND tse.status IN :activeSoloStatuses"
                                        + " ) OR EXISTS ("
                                        + " SELECT ttm.id FROM TournamentTeamMember ttm"
                                        + " WHERE ttm.team.tournament = t"
                                        + " AND ttm.user = :user"
                                        + " ))",
                                Long.class)
                        .setParameter("tournamentIds", tournamentIds)
                        .setParameter("user", user)
                        .setParameter(
                                "activeSoloStatuses",
                                List.of(
                                        TournamentSoloEntryStatus.IN_POOL,
                                        TournamentSoloEntryStatus.ASSIGNED))
                        .getResultList());
    }

    @Override
    public Optional<Tournament> refreshScheduleWindow(final long tournamentId) {
        final Tournament tournament = em.find(Tournament.class, tournamentId);
        if (tournament == null) {
            return Optional.empty();
        }

        final Object[] scheduleWindow =
                em.createQuery(
                                "SELECT MIN(tm.scheduledStartsAt),"
                                        + " MAX(COALESCE(tm.scheduledEndsAt, tm.scheduledStartsAt))"
                                        + " FROM TournamentMatch tm"
                                        + " WHERE tm.tournament.id = :tournamentId"
                                        + " AND tm.scheduledStartsAt IS NOT NULL",
                                Object[].class)
                        .setParameter("tournamentId", tournamentId)
                        .getSingleResult();

        tournament.setStartsAt((Instant) scheduleWindow[0]);
        tournament.setEndsAt((Instant) scheduleWindow[1]);
        tournament.setUpdatedAt(Instant.now());
        return Optional.of(tournament);
    }

    @Override
    public Tournament update(final Tournament tournament) {
        tournament.setUpdatedAt(Instant.now());
        return em.merge(tournament);
    }

    private List<Tournament> findPage(
            final QueryParts parts,
            final EventSort sort,
            final Double latitude,
            final Double longitude,
            final int offset,
            final int limit) {
        final String where = whereClause(parts);
        final TypedQuery<Long> idQuery =
                em.createQuery(
                                "SELECT t.id FROM Tournament t JOIN t.host hu"
                                        + where
                                        + orderBy(sort, latitude, longitude),
                                Long.class)
                        .setFirstResult(Math.max(0, offset))
                        .setMaxResults(limit);
        setParams(idQuery, parts.params);
        if (sort == EventSort.DISTANCE) {
            setDistanceParams(idQuery, latitude, longitude);
        }

        final List<Long> ids = idQuery.getResultList();
        if (ids.isEmpty()) {
            return List.of();
        }

        final TypedQuery<Tournament> tournamentsQuery =
                em.createQuery("FROM Tournament t WHERE t.id IN :ids", Tournament.class)
                        .setParameter("ids", ids);
        final Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            order.put(ids.get(i), i);
        }

        return tournamentsQuery.getResultList().stream()
                .sorted(Comparator.comparingInt(tournament -> order.get(tournament.getId())))
                .toList();
    }

    private int countTournaments(final QueryParts parts) {
        final TypedQuery<Long> countQuery =
                em.createQuery(
                        "SELECT COUNT(t.id) FROM Tournament t JOIN t.host hu" + whereClause(parts),
                        Long.class);
        setParams(countQuery, parts.params);
        return countQuery.getSingleResult().intValue();
    }

    private static QueryParts publicSearchParts() {
        final QueryParts parts = new QueryParts();
        parts.where.add("t.deleted = FALSE");
        parts.where.add("t.status IN :statuses");
        parts.params.put("statuses", PUBLIC_ACTIVE_STATUSES);
        return parts;
    }

    private static QueryParts dashboardSearchParts(
            final User user, final Boolean upcoming, final Boolean includeHosted) {
        final QueryParts parts = new QueryParts();
        parts.where.add("t.deleted = FALSE");
        if (includeHosted != null && includeHosted) {
            parts.where.add("t.host = :user");
            parts.params.put("user", user);
        }
        if (upcoming != null) {
            if (upcoming) {
                parts.where.add("(t.endsAt IS NULL OR t.endsAt >= :now)");
            } else {
                parts.where.add("t.endsAt < :now");
            }
            parts.params.put("now", Instant.now());
        }
        return parts;
    }

    private static void appendFilters(
            final QueryParts parts,
            final String query,
            final List<Sport> sports,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        appendSearchFilter(parts, query);
        if (sports != null && !sports.isEmpty()) {
            parts.where.add("t.sport IN :sports");
            parts.params.put("sports", sports);
        }
        if (startsAtFrom != null) {
            parts.where.add("t.startsAt >= :startsAtFrom");
            parts.params.put("startsAtFrom", startsAtFrom);
        }
        if (startsAtTo != null) {
            parts.where.add("t.startsAt < :startsAtTo");
            parts.params.put("startsAtTo", startsAtTo);
        }
        if (minPrice != null) {
            parts.where.add("t.pricePerPlayer >= :minPrice");
            parts.params.put("minPrice", minPrice);
        }
        if (maxPrice != null) {
            parts.where.add("t.pricePerPlayer <= :maxPrice");
            parts.params.put("maxPrice", maxPrice);
        }
    }

    private static void appendSearchFilter(final QueryParts parts, final String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        parts.where.add(
                "(LOWER(t.title) LIKE :query"
                        + " OR LOWER(COALESCE(t.description, '')) LIKE :query"
                        + " OR LOWER(COALESCE(t.address, '')) LIKE :query"
                        + " OR LOWER(COALESCE(hu.username, '')) LIKE :query)");
        parts.params.put("query", "%" + query.trim().toLowerCase() + "%");
    }

    private static String whereClause(final QueryParts parts) {
        return parts.where.isEmpty() ? "" : " WHERE " + String.join(" AND ", parts.where);
    }

    private static String orderBy(
            final EventSort sort, final Double latitude, final Double longitude) {
        final EventSort safeSort = sort == null ? EventSort.SOONEST : sort;
        if (safeSort == EventSort.DISTANCE && latitude != null && longitude != null) {
            return " ORDER BY CASE WHEN t.latitude IS NULL OR t.longitude IS NULL THEN 1 ELSE 0 END ASC,"
                    + " ((t.latitude - :latitude) * (t.latitude - :latitude))"
                    + " + ((t.longitude - :longitude) * :cosLatitude * (t.longitude - :longitude) * :cosLatitude) ASC,"
                    + " t.startsAt ASC, t.id ASC";
        }
        if (safeSort == EventSort.PRICE_LOW) {
            return " ORDER BY COALESCE(t.pricePerPlayer, 0) ASC, t.startsAt ASC, t.id ASC";
        }
        return " ORDER BY t.startsAt ASC, t.id ASC";
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

    private static final class QueryParts {
        private final List<String> where = new ArrayList<>();
        private final Map<String, Object> params = new HashMap<>();
    }
}
