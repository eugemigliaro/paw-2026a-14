package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserSportRating;
import ar.edu.itba.paw.models.types.Sport;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class UserSportRatingJpaDao implements UserSportRatingDao {

    @PersistenceContext private EntityManager em;

    @Override
    public Optional<UserSportRating> findByUserAndSport(final User user, final Sport sport) {
        if (user == null || sport == null || sport == Sport.OTHER) {
            return Optional.empty();
        }

        return em
                .createQuery(
                        "FROM UserSportRating usr"
                                + " WHERE usr.user.id = :userId"
                                + " AND usr.sport = :sport",
                        UserSportRating.class)
                .setParameter("userId", user.getId())
                .setParameter("sport", sport)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public UserSportRatingLookupResult getOrCreate(
            final User user, final Sport sport, final int initialElo) {
        assertPersistedUser(user);
        assertRatedSport(sport);
        lockUser(user.getId());

        final Optional<UserSportRating> existing =
                em
                        .createQuery(
                                "FROM UserSportRating usr"
                                        + " WHERE usr.user.id = :userId"
                                        + " AND usr.sport = :sport",
                                UserSportRating.class)
                        .setParameter("userId", user.getId())
                        .setParameter("sport", sport)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .getResultList()
                        .stream()
                        .findFirst();

        if (existing.isPresent()) {
            return new UserSportRatingLookupResult(existing.get(), false);
        }

        final Instant now = Instant.now();
        final UserSportRating rating = new UserSportRating(null, user, sport, initialElo, now, now);
        em.persist(rating);
        return new UserSportRatingLookupResult(rating, true);
    }

    @Override
    public UserSportRating save(final UserSportRating rating) {
        assertRatedSport(rating.getSport());
        rating.setUpdatedAt(Instant.now());
        return em.merge(rating);
    }

    @Override
    public List<UserSportRating> findByUser(final User user) {
        if (user == null) {
            return List.of();
        }

        return em.createQuery(
                        "FROM UserSportRating usr"
                                + " WHERE usr.user.id = :userId"
                                + " ORDER BY usr.sport ASC",
                        UserSportRating.class)
                .setParameter("userId", user.getId())
                .getResultList();
    }

    @Override
    public List<UserSportRating> findTopBySport(final Sport sport, final int limit) {
        if (sport == null || sport == Sport.OTHER || limit <= 0) {
            return List.of();
        }

        return em.createQuery(
                        "FROM UserSportRating usr"
                                + " WHERE usr.sport = :sport"
                                + " ORDER BY usr.elo DESC, usr.updatedAt ASC, usr.user.id ASC",
                        UserSportRating.class)
                .setParameter("sport", sport)
                .setMaxResults(limit)
                .getResultList();
    }

    private void assertRatedSport(final Sport sport) {
        if (sport == null || sport == Sport.OTHER) {
            throw new IllegalArgumentException("Sport is not rated: " + sport);
        }
    }

    private void assertPersistedUser(final User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be persisted");
        }
    }

    private void lockUser(final Long userId) {
        em.createQuery("SELECT u.id FROM UserAccount u WHERE u.id = :userId", Long.class)
                .setParameter("userId", userId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }
}
