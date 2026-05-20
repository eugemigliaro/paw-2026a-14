package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EloUpdatedResult;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserSportRating;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.persistence.UserSportRatingDao;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSportRatingServiceImpl implements UserSportRatingService {

    private final UserSportRatingDao userSportRatingDao;

    public UserSportRatingServiceImpl(final UserSportRatingDao userSportRatingDao) {
        this.userSportRatingDao = userSportRatingDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSportRating> findRating(final User user, final Sport sport) {
        return userSportRatingDao.findByUserAndSport(user, sport);
    }

    @Override
    public List<UserSportRating> findRatingsForUser(final User user) {
        return userSportRatingDao.findByUser(user);
    }

    @Override
    public List<UserSportRating> findLeaderboard(final Sport sport, final int limit) {
        throw new UnsupportedOperationException("findLeaderboard is not implemented yet");
    }

    @Override
    public EloUpdatedResult applyMatchResult(
            final User winner, final User loser, final Sport sport) {
        throw new UnsupportedOperationException("applyMatchResult is not implemented yet");
    }
}
