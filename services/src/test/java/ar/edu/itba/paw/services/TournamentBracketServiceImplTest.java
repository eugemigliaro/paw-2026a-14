package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import ar.edu.itba.paw.models.types.TournamentPairingStrategy;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.persistence.TournamentDao;
import ar.edu.itba.paw.persistence.TournamentMatchDao;
import ar.edu.itba.paw.persistence.TournamentTeamDao;
import ar.edu.itba.paw.services.exceptions.TournamentBracketException;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class TournamentBracketServiceImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T00:00:00Z");

    @Mock private TournamentDao tournamentDao;
    @Mock private TournamentTeamDao tournamentTeamDao;
    @Mock private TournamentMatchDao tournamentMatchDao;
    @Mock private UserSportRatingService userSportRatingService;
    @Mock private TournamentMailService tournamentMailService;
    @Mock private MessageSource messageSource;

    private TournamentBracketServiceImpl bracketService;

    @BeforeEach
    public void setUp() {
        bracketService =
                new TournamentBracketServiceImpl(
                        tournamentDao,
                        tournamentTeamDao,
                        tournamentMatchDao,
                        userSportRatingService,
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
                .when(tournamentDao.update(ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.lenient()
                .when(tournamentMatchDao.update(ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void generateCreatesCorrectFixtureCountForFourTeams() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        configureGenerate(tournament, teams(tournament, 4));

        // 2. Exercise
        final List<TournamentMatch> matches =
                bracketService.generateBracket(10L, tournament.getHost());

        // 3. Assert
        Assertions.assertEquals(3, matches.size());
        Assertions.assertEquals(FIXED_NOW, tournament.getBracketGeneratedAt());
    }

    @Test
    public void generateCreatesCorrectFixtureCountForEightTeams() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 8, TournamentStatus.BRACKET_SETUP);
        configureGenerate(tournament, teams(tournament, 8));

        // 2. Exercise
        final List<TournamentMatch> matches =
                bracketService.generateBracket(10L, tournament.getHost());

        // 3. Assert
        Assertions.assertEquals(7, matches.size());
        Assertions.assertEquals(FIXED_NOW, tournament.getBracketGeneratedAt());
    }

    @Test
    public void generateCreatesCorrectFixtureCountForSixteenTeams() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 16, TournamentStatus.BRACKET_SETUP);
        configureGenerate(tournament, teams(tournament, 16));

        // 2. Exercise
        final List<TournamentMatch> matches =
                bracketService.generateBracket(10L, tournament.getHost());

        // 3. Assert
        Assertions.assertEquals(15, matches.size());
        Assertions.assertEquals(FIXED_NOW, tournament.getBracketGeneratedAt());
    }

    @Test
    public void generateCreatesPlayInRoundWhenTeamCountIsNotPowerOfTwo() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 8, TournamentStatus.BRACKET_SETUP);
        configureGenerate(tournament, teams(tournament, 6));

        // 2. Exercise
        final List<TournamentMatch> matches =
                bracketService.generateBracket(10L, tournament.getHost());

        // 3. Assert
        Assertions.assertEquals(5, matches.size());
        Assertions.assertEquals(
                2, matches.stream().filter(match -> match.getRoundNumber() == 1).count());
        Assertions.assertEquals(
                2, matches.stream().filter(match -> match.getRoundNumber() == 2).count());
        Assertions.assertEquals(
                1, matches.stream().filter(match -> match.getRoundNumber() == 3).count());
    }

    @Test
    public void roundOneMatchesContainEveryTeamExactlyOnce() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 8, TournamentStatus.BRACKET_SETUP);
        final List<TournamentTeam> teams = teams(tournament, 8);
        configureGenerate(tournament, teams);

        // 2. Exercise
        final List<TournamentMatch> matches =
                bracketService.generateBracket(10L, tournament.getHost());

        // 3. Assert
        final Set<Long> expectedTeamIds =
                teams.stream().map(TournamentTeam::getId).collect(Collectors.toSet());
        final List<Long> roundOneTeamIds =
                matches.stream()
                        .filter(match -> match.getRoundNumber() == 1)
                        .flatMap(match -> Stream.of(match.getTeamA(), match.getTeamB()))
                        .map(TournamentTeam::getId)
                        .toList();
        Assertions.assertEquals(8, roundOneTeamIds.size());
        Assertions.assertEquals(expectedTeamIds, new HashSet<>(roundOneTeamIds));
    }

    @Test
    public void generateCreatesParentLinksForLaterRounds() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        configureGenerate(tournament, teams(tournament, 4));

        // 2. Exercise
        final List<TournamentMatch> matches =
                bracketService.generateBracket(10L, tournament.getHost());

        // 3. Assert
        final TournamentMatch firstRoundFirst = matches.get(0);
        final TournamentMatch firstRoundSecond = matches.get(1);
        final TournamentMatch finalMatch = matches.get(2);
        Assertions.assertEquals(2, finalMatch.getRoundNumber());
        Assertions.assertSame(firstRoundFirst, finalMatch.getParentMatchA());
        Assertions.assertSame(firstRoundSecond, finalMatch.getParentMatchB());
        Assertions.assertNull(finalMatch.getTeamA());
        Assertions.assertNull(finalMatch.getTeamB());
    }

    @Test
    public void cannotGenerateUnderCapacity() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        configureGenerate(tournament, teams(tournament, 1), false);

        // 2. Exercise
        final TournamentBracketException exception =
                Assertions.assertThrows(
                        TournamentBracketException.class,
                        () -> bracketService.generateBracket(10L, tournament.getHost()));

        // 3. Assert
        Assertions.assertEquals(
                TournamentBracketFailureReason.UNDER_CAPACITY, exception.getReason());
    }

    @Test
    public void cannotGenerateOutsideBracketSetup() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.REGISTRATION);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise
        final TournamentBracketException exception =
                Assertions.assertThrows(
                        TournamentBracketException.class,
                        () -> bracketService.generateBracket(10L, tournament.getHost()));

        // 3. Assert
        Assertions.assertEquals(
                TournamentBracketFailureReason.NOT_READY_FOR_BRACKET, exception.getReason());
    }

    @Test
    public void cannotGenerateTwice() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournament(10L))
                .thenReturn(List.of(match(1000L, tournament, 1, 0, null, null, null, null)));

        // 2. Exercise
        final TournamentBracketException exception =
                Assertions.assertThrows(
                        TournamentBracketException.class,
                        () -> bracketService.generateBracket(10L, tournament.getHost()));

        // 3. Assert
        Assertions.assertEquals(
                TournamentBracketFailureReason.BRACKET_ALREADY_GENERATED, exception.getReason());
    }

    @Test
    public void nonHostCannotGenerate() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise
        final TournamentBracketException exception =
                Assertions.assertThrows(
                        TournamentBracketException.class,
                        () -> bracketService.generateBracket(10L, UserUtils.getUser(2L)));

        // 3. Assert
        Assertions.assertEquals(TournamentBracketFailureReason.FORBIDDEN, exception.getReason());
    }

    @Test
    public void adminModCanGenerateForAnotherHost() {
        // 1. Arrange
        authenticateAdminMod();
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        configureGenerate(tournament, teams(tournament, 4));

        // 2. Exercise
        final List<TournamentMatch> matches =
                bracketService.generateBracket(10L, UserUtils.getUser(99L));

        // 3. Assert
        Assertions.assertEquals(3, matches.size());
        Assertions.assertEquals(FIXED_NOW, tournament.getBracketGeneratedAt());
    }

    @Test
    public void nonHostCannotPublish() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise
        final TournamentBracketException exception =
                Assertions.assertThrows(
                        TournamentBracketException.class,
                        () -> bracketService.publishBracket(10L, UserUtils.getUser(2L), List.of()));

        // 3. Assert
        Assertions.assertEquals(TournamentBracketFailureReason.FORBIDDEN, exception.getReason());
    }

    @Test
    public void adminModCanPublishForAnotherHost() {
        // 1. Arrange
        authenticateAdminMod();
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final Tournament result =
                bracketService.publishBracket(
                        10L,
                        UserUtils.getUser(99L),
                        List.of(
                                schedule(matches.get(0).getId()),
                                schedule(matches.get(1).getId())));

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.IN_PROGRESS, result.getStatus());
        Assertions.assertEquals(FIXED_NOW, result.getStartedAt());
    }

    @Test
    public void publishReturnsTournamentWithDerivedScheduleWindow() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        tournament.setStartsAt(null);
        tournament.setEndsAt(null);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);
        Mockito.when(tournamentDao.refreshScheduleWindow(10L))
                .thenAnswer(
                        invocation -> {
                            tournament.setStartsAt(FIXED_NOW.plusSeconds(3_600));
                            tournament.setEndsAt(FIXED_NOW.plusSeconds(14_400));
                            return Optional.of(tournament);
                        });

        // 2. Exercise
        final Tournament result =
                bracketService.publishBracket(
                        10L,
                        tournament.getHost(),
                        List.of(
                                new TournamentMatchScheduleRequest(
                                        matches.get(0).getId(),
                                        FIXED_NOW.plusSeconds(7_200),
                                        FIXED_NOW.plusSeconds(10_800),
                                        "Club Court 1",
                                        -34.56,
                                        -58.45),
                                new TournamentMatchScheduleRequest(
                                        matches.get(1).getId(),
                                        FIXED_NOW.plusSeconds(3_600),
                                        FIXED_NOW.plusSeconds(14_400),
                                        "Club Court 2",
                                        -34.56,
                                        -58.45)));

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.IN_PROGRESS, result.getStatus());
        Assertions.assertEquals(FIXED_NOW.plusSeconds(3_600), result.getStartsAt());
        Assertions.assertEquals(FIXED_NOW.plusSeconds(14_400), result.getEndsAt());
    }

    @Test
    public void nonHostCannotDeclareWinner() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise
        final TournamentBracketException exception =
                Assertions.assertThrows(
                        TournamentBracketException.class,
                        () ->
                                bracketService.declareWinner(
                                        10L,
                                        1000L,
                                        new TournamentWinnerDeclarationRequest(100L),
                                        UserUtils.getUser(2L)));

        // 3. Assert
        Assertions.assertEquals(TournamentBracketFailureReason.FORBIDDEN, exception.getReason());
    }

    @Test
    public void adminModCanDeclareWinnerForAnotherHost() {
        // 1. Arrange
        authenticateAdminMod();
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final TournamentMatch firstRoundMatch = matches.get(0);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournamentAndId(10L, firstRoundMatch.getId()))
                .thenReturn(Optional.of(firstRoundMatch));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final TournamentMatch result =
                bracketService.declareWinner(
                        10L,
                        firstRoundMatch.getId(),
                        new TournamentWinnerDeclarationRequest(firstRoundMatch.getTeamA().getId()),
                        UserUtils.getUser(99L));

        // 3. Assert
        Assertions.assertSame(firstRoundMatch.getTeamA(), result.getWinnerTeam());
        Assertions.assertEquals(TournamentMatchStatus.DONE, result.getStatus());
    }

    @Test
    public void adminModCanRecordWalkoverForAnotherHost() {
        // 1. Arrange
        authenticateAdminMod();
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final TournamentMatch firstRoundMatch = matches.get(0);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournamentAndId(10L, firstRoundMatch.getId()))
                .thenReturn(Optional.of(firstRoundMatch));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final TournamentMatch result =
                bracketService.recordWalkover(
                        10L,
                        firstRoundMatch.getId(),
                        firstRoundMatch.getTeamA().getId(),
                        UserUtils.getUser(99L));

        // 3. Assert
        Assertions.assertSame(firstRoundMatch.getTeamB(), result.getWinnerTeam());
        Assertions.assertEquals(TournamentMatchStatus.WALKOVER, result.getStatus());
    }

    @Test
    public void publicCannotReadBracketBeforePublish() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise
        final TournamentBracketException exception =
                Assertions.assertThrows(
                        TournamentBracketException.class,
                        () -> bracketService.getBracket(10L, null));

        // 3. Assert
        Assertions.assertEquals(TournamentBracketFailureReason.FORBIDDEN, exception.getReason());
    }

    @Test
    public void hostCanInspectBracketBeforePublish() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final List<TournamentTeam> teams = bracketTeams(matches);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDao.findByTournament(10L)).thenReturn(teams);
        Mockito.when(tournamentTeamDao.findUserTeam(10L, tournament.getHost().getId()))
                .thenReturn(Optional.empty());
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final TournamentBracketView view = bracketService.getBracket(10L, tournament.getHost());

        // 3. Assert
        Assertions.assertSame(tournament, view.getTournament());
        Assertions.assertEquals(teams, view.getTeams());
        Assertions.assertEquals(matches, view.getMatches());
        Assertions.assertNull(view.getViewerTeam());
        Assertions.assertSame(matches.get(0), view.getFocusedMatch());
    }

    @Test
    public void publicCanReadBracketAfterPublish() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final List<TournamentTeam> teams = bracketTeams(matches);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDao.findByTournament(10L)).thenReturn(teams);
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final TournamentBracketView view = bracketService.getBracket(10L, null);

        // 3. Assert
        Assertions.assertSame(tournament, view.getTournament());
        Assertions.assertEquals(teams, view.getTeams());
        Assertions.assertEquals(matches, view.getMatches());
        Assertions.assertSame(matches.get(0), view.getFocusedMatch());
    }

    @Test
    public void viewerTeamDrivesFocusedMatch() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final User viewer = UserUtils.getUser(50L);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final List<TournamentTeam> teams = bracketTeams(matches);
        final TournamentTeam viewerTeam = matches.get(1).getTeamA();
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDao.findByTournament(10L)).thenReturn(teams);
        Mockito.when(tournamentTeamDao.findUserTeam(10L, viewer.getId()))
                .thenReturn(Optional.of(viewerTeam));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final TournamentBracketView view = bracketService.getBracket(10L, viewer);

        // 3. Assert
        Assertions.assertSame(viewerTeam, view.getViewerTeam());
        Assertions.assertSame(matches.get(1), view.getFocusedMatch());
    }

    @Test
    public void completedBracketRemainsReadable() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.COMPLETED);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final List<TournamentTeam> teams = bracketTeams(matches);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDao.findByTournament(10L)).thenReturn(teams);
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final TournamentBracketView view = bracketService.getBracket(10L, null);

        // 3. Assert
        Assertions.assertSame(tournament, view.getTournament());
        Assertions.assertEquals(matches, view.getMatches());
    }

    @Test
    public void publishRejectsMissingRoundOneSchedules() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final TournamentBracketException exception =
                Assertions.assertThrows(
                        TournamentBracketException.class,
                        () ->
                                bracketService.publishBracket(
                                        10L,
                                        tournament.getHost(),
                                        List.of(schedule(matches.get(0).getId()))));

        // 3. Assert
        Assertions.assertEquals(
                TournamentBracketFailureReason.MISSING_ROUND_ONE_SCHEDULE, exception.getReason());
    }

    @Test
    public void publishTransitionsToInProgress() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final Tournament result =
                bracketService.publishBracket(
                        10L,
                        tournament.getHost(),
                        List.of(
                                schedule(matches.get(0).getId()),
                                schedule(matches.get(1).getId())));

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.IN_PROGRESS, result.getStatus());
        Assertions.assertEquals(FIXED_NOW, result.getStartedAt());
        Assertions.assertEquals(FIXED_NOW, result.getUpdatedAt());
        Assertions.assertTrue(
                matches.stream()
                        .filter(match -> match.getRoundNumber() == 1)
                        .allMatch(match -> TournamentMatchStatus.SCHEDULED == match.getStatus()));
    }

    @Test
    public void cannotDeclareWinnerBeforeInProgress() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise
        final TournamentBracketException exception =
                Assertions.assertThrows(
                        TournamentBracketException.class,
                        () ->
                                bracketService.declareWinner(
                                        10L,
                                        1000L,
                                        new TournamentWinnerDeclarationRequest(100L),
                                        tournament.getHost()));

        // 3. Assert
        Assertions.assertEquals(
                TournamentBracketFailureReason.NOT_IN_PROGRESS, exception.getReason());
    }

    @Test
    public void cannotDeclareUnknownTeamAsWinner() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournamentAndId(10L, matches.get(0).getId()))
                .thenReturn(Optional.of(matches.get(0)));

        // 2. Exercise
        final TournamentBracketException exception =
                Assertions.assertThrows(
                        TournamentBracketException.class,
                        () ->
                                bracketService.declareWinner(
                                        10L,
                                        matches.get(0).getId(),
                                        new TournamentWinnerDeclarationRequest(999L),
                                        tournament.getHost()));

        // 3. Assert
        Assertions.assertEquals(
                TournamentBracketFailureReason.WINNER_NOT_IN_MATCH, exception.getReason());
    }

    @Test
    public void winnerPropagatesToChildTeamA() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final TournamentMatch firstRoundMatch = matches.get(0);
        final TournamentMatch finalMatch = matches.get(2);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournamentAndId(10L, firstRoundMatch.getId()))
                .thenReturn(Optional.of(firstRoundMatch));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        bracketService.declareWinner(
                10L,
                firstRoundMatch.getId(),
                new TournamentWinnerDeclarationRequest(firstRoundMatch.getTeamA().getId()),
                tournament.getHost());

        // 3. Assert
        Assertions.assertSame(firstRoundMatch.getTeamA(), finalMatch.getTeamA());
        Assertions.assertNull(finalMatch.getTeamB());
        Assertions.assertEquals(TournamentMatchStatus.PENDING, finalMatch.getStatus());
    }

    @Test
    public void winnerPropagatesToChildTeamB() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final TournamentMatch secondRoundMatch = matches.get(1);
        final TournamentMatch finalMatch = matches.get(2);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournamentAndId(10L, secondRoundMatch.getId()))
                .thenReturn(Optional.of(secondRoundMatch));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        bracketService.declareWinner(
                10L,
                secondRoundMatch.getId(),
                new TournamentWinnerDeclarationRequest(secondRoundMatch.getTeamB().getId()),
                tournament.getHost());

        // 3. Assert
        Assertions.assertSame(secondRoundMatch.getTeamB(), finalMatch.getTeamB());
        Assertions.assertNull(finalMatch.getTeamA());
        Assertions.assertEquals(TournamentMatchStatus.PENDING, finalMatch.getStatus());
    }

    @Test
    public void parentLinksPropagateBothWinnersIntoFinalSlots() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final TournamentMatch firstRoundMatch = matches.get(0);
        final TournamentMatch secondRoundMatch = matches.get(1);
        final TournamentMatch finalMatch = matches.get(2);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournamentAndId(10L, firstRoundMatch.getId()))
                .thenReturn(Optional.of(firstRoundMatch));
        Mockito.when(tournamentMatchDao.findByTournamentAndId(10L, secondRoundMatch.getId()))
                .thenReturn(Optional.of(secondRoundMatch));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        bracketService.declareWinner(
                10L,
                firstRoundMatch.getId(),
                new TournamentWinnerDeclarationRequest(firstRoundMatch.getTeamA().getId()),
                tournament.getHost());
        bracketService.declareWinner(
                10L,
                secondRoundMatch.getId(),
                new TournamentWinnerDeclarationRequest(secondRoundMatch.getTeamA().getId()),
                tournament.getHost());

        // 3. Assert
        Assertions.assertSame(firstRoundMatch.getTeamA(), finalMatch.getTeamA());
        Assertions.assertSame(secondRoundMatch.getTeamA(), finalMatch.getTeamB());
        Assertions.assertEquals(TournamentMatchStatus.SCHEDULED, finalMatch.getStatus());
    }

    @Test
    public void finalWinnerCompletesTournament() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final TournamentMatch finalMatch = matches.get(2);
        finalMatch.setTeamA(matches.get(0).getTeamA());
        finalMatch.setTeamB(matches.get(1).getTeamA());
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournamentAndId(10L, finalMatch.getId()))
                .thenReturn(Optional.of(finalMatch));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final TournamentMatch result =
                bracketService.declareWinner(
                        10L,
                        finalMatch.getId(),
                        new TournamentWinnerDeclarationRequest(finalMatch.getTeamA().getId()),
                        tournament.getHost());

        // 3. Assert
        Assertions.assertSame(finalMatch.getTeamA(), result.getWinnerTeam());
        Assertions.assertEquals(TournamentMatchStatus.DONE, result.getStatus());
        Assertions.assertEquals(TournamentStatus.COMPLETED, tournament.getStatus());
        Assertions.assertEquals(FIXED_NOW, tournament.getCompletedAt());
        Assertions.assertEquals(FIXED_NOW, tournament.getUpdatedAt());
    }

    @Test
    public void walkoverAdvancesNonForfeitingTeam() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final TournamentMatch firstRoundMatch = matches.get(0);
        final TournamentMatch finalMatch = matches.get(2);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournamentAndId(10L, firstRoundMatch.getId()))
                .thenReturn(Optional.of(firstRoundMatch));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final TournamentMatch result =
                bracketService.recordWalkover(
                        10L,
                        firstRoundMatch.getId(),
                        firstRoundMatch.getTeamA().getId(),
                        tournament.getHost());

        // 3. Assert
        Assertions.assertSame(firstRoundMatch.getTeamB(), result.getWinnerTeam());
        Assertions.assertEquals(TournamentMatchStatus.WALKOVER, result.getStatus());
        Assertions.assertSame(firstRoundMatch.getTeamB(), finalMatch.getTeamA());
    }

    @Test
    public void bracketCanRunFromGenerationToCompletion() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final Tournament tournament = tournament(10L, host, 4, TournamentStatus.BRACKET_SETUP);
        final List<TournamentTeam> teams = teams(tournament, 4);
        final List<TournamentMatch> persistedMatches = new ArrayList<>();
        configureStatefulBracket(tournament, teams, persistedMatches);

        // 2. Exercise
        final List<TournamentMatch> generated = bracketService.generateBracket(10L, host);
        final List<TournamentMatchScheduleRequest> schedules =
                generated.stream()
                        .filter(match -> match.getRoundNumber() == 1)
                        .map(match -> schedule(match.getId()))
                        .toList();
        bracketService.publishBracket(10L, host, schedules);

        final TournamentMatch firstRoundFirst = persistedMatches.get(0);
        final TournamentMatch firstRoundSecond = persistedMatches.get(1);
        bracketService.declareWinner(
                10L,
                firstRoundFirst.getId(),
                new TournamentWinnerDeclarationRequest(firstRoundFirst.getTeamA().getId()),
                host);
        bracketService.declareWinner(
                10L,
                firstRoundSecond.getId(),
                new TournamentWinnerDeclarationRequest(firstRoundSecond.getTeamB().getId()),
                host);
        final TournamentMatch finalMatch = persistedMatches.get(2);
        final TournamentMatch decidedFinal =
                bracketService.declareWinner(
                        10L,
                        finalMatch.getId(),
                        new TournamentWinnerDeclarationRequest(finalMatch.getTeamA().getId()),
                        host);

        // 3. Assert
        Assertions.assertEquals(3, generated.size());
        Assertions.assertEquals(TournamentStatus.COMPLETED, tournament.getStatus());
        Assertions.assertEquals(FIXED_NOW, tournament.getCompletedAt());
        Assertions.assertSame(finalMatch.getTeamA(), decidedFinal.getWinnerTeam());
        Assertions.assertEquals(TournamentMatchStatus.DONE, decidedFinal.getStatus());
    }

    private void configureGenerate(final Tournament tournament, final List<TournamentTeam> teams) {
        configureGenerate(tournament, teams, true);
    }

    private void configureGenerate(
            final Tournament tournament,
            final List<TournamentTeam> teams,
            final boolean configureMatchCreation) {
        tournament.setPairingStrategy(TournamentPairingStrategy.RANDOM);
        Mockito.when(tournamentDao.findById(tournament.getId()))
                .thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDao.findByTournament(tournament.getId())).thenReturn(teams);
        Mockito.when(tournamentMatchDao.findByTournament(tournament.getId())).thenReturn(List.of());
        if (configureMatchCreation) {
            configureMatchCreation();
        }
    }

    private void configureMatchCreation() {
        final AtomicLong matchIds = new AtomicLong(1000L);
        Mockito.when(
                        tournamentMatchDao.create(
                                ArgumentMatchers.any(Tournament.class),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.nullable(TournamentTeam.class),
                                ArgumentMatchers.nullable(TournamentTeam.class),
                                ArgumentMatchers.any(TournamentMatchStatus.class),
                                ArgumentMatchers.nullable(TournamentMatch.class),
                                ArgumentMatchers.nullable(TournamentMatch.class)))
                .thenAnswer(
                        invocation ->
                                new TournamentMatch(
                                        matchIds.getAndIncrement(),
                                        invocation.getArgument(0),
                                        invocation.getArgument(1),
                                        invocation.getArgument(2),
                                        invocation.getArgument(3),
                                        invocation.getArgument(4),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        invocation.getArgument(5),
                                        invocation.getArgument(6),
                                        invocation.getArgument(7),
                                        FIXED_NOW,
                                        FIXED_NOW));
    }

    private void configureStatefulBracket(
            final Tournament tournament,
            final List<TournamentTeam> teams,
            final List<TournamentMatch> persistedMatches) {
        final AtomicLong matchIds = new AtomicLong(1000L);
        tournament.setPairingStrategy(TournamentPairingStrategy.RANDOM);
        Mockito.when(tournamentDao.findById(tournament.getId()))
                .thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDao.findByTournament(tournament.getId())).thenReturn(teams);
        Mockito.when(tournamentMatchDao.findByTournament(tournament.getId()))
                .thenReturn(persistedMatches);
        Mockito.when(
                        tournamentMatchDao.findByTournamentAndId(
                                ArgumentMatchers.eq(tournament.getId()),
                                ArgumentMatchers.anyLong()))
                .thenAnswer(
                        invocation -> {
                            final long matchId = invocation.getArgument(1);
                            return persistedMatches.stream()
                                    .filter(match -> match.getId() == matchId)
                                    .findFirst();
                        });
        Mockito.when(
                        tournamentMatchDao.create(
                                ArgumentMatchers.eq(tournament),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.nullable(TournamentTeam.class),
                                ArgumentMatchers.nullable(TournamentTeam.class),
                                ArgumentMatchers.any(TournamentMatchStatus.class),
                                ArgumentMatchers.nullable(TournamentMatch.class),
                                ArgumentMatchers.nullable(TournamentMatch.class)))
                .thenAnswer(
                        invocation -> {
                            final TournamentMatch match =
                                    new TournamentMatch(
                                            matchIds.getAndIncrement(),
                                            invocation.getArgument(0),
                                            invocation.getArgument(1),
                                            invocation.getArgument(2),
                                            invocation.getArgument(3),
                                            invocation.getArgument(4),
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            invocation.getArgument(5),
                                            invocation.getArgument(6),
                                            invocation.getArgument(7),
                                            FIXED_NOW,
                                            FIXED_NOW);
                            persistedMatches.add(match);
                            return match;
                        });
    }

    private static List<TournamentMatch> fourTeamBracket(final Tournament tournament) {
        final List<TournamentTeam> teams = teams(tournament, 4);
        final TournamentMatch first =
                match(1000L, tournament, 1, 0, teams.get(0), teams.get(1), null, null);
        final TournamentMatch second =
                match(1001L, tournament, 1, 1, teams.get(2), teams.get(3), null, null);
        final TournamentMatch finalMatch =
                match(1002L, tournament, 2, 0, null, null, first, second);
        return new ArrayList<>(List.of(first, second, finalMatch));
    }

    private static List<TournamentTeam> bracketTeams(final List<TournamentMatch> matches) {
        return List.of(
                matches.get(0).getTeamA(),
                matches.get(0).getTeamB(),
                matches.get(1).getTeamA(),
                matches.get(1).getTeamB());
    }

    private static TournamentMatchScheduleRequest schedule(final long matchId) {
        return new TournamentMatchScheduleRequest(
                matchId,
                FIXED_NOW.plusSeconds(3600),
                FIXED_NOW.plusSeconds(7200),
                "Club Court 1",
                -34.56,
                -58.45);
    }

    private static TournamentMatch match(
            final Long id,
            final Tournament tournament,
            final int roundNumber,
            final int matchIndex,
            final TournamentTeam teamA,
            final TournamentTeam teamB,
            final TournamentMatch parentMatchA,
            final TournamentMatch parentMatchB) {
        return new TournamentMatch(
                id,
                tournament,
                roundNumber,
                matchIndex,
                teamA,
                teamB,
                null,
                null,
                null,
                null,
                null,
                null,
                TournamentMatchStatus.PENDING,
                parentMatchA,
                parentMatchB,
                FIXED_NOW,
                FIXED_NOW);
    }

    private static List<TournamentTeam> teams(final Tournament tournament, final int count) {
        final List<TournamentTeam> teams = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            teams.add(
                    new TournamentTeam(
                            100L + index,
                            tournament,
                            "Team " + (index + 1),
                            TournamentTeamOrigin.SOLO_POOL,
                            null,
                            FIXED_NOW));
        }
        return teams;
    }

    private static Tournament tournament(
            final long id, final User host, final int bracketSize, final TournamentStatus status) {
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
                1,
                true,
                false,
                FIXED_NOW.plusSeconds(3600),
                FIXED_NOW.plusSeconds(7200),
                status,
                FIXED_NOW,
                FIXED_NOW);
    }

    private static void authenticateAdminMod() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "admin",
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN_MOD"))));
    }
}
