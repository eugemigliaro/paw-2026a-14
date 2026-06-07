package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
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
public class TournamentJpaDaoTest {

    @Autowired private TournamentDao tournamentDao;
    @Autowired private TournamentTeamDao tournamentTeamDao;
    @Autowired private TournamentSoloEntryDao tournamentSoloEntryDao;
    @Autowired private TournamentMatchDao tournamentMatchDao;

    @PersistenceContext private EntityManager em;

    private User host;
    private User player;
    private User secondPlayer;

    @BeforeEach
    public void setUp() {
        insertUser(100L, "host", "host@test.com");
        insertUser(101L, "player", "player@test.com");
        insertUser(102L, "player2", "player2@test.com");
        em.flush();
        em.clear();

        host = em.find(User.class, 100L);
        player = em.find(User.class, 101L);
        secondPlayer = em.find(User.class, 102L);
    }

    @Test
    public void shouldCreateAndFindTournament() {
        final Tournament tournament = createTournament(TournamentStatus.REGISTRATION);

        em.flush();
        em.clear();

        final Tournament found = tournamentDao.findById(tournament.getId()).orElseThrow();
        final Tournament publicTournament =
                tournamentDao.findPublicById(tournament.getId()).orElseThrow();

        Assertions.assertNotNull(tournament.getId());
        Assertions.assertEquals(host.getId(), found.getHost().getId());
        Assertions.assertEquals(Sport.PADEL, found.getSport());
        Assertions.assertEquals("Summer Cup", found.getTitle());
        Assertions.assertEquals(TournamentFormat.SINGLE_ELIMINATION, found.getFormat());
        Assertions.assertEquals(8, found.getBracketSize());
        Assertions.assertEquals(2, found.getTeamSize());
        Assertions.assertEquals(TournamentStatus.REGISTRATION, publicTournament.getStatus());
    }

    @Test
    public void shouldSaveManualSeedOrderWithoutViolatingUniqueConstraint() {
        final Tournament tournament = createTournament(TournamentStatus.BRACKET_SETUP);
        final TournamentTeam firstTeam =
                tournamentTeamDao.create(tournament, "Team A", TournamentTeamOrigin.SOLO_POOL, 1);
        final TournamentTeam secondTeam =
                tournamentTeamDao.create(tournament, "Team B", TournamentTeamOrigin.SOLO_POOL, 2);

        em.flush();
        em.clear();

        final TournamentTeam persistedFirst =
                tournamentTeamDao.findById(firstTeam.getId()).orElseThrow();
        final TournamentTeam persistedSecond =
                tournamentTeamDao.findById(secondTeam.getId()).orElseThrow();

        tournamentTeamDao.saveSeedOrder(
                List.of(persistedFirst, persistedSecond),
                List.of(persistedSecond.getId(), persistedFirst.getId()));

        em.flush();
        em.clear();

        final List<TournamentTeam> teams = tournamentTeamDao.findByTournament(tournament.getId());

        Assertions.assertEquals(
                List.of(persistedSecond.getId(), persistedFirst.getId()),
                teams.stream().map(TournamentTeam::getId).toList());
        Assertions.assertEquals(1, teams.get(0).getSeedPosition());
        Assertions.assertEquals(2, teams.get(1).getSeedPosition());
    }

