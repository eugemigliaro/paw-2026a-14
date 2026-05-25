package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentPairingStrategy;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.persistence.TournamentDao;
import ar.edu.itba.paw.persistence.TournamentSoloEntryDao;
import ar.edu.itba.paw.persistence.TournamentTeamDao;
import ar.edu.itba.paw.services.exceptions.TournamentRegistrationException;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class TournamentRegistrationServiceImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T00:00:00Z");

    @Mock private TournamentDao tournamentDao;
    @Mock private TournamentSoloEntryDao tournamentSoloEntryDao;
    @Mock private TournamentTeamDao tournamentTeamDao;
    @Mock private TournamentMailService tournamentMailService;
    @Mock private MessageSource messageSource;

    private TournamentRegistrationServiceImpl registrationService;

    @BeforeEach
    public void setUp() {
        registrationService =
                new TournamentRegistrationServiceImpl(
                        tournamentDao,
                        tournamentSoloEntryDao,
                        tournamentTeamDao,
                        tournamentMailService,
                        messageSource,
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        Mockito.lenient()
                .when(
                        messageSource.getMessage(
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));
        Mockito.lenient()
                .when(
                        messageSource.getMessage(
                                ArgumentMatchers.eq("tournament.team.solo.name"),
                                ArgumentMatchers.any(Object[].class),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));
        Mockito.lenient()
                .when(tournamentSoloEntryDao.update(ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.lenient()
                .when(tournamentDao.update(ArgumentMatchers.any()))
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
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDao.findUserTeam(10L, user.getId()))
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
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise
        final TournamentRegistrationException exception =
                Assertions.assertThrows(
                        TournamentRegistrationException.class,
                        () -> registrationService.joinSolo(10L, user));

        // 3. Assert
        Assertions.assertEquals(
                TournamentJoinFailureReason.REGISTRATION_NOT_OPEN, exception.getReason());
    }

    @Test
    public void joinSoloTwiceReturnsExistingEntry() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, UserUtils.getUser(1L), 4, 1);
        final User user = UserUtils.getUser(2L);
        final TournamentSoloEntry existing =
                soloEntry(20L, tournament, user, TournamentSoloEntryStatus.IN_POOL);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDao.findUserTeam(10L, user.getId()))
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
    public void leaveSoloSucceeds() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, UserUtils.getUser(1L), 4, 1);
        final User user = UserUtils.getUser(2L);
        final TournamentSoloEntry existing =
                soloEntry(20L, tournament, user, TournamentSoloEntryStatus.IN_POOL);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
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
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise
        final TournamentRegistrationException exception =
                Assertions.assertThrows(
                        TournamentRegistrationException.class,
                        () -> registrationService.leaveSolo(10L, user));

        // 3. Assert
        Assertions.assertEquals(
                TournamentJoinFailureReason.REGISTRATION_NOT_OPEN, exception.getReason());
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
        final Tournament result = registrationService.closeRegistration(10L, host);

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.BRACKET_SETUP, result.getStatus());
        Assertions.assertEquals(TournamentPairingStrategy.RANDOM, result.getPairingStrategy());
        Assertions.assertEquals(FIXED_NOW, result.getRegistrationClosedAt());
        Assertions.assertEquals(2, createdTeams.size());
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
        final Tournament result = registrationService.closeRegistration(10L, host);

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
        final Tournament result = registrationService.closeRegistration(10L, host);

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.BRACKET_SETUP, result.getStatus());
        Assertions.assertEquals(2, createdTeams.size());
        Assertions.assertEquals(TournamentSoloEntryStatus.UNASSIGNED, entries.get(2).getStatus());
        Assertions.assertNull(entries.get(2).getAssignedTeam());
    }

    @Test
    public void closeRegistrationWithOneTeamCancelsTournament() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final Tournament tournament = tournament(10L, host, 4, 2);
        final List<TournamentSoloEntry> entries = activeEntries(tournament, 2);
        configureCloseRegistration(tournament, entries);
        final List<TournamentTeam> createdTeams = new ArrayList<>();

        // 2. Exercise
        final Tournament result = registrationService.closeRegistration(10L, host);

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.CANCELLED, result.getStatus());
        Assertions.assertEquals(FIXED_NOW, result.getRegistrationClosedAt());
        Assertions.assertEquals(FIXED_NOW, result.getCancelledAt());
        Assertions.assertEquals(
                "tournament.registration.close.underCapacity", result.getCancelReason());
        Assertions.assertTrue(createdTeams.isEmpty());
        Assertions.assertTrue(
                entries.stream()
                        .allMatch(
                                entry ->
                                        TournamentSoloEntryStatus.UNASSIGNED == entry.getStatus()));
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
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise
        final TournamentRegistrationException exception =
                Assertions.assertThrows(
                        TournamentRegistrationException.class,
                        () -> registrationService.closeRegistration(10L, host));

        // 3. Assert
        Assertions.assertEquals(
                TournamentJoinFailureReason.REGISTRATION_NOT_OPEN, exception.getReason());
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
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDao.findUserTeam(10L, user.getId()))
                .thenReturn(Optional.of(team));

        // 2. Exercise
        final TournamentRegistrationException exception =
                Assertions.assertThrows(
                        TournamentRegistrationException.class,
                        () -> registrationService.joinSolo(10L, user));

        // 3. Assert
        Assertions.assertEquals(TournamentJoinFailureReason.ALREADY_ON_TEAM, exception.getReason());
    }

    @Test
    public void nonHostCannotCloseRegistration() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, UserUtils.getUser(1L), 4, 1);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise
        final TournamentRegistrationException exception =
                Assertions.assertThrows(
                        TournamentRegistrationException.class,
                        () -> registrationService.closeRegistration(10L, UserUtils.getUser(2L)));

        // 3. Assert
        Assertions.assertEquals(TournamentJoinFailureReason.FORBIDDEN, exception.getReason());
    }

    private List<TournamentTeam> configureCloseRegistrationWithTeamCreation(
            final Tournament tournament, final List<TournamentSoloEntry> activeEntries) {
        configureCloseRegistration(tournament, activeEntries);
        final List<TournamentTeam> createdTeams = new ArrayList<>();
        final AtomicLong teamIds = new AtomicLong(100L);
        Mockito.when(tournamentTeamDao.findByTournament(tournament.getId())).thenReturn(List.of());
        Mockito.when(
                        tournamentTeamDao.create(
                                ArgumentMatchers.eq(tournament),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.eq(TournamentTeamOrigin.SOLO_POOL),
                                ArgumentMatchers.isNull()))
                .thenAnswer(
                        invocation -> {
                            final TournamentTeam team =
                                    new TournamentTeam(
                                            teamIds.getAndIncrement(),
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
        Mockito.when(tournamentDao.findById(tournament.getId()))
                .thenReturn(Optional.of(tournament));
        Mockito.when(tournamentSoloEntryDao.findActiveByTournament(tournament.getId()))
                .thenReturn(activeEntries);
        Mockito.when(tournamentTeamDao.countByTournament(tournament.getId())).thenReturn(0L);
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

    private static Tournament tournament(
            final long id, final User host, final int bracketSize, final int teamSize) {
        return tournament(id, host, bracketSize, teamSize, TournamentStatus.REGISTRATION);
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
}
