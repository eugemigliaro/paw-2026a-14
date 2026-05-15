package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchParticipant;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
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

    @Override
    public boolean hasActiveReservation(final Long matchId, final Long userId) {
        final TypedQuery<Long> query =
                em.createQuery(
                        "SELECT COUNT(mp) FROM MatchParticipant mp"
                                + " WHERE mp.match.id = :matchId AND mp.user.id = :userId"
                                + " AND mp.status IN :statuses",
                        Long.class);
        query.setParameter("matchId", matchId);
        query.setParameter("userId", userId);
        query.setParameter("statuses", ACTIVE_RESERVATION_STATUSES);
        return query.getSingleResult() > 0;
    }

    @Override
    public List<Long> findActiveFutureReservationMatchIdsForSeries(
            final Long seriesId, final Long userId, final Instant startsAfter) {
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
        query.setParameter("userId", userId);
        query.setParameter("statuses", ACTIVE_RESERVATION_STATUSES);
        return query.getResultList();
    }

    @Override
    public List<Long> findPendingFutureRequestMatchIdsForSeries(
            final Long seriesId, final Long userId, final Instant startsAfter) {
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
        query.setParameter("userId", userId);
        query.setParameter("status", ParticipantStatus.PENDING_APPROVAL);
        return query.getResultList();
    }

    @Override
    public boolean createReservationIfSpace(final Long matchId, final Long userId) {
        final Match match = em.find(Match.class, matchId, LockModeType.PESSIMISTIC_WRITE);
        if (match == null) {
            return false;
        }

        if (match.getStatus() != EventStatus.OPEN || !match.getStartsAt().isAfter(Instant.now())) {
            return false;
        }

        if (match.getVisibility() != EventVisibility.PUBLIC
                || match.getJoinPolicy() != EventJoinPolicy.DIRECT) {
            if (!match.getHostUserId().equals(userId)) {
                return false;
            }
        }

        final long joinedCount = countParticipants(matchId, ACTIVE_RESERVATION_STATUSES);
        if (joinedCount >= match.getMaxPlayers()) {
            return false;
        }

        return upsertParticipant(match, userId, ParticipantStatus.JOINED, ParticipantScope.MATCH);
    }

    @Override
    public int createSeriesReservationsIfSpace(
            final Long seriesId, final Long userId, final Instant startsAfter) {
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
        query.setParameter("userId", userId);

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
                        match, userId, ParticipantStatus.JOINED, ParticipantScope.SERIES)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public int cancelFutureSeriesReservations(
            final Long seriesId, final Long userId, final Instant startsAfter) {
        return em.createQuery(
                        "UPDATE MatchParticipant mp SET mp.status = :cancelledStatus, mp.version = mp.version + 1"
                                + " WHERE mp.user.id = :userId"
                                + " AND mp.status IN :activeStatuses"
                                + " AND mp.match.id IN (SELECT m.id FROM Match m"
                                + "                    WHERE m.series.id = :seriesId"
                                + "                    AND m.startsAt > :startsAfter)")
                .setParameter("cancelledStatus", ParticipantStatus.CANCELLED)
                .setParameter("userId", userId)
                .setParameter("activeStatuses", ACTIVE_RESERVATION_STATUSES)
                .setParameter("seriesId", seriesId)
                .setParameter("startsAfter", startsAfter)
                .executeUpdate();
    }

    @Override
    public List<User> findConfirmedParticipants(final Long matchId) {
        final TypedQuery<User> query =
                em.createQuery(
                        "SELECT new ar.edu.itba.paw.models.User(u.id, u.email, u.username, u.name, u.lastName, u.phone, u.profileImageId, u.preferredLanguage)"
                                + " FROM MatchParticipant mp"
                                + " JOIN mp.user u"
                                + " WHERE mp.match.id = :matchId"
                                + " AND mp.status IN :statuses"
                                + " ORDER BY mp.joinedAt ASC, u.username ASC",
                        User.class);
        query.setParameter("matchId", matchId);
        query.setParameter("statuses", ACTIVE_RESERVATION_STATUSES);
        return query.getResultList();
    }

    @Override
    public boolean hasPendingRequest(final Long matchId, final Long userId) {
        final TypedQuery<Long> query =
                em.createQuery(
                        "SELECT COUNT(mp) FROM MatchParticipant mp"
                                + " WHERE mp.match.id = :matchId AND mp.user.id = :userId"
                                + " AND mp.status = :status",
                        Long.class);
        query.setParameter("matchId", matchId);
        query.setParameter("userId", userId);
        query.setParameter("status", ParticipantStatus.PENDING_APPROVAL);
        return query.getSingleResult() > 0;
    }

    @Override
    public boolean createJoinRequest(final Long matchId, final Long userId) {
        final Match match = em.find(Match.class, matchId);
        if (match == null) {
            return false;
        }
        return upsertParticipant(
                match, userId, ParticipantStatus.PENDING_APPROVAL, ParticipantScope.MATCH);
    }

    @Override
    public boolean createSeriesJoinRequestIfSpace(final Long matchId, final Long userId) {
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
                match, userId, ParticipantStatus.PENDING_APPROVAL, ParticipantScope.SERIES);
    }

    @Override
    public List<User> findPendingRequests(final Long matchId) {
        final TypedQuery<User> query =
                em.createQuery(
                        "SELECT new ar.edu.itba.paw.models.User(u.id, u.email, u.username)"
                                + " FROM MatchParticipant mp"
                                + " JOIN mp.user u"
                                + " WHERE mp.match.id = :matchId"
                                + " AND mp.status = :status"
                                + " ORDER BY mp.joinedAt ASC, u.username ASC",
                        User.class);
        query.setParameter("matchId", matchId);
        query.setParameter("status", ParticipantStatus.PENDING_APPROVAL);
        return query.getResultList();
    }

    @Override
    public int countPendingRequests(final Long matchId) {
        return em.createQuery(
                        "SELECT COUNT(mp) FROM MatchParticipant mp"
                                + " WHERE mp.match.id = :matchId AND mp.status = :status",
                        Long.class)
                .setParameter("matchId", matchId)
                .setParameter("status", ParticipantStatus.PENDING_APPROVAL)
                .getSingleResult()
                .intValue();
    }

    @Override
    public List<PendingJoinRequest> findPendingRequestsForHost(final Long hostUserId) {
        final List<PendingJoinRequestProjection> projections =
                em.createQuery(
                                "SELECT new ar.edu.itba.paw.persistence.PendingJoinRequestProjection("
                                        + "mp.match.id, u.id, u.email, u.username, u.name, u.lastName, u.phone, u.profileImageId, u.preferredLanguage, mp.scope)"
                                        + " FROM MatchParticipant mp"
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
                                PendingJoinRequestProjection.class)
                        .setParameter("hostUserId", hostUserId)
                        .setParameter("joinPolicy", EventJoinPolicy.APPROVAL_REQUIRED)
                        .setParameter("status", ParticipantStatus.PENDING_APPROVAL)
                        .setParameter("matchScope", ParticipantScope.MATCH)
                        .setParameter("seriesScope", ParticipantScope.SERIES)
                        .getResultList();

        if (projections.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Long> matchIds =
                projections.stream().map(PendingJoinRequestProjection::matchId).distinct().toList();

        final List<Match> matches = findMatchesByIds(matchIds);
        final Map<Long, Match> matchMap =
                matches.stream().collect(Collectors.toMap(Match::getId, m -> m));

        return projections.stream().map(r -> r.toModel(matchMap.get(r.matchId()))).toList();
    }

    @Override
    public boolean approveRequest(final Long matchId, final Long userId) {
        final MatchParticipant mp = findParticipantInternal(matchId, userId);
        if (mp != null && mp.getStatus() == ParticipantStatus.PENDING_APPROVAL) {
            mp.setStatus(ParticipantStatus.JOINED);
            mp.setScope(ParticipantScope.MATCH);
            return true;
        }
        return false;
    }

    @Override
    public int approveAllPendingRequests(final Long matchId) {
        final int updated =
                em.createQuery(
                                "UPDATE MatchParticipant mp SET mp.status = :joinedStatus, mp.scope = :matchScope, mp.joinedAt = :now, mp.version = mp.version + 1"
                                        + " WHERE mp.match.id = :matchId AND mp.status = :pendingStatus")
                        .setParameter("joinedStatus", ParticipantStatus.JOINED)
                        .setParameter("now", Instant.now())
                        .setParameter("matchId", matchId)
                        .setParameter("pendingStatus", ParticipantStatus.PENDING_APPROVAL)
                        .setParameter("matchScope", ParticipantScope.MATCH)
                        .executeUpdate();
        if (updated > 0) {
            flushAndClear();
        }
        return updated;
    }

    @Override
    public int approveSeriesJoinRequest(
            final Long seriesId, final Long userId, final Instant startsAfter) {
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
                        match, userId, ParticipantStatus.JOINED, ParticipantScope.MATCH)) {
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
                    .setParameter("userId", userId)
                    .setParameter("pendingStatus", ParticipantStatus.PENDING_APPROVAL)
                    .setParameter("seriesId", seriesId)
                    .setParameter("matchScope", ParticipantScope.MATCH)
                    .setParameter("seriesScope", ParticipantScope.SERIES)
                    .executeUpdate();
            flushAndClear();
        }

        return approvedCount;
    }

    @Override
    public boolean isSeriesJoinRequest(final Long matchId, final Long userId) {
        return em.createQuery(
                                "SELECT COUNT(mp) FROM MatchParticipant mp"
                                        + " WHERE mp.match.id = :matchId AND mp.user.id = :userId"
                                        + " AND mp.status = :status AND mp.scope = :scope",
                                Long.class)
                        .setParameter("matchId", matchId)
                        .setParameter("userId", userId)
                        .setParameter("status", ParticipantStatus.PENDING_APPROVAL)
                        .setParameter("scope", ParticipantScope.SERIES)
                        .getSingleResult()
                > 0;
    }

    @Override
    public boolean hasPendingSeriesRequest(final Long seriesId, final Long userId) {
        return em.createQuery(
                                "SELECT COUNT(mp) FROM MatchParticipant mp"
                                        + " JOIN mp.match m"
                                        + " WHERE m.series.id = :seriesId AND mp.user.id = :userId"
                                        + " AND mp.status = :status AND mp.scope = :scope"
                                        + " AND m.visibility = :publicVis AND m.joinPolicy = :approvalJoin"
                                        + " AND m.status = :matchStatus AND m.startsAt > :now",
                                Long.class)
                        .setParameter("seriesId", seriesId)
                        .setParameter("userId", userId)
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
    public boolean rejectRequest(final Long matchId, final Long userId) {
        final MatchParticipant mp = findParticipantInternal(matchId, userId);
        if (mp != null && mp.getStatus() == ParticipantStatus.PENDING_APPROVAL) {
            mp.setStatus(ParticipantStatus.CANCELLED);
            mp.setScope(ParticipantScope.MATCH);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeParticipant(final Long matchId, final Long userId) {
        final MatchParticipant mp = findParticipantInternal(matchId, userId);
        if (mp != null && ACTIVE_RESERVATION_STATUSES.contains(mp.getStatus())) {
            mp.setStatus(ParticipantStatus.CANCELLED);
            return true;
        }
        return false;
    }

    @Override
    public boolean cancelJoinRequest(final Long matchId, final Long userId) {
        return rejectRequest(matchId, userId);
    }

    @Override
    public int cancelPendingRequests(final Long matchId) {
        final int updated =
                em.createQuery(
                                "UPDATE MatchParticipant mp SET mp.status = :cancelledStatus, mp.scope = :matchScope, mp.version = mp.version + 1"
                                        + " WHERE mp.match.id = :matchId AND mp.status = :pendingStatus")
                        .setParameter("cancelledStatus", ParticipantStatus.CANCELLED)
                        .setParameter("matchId", matchId)
                        .setParameter("pendingStatus", ParticipantStatus.PENDING_APPROVAL)
                        .setParameter("matchScope", ParticipantScope.MATCH)
                        .executeUpdate();
        if (updated > 0) {
            flushAndClear();
        }
        return updated;
    }

    @Override
    public List<Long> findPendingMatchIds(final Long userId) {
        return em.createQuery(
                        "SELECT mp.match.id FROM MatchParticipant mp"
                                + " WHERE mp.user.id = :userId AND mp.status = :status"
                                + " ORDER BY mp.joinedAt ASC",
                        Long.class)
                .setParameter("userId", userId)
                .setParameter("status", ParticipantStatus.PENDING_APPROVAL)
                .getResultList();
    }

    @Override
    public boolean inviteUser(final Long matchId, final Long userId) {
        return inviteUser(matchId, userId, false);
    }

    @Override
    public boolean inviteUser(
            final Long matchId, final Long userId, final boolean seriesInvitation) {
        final Match match = em.find(Match.class, matchId);
        if (match == null) {
            return false;
        }
        final ParticipantScope scope =
                seriesInvitation ? ParticipantScope.SERIES : ParticipantScope.MATCH;
        return upsertParticipant(match, userId, ParticipantStatus.INVITED, scope);
    }

    @Override
    public boolean hasInvitation(final Long matchId, final Long userId) {
        return em.createQuery(
                                "SELECT COUNT(mp) FROM MatchParticipant mp"
                                        + " WHERE mp.match.id = :matchId AND mp.user.id = :userId"
                                        + " AND mp.status = :status",
                                Long.class)
                        .setParameter("matchId", matchId)
                        .setParameter("userId", userId)
                        .setParameter("status", ParticipantStatus.INVITED)
                        .getSingleResult()
                > 0;
    }

    @Override
    public boolean isSeriesInvitation(final Long matchId, final Long userId) {
        return em.createQuery(
                                "SELECT COUNT(mp) FROM MatchParticipant mp"
                                        + " JOIN mp.match m"
                                        + " WHERE mp.match.id = :matchId AND mp.user.id = :userId"
                                        + " AND mp.status = :status AND m.series.id IS NOT NULL"
                                        + " AND mp.scope = :scope",
                                Long.class)
                        .setParameter("matchId", matchId)
                        .setParameter("userId", userId)
                        .setParameter("status", ParticipantStatus.INVITED)
                        .setParameter("scope", ParticipantScope.SERIES)
                        .getSingleResult()
                > 0;
    }

    @Override
    public boolean acceptInvite(final Long matchId, final Long userId) {
        final MatchParticipant mp = findParticipantInternal(matchId, userId);
        if (mp != null && mp.getStatus() == ParticipantStatus.INVITED) {
            mp.setStatus(ParticipantStatus.JOINED);
            mp.setScope(ParticipantScope.MATCH);
            return true;
        }
        return false;
    }

    @Override
    public int acceptSeriesInvite(
            final Long seriesId, final Long userId, final Instant startsAfter) {
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
                        .setParameter("userId", userId)
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
                    .setParameter("userId", userId)
                    .setParameter("invitedStatus", ParticipantStatus.INVITED)
                    .setParameter("seriesId", seriesId)
                    .setParameter("matchScope", ParticipantScope.MATCH)
                    .setParameter("seriesScope", ParticipantScope.SERIES)
                    .executeUpdate();
            flushAndClear();
        }
        return updated;
    }

    @Override
    public boolean declineInvite(final Long matchId, final Long userId) {
        final MatchParticipant mp = findParticipantInternal(matchId, userId);
        if (mp != null && mp.getStatus() == ParticipantStatus.INVITED) {
            mp.setStatus(ParticipantStatus.DECLINED_INVITE);
            mp.setScope(ParticipantScope.MATCH);
            return true;
        }
        return false;
    }

    @Override
    public int declineSeriesInvite(final Long seriesId, final Long userId) {
        final int updated =
                em.createQuery(
                                "UPDATE MatchParticipant mp SET mp.status = :declinedStatus, mp.scope = :matchScope, mp.version = mp.version + 1"
                                        + " WHERE mp.user.id = :userId AND mp.status = :invitedStatus"
                                        + " AND mp.scope = :seriesScope"
                                        + " AND mp.match.id IN (SELECT m.id FROM Match m WHERE m.series.id = :seriesId)")
                        .setParameter("declinedStatus", ParticipantStatus.DECLINED_INVITE)
                        .setParameter("userId", userId)
                        .setParameter("invitedStatus", ParticipantStatus.INVITED)
                        .setParameter("seriesId", seriesId)
                        .setParameter("matchScope", ParticipantScope.MATCH)
                        .setParameter("seriesScope", ParticipantScope.SERIES)
                        .executeUpdate();
        if (updated > 0) {
            flushAndClear();
        }
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
        if (updated > 0) {
            flushAndClear();
        }
        return updated;
    }

    @Override
    public List<User> findDeclinedInvitees(final Long matchId) {
        return em.createQuery(
                        "SELECT new ar.edu.itba.paw.models.User(u.id, u.email, u.username)"
                                + " FROM MatchParticipant mp"
                                + " JOIN mp.user u"
                                + " WHERE mp.match.id = :matchId"
                                + " AND mp.status = :status"
                                + " ORDER BY mp.joinedAt ASC, u.username ASC",
                        User.class)
                .setParameter("matchId", matchId)
                .setParameter("status", ParticipantStatus.DECLINED_INVITE)
                .getResultList();
    }

    @Override
    public List<Long> findInvitedMatchIds(final Long userId) {
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
                .setParameter("userId", userId)
                .setParameter("status", ParticipantStatus.INVITED)
                .setParameter("matchScope", ParticipantScope.MATCH)
                .setParameter("seriesScope", ParticipantScope.SERIES)
                .getResultList();
    }

    private boolean upsertParticipant(
            final Match match,
            final Long userId,
            final ParticipantStatus status,
            final ParticipantScope scope) {
        final MatchParticipant existing = findParticipantInternal(match.getId(), userId);
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

        final UserAccount user = em.getReference(UserAccount.class, userId);
        final MatchParticipant participant =
                new MatchParticipant(match, user, status, Instant.now(), scope);
        em.persist(participant);
        return true;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
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
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        final TypedQuery<MatchProjection> query =
                em.createQuery(projectedSelect() + " WHERE m.id IN :ids", MatchProjection.class);
        query.setParameter("ids", ids);
        query.setParameter(
                "activeStatuses",
                List.of(
                        ParticipantStatus.JOINED,
                        ParticipantStatus.CHECKED_IN,
                        ParticipantStatus.INVITED));
        final Instant now = Instant.now();
        return query.getResultList().stream().map(p -> p.toMatch(now)).collect(Collectors.toList());
    }

    private String projectedSelect() {
        return "SELECT NEW ar.edu.itba.paw.persistence.MatchProjection("
                + "m.id, m.sport, m.host.id, m.address, m.latitude, m.longitude, "
                + "m.title, m.description, m.startsAt, m.endsAt, m.maxPlayers, "
                + "m.pricePerPlayer, m.visibility, m.joinPolicy, m.status, "
                + "(SELECT COUNT(mp.id) FROM MatchParticipant mp WHERE mp.match = m AND mp.status IN :activeStatuses)"
                + ", m.bannerImageId, m.series.id, m.seriesOccurrenceIndex, "
                + "m.deleted, m.deletedAt, m.deletedByUserId, m.deleteReason)"
                + " FROM Match m";
    }
}
