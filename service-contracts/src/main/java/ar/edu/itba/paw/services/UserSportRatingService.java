package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EloUpdatedResult;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserSportRating;
import ar.edu.itba.paw.models.types.Sport;
import java.util.List;
import java.util.Optional;

public interface UserSportRatingService {

    Optional<UserSportRating> findRating(User user, Sport sport);

    List<UserSportRating> findRatingsForUser(User user);

    List<UserSportRating> findLeaderboard(Sport sport, int limit);

    EloUpdatedResult applyMatchResult(User winner, User loser, Sport sport);
}
