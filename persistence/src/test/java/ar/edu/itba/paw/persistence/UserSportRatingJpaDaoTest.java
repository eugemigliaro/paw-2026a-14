package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserSportRating;
import ar.edu.itba.paw.models.types.Sport;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class UserSportRatingJpaDaoTest {

    @Autowired private UserDao userDao;
    @Autowired private UserSportRatingDao userSportRatingDao;

    @PersistenceContext private EntityManager em;

    @Test
    public void testMigrationDoesNotSeedExistingUsers() {
        final User user = createUser("unrated", "unrated@test.com");

        final List<UserSportRating> ratings = userSportRatingDao.findByUser(user);

        Assertions.assertTrue(ratings.isEmpty());
    }

    @Test
    public void testFindByUserAndSportReturnsEmptyForUnratedUser() {
        final User user = createUser("unrated_football", "unrated_football@test.com");

        final Optional<UserSportRating> rating =
                userSportRatingDao.findByUserAndSport(user, Sport.FOOTBALL);

        Assertions.assertTrue(rating.isEmpty());
    }

    @Test
    public void testGetOrCreateCreatesRatingWithProvidedInitialElo() {
        final User user = createUser("rated", "rated@test.com");

        final UserSportRatingLookupResult result =
                userSportRatingDao.getOrCreate(user, Sport.FOOTBALL, 1000);
        final UserSportRating rating = result.getRating();

        Assertions.assertTrue(result.isCreated());
        Assertions.assertNotNull(rating.getId());
        Assertions.assertEquals(user.getId(), rating.getUser().getId());
        Assertions.assertEquals(Sport.FOOTBALL, rating.getSport());
        Assertions.assertEquals(1000, rating.getElo());
        Assertions.assertNotNull(rating.getCreatedAt());
        Assertions.assertNotNull(rating.getUpdatedAt());
    }

    @Test
    public void testGetOrCreateReturnsExistingRating() {
        final User user = createUser("existing_rating", "existing_rating@test.com");
        final UserSportRating initial =
                userSportRatingDao.getOrCreate(user, Sport.TENNIS, 1000).getRating();

        final UserSportRatingLookupResult result =
                userSportRatingDao.getOrCreate(user, Sport.TENNIS, 1500);
        final UserSportRating existing = result.getRating();

        Assertions.assertFalse(result.isCreated());
        Assertions.assertEquals(initial.getId(), existing.getId());
        Assertions.assertEquals(1L, countRatings());
    }

    @Test
    public void testGetOrCreateRejectsNullUser() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> userSportRatingDao.getOrCreate(null, Sport.FOOTBALL, 1000));
    }

    @Test
    public void testGetOrCreateRejectsUnpersistedUser() {
        final User user =
                new User(null, "unpersisted@test.com", "unpersisted", null, null, null, null, "en");

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> userSportRatingDao.getOrCreate(user, Sport.FOOTBALL, 1000));
    }

    @Test
    public void testGetOrCreateRejectsOtherSport() {
        final User user = createUser("other_rating", "other_rating@test.com");

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> userSportRatingDao.getOrCreate(user, Sport.OTHER, 1000));
    }

    @Test
    public void testFindByUserAndSportReturnsEmptyForOtherSport() {
        final User user = createUser("find_other", "find_other@test.com");

        final Optional<UserSportRating> rating =
                userSportRatingDao.findByUserAndSport(user, Sport.OTHER);

        Assertions.assertTrue(rating.isEmpty());
    }

    @Test
    public void testSavePersistsEloChange() {
        final User user = createUser("save_rating", "save_rating@test.com");
        final UserSportRating rating =
                userSportRatingDao.getOrCreate(user, Sport.PADEL, 1000).getRating();
        rating.setElo(1175);

        userSportRatingDao.save(rating);
        flushAndClear();
        final UserSportRating persisted =
                userSportRatingDao
                        .findByUserAndSport(user, Sport.PADEL)
                        .orElseThrow(AssertionError::new);

        Assertions.assertEquals(1175, persisted.getElo());
    }

    @Test
    public void testFindByUserReturnsEstablishedRatingsOnly() {
        final User user = createUser("profile_ratings", "profile_ratings@test.com");
        userSportRatingDao.getOrCreate(user, Sport.FOOTBALL, 1000);
        userSportRatingDao.getOrCreate(user, Sport.BASKETBALL, 1000);

        final List<UserSportRating> ratings = userSportRatingDao.findByUser(user);

        Assertions.assertEquals(2, ratings.size());
        Assertions.assertTrue(
                ratings.stream().anyMatch(rating -> rating.getSport() == Sport.FOOTBALL));
        Assertions.assertTrue(
                ratings.stream().anyMatch(rating -> rating.getSport() == Sport.BASKETBALL));
        Assertions.assertFalse(
                ratings.stream().anyMatch(rating -> rating.getSport() == Sport.OTHER));
    }

    @Test
    public void testFindTopBySportOrdersByHighestEloAndRespectsLimit() {
        final User user1 = createUser("leader1", "leader1@test.com");
        final User user2 = createUser("leader2", "leader2@test.com");
        final User user3 = createUser("leader3", "leader3@test.com");
        setElo(user1, Sport.FOOTBALL, 1100);
        setElo(user2, Sport.FOOTBALL, 1300);
        setElo(user3, Sport.FOOTBALL, 1200);

        final List<UserSportRating> leaders = userSportRatingDao.findTopBySport(Sport.FOOTBALL, 2);

        Assertions.assertEquals(2, leaders.size());
        Assertions.assertEquals(user2.getId(), leaders.get(0).getUser().getId());
        Assertions.assertEquals(user3.getId(), leaders.get(1).getUser().getId());
    }

    @Test
    public void testFindTopBySportReturnsEmptyForOtherSport() {
        final User user = createUser("other_leader", "other_leader@test.com");
        userSportRatingDao.getOrCreate(user, Sport.FOOTBALL, 1000);

        final List<UserSportRating> leaders = userSportRatingDao.findTopBySport(Sport.OTHER, 10);

        Assertions.assertTrue(leaders.isEmpty());
    }

    private User createUser(final String username, final String email) {
        return userDao.createUser(email, username);
    }

    private void setElo(final User user, final Sport sport, final int elo) {
        final UserSportRating rating =
                userSportRatingDao.getOrCreate(user, sport, 1000).getRating();
        rating.setElo(elo);
        userSportRatingDao.save(rating);
    }

    private Long countRatings() {
        return em.createQuery("SELECT COUNT(usr) FROM UserSportRating usr", Long.class)
                .getSingleResult();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
