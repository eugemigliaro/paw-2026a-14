package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchParticipant;
import ar.edu.itba.paw.models.MatchSeries;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.ParticipantScope;
import ar.edu.itba.paw.models.types.ParticipantStatus;
import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class MatchParticipantJpaDaoTest {

    @Autowired private MatchParticipantDao matchParticipantDao;

    @PersistenceContext private EntityManager em;

    private Match match;
    private User host;
    private User player;
    private User other;

    @BeforeEach
    public void setUp() {
        host = createUser("host", "host@test.com");
        player = createUser("player", "player@test.com");
        other = createUser("other", "other@test.com");

        match =
                createMatch(
                        host,
                        "Address",
                        "Match",
                        Instant.now().plusSeconds(86400),
                        2,
                        EventJoinPolicy.DIRECT);
        flushAndClear();
    }

    @Test
    public void testCreateReservationIfSpaceInsertsParticipant() {
        final boolean inserted =
                matchParticipantDao.createReservationIfSpace(match.getId(), player);

        Assertions.assertTrue(inserted);
        flushAndClear();
        final MatchParticipant participant = findParticipant(match.getId(), player.getId());
        Assertions.assertNotNull(participant);
        Assertions.assertEquals(ParticipantStatus.JOINED, participant.getStatus());
    }

    @Test
    public void testCreateReservationIfSpaceRejectsFullMatch() {
        createParticipant(match, player, ParticipantStatus.JOINED);
        createParticipant(match, other, ParticipantStatus.JOINED);
        flushAndClear();

        final boolean inserted = matchParticipantDao.createReservationIfSpace(match.getId(), host);

        Assertions.assertFalse(inserted);
    }

    @Test
    public void testCreateReservationIfSpaceAllowsHostForInviteOnlyMatch() {
        match = em.find(Match.class, match.getId());
        match.setVisibility(EventVisibility.PRIVATE);
        match.setJoinPolicy(EventJoinPolicy.INVITE_ONLY);
        flushAndClear();
        final boolean inserted = matchParticipantDao.createReservationIfSpace(match.getId(), host);

        Assertions.assertTrue(inserted);
        Assertions.assertTrue(matchParticipantDao.hasActiveReservation(match.getId(), host));
    }

    @Test
    public void testCreateReservationIfSpaceRejectsNonHostForInviteOnlyMatch() {
        match = em.find(Match.class, match.getId());
        match.setVisibility(EventVisibility.PRIVATE);
        match.setJoinPolicy(EventJoinPolicy.INVITE_ONLY);
        flushAndClear();

        final boolean inserted =
                matchParticipantDao.createReservationIfSpace(match.getId(), player);

        Assertions.assertFalse(inserted);
        Assertions.assertFalse(matchParticipantDao.hasActiveReservation(match.getId(), player));
    }

    @Test
    public void testCreateReservationIfSpaceRestoresInactiveParticipantRow() {
        createParticipant(match, player, ParticipantStatus.CANCELLED);
        flushAndClear();

        final boolean inserted =
                matchParticipantDao.createReservationIfSpace(match.getId(), player);

        Assertions.assertTrue(inserted);
        flushAndClear();
        final MatchParticipant participant = findParticipant(match.getId(), player.getId());
        Assertions.assertEquals(ParticipantStatus.JOINED, participant.getStatus());
    }

    @Test
    public void testCreateReservationIfSpaceRejectsInactiveParticipantRowWhenFull() {
        createParticipant(match, host, ParticipantStatus.JOINED);
        createParticipant(match, other, ParticipantStatus.JOINED);
        createParticipant(match, player, ParticipantStatus.CANCELLED);
        flushAndClear();

        final boolean inserted =
                matchParticipantDao.createReservationIfSpace(match.getId(), player);

        Assertions.assertFalse(inserted);
        flushAndClear();
        final MatchParticipant participant = findParticipant(match.getId(), player.getId());
        Assertions.assertEquals(ParticipantStatus.CANCELLED, participant.getStatus());
    }

    @Test
    public void testCreateReservationIfSpaceRejectsDuplicateReservation() {
        createParticipant(match, player, ParticipantStatus.JOINED);
        flushAndClear();

        final boolean inserted =
                matchParticipantDao.createReservationIfSpace(match.getId(), player);

        Assertions.assertFalse(inserted);
    }

    @Test
    public void testHasActiveReservation() {
        createParticipant(match, player, ParticipantStatus.JOINED);
        flushAndClear();

        Assertions.assertTrue(matchParticipantDao.hasActiveReservation(match.getId(), player));
        Assertions.assertFalse(matchParticipantDao.hasActiveReservation(match.getId(), other));
    }

    @Test
    public void testFindPendingRequestsForHost() {
        match = em.find(Match.class, match.getId());
        match.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);
        createParticipant(match, player, ParticipantStatus.PENDING_APPROVAL);
        flushAndClear();

        final List<PendingJoinRequest> requests =
                matchParticipantDao.findPendingRequestsForHost(host);

        Assertions.assertEquals(1, requests.size());
        Assertions.assertEquals(match.getId(), requests.get(0).getMatch().getId());
        Assertions.assertEquals(player.getId(), requests.get(0).getUser().getId());
    }

    @Test
    public void testCreateSeriesJoinRequestIfSpaceInsertsSingleSeriesRequest() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        m1.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);
        final Match m2 = createMatchInSeries(series, host, now.plusSeconds(172800), 2);
        m2.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);
        flushAndClear();

        final boolean requested =
                matchParticipantDao.createSeriesJoinRequestIfSpace(m1.getId(), player);

        Assertions.assertTrue(requested);
        flushAndClear();
        final MatchParticipant p1 = findParticipant(m1.getId(), player.getId());
        Assertions.assertEquals(ParticipantStatus.PENDING_APPROVAL, p1.getStatus());
        Assertions.assertEquals(ParticipantScope.SERIES, p1.getScope());

        final List<PendingJoinRequest> hostRequests =
                matchParticipantDao.findPendingRequestsForHost(host);
        Assertions.assertEquals(1, hostRequests.size());
        Assertions.assertTrue(hostRequests.get(0).isSeriesRequest());
    }

    @Test
    public void testCreateSeriesJoinRequestIfSpaceRestoresCancelledRequest() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        m1.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);
        createParticipant(m1, player, ParticipantStatus.CANCELLED);
        flushAndClear();

        final boolean requested =
                matchParticipantDao.createSeriesJoinRequestIfSpace(m1.getId(), player);

        Assertions.assertTrue(requested);
        flushAndClear();
        final MatchParticipant p1 = findParticipant(m1.getId(), player.getId());
        Assertions.assertEquals(ParticipantStatus.PENDING_APPROVAL, p1.getStatus());
        Assertions.assertEquals(ParticipantScope.SERIES, p1.getScope());
    }

    @Test
    public void testApproveSeriesJoinRequestExpandsOnePendingRequestToFutureOccurrences() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m0 = createMatchInSeries(series, host, now.minusSeconds(86400), 0);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        final Match m2 = createMatchInSeries(series, host, now.plusSeconds(172800), 2);
        m0.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);
        m1.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);
        m2.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);
        createParticipant(m1, player, ParticipantStatus.PENDING_APPROVAL, ParticipantScope.SERIES);
        flushAndClear();

        final int approvedRows =
                matchParticipantDao.approveSeriesJoinRequest(
                        series.getId(), player, now.minusSeconds(60));

        Assertions.assertEquals(2, approvedRows);
        flushAndClear();
        Assertions.assertEquals(
                ParticipantStatus.JOINED, findParticipant(m1.getId(), player.getId()).getStatus());
        Assertions.assertEquals(
                ParticipantStatus.JOINED, findParticipant(m2.getId(), player.getId()).getStatus());
        Assertions.assertNull(findParticipant(m0.getId(), player.getId()));
    }

    @Test
    public void testHasPendingSeriesRequestIgnoresStaleAnchors() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.minusSeconds(86400), 1);
        final Match m2 = createMatchInSeries(series, host, now.plusSeconds(86400), 2);
        final Match m3 = createMatchInSeries(series, host, now.plusSeconds(172800), 3);
        m1.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);
        m2.setStatus(EventStatus.CANCELLED);
        m2.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);
        m3.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);
        createParticipant(m1, player, ParticipantStatus.PENDING_APPROVAL, ParticipantScope.SERIES);
        createParticipant(m2, player, ParticipantStatus.PENDING_APPROVAL, ParticipantScope.SERIES);
        flushAndClear();

        Assertions.assertFalse(matchParticipantDao.hasPendingSeriesRequest(series.getId(), player));

        createParticipant(m3, player, ParticipantStatus.PENDING_APPROVAL, ParticipantScope.SERIES);
        flushAndClear();
        Assertions.assertTrue(matchParticipantDao.hasPendingSeriesRequest(series.getId(), player));
    }

    @Test
    public void testApproveRequest() {
        createParticipant(match, player, ParticipantStatus.PENDING_APPROVAL);
        flushAndClear();

        final boolean approved = matchParticipantDao.approveRequest(match.getId(), player);

        Assertions.assertTrue(approved);
        flushAndClear();
        final MatchParticipant participant = findParticipant(match.getId(), player.getId());
        Assertions.assertEquals(ParticipantStatus.JOINED, participant.getStatus());
    }

    @Test
    public void testApproveAllPendingRequests() {
        createParticipant(match, player, ParticipantStatus.PENDING_APPROVAL);
        createParticipant(match, other, ParticipantStatus.PENDING_APPROVAL);
        flushAndClear();

        final int updated = matchParticipantDao.approveAllPendingRequests(match.getId());

        Assertions.assertEquals(2, updated);
        flushAndClear();
        Assertions.assertEquals(
                ParticipantStatus.JOINED,
                findParticipant(match.getId(), player.getId()).getStatus());
        Assertions.assertEquals(
                ParticipantStatus.JOINED,
                findParticipant(match.getId(), other.getId()).getStatus());
    }

    @Test
    public void testCancelPendingRequests() {
        createParticipant(match, player, ParticipantStatus.PENDING_APPROVAL);
        flushAndClear();

        final int cancelled = matchParticipantDao.cancelPendingRequests(match.getId());

        Assertions.assertEquals(1, cancelled);
        flushAndClear();
        Assertions.assertEquals(
                ParticipantStatus.CANCELLED,
                findParticipant(match.getId(), player.getId()).getStatus());
    }

    @Test
    public void testInviteUser() {
        final boolean invited = matchParticipantDao.inviteUser(match.getId(), player);

        Assertions.assertTrue(invited);
        flushAndClear();
        final MatchParticipant participant = findParticipant(match.getId(), player.getId());
        Assertions.assertEquals(ParticipantStatus.INVITED, participant.getStatus());
    }

    @Test
    public void testAcceptSeriesInviteJoinsOnlySeriesInvitationRows() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        final Match m2 = createMatchInSeries(series, host, now.plusSeconds(172800), 2);
        final Match m3 = createMatchInSeries(series, host, now.plusSeconds(259200), 3);

        createParticipant(m1, player, ParticipantStatus.INVITED, ParticipantScope.SERIES);
        createParticipant(m2, player, ParticipantStatus.INVITED, ParticipantScope.SERIES);
        createParticipant(m3, player, ParticipantStatus.INVITED, ParticipantScope.MATCH);
        flushAndClear();

        final int accepted =
                matchParticipantDao.acceptSeriesInvite(
                        series.getId(), player, now.minusSeconds(60));

        Assertions.assertEquals(2, accepted);
        flushAndClear();
        Assertions.assertEquals(
                ParticipantStatus.JOINED, findParticipant(m1.getId(), player.getId()).getStatus());
        Assertions.assertEquals(
                ParticipantStatus.JOINED, findParticipant(m2.getId(), player.getId()).getStatus());
        Assertions.assertEquals(
                ParticipantStatus.INVITED, findParticipant(m3.getId(), player.getId()).getStatus());
    }

    @Test
    public void testDeclineSeriesInviteDeclinesOnlySeriesInvitationRows() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        final Match m2 = createMatchInSeries(series, host, now.plusSeconds(172800), 2);

        createParticipant(m1, player, ParticipantStatus.INVITED, ParticipantScope.SERIES);
        createParticipant(m2, player, ParticipantStatus.INVITED, ParticipantScope.MATCH);
        flushAndClear();

        final int declined = matchParticipantDao.declineSeriesInvite(series.getId(), player);

        Assertions.assertEquals(1, declined);
        flushAndClear();
        Assertions.assertEquals(
                ParticipantStatus.DECLINED_INVITE,
                findParticipant(m1.getId(), player.getId()).getStatus());
        Assertions.assertEquals(
                ParticipantStatus.INVITED, findParticipant(m2.getId(), player.getId()).getStatus());
    }

    @Test
    public void testFindInvitedMatchIdsCollapsesSeriesInvitations() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        final Match m2 = createMatchInSeries(series, host, now.plusSeconds(172800), 2);
        final Match m3 = createMatchInSeries(series, host, now.plusSeconds(259200), 3);

        createParticipant(m1, player, ParticipantStatus.INVITED, ParticipantScope.SERIES);
        createParticipant(m2, player, ParticipantStatus.INVITED, ParticipantScope.SERIES);
        createParticipant(m3, player, ParticipantStatus.INVITED, ParticipantScope.MATCH);
        flushAndClear();

        final List<Long> invitedIds = matchParticipantDao.findInvitedMatchIds(player);

        Assertions.assertEquals(2, invitedIds.size());
        Assertions.assertTrue(invitedIds.containsAll(List.of(m1.getId(), m3.getId())));
    }

    @Test
    public void testCancelPendingInvitations() {
        createParticipant(match, player, ParticipantStatus.INVITED);
        flushAndClear();

        final int cancelled = matchParticipantDao.cancelPendingInvitations(match.getId());

        Assertions.assertEquals(1, cancelled);
        flushAndClear();
        Assertions.assertEquals(
                ParticipantStatus.CANCELLED,
                findParticipant(match.getId(), player.getId()).getStatus());
    }

    @Test
    public void testCancelFutureSeriesReservations() {
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, Instant.now().plusSeconds(10000), 1);
        final Match m2 = createMatchInSeries(series, host, Instant.now().plusSeconds(20000), 2);
        createParticipant(m1, player, ParticipantStatus.JOINED);
        createParticipant(m2, player, ParticipantStatus.JOINED);
        flushAndClear();

        final int cancelled =
                matchParticipantDao.cancelFutureSeriesReservations(
                        series.getId(), player, Instant.now());

        Assertions.assertEquals(2, cancelled);
        flushAndClear();
        Assertions.assertEquals(
                ParticipantStatus.CANCELLED,
                findParticipant(m1.getId(), player.getId()).getStatus());
        Assertions.assertEquals(
                ParticipantStatus.CANCELLED,
                findParticipant(m2.getId(), player.getId()).getStatus());
    }

    @Test
    public void testCreateSeriesReservationsIfSpaceInsertsFutureEligibleOccurrences() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        final Match m2 = createMatchInSeries(series, host, now.plusSeconds(172800), 2);
        final Match m3 = createMatchInSeries(series, host, now.minusSeconds(86400), 3);
        final Match m4 = createMatchInSeries(series, host, now.plusSeconds(259200), 4);
        m4.setStatus(EventStatus.CANCELLED);
        createParticipant(m2, other, ParticipantStatus.JOINED);
        flushAndClear();

        final int insertedRows =
                matchParticipantDao.createSeriesReservationsIfSpace(series.getId(), player, now);

        Assertions.assertEquals(2, insertedRows);
        flushAndClear();
        Assertions.assertEquals(
                ParticipantStatus.JOINED, findParticipant(m1.getId(), player.getId()).getStatus());
        Assertions.assertEquals(
                ParticipantStatus.JOINED, findParticipant(m2.getId(), player.getId()).getStatus());
        Assertions.assertNull(findParticipant(m3.getId(), player.getId()));
        Assertions.assertNull(findParticipant(m4.getId(), player.getId()));
    }

    @Test
    public void testCreateSeriesReservationsIfSpaceAllowsHostForApprovalRequiredOccurrences() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        m1.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);
        final Match m2 = createMatchInSeries(series, host, now.plusSeconds(172800), 2);
        m2.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);
        flushAndClear();

        final int insertedRows =
                matchParticipantDao.createSeriesReservationsIfSpace(series.getId(), host, now);

        Assertions.assertEquals(2, insertedRows);
        Assertions.assertTrue(matchParticipantDao.hasActiveReservation(m1.getId(), host));
        Assertions.assertTrue(matchParticipantDao.hasActiveReservation(m2.getId(), host));
    }

    @Test
    public void testCreateSeriesReservationsIfSpaceRejectsNonHostForApprovalRequiredOccurrences() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        final Match m2 = createMatchInSeries(series, host, now.plusSeconds(172800), 2);
        m1.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);
        m2.setJoinPolicy(EventJoinPolicy.APPROVAL_REQUIRED);
        flushAndClear();

        final int insertedRows =
                matchParticipantDao.createSeriesReservationsIfSpace(series.getId(), player, now);

        Assertions.assertEquals(0, insertedRows);
        Assertions.assertFalse(matchParticipantDao.hasActiveReservation(m1.getId(), player));
        Assertions.assertFalse(matchParticipantDao.hasActiveReservation(m2.getId(), player));
    }

    @Test
    public void testCreateSeriesReservationsIfSpaceRestoresCancelledReservation() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        createParticipant(m1, player, ParticipantStatus.CANCELLED);
        flushAndClear();

        final int insertedRows =
                matchParticipantDao.createSeriesReservationsIfSpace(series.getId(), player, now);

        Assertions.assertEquals(1, insertedRows);
        flushAndClear();
        Assertions.assertEquals(
                ParticipantStatus.JOINED, findParticipant(m1.getId(), player.getId()).getStatus());
    }

    @Test
    public void testCreateSeriesReservationsIfSpaceRestoresNonActiveParticipantRows() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        final Match m2 = createMatchInSeries(series, host, now.plusSeconds(172800), 2);
        final Match m3 = createMatchInSeries(series, host, now.plusSeconds(259200), 3);
        createParticipant(m1, player, ParticipantStatus.PENDING_APPROVAL);
        createParticipant(m2, player, ParticipantStatus.INVITED);
        createParticipant(m3, player, ParticipantStatus.DECLINED_INVITE);
        flushAndClear();

        final int insertedRows =
                matchParticipantDao.createSeriesReservationsIfSpace(series.getId(), player, now);

        Assertions.assertEquals(3, insertedRows);
        flushAndClear();
        Assertions.assertEquals(
                ParticipantStatus.JOINED, findParticipant(m1.getId(), player.getId()).getStatus());
        Assertions.assertEquals(
                ParticipantStatus.JOINED, findParticipant(m2.getId(), player.getId()).getStatus());
        Assertions.assertEquals(
                ParticipantStatus.JOINED, findParticipant(m3.getId(), player.getId()).getStatus());
    }

    @Test
    public void testCreateSeriesReservationsIfSpaceRejectsExistingActiveReservations() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        final Match m2 = createMatchInSeries(series, host, now.plusSeconds(172800), 2);
        createParticipant(m1, player, ParticipantStatus.JOINED);
        createParticipant(m2, player, ParticipantStatus.CHECKED_IN);
        flushAndClear();

        final int insertedRows =
                matchParticipantDao.createSeriesReservationsIfSpace(series.getId(), player, now);

        Assertions.assertEquals(0, insertedRows);
        flushAndClear();
        Assertions.assertEquals(
                ParticipantStatus.JOINED, findParticipant(m1.getId(), player.getId()).getStatus());
        Assertions.assertEquals(
                ParticipantStatus.CHECKED_IN,
                findParticipant(m2.getId(), player.getId()).getStatus());
    }

    @Test
    public void testCreateSeriesReservationsIfSpaceRejectsFullOccurrences() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        m1.setMaxPlayers(1);
        createParticipant(m1, other, ParticipantStatus.JOINED);
        flushAndClear();

        final int insertedRows =
                matchParticipantDao.createSeriesReservationsIfSpace(series.getId(), player, now);

        Assertions.assertEquals(0, insertedRows);
        Assertions.assertFalse(matchParticipantDao.hasActiveReservation(m1.getId(), player));
    }

    @Test
    public void testCreateReservationIfSpaceRestoresOccurrenceAfterSeriesCancellation() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        final Match m2 = createMatchInSeries(series, host, now.plusSeconds(172800), 2);
        flushAndClear();

        matchParticipantDao.createSeriesReservationsIfSpace(series.getId(), player, now);
        matchParticipantDao.cancelFutureSeriesReservations(series.getId(), player, now);
        flushAndClear();

        final boolean inserted = matchParticipantDao.createReservationIfSpace(m1.getId(), player);

        Assertions.assertTrue(inserted);
        flushAndClear();
        Assertions.assertEquals(
                ParticipantStatus.JOINED, findParticipant(m1.getId(), player.getId()).getStatus());
        Assertions.assertEquals(
                ParticipantStatus.CANCELLED,
                findParticipant(m2.getId(), player.getId()).getStatus());
    }

    @Test
    public void testFindPendingFutureRequestMatchIdsForSeries() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        final Match m2 = createMatchInSeries(series, host, now.plusSeconds(172800), 2);
        final Match m3 = createMatchInSeries(series, host, now.minusSeconds(86400), 3);
        createParticipant(m1, player, ParticipantStatus.PENDING_APPROVAL);
        createParticipant(m2, player, ParticipantStatus.PENDING_APPROVAL);
        createParticipant(m3, player, ParticipantStatus.PENDING_APPROVAL);
        flushAndClear();

        final List<Long> matchIds =
                matchParticipantDao.findPendingFutureRequestMatchIdsForSeries(
                        series.getId(), player, now);

        Assertions.assertEquals(2, matchIds.size());
        Assertions.assertTrue(matchIds.containsAll(List.of(m1.getId(), m2.getId())));
    }

    @Test
    public void testFindActiveFutureReservationMatchIdsForSeries() {
        final Instant now = Instant.now();
        final MatchSeries series = createSeries(host);
        final Match m1 = createMatchInSeries(series, host, now.plusSeconds(86400), 1);
        final Match m2 = createMatchInSeries(series, host, now.plusSeconds(172800), 2);
        final Match m3 = createMatchInSeries(series, host, now.minusSeconds(86400), 3);
        createParticipant(m1, player, ParticipantStatus.JOINED);
        createParticipant(m2, player, ParticipantStatus.CHECKED_IN);
        createParticipant(m3, player, ParticipantStatus.JOINED);
        flushAndClear();

        final List<Long> matchIds =
                matchParticipantDao.findActiveFutureReservationMatchIdsForSeries(
                        series.getId(), player, now);

        Assertions.assertEquals(2, matchIds.size());
        Assertions.assertTrue(matchIds.containsAll(List.of(m1.getId(), m2.getId())));
    }

    @Test
    public void testAcceptInvite() {
        createParticipant(match, player, ParticipantStatus.INVITED);
        flushAndClear();

        final boolean accepted = matchParticipantDao.acceptInvite(match.getId(), player);

        Assertions.assertTrue(accepted);
        flushAndClear();
        Assertions.assertEquals(
                ParticipantStatus.JOINED,
                findParticipant(match.getId(), player.getId()).getStatus());
    }

    @Test
    public void testRemoveParticipantCancelsActiveReservation() {
        createParticipant(match, player, ParticipantStatus.JOINED);
        flushAndClear();

        final boolean cancelled = matchParticipantDao.removeParticipant(match.getId(), player);

        Assertions.assertTrue(cancelled);
        Assertions.assertFalse(matchParticipantDao.hasActiveReservation(match.getId(), player));
        flushAndClear();
        final MatchParticipant participant = findParticipant(match.getId(), player.getId());
        Assertions.assertEquals(ParticipantStatus.CANCELLED, participant.getStatus());
    }

    private User createUser(String username, String email) {
        User user = new User(null, email, username, "Name", "Last", "123", null, "en");
        em.persist(user);
        return user;
    }

    private Match createMatch(
            User host,
            String address,
            String title,
            Instant startsAt,
            int maxPlayers,
            EventJoinPolicy joinPolicy) {
        Match match =
                new Match(
                        null,
                        Sport.FOOTBALL,
                        host,
                        address,
                        null,
                        null,
                        title,
                        "Desc",
                        startsAt,
                        startsAt.plusSeconds(3600),
                        maxPlayers,
                        BigDecimal.ZERO,
                        EventVisibility.PUBLIC,
                        joinPolicy,
                        EventStatus.OPEN,
                        0,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null);
        match.setHost(host);
        match.setCreatedAt(Instant.now());
        match.setUpdatedAt(Instant.now());
        em.persist(match);
        return match;
    }

    private MatchSeries createSeries(User host) {
        MatchSeries series =
                new MatchSeries(
                        null,
                        host,
                        "weekly",
                        Instant.now(),
                        Instant.now().plusSeconds(86400 * 30),
                        "UTC",
                        null,
                        4,
                        Instant.now(),
                        Instant.now());
        em.persist(series);
        return series;
    }

    private Match createMatchInSeries(MatchSeries series, User host, Instant startsAt, int index) {
        Match m = createMatch(host, "Address", "Title", startsAt, 5, EventJoinPolicy.DIRECT);
        m.setSeries(series);
        m.setSeriesOccurrenceIndex(index);
        return em.merge(m);
    }

    private void createParticipant(Match m, User user, ParticipantStatus status) {
        createParticipant(m, user, status, ParticipantScope.MATCH);
    }

    private void createParticipant(
            Match m, User user, ParticipantStatus status, ParticipantScope scope) {
        MatchParticipant participant =
                new MatchParticipant(
                        em.find(Match.class, m.getId()), user, status, Instant.now(), scope);
        em.persist(participant);
    }

    private MatchParticipant findParticipant(Long matchId, Long userId) {
        List<MatchParticipant> results =
                em.createQuery(
                                "FROM MatchParticipant mp WHERE mp.match.id = :matchId AND mp.user.id = :userId",
                                MatchParticipant.class)
                        .setParameter("matchId", matchId)
                        .setParameter("userId", userId)
                        .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
