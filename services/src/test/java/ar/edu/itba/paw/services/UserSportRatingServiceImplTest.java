package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserSportRating;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.persistence.UserSportRatingDao;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private User createUser(final Long id) {
        return new User(id, "user" + id + "@test.com", "user" + id, null, null, null, null, "en");
    }
}
