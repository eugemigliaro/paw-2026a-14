package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import java.time.Instant;
import java.util.List;
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
    public long countByTournament(final long tournamentId) {
        return em.createQuery(
                        "SELECT COUNT(tt) FROM TournamentTeam tt"
                                + " WHERE tt.tournament.id = :tournamentId",
                        Long.class)
                .setParameter("tournamentId", tournamentId)
                .getSingleResult();
    }
}
