package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.MatchReservationException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchReservationServiceImpl implements MatchReservationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchReservationServiceImpl.class);
    private final MatchDao matchDao;
    private final MatchParticipantDao matchParticipantDao;
    private final Clock clock;

    @Autowired
    public MatchReservationServiceImpl(
            final MatchDao matchDao,
            final MatchParticipantDao matchParticipantDao,
            final Clock clock) {
        this.matchDao = matchDao;
        this.matchParticipantDao = matchParticipantDao;
        this.clock = clock;
    }

    @Override
    public boolean hasActiveReservation(final Long matchId, final Long userId) {
        return matchParticipantDao.hasActiveReservation(matchId, userId);
    }

    @Override
    public void reserveSpot(final Long matchId, final Long userId) {
        LOGGER.info("Reservation requested matchId={} userId={}", matchId, userId);
        final Match match =
                matchDao.findMatchById(matchId)
                        .orElseThrow(
                                () -> {
                                    LOGGER.warn(
                                            "Reservation rejected code=not_found matchId={} userId={}",
                                            matchId,
                                            userId);
                                    return new MatchReservationException(
                                            "not_found", "The event does not exist.");
                                });

        validateReservable(match, userId);

        if (!matchParticipantDao.createReservationIfSpace(matchId, userId)) {
            final MatchReservationException failure = buildReservationFailure(matchId, userId);
            LOGGER.warn(
                    "Reservation rejected code={} matchId={} userId={}",
                    failure.getCode(),
                    matchId,
                    userId);
            throw failure;
        }

        LOGGER.info("Reservation created matchId={} userId={}", matchId, userId);
    }

    @Transactional
    @Override
    public void reserveSeries(final Long matchId, final Long userId) {
        LOGGER.info("Recurring reservation requested matchId={} userId={}", matchId, userId);
        final Match match =
                matchDao.findMatchById(matchId)
                        .orElseThrow(
                                () -> {
                                    LOGGER.warn(
                                            "Recurring reservation rejected code=not_found matchId={} userId={}",
                                            matchId,
                                            userId);
                                    return new MatchReservationException(
                                            "not_found", "The event does not exist.");
                                });

        if (!match.isRecurringOccurrence()) {
            LOGGER.warn(
                    "Recurring reservation rejected code=not_recurring matchId={} userId={}",
                    matchId,
                    userId);
            throw new MatchReservationException(
                    "not_recurring", "The event is not a recurring event.");
        }

        final List<Match> occurrences = matchDao.findSeriesOccurrences(match.getSeriesId());
        final SeriesReservationEvaluation evaluation =
                evaluateSeriesOccurrences(occurrences, userId);
        if (evaluation.reservableOccurrenceCount() == 0) {
            final MatchReservationException failure =
                    buildSeriesReservationFailure(matchId, userId, evaluation);
            LOGGER.warn(
                    "Recurring reservation rejected code={} matchId={} seriesId={} userId={}",
                    failure.getCode(),
                    matchId,
                    match.getSeriesId(),
                    userId);
            throw failure;
        }

        final int reservedOccurrences =
                matchParticipantDao.createSeriesReservationsIfSpace(
                        match.getSeriesId(), userId, Instant.now(clock));
        if (reservedOccurrences <= 0) {
            final SeriesReservationEvaluation currentEvaluation =
                    evaluateSeriesOccurrences(
                            matchDao.findSeriesOccurrences(match.getSeriesId()), userId);
            final MatchReservationException failure =
                    buildSeriesReservationFailure(matchId, userId, currentEvaluation);
            LOGGER.warn(
                    "Recurring reservation rejected code={} matchId={} seriesId={} userId={}",
                    failure.getCode(),
                    matchId,
                    match.getSeriesId(),
                    userId);
            throw failure;
        }

        LOGGER.info(
                "Recurring reservations created matchId={} seriesId={} userId={} occurrences={}",
                matchId,
                match.getSeriesId(),
                userId,
                reservedOccurrences);
    }

    @Override
    @Transactional
    public void cancelSeriesReservations(final Long matchId, final Long userId) {
        LOGGER.info(
                "Recurring reservation cancellation requested matchId={} userId={}",
                matchId,
                userId);
        final Match match =
                matchDao.findMatchById(matchId)
                        .orElseThrow(
                                () -> {
                                    LOGGER.warn(
                                            "Recurring reservation cancellation rejected code=not_found matchId={} userId={}",
                                            matchId,
                                            userId);
                                    return new MatchReservationException(
                                            "not_found", "The event does not exist.");
                                });

        if (!match.isRecurringOccurrence()) {
            LOGGER.warn(
                    "Recurring reservation cancellation rejected code=not_recurring matchId={} userId={}",
                    matchId,
                    userId);
            throw new MatchReservationException(
                    "not_recurring", "The event is not a recurring event.");
        }

        final List<Match> occurrences = matchDao.findSeriesOccurrences(match.getSeriesId());
        final SeriesCancellationEvaluation evaluation =
                evaluateSeriesCancellations(occurrences, userId);
        if (evaluation.activeFutureReservationCount() == 0) {
            final MatchReservationException failure = buildSeriesCancellationFailure(evaluation);
            LOGGER.warn(
                    "Recurring reservation cancellation rejected code={} matchId={} seriesId={} userId={}",
                    failure.getCode(),
                    matchId,
                    match.getSeriesId(),
                    userId);
            throw failure;
        }

        final int cancelledReservations =
                matchParticipantDao.cancelFutureSeriesReservations(
                        match.getSeriesId(), userId, Instant.now(clock));
        if (cancelledReservations <= 0) {
            final SeriesCancellationEvaluation currentEvaluation =
                    evaluateSeriesCancellations(
                            matchDao.findSeriesOccurrences(match.getSeriesId()), userId);
            final MatchReservationException failure =
                    buildSeriesCancellationFailure(currentEvaluation);
            LOGGER.warn(
                    "Recurring reservation cancellation rejected code={} matchId={} seriesId={} userId={}",
                    failure.getCode(),
                    matchId,
                    match.getSeriesId(),
                    userId);
            throw failure;
        }

        LOGGER.info(
                "Recurring reservations cancelled matchId={} seriesId={} userId={} reservations={}",
                matchId,
                match.getSeriesId(),
                userId,
                cancelledReservations);
    }

    private void validateReservable(final Match match, final Long userId) {
        if (!"open".equalsIgnoreCase(match.getStatus())) {
            LOGGER.warn(
                    "Reservation rejected code=closed matchId={} userId={} status={}",
                    match.getId(),
                    userId,
                    match.getStatus());
            throw new MatchReservationException(
                    "closed", "The event is not open for reservations.");
        }

        if (!"public".equalsIgnoreCase(match.getVisibility())) {
            LOGGER.warn(
                    "Reservation rejected code=closed matchId={} userId={} visibility={}",
                    match.getId(),
                    userId,
                    match.getVisibility());
            throw new MatchReservationException(
                    "closed", "The event is not open for reservations.");
        }

        if (!"direct".equalsIgnoreCase(match.getJoinPolicy())) {
            LOGGER.warn(
                    "Reservation rejected code=closed matchId={} userId={} joinPolicy={}",
                    match.getId(),
                    userId,
                    match.getJoinPolicy());
            throw new MatchReservationException(
                    "closed", "The event requires host approval to join.");
        }

        if (!match.getStartsAt().isAfter(Instant.now(clock))) {
            LOGGER.warn(
                    "Reservation rejected code=started matchId={} userId={} startsAt={}",
                    match.getId(),
                    userId,
                    match.getStartsAt());
            throw new MatchReservationException("started", "The event has already started.");
        }

        if (matchParticipantDao.hasActiveReservation(match.getId(), userId)) {
            LOGGER.warn(
                    "Reservation rejected code=already_joined matchId={} userId={}",
                    match.getId(),
                    userId);
            throw new MatchReservationException(
                    "already_joined",
                    "This email already has a confirmed reservation for the event.");
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
            LOGGER.warn(
                    "Reservation rejected code=full matchId={} userId={} joinedPlayers={} maxPlayers={}",
                    match.getId(),
                    userId,
                    match.getJoinedPlayers(),
                    match.getMaxPlayers());
            throw new MatchReservationException("full", "The event is already full.");
        }
    }

    private MatchReservationException buildReservationFailure(
            final Long matchId, final Long userId) {
        final Match currentMatch = matchDao.findMatchById(matchId).orElse(null);

        if (currentMatch == null) {
            return new MatchReservationException("not_found", "The event does not exist.");
        }

        if (!"open".equalsIgnoreCase(currentMatch.getStatus())
                || !"public".equalsIgnoreCase(currentMatch.getVisibility())
                || !"direct".equalsIgnoreCase(currentMatch.getJoinPolicy())) {
            return new MatchReservationException(
                    "closed", "The event is not open for reservations.");
        }

        if (!currentMatch.getStartsAt().isAfter(Instant.now(clock))) {
            return new MatchReservationException("started", "The event has already started.");
        }

        if (matchParticipantDao.hasActiveReservation(matchId, userId)) {
            return new MatchReservationException(
                    "already_joined",
                    "This email already has a confirmed reservation for the event.");
        }

        return new MatchReservationException(
                "full", "The event filled up before the reservation could be confirmed.");
    }

    private SeriesReservationEvaluation evaluateSeriesOccurrences(
            final List<Match> occurrences, final Long userId) {
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
            if (!isSeriesReservableOccurrence(occurrence)) {
                continue;
            }

            futureOpenOccurrenceCount++;
            final boolean alreadyJoined =
                    userId != null
                            && matchParticipantDao.hasActiveReservation(occurrence.getId(), userId);
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
                userId != null
                        && futureOpenOccurrenceCount > 0
                        && joinedFutureOpenOccurrenceCount == futureOpenOccurrenceCount;
        return new SeriesReservationEvaluation(
                futureOccurrenceCount,
                futureOpenOccurrenceCount,
                joined,
                reservableOccurrenceCount,
                fullOccurrenceCount);
    }

    private static boolean isSeriesReservableOccurrence(final Match occurrence) {
        return "open".equalsIgnoreCase(occurrence.getStatus())
                && "public".equalsIgnoreCase(occurrence.getVisibility())
                && "direct".equalsIgnoreCase(occurrence.getJoinPolicy());
    }

    private MatchReservationException buildSeriesReservationFailure(
            final Long matchId, final Long userId, final SeriesReservationEvaluation evaluation) {
        if (evaluation.futureOccurrenceCount() == 0) {
            return new MatchReservationException(
                    "series_started", "There are no upcoming dates left in this recurring event.");
        }

        if (evaluation.futureOpenOccurrenceCount() == 0) {
            return new MatchReservationException(
                    "series_closed", "The upcoming recurring dates are not open.");
        }

        if (evaluation.joined()) {
            return new MatchReservationException(
                    "series_already_joined",
                    "This account already has reservations for the future recurring dates.");
        }

        if (evaluation.fullOccurrenceCount() > 0) {
            return new MatchReservationException(
                    "series_full", "The available future recurring dates are full.");
        }

        LOGGER.warn(
                "Recurring reservation rejected code=series_closed matchId={} userId={}",
                matchId,
                userId);
        return new MatchReservationException(
                "series_closed", "The upcoming recurring dates are not open.");
    }

    private SeriesCancellationEvaluation evaluateSeriesCancellations(
            final List<Match> occurrences, final Long userId) {
        int futureOccurrenceCount = 0;
        int activeFutureReservationCount = 0;
        final Instant now = Instant.now(clock);

        for (final Match occurrence : occurrences) {
            if (!occurrence.getStartsAt().isAfter(now)) {
                continue;
            }

            futureOccurrenceCount++;
            if (matchParticipantDao.hasActiveReservation(occurrence.getId(), userId)) {
                activeFutureReservationCount++;
            }
        }

        return new SeriesCancellationEvaluation(
                futureOccurrenceCount, activeFutureReservationCount);
    }

    private static MatchReservationException buildSeriesCancellationFailure(
            final SeriesCancellationEvaluation evaluation) {
        if (evaluation.futureOccurrenceCount() == 0) {
            return new MatchReservationException(
                    "series_started", "There are no upcoming dates left in this recurring event.");
        }

        return new MatchReservationException(
                "series_not_joined",
                "This account does not have future reservations for this recurring event.");
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
