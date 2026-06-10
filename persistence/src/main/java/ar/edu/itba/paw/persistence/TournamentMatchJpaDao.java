package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.query.InvolvementScope;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import java.time.Instant;
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
public class TournamentMatchJpaDao implements TournamentMatchDao {

    @PersistenceContext private EntityManager em;

    private static final String USER_JOIN =
            " LEFT JOIN tm.teamA ta LEFT JOIN ta.members tam"
                    + " LEFT JOIN tm.teamB tb LEFT JOIN tb.members tbm";

    private static final String USER_CONDITION =
            "(tam.user.id = :userId OR tbm.user.id = :userId OR t.host.id = :userId)";

    @Override
    public TournamentMatch create(
            final Tournament tournament,
            final int roundNumber,
            final int matchIndex,
            final TournamentTeam teamA,
            final TournamentTeam teamB,
            final TournamentMatchStatus status,
            final TournamentMatch parentMatchA,
            final TournamentMatch parentMatchB) {
        final Instant now = Instant.now();
        final TournamentMatch match =
                new TournamentMatch(
                        null,
                        tournament,
                        roundNumber,
                        matchIndex,
                        teamA,
                        teamB,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        status,
                        parentMatchA,
                        parentMatchB,
                        now,
                        now);
        em.persist(match);
        return match;
    }

    @Override
    public List<TournamentMatch> findByTournament(final long tournamentId) {
        return em.createQuery(
                        "FROM TournamentMatch tm"
                                + " WHERE tm.tournament.id = :tournamentId"
                                + " ORDER BY tm.roundNumber ASC, tm.matchIndex ASC",
                        TournamentMatch.class)
                .setParameter("tournamentId", tournamentId)
                .getResultList();
    }

    @Override
    public Optional<TournamentMatch> findById(final long matchId) {
        return Optional.ofNullable(em.find(TournamentMatch.class, matchId));
    }

