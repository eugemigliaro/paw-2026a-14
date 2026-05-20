package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class TournamentSoloEntryJpaDao implements TournamentSoloEntryDao {

    @PersistenceContext private EntityManager em;

    @Override
    public TournamentSoloEntry create(
            final Tournament tournament, final User user, final TournamentSoloEntryStatus status) {
        final TournamentSoloEntry soloEntry =
                new TournamentSoloEntry(null, tournament, user, status, null, Instant.now(), null);
        em.persist(soloEntry);
        return soloEntry;
    }

    @Override
    public Optional<TournamentSoloEntry> findByTournamentAndUser(
            final long tournamentId, final long userId) {
        return em.createQuery(
                        "FROM TournamentSoloEntry tse"
                                + " WHERE tse.tournament.id = :tournamentId"
                                + " AND tse.user.id = :userId",
                        TournamentSoloEntry.class)
                .setParameter("tournamentId", tournamentId)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<TournamentSoloEntry> findActiveByTournament(final long tournamentId) {
        return em.createQuery(
                        "FROM TournamentSoloEntry tse"
                                + " WHERE tse.tournament.id = :tournamentId"
                                + " AND tse.status = :status"
                                + " ORDER BY tse.joinedAt ASC, tse.id ASC",
                        TournamentSoloEntry.class)
                .setParameter("tournamentId", tournamentId)
                .setParameter("status", TournamentSoloEntryStatus.IN_POOL)
                .getResultList();
    }

    @Override
    public long countActiveByTournament(final long tournamentId) {
        return em.createQuery(
                        "SELECT COUNT(tse) FROM TournamentSoloEntry tse"
                                + " WHERE tse.tournament.id = :tournamentId"
                                + " AND tse.status = :status",
                        Long.class)
                .setParameter("tournamentId", tournamentId)
                .setParameter("status", TournamentSoloEntryStatus.IN_POOL)
                .getSingleResult();
    }

    @Override
    public TournamentSoloEntry update(final TournamentSoloEntry soloEntry) {
        return em.merge(soloEntry);
    }
}
