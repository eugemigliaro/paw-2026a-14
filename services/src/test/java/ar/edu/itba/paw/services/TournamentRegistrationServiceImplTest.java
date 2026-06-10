package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.tournamentRegistration.*;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentPairingStrategy;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.persistence.TournamentSoloEntryDao;
import ar.edu.itba.paw.services.internal.TournamentDataService;
import ar.edu.itba.paw.services.internal.TournamentTeamDataService;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class TournamentRegistrationServiceImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T00:00:00Z");

    @Mock private TournamentDataService tournamentDataService;
    @Mock private TournamentSoloEntryDao tournamentSoloEntryDao;
    @Mock private TournamentTeamDataService tournamentTeamDataService;

    private TournamentRegistrationServiceImpl registrationService;

    @BeforeEach
    public void setUp() {
        registrationService =
                new TournamentRegistrationServiceImpl(
                        tournamentDataService,
                        tournamentSoloEntryDao,
                        tournamentTeamDataService,
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        Mockito.lenient()
                .when(tournamentSoloEntryDao.update(ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.lenient()
                .when(tournamentDataService.update(ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void joinSoloSucceeds() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, UserUtils.getUser(1L), 4, 1);
        final User user = UserUtils.getUser(2L);
        final TournamentSoloEntry created =
                soloEntry(20L, tournament, user, TournamentSoloEntryStatus.IN_POOL);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDataService.findUserTeam(10L, user.getId()))
                .thenReturn(Optional.empty());
        Mockito.when(tournamentSoloEntryDao.findByTournamentAndUser(10L, user.getId()))
                .thenReturn(Optional.empty());
        Mockito.when(
                        tournamentSoloEntryDao.create(
                                tournament, user, TournamentSoloEntryStatus.IN_POOL))
                .thenReturn(created);

        // 2. Exercise
        final TournamentSoloEntry result = registrationService.joinSolo(10L, user);

        // 3. Assert
        Assertions.assertSame(created, result);
        Assertions.assertEquals(TournamentSoloEntryStatus.IN_POOL, result.getStatus());
    }

    @Test
    public void joinSoloBeforeRegistrationOpensFails() {
        // 1. Arrange
        final Tournament tournament =
                tournament(
                        10L,
                        UserUtils.getUser(1L),
                        4,
                        1,
                        TournamentStatus.REGISTRATION,
                        FIXED_NOW.plusSeconds(3600),
                        FIXED_NOW.plusSeconds(7200));
        final User user = UserUtils.getUser(2L);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationNotOpenException.class,
                () -> registrationService.joinSolo(10L, user));
    }

    @Test
    public void joinSoloTwiceReturnsExistingEntry() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, UserUtils.getUser(1L), 4, 1);
        final User user = UserUtils.getUser(2L);
        final TournamentSoloEntry existing =
                soloEntry(20L, tournament, user, TournamentSoloEntryStatus.IN_POOL);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDataService.findUserTeam(10L, user.getId()))
                .thenReturn(Optional.empty());
        Mockito.when(tournamentSoloEntryDao.findByTournamentAndUser(10L, user.getId()))
                .thenReturn(Optional.of(existing));

        // 2. Exercise
        final TournamentSoloEntry result = registrationService.joinSolo(10L, user);

        // 3. Assert
        Assertions.assertSame(existing, result);
        Assertions.assertEquals(TournamentSoloEntryStatus.IN_POOL, result.getStatus());
    }

    @Test
    public void joinSoloWhenPoolIsFullFails() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, UserUtils.getUser(1L), 2, 2);
        final User user = UserUtils.getUser(2L);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDataService.findUserTeam(10L, user.getId()))
                .thenReturn(Optional.empty());
        Mockito.when(tournamentSoloEntryDao.findByTournamentAndUser(10L, user.getId()))
                .thenReturn(Optional.empty());
        Mockito.when(tournamentSoloEntryDao.countActiveByTournament(10L)).thenReturn(4L);

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationSoloPoolFullException.class,
                () -> registrationService.joinSolo(10L, user));
    }

    @Test
    public void leaveSoloSucceeds() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, UserUtils.getUser(1L), 4, 1);
        final User user = UserUtils.getUser(2L);
        final TournamentSoloEntry existing =
                soloEntry(20L, tournament, user, TournamentSoloEntryStatus.IN_POOL);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentSoloEntryDao.findByTournamentAndUser(10L, user.getId()))
                .thenReturn(Optional.of(existing));

        // 2. Exercise
        registrationService.leaveSolo(10L, user);

        // 3. Assert
        Assertions.assertEquals(TournamentSoloEntryStatus.LEFT, existing.getStatus());
        Assertions.assertEquals(FIXED_NOW, existing.getLeftAt());
        Assertions.assertNull(existing.getAssignedTeam());
    }

    @Test
    public void leaveSoloAfterRegistrationClosesFails() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, 1, TournamentStatus.BRACKET_SETUP);
        final User user = UserUtils.getUser(2L);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationNotOpenException.class,
                () -> registrationService.leaveSolo(10L, user));
    }

    @Test
    public void withdrawFromOpenRegistrationsMarksInPoolEntriesAsLeft() {
        // 1. Arrange
        final User user = UserUtils.getUser(2L);
        final Tournament tournamentA = tournament(10L, UserUtils.getUser(1L), 4, 1);
        final Tournament tournamentB = tournament(11L, UserUtils.getUser(1L), 4, 1);
        final TournamentSoloEntry entryA =
                soloEntry(20L, tournamentA, user, TournamentSoloEntryStatus.IN_POOL);
        final TournamentSoloEntry entryB =
                soloEntry(21L, tournamentB, user, TournamentSoloEntryStatus.IN_POOL);
        Mockito.when(tournamentSoloEntryDao.findInPoolEntriesByUser(user))
                .thenReturn(List.of(entryA, entryB));

        // 2. Exercise
        registrationService.withdrawFromOpenRegistrations(user);

        // 3. Assert
        Assertions.assertEquals(TournamentSoloEntryStatus.LEFT, entryA.getStatus());
        Assertions.assertEquals(TournamentSoloEntryStatus.LEFT, entryB.getStatus());
        Assertions.assertEquals(FIXED_NOW, entryA.getLeftAt());
        Assertions.assertNull(entryA.getAssignedTeam());
    }

    @Test
    public void withdrawFromOpenRegistrationsRemovesUserFromRegistrationTeamAndDeletesEmptyTeam() {
        // 1. Arrange
        final User user = UserUtils.getUser(2L);
        final Tournament tournament = tournament(10L, UserUtils.getUser(1L), 4, 1);
        final TournamentTeam team =
                new TournamentTeam(
                        50L,
                        tournament,
                        "Falcons",
                        TournamentTeamOrigin.TEAM_DRAFT,
                        null,
                        FIXED_NOW);
        final RecordingTeamDataService teamData = new RecordingTeamDataService();
        teamData.tournamentsByMember = List.of(tournament);
        teamData.userTeam = Optional.of(team);
        teamData.membersAfterRemoval = 0L;
        final TournamentRegistrationServiceImpl service =
                new TournamentRegistrationServiceImpl(
                        tournamentDataService,
                        tournamentSoloEntryDao,
                        teamData,
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        Mockito.when(tournamentSoloEntryDao.findInPoolEntriesByUser(user)).thenReturn(List.of());

        // 2. Exercise
        service.withdrawFromOpenRegistrations(user);

        // 3. Assert
        Assertions.assertEquals(List.of(team), teamData.deletedTeams);
    }

    @Test
    public void withdrawFromOpenRegistrationsLeavesTeamsOnceBracketSetupStarts() {
        // 1. Arrange
        final User user = UserUtils.getUser(2L);
        final Tournament tournament =
                tournament(11L, UserUtils.getUser(1L), 4, 1, TournamentStatus.BRACKET_SETUP);
        final TournamentTeam team =
                new TournamentTeam(
                        50L,
                        tournament,
                        "Falcons",
                        TournamentTeamOrigin.TEAM_DRAFT,
                        null,
                        FIXED_NOW);
        final RecordingTeamDataService teamData = new RecordingTeamDataService();
        teamData.tournamentsByMember = List.of(tournament);
        teamData.userTeam = Optional.of(team);
        teamData.membersAfterRemoval = 0L;
        final TournamentRegistrationServiceImpl service =
                new TournamentRegistrationServiceImpl(
                        tournamentDataService,
                        tournamentSoloEntryDao,
                        teamData,
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        Mockito.when(tournamentSoloEntryDao.findInPoolEntriesByUser(user)).thenReturn(List.of());

        // 2. Exercise
        service.withdrawFromOpenRegistrations(user);

        // 3. Assert
        Assertions.assertTrue(teamData.deletedTeams.isEmpty());
    }

    @Test
    public void listTeamMembersAfterRegistrationClosesReturnsAssignedMembers() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, 1, TournamentStatus.BRACKET_SETUP);
        final TournamentTeam team =
                new TournamentTeam(
                        20L, tournament, null, TournamentTeamOrigin.SOLO_POOL, null, FIXED_NOW);
        final List<TournamentTeamMember> members =
                List.of(
                        new TournamentTeamMember(
                                30L, team, UserUtils.getUser(2L), false, FIXED_NOW));
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDataService.findMembersByTournament(10L)).thenReturn(members);

        // 2. Exercise
        final List<TournamentTeamMember> result = registrationService.listTeamMembers(10L);

        // 3. Assert
        Assertions.assertEquals(members, result);
    }

    @Test
    public void closeRegistrationGroupsExactMultipleIntoTeams() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final Tournament tournament = tournament(10L, host, 2, 2);
        final List<TournamentSoloEntry> entries = activeEntries(tournament, 4);
        final List<TournamentTeam> createdTeams =
                configureCloseRegistrationWithTeamCreation(tournament, entries);

        // 2. Exercise
        final Tournament result = registrationService.closeRegistration(10L);

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.BRACKET_SETUP, result.getStatus());
        Assertions.assertEquals(TournamentPairingStrategy.RANDOM, result.getPairingStrategy());
        Assertions.assertEquals(FIXED_NOW, result.getRegistrationClosedAt());
        Assertions.assertEquals(2, createdTeams.size());
        Assertions.assertTrue(createdTeams.stream().allMatch(team -> team.getName() == null));
        Assertions.assertTrue(
                entries.stream()
                        .allMatch(
                                entry -> TournamentSoloEntryStatus.ASSIGNED == entry.getStatus()));
        Assertions.assertTrue(entries.stream().allMatch(entry -> entry.getAssignedTeam() != null));
    }

    @Test
    public void closeRegistrationMarksLeftoversUnassigned() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final Tournament tournament = tournament(10L, host, 2, 2);
        final List<TournamentSoloEntry> entries = activeEntries(tournament, 5);
        final List<TournamentTeam> createdTeams =
                configureCloseRegistrationWithTeamCreation(tournament, entries);

        // 2. Exercise
        final Tournament result = registrationService.closeRegistration(10L);

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.BRACKET_SETUP, result.getStatus());
        Assertions.assertEquals(2, createdTeams.size());
        Assertions.assertEquals(TournamentSoloEntryStatus.UNASSIGNED, entries.get(4).getStatus());
        Assertions.assertNull(entries.get(4).getAssignedTeam());
    }

    @Test
    public void closeRegistrationMarksOverflowUnassigned() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final Tournament tournament = tournament(10L, host, 2, 1);
        final List<TournamentSoloEntry> entries = activeEntries(tournament, 3);
        final List<TournamentTeam> createdTeams =
                configureCloseRegistrationWithTeamCreation(tournament, entries);

        // 2. Exercise
        final Tournament result = registrationService.closeRegistration(10L);

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.BRACKET_SETUP, result.getStatus());
        Assertions.assertEquals(2, createdTeams.size());
        Assertions.assertEquals(TournamentSoloEntryStatus.UNASSIGNED, entries.get(2).getStatus());
        Assertions.assertNull(entries.get(2).getAssignedTeam());
    }

    @Test
    public void closeRegistrationWithOneTeamFailsWithoutCancellingTournament() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final Tournament tournament = tournament(10L, host, 4, 2);
        final List<TournamentSoloEntry> entries = activeEntries(tournament, 2);
        configureCloseRegistration(tournament, entries);

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationUnderCapacityException.class,
                () -> registrationService.closeRegistration(10L));
        Assertions.assertEquals(TournamentStatus.REGISTRATION, tournament.getStatus());
        Assertions.assertNull(tournament.getRegistrationClosedAt());
        Assertions.assertNull(tournament.getCancelledAt());
        Assertions.assertTrue(
                entries.stream()
                        .allMatch(entry -> TournamentSoloEntryStatus.IN_POOL == entry.getStatus()));
    }

    @Test
    public void closeRegistrationBeforeRegistrationOpensFails() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final Tournament tournament =
                tournament(
                        10L,
                        host,
                        4,
                        2,
                        TournamentStatus.REGISTRATION,
                        FIXED_NOW.plusSeconds(3600),
                        FIXED_NOW.plusSeconds(7200));
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationNotOpenException.class,
                () -> registrationService.closeRegistration(10L));
    }

    @Test
    public void userAlreadyOnTeamCannotJoinSolo() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, UserUtils.getUser(1L), 4, 1);
        final User user = UserUtils.getUser(2L);
        final TournamentTeam team =
                new TournamentTeam(
                        30L,
                        tournament,
                        "Existing team",
                        TournamentTeamOrigin.SOLO_POOL,
                        null,
                        FIXED_NOW);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDataService.findUserTeam(10L, user.getId()))
                .thenReturn(Optional.of(team));

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationAlreadyOnTeamException.class,
                () -> registrationService.joinSolo(10L, user));
    }

    @Test
    public void createTeamSucceedsAndTrimsName() {
        // 1. Arrange
        final Tournament tournament = tournamentTeamDraft(10L, UserUtils.getUser(1L), 4, 5);
        final User user = UserUtils.getUser(2L);
        final TournamentTeam team =
                new TournamentTeam(
                        50L,
                        tournament,
                        "Falcons",
                        TournamentTeamOrigin.TEAM_DRAFT,
                        null,
                        FIXED_NOW);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(
                        tournamentTeamDataService.create(
                                tournament, "Falcons", TournamentTeamOrigin.TEAM_DRAFT, null))
                .thenReturn(team);

        // 2. Exercise
        final TournamentTeam result = registrationService.createTeam(10L, user, "  Falcons  ");

        // 3. Assert
        Assertions.assertSame(team, result);
    }

    @Test
    public void createTeamWithBlankNameFails() {
        // 1. Arrange
        final Tournament tournament = tournamentTeamDraft(10L, UserUtils.getUser(1L), 4, 5);
        final User user = UserUtils.getUser(2L);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationTeamNameRequiredException.class,
                () -> registrationService.createTeam(10L, user, "   "));
    }

    @Test
    public void createTeamWhenNameTakenFails() {
        // 1. Arrange
        final Tournament tournament = tournamentTeamDraft(10L, UserUtils.getUser(1L), 4, 5);
        final User user = UserUtils.getUser(2L);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDataService.existsByTournamentAndName(10L, "Falcons"))
                .thenReturn(true);

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationTeamNameTakenException.class,
                () -> registrationService.createTeam(10L, user, "Falcons"));
    }

    @Test
    public void createTeamWhenTeamDraftDisabledFails() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, UserUtils.getUser(1L), 4, 5);
        final User user = UserUtils.getUser(2L);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationTeamDraftDisabledException.class,
                () -> registrationService.createTeam(10L, user, "Falcons"));
    }

    @Test
    public void createTeamWhenAlreadyOnTeamFails() {
        // 1. Arrange
        final Tournament tournament = tournamentTeamDraft(10L, UserUtils.getUser(1L), 4, 5);
        final User user = UserUtils.getUser(2L);
        final TournamentTeam existing =
                new TournamentTeam(
                        70L, tournament, "Other", TournamentTeamOrigin.TEAM_DRAFT, null, FIXED_NOW);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDataService.findUserTeam(10L, user.getId()))
                .thenReturn(Optional.of(existing));

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationAlreadyOnTeamException.class,
                () -> registrationService.createTeam(10L, user, "Falcons"));
    }

    @Test
    public void createTeamWhenInSoloPoolFails() {
        // 1. Arrange
        final Tournament tournament = tournamentTeamDraft(10L, UserUtils.getUser(1L), 4, 5);
        final User user = UserUtils.getUser(2L);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentSoloEntryDao.findByTournamentAndUser(10L, user.getId()))
                .thenReturn(
                        Optional.of(
                                soloEntry(
                                        20L, tournament, user, TournamentSoloEntryStatus.IN_POOL)));

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationAlreadyInSoloPoolException.class,
                () -> registrationService.createTeam(10L, user, "Falcons"));
    }

    @Test
    public void createTeamWhenTeamCapReachedFails() {
        // 1. Arrange
        final Tournament tournament = tournamentTeamDraft(10L, UserUtils.getUser(1L), 4, 5);
        final User user = UserUtils.getUser(2L);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDataService.countByTournament(10L)).thenReturn(4L);

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationTeamCapReachedException.class,
                () -> registrationService.createTeam(10L, user, "Falcons"));
    }

    @Test
    public void createTeamWhenTournamentFullFails() {
        // 1. Arrange
        final Tournament tournament = tournamentTeamDraft(10L, UserUtils.getUser(1L), 4, 5);
        final User user = UserUtils.getUser(2L);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentSoloEntryDao.countActiveByTournament(10L)).thenReturn(20L);

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationTournamentFullException.class,
                () -> registrationService.createTeam(10L, user, "Falcons"));
    }

    @Test
    public void joinTeamSucceeds() {
        // 1. Arrange
        final Tournament tournament = tournamentTeamDraft(10L, UserUtils.getUser(1L), 4, 5);
        final User user = UserUtils.getUser(2L);
        final TournamentTeam team =
                new TournamentTeam(
                        50L,
                        tournament,
                        "Falcons",
                        TournamentTeamOrigin.TEAM_DRAFT,
                        null,
                        FIXED_NOW);
        final TournamentTeamMember member =
                new TournamentTeamMember(60L, team, user, false, FIXED_NOW);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDataService.findById(50L)).thenReturn(Optional.of(team));
        Mockito.when(tournamentTeamDataService.countMembers(50L)).thenReturn(1L);
        Mockito.when(tournamentTeamDataService.addMember(team, user, false)).thenReturn(member);

        // 2. Exercise
        final TournamentTeamMember result = registrationService.joinTeam(10L, 50L, user);

        // 3. Assert
        Assertions.assertSame(member, result);
    }

    @Test
    public void joinTeamWhenTeamNotFoundFails() {
        // 1. Arrange
        final Tournament tournament = tournamentTeamDraft(10L, UserUtils.getUser(1L), 4, 5);
        final User user = UserUtils.getUser(2L);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDataService.findById(50L)).thenReturn(Optional.empty());

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationTeamNotFoundException.class,
                () -> registrationService.joinTeam(10L, 50L, user));
    }

    @Test
    public void joinTeamWhenTeamFullFails() {
        // 1. Arrange
        final Tournament tournament = tournamentTeamDraft(10L, UserUtils.getUser(1L), 4, 5);
        final User user = UserUtils.getUser(2L);
        final TournamentTeam team =
                new TournamentTeam(
                        50L,
                        tournament,
                        "Falcons",
                        TournamentTeamOrigin.TEAM_DRAFT,
                        null,
                        FIXED_NOW);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDataService.findById(50L)).thenReturn(Optional.of(team));
        Mockito.when(tournamentTeamDataService.countMembers(50L)).thenReturn(5L);

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationTeamFullException.class,
                () -> registrationService.joinTeam(10L, 50L, user));
    }

    @Test
    public void joinTeamWhenTournamentFullFails() {
        // 1. Arrange
        final Tournament tournament = tournamentTeamDraft(10L, UserUtils.getUser(1L), 4, 5);
        final User user = UserUtils.getUser(2L);
        final TournamentTeam team =
                new TournamentTeam(
                        50L,
                        tournament,
                        "Falcons",
                        TournamentTeamOrigin.TEAM_DRAFT,
                        null,
                        FIXED_NOW);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDataService.findById(50L)).thenReturn(Optional.of(team));
        Mockito.when(tournamentTeamDataService.countMembers(50L)).thenReturn(1L);
        Mockito.when(tournamentTeamDataService.countMembersByTournament(10L)).thenReturn(20L);

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationTournamentFullException.class,
                () -> registrationService.joinTeam(10L, 50L, user));
    }

    @Test
    public void leaveTeamWhenNotOnTeamFails() {
        // 1. Arrange
        final Tournament tournament = tournamentTeamDraft(10L, UserUtils.getUser(1L), 4, 5);
        final User user = UserUtils.getUser(2L);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDataService.findUserTeam(10L, user.getId()))
                .thenReturn(Optional.empty());

        // 2. Exercise + Assert
        Assertions.assertThrows(
                TournamentRegistrationNotOnTeamException.class,
                () -> registrationService.leaveTeam(10L, user));
    }

    @Test
    public void leaveTeamDeletesTeamWhenLastMemberLeaves() {
        // 1. Arrange
        final Tournament tournament = tournamentTeamDraft(10L, UserUtils.getUser(1L), 4, 5);
        final User user = UserUtils.getUser(2L);
        final TournamentTeam team =
                new TournamentTeam(
                        50L,
                        tournament,
                        "Falcons",
                        TournamentTeamOrigin.TEAM_DRAFT,
                        null,
                        FIXED_NOW);
        final RecordingTeamDataService teamData = new RecordingTeamDataService();
        teamData.userTeam = Optional.of(team);
        teamData.membersAfterRemoval = 0L;
        final TournamentRegistrationServiceImpl service =
                new TournamentRegistrationServiceImpl(
                        tournamentDataService,
                        tournamentSoloEntryDao,
                        teamData,
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise
        service.leaveTeam(10L, user);

        // 3. Assert
        Assertions.assertEquals(List.of(team), teamData.deletedTeams);
    }

    @Test
    public void leaveTeamKeepsTeamWhenMembersRemain() {
        // 1. Arrange
        final Tournament tournament = tournamentTeamDraft(10L, UserUtils.getUser(1L), 4, 5);
        final User user = UserUtils.getUser(2L);
        final TournamentTeam team =
                new TournamentTeam(
                        50L,
                        tournament,
                        "Falcons",
                        TournamentTeamOrigin.TEAM_DRAFT,
                        null,
                        FIXED_NOW);
        final RecordingTeamDataService teamData = new RecordingTeamDataService();
        teamData.userTeam = Optional.of(team);
        teamData.membersAfterRemoval = 2L;
        final TournamentRegistrationServiceImpl service =
                new TournamentRegistrationServiceImpl(
                        tournamentDataService,
                        tournamentSoloEntryDao,
                        teamData,
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise
        service.leaveTeam(10L, user);

        // 3. Assert
        Assertions.assertTrue(teamData.deletedTeams.isEmpty());
    }

    @Test
    public void closeRegistrationTopsUpIncompleteTeamFromSoloPool() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final Tournament tournament = tournament(10L, host, 3, 2);
        final TournamentTeam bravo =
                new TournamentTeam(
                        60L, tournament, "Bravo", TournamentTeamOrigin.TEAM_DRAFT, null, FIXED_NOW);
        final TournamentSoloEntry first =
                soloEntry(
                        100L,
                        tournament,
                        UserUtils.getUser(100L),
                        TournamentSoloEntryStatus.IN_POOL);
        final TournamentSoloEntry second =
                soloEntry(
                        101L,
                        tournament,
                        UserUtils.getUser(101L),
                        TournamentSoloEntryStatus.IN_POOL);
        final TournamentSoloEntry third =
                soloEntry(
                        102L,
                        tournament,
                        UserUtils.getUser(102L),
                        TournamentSoloEntryStatus.IN_POOL);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDataService.findByTournamentUnordered(10L))
                .thenReturn(List.of(bravo));
        Mockito.when(tournamentTeamDataService.findMembersByTournament(10L))
                .thenReturn(
                        List.of(member(70L, bravo, UserUtils.getUser(200L))),
                        List.of(
                                member(70L, bravo, UserUtils.getUser(200L)),
                                member(71L, bravo, first.getUser())));
        Mockito.when(tournamentSoloEntryDao.findActiveByTournament(10L))
                .thenReturn(List.of(first, second, third), List.of(second, third));
        Mockito.when(tournamentTeamDataService.countByTournament(10L)).thenReturn(1L);
        final List<TournamentTeam> createdTeams = recordCreatedSoloTeams(tournament);

        // 2. Exercise
        final Tournament result = registrationService.closeRegistration(10L);

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.BRACKET_SETUP, result.getStatus());
        Assertions.assertEquals(1, createdTeams.size());
        Assertions.assertEquals(TournamentSoloEntryStatus.ASSIGNED, first.getStatus());
        Assertions.assertSame(bravo, first.getAssignedTeam());
        Assertions.assertEquals(TournamentSoloEntryStatus.ASSIGNED, second.getStatus());
        Assertions.assertSame(createdTeams.get(0), second.getAssignedTeam());
    }

    @Test
    public void closeRegistrationDissolvesUnfillableTeamsAndRepacks() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final Tournament tournament = tournament(10L, host, 3, 2);
        final TournamentTeam alpha =
                new TournamentTeam(
                        50L, tournament, "Alpha", TournamentTeamOrigin.TEAM_DRAFT, null, FIXED_NOW);
        final TournamentTeam echo =
                new TournamentTeam(
                        60L, tournament, "Echo", TournamentTeamOrigin.TEAM_DRAFT, null, FIXED_NOW);
        final TournamentTeam foxtrot =
                new TournamentTeam(
                        61L,
                        tournament,
                        "Foxtrot",
                        TournamentTeamOrigin.TEAM_DRAFT,
                        null,
                        FIXED_NOW);
        final User echoUser = UserUtils.getUser(302L);
        final User foxtrotUser = UserUtils.getUser(303L);
        Mockito.when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDataService.findByTournamentUnordered(10L))
                .thenReturn(List.of(alpha, echo, foxtrot));
        Mockito.when(tournamentTeamDataService.findMembersByTournament(10L))
                .thenReturn(
                        List.of(
                                member(40L, alpha, UserUtils.getUser(200L)),
                                member(41L, alpha, UserUtils.getUser(201L)),
                                member(42L, echo, echoUser),
                                member(43L, foxtrot, foxtrotUser)));
        final TournamentSoloEntry echoEntry =
                soloEntry(80L, tournament, echoUser, TournamentSoloEntryStatus.IN_POOL);
        final TournamentSoloEntry foxtrotEntry =
                soloEntry(81L, tournament, foxtrotUser, TournamentSoloEntryStatus.IN_POOL);
        Mockito.when(tournamentSoloEntryDao.findActiveByTournament(10L))
                .thenReturn(List.of(), List.of(echoEntry, foxtrotEntry));
        Mockito.when(tournamentTeamDataService.countByTournament(10L)).thenReturn(1L);
        final List<TournamentTeam> createdTeams = recordCreatedSoloTeams(tournament);

        // 2. Exercise
        final Tournament result = registrationService.closeRegistration(10L);

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.BRACKET_SETUP, result.getStatus());
        Assertions.assertEquals(1, createdTeams.size());
        Assertions.assertEquals(TournamentSoloEntryStatus.ASSIGNED, echoEntry.getStatus());
        Assertions.assertSame(createdTeams.get(0), echoEntry.getAssignedTeam());
        Assertions.assertEquals(TournamentSoloEntryStatus.ASSIGNED, foxtrotEntry.getStatus());
    }

    private List<TournamentTeam> recordCreatedSoloTeams(final Tournament tournament) {
        final List<TournamentTeam> createdTeams = new ArrayList<>();
        Mockito.when(
                        tournamentTeamDataService.create(
                                ArgumentMatchers.eq(tournament),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.eq(TournamentTeamOrigin.SOLO_POOL),
                                ArgumentMatchers.isNull()))
                .thenAnswer(
                        invocation -> {
                            final TournamentTeam team =
                                    new TournamentTeam(
                                            200L + createdTeams.size(),
                                            tournament,
                                            null,
                                            TournamentTeamOrigin.SOLO_POOL,
                                            null,
                                            FIXED_NOW);
                            createdTeams.add(team);
                            return team;
                        });
        return createdTeams;
    }

    private List<TournamentTeam> configureCloseRegistrationWithTeamCreation(
            final Tournament tournament, final List<TournamentSoloEntry> activeEntries) {
        configureCloseRegistration(tournament, activeEntries);
        final List<TournamentTeam> createdTeams = new ArrayList<>();
        Mockito.when(
                        tournamentTeamDataService.create(
                                ArgumentMatchers.eq(tournament),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.eq(TournamentTeamOrigin.SOLO_POOL),
                                ArgumentMatchers.isNull()))
                .thenAnswer(
                        invocation -> {
                            final TournamentTeam team =
                                    new TournamentTeam(
                                            100L + createdTeams.size(),
                                            tournament,
                                            invocation.getArgument(1),
                                            TournamentTeamOrigin.SOLO_POOL,
                                            null,
                                            FIXED_NOW);
                            createdTeams.add(team);
                            return team;
                        });
        return createdTeams;
    }

    private void configureCloseRegistration(
            final Tournament tournament, final List<TournamentSoloEntry> activeEntries) {
        Mockito.when(tournamentDataService.findById(tournament.getId()))
                .thenReturn(Optional.of(tournament));
        Mockito.when(tournamentSoloEntryDao.findActiveByTournament(tournament.getId()))
                .thenReturn(activeEntries);
        Mockito.when(tournamentTeamDataService.countByTournament(tournament.getId()))
                .thenReturn(0L);
    }

    private static List<TournamentSoloEntry> activeEntries(
            final Tournament tournament, final int count) {
        final List<TournamentSoloEntry> entries = new ArrayList<>();
        for (long index = 0; index < count; index++) {
            entries.add(
                    soloEntry(
                            20L + index,
                            tournament,
                            UserUtils.getUser(100L + index),
                            TournamentSoloEntryStatus.IN_POOL));
        }
        return entries;
    }

    private static TournamentSoloEntry soloEntry(
            final Long id,
            final Tournament tournament,
            final User user,
            final TournamentSoloEntryStatus status) {
        return new TournamentSoloEntry(id, tournament, user, status, null, FIXED_NOW, null);
    }

    private static TournamentTeamMember member(
            final long id, final TournamentTeam team, final User user) {
        return new TournamentTeamMember(id, team, user, false, FIXED_NOW);
    }

    private static Tournament tournament(
            final long id, final User host, final int bracketSize, final int teamSize) {
        return tournament(id, host, bracketSize, teamSize, TournamentStatus.REGISTRATION);
    }

    private static Tournament tournamentTeamDraft(
            final long id, final User host, final int bracketSize, final int teamSize) {
        return new Tournament(
                id,
                host,
                Sport.FOOTBALL,
                "Saturday Cup",
                "Friendly tournament",
                "Club Street 123",
                -34.60,
                -58.38,
                FIXED_NOW.plusSeconds(86400),
                FIXED_NOW.plusSeconds(90000),
                BigDecimal.ZERO,
                null,
                TournamentFormat.SINGLE_ELIMINATION,
                bracketSize,
                teamSize,
                true,
                true,
                FIXED_NOW.minusSeconds(3600),
                FIXED_NOW.plusSeconds(7200),
                TournamentStatus.REGISTRATION,
                FIXED_NOW,
                FIXED_NOW);
    }

    private static Tournament tournament(
            final long id,
            final User host,
            final int bracketSize,
            final int teamSize,
            final TournamentStatus status,
            final Instant registrationOpensAt,
            final Instant registrationClosesAt) {
        return new Tournament(
                id,
                host,
                Sport.FOOTBALL,
                "Saturday Cup",
                "Friendly tournament",
                "Club Street 123",
                -34.60,
                -58.38,
                FIXED_NOW.plusSeconds(86400),
                FIXED_NOW.plusSeconds(90000),
                BigDecimal.ZERO,
                null,
                TournamentFormat.SINGLE_ELIMINATION,
                bracketSize,
                teamSize,
                true,
                false,
                registrationOpensAt,
                registrationClosesAt,
                status,
                FIXED_NOW,
                FIXED_NOW);
    }

    private static Tournament tournament(
            final long id,
            final User host,
            final int bracketSize,
            final int teamSize,
            final TournamentStatus status) {
        return new Tournament(
                id,
                host,
                Sport.FOOTBALL,
                "Saturday Cup",
                "Friendly tournament",
                "Club Street 123",
                -34.60,
                -58.38,
                FIXED_NOW.plusSeconds(86400),
                FIXED_NOW.plusSeconds(90000),
                BigDecimal.ZERO,
                null,
                TournamentFormat.SINGLE_ELIMINATION,
                bracketSize,
                teamSize,
                true,
                false,
                FIXED_NOW.minusSeconds(3600),
                FIXED_NOW.plusSeconds(7200),
                status,
                FIXED_NOW,
                FIXED_NOW);
    }

    private static class RecordingTeamDataService implements TournamentTeamDataService {

        private Optional<TournamentTeam> userTeam = Optional.empty();
        private long membersAfterRemoval = 0L;
        private final List<TournamentTeam> deletedTeams = new ArrayList<>();
        private List<Tournament> tournamentsByMember = new ArrayList<>();

        @Override
        public Optional<TournamentTeam> findUserTeam(final long tournamentId, final long userId) {
            return userTeam;
        }

        @Override
        public void removeMember(final TournamentTeam team, final User user) {
            // recorded implicitly via membersAfterRemoval
        }

        @Override
        public long countMembers(final long teamId) {
            return membersAfterRemoval;
        }

        @Override
        public void delete(final TournamentTeam team) {
            deletedTeams.add(team);
        }

        @Override
        public TournamentTeam create(
                final Tournament tournament,
                final String name,
                final TournamentTeamOrigin origin,
                final Integer seedPosition) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TournamentTeamMember addMember(
                final TournamentTeam team, final User user, final boolean captain) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countMembersByTournament(final long tournamentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TournamentTeam> findJoinableByTournament(
                final long tournamentId, final int teamSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsByTournamentAndName(final long tournamentId, final String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TournamentTeam> findById(final long teamId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TournamentTeam> findByTournament(final long tournamentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TournamentTeam> findByTournamentUnordered(final long tournamentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TournamentTeam> findSeededByTournament(final long tournamentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TournamentTeamMember> findMembersByTournament(final long tournamentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Tournament> findTournamentsByMember(final User user) {
            return tournamentsByMember;
        }

        @Override
        public List<TournamentTeam> findByTournaments(Collection<Long> teamIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveSeedOrder(
                final List<TournamentTeam> teams, final List<Long> orderedTeamIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countByTournament(final long tournamentId) {
            throw new UnsupportedOperationException();
        }
    }
}
