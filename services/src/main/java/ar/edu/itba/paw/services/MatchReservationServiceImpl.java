package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.exceptions.MatchReservationException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MatchReservationServiceImpl implements MatchReservationService {

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
        final Match match =
                matchDao.findMatchById(matchId)
                        .orElseThrow(
                                () ->
                                        new MatchReservationException(
                                                "not_found", "The event does not exist."));

        validateReservable(match, userId);

        if (!matchParticipantDao.createReservationIfSpace(matchId, userId)) {
            throw buildReservationFailure(matchId, userId);
        }
    }

    private void validateReservable(final Match match, final Long userId) {
        if (!"open".equalsIgnoreCase(match.getStatus())) {
            throw new MatchReservationException(
                    "closed", "The event is not open for reservations.");
        }

        if (!"public".equalsIgnoreCase(match.getVisibility())) {
            throw new MatchReservationException(
                    "closed", "The event is not open for reservations.");
        }

        if (!"direct".equalsIgnoreCase(match.getJoinPolicy())) {
            throw new MatchReservationException(
                    "closed", "The event requires host approval to join.");
        }

        if (!match.getStartsAt().isAfter(Instant.now(clock))) {
            throw new MatchReservationException("started", "The event has already started.");
        }

        if (matchParticipantDao.hasActiveReservation(match.getId(), userId)) {
            throw new MatchReservationException(
                    "already_joined",
                    "This email already has a confirmed reservation for the event.");
        }

        if (match.getJoinedPlayers() >= match.getMaxPlayers()) {
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
