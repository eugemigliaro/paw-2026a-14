package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class TournamentMatchJpaDao implements TournamentMatchDao {

    @PersistenceContext private EntityManager em;

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
}
