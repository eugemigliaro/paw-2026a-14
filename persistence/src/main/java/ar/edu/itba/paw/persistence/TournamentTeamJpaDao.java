package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class TournamentTeamJpaDao implements TournamentTeamDao {

    @PersistenceContext private EntityManager em;

    @Override
    public TournamentTeam create(
            final Tournament tournament,
            final String name,
            final TournamentTeamOrigin origin,
            final Integer seedPosition) {
        final TournamentTeam team =
                new TournamentTeam(null, tournament, name, origin, seedPosition, Instant.now());
        em.persist(team);
        return team;
    }

    @Override
    public TournamentTeamMember addMember(
            final TournamentTeam team, final User user, final boolean captain) {
        final TournamentTeamMember member =
                new TournamentTeamMember(null, team, user, captain, Instant.now());
        em.persist(member);
        return member;
    }

    @Override
    public void removeMember(final TournamentTeam team, final User user) {
        em.createQuery(
                        "DELETE FROM TournamentTeamMember ttm"
                                + " WHERE ttm.team.id = :teamId"
                                + " AND ttm.user.id = :userId")
                .setParameter("teamId", team.getId())
                .setParameter("userId", user.getId())
                .executeUpdate();
    }

    @Override
    public void delete(final TournamentTeam team) {
        em.createQuery("DELETE FROM TournamentTeamMember ttm WHERE ttm.team.id = :teamId")
                .setParameter("teamId", team.getId())
                .executeUpdate();
        em.remove(em.contains(team) ? team : em.merge(team));
    }

    @Override
    public long countMembers(final long teamId) {
        return em.createQuery(
                        "SELECT COUNT(ttm) FROM TournamentTeamMember ttm"
                                + " WHERE ttm.team.id = :teamId",
                        Long.class)
                .setParameter("teamId", teamId)
                .getSingleResult();
    }

    @Override
    public long countMembersByTournament(final long tournamentId) {
        return em.createQuery(
                        "SELECT COUNT(ttm) FROM TournamentTeamMember ttm"
                                + " WHERE ttm.team.tournament.id = :tournamentId",
                        Long.class)
                .setParameter("tournamentId", tournamentId)
                .getSingleResult();
    }

    @Override
    public List<TournamentTeam> findJoinableByTournament(
            final long tournamentId, final int teamSize) {
        return em.createQuery(
                        "SELECT tt FROM TournamentTeam tt"
                                + " LEFT JOIN tt.members m"
                                + " WHERE tt.tournament.id = :tournamentId"
                                + " GROUP BY tt"
                                + " HAVING COUNT(m) < :teamSize"
                                + " ORDER BY tt.createdAt ASC, tt.id ASC",
                        TournamentTeam.class)
                .setParameter("tournamentId", tournamentId)
                .setParameter("teamSize", (long) teamSize)
                .getResultList();
    }

    @Override
    public boolean existsByTournamentAndName(final long tournamentId, final String name) {
        return em.createQuery(
                                "SELECT COUNT(tt) FROM TournamentTeam tt"
                                        + " WHERE tt.tournament.id = :tournamentId"
                                        + " AND tt.name = :name",
                                Long.class)
                        .setParameter("tournamentId", tournamentId)
                        .setParameter("name", name)
                        .getSingleResult()
                > 0;
    }

    @Override
    public Optional<TournamentTeam> findById(final long teamId) {
        return Optional.ofNullable(em.find(TournamentTeam.class, teamId));
    }

    @Override
    public List<TournamentTeam> findByTournament(final long tournamentId) {
        return em.createQuery(
                        "FROM TournamentTeam tt"
                                + " WHERE tt.tournament.id = :tournamentId"
                                + " ORDER BY COALESCE(tt.seedPosition, 32767) ASC, tt.id ASC",
                        TournamentTeam.class)
                .setParameter("tournamentId", tournamentId)
                .getResultList();
    }

    @Override
    public List<TournamentTeam> findByTournaments(final java.util.Collection<Long> tournamentIds) {
        if (tournamentIds == null || tournamentIds.isEmpty()) {
            return List.of();
        }
        return em.createQuery(
                        "FROM TournamentTeam tt"
                                + " WHERE tt.tournament.id IN :tournamentIds"
                                + " ORDER BY tt.tournament.id ASC, COALESCE(tt.seedPosition, 32767) ASC, tt.id ASC",
                        TournamentTeam.class)
                .setParameter("tournamentIds", tournamentIds)
                .getResultList();
    }

    @Override
    public List<TournamentTeam> findByTournamentUnordered(final long tournamentId) {
        return em.createQuery(
                        "FROM TournamentTeam tt"
                                + " WHERE tt.tournament.id = :tournamentId"
                                + " ORDER BY tt.id ASC",
                        TournamentTeam.class)
                .setParameter("tournamentId", tournamentId)
                .getResultList();
    }

    @Override
    public List<TournamentTeam> findSeededByTournament(final long tournamentId) {
        return em.createQuery(
                        "FROM TournamentTeam tt"
                                + " WHERE tt.tournament.id = :tournamentId"
                                + " AND tt.seedPosition IS NOT NULL"
                                + " ORDER BY tt.seedPosition ASC, tt.id ASC",
                        TournamentTeam.class)
                .setParameter("tournamentId", tournamentId)
                .getResultList();
    }

    @Override
    public List<TournamentTeamMember> findMembersByTournament(final long tournamentId) {
        return em.createQuery(
                        "SELECT ttm FROM TournamentTeamMember ttm"
                                + " JOIN FETCH ttm.user"
                                + " JOIN FETCH ttm.team tt"
                                + " WHERE tt.tournament.id = :tournamentId"
                                + " ORDER BY tt.id ASC, ttm.id ASC",
                        TournamentTeamMember.class)
                .setParameter("tournamentId", tournamentId)
                .getResultList();
    }

    @Override
    public Optional<TournamentTeam> findUserTeam(final long tournamentId, final long userId) {
        return em.createQuery(
                        "SELECT tt FROM TournamentTeamMember ttm"
                                + " JOIN ttm.team tt"
                                + " WHERE tt.tournament.id = :tournamentId"
                                + " AND ttm.user.id = :userId",
                        TournamentTeam.class)
                .setParameter("tournamentId", tournamentId)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Tournament> findTournamentsByMember(final User user) {
        final List<Long> ids =
                em.createQuery(
                                "SELECT tt.tournament.id FROM TournamentTeamMember ttm"
                                        + " JOIN ttm.team tt"
                                        + " WHERE ttm.user.id = :userId"
                                        + " AND tt.tournament.deleted = FALSE"
                                        + " GROUP BY tt.tournament.id, tt.tournament.startsAt"
                                        + " ORDER BY tt.tournament.startsAt ASC, tt.tournament.id ASC",
                                Long.class)
                        .setParameter("userId", user.getId())
                        .getResultList();
        if (ids.isEmpty()) {
            return List.of();
        }

        final List<Tournament> tournaments =
                em.createQuery("FROM Tournament t WHERE t.id IN :ids", Tournament.class)
                        .setParameter("ids", ids)
                        .getResultList();
        final Map<Long, Tournament> byId = new HashMap<>();
        for (final Tournament tournament : tournaments) {
            byId.put(tournament.getId(), tournament);
        }

        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    @Override
    public void saveSeedOrder(final List<TournamentTeam> teams, final List<Long> orderedTeamIds) {
        final Map<Long, Integer> positionByTeamId = new HashMap<>();
        for (int index = 0; index < orderedTeamIds.size(); index++) {
            positionByTeamId.put(orderedTeamIds.get(index), index + 1);
        }

        final int temporaryOffset = orderedTeamIds.size();
        for (int index = 0; index < teams.size(); index++) {
            teams.get(index).setSeedPosition(temporaryOffset + index + 1);
        }
        em.flush();

        for (final TournamentTeam team : teams) {
            team.setSeedPosition(positionByTeamId.get(team.getId()));
        }
    }

    @Override
    public long countByTournament(final long tournamentId) {
        return em.createQuery(
                        "SELECT COUNT(tt) FROM TournamentTeam tt"
                                + " WHERE tt.tournament.id = :tournamentId",
                        Long.class)
                .setParameter("tournamentId", tournamentId)
                .getSingleResult();
    }
}
