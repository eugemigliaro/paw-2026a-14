package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchParticipant;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.ParticipantScope;
import ar.edu.itba.paw.models.types.ParticipantStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

@Repository
public class MatchParticipantJpaDao implements MatchParticipantDao {

    @PersistenceContext private EntityManager em;

    private static final List<ParticipantStatus> ACTIVE_RESERVATION_STATUSES =
            List.of(ParticipantStatus.JOINED, ParticipantStatus.CHECKED_IN);

    private static final List<ParticipantStatus> ACTIVE_AND_INVITED_STATUSES =
            List.of(
                    ParticipantStatus.JOINED,
                    ParticipantStatus.CHECKED_IN,
                    ParticipantStatus.INVITED);

    private static final List<ParticipantStatus> ACTIVE_PARTICIPANT_STATUSES =
            List.of(
                    ParticipantStatus.JOINED,
                    ParticipantStatus.CHECKED_IN,
                    ParticipantStatus.INVITED);

    @Override
    public boolean hasActiveReservation(final Long matchId, final User user) {
        final TypedQuery<Long> query =
                em.createQuery(
                        "SELECT COUNT(mp) FROM MatchParticipant mp"
                                + " WHERE mp.match.id = :matchId AND mp.user.id = :userId"
                                + " AND mp.status IN :statuses",
                        Long.class);
        query.setParameter("matchId", matchId);
        query.setParameter("userId", user.getId());
        query.setParameter("statuses", ACTIVE_RESERVATION_STATUSES);
        return query.getSingleResult() > 0;
    }

    @Override
    public List<Long> findActiveFutureReservationMatchIdsForSeries(
            final Long seriesId, final User user, final Instant startsAfter) {
        final TypedQuery<Long> query =
                em.createQuery(
                        "SELECT mp.match.id FROM MatchParticipant mp"
                                + " JOIN mp.match m"
                                + " WHERE m.series.id = :seriesId"
                                + " AND m.startsAt > :startsAfter"
                                + " AND mp.user.id = :userId"
                                + " AND mp.status IN :statuses"
                                + " ORDER BY m.startsAt ASC, m.id ASC",
                        Long.class);
        query.setParameter("seriesId", seriesId);
        query.setParameter("startsAfter", startsAfter);
        query.setParameter("userId", user.getId());
        query.setParameter("statuses", ACTIVE_RESERVATION_STATUSES);
        return query.getResultList();
    }

    @Override
    public List<Long> findPendingFutureRequestMatchIdsForSeries(
            final Long seriesId, final User user, final Instant startsAfter) {
        final TypedQuery<Long> query =
                em.createQuery(
                        "SELECT mp.match.id FROM MatchParticipant mp"
                                + " JOIN mp.match m"
                                + " WHERE m.series.id = :seriesId"
                                + " AND m.startsAt > :startsAfter"
                                + " AND mp.user.id = :userId"
                                + " AND mp.status = :status"
                                + " ORDER BY m.startsAt ASC, m.id ASC",
                        Long.class);
        query.setParameter("seriesId", seriesId);
        query.setParameter("startsAfter", startsAfter);
        query.setParameter("userId", user.getId());
        query.setParameter("status", ParticipantStatus.PENDING_APPROVAL);
        return query.getResultList();
    }

    @Override
    public boolean createReservationIfSpace(final Long matchId, final User user) {
        final Match match = em.find(Match.class, matchId, LockModeType.PESSIMISTIC_WRITE);
        if (match == null) {
            return false;
        }

        if (match.getStatus() != EventStatus.OPEN || !match.getStartsAt().isAfter(Instant.now())) {
            return false;
        }

        if (match.getVisibility() != EventVisibility.PUBLIC
                || match.getJoinPolicy() != EventJoinPolicy.DIRECT) {
            if (!match.getHost().getId().equals(user.getId())) {
                return false;
            }
        }

        final long joinedCount = countParticipants(matchId, ACTIVE_RESERVATION_STATUSES);
        if (joinedCount >= match.getMaxPlayers()) {
            return false;
        }

        return upsertParticipant(match, user, ParticipantStatus.JOINED, ParticipantScope.MATCH);
    }

