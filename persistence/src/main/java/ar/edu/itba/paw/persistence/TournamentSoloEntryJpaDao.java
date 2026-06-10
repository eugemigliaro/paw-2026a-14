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
    public List<Tournament> findTournamentsByUser(final User user) {
        return em.createQuery(
                        "SELECT tse.tournament FROM TournamentSoloEntry tse"
                                + " WHERE tse.user.id = :userId"
                                + " AND tse.tournament.deleted = FALSE"
                                + " AND tse.status IN :statuses"
                                + " ORDER BY tse.tournament.startsAt ASC, tse.tournament.id ASC",
                        Tournament.class)
                .setParameter("userId", user.getId())
                .setParameter(
                        "statuses",
                        List.of(
                                TournamentSoloEntryStatus.IN_POOL,
                                TournamentSoloEntryStatus.ASSIGNED))
                .getResultList();
    }

    @Override
    public List<TournamentSoloEntry> findInPoolEntriesByUser(final User user) {
        return em.createQuery(
                        "FROM TournamentSoloEntry tse"
                                + " WHERE tse.user.id = :userId"
                                + " AND tse.status = :status"
                                + " AND tse.tournament.deleted = FALSE",
                        TournamentSoloEntry.class)
                .setParameter("userId", user.getId())
                .setParameter("status", TournamentSoloEntryStatus.IN_POOL)
                .getResultList();
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
    public List<TournamentSoloEntry> findByTournamentAndStatus(
            final long tournamentId, final TournamentSoloEntryStatus status) {
        return em.createQuery(
                        "SELECT tse FROM TournamentSoloEntry tse"
                                + " JOIN FETCH tse.user"
                                + " WHERE tse.tournament.id = :tournamentId"
                                + " AND tse.status = :status"
                                + " ORDER BY tse.joinedAt ASC, tse.id ASC",
                        TournamentSoloEntry.class)
                .setParameter("tournamentId", tournamentId)
                .setParameter("status", status)
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
