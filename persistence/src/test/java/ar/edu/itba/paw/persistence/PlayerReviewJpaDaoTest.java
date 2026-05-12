package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchParticipant;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.query.PlayerReviewFilter;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.ParticipantScope;
import ar.edu.itba.paw.models.types.ParticipantStatus;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
public class PlayerReviewJpaDaoTest {

    private static final Instant PAST_START = Instant.parse("2026-04-01T18:00:00Z");
    private static final Instant FUTURE_START = Instant.parse("2099-04-01T18:00:00Z");

    @Autowired private PlayerReviewDao playerReviewDao;

    @PersistenceContext private EntityManager em;

    private UserAccount user1;
    private UserAccount user2;
    private UserAccount user3;
    private UserAccount user4;
    private Match matchCompleted;
    private Match matchOpenPast;
    private Match matchOpenFuture;
    private Match matchCancelled;

    @BeforeEach
    public void setUp() {
        user1 = createUser("user1", "user1@test.com");
        user2 = createUser("user2", "user2@test.com");
        user3 = createUser("user3", "user3@test.com");
        user4 = createUser("user4", "user4@test.com");

        matchCompleted = createMatch(user1, "completed", PAST_START);
        matchOpenPast = createMatch(user1, "open", PAST_START);
        matchOpenFuture = createMatch(user1, "open", FUTURE_START);
        matchCancelled = createMatch(user1, "cancelled", PAST_START);
    }