    @Test
    public void shouldFindHostedAndPublicActiveTournaments() {
        final Tournament first = createTournament(TournamentStatus.REGISTRATION);
        final Tournament second = createTournament(TournamentStatus.IN_PROGRESS);
        createTournament(TournamentStatus.COMPLETED);
        final Tournament unscheduled = createUnscheduledTournament(TournamentStatus.REGISTRATION);
        createTournament(player, "Other Host Cup", TournamentStatus.REGISTRATION);
        final Tournament deleted = createTournament(TournamentStatus.REGISTRATION);
        deleted.setDeleted(true);
        tournamentDao.update(deleted);

        em.flush();
        em.clear();

        final List<Tournament> hosted =
                tournamentDao.findDashboardTournaments(
                        host,
                        Boolean.TRUE,
                        Boolean.TRUE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        EventSort.SOONEST,
                        null,
                        null,
                        0,
                        10);
        final int hostedCount =
                tournamentDao.countDashboardTournaments(
                        host, Boolean.TRUE, Boolean.TRUE, null, null, null, null, null, null);
        final List<Tournament> hostedPast =
                tournamentDao.findDashboardTournaments(
                        host,
                        Boolean.FALSE,
                        Boolean.TRUE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        EventSort.SOONEST,
                        null,
                        null,
                        0,
                        10);
        final List<Tournament> publicActive = tournamentDao.findPublicRegistrationOrLive(0, 10);

        Assertions.assertEquals(4, hosted.size());
        Assertions.assertEquals(4, hostedCount);
        Assertions.assertTrue(hosted.stream().anyMatch(t -> t.getId().equals(unscheduled.getId())));
        Assertions.assertTrue(
                hostedPast.stream().noneMatch(t -> t.getId().equals(unscheduled.getId())));
        Assertions.assertEquals(4, publicActive.size());
        Assertions.assertTrue(publicActive.stream().anyMatch(t -> t.getId().equals(first.getId())));
        Assertions.assertTrue(
                publicActive.stream().anyMatch(t -> t.getId().equals(second.getId())));
    }

    @Test
    public void shouldSearchPublicTournamentsByActiveStatusAndCountMatchesResults() {
        final Instant now = Instant.parse("2026-04-05T00:00:00Z");
        final Tournament registration =
                createTournament(
                        "Registration Cup",
                        Sport.PADEL,
                        TournamentStatus.REGISTRATION,
                        now.plusSeconds(86_400),
                        BigDecimal.TEN,
                        -34.56,
                        -58.45);
        final Tournament bracketSetup =
                createTournament(
                        "Bracket Cup",
                        Sport.FOOTBALL,
                        TournamentStatus.BRACKET_SETUP,
                        now.plusSeconds(172_800),
                        BigDecimal.ZERO,
                        -34.57,
                        -58.46);
        final Tournament inProgress =
                createTournament(
                        "Live Cup",
                        Sport.TENNIS,
                        TournamentStatus.IN_PROGRESS,
                        now.plusSeconds(259_200),
                        BigDecimal.ONE,
                        -34.58,
                        -58.47);
        createTournament(
                "Completed Cup",
                Sport.PADEL,
                TournamentStatus.COMPLETED,
                now.plusSeconds(345_600),
                BigDecimal.ZERO,
                -34.59,
                -58.48);
        createTournament(
                "Cancelled Cup",
                Sport.PADEL,
                TournamentStatus.CANCELLED,
                now.plusSeconds(432_000),
                BigDecimal.ZERO,
                -34.60,
                -58.49);
        final Tournament deleted =
                createTournament(
                        "Deleted Cup",
                        Sport.PADEL,
                        TournamentStatus.REGISTRATION,
                        now.plusSeconds(518_400),
                        BigDecimal.ZERO,
                        -34.61,
                        -58.50);
        deleted.setDeleted(true);
        tournamentDao.update(deleted);

        em.flush();
        em.clear();

        final List<Tournament> results =
                tournamentDao.findPublicTournaments(
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        EventSort.SOONEST,
                        null,
                        null,
                        0,
                        10);
        final int count =
                tournamentDao.countPublicTournaments(null, List.of(), null, null, null, null);

        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals(3, count);
        Assertions.assertEquals(
                List.of(registration.getId(), bracketSetup.getId(), inProgress.getId()),
                tournamentIds(results));
    }