    @Override
    public int createSeriesReservationsIfSpace(
            final Long seriesId, final User user, final Instant startsAfter) {
        final TypedQuery<Match> query =
                em.createQuery(
                        "FROM Match m WHERE m.series.id = :seriesId"
                                + " AND m.startsAt > :startsAfter"
                                + " AND m.status = :status"
                                + " AND ((m.visibility = :publicVis AND m.joinPolicy = :directJoin)"
                                + "      OR m.host.id = :userId)"
                                + " ORDER BY m.startsAt ASC",
                        Match.class);
        query.setParameter("seriesId", seriesId);
        query.setParameter("startsAfter", startsAfter);
        query.setParameter("status", EventStatus.OPEN);
        query.setParameter("publicVis", EventVisibility.PUBLIC);
        query.setParameter("directJoin", EventJoinPolicy.DIRECT);
        query.setParameter("userId", user.getId());

        final List<Match> matches = query.getResultList();
        if (matches.isEmpty()) {
            return 0;
        }

        for (Match m : matches) {
            em.lock(m, LockModeType.PESSIMISTIC_WRITE);
        }

        final Map<Long, Long> counts =
                countParticipantsBatch(
                        matches.stream().map(Match::getId).toList(), ACTIVE_RESERVATION_STATUSES);

        int count = 0;
        for (final Match match : matches) {
            final long joinedCount = counts.getOrDefault(match.getId(), 0L);
            if (joinedCount < match.getMaxPlayers()) {
                if (upsertParticipant(
                        match, user, ParticipantStatus.JOINED, ParticipantScope.SERIES)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public int cancelFutureSeriesReservations(
            final Long seriesId, final User user, final Instant startsAfter) {
        return em.createQuery(
                        "UPDATE MatchParticipant mp SET mp.status = :cancelledStatus, mp.version = mp.version + 1"
                                + " WHERE mp.user.id = :userId"
                                + " AND mp.status IN :activeStatuses"
                                + " AND mp.match.id IN (SELECT m.id FROM Match m"
                                + "                    WHERE m.series.id = :seriesId"
                                + "                    AND m.startsAt > :startsAfter)")
                .setParameter("cancelledStatus", ParticipantStatus.CANCELLED)
                .setParameter("userId", user.getId())
                .setParameter("activeStatuses", ACTIVE_RESERVATION_STATUSES)
                .setParameter("seriesId", seriesId)
                .setParameter("startsAfter", startsAfter)
                .executeUpdate();
    }

    @Override
    public List<User> findConfirmedParticipants(final Long matchId) {
        final Match match = em.find(Match.class, matchId);
        if (match == null) {
            return Collections.emptyList();
        }

        return match.getParticipants().stream()
                .filter(mp -> ACTIVE_RESERVATION_STATUSES.contains(mp.getStatus()))
                .map(MatchParticipant::getUser)
                .toList();
    }

    @Override
    public boolean hasPendingRequest(final Long matchId, final User user) {
        final Match match = em.find(Match.class, matchId);
        if (match == null) {
            return false;
        }

        return !match.getParticipants().stream()
                .filter(
                        mp ->
                                mp.getUser().equals(user)
                                        && mp.getStatus() == ParticipantStatus.PENDING_APPROVAL)
                .findFirst()
                .isEmpty();
    }

    @Override
    public boolean createJoinRequest(final Long matchId, final User user) {
        final Match match = em.find(Match.class, matchId);
        if (match == null) {
            return false;
        }
        return upsertParticipant(
                match, user, ParticipantStatus.PENDING_APPROVAL, ParticipantScope.MATCH);
    }

    @Override
    public boolean createSeriesJoinRequestIfSpace(final Long matchId, final User user) {
        final Match match = em.find(Match.class, matchId, LockModeType.PESSIMISTIC_WRITE);
        if (match == null
                || match.getVisibility() != EventVisibility.PUBLIC
                || match.getJoinPolicy() != EventJoinPolicy.APPROVAL_REQUIRED
                || match.getStatus() != EventStatus.OPEN
                || !match.getStartsAt().isAfter(Instant.now())) {
            return false;
        }

        final long activeCount = countParticipants(matchId, ACTIVE_AND_INVITED_STATUSES);
        if (activeCount >= match.getMaxPlayers()) {
            return false;
        }

        return upsertParticipant(
                match, user, ParticipantStatus.PENDING_APPROVAL, ParticipantScope.SERIES);
    }

    @Override
    public List<User> findPendingRequests(final Long matchId) {
        final Match match = em.find(Match.class, matchId);
        if (match == null) {
            return Collections.emptyList();
        }

        return match.getParticipants().stream()
                .filter(mp -> mp.getStatus() == ParticipantStatus.PENDING_APPROVAL)
                .map(MatchParticipant::getUser)
                .toList();
    }

    @Override
    public int countPendingRequests(final Long matchId) {
        return findPendingRequests(matchId).size();
    }

    @Override
    public List<PendingJoinRequest> findPendingRequestsForHost(final User host) {
        final List<MatchParticipant> participants =
                em.createQuery(
                                "SELECT mp FROM MatchParticipant mp"
                                        + " JOIN mp.match m"
                                        + " JOIN mp.user u"
                                        + " WHERE m.host.id = :hostUserId"
                                        + " AND m.joinPolicy = :joinPolicy"
                                        + " AND mp.status = :status"
                                        + " AND (mp.scope = :matchScope OR m.startsAt = ("
                                        + "   SELECT MIN(m2.startsAt) FROM MatchParticipant mp2"
                                        + "   JOIN mp2.match m2"
                                        + "   WHERE mp2.user.id = u.id AND mp2.status = :status AND mp2.scope = :seriesScope"
                                        + "   AND m2.series.id = m.series.id"
                                        + " ))"
                                        + " ORDER BY m.startsAt ASC, mp.joinedAt ASC, u.username ASC",
                                MatchParticipant.class)
                        .setParameter("hostUserId", host.getId())
                        .setParameter("joinPolicy", EventJoinPolicy.APPROVAL_REQUIRED)
                        .setParameter("status", ParticipantStatus.PENDING_APPROVAL)
                        .setParameter("matchScope", ParticipantScope.MATCH)
                        .setParameter("seriesScope", ParticipantScope.SERIES)
                        .getResultList();

        if (participants.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Long> matchIds =
                participants.stream().map(mp -> mp.getMatch().getId()).distinct().toList();

        final List<Match> matches = findMatchesByIds(matchIds);
        final Map<Long, Match> matchMap =
                matches.stream().collect(Collectors.toMap(Match::getId, m -> m));

        return participants.stream()
                .map(
                        mp ->
                                new PendingJoinRequest(
                                        matchMap.get(mp.getMatch().getId()),
                                        mp.getUser(),
                                        mp.getScope() == ParticipantScope.SERIES))
                .toList();
    }

    @Override
    public boolean approveRequest(final Long matchId, final User user) {
        final MatchParticipant mp = findParticipantInternal(matchId, user.getId());
        if (mp != null && mp.getStatus() == ParticipantStatus.PENDING_APPROVAL) {
            mp.setStatus(ParticipantStatus.JOINED);
            mp.setScope(ParticipantScope.MATCH);
            return true;
        }
        return false;
    }

    @Override
    public int approveAllPendingRequests(final Long matchId) {
        final Match match = em.find(Match.class, matchId, LockModeType.PESSIMISTIC_WRITE);

        if (match == null) {
            return 0;
        }

        Instant now = Instant.now();

        final List<MatchParticipant> toBeUpdated =
                match.getParticipants().stream()
                        .filter(mp -> mp.getStatus() == ParticipantStatus.PENDING_APPROVAL)
                        .toList();

        toBeUpdated.forEach(
                mp -> {
                    mp.setStatus(ParticipantStatus.JOINED);
                    mp.setScope(ParticipantScope.MATCH);
                    mp.setJoinedAt(now);
                    mp.setVersion(mp.getVersion() + 1);
                });

        return toBeUpdated.size();
    }

    @Override
    public int approveSeriesJoinRequest(
            final Long seriesId, final User user, final Instant startsAfter) {
        final TypedQuery<Match> query =
                em.createQuery(
                        "FROM Match m WHERE m.series.id = :seriesId"
                                + " AND m.startsAt > :startsAfter"
                                + " AND m.status = :status"
                                + " AND m.visibility = :publicVis"
                                + " AND m.joinPolicy = :approvalJoin"
                                + " ORDER BY m.startsAt ASC",
                        Match.class);
        query.setParameter("seriesId", seriesId);
        query.setParameter("startsAfter", startsAfter);
        query.setParameter("status", EventStatus.OPEN);
        query.setParameter("publicVis", EventVisibility.PUBLIC);
        query.setParameter("approvalJoin", EventJoinPolicy.APPROVAL_REQUIRED);

        final List<Match> matches = query.getResultList();
        if (matches.isEmpty()) {
            return 0;
        }

        for (Match m : matches) {
            em.lock(m, LockModeType.PESSIMISTIC_WRITE);
        }

        final Map<Long, Long> counts =
                countParticipantsBatch(
                        matches.stream().map(Match::getId).toList(), ACTIVE_AND_INVITED_STATUSES);

        int approvedCount = 0;
        for (final Match match : matches) {
            final long activeCount = counts.getOrDefault(match.getId(), 0L);
            if (activeCount < match.getMaxPlayers()) {
                if (upsertParticipant(
                        match, user, ParticipantStatus.JOINED, ParticipantScope.MATCH)) {
                    approvedCount++;
                }
            }
        }

        if (approvedCount > 0) {
            em.createQuery(
                            "UPDATE MatchParticipant mp SET mp.status = :cancelledStatus, mp.scope = :matchScope, mp.version = mp.version + 1"
                                    + " WHERE mp.user.id = :userId AND mp.status = :pendingStatus"
                                    + " AND mp.scope = :seriesScope"
                                    + " AND mp.match.id IN (SELECT m.id FROM Match m WHERE m.series.id = :seriesId)")
                    .setParameter("cancelledStatus", ParticipantStatus.CANCELLED)
                    .setParameter("userId", user.getId())
                    .setParameter("pendingStatus", ParticipantStatus.PENDING_APPROVAL)
                    .setParameter("seriesId", seriesId)
                    .setParameter("matchScope", ParticipantScope.MATCH)
                    .setParameter("seriesScope", ParticipantScope.SERIES)
                    .executeUpdate();
        }

        return approvedCount;
    }

    @Override
    public boolean isSeriesJoinRequest(final Long matchId, final User user) {
        return em.createQuery(
                                "SELECT COUNT(mp) FROM MatchParticipant mp"
                                        + " WHERE mp.match.id = :matchId AND mp.user.id = :userId"
                                        + " AND mp.status = :status AND mp.scope = :scope",
                                Long.class)
                        .setParameter("matchId", matchId)
                        .setParameter("userId", user.getId())
                        .setParameter("status", ParticipantStatus.PENDING_APPROVAL)
                        .setParameter("scope", ParticipantScope.SERIES)
                        .getSingleResult()
                > 0;
    }

    @Override
    public boolean hasPendingSeriesRequest(final Long seriesId, final User user) {
        return em.createQuery(
                                "SELECT COUNT(mp) FROM MatchParticipant mp"
                                        + " JOIN mp.match m"
                                        + " WHERE m.series.id = :seriesId AND mp.user.id = :userId"
                                        + " AND mp.status = :status AND mp.scope = :scope"
                                        + " AND m.visibility = :publicVis AND m.joinPolicy = :approvalJoin"
                                        + " AND m.status = :matchStatus AND m.startsAt > :now",
                                Long.class)
                        .setParameter("seriesId", seriesId)
                        .setParameter("userId", user.getId())
                        .setParameter("status", ParticipantStatus.PENDING_APPROVAL)
                        .setParameter("scope", ParticipantScope.SERIES)
                        .setParameter("publicVis", EventVisibility.PUBLIC)
                        .setParameter("approvalJoin", EventJoinPolicy.APPROVAL_REQUIRED)
                        .setParameter("matchStatus", EventStatus.OPEN)
                        .setParameter("now", Instant.now())
                        .getSingleResult()
                > 0;
    }

    @Override
    public boolean rejectRequest(final Long matchId, final User user) {
        final MatchParticipant mp = findParticipantInternal(matchId, user.getId());
        if (mp != null && mp.getStatus() == ParticipantStatus.PENDING_APPROVAL) {
            mp.setStatus(ParticipantStatus.CANCELLED);
            mp.setScope(ParticipantScope.MATCH);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeParticipant(final Long matchId, final User user) {
        final MatchParticipant mp = findParticipantInternal(matchId, user.getId());
        if (mp != null && ACTIVE_RESERVATION_STATUSES.contains(mp.getStatus())) {
            mp.setStatus(ParticipantStatus.CANCELLED);
            return true;
        }
        return false;
    }

    @Override
    public boolean cancelJoinRequest(final Long matchId, final User user) {
        return rejectRequest(matchId, user);
    }

    @Override
    public int cancelPendingRequests(final Long matchId) {
        final Match match = em.find(Match.class, matchId, LockModeType.PESSIMISTIC_WRITE);
        if (match == null) {
            return 0;
        }

        List<MatchParticipant> toBeUpdated =
                match.getParticipants().stream()
                        .filter(mp -> mp.getStatus() == ParticipantStatus.PENDING_APPROVAL)
                        .toList();

        toBeUpdated.forEach(
                mp -> {
                    mp.setStatus(ParticipantStatus.CANCELLED);
                    mp.setScope(ParticipantScope.MATCH);
                    mp.setVersion(mp.getVersion() + 1);
                });

        return toBeUpdated.size();
    }

    @Override
    public List<Long> findPendingMatchIds(final User user) {
        return em.createQuery(
                        "SELECT mp.match.id FROM MatchParticipant mp"
                                + " WHERE mp.user.id = :userId AND mp.status = :status"
                                + " ORDER BY mp.joinedAt ASC",
                        Long.class)
                .setParameter("userId", user.getId())
                .setParameter("status", ParticipantStatus.PENDING_APPROVAL)
                .getResultList();
    }

    @Override
    public boolean inviteUser(final Long matchId, final User user) {
        return inviteUser(matchId, user, false);
    }

    @Override
    public boolean inviteUser(final Long matchId, final User user, final boolean seriesInvitation) {
        final Match match = em.find(Match.class, matchId);
        if (match == null) {
            return false;
        }
        final ParticipantScope scope =
                seriesInvitation ? ParticipantScope.SERIES : ParticipantScope.MATCH;
        return upsertParticipant(match, user, ParticipantStatus.INVITED, scope);
    }

    @Override
    public boolean hasInvitation(final Long matchId, final User user) {
        return em.createQuery(
                                "SELECT COUNT(mp) FROM MatchParticipant mp"
                                        + " WHERE mp.match.id = :matchId AND mp.user.id = :userId"
                                        + " AND mp.status = :status",
                                Long.class)
                        .setParameter("matchId", matchId)
                        .setParameter("userId", user.getId())
                        .setParameter("status", ParticipantStatus.INVITED)
                        .getSingleResult()
                > 0;
    }

    @Override
    public boolean isSeriesInvitation(final Long matchId, final User user) {
        return em.createQuery(
                                "SELECT COUNT(mp) FROM MatchParticipant mp"
                                        + " JOIN mp.match m"
                                        + " WHERE mp.match.id = :matchId AND mp.user.id = :userId"
                                        + " AND mp.status = :status AND m.series.id IS NOT NULL"
                                        + " AND mp.scope = :scope",
                                Long.class)
                        .setParameter("matchId", matchId)
                        .setParameter("userId", user.getId())
                        .setParameter("status", ParticipantStatus.INVITED)
                        .setParameter("scope", ParticipantScope.SERIES)
                        .getSingleResult()
                > 0;
    }

    @Override
    public boolean acceptInvite(final Long matchId, final User user) {
        final MatchParticipant mp = findParticipantInternal(matchId, user.getId());
        if (mp != null && mp.getStatus() == ParticipantStatus.INVITED) {
            mp.setStatus(ParticipantStatus.JOINED);
            mp.setScope(ParticipantScope.MATCH);
            return true;
        }
        return false;
    }

    @Override
    public int acceptSeriesInvite(final Long seriesId, final User user, final Instant startsAfter) {
        final int updated =
                em.createQuery(
                                "UPDATE MatchParticipant mp SET mp.status = :joinedStatus, mp.scope = :matchScope, mp.version = mp.version + 1"
                                        + " WHERE mp.user.id = :userId AND mp.status = :invitedStatus"
                                        + " AND mp.scope = :seriesScope"
                                        + " AND mp.match.id IN (SELECT m.id FROM Match m"
                                        + "                    WHERE m.series.id = :seriesId"
                                        + "                    AND m.status = :openStatus"
                                        + "                    AND m.startsAt > :startsAfter)")
                        .setParameter("joinedStatus", ParticipantStatus.JOINED)
                        .setParameter("invitedStatus", ParticipantStatus.INVITED)
                        .setParameter("userId", user.getId())
                        .setParameter("seriesId", seriesId)
                        .setParameter("openStatus", EventStatus.OPEN)
                        .setParameter("startsAfter", startsAfter)
                        .setParameter("matchScope", ParticipantScope.MATCH)
                        .setParameter("seriesScope", ParticipantScope.SERIES)
                        .executeUpdate();

        if (updated > 0) {
            em.createQuery(
                            "UPDATE MatchParticipant mp SET mp.status = :declinedStatus, mp.scope = :matchScope, mp.version = mp.version + 1"
                                    + " WHERE mp.user.id = :userId AND mp.status = :invitedStatus"
                                    + " AND mp.scope = :seriesScope"
                                    + " AND mp.match.id IN (SELECT m.id FROM Match m WHERE m.series.id = :seriesId)")
                    .setParameter("declinedStatus", ParticipantStatus.DECLINED_INVITE)
                    .setParameter("userId", user.getId())
                    .setParameter("invitedStatus", ParticipantStatus.INVITED)
                    .setParameter("seriesId", seriesId)
                    .setParameter("matchScope", ParticipantScope.MATCH)
                    .setParameter("seriesScope", ParticipantScope.SERIES)
                    .executeUpdate();
        }
        return updated;
    }

    @Override
    public boolean declineInvite(final Long matchId, final User user) {
        final MatchParticipant mp = findParticipantInternal(matchId, user.getId());
        if (mp != null && mp.getStatus() == ParticipantStatus.INVITED) {
            mp.setStatus(ParticipantStatus.DECLINED_INVITE);
            mp.setScope(ParticipantScope.MATCH);
            return true;
        }
        return false;
    }

    @Override
    public int declineSeriesInvite(final Long seriesId, final User user) {
        final int updated =
                em.createQuery(
                                "UPDATE MatchParticipant mp SET mp.status = :declinedStatus, mp.scope = :matchScope, mp.version = mp.version + 1"
                                        + " WHERE mp.user.id = :userId AND mp.status = :invitedStatus"
                                        + " AND mp.scope = :seriesScope"
                                        + " AND mp.match.id IN (SELECT m.id FROM Match m WHERE m.series.id = :seriesId)")
                        .setParameter("declinedStatus", ParticipantStatus.DECLINED_INVITE)
                        .setParameter("userId", user.getId())
                        .setParameter("invitedStatus", ParticipantStatus.INVITED)
                        .setParameter("seriesId", seriesId)
                        .setParameter("matchScope", ParticipantScope.MATCH)
                        .setParameter("seriesScope", ParticipantScope.SERIES)
                        .executeUpdate();
        return updated;
    }

    @Override
    public List<User> findInvitedUsers(final Long matchId) {
        return em.createQuery(
                        "SELECT new ar.edu.itba.paw.models.User(u.id, u.email, u.username)"
                                + " FROM MatchParticipant mp"
                                + " JOIN mp.user u"
                                + " WHERE mp.match.id = :matchId"
                                + " AND mp.status = :status"
                                + " ORDER BY mp.joinedAt ASC, u.username ASC",
                        User.class)
                .setParameter("matchId", matchId)
                .setParameter("status", ParticipantStatus.INVITED)
                .getResultList();
    }

    @Override
    public int cancelPendingInvitations(final Long matchId) {
        final int updated =
                em.createQuery(
                                "UPDATE MatchParticipant mp SET mp.status = :cancelledStatus, mp.scope = :matchScope, mp.version = mp.version + 1"
                                        + " WHERE mp.match.id = :matchId AND mp.status = :invitedStatus")
                        .setParameter("cancelledStatus", ParticipantStatus.CANCELLED)
                        .setParameter("matchId", matchId)
                        .setParameter("invitedStatus", ParticipantStatus.INVITED)
                        .setParameter("matchScope", ParticipantScope.MATCH)
                        .executeUpdate();
        return updated;
    }

    @Override
    public List<User> findDeclinedInvitees(final Long matchId) {
        final Match match = em.find(Match.class, matchId);
        if (match == null) {
            return Collections.emptyList();
        }

        return match.getParticipants().stream()
                .filter(mp -> mp.getStatus() == ParticipantStatus.DECLINED_INVITE)
                .map(MatchParticipant::getUser)
                .toList();
    }

    @Override
    public List<Long> findInvitedMatchIds(final User user) {
        return em.createQuery(
                        "SELECT mp.match.id FROM MatchParticipant mp"
                                + " WHERE mp.user.id = :userId AND mp.status = :status"
                                + " AND (mp.scope = :matchScope OR mp.match.startsAt = ("
                                + "   SELECT MIN(m2.startsAt) FROM MatchParticipant mp2"
                                + "   JOIN mp2.match m2"
                                + "   WHERE mp2.user.id = :userId AND mp2.status = :status AND mp2.scope = :seriesScope"
                                + "   AND m2.series.id = mp.match.series.id"
                                + " ))"
                                + " ORDER BY mp.match.startsAt ASC",
                        Long.class)
                .setParameter("userId", user.getId())
                .setParameter("status", ParticipantStatus.INVITED)
                .setParameter("matchScope", ParticipantScope.MATCH)
                .setParameter("seriesScope", ParticipantScope.SERIES)
                .getResultList();
    }

    private boolean upsertParticipant(
            final Match match,
            final User user,
            final ParticipantStatus status,
            final ParticipantScope scope) {
        final MatchParticipant existing = findParticipantInternal(match.getId(), user.getId());
        if (existing != null) {
            if (existing.getStatus() != ParticipantStatus.JOINED
                    && existing.getStatus() != ParticipantStatus.CHECKED_IN) {
                existing.setStatus(status);
                existing.setJoinedAt(Instant.now());
                existing.setScope(scope);
                return true;
            }
            return false;
        }

        final MatchParticipant participant =
                new MatchParticipant(match, user, status, Instant.now(), scope);
        em.persist(participant);

        return true;
    }

    private Map<Long, Long> countParticipantsBatch(
            final Collection<Long> matchIds, final List<ParticipantStatus> statuses) {
        if (matchIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final List<Object[]> results =
                em.createQuery(
                                "SELECT mp.match.id, COUNT(mp) FROM MatchParticipant mp"
                                        + " WHERE mp.match.id IN :matchIds AND mp.status IN :statuses"
                                        + " GROUP BY mp.match.id",
                                Object[].class)
                        .setParameter("matchIds", matchIds)
                        .setParameter("statuses", statuses)
                        .getResultList();

        final Map<Long, Long> counts = new HashMap<>();
        for (final Object[] row : results) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private MatchParticipant findParticipantInternal(final Long matchId, final Long userId) {
        final List<MatchParticipant> results =
                em.createQuery(
                                "FROM MatchParticipant mp WHERE mp.match.id = :matchId AND mp.user.id = :userId",
                                MatchParticipant.class)
                        .setParameter("matchId", matchId)
                        .setParameter("userId", userId)
                        .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    private long countParticipants(final Long matchId, final List<ParticipantStatus> statuses) {
        return em.createQuery(
                        "SELECT COUNT(mp) FROM MatchParticipant mp"
                                + " WHERE mp.match.id = :matchId AND mp.status IN :statuses",
                        Long.class)
                .setParameter("matchId", matchId)
                .setParameter("statuses", statuses)
                .getSingleResult();
    }

    private List<Match> findMatchesByIds(final Collection<Long> ids) {
        final List<Match> matches =
                em.createQuery(
                                "FROM Match m WHERE m.id IN :ids ORDER BY m.startsAt ASC",
                                Match.class)
                        .setParameter("ids", ids)
                        .getResultList();

        if (!matches.isEmpty()) {
            final List<Object[]> counts =
                    em.createQuery(
                                    "SELECT mp.match.id, COUNT(mp.id) FROM MatchParticipant mp WHERE mp.match.id IN :ids AND mp.status IN :activeStatuses GROUP BY mp.match.id",
                                    Object[].class)
                            .setParameter("ids", ids)
                            .setParameter("activeStatuses", ACTIVE_PARTICIPANT_STATUSES)
                            .getResultList();

            final Map<Long, Integer> joinedPlayersByMatchId = new HashMap<>();

            for (final Object[] row : counts) {
                joinedPlayersByMatchId.put((Long) row[0], ((Long) row[1]).intValue());
            }

            matches.forEach(
                    match ->
                            match.setJoinedPlayers(
                                    joinedPlayersByMatchId.getOrDefault(match.getId(), 0)));
        }

        return matches;
    }
}