    @Test
    public void testUpsertReviewCreatesReview() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);

        final PlayerReview review =
                playerReviewDao.upsertReview(
                        user2.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Good teammate");

        Assertions.assertNotNull(review.getId());
        Assertions.assertEquals(user2.getId(), review.getReviewer().getId());
        Assertions.assertEquals(user3.getId(), review.getReviewed().getId());
        Assertions.assertEquals(PlayerReviewReaction.LIKE, review.getReaction());
        Assertions.assertEquals("Good teammate", review.getComment());
        Assertions.assertFalse(review.isDeleted());

        flushAndClear();
        final Long count =
                em.createQuery(
                                "SELECT COUNT(pr) FROM PlayerReview pr WHERE pr.reviewer.id = :rid AND pr.reviewed.id = :uid AND pr.deleted = false",
                                Long.class)
                        .setParameter("rid", user2.getId())
                        .setParameter("uid", user3.getId())
                        .getSingleResult();
        Assertions.assertEquals(1L, count);
    }

    @Test
    public void testUpsertReviewUpdatesExistingReview() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);
        final PlayerReview initial =
                playerReviewDao.upsertReview(
                        user2.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Good teammate");

        final PlayerReview updated =
                playerReviewDao.upsertReview(
                        user2.getId(), user3.getId(), PlayerReviewReaction.DISLIKE, "Arrived late");

        Assertions.assertEquals(initial.getId(), updated.getId());
        Assertions.assertEquals(PlayerReviewReaction.DISLIKE, updated.getReaction());
        Assertions.assertEquals("Arrived late", updated.getComment());

        flushAndClear();
        final PlayerReview entity =
                em.createQuery(
                                "SELECT pr FROM PlayerReview pr WHERE pr.id = :id",
                                PlayerReview.class)
                        .setParameter("id", updated.getId())
                        .getSingleResult();
        Assertions.assertEquals(PlayerReviewReaction.DISLIKE, entity.getReaction());
    }

    @Test
    public void testUpsertReviewRestoresSoftDeletedReview() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);
        final PlayerReview initial =
                playerReviewDao.upsertReview(
                        user2.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Good teammate");
        Assertions.assertTrue(playerReviewDao.softDeleteReview(user2.getId(), user3.getId()));

        final PlayerReview restored =
                playerReviewDao.upsertReview(
                        user2.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Played well");

        Assertions.assertEquals(initial.getId(), restored.getId());
        Assertions.assertFalse(restored.isDeleted());
        Assertions.assertEquals("Played well", restored.getComment());

        flushAndClear();
        final PlayerReview entity = em.find(PlayerReview.class, restored.getId());
        Assertions.assertNull(entity.getDeletedAt());
    }

    @Test
    public void testSoftDeleteExcludesReviewFromSummary() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);
        playerReviewDao.upsertReview(
                user2.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Good teammate");

        Assertions.assertTrue(playerReviewDao.softDeleteReview(user2.getId(), user3.getId()));
        final PlayerReviewSummary summary = playerReviewDao.getSummaryForUser(user3.getId());

        Assertions.assertEquals(0L, summary.getReviewCount());
        Assertions.assertEquals(0L, summary.getLikeCount());
    }

    @Test
    public void testSoftDeleteReviewWithAdminDetails() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);
        final PlayerReview initial =
                playerReviewDao.upsertReview(
                        user2.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Good teammate");

        final boolean deleted =
                playerReviewDao.softDeleteReview(
                        user2.getId(), user3.getId(), user4.getId(), "spam");

        Assertions.assertTrue(deleted);
        final Optional<PlayerReview> found =
                playerReviewDao.findByIdIncludingDeleted(initial.getId());
        Assertions.assertTrue(found.isPresent());
        Assertions.assertTrue(found.get().isDeleted());
        Assertions.assertEquals(user4.getId(), found.get().getDeletedBy().getId());
        Assertions.assertEquals("spam", found.get().getDeleteReason());
    }

    @Test
    public void testRestoreReview() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);

        playerReviewDao.upsertReview(
                user2.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Good teammate");
        playerReviewDao.softDeleteReview(user2.getId(), user3.getId(), user4.getId(), "spam");

        final boolean restored = playerReviewDao.restoreReview(user2.getId(), user3.getId());

        Assertions.assertTrue(restored);

        final Optional<PlayerReview> found =
                playerReviewDao.findByPair(user2.getId(), user3.getId());
        Assertions.assertTrue(found.isPresent());
        Assertions.assertFalse(found.get().isDeleted());
        Assertions.assertNull(found.get().getDeletedBy());
        Assertions.assertNull(found.get().getDeleteReason());
        Assertions.assertNull(found.get().getDeletedAt());
    }

    @Test
    public void testSummaryCountsLikesAndDislikes() {
        joinMatch(matchCompleted, user1, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);
        playerReviewDao.upsertReview(
                user1.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Great");
        playerReviewDao.upsertReview(
                user2.getId(), user3.getId(), PlayerReviewReaction.DISLIKE, null);

        final PlayerReviewSummary summary = playerReviewDao.getSummaryForUser(user3.getId());

        Assertions.assertEquals(user3.getId(), summary.getReviewedUserId());
        Assertions.assertEquals(2L, summary.getReviewCount());
        Assertions.assertEquals(1L, summary.getLikeCount());
        Assertions.assertEquals(1L, summary.getDislikeCount());
    }

    @Test
    public void testFindReviewsReturnsNewestFirst() {
        joinMatch(matchCompleted, user1, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);
        playerReviewDao.upsertReview(
                user1.getId(), user3.getId(), PlayerReviewReaction.LIKE, "First");
        playerReviewDao.upsertReview(
                user2.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Second");

        final List<PlayerReview> reviews =
                playerReviewDao.findReviewsForUser(user3.getId(), PlayerReviewFilter.BOTH, 10, 0);

        Assertions.assertEquals(2, reviews.size());
        Assertions.assertEquals("Second", reviews.get(0).getComment());
        Assertions.assertEquals("First", reviews.get(1).getComment());
    }

    @Test
    public void testFindReviewsFiltersPositiveReviews() {
        joinMatch(matchCompleted, user1, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);
        playerReviewDao.upsertReview(
                user1.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Helpful");
        playerReviewDao.upsertReview(
                user2.getId(), user3.getId(), PlayerReviewReaction.DISLIKE, "Late");

        final List<PlayerReview> reviews =
                playerReviewDao.findReviewsForUser(
                        user3.getId(), PlayerReviewFilter.POSITIVE, 10, 0);

        Assertions.assertEquals(1, reviews.size());
        Assertions.assertEquals(PlayerReviewReaction.LIKE, reviews.get(0).getReaction());
        Assertions.assertEquals("Helpful", reviews.get(0).getComment());
    }

    @Test
    public void testFindReviewsFiltersBadReviews() {
        joinMatch(matchCompleted, user1, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);
        playerReviewDao.upsertReview(
                user1.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Helpful");
        playerReviewDao.upsertReview(
                user2.getId(), user3.getId(), PlayerReviewReaction.DISLIKE, "Late");

        final List<PlayerReview> reviews =
                playerReviewDao.findReviewsForUser(user3.getId(), PlayerReviewFilter.BAD, 10, 0);

        Assertions.assertEquals(1, reviews.size());
        Assertions.assertEquals(PlayerReviewReaction.DISLIKE, reviews.get(0).getReaction());
        Assertions.assertEquals("Late", reviews.get(0).getComment());
    }

    @Test
    public void testCountReviewsForUserCountsActiveReviewsByFilter() {
        joinMatch(matchCompleted, user1, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user4, ParticipantStatus.JOINED);
        playerReviewDao.upsertReview(
                user1.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Helpful");
        playerReviewDao.upsertReview(
                user2.getId(), user3.getId(), PlayerReviewReaction.DISLIKE, "Late");
        playerReviewDao.upsertReview(
                user4.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Reliable");

        final int totalReviews =
                playerReviewDao.countReviewsForUser(user3.getId(), PlayerReviewFilter.BOTH);
        final int positiveReviews =
                playerReviewDao.countReviewsForUser(user3.getId(), PlayerReviewFilter.POSITIVE);
        final int badReviews =
                playerReviewDao.countReviewsForUser(user3.getId(), PlayerReviewFilter.BAD);

        Assertions.assertEquals(3, totalReviews);
        Assertions.assertEquals(2, positiveReviews);
        Assertions.assertEquals(1, badReviews);
    }

    @Test
    public void testCountReviewsForUserExcludesDeletedReviews() {
        joinMatch(matchCompleted, user1, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);
        playerReviewDao.upsertReview(
                user1.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Helpful");
        playerReviewDao.upsertReview(
                user2.getId(), user3.getId(), PlayerReviewReaction.DISLIKE, "Late");
        Assertions.assertTrue(playerReviewDao.softDeleteReview(user2.getId(), user3.getId()));

        Assertions.assertEquals(
                1, playerReviewDao.countReviewsForUser(user3.getId(), PlayerReviewFilter.BOTH));
        Assertions.assertEquals(
                0, playerReviewDao.countReviewsForUser(user3.getId(), PlayerReviewFilter.BAD));
    }

    @Test
    public void testFindReviewsBothPreservesLimitAndOffset() {
        joinMatch(matchCompleted, user1, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user4, ParticipantStatus.JOINED);
        playerReviewDao.upsertReview(
                user1.getId(), user3.getId(), PlayerReviewReaction.LIKE, "First");
        playerReviewDao.upsertReview(
                user2.getId(), user3.getId(), PlayerReviewReaction.DISLIKE, "Second");
        playerReviewDao.upsertReview(
                user4.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Third");

        final List<PlayerReview> reviews =
                playerReviewDao.findReviewsForUser(user3.getId(), PlayerReviewFilter.BOTH, 1, 1);

        Assertions.assertEquals(1, reviews.size());
        Assertions.assertEquals("Second", reviews.get(0).getComment());
    }

    @Test
    public void testCanReviewSharedCompletedMatch() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.CHECKED_IN);

        Assertions.assertTrue(playerReviewDao.canReview(user2.getId(), user3.getId()));
    }

    @Test
    public void testCanReviewDerivedCompletedOpenPastMatch() {
        joinMatch(matchOpenPast, user2, ParticipantStatus.JOINED);
        joinMatch(matchOpenPast, user3, ParticipantStatus.JOINED);

        Assertions.assertTrue(playerReviewDao.canReview(user2.getId(), user3.getId()));
    }

    @Test
    public void testCanReviewRejectsInvalidCases() {
        joinMatch(matchOpenFuture, user2, ParticipantStatus.JOINED);
        joinMatch(matchOpenFuture, user3, ParticipantStatus.JOINED);
        joinMatch(matchCancelled, user2, ParticipantStatus.JOINED);
        joinMatch(matchCancelled, user3, ParticipantStatus.JOINED);

        Assertions.assertFalse(playerReviewDao.canReview(user2.getId(), user2.getId()));
        Assertions.assertFalse(playerReviewDao.canReview(user2.getId(), user4.getId()));
        Assertions.assertFalse(playerReviewDao.canReview(user2.getId(), user3.getId()));
    }

    @Test
    public void testCanReviewRejectsCancelledParticipant() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.CANCELLED);

        Assertions.assertFalse(playerReviewDao.canReview(user2.getId(), user3.getId()));
    }

    @Test
    public void testFindReviewableUserIdsReturnsSharedCompletedParticipants() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.CHECKED_IN);
        joinMatch(matchCompleted, user4, ParticipantStatus.JOINED);
        joinMatch(matchOpenFuture, user2, ParticipantStatus.JOINED);
        joinMatch(matchOpenFuture, user1, ParticipantStatus.JOINED);
        joinMatch(matchCancelled, user2, ParticipantStatus.JOINED);
        joinMatch(matchCancelled, user1, ParticipantStatus.JOINED);

        final Set<Long> reviewableUserIds =
                Set.copyOf(playerReviewDao.findReviewableUserIds(user2.getId()));

        Assertions.assertEquals(Set.of(user3.getId(), user4.getId()), reviewableUserIds);
    }

    @Test
    public void shouldUpsertReview_WithNullComment_WhenDislikeReaction() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);

        final PlayerReview review =
                playerReviewDao.upsertReview(
                        user2.getId(), user3.getId(), PlayerReviewReaction.DISLIKE, null);

        flushAndClear();
        final PlayerReview entity = em.find(PlayerReview.class, review.getId());
        Assertions.assertNull(
                entity.getComment(), "NULL comment should be persisted for DISLIKE reactions");
    }

    @Test
    public void shouldFindReviewableUserIds_WhenNoSharedMatches() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchOpenPast, user3, ParticipantStatus.JOINED);
        joinMatch(matchOpenFuture, user4, ParticipantStatus.JOINED);

        final List<Long> reviewableUserIds = playerReviewDao.findReviewableUserIds(user2.getId());

        Assertions.assertTrue(
                reviewableUserIds.isEmpty(),
                "User with no shared completed matches should have no reviewable users");
    }

    @Test
    public void shouldSoftDeleteReview_ExcludesReviewFromQueries() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);
        final PlayerReview review =
                playerReviewDao.upsertReview(
                        user2.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Good");

        Assertions.assertFalse(
                playerReviewDao
                        .findReviewsForUser(user3.getId(), PlayerReviewFilter.BOTH, 10, 0)
                        .isEmpty());

        final boolean deleted = playerReviewDao.softDeleteReview(user2.getId(), user3.getId());

        Assertions.assertTrue(deleted);

        flushAndClear();
        final PlayerReview entity = em.find(PlayerReview.class, review.getId());
        Assertions.assertNotNull(
                entity.getDeletedAt(), "deleted_at should be set after soft delete");

        final List<PlayerReview> reviewsAfterDelete =
                playerReviewDao.findReviewsForUser(user3.getId(), PlayerReviewFilter.BOTH, 10, 0);
        Assertions.assertTrue(
                reviewsAfterDelete.isEmpty(),
                "Soft-deleted review should be excluded from queries");
    }

    @Test
    public void shouldUpsertReview_UpdatesExistingReviewIdempotently() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);

        final PlayerReview first =
                playerReviewDao.upsertReview(
                        user2.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Great");

        final PlayerReview second =
                playerReviewDao.upsertReview(
                        user2.getId(), user3.getId(), PlayerReviewReaction.DISLIKE, null);

        Assertions.assertEquals(first.getId(), second.getId());
        Assertions.assertEquals(PlayerReviewReaction.DISLIKE, second.getReaction());

        flushAndClear();
        final Long count =
                em.createQuery(
                                "SELECT COUNT(pr) FROM PlayerReview pr WHERE pr.reviewer.id = :rid AND pr.reviewed.id = :uid AND pr.deleted = false",
                                Long.class)
                        .setParameter("rid", user2.getId())
                        .setParameter("uid", user3.getId())
                        .getSingleResult();
        Assertions.assertEquals(
                1L, count, "Should have exactly one review record after upsert update");
    }

    @Test
    public void shouldReturnFalse_WhenSoftDeletingNonExistingReview() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);

        final boolean deleted = playerReviewDao.softDeleteReview(user2.getId(), user3.getId());

        Assertions.assertFalse(deleted);
    }

    @Test
    public void shouldReturnFalse_WhenRestoringNonExistingReview() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);

        final boolean restored = playerReviewDao.restoreReview(user2.getId(), user3.getId());

        Assertions.assertFalse(restored);
    }

    @Test
    public void shouldSoftDeleteBeIdempotent() {
        joinMatch(matchCompleted, user2, ParticipantStatus.JOINED);
        joinMatch(matchCompleted, user3, ParticipantStatus.JOINED);
        playerReviewDao.upsertReview(
                user2.getId(), user3.getId(), PlayerReviewReaction.LIKE, "Good teammate");

        final boolean firstDelete = playerReviewDao.softDeleteReview(user2.getId(), user3.getId());
        final boolean secondDelete = playerReviewDao.softDeleteReview(user2.getId(), user3.getId());

        Assertions.assertTrue(firstDelete);
        Assertions.assertFalse(secondDelete);
    }

    private UserAccount createUser(String username, String email) {
        UserAccount user =
                new UserAccount(
                        null,
                        email,
                        username,
                        "Name",
                        "Last",
                        null,
                        null,
                        "hash",
                        UserRole.USER,
                        null,
                        "en",
                        Instant.now(),
                        Instant.now());
        em.persist(user);
        return user;
    }

    private Match createMatch(UserAccount host, String status, Instant startsAt) {
        Match match =
                new Match(
                        null,
                        Sport.FOOTBALL,
                        host.getId(),
                        "Address",
                        "Match",
                        "Description",
                        startsAt,
                        startsAt.plusSeconds(3600),
                        4,
                        BigDecimal.ZERO,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.valueOf(status.toUpperCase()),
                        0,
                        null);
        match.setHost(host);
        match.setCreatedAt(Instant.now());
        match.setUpdatedAt(Instant.now());
        em.persist(match);
        return match;
    }

    private void joinMatch(Match match, UserAccount user, ParticipantStatus status) {
        MatchParticipant participant =
                new MatchParticipant(match, user, status, Instant.now(), ParticipantScope.MATCH);
        em.persist(participant);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