    @Test
    public void shouldFilterPublicTournamentsByQuerySportDateAndPrice() {
        final Instant now = Instant.parse("2026-04-05T00:00:00Z");
        final Tournament matching =
                createTournament(
                        "Sunrise Open",
                        Sport.PADEL,
                        TournamentStatus.REGISTRATION,
                        now.plusSeconds(172_800),
                        new BigDecimal("15"),
                        -34.56,
                        -58.45);
        createTournament(
                "Sunrise Football",
                Sport.FOOTBALL,
                TournamentStatus.REGISTRATION,
                now.plusSeconds(172_800),
                new BigDecimal("15"),
                -34.57,
                -58.46);
        createTournament(
                "Sunrise Later",
                Sport.PADEL,
                TournamentStatus.REGISTRATION,
                now.plusSeconds(604_800),
                new BigDecimal("15"),
                -34.58,
                -58.47);
        createTournament(
                "Sunrise Premium",
                Sport.PADEL,
                TournamentStatus.REGISTRATION,
                now.plusSeconds(172_800),
                new BigDecimal("50"),
                -34.59,
                -58.48);

        em.flush();
        em.clear();

        final List<Tournament> results =
                tournamentDao.findPublicTournaments(
                        "sunrise",
                        List.of(Sport.PADEL),
                        now.plusSeconds(86_400),
                        now.plusSeconds(259_200),
                        new BigDecimal("10"),
                        new BigDecimal("20"),
                        EventSort.SOONEST,
                        null,
                        null,
                        0,
                        10);
        final int count =
                tournamentDao.countPublicTournaments(
                        "sunrise",
                        List.of(Sport.PADEL),
                        now.plusSeconds(86_400),
                        now.plusSeconds(259_200),
                        new BigDecimal("10"),
                        new BigDecimal("20"));

        Assertions.assertEquals(List.of(matching.getId()), tournamentIds(results));
        Assertions.assertEquals(1, count);
    }

    @Test
    public void shouldSortPublicTournamentsByPriceAndDistance() {
        final Instant now = Instant.parse("2026-04-05T00:00:00Z");
        final Tournament nearExpensive =
                createTournament(
                        "Near Expensive",
                        Sport.PADEL,
                        TournamentStatus.REGISTRATION,
                        now.plusSeconds(86_400),
                        new BigDecimal("30"),
                        -34.600,
                        -58.380);
        final Tournament farCheap =
                createTournament(
                        "Far Cheap",
                        Sport.PADEL,
                        TournamentStatus.REGISTRATION,
                        now.plusSeconds(172_800),
                        BigDecimal.ONE,
                        -34.700,
                        -58.480);
        final Tournament noCoordinates =
                createTournament(
                        "No Coordinates",
                        Sport.PADEL,
                        TournamentStatus.REGISTRATION,
                        now.plusSeconds(259_200),
                        BigDecimal.TEN,
                        null,
                        null);

        em.flush();
        em.clear();

        final List<Tournament> priceSorted =
                tournamentDao.findPublicTournaments(
                        null,
                        List.of(Sport.PADEL),
                        null,
                        null,
                        null,
                        null,
                        EventSort.PRICE_LOW,
                        null,
                        null,
                        0,
                        10);
        final List<Tournament> distanceSorted =
                tournamentDao.findPublicTournaments(
                        null,
                        List.of(Sport.PADEL),
                        null,
                        null,
                        null,
                        null,
                        EventSort.DISTANCE,
                        -34.601,
                        -58.381,
                        0,
                        10);

        Assertions.assertEquals(
                List.of(farCheap.getId(), noCoordinates.getId(), nearExpensive.getId()),
                tournamentIds(priceSorted));
        Assertions.assertEquals(
                List.of(nearExpensive.getId(), farCheap.getId(), noCoordinates.getId()),
                tournamentIds(distanceSorted));
    }

    @Test
    public void shouldCreateTeamsMembersAndFindUserTeam() {
        final Tournament tournament = createTournament(TournamentStatus.BRACKET_SETUP);
        final TournamentTeam team =
                tournamentTeamDao.create(tournament, null, TournamentTeamOrigin.SOLO_POOL, 1);
        final TournamentTeam secondTeam =
                tournamentTeamDao.create(tournament, null, TournamentTeamOrigin.SOLO_POOL, 2);

        tournamentTeamDao.addMember(team, player, false);

        em.flush();
        em.clear();

        final List<TournamentTeam> teams = tournamentTeamDao.findByTournament(tournament.getId());
        final List<TournamentTeam> seeded =
                tournamentTeamDao.findSeededByTournament(tournament.getId());
        final TournamentTeam userTeam =
                tournamentTeamDao.findUserTeam(tournament.getId(), player.getId()).orElseThrow();

        Assertions.assertEquals(2, tournamentTeamDao.countByTournament(tournament.getId()));
        Assertions.assertEquals(2, teams.size());
        Assertions.assertEquals(2, seeded.size());
        Assertions.assertEquals(team.getId(), userTeam.getId());
        Assertions.assertNull(teams.get(0).getName());
        Assertions.assertNull(teams.get(1).getName());
        Assertions.assertEquals(secondTeam.getId(), teams.get(1).getId());
    }

