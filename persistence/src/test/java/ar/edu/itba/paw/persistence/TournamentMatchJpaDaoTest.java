package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.query.InvolvementScope;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
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
public class TournamentMatchJpaDaoTest {

    @Autowired private TournamentMatchDao tournamentMatchDao;
    @Autowired private TournamentDao tournamentDao;
    @Autowired private TournamentTeamDao tournamentTeamDao;

    @PersistenceContext private EntityManager em;

    private User hostUser;
    private User teamAMember;
    private User teamBMember;
    private User hostOnlyUser;

    private Tournament padelTournament;
    private Tournament tennisTournament;
    private Tournament basketballTournament;

    private TournamentTeam padelTeamA;
    private TournamentTeam tennisTeamC;

    private TournamentMatch padelUpcomingMatch;
    private TournamentMatch padelPastMatch;
    private TournamentMatch padelUnscheduledMatch;
    private TournamentMatch tennisUpcomingMatch1;
    private TournamentMatch tennisUpcomingMatch2;
    private TournamentMatch basketballUpcomingMatch;

    @BeforeEach
    public void setUp() {
        hostUser = insertAndFindUser(100L, "host", "host@test.com");
        teamAMember = insertAndFindUser(101L, "player", "player@test.com");
        teamBMember = insertAndFindUser(102L, "player2", "player2@test.com");
        hostOnlyUser = insertAndFindUser(103L, "player3", "player3@test.com");

        final Instant now = Instant.now();

        padelTournament =
                createTournament(
                        hostUser, Sport.PADEL, "Padel Tourney", now, TournamentStatus.IN_PROGRESS);
        tennisTournament =
                createTournament(
                        hostUser,
                        Sport.TENNIS,
                        "Tennis Tourney",
                        now,
                        TournamentStatus.IN_PROGRESS);
        basketballTournament =
                createTournament(
                        hostOnlyUser,
                        Sport.BASKETBALL,
                        "Basketball Tourney",
                        now,
                        TournamentStatus.IN_PROGRESS);

        padelTeamA =
                tournamentTeamDao.create(
                        padelTournament, "Alpha", TournamentTeamOrigin.SOLO_POOL, 1);
        final TournamentTeam padelTeamB =
                tournamentTeamDao.create(
                        padelTournament, "Beta", TournamentTeamOrigin.SOLO_POOL, 2);
        tournamentTeamDao.addMember(padelTeamA, teamAMember, true);
        tournamentTeamDao.addMember(padelTeamB, teamBMember, true);

        tennisTeamC =
                tournamentTeamDao.create(
                        tennisTournament, "Gamma", TournamentTeamOrigin.SOLO_POOL, 1);
        final TournamentTeam tennisTeamD =
                tournamentTeamDao.create(
                        tennisTournament, "Delta", TournamentTeamOrigin.SOLO_POOL, 2);
        tournamentTeamDao.addMember(tennisTeamC, teamAMember, true);
        tournamentTeamDao.addMember(tennisTeamD, teamBMember, true);

        em.flush();

        padelUpcomingMatch =
                createScheduledMatch(
                        padelTournament,
                        1,
                        0,
                        padelTeamA,
                        padelTeamB,
                        TournamentMatchStatus.SCHEDULED,
                        now.plusSeconds(86400));
        padelPastMatch =
                createScheduledMatch(
                        padelTournament,
                        1,
                        1,
                        padelTeamA,
                        padelTeamB,
                        TournamentMatchStatus.DONE,
                        now.minusSeconds(2 * 86400));
        padelUnscheduledMatch =
                tournamentMatchDao.create(
                        padelTournament,
                        1,
                        2,
                        padelTeamA,
                        padelTeamB,
                        TournamentMatchStatus.PENDING,
                        null,
                        null);

        tennisUpcomingMatch1 =
                createScheduledMatch(
                        tennisTournament,
                        1,
                        0,
                        tennisTeamC,
                        tennisTeamD,
                        TournamentMatchStatus.SCHEDULED,
                        now.plusSeconds(3 * 86400));
        tennisUpcomingMatch2 =
                createScheduledMatch(
                        tennisTournament,
                        1,
                        1,
                        tennisTeamC,
                        tennisTeamD,
                        TournamentMatchStatus.SCHEDULED,
                        now.plusSeconds(5 * 86400));

        basketballUpcomingMatch =
                createScheduledMatch(
                        basketballTournament,
                        1,
                        0,
                        null,
                        null,
                        TournamentMatchStatus.SCHEDULED,
                        now.plusSeconds(86400));

        em.flush();
        em.clear();
    }

