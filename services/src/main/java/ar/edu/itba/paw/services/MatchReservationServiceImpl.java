package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.match.*;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationAlreadyJoinedException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationSeriesAlreadyJoinedException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationSeriesNotJoinedException;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MatchReservationServiceImpl implements MatchReservationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchReservationServiceImpl.class);
    private final MatchDao matchDao;
    private final MatchParticipantDao matchParticipantDao;
    private final MatchNotificationService matchNotificationService;
    private final Clock clock;

    @Autowired
    public MatchReservationServiceImpl(
            final MatchDao matchDao,
            final MatchParticipantDao matchParticipantDao,
            final MatchNotificationService matchNotificationService,
            final Clock clock) {
        this.matchDao = matchDao;
        this.matchParticipantDao = matchParticipantDao;
        this.matchNotificationService = matchNotificationService;
        this.clock = clock;
    }

    @Override
    public boolean hasActiveReservation(final Long matchId, final User user) {

        return matchParticipantDao.hasActiveReservation(matchId, user);
    }

    @Override
    public Set<Long> findActiveFutureReservationMatchIdsForSeries(
            final Long seriesId, final User user) {
        if (seriesId == null || user == null) {
            return Set.of();
        }
        return Set.copyOf(
                matchParticipantDao.findActiveFutureReservationMatchIdsForSeries(
                        seriesId, user, Instant.now(clock)));
    }

    @Override
    @Transactional
    public void reserveSpot(final Long matchId, final User user) {
        nonNullUser(user);
        LOGGER.info("Reservation requested matchId={} userId={}", matchId, user);
        final Match match =
                matchDao.findMatchById(matchId)
                        .orElseThrow(
                                () -> {
                                    LOGGER.warn(
                                            "Reservation rejected code=not_found matchId={} userId={}",
                                            matchId,
                                            user.getId());
                                    return new MatchNotFoundException();
                                });

        validateReservable(match, user);

        if (!matchParticipantDao.createReservationIfSpace(matchId, user)) {
            throwReservationFailure(matchId, user);
        }

        LOGGER.info("Reservation created matchId={} userId={}", matchId, user.getId());

        if (!isHost(match, user)) {
            matchNotificationService.notifyHostPlayerJoined(match, user);
        }
    }

    @Transactional
    @Override
    public void reserveSeries(final Long matchId, final User user) {
        nonNullUser(user);
        LOGGER.info("Recurring reservation requested matchId={} userId={}", matchId, user.getId());
        final Match match =
                matchDao.findMatchById(matchId)
                        .orElseThrow(
                                () -> {
                                    LOGGER.warn(
                                            "Recurring reservation rejected code=not_found matchId={} userId={}",
                                            matchId,
                                            user.getId());
                                    return new MatchNotFoundException();
                                });

        if (!match.isRecurringOccurrence()) {
            LOGGER.warn(
                    "Recurring reservation rejected code=not_recurring matchId={} userId={}",
                    matchId,
                    user.getId());
            throw new MatchNotRecurringException();
        }

        final List<Match> occurrences = matchDao.findSeriesOccurrences(match.getSeries().getId());
        final SeriesReservationEvaluation evaluation = evaluateSeriesOccurrences(occurrences, user);
        if (evaluation.reservableOccurrenceCount() == 0) {
            throwSeriesReservationFailure(matchId, user, evaluation);
        }

        final int reservedOccurrences =
                matchParticipantDao.createSeriesReservationsIfSpace(
                        match.getSeries().getId(), user, Instant.now(clock));
        if (reservedOccurrences <= 0) {
            final SeriesReservationEvaluation currentEvaluation =
                    evaluateSeriesOccurrences(
                            matchDao.findSeriesOccurrences(match.getSeries().getId()), user);
            throwSeriesReservationFailure(matchId, user, currentEvaluation);
        }

        LOGGER.info(
                "Recurring reservations created matchId={} seriesId={} userId={} occurrences={}",
                matchId,
                match.getSeries().getId(),
                user.getId(),
                reservedOccurrences);

        if (!isHost(match, user)) {
            matchNotificationService.notifyHostPlayerJoined(match, user);
        }
    }

    @Override
    @Transactional
    public void cancelSeriesReservations(final Long matchId, final User user) {
        nonNullUser(user);
        LOGGER.info(
                "Recurring reservation cancellation requested matchId={} userId={}",
                matchId,
                user.getId());
        final Match match =
                matchDao.findMatchById(matchId)
                        .orElseThrow(
                                () -> {
                                    LOGGER.warn(
                                            "Recurring reservation cancellation rejected code=not_found matchId={} userId={}",
                                            matchId,
                                            user.getId());
                                    return new MatchNotFoundException();
                                });

        if (!match.isRecurringOccurrence()) {
            LOGGER.warn(
                    "Recurring reservation cancellation rejected code=not_recurring matchId={} userId={}",
                    matchId,
                    user.getId());
            throw new MatchNotRecurringException();
        }

        final List<Match> occurrences = matchDao.findSeriesOccurrences(match.getSeries().getId());
        final SeriesCancellationEvaluation evaluation =
                evaluateSeriesCancellations(occurrences, user);
        if (evaluation.activeFutureReservationCount() == 0) {
            throwSeriesCancellationFailure(evaluation);
        }

        final int cancelledReservations =
                matchParticipantDao.cancelFutureSeriesReservations(
                        match.getSeries().getId(), user, Instant.now(clock));
        if (cancelledReservations <= 0) {
            final SeriesCancellationEvaluation currentEvaluation =
                    evaluateSeriesCancellations(
                            matchDao.findSeriesOccurrences(match.getSeries().getId()), user);
            throwSeriesCancellationFailure(currentEvaluation);
        }

        LOGGER.info(
                "Recurring reservations cancelled matchId={} seriesId={} userId={} reservations={}",
                matchId,
                match.getSeries().getId(),
                user,
                cancelledReservations);
    }

    private void validateReservable(final Match match, final User user) {
        if (!EventStatus.OPEN.equals(match.getStatus())) {
            LOGGER.warn(
                    "Reservation rejected code=closed matchId={} userId={} status={}",
                    match.getId(),
                    user,
                    match.getStatus());
            throw new MatchClosedException();
        }

        final boolean hostReservation = isHost(match, user);

        if (!hostReservation && match.getVisibility() != EventVisibility.PUBLIC) {
            LOGGER.warn(
                    "Reservation rejected code=closed matchId={} userId={} visibility={}",
                    match.getId(),
                    user,
                    match.getVisibility());
            throw new MatchClosedException();
        }

        if (!hostReservation && match.getJoinPolicy() != EventJoinPolicy.DIRECT) {
            LOGGER.warn(
                    "Reservation rejected code=closed matchId={} userId={} joinPolicy={}",
                    match.getId(),
                    user,
                    match.getJoinPolicy());
            throw new MatchClosedException();
        }

        if (!match.getStartsAt().isAfter(Instant.now(clock))) {
            LOGGER.warn(
                    "Reservation rejected code=started matchId={} userId={} startsAt={}",
                    match.getId(),
                    user,
                    match.getStartsAt());
            throw new MatchStartedException();
        }

        if (matchParticipantDao.hasActiveReservation(match.getId(), user)) {
            LOGGER.warn(
                    "Reservation rejected code=already_joined matchId={} userId={}",
                    match.getId(),
                    user);
            throw new MatchParticipationAlreadyJoinedException();
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            LOGGER.warn(
                    "Reservation rejected code=full matchId={} userId={} joinedPlayers={} maxPlayers={}",
                    match.getId(),
                    user,
                    match.getJoinedPlayers(),
                    match.getMaxPlayers());
            throw new MatchFullException();
        }
    }

    private void throwReservationFailure(final Long matchId, final User user) {
        final Match currentMatch = matchDao.findMatchById(matchId).orElse(null);

        if (currentMatch == null) {
            throw new MatchNotFoundException();
        }

        final boolean hostReservation = isHost(currentMatch, user);

        if (!EventStatus.OPEN.equals(currentMatch.getStatus())
                || (!hostReservation
                        && (currentMatch.getVisibility() != EventVisibility.PUBLIC
                                || currentMatch.getJoinPolicy() != EventJoinPolicy.DIRECT))) {
            throw new MatchClosedException();
        }

        if (!currentMatch.getStartsAt().isAfter(Instant.now(clock))) {
            throw new MatchStartedException();
        }

        if (matchParticipantDao.hasActiveReservation(matchId, user)) {
            throw new MatchParticipationAlreadyJoinedException();
        }

        throw new MatchFullException();
    }

    private SeriesReservationEvaluation evaluateSeriesOccurrences(
            final List<Match> occurrences, final User user) {
        int futureOccurrenceCount = 0;
        int futureOpenOccurrenceCount = 0;
        int joinedFutureOpenOccurrenceCount = 0;
        int reservableOccurrenceCount = 0;
        int fullOccurrenceCount = 0;
        final Instant now = Instant.now(clock);

        for (final Match occurrence : occurrences) {
            if (!occurrence.getStartsAt().isAfter(now)) {
                continue;
            }

            futureOccurrenceCount++;
            if (!isSeriesReservableOccurrence(occurrence, user)) {
                continue;
            }

            futureOpenOccurrenceCount++;
            final boolean alreadyJoined =
                    user != null
                            && matchParticipantDao.hasActiveReservation(occurrence.getId(), user);
            if (alreadyJoined) {
                joinedFutureOpenOccurrenceCount++;
                continue;
            }

            if (occurrence.getJoinedPlayers() >= occurrence.getMaxPlayers()) {
                fullOccurrenceCount++;
                continue;
            }

            reservableOccurrenceCount++;
        }

        final boolean joined =
                user != null
                        && futureOpenOccurrenceCount > 0
                        && joinedFutureOpenOccurrenceCount == futureOpenOccurrenceCount;
        return new SeriesReservationEvaluation(
                futureOccurrenceCount,
                futureOpenOccurrenceCount,
                joined,
                reservableOccurrenceCount,
                fullOccurrenceCount);
    }

    private static boolean isSeriesReservableOccurrence(final Match occurrence, final User user) {
        return EventStatus.OPEN.equals(occurrence.getStatus())
                && (isHost(occurrence, user)
                        || (occurrence.getVisibility() == EventVisibility.PUBLIC
                                && occurrence.getJoinPolicy() == EventJoinPolicy.DIRECT));
    }

    private static boolean isHost(final Match match, final User user) {
        return user != null && user.getId().equals(match.getHost().getId());
    }

    private void throwSeriesReservationFailure(
            final Long matchId, final User user, final SeriesReservationEvaluation evaluation) {
        if (evaluation.futureOccurrenceCount() == 0) {
            throw new MatchSeriesStartedException();
        }

        if (evaluation.futureOpenOccurrenceCount() == 0) {
            throw new MatchClosedException();
        }

        if (evaluation.joined()) {
            throw new MatchParticipationSeriesAlreadyJoinedException();
        }

        if (evaluation.fullOccurrenceCount() > 0) {
            throw new MatchSeriesFullException();
        }

        LOGGER.warn(
                "Recurring reservation rejected code=series_closed matchId={} userId={}",
                matchId,
                user != null ? user.getId() : null);
        throw new MatchClosedException();
    }

    private SeriesCancellationEvaluation evaluateSeriesCancellations(
            final List<Match> occurrences, final User user) {
        int futureOccurrenceCount = 0;
        int activeFutureReservationCount = 0;
        final Instant now = Instant.now(clock);

        for (final Match occurrence : occurrences) {
            if (!occurrence.getStartsAt().isAfter(now)) {
                continue;
            }

            futureOccurrenceCount++;
            if (matchParticipantDao.hasActiveReservation(occurrence.getId(), user)) {
                activeFutureReservationCount++;
            }
        }

        return new SeriesCancellationEvaluation(
                futureOccurrenceCount, activeFutureReservationCount);
    }

    private void nonNullUser(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null.");
        }
    }

    private static void throwSeriesCancellationFailure(
            final SeriesCancellationEvaluation evaluation) {
        if (evaluation.futureOccurrenceCount() == 0) {
            throw new MatchSeriesStartedException();
        }

        throw new MatchParticipationSeriesNotJoinedException();
    }

    private record SeriesReservationEvaluation(
            int futureOccurrenceCount,
            int futureOpenOccurrenceCount,
            boolean joined,
            int reservableOccurrenceCount,
            int fullOccurrenceCount) {}

    private record SeriesCancellationEvaluation(
            int futureOccurrenceCount, int activeFutureReservationCount) {}
}