    @Test
    public void shouldFindMemberTournamentsOnceOrderedByStartDate() {
        final Instant now = Instant.parse("2026-04-05T00:00:00Z");
        final Tournament later =
                createTournament(
                        "Later Cup",
                        Sport.PADEL,
                        TournamentStatus.BRACKET_SETUP,
                        now.plusSeconds(172_800),
                        BigDecimal.ZERO,
                        -34.56,
                        -58.45);
        final Tournament earlier =
                createTournament(
                        "Earlier Cup",
                        Sport.PADEL,
                        TournamentStatus.BRACKET_SETUP,
                        now.plusSeconds(86_400),
                        BigDecimal.ZERO,
                        -34.56,
                        -58.45);
        final TournamentTeam laterTeam =
                tournamentTeamDao.create(later, "Later squad", TournamentTeamOrigin.SOLO_POOL, 1);
        final TournamentTeam earlierTeamOne =
                tournamentTeamDao.create(
                        earlier, "Earlier squad #1", TournamentTeamOrigin.SOLO_POOL, 1);
        final TournamentTeam earlierTeamTwo =
                tournamentTeamDao.create(
                        earlier, "Earlier squad #2", TournamentTeamOrigin.SOLO_POOL, 2);
        tournamentTeamDao.addMember(laterTeam, player, false);
        tournamentTeamDao.addMember(earlierTeamOne, player, false);
        tournamentTeamDao.addMember(earlierTeamTwo, player, false);

        em.flush();
        em.clear();

        final List<Tournament> tournaments = tournamentTeamDao.findTournamentsByMember(player);

        Assertions.assertEquals(
                List.of(earlier.getId(), later.getId()), tournamentIds(tournaments));
    }

    @Test
    public void shouldRefreshTournamentScheduleWindowFromScheduledMatches() {
        final Instant now = Instant.parse("2026-04-05T00:00:00Z");
        final Tournament tournament =
                tournamentDao.create(
                        host,
                        Sport.PADEL,
                        "Derived Schedule Cup",
                        "Tournament with scheduled fixtures",
                        "Club Court 1",
                        -34.56,
                        -58.45,
                        null,
                        null,
                        BigDecimal.ZERO,
                        null,
                        TournamentFormat.SINGLE_ELIMINATION,
                        4,
                        1,
                        true,
                        false,
                        now.minusSeconds(86_400),
                        now.plusSeconds(86_400),
                        TournamentStatus.BRACKET_SETUP);
        final TournamentTeam teamOne =
                tournamentTeamDao.create(tournament, "Team 1", TournamentTeamOrigin.SOLO_POOL, 1);
        final TournamentTeam teamTwo =
                tournamentTeamDao.create(tournament, "Team 2", TournamentTeamOrigin.SOLO_POOL, 2);
        final TournamentTeam teamThree =
                tournamentTeamDao.create(tournament, "Team 3", TournamentTeamOrigin.SOLO_POOL, 3);
        final TournamentTeam teamFour =
                tournamentTeamDao.create(tournament, "Team 4", TournamentTeamOrigin.SOLO_POOL, 4);
        final TournamentMatch laterMatch =
                tournamentMatchDao.create(
                        tournament,
                        1,
                        0,
                        teamOne,
                        teamTwo,
                        TournamentMatchStatus.SCHEDULED,
                        null,
                        null);
        final TournamentMatch earlierMatch =
                tournamentMatchDao.create(
                        tournament,
                        1,
                        1,
                        teamThree,
                        teamFour,
                        TournamentMatchStatus.SCHEDULED,
                        null,
                        null);
        laterMatch.setScheduledStartsAt(now.plusSeconds(10_800));
        laterMatch.setScheduledEndsAt(now.plusSeconds(14_400));
        earlierMatch.setScheduledStartsAt(now.plusSeconds(3_600));
        earlierMatch.setScheduledEndsAt(now.plusSeconds(7_200));
        tournamentMatchDao.update(laterMatch);
        tournamentMatchDao.update(earlierMatch);
        em.flush();
        em.clear();

        final Tournament refreshed =
                tournamentDao.refreshScheduleWindow(tournament.getId()).orElseThrow();

        Assertions.assertEquals(now.plusSeconds(3_600), refreshed.getStartsAt());
        Assertions.assertEquals(now.plusSeconds(14_400), refreshed.getEndsAt());
    }

