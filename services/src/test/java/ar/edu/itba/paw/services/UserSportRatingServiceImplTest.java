package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EloUpdatedResult;
import ar.edu.itba.paw.models.PlayerEloChange;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserSportRating;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.persistence.UserSportRatingDao;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserSportRatingServiceImplTest {

    @Mock private UserSportRatingDao userSportRatingDao;

    @InjectMocks private UserSportRatingServiceImpl userSportRatingService;

    @Test
    public void testFindRatingReturnsExistingRating() {
        final User user = createUser(10L);
        final UserSportRating rating =
                new UserSportRating(
                        20L,
                        user,
                        Sport.FOOTBALL,
                        1120,
                        Instant.parse("2026-05-20T12:00:00Z"),
                        Instant.parse("2026-05-20T12:00:00Z"));
        Mockito.when(userSportRatingDao.findByUserAndSport(user, Sport.FOOTBALL))
                .thenReturn(Optional.of(rating));

        final Optional<UserSportRating> result =
                userSportRatingService.findRating(user, Sport.FOOTBALL);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1120, result.get().getElo());
        Assertions.assertEquals(Sport.FOOTBALL, result.get().getSport());
    }

    @Test
    public void testFindRatingReturnsEmptyForUnratedUser() {
        final User user = createUser(11L);
        Mockito.when(userSportRatingDao.findByUserAndSport(user, Sport.TENNIS))
                .thenReturn(Optional.empty());

        final Optional<UserSportRating> result =
                userSportRatingService.findRating(user, Sport.TENNIS);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetEffectiveEloReturnsExistingRatingElo() {
        final User user = createUser(12L);
        final UserSportRating rating = createRating(21L, user, Sport.BASKETBALL, 1234);
        Mockito.when(userSportRatingDao.findByUserAndSport(user, Sport.BASKETBALL))
                .thenReturn(Optional.of(rating));

        final int elo = userSportRatingService.getEffectiveElo(user, Sport.BASKETBALL);

        Assertions.assertEquals(1234, elo);
    }

    @Test
    public void testGetEffectiveEloReturnsInitialEloForUnratedUser() {
        final User user = createUser(13L);
        Mockito.when(userSportRatingDao.findByUserAndSport(user, Sport.PADEL))
                .thenReturn(Optional.empty());

        final int elo = userSportRatingService.getEffectiveElo(user, Sport.PADEL);

        Assertions.assertEquals(1000, elo);
    }

    @Test
    public void testGetEffectiveEloRejectsUnratedSport() {
        final User user = createUser(14L);

        final IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> userSportRatingService.getEffectiveElo(user, Sport.OTHER));

        Assertions.assertEquals("Sport is not rated: other", exception.getMessage());
    }

    @Test
    public void testFindRatingsForUserReturnsUserRatings() {
        final User user = createUser(15L);
        final UserSportRating footballRating = createRating(22L, user, Sport.FOOTBALL, 1120);
        final UserSportRating tennisRating = createRating(23L, user, Sport.TENNIS, 980);
        Mockito.when(userSportRatingDao.findByUser(user))
                .thenReturn(List.of(footballRating, tennisRating));

        final List<UserSportRating> ratings = userSportRatingService.findRatingsForUser(user);

        Assertions.assertEquals(2, ratings.size());
        Assertions.assertEquals(Sport.FOOTBALL, ratings.get(0).getSport());
        Assertions.assertEquals(Sport.TENNIS, ratings.get(1).getSport());
    }

    @Test
    public void testApplyMatchResultUpdatesPlayersAgainstOpponentTeamAverage() {
        final User strongerWinner = createUser(1L);
        final User weakerWinner = createUser(2L);
        final User loser = createUser(3L);
        final UserSportRating strongerWinnerRating = createRating(10L, strongerWinner, 1400);
        final UserSportRating weakerWinnerRating = createRating(11L, weakerWinner, 800);
        final UserSportRating loserRating = createRating(12L, loser, 1200);
        stubExistingRating(strongerWinner, strongerWinnerRating);
        stubExistingRating(weakerWinner, weakerWinnerRating);
        stubExistingRating(loser, loserRating);
        Mockito.when(userSportRatingDao.save(ArgumentMatchers.any(UserSportRating.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final EloUpdatedResult result =
                userSportRatingService.applyMatchResult(
                        List.of(strongerWinner, weakerWinner), List.of(loser), Sport.FOOTBALL);

        Assertions.assertEquals(Sport.FOOTBALL, result.getSport());
        Assertions.assertEquals(3, result.getChanges().size());
        Assertions.assertEquals(1405, strongerWinnerRating.getElo());
        Assertions.assertEquals(818, weakerWinnerRating.getElo());
        Assertions.assertEquals(1187, loserRating.getElo());
        Assertions.assertEquals(5, findChange(result, strongerWinner).getDelta());
        Assertions.assertEquals(18, findChange(result, weakerWinner).getDelta());
        Assertions.assertEquals(-13, findChange(result, loser).getDelta());
    }

    @Test
    public void testApplyMatchResultMarksCreatedRatingAsPreviouslyUnrated() {
        final User winner = createUser(4L);
        final User loser = createUser(5L);
        final UserSportRating winnerRating = createRating(13L, winner, Sport.PADEL, 1000);
        final UserSportRating loserRating = createRating(14L, loser, Sport.PADEL, 1000);
        Mockito.when(userSportRatingDao.findByUserAndSport(winner, Sport.PADEL))
                .thenReturn(Optional.empty());
        Mockito.when(userSportRatingDao.getOrCreate(winner, Sport.PADEL, 1000))
                .thenReturn(winnerRating);
        stubExistingRating(loser, loserRating, Sport.PADEL);
        Mockito.when(userSportRatingDao.save(ArgumentMatchers.any(UserSportRating.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final EloUpdatedResult result =
                userSportRatingService.applyMatchResult(
                        List.of(winner), List.of(loser), Sport.PADEL);

        Assertions.assertTrue(findChange(result, winner).isPreviouslyUnrated());
        Assertions.assertFalse(findChange(result, loser).isPreviouslyUnrated());
        Assertions.assertEquals(1010, winnerRating.getElo());
        Assertions.assertEquals(990, loserRating.getElo());
    }

    @Test
    public void testApplyMatchResultRejectsUnratedSport() {
        final User winner = createUser(6L);
        final User loser = createUser(7L);

        final IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userSportRatingService.applyMatchResult(
                                        List.of(winner), List.of(loser), Sport.OTHER));

        Assertions.assertEquals("Sport is not rated: other", exception.getMessage());
    }

    @Test
    public void testApplyMatchResultRejectsEmptyWinnerTeam() {
        final User loser = createUser(8L);

        final IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userSportRatingService.applyMatchResult(
                                        List.of(), List.of(loser), Sport.FOOTBALL));

        Assertions.assertEquals(
                "Both teams must contain at least one player", exception.getMessage());
    }

    @Test
    public void testApplyMatchResultRejectsNullPlayers() {
        final User winner = createUser(9L);

        final IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userSportRatingService.applyMatchResult(
                                        List.of(winner),
                                        Collections.singletonList(null),
                                        Sport.FOOTBALL));

        Assertions.assertEquals("Teams cannot contain null players", exception.getMessage());
    }

    private void stubExistingRating(final User user, final UserSportRating rating) {
        stubExistingRating(user, rating, Sport.FOOTBALL);
    }

    private void stubExistingRating(
            final User user, final UserSportRating rating, final Sport sport) {
        Mockito.when(userSportRatingDao.findByUserAndSport(user, sport))
                .thenReturn(Optional.of(rating));
        Mockito.when(userSportRatingDao.getOrCreate(user, sport, 1000)).thenReturn(rating);
    }

    private PlayerEloChange findChange(final EloUpdatedResult result, final User user) {
        return result.getChanges().stream()
                .filter(change -> change.getUser().equals(user))
                .findFirst()
                .orElseThrow(AssertionError::new);
    }

    private UserSportRating createRating(final Long id, final User user, final int elo) {
        return createRating(id, user, Sport.FOOTBALL, elo);
    }

    private UserSportRating createRating(
            final Long id, final User user, final Sport sport, final int elo) {
        return new UserSportRating(
                id,
                user,
                sport,
                elo,
                Instant.parse("2026-05-20T12:00:00Z"),
                Instant.parse("2026-05-20T12:00:00Z"));
    }

    private User createUser(final Long id) {
        return new User(id, "user" + id + "@test.com", "user" + id, null, null, null, null, "en");
    }
}
