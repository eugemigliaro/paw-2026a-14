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
    public void reserveSeries(final Long matchId, final Long userId) {
        LOGGER.info("Series reservation requested matchId={} userId={}", matchId, userId);
        final Match match =
                matchDao.findMatchById(matchId)
                        .orElseThrow(
                                () -> {
                                    LOGGER.warn(
                                            "Series reservation rejected code=not_found matchId={} userId={}",
                                            matchId,
                                            userId);
                                    return new MatchReservationException(
                                            "not_found", "The event does not exist.");
                                });

        if (!match.isRecurringOccurrence()) {
            LOGGER.warn(
                    "Series reservation rejected code=not_recurring matchId={} userId={}",
                    matchId,
                    userId);
            throw new MatchReservationException(
                    "not_recurring", "The event is not part of a recurring series.");
        }

        final List<Match> occurrences = matchDao.findSeriesOccurrences(match.getSeriesId());
        final SeriesReservationEvaluation evaluation =
                evaluateSeriesOccurrences(occurrences, userId);
        if (evaluation.reservableOccurrenceCount() == 0) {
            final MatchReservationException failure =
                    buildSeriesReservationFailure(matchId, userId, evaluation);
            LOGGER.warn(
                    "Series reservation rejected code={} matchId={} seriesId={} userId={}",
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
                    "Series reservation rejected code={} matchId={} seriesId={} userId={}",
                    failure.getCode(),
                    matchId,
                    match.getSeriesId(),
                    userId);
            throw failure;
        }

        LOGGER.info(
                "Series reservations created matchId={} seriesId={} userId={} occurrences={}",
                matchId,
                match.getSeriesId(),
                userId,
                reservedOccurrences);
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
                    "series_started", "There are no upcoming occurrences left in this series.");
        }

        if (evaluation.futureOpenOccurrenceCount() == 0) {
            return new MatchReservationException(
                    "series_closed", "The upcoming series occurrences are not open.");
        }

        if (evaluation.joined()) {
            return new MatchReservationException(
                    "series_already_joined",
                    "This account already has reservations for the future series.");
        }

        if (evaluation.fullOccurrenceCount() > 0) {
            return new MatchReservationException(
                    "series_full", "The available future series occurrences are full.");
        }

        LOGGER.warn(
                "Series reservation rejected code=series_closed matchId={} userId={}",
                matchId,
                userId);
        return new MatchReservationException(
                "series_closed", "The upcoming series occurrences are not open.");
    }

    private record SeriesReservationEvaluation(
            int futureOccurrenceCount,
            int futureOpenOccurrenceCount,
            boolean joined,
            int reservableOccurrenceCount,
            int fullOccurrenceCount) {}
}
