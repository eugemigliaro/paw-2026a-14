package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
    public List<Tournament> findHostedByUser(final User host, final int offset, final int limit) {
        return em.createQuery(
                        "FROM Tournament t"
                                + " WHERE t.host.id = :hostUserId"
                                + " AND t.deleted = FALSE"
                                + " ORDER BY t.createdAt DESC, t.id DESC",
                        Tournament.class)
                .setParameter("hostUserId", host.getId())
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public Tournament update(final Tournament tournament) {
        tournament.setUpdatedAt(Instant.now());
        return em.merge(tournament);
    }
}
