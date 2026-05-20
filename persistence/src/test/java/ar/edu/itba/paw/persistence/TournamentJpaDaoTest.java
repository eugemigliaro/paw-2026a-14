package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
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
    public void shouldFindHostedAndPublicActiveTournaments() {
        final Tournament first = createTournament(TournamentStatus.REGISTRATION);
        final Tournament second = createTournament(TournamentStatus.IN_PROGRESS);
        createTournament(TournamentStatus.COMPLETED);

        em.flush();
        em.clear();

        final List<Tournament> hosted = tournamentDao.findHostedByUser(host, 0, 10);
        final List<Tournament> publicActive = tournamentDao.findPublicRegistrationOrLive(0, 10);

        Assertions.assertEquals(3, hosted.size());
        Assertions.assertEquals(2, publicActive.size());
        Assertions.assertTrue(publicActive.stream().anyMatch(t -> t.getId().equals(first.getId())));
        Assertions.assertTrue(
                publicActive.stream().anyMatch(t -> t.getId().equals(second.getId())));
    }

    @Test
    public void shouldCreateTeamsMembersAndFindUserTeam() {
        final Tournament tournament = createTournament(TournamentStatus.BRACKET_SETUP);
        final TournamentTeam team =
                tournamentTeamDao.create(
                        tournament, "Solo squad #1", TournamentTeamOrigin.SOLO_POOL, 1);

        tournamentTeamDao.addMember(team, player, false);

        em.flush();
        em.clear();

        final List<TournamentTeam> teams = tournamentTeamDao.findByTournament(tournament.getId());
        final List<TournamentTeam> seeded =
                tournamentTeamDao.findSeededByTournament(tournament.getId());
        final TournamentTeam userTeam =
                tournamentTeamDao.findUserTeam(tournament.getId(), player.getId()).orElseThrow();

        Assertions.assertEquals(1, tournamentTeamDao.countByTournament(tournament.getId()));
        Assertions.assertEquals(1, teams.size());
        Assertions.assertEquals(1, seeded.size());
        Assertions.assertEquals(team.getId(), userTeam.getId());
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
        final Instant now = Instant.now();
        return tournamentDao.create(
                host,
                Sport.PADEL,
                "Summer Cup",
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
}
