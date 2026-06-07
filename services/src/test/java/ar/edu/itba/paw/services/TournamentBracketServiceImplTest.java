package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EloUpdatedResult;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserSportRating;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
public class TournamentBracketServiceImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T00:00:00Z");

    @Mock private TournamentDao tournamentDao;
    @Mock private TournamentTeamDao tournamentTeamDao;
    @Mock private TournamentMatchDao tournamentMatchDao;
    @Mock private UserSportRatingService userSportRatingService;
    @Mock private TournamentMailService tournamentMailService;
    @Mock private SecurityService securityService;
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
                        securityService,
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
    public void eloPairingMatchesHighestAgainstLowest() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        tournament.setPairingStrategy(TournamentPairingStrategy.ELO);
        final List<TournamentTeam> teams = teams(tournament, 4);
        final User topSeedPlayer = createUser(21L);
        final User secondSeedPlayer = createUser(22L);
        final User thirdSeedPlayer = createUser(23L);
        final User fourthSeedPlayer = createUser(24L);
        configureGenerate(tournament, teams);
        tournament.setPairingStrategy(TournamentPairingStrategy.ELO);
        Mockito.when(tournamentTeamDao.findMembersByTournament(10L))
                .thenReturn(
                        List.of(
                                member(teams.get(0), topSeedPlayer),
                                member(teams.get(1), secondSeedPlayer),
                                member(teams.get(2), thirdSeedPlayer),
                                member(teams.get(3), fourthSeedPlayer)));
        Mockito.when(userSportRatingService.getEffectiveElo(topSeedPlayer, Sport.FOOTBALL))
                .thenReturn(1600);
        Mockito.when(userSportRatingService.getEffectiveElo(secondSeedPlayer, Sport.FOOTBALL))
                .thenReturn(1200);
        Mockito.when(userSportRatingService.getEffectiveElo(thirdSeedPlayer, Sport.FOOTBALL))
                .thenReturn(1100);
        Mockito.when(userSportRatingService.getEffectiveElo(fourthSeedPlayer, Sport.FOOTBALL))
                .thenReturn(800);

        // 2. Exercise
        final List<TournamentMatch> matches =
                bracketService.generateBracket(10L, tournament.getHost());

        // 3. Assert
        Assertions.assertSame(teams.get(0), matches.get(0).getTeamA());
        Assertions.assertSame(teams.get(3), matches.get(0).getTeamB());
        Assertions.assertSame(teams.get(1), matches.get(1).getTeamA());
        Assertions.assertSame(teams.get(2), matches.get(1).getTeamB());
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
        final User actingUser = UserUtils.getUser(99L);
        Mockito.when(securityService.canActAsAdminMod(actingUser)).thenReturn(true);
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        configureGenerate(tournament, teams(tournament, 4));

        // 2. Exercise
        final List<TournamentMatch> matches = bracketService.generateBracket(10L, actingUser);

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
        final User actingUser = UserUtils.getUser(99L);
        Mockito.when(securityService.canActAsAdminMod(actingUser)).thenReturn(true);
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final Tournament result =
                bracketService.publishBracket(
                        10L,
                        actingUser,
                        List.of(
                                scheduleAt(
                                        matches.get(0).getId(),
                                        FIXED_NOW.plusSeconds(3600),
                                        FIXED_NOW.plusSeconds(7200)),
                                scheduleAt(
                                        matches.get(1).getId(),
                                        FIXED_NOW.plusSeconds(3600),
                                        FIXED_NOW.plusSeconds(7200)),
                                scheduleAt(
                                        matches.get(2).getId(),
                                        FIXED_NOW.plusSeconds(7500),
                                        FIXED_NOW.plusSeconds(9000))));

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.IN_PROGRESS, result.getStatus());
        Assertions.assertEquals(FIXED_NOW, result.getStartedAt());
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
        final User actingUser = UserUtils.getUser(99L);
        Mockito.when(securityService.canActAsAdminMod(actingUser)).thenReturn(true);
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
                        actingUser);

        // 3. Assert
        Assertions.assertSame(firstRoundMatch.getTeamA(), result.getWinnerTeam());
        Assertions.assertEquals(TournamentMatchStatus.DONE, result.getStatus());
    }

    @Test
    public void declareWinnerUpdatesEloForRatedSports() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final TournamentMatch firstRoundMatch = matches.get(0);
        final User winningPlayer = createUser(11L);
        final User losingPlayer = createUser(12L);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournamentAndId(10L, firstRoundMatch.getId()))
                .thenReturn(Optional.of(firstRoundMatch));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);
        Mockito.when(tournamentTeamDao.findMembersByTournament(10L))
                .thenReturn(
                        List.of(
                                member(firstRoundMatch.getTeamA(), winningPlayer),
                                member(firstRoundMatch.getTeamB(), losingPlayer)));
        final RecordingUserSportRatingService recordingRatingService =
                new RecordingUserSportRatingService();
        final TournamentBracketServiceImpl bracketService = bracketService(recordingRatingService);

        // 2. Exercise
        bracketService.declareWinner(
                10L,
                firstRoundMatch.getId(),
                new TournamentWinnerDeclarationRequest(firstRoundMatch.getTeamA().getId()),
                tournament.getHost());

        // 3. Assert
        Assertions.assertEquals(List.of(winningPlayer), recordingRatingService.winners);
        Assertions.assertEquals(List.of(losingPlayer), recordingRatingService.losers);
        Assertions.assertEquals(Sport.FOOTBALL, recordingRatingService.sport);
    }

    @Test
    public void declareWinnerSkipsEloForOtherSports() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        tournament.setSport(Sport.OTHER);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final TournamentMatch firstRoundMatch = matches.get(0);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournamentAndId(10L, firstRoundMatch.getId()))
                .thenReturn(Optional.of(firstRoundMatch));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);
        Mockito.lenient()
                .when(
                        userSportRatingService.applyMatchResult(
                                ArgumentMatchers.anyList(),
                                ArgumentMatchers.anyList(),
                                ArgumentMatchers.eq(Sport.OTHER)))
                .thenThrow(new AssertionError("ELO should not be updated for OTHER sports"));

        // 2. Exercise
        final TournamentMatch result =
                bracketService.declareWinner(
                        10L,
                        firstRoundMatch.getId(),
                        new TournamentWinnerDeclarationRequest(firstRoundMatch.getTeamA().getId()),
                        tournament.getHost());

        // 3. Assert
        Assertions.assertSame(firstRoundMatch.getTeamA(), result.getWinnerTeam());
        Assertions.assertEquals(TournamentMatchStatus.DONE, result.getStatus());
    }

    @Test
    public void declareWinnerSendsMatchResultEmailForNonFinalMatch() {
        // 1. Arrange
        final RecordingTournamentMailService recordingMailService =
                new RecordingTournamentMailService();
        bracketService = bracketService(recordingMailService);
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        tournament.setSport(Sport.OTHER);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final TournamentMatch firstRoundMatch = matches.get(0);
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
        Assertions.assertEquals(List.of("match-result"), recordingMailService.actions);
    }

    @Test
    public void declareWinnerSendsOnlyCompletedEmailForFinalMatch() {
        // 1. Arrange
        final RecordingTournamentMailService recordingMailService =
                new RecordingTournamentMailService();
        bracketService = bracketService(recordingMailService);
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        tournament.setSport(Sport.OTHER);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final TournamentMatch firstRoundMatch = matches.get(0);
        final TournamentMatch secondRoundMatch = matches.get(1);
        final TournamentMatch finalMatch = matches.get(2);
        firstRoundMatch.setWinnerTeam(firstRoundMatch.getTeamA());
        firstRoundMatch.setStatus(TournamentMatchStatus.DONE);
        secondRoundMatch.setWinnerTeam(secondRoundMatch.getTeamA());
        secondRoundMatch.setStatus(TournamentMatchStatus.DONE);
        finalMatch.setTeamA(firstRoundMatch.getTeamA());
        finalMatch.setTeamB(secondRoundMatch.getTeamA());
        finalMatch.setStatus(TournamentMatchStatus.SCHEDULED);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournamentAndId(10L, finalMatch.getId()))
                .thenReturn(Optional.of(finalMatch));
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        bracketService.declareWinner(
                10L,
                finalMatch.getId(),
                new TournamentWinnerDeclarationRequest(finalMatch.getTeamA().getId()),
                tournament.getHost());

        // 3. Assert
        Assertions.assertEquals(List.of("completed"), recordingMailService.actions);
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
    public void unrelatedUserCannotReadBracketBeforePublish() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));

        // 2. Exercise
        final TournamentBracketException exception =
                Assertions.assertThrows(
                        TournamentBracketException.class,
                        () -> bracketService.getBracket(10L, UserUtils.getUser(2L)));

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
    public void adminModCanInspectBracketBeforePublish() {
        // 1. Arrange
        final User actingUser = UserUtils.getUser(99L);
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.BRACKET_SETUP);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final List<TournamentTeam> teams = bracketTeams(matches);
        Mockito.when(securityService.canActAsAdminMod(actingUser)).thenReturn(true);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDao.findByTournament(10L)).thenReturn(teams);
        Mockito.when(tournamentTeamDao.findUserTeam(10L, actingUser.getId()))
                .thenReturn(Optional.empty());
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final TournamentBracketView view = bracketService.getBracket(10L, actingUser);

        // 3. Assert
        Assertions.assertSame(tournament, view.getTournament());
        Assertions.assertEquals(matches, view.getMatches());
        Assertions.assertFalse(view.isResultRecordable(matches.get(0).getId()));
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
    public void hostCanRecordEligibleBracketResultInReadState() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
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
        Assertions.assertTrue(view.isResultRecordable(matches.get(0).getId()));
        Assertions.assertFalse(view.isResultRecordable(matches.get(2).getId()));
    }

    @Test
    public void adminModCanRecordEligibleBracketResultInReadState() {
        // 1. Arrange
        final User actingUser = UserUtils.getUser(99L);
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final List<TournamentTeam> teams = bracketTeams(matches);
        Mockito.when(securityService.canActAsAdminMod(actingUser)).thenReturn(true);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDao.findByTournament(10L)).thenReturn(teams);
        Mockito.when(tournamentTeamDao.findUserTeam(10L, actingUser.getId()))
                .thenReturn(Optional.empty());
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final TournamentBracketView view = bracketService.getBracket(10L, actingUser);

        // 3. Assert
        Assertions.assertTrue(view.isResultRecordable(matches.get(0).getId()));
        Assertions.assertFalse(view.isResultRecordable(matches.get(2).getId()));
    }

    @Test
    public void unrelatedUserCannotRecordBracketResultInReadState() {
        // 1. Arrange
        final User viewer = UserUtils.getUser(50L);
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final List<TournamentTeam> teams = bracketTeams(matches);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDao.findByTournament(10L)).thenReturn(teams);
        Mockito.when(tournamentTeamDao.findUserTeam(10L, viewer.getId()))
                .thenReturn(Optional.empty());
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final TournamentBracketView view = bracketService.getBracket(10L, viewer);

        // 3. Assert
        Assertions.assertFalse(view.isResultRecordable(matches.get(0).getId()));
        Assertions.assertFalse(view.isResultRecordable(matches.get(2).getId()));
    }

    @Test
    public void alreadyDecidedMatchCannotRecordBracketResultInReadState() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final TournamentMatch decidedMatch = matches.get(0);
        decidedMatch.setWinnerTeam(decidedMatch.getTeamA());
        decidedMatch.setStatus(TournamentMatchStatus.DONE);
        final List<TournamentTeam> teams = bracketTeams(matches);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentTeamDao.findByTournament(10L)).thenReturn(teams);
        Mockito.when(tournamentTeamDao.findUserTeam(10L, tournament.getHost().getId()))
                .thenReturn(Optional.empty());
        Mockito.when(tournamentMatchDao.findByTournament(10L)).thenReturn(matches);

        // 2. Exercise
        final TournamentBracketView view = bracketService.getBracket(10L, tournament.getHost());

        // 3. Assert
        Assertions.assertFalse(view.isResultRecordable(decidedMatch.getId()));
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
    public void publishRejectsMissingMatchSchedules() {
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
                TournamentBracketFailureReason.MISSING_MATCH_SCHEDULE, exception.getReason());
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
                                scheduleAt(
                                        matches.get(0).getId(),
                                        FIXED_NOW.plusSeconds(3600),
                                        FIXED_NOW.plusSeconds(7200)),
                                scheduleAt(
                                        matches.get(1).getId(),
                                        FIXED_NOW.plusSeconds(3600),
                                        FIXED_NOW.plusSeconds(7200)),
                                scheduleAt(
                                        matches.get(2).getId(),
                                        FIXED_NOW.plusSeconds(7500),
                                        FIXED_NOW.plusSeconds(9000))));

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.IN_PROGRESS, result.getStatus());
        Assertions.assertEquals(FIXED_NOW, result.getStartedAt());
        Assertions.assertEquals(FIXED_NOW, result.getUpdatedAt());
        Assertions.assertTrue(
                matches.stream()
                        .allMatch(match -> TournamentMatchStatus.SCHEDULED == match.getStatus()));
    }

    @Test
    public void publishRejectsScheduleBeforeNow() {
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
                                        List.of(
                                                scheduleAt(
                                                        matches.get(0).getId(),
                                                        FIXED_NOW.minusSeconds(3600),
                                                        FIXED_NOW.plusSeconds(3600)),
                                                scheduleAt(
                                                        matches.get(1).getId(),
                                                        FIXED_NOW.plusSeconds(3600),
                                                        FIXED_NOW.plusSeconds(7200)),
                                                scheduleAt(
                                                        matches.get(2).getId(),
                                                        FIXED_NOW.plusSeconds(7500),
                                                        FIXED_NOW.plusSeconds(9000)))));

        // 3. Assert
        Assertions.assertEquals(
                TournamentBracketFailureReason.SCHEDULE_BEFORE_NOW, exception.getReason());
    }

    @Test
    public void publishRejectsInvalidRoundOrder() {
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
                                        List.of(
                                                scheduleAt(
                                                        matches.get(0).getId(),
                                                        FIXED_NOW.plusSeconds(3600),
                                                        FIXED_NOW.plusSeconds(7200)),
                                                scheduleAt(
                                                        matches.get(1).getId(),
                                                        FIXED_NOW.plusSeconds(3900),
                                                        FIXED_NOW.plusSeconds(7800)),
                                                scheduleAt(
                                                        matches.get(2).getId(),
                                                        FIXED_NOW.plusSeconds(5400),
                                                        FIXED_NOW.plusSeconds(9000)))));

        // 3. Assert
        Assertions.assertEquals(
                TournamentBracketFailureReason.INVALID_ROUND_ORDER, exception.getReason());
    }

    @Test
    public void publishAllowsSameRoundOverlap() {
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
                                scheduleAt(
                                        matches.get(0).getId(),
                                        FIXED_NOW.plusSeconds(3600),
                                        FIXED_NOW.plusSeconds(7200)),
                                scheduleAt(
                                        matches.get(1).getId(),
                                        FIXED_NOW.plusSeconds(3600),
                                        FIXED_NOW.plusSeconds(7200)),
                                scheduleAt(
                                        matches.get(2).getId(),
                                        FIXED_NOW.plusSeconds(7500),
                                        FIXED_NOW.plusSeconds(9000))));

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.IN_PROGRESS, result.getStatus());
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
    public void cannotDeclareWinnerForIncompleteMatch() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final TournamentMatch finalMatch = matches.get(2);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournamentAndId(10L, finalMatch.getId()))
                .thenReturn(Optional.of(finalMatch));

        // 2. Exercise
        final TournamentBracketException exception =
                Assertions.assertThrows(
                        TournamentBracketException.class,
                        () ->
                                bracketService.declareWinner(
                                        10L,
                                        finalMatch.getId(),
                                        new TournamentWinnerDeclarationRequest(100L),
                                        tournament.getHost()));

        // 3. Assert
        Assertions.assertEquals(
                TournamentBracketFailureReason.MATCH_NOT_READY, exception.getReason());
    }

    @Test
    public void cannotDeclareWinnerForAlreadyDecidedMatch() {
        // 1. Arrange
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), 4, TournamentStatus.IN_PROGRESS);
        final List<TournamentMatch> matches = fourTeamBracket(tournament);
        final TournamentMatch decidedMatch = matches.get(0);
        decidedMatch.setWinnerTeam(decidedMatch.getTeamA());
        decidedMatch.setStatus(TournamentMatchStatus.DONE);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentMatchDao.findByTournamentAndId(10L, decidedMatch.getId()))
                .thenReturn(Optional.of(decidedMatch));

        // 2. Exercise
        final TournamentBracketException exception =
                Assertions.assertThrows(
                        TournamentBracketException.class,
                        () ->
                                bracketService.declareWinner(
                                        10L,
                                        decidedMatch.getId(),
                                        new TournamentWinnerDeclarationRequest(
                                                decidedMatch.getTeamA().getId()),
                                        tournament.getHost()));

        // 3. Assert
        Assertions.assertEquals(
                TournamentBracketFailureReason.MATCH_ALREADY_DECIDED, exception.getReason());
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
                List.of(
                        scheduleAt(
                                generated.get(0).getId(),
                                FIXED_NOW.plusSeconds(3600),
                                FIXED_NOW.plusSeconds(7200)),
                        scheduleAt(
                                generated.get(1).getId(),
                                FIXED_NOW.plusSeconds(3600),
                                FIXED_NOW.plusSeconds(7200)),
                        scheduleAt(
                                generated.get(2).getId(),
                                FIXED_NOW.plusSeconds(7500),
                                FIXED_NOW.plusSeconds(9000)));
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
                                        matchIdFor(
                                                invocation.getArgument(1),
                                                invocation.getArgument(2)),
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
                                            matchIdFor(
                                                    invocation.getArgument(1),
                                                    invocation.getArgument(2)),
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

    private static long matchIdFor(final int roundNumber, final int matchIndex) {
        return 1000L + roundNumber * 100L + matchIndex;
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
        return scheduleAt(matchId, FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200));
    }

    private static TournamentMatchScheduleRequest scheduleAt(
            final long matchId, final Instant startsAt, final Instant endsAt) {
        return new TournamentMatchScheduleRequest(
                matchId, startsAt, endsAt, "Club Court 1", -34.56, -58.45);
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

    private static TournamentTeamMember member(final TournamentTeam team, final User user) {
        return new TournamentTeamMember(null, team, user, false, FIXED_NOW);
    }

    private static User createUser(final Long id) {
        return new User(id, "user" + id + "@test.com", "user" + id, null, null, null, null, "en");
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

    private TournamentBracketServiceImpl bracketService(
            final TournamentMailService tournamentMailService) {
        return new TournamentBracketServiceImpl(
                tournamentDao,
                tournamentTeamDao,
                tournamentMatchDao,
                userSportRatingService,
                tournamentMailService,
                securityService,
                messageSource,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    private TournamentBracketServiceImpl bracketService(
            final UserSportRatingService userSportRatingService) {
        return new TournamentBracketServiceImpl(
                tournamentDao,
                tournamentTeamDao,
                tournamentMatchDao,
                userSportRatingService,
                tournamentMailService,
                securityService,
                messageSource,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    private static class RecordingUserSportRatingService implements UserSportRatingService {

        private List<User> winners = List.of();
        private List<User> losers = List.of();
        private Sport sport;

        @Override
        public Optional<UserSportRating> findRating(final User user, final Sport sport) {
            return Optional.empty();
        }

        @Override
        public int getEffectiveElo(final User user, final Sport sport) {
            return 1000;
        }

        @Override
        public List<UserSportRating> findRatingsForUser(final User user) {
            return List.of();
        }

        @Override
        public EloUpdatedResult applyMatchResult(
                final List<User> winners, final List<User> losers, final Sport sport) {
            this.winners = List.copyOf(winners);
            this.losers = List.copyOf(losers);
            this.sport = sport;
            return new EloUpdatedResult(sport, List.of());
        }
    }

    private static class RecordingTournamentMailService implements TournamentMailService {

        private final List<String> actions = new ArrayList<>();

        @Override
        public void sendBracketPublishedEmail(final Tournament tournament) {
            actions.add("bracket-published");
        }

        @Override
        public void sendMatchResultEmail(
                final Tournament tournament,
                final TournamentMatch match,
                final TournamentTeam winner,
                final TournamentTeam loser) {
            actions.add("match-result");
        }

        @Override
        public void sendTournamentCompletedEmail(
                final Tournament tournament, final TournamentTeam champion) {
            actions.add("completed");
        }

        @Override
        public void sendTournamentCancelledEmail(final Tournament tournament) {
            actions.add("cancelled");
        }
    }
}