    @Override
    public Optional<TournamentMatch> findByTournamentAndId(
            final long tournamentId, final long matchId) {
        return em.createQuery(
                        "FROM TournamentMatch tm"
                                + " WHERE tm.tournament.id = :tournamentId"
                                + " AND tm.id = :matchId",
                        TournamentMatch.class)
                .setParameter("tournamentId", tournamentId)
                .setParameter("matchId", matchId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<TournamentMatch> findByTournamentAndRound(
            final long tournamentId, final int roundNumber) {
        return em.createQuery(
                        "FROM TournamentMatch tm"
                                + " WHERE tm.tournament.id = :tournamentId"
                                + " AND tm.roundNumber = :roundNumber"
                                + " ORDER BY tm.matchIndex ASC",
                        TournamentMatch.class)
                .setParameter("tournamentId", tournamentId)
                .setParameter("roundNumber", (short) roundNumber)
                .getResultList();
    }

    @Override
    public TournamentMatch update(final TournamentMatch match) {
        match.setUpdatedAt(Instant.now());
        return em.merge(match);
    }

    @Override
    public List<TournamentMatch> findByUserParticipant(
            final User user,
            final Boolean upcoming,
            final String query,
            final List<Sport> sports,
            final List<TournamentMatchStatus> statuses,
            final InvolvementScope involvement,
            final EventSort sort,
            final int offset,
            final int limit) {
        final QueryParts parts =
                buildQueryParts(user, upcoming, query, sports, statuses, involvement);
        final String where = whereClause(parts);
        final TypedQuery<Long> idQuery =
                em.createQuery(
                                "SELECT tm.id FROM TournamentMatch tm JOIN tm.tournament t"
                                        + USER_JOIN
                                        + where
                                        + " GROUP BY tm.id, tm.scheduledStartsAt"
                                        + orderBy(sort, upcoming),
                                Long.class)
                        .setFirstResult(offset)
                        .setMaxResults(limit);
        setParams(idQuery, parts.params);

        final List<Long> ids = idQuery.getResultList();
        if (ids.isEmpty()) {
            return List.of();
        }

        final TypedQuery<TournamentMatch> matchesQuery =
                em.createQuery(
                        "FROM TournamentMatch tm"
                                + " LEFT JOIN FETCH tm.tournament"
                                + " LEFT JOIN FETCH tm.teamA"
                                + " LEFT JOIN FETCH tm.teamB"
                                + " WHERE tm.id IN :ids",
                        TournamentMatch.class);
        matchesQuery.setParameter("ids", ids);

        final Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            order.put(ids.get(i), i);
        }

        return matchesQuery.getResultList().stream()
                .sorted(Comparator.comparingInt(m -> order.get(m.getId())))
                .toList();
    }

    @Override
    public int countByUserParticipant(
            final User user,
            final Boolean upcoming,
            final String query,
            final List<Sport> sports,
            final List<TournamentMatchStatus> statuses,
            final InvolvementScope involvement) {
        final QueryParts parts =
                buildQueryParts(user, upcoming, query, sports, statuses, involvement);
        final TypedQuery<Long> countQuery =
                em.createQuery(
                        "SELECT COUNT(DISTINCT tm.id) FROM TournamentMatch tm JOIN tm.tournament t"
                                + USER_JOIN
                                + whereClause(parts),
                        Long.class);
        setParams(countQuery, parts.params);
        return countQuery.getSingleResult().intValue();
    }

    private static QueryParts buildQueryParts(
            final User user,
            final Boolean upcoming,
            final String query,
            final List<Sport> sports,
            final List<TournamentMatchStatus> statuses,
            final InvolvementScope involvement) {
        final QueryParts parts = new QueryParts();
        if (involvement == InvolvementScope.HOST) {
            parts.where.add("t.host.id = :userId");
        } else if (involvement == InvolvementScope.PARTICIPANT) {
            parts.where.add("(tam.user.id = :userId OR tbm.user.id = :userId)");
        } else {
            parts.where.add(USER_CONDITION);
        }
        parts.params.put("userId", user.getId());

        parts.where.add("tm.scheduledStartsAt IS NOT NULL");

        if (Boolean.TRUE.equals(upcoming)) {
            parts.where.add("tm.scheduledStartsAt >= CURRENT_TIMESTAMP");
        } else if (Boolean.FALSE.equals(upcoming)) {
            parts.where.add("tm.scheduledStartsAt < CURRENT_TIMESTAMP");
        }

        appendSearchFilter(parts, query);
        appendSportFilter(parts, sports);
        appendStatusFilter(parts, statuses, upcoming);

        return parts;
    }

    private static void appendSearchFilter(final QueryParts parts, final String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        parts.where.add(
                "(LOWER(t.title) LIKE :tmQuery ESCAPE '\\'"
                        + " OR LOWER(COALESCE(tm.address, '')) LIKE :tmQuery ESCAPE '\\'"
                        + " OR LOWER(COALESCE(ta.name, '')) LIKE :tmQuery ESCAPE '\\'"
                        + " OR LOWER(COALESCE(tb.name, '')) LIKE :tmQuery ESCAPE '\\')");
        final String normalizedQuery = "%" + escapeLikePattern(query.trim().toLowerCase()) + "%";
        parts.params.put("tmQuery", normalizedQuery);
    }

    private static void appendSportFilter(final QueryParts parts, final List<Sport> sports) {
        if (sports == null || sports.isEmpty()) {
            return;
        }
        parts.where.add("t.sport IN :sports");
        parts.params.put("sports", sports);
    }

    private static void appendStatusFilter(
            final QueryParts parts,
            final List<TournamentMatchStatus> statuses,
            final Boolean upcoming) {
        if (statuses == null || statuses.isEmpty()) {
            return;
        }

        if (Boolean.FALSE.equals(upcoming)
                && statuses.contains(TournamentMatchStatus.AWAITING_RESULT)) {
            final List<TournamentMatchStatus> otherStatuses = new ArrayList<>(statuses);
            otherStatuses.remove(TournamentMatchStatus.AWAITING_RESULT);

            final StringBuilder sb = new StringBuilder();
            sb.append("(");
            if (!otherStatuses.isEmpty()) {
                sb.append("tm.status IN :tmOtherStatuses OR ");
            }
            sb.append(
                    "(tm.status = :scheduledAwaitingStatus AND tm.scheduledStartsAt < CURRENT_TIMESTAMP)");
            sb.append(")");
            parts.where.add(sb.toString());

            if (!otherStatuses.isEmpty()) {
                parts.params.put("tmOtherStatuses", otherStatuses);
            }
            parts.params.put("scheduledAwaitingStatus", TournamentMatchStatus.SCHEDULED);
            return;
        }

        parts.where.add("tm.status IN :tmStatuses");
        parts.params.put("tmStatuses", statuses);
    }

    private static String escapeLikePattern(final String query) {
        return query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String orderBy(final EventSort sort, final Boolean upcoming) {
        final boolean past = Boolean.FALSE.equals(upcoming);
        if (past) {
            return " ORDER BY tm.scheduledStartsAt DESC, tm.id DESC";
        }
        return " ORDER BY tm.scheduledStartsAt ASC, tm.id ASC";
    }

    private static String whereClause(final QueryParts parts) {
        return parts.where.isEmpty() ? "" : " WHERE " + String.join(" AND ", parts.where);
    }

    private static void setParams(final Query query, final Map<String, Object> params) {
        params.forEach(query::setParameter);
    }

    private static final class QueryParts {
        private final List<String> where = new ArrayList<>();
        private final Map<String, Object> params = new HashMap<>();
    }
}