    @Test
    public void shouldRejectDuplicateSeedPosition() {
        final Tournament tournament = createTournament(TournamentStatus.BRACKET_SETUP);
        tournamentTeamDao.create(tournament, "Solo squad #1", TournamentTeamOrigin.SOLO_POOL, 1);
        tournamentTeamDao.create(tournament, "Solo squad #2", TournamentTeamOrigin.SOLO_POOL, 1);

        Assertions.assertThrows(PersistenceException.class, () -> em.flush());
    }

    @Test
    public void shouldCreateAndQuerySoloEntries() {
        final Tournament tournament = createTournament(TournamentStatus.REGISTRATION);
        final TournamentSoloEntry active =
                tournamentSoloEntryDao.create(
                        tournament, player, TournamentSoloEntryStatus.IN_POOL);
        tournamentSoloEntryDao.create(tournament, secondPlayer, TournamentSoloEntryStatus.LEFT);

        em.flush();
        em.clear();

        final TournamentSoloEntry found =
                tournamentSoloEntryDao
                        .findByTournamentAndUser(tournament.getId(), player.getId())
                        .orElseThrow();
        final List<TournamentSoloEntry> activeEntries =
                tournamentSoloEntryDao.findActiveByTournament(tournament.getId());

        Assertions.assertEquals(active.getId(), found.getId());
        Assertions.assertEquals(TournamentSoloEntryStatus.IN_POOL, found.getStatus());
        Assertions.assertEquals(
                1, tournamentSoloEntryDao.countActiveByTournament(tournament.getId()));
        Assertions.assertEquals(1, activeEntries.size());
    }

    @Test
    public void shouldRejectDuplicateSoloEntryForUser() {
        final Tournament tournament = createTournament(TournamentStatus.REGISTRATION);
        tournamentSoloEntryDao.create(tournament, player, TournamentSoloEntryStatus.IN_POOL);
        tournamentSoloEntryDao.create(tournament, player, TournamentSoloEntryStatus.IN_POOL);

        Assertions.assertThrows(PersistenceException.class, () -> em.flush());
    }

    @Test
    public void shouldCreateAndQueryTournamentBracketOrderedByRoundAndIndex() {
        final Tournament tournament = createTournament(TournamentStatus.BRACKET_SETUP);
        final TournamentTeam teamA =
                tournamentTeamDao.create(tournament, "Team A", TournamentTeamOrigin.SOLO_POOL, 1);
        final TournamentTeam teamB =
                tournamentTeamDao.create(tournament, "Team B", TournamentTeamOrigin.SOLO_POOL, 2);
        final TournamentTeam teamC =
                tournamentTeamDao.create(tournament, "Team C", TournamentTeamOrigin.SOLO_POOL, 3);
        final TournamentTeam teamD =
                tournamentTeamDao.create(tournament, "Team D", TournamentTeamOrigin.SOLO_POOL, 4);
        final TournamentMatch first =
                tournamentMatchDao.create(
                        tournament,
                        1,
                        0,
                        teamA,
                        teamB,
                        TournamentMatchStatus.SCHEDULED,
                        null,
                        null);
        final TournamentMatch second =
                tournamentMatchDao.create(
                        tournament,
                        1,
                        1,
                        teamC,
                        teamD,
                        TournamentMatchStatus.SCHEDULED,
                        null,
                        null);
        final TournamentMatch finalMatch =
                tournamentMatchDao.create(
                        tournament, 2, 0, null, null, TournamentMatchStatus.PENDING, first, second);

        em.flush();
        em.clear();

        final List<TournamentMatch> bracket =
                tournamentMatchDao.findByTournament(tournament.getId());
        final List<TournamentMatch> roundOne =
                tournamentMatchDao.findByTournamentAndRound(tournament.getId(), 1);
        final TournamentMatch found =
                tournamentMatchDao
                        .findByTournamentAndId(tournament.getId(), finalMatch.getId())
                        .orElseThrow();

        Assertions.assertEquals(
                List.of(first.getId(), second.getId(), finalMatch.getId()), ids(bracket));
        Assertions.assertEquals(List.of(first.getId(), second.getId()), ids(roundOne));
        Assertions.assertEquals(finalMatch.getId(), found.getId());
        Assertions.assertEquals(first.getId(), found.getParentMatchA().getId());
        Assertions.assertEquals(second.getId(), found.getParentMatchB().getId());
    }

