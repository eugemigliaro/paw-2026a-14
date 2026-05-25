package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserSportRating;
import ar.edu.itba.paw.models.types.Sport;
import java.util.List;
import java.util.Optional;

public interface UserSportRatingDao {

    Optional<UserSportRating> findByUserAndSport(User user, Sport sport);

    UserSportRatingLookupResult getOrCreate(User user, Sport sport, int initialElo);

    UserSportRating save(UserSportRating rating);

    List<UserSportRating> findByUser(User user);

    List<UserSportRating> findTopBySport(Sport sport, int limit);
}