    @Test
    public void findMatchesByTeamAMembership() {
        final List<TournamentMatch> results =
                tournamentMatchDao.findByUserParticipant(
                        teamAMember,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        0,
                        100);

        Assertions.assertEquals(4, results.size());
        final List<Long> ids = ids(results);
        Assertions.assertTrue(ids.contains(padelUpcomingMatch.getId()));
        Assertions.assertTrue(ids.contains(padelPastMatch.getId()));
        Assertions.assertTrue(ids.contains(tennisUpcomingMatch1.getId()));
        Assertions.assertTrue(ids.contains(tennisUpcomingMatch2.getId()));
    }

    @Test
    public void findMatchesByTeamBMembership() {
        final List<TournamentMatch> results =
                tournamentMatchDao.findByUserParticipant(
                        teamBMember,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        0,
                        100);

        Assertions.assertEquals(4, results.size());
    }

    @Test
    public void findMatchesByHostMembership() {
        final List<TournamentMatch> results =
                tournamentMatchDao.findByUserParticipant(
                        hostOnlyUser,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        0,
                        100);

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(basketballUpcomingMatch.getId(), results.get(0).getId());
    }

    @Test
    public void findMatchesForUninvolvedUserReturnsEmpty() {
        final User uninvolved = insertAndFindUser(999L, "nobody", "nobody@test.com");

        final List<TournamentMatch> results =
                tournamentMatchDao.findByUserParticipant(
                        uninvolved,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        0,
                        100);

        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    public void excludeUnscheduledMatches() {
        Assertions.assertNull(padelUnscheduledMatch.getScheduledStartsAt());

        final List<TournamentMatch> results =
                tournamentMatchDao.findByUserParticipant(
                        teamAMember,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        0,
                        100);

        Assertions.assertFalse(ids(results).contains(padelUnscheduledMatch.getId()));
    }

    @Test
    public void filterUpcomingMatches() {
        final List<TournamentMatch> results =
                tournamentMatchDao.findByUserParticipant(
                        teamAMember,
                        true,
                        null,
                        List.of(),
                        List.of(),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        0,
                        100);

        Assertions.assertEquals(3, results.size());
        final List<Long> resultIds = ids(results);
        Assertions.assertTrue(resultIds.contains(padelUpcomingMatch.getId()));
        Assertions.assertTrue(resultIds.contains(tennisUpcomingMatch1.getId()));
        Assertions.assertTrue(resultIds.contains(tennisUpcomingMatch2.getId()));
        Assertions.assertFalse(resultIds.contains(padelPastMatch.getId()));
    }

    @Test
    public void filterPastMatches() {
        final List<TournamentMatch> results =
                tournamentMatchDao.findByUserParticipant(
                        teamAMember,
                        false,
                        null,
                        List.of(),
                        List.of(),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        0,
                        100);

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(padelPastMatch.getId(), results.get(0).getId());
    }

    @Test
    public void sortUpcomingAscending() {
        final List<TournamentMatch> results =
                tournamentMatchDao.findByUserParticipant(
                        teamAMember,
                        true,
                        null,
                        List.of(),
                        List.of(),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        0,
                        100);

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals(padelUpcomingMatch.getId(), results.get(0).getId());
        Assertions.assertEquals(tennisUpcomingMatch1.getId(), results.get(1).getId());
        Assertions.assertEquals(tennisUpcomingMatch2.getId(), results.get(2).getId());
    }

    @Test
    public void sortPastDescending() {
        final List<TournamentMatch> results =
                tournamentMatchDao.findByUserParticipant(
                        teamAMember,
                        false,
                        null,
                        List.of(),
                        List.of(),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        0,
                        100);

        Assertions.assertEquals(1, results.size());
    }

    @Test
    public void filterBySport() {
        final List<TournamentMatch> results =
                tournamentMatchDao.findByUserParticipant(
                        teamAMember,
                        null,
                        null,
                        List.of(Sport.PADEL),
                        List.of(),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        0,
                        100);

        Assertions.assertEquals(2, results.size());
        final List<Long> resultIds = ids(results);
        Assertions.assertTrue(resultIds.contains(padelUpcomingMatch.getId()));
        Assertions.assertTrue(resultIds.contains(padelPastMatch.getId()));
        Assertions.assertFalse(resultIds.contains(tennisUpcomingMatch1.getId()));
    }

    @Test
    public void filterByStatus() {
        final List<TournamentMatch> results =
                tournamentMatchDao.findByUserParticipant(
                        teamAMember,
                        null,
                        null,
                        List.of(),
                        List.of(TournamentMatchStatus.SCHEDULED),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        0,
                        100);

        Assertions.assertEquals(3, results.size());
        final List<Long> resultIds = ids(results);
        Assertions.assertTrue(resultIds.contains(padelUpcomingMatch.getId()));
        Assertions.assertTrue(resultIds.contains(tennisUpcomingMatch1.getId()));
        Assertions.assertTrue(resultIds.contains(tennisUpcomingMatch2.getId()));
        Assertions.assertFalse(resultIds.contains(padelPastMatch.getId()));
    }

    @Test
    public void searchByTournamentTitle() {
        final List<TournamentMatch> results =
                tournamentMatchDao.findByUserParticipant(
                        teamAMember,
                        null,
                        "Padel",
                        List.of(),
                        List.of(),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        0,
                        100);

        Assertions.assertEquals(2, results.size());
        final List<Long> resultIds = ids(results);
        Assertions.assertTrue(resultIds.contains(padelUpcomingMatch.getId()));
        Assertions.assertTrue(resultIds.contains(padelPastMatch.getId()));
    }

    @Test
    public void searchByTeamName() {
        final List<TournamentMatch> results =
                tournamentMatchDao.findByUserParticipant(
                        teamAMember,
                        null,
                        "Gamma",
                        List.of(),
                        List.of(),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        0,
                        100);

        Assertions.assertEquals(2, results.size());
        final List<Long> resultIds = ids(results);
        Assertions.assertTrue(resultIds.contains(tennisUpcomingMatch1.getId()));
        Assertions.assertTrue(resultIds.contains(tennisUpcomingMatch2.getId()));
    }

    @Test
    public void paginateWithLimitAndOffset() {
        final List<TournamentMatch> firstPage =
                tournamentMatchDao.findByUserParticipant(
                        teamAMember,
                        true,
                        null,
                        List.of(),
                        List.of(),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        0,
                        2);

        Assertions.assertEquals(2, firstPage.size());
        Assertions.assertEquals(padelUpcomingMatch.getId(), firstPage.get(0).getId());
        Assertions.assertEquals(tennisUpcomingMatch1.getId(), firstPage.get(1).getId());

        final List<TournamentMatch> secondPage =
                tournamentMatchDao.findByUserParticipant(
                        teamAMember,
                        true,
                        null,
                        List.of(),
                        List.of(),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        2,
                        2);

        Assertions.assertEquals(1, secondPage.size());
        Assertions.assertEquals(tennisUpcomingMatch2.getId(), secondPage.get(0).getId());
    }

    @Test
    public void countByUserParticipant() {
        final int count =
                tournamentMatchDao.countByUserParticipant(
                        teamAMember, null, null, List.of(), List.of(), InvolvementScope.ALL);

        Assertions.assertEquals(4, count);
    }

    @Test
    public void countByUserParticipantUpcoming() {
        final int count =
                tournamentMatchDao.countByUserParticipant(
                        teamAMember, true, null, List.of(), List.of(), InvolvementScope.ALL);

        Assertions.assertEquals(3, count);
    }

    @Test
    public void countByUserParticipantPast() {
        final int count =
                tournamentMatchDao.countByUserParticipant(
                        teamAMember, false, null, List.of(), List.of(), InvolvementScope.ALL);

        Assertions.assertEquals(1, count);
    }

    @Test
    public void countByUserParticipantWithSportFilter() {
        final int count =
                tournamentMatchDao.countByUserParticipant(
                        teamAMember,
                        null,
                        null,
                        List.of(Sport.TENNIS),
                        List.of(),
                        InvolvementScope.ALL);

        Assertions.assertEquals(2, count);
    }

    @Test
    public void countByUserParticipantWithStatusFilter() {
        final int count =
                tournamentMatchDao.countByUserParticipant(
                        teamAMember,
                        null,
                        null,
                        List.of(),
                        List.of(TournamentMatchStatus.DONE),
                        InvolvementScope.ALL);

        Assertions.assertEquals(1, count);
    }

    @Test
    public void countByUserParticipantWithSearchQuery() {
        final int count =
                tournamentMatchDao.countByUserParticipant(
                        teamAMember, null, "Padel", List.of(), List.of(), InvolvementScope.ALL);

        Assertions.assertEquals(2, count);
    }

    @Test
    public void searchEscapesLikeWildcards() {
        final List<TournamentMatch> results =
                tournamentMatchDao.findByUserParticipant(
                        teamAMember,
                        null,
                        "%",
                        List.of(),
                        List.of(),
                        InvolvementScope.ALL,
                        EventSort.SOONEST,
                        0,
                        100);

        Assertions.assertTrue(results.isEmpty());
    }

    private User insertAndFindUser(final long id, final String username, final String email) {
        em.createNativeQuery(
                        "INSERT INTO users (id, username, email, created_at, updated_at)"
                                + " VALUES (:id, :username, :email, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .setParameter("id", id)
                .setParameter("username", username)
                .setParameter("email", email)
                .executeUpdate();
        return em.find(User.class, id);
    }

    private Tournament createTournament(
            final User host,
            final Sport sport,
            final String title,
            final Instant now,
            final TournamentStatus status) {
        return tournamentDao.create(
                host,
                sport,
                title,
                title + " description",
                "Club Court",
                null,
                null,
                now.plusSeconds(7 * 86400),
                now.plusSeconds(7 * 86400 + 10800),
                BigDecimal.ZERO,
                null,
                TournamentFormat.SINGLE_ELIMINATION,
                8,
                2,
                true,
                false,
                now.minusSeconds(86400),
                now.plusSeconds(86400),
                status);
    }

    private TournamentMatch createScheduledMatch(
            final Tournament tournament,
            final int round,
            final int index,
            final TournamentTeam teamA,
            final TournamentTeam teamB,
            final TournamentMatchStatus status,
            final Instant scheduledStartsAt) {
        final TournamentMatch match =
                tournamentMatchDao.create(
                        tournament, round, index, teamA, teamB, status, null, null);
        if (scheduledStartsAt != null) {
            match.setScheduledStartsAt(scheduledStartsAt);
            match.setScheduledEndsAt(scheduledStartsAt.plusSeconds(7200));
            tournamentMatchDao.update(match);
        }
        return match;
    }

    private static List<Long> ids(final List<TournamentMatch> matches) {
        return matches.stream().map(TournamentMatch::getId).toList();
    }
}