    @Test
    public void shouldRejectDuplicateFixtureRoundAndIndex() {
        final Tournament tournament = createTournament(TournamentStatus.BRACKET_SETUP);
        tournamentMatchDao.create(
                tournament, 1, 0, null, null, TournamentMatchStatus.PENDING, null, null);
        tournamentMatchDao.create(
                tournament, 1, 0, null, null, TournamentMatchStatus.PENDING, null, null);

        Assertions.assertThrows(PersistenceException.class, () -> em.flush());
    }

    private Tournament createTournament(final TournamentStatus status) {
        return createTournament(host, "Summer Cup", status);
    }

    private Tournament createUnscheduledTournament(final TournamentStatus status) {
        final Instant now = Instant.now();
        return tournamentDao.create(
                host,
                Sport.PADEL,
                "Unscheduled Cup",
                "Competitive padel tournament",
                "Club Court 1",
                -34.56,
                -58.45,
                null,
                null,
                BigDecimal.ZERO,
                null,
                TournamentFormat.SINGLE_ELIMINATION,
                8,
                2,
                true,
                false,
                now,
                now.plusSeconds(2 * 24 * 60 * 60),
                status);
    }

    private Tournament createTournament(
            final User tournamentHost, final String title, final TournamentStatus status) {
        final Instant now = Instant.now();
        return tournamentDao.create(
                tournamentHost,
                Sport.PADEL,
                title,
                "Competitive padel tournament",
                "Club Court 1",
                -34.56,
                -58.45,
                now.plusSeconds(7 * 24 * 60 * 60),
                now.plusSeconds(7 * 24 * 60 * 60 + 3 * 60 * 60),
                BigDecimal.ZERO,
                null,
                TournamentFormat.SINGLE_ELIMINATION,
                8,
                2,
                true,
                false,
                now,
                now.plusSeconds(2 * 24 * 60 * 60),
                status);
    }

    private Tournament createTournament(
            final String title,
            final Sport sport,
            final TournamentStatus status,
            final Instant startsAt,
            final BigDecimal price,
            final Double latitude,
            final Double longitude) {
        return tournamentDao.create(
                host,
                sport,
                title,
                title + " description",
                "Club Court 1",
                latitude,
                longitude,
                startsAt,
                startsAt.plusSeconds(3 * 60 * 60),
                price,
                null,
                TournamentFormat.SINGLE_ELIMINATION,
                8,
                2,
                true,
                false,
                startsAt.minusSeconds(7 * 24 * 60 * 60),
                startsAt.minusSeconds(24 * 60 * 60),
                status);
    }

    private void insertUser(final long id, final String username, final String email) {
        em.createNativeQuery(
                        "INSERT INTO users (id, username, email, created_at, updated_at)"
                                + " VALUES (:id, :username, :email, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .setParameter("id", id)
                .setParameter("username", username)
                .setParameter("email", email)
                .executeUpdate();
    }

    private static List<Long> ids(final List<TournamentMatch> matches) {
        return matches.stream().map(TournamentMatch::getId).toList();
    }

    private static List<Long> tournamentIds(final List<Tournament> tournaments) {
        return tournaments.stream().map(Tournament::getId).toList();
    }
}
