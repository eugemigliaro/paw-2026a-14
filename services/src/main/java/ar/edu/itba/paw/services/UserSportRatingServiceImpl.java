package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EloUpdatedResult;
import ar.edu.itba.paw.models.PlayerEloChange;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserSportRating;
import ar.edu.itba.paw.models.exceptions.userSportRating.UserSportRatingInvalidTeamsException;
import ar.edu.itba.paw.models.exceptions.userSportRating.UserSportRatingSportNotRatedException;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.persistence.UserSportRatingDao;
import ar.edu.itba.paw.persistence.UserSportRatingLookupResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserSportRatingServiceImpl implements UserSportRatingService {

    public static final int INITIAL_ELO = 1000;
    private static final int K_FACTOR = 20;

    private final UserSportRatingDao userSportRatingDao;

    public UserSportRatingServiceImpl(final UserSportRatingDao userSportRatingDao) {
        this.userSportRatingDao = userSportRatingDao;
    }

    @Override
    public Optional<UserSportRating> findRating(final User user, final Sport sport) {
        return userSportRatingDao.findByUserAndSport(user, sport);
    }

    @Override
    public int getEffectiveElo(final User user, final Sport sport) {
        assertRatedSport(sport);
        return findRating(user, sport).map(UserSportRating::getElo).orElse(INITIAL_ELO);
    }

    @Override
    public List<UserSportRating> findRatingsForUser(final User user) {
        return userSportRatingDao.findByUser(user);
    }

    @Override
    @Transactional
    public EloUpdatedResult applyMatchResult(
            final List<User> winners, final List<User> losers, final Sport sport) {
        assertValidMatchResult(winners, losers, sport);

        final double winnersElo = averageTeamElo(winners, sport);
        final double losersElo = averageTeamElo(losers, sport);
        final List<PlayerEloChange> changes = new ArrayList<>();

        for (final User winner : winners) {
            changes.add(updateRating(winner, sport, 1.0, losersElo));
        }

        for (final User loser : losers) {
            changes.add(updateRating(loser, sport, 0.0, winnersElo));
        }

        return new EloUpdatedResult(sport, changes);
    }

    private PlayerEloChange updateRating(
            final User user, final Sport sport, final double result, final double opponentTeamElo) {

        final UserSportRatingLookupResult lookupResult =
                userSportRatingDao.getOrCreate(user, sport, INITIAL_ELO);
        final UserSportRating rating = lookupResult.getRating();
        final boolean previouslyUnrated = lookupResult.isCreated();
        final int previousElo = rating.getElo();
        final int newElo =
                computeNewElo(previousElo, result, expectedResult(previousElo, opponentTeamElo));

        rating.setElo(newElo);
        userSportRatingDao.save(rating);
        return new PlayerEloChange(user, previousElo, newElo, previouslyUnrated);
    }

    private double averageTeamElo(final List<User> team, final Sport sport) {
        return team.stream()
                .mapToInt(user -> getEffectiveElo(user, sport))
                .average()
                .orElseThrow(UserSportRatingInvalidTeamsException::new);
    }

    private double expectedResult(final int playerElo, final double opponentElo) {
        return 1.0 / (1.0 + Math.pow(10.0, (opponentElo - playerElo) / 400.0));
    }

    private int computeNewElo(
            final int currentElo, final double result, final double expectedResult) {
        return (int) Math.round(currentElo + K_FACTOR * (result - expectedResult));
    }

    private void assertValidMatchResult(
            final List<User> winners, final List<User> losers, final Sport sport) {
        assertRatedSport(sport);
        if (winners == null || winners.isEmpty() || losers == null || losers.isEmpty()) {
            throw new UserSportRatingInvalidTeamsException();
        }
        if (winners.stream().anyMatch(user -> user == null)
                || losers.stream().anyMatch(user -> user == null)) {
            throw new UserSportRatingInvalidTeamsException();
        }
    }

    private void assertRatedSport(final Sport sport) {
        if (sport == null || sport == Sport.OTHER) {
            throw new UserSportRatingSportNotRatedException();
        }
    }
}
