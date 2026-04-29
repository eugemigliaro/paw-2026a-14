package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.MatchReservationException;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
