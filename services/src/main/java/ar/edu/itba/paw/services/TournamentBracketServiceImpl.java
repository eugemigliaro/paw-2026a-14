package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import ar.edu.itba.paw.models.types.TournamentPairingStrategy;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.persistence.TournamentDao;
import ar.edu.itba.paw.persistence.TournamentMatchDao;
import ar.edu.itba.paw.persistence.TournamentTeamDao;
import ar.edu.itba.paw.services.exceptions.TournamentBracketException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TournamentBracketServiceImpl implements TournamentBracketService {

    private static final List<Integer> SUPPORTED_BRACKET_SIZES = List.of(4, 8, 16);
    private static final Set<TournamentStatus> PUBLIC_BRACKET_STATUSES =
            Set.of(
                    TournamentStatus.IN_PROGRESS,
                    TournamentStatus.COMPLETED,
                    TournamentStatus.CANCELLED);
    private static final String ADMIN_MOD_AUTHORITY = "ROLE_ADMIN_MOD";

    private final TournamentDao tournamentDao;
    private final TournamentTeamDao tournamentTeamDao;
    private final TournamentMatchDao tournamentMatchDao;
    private final UserSportRatingService userSportRatingService;
    private final TournamentMailService tournamentMailService;
    private final MessageSource messageSource;
    private final Clock clock;

    public TournamentBracketServiceImpl(
            final TournamentDao tournamentDao,
            final TournamentTeamDao tournamentTeamDao,
            final TournamentMatchDao tournamentMatchDao,
            final UserSportRatingService userSportRatingService,
            final TournamentMailService tournamentMailService,
            final MessageSource messageSource,
            final Clock clock) {
        this.tournamentDao = tournamentDao;
        this.tournamentTeamDao = tournamentTeamDao;
        this.tournamentMatchDao = tournamentMatchDao;
        this.userSportRatingService = userSportRatingService;
        this.tournamentMailService = tournamentMailService;
        this.messageSource = messageSource;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<TournamentMatch> generateBracket(final long tournamentId, final User actingUser) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        validateCanMutate(tournament, actingUser);
        requireBracketSetup(tournament);
        validateSupportedBracketSize(tournament);

        final List<TournamentMatch> existingMatches =
                tournamentMatchDao.findByTournament(tournamentId);
        if (!existingMatches.isEmpty()) {
            throw bracketException(
                    TournamentBracketFailureReason.BRACKET_ALREADY_GENERATED,
                    "tournament.bracket.error.alreadyGenerated");
        }

        final List<TournamentTeam> teams = orderedTeamsForStrategy(tournament);
        validateTeamCount(tournament, teams);

        final List<TournamentMatch> createdMatches = createFixtures(tournament, teams);

        final Instant now = Instant.now(clock);
        tournament.setBracketGeneratedAt(now);
        tournament.setUpdatedAt(now);
        return createdMatches;
    }

    @Override
    public List<TournamentTeam> listTeamsForSetup(final long tournamentId, final User actingUser) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        validateCanMutate(tournament, actingUser);
        requireBracketSetup(tournament);
        return tournamentTeamDao.findByTournament(tournamentId);
    }

    @Override
    @Transactional
    public Tournament updatePairingStrategy(
            final long tournamentId,
            final User actingUser,
            final TournamentPairingStrategy pairingStrategy) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        validateCanMutate(tournament, actingUser);
        requireBracketSetup(tournament);
        if (pairingStrategy == null) {
            throw bracketException(
                    TournamentBracketFailureReason.PAIRING_STRATEGY_REQUIRED,
                    "tournament.bracket.error.pairingStrategyRequired");
        }
        tournament.setPairingStrategy(pairingStrategy);
        tournament.setUpdatedAt(Instant.now(clock));
        return tournament;
    }

    @Override
    @Transactional
    public void saveManualPairings(
            final long tournamentId, final User actingUser, final List<Long> orderedTeamIds) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        validateCanMutate(tournament, actingUser);
        requireBracketSetup(tournament);
        if (tournament.getPairingStrategy() != TournamentPairingStrategy.MANUAL) {
            throw bracketException(
                    TournamentBracketFailureReason.INVALID_PAIRINGS,
                    "tournament.bracket.error.invalidPairings");
        }
        if (!tournamentMatchDao.findByTournament(tournamentId).isEmpty()) {
            throw bracketException(
                    TournamentBracketFailureReason.BRACKET_ALREADY_GENERATED,
                    "tournament.bracket.error.alreadyGenerated");
        }
        final List<TournamentTeam> teams =
                tournamentTeamDao.findByTournamentUnordered(tournamentId);
        validateManualPairings(teams, orderedTeamIds);
        tournamentTeamDao.saveSeedOrder(teams, orderedTeamIds);
        tournament.setUpdatedAt(Instant.now(clock));
    }

    @Override
    @Transactional
    public Tournament publishBracket(
            final long tournamentId,
            final User actingUser,
            final List<TournamentMatchScheduleRequest> schedules) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        validateCanMutate(tournament, actingUser);
        requireBracketSetup(tournament);

        final List<TournamentMatch> matches = tournamentMatchDao.findByTournament(tournamentId);
        if (matches.isEmpty()) {
            throw bracketException(
                    TournamentBracketFailureReason.BRACKET_NOT_GENERATED,
                    "tournament.bracket.error.notGenerated");
        }

        final Map<Long, TournamentMatchScheduleRequest> schedulesByMatch =
                schedulesByMatchId(schedules);
        validateScheduleCoverage(matches, schedulesByMatch);

        final Instant now = Instant.now(clock);
        validateRoundOrdering(matches, schedulesByMatch);
        for (final TournamentMatch match : matches) {
            final TournamentMatchScheduleRequest schedule = schedulesByMatch.get(match.getId());
            if (schedule == null) {
                throw bracketException(
                        TournamentBracketFailureReason.MISSING_MATCH_SCHEDULE,
                        "tournament.bracket.error.missingMatchSchedule");
            }
            validateSchedule(schedule, now);
            applySchedule(match, schedule, now);
            tournamentMatchDao.update(match);
        }

        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournament.setStartedAt(now);
        tournament.setUpdatedAt(now);
        tournamentMailService.sendBracketPublishedEmail(tournament);
        return tournament;
    }

    @Override
    public TournamentBracketView getBracket(final long tournamentId, final User viewer) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        if (!canReadBracket(tournament, viewer)) {
            throw bracketException(
                    TournamentBracketFailureReason.FORBIDDEN, "tournament.bracket.error.forbidden");
        }

        final List<TournamentTeam> teams = tournamentTeamDao.findByTournament(tournamentId);
        final List<TournamentMatch> matches = tournamentMatchDao.findByTournament(tournamentId);
        if (matches.isEmpty()) {
            throw bracketException(
                    TournamentBracketFailureReason.BRACKET_NOT_GENERATED,
                    "tournament.bracket.error.notGenerated");
        }

        final TournamentTeam viewerTeam =
                viewer == null || viewer.getId() == null
                        ? null
                        : tournamentTeamDao.findUserTeam(tournamentId, viewer.getId()).orElse(null);

        return new TournamentBracketView(
                tournament,
                teams,
                matches,
                viewerTeam,
                focusedMatch(matches, viewerTeam),
                tournamentTeamDao.findMembersByTournament(tournamentId));
    }

    @Override
    @Transactional
    public TournamentMatch declareWinner(
            final long tournamentId,
            final long matchId,
            final TournamentWinnerDeclarationRequest request,
            final User actingUser) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        validateCanMutate(tournament, actingUser);
        requireInProgress(tournament);

        final TournamentMatch match = findMatchOrThrow(tournamentId, matchId);
        validateMatchReadyForDecision(match);
        final TournamentTeam winner = winnerTeam(match, request);

        final Instant now = Instant.now(clock);
        match.setWinnerTeam(winner);
        match.setStatus(TournamentMatchStatus.DONE);
        match.setUpdatedAt(now);
        final TournamentMatch updatedMatch = tournamentMatchDao.update(match);

        if (tournament.getSport() != Sport.OTHER) {
            updateRatingsForMatchResult(tournament, updatedMatch);
        }

        final boolean completed = propagateWinner(tournament, updatedMatch, winner, now);
        if (completed) {
            tournamentMailService.sendTournamentCompletedEmail(tournament, winner);
        } else {
            tournamentMailService.sendMatchResultEmail(
                    tournament, updatedMatch, winner, losingTeam(updatedMatch, winner));
        }
        return updatedMatch;
    }

    private List<TournamentMatch> createFixtures(
            final Tournament tournament, final List<TournamentTeam> teams) {
        final int teamCount = teams.size();
        final int nearestPower = highestPowerOfTwoAtMost(teamCount);
        if (teamCount == nearestPower) {
            return createPowerOfTwoFixtures(tournament, teams);
        }

        final List<TournamentMatch> createdMatches = new ArrayList<>();
        final int playInTeamCount = 2 * (teamCount - nearestPower);
        final int playInMatches = playInTeamCount / 2;
        final List<TournamentMatch> previousRound = new ArrayList<>();

        for (int matchIndex = 0; matchIndex < playInMatches; matchIndex++) {
            final TournamentMatch match =
                    tournamentMatchDao.create(
                            tournament,
                            1,
                            matchIndex,
                            teams.get(matchIndex * 2),
                            teams.get(matchIndex * 2 + 1),
                            TournamentMatchStatus.PENDING,
                            null,
                            null);
            previousRound.add(match);
            createdMatches.add(match);
        }

        final List<BracketSlot> slots = new ArrayList<>();
        for (final TournamentMatch playIn : previousRound) {
            slots.add(BracketSlot.fromParent(playIn));
        }
        for (int index = playInTeamCount; index < teamCount; index++) {
            slots.add(BracketSlot.fromTeam(teams.get(index)));
        }

        List<TournamentMatch> currentParents =
                createRoundFromSlots(tournament, 2, slots, createdMatches);
        int roundNumber = 3;
        while (currentParents.size() > 1) {
            currentParents =
                    createRoundFromParents(tournament, roundNumber, currentParents, createdMatches);
            roundNumber++;
        }
        return createdMatches;
    }

    private List<TournamentMatch> createPowerOfTwoFixtures(
            final Tournament tournament, final List<TournamentTeam> teams) {
        final List<TournamentMatch> createdMatches = new ArrayList<>();
        List<TournamentMatch> previousRound = new ArrayList<>();

        for (int matchIndex = 0; matchIndex < teams.size() / 2; matchIndex++) {
            final TournamentMatch match =
                    tournamentMatchDao.create(
                            tournament,
                            1,
                            matchIndex,
                            teams.get(matchIndex * 2),
                            teams.get(matchIndex * 2 + 1),
                            TournamentMatchStatus.PENDING,
                            null,
                            null);
            previousRound.add(match);
            createdMatches.add(match);
        }

        int roundNumber = 2;
        while (previousRound.size() > 1) {
            final List<TournamentMatch> currentRound = new ArrayList<>();
            for (int matchIndex = 0; matchIndex < previousRound.size() / 2; matchIndex++) {
                final TournamentMatch match =
                        tournamentMatchDao.create(
                                tournament,
                                roundNumber,
                                matchIndex,
                                null,
                                null,
                                TournamentMatchStatus.PENDING,
                                previousRound.get(matchIndex * 2),
                                previousRound.get(matchIndex * 2 + 1));
                currentRound.add(match);
                createdMatches.add(match);
            }
            previousRound = currentRound;
            roundNumber++;
        }
        return createdMatches;
    }

    private List<TournamentMatch> createRoundFromSlots(
            final Tournament tournament,
            final int roundNumber,
            final List<BracketSlot> slots,
            final List<TournamentMatch> createdMatches) {
        final List<TournamentMatch> currentRound = new ArrayList<>();
        for (int matchIndex = 0; matchIndex < slots.size() / 2; matchIndex++) {
            final BracketSlot slotA = slots.get(matchIndex * 2);
            final BracketSlot slotB = slots.get(matchIndex * 2 + 1);
            final TournamentMatch match =
                    tournamentMatchDao.create(
                            tournament,
                            roundNumber,
                            matchIndex,
                            slotA.team,
                            slotB.team,
                            TournamentMatchStatus.PENDING,
                            slotA.parentMatch,
                            slotB.parentMatch);
            currentRound.add(match);
            createdMatches.add(match);
        }
        return currentRound;
    }

    private List<TournamentMatch> createRoundFromParents(
            final Tournament tournament,
            final int roundNumber,
            final List<TournamentMatch> parents,
            final List<TournamentMatch> createdMatches) {
        final List<TournamentMatch> currentRound = new ArrayList<>();
        for (int matchIndex = 0; matchIndex < parents.size() / 2; matchIndex++) {
            final TournamentMatch match =
                    tournamentMatchDao.create(
                            tournament,
                            roundNumber,
                            matchIndex,
                            null,
                            null,
                            TournamentMatchStatus.PENDING,
                            parents.get(matchIndex * 2),
                            parents.get(matchIndex * 2 + 1));
            currentRound.add(match);
            createdMatches.add(match);
        }
        return currentRound;
    }

    private Tournament findTournamentOrThrow(final long tournamentId) {
        return tournamentDao
                .findById(tournamentId)
                .filter(tournament -> !tournament.isDeleted())
                .orElseThrow(
                        () ->
                                bracketException(
                                        TournamentBracketFailureReason.TOURNAMENT_NOT_FOUND,
                                        "tournament.bracket.error.notFound"));
    }

    private TournamentMatch findMatchOrThrow(final long tournamentId, final long matchId) {
        return tournamentMatchDao
                .findByTournamentAndId(tournamentId, matchId)
                .orElseThrow(
                        () ->
                                bracketException(
                                        TournamentBracketFailureReason.MATCH_NOT_FOUND,
                                        "tournament.bracket.error.matchNotFound"));
    }

    private void validateCanMutate(final Tournament tournament, final User actingUser) {
        if (!canMutate(tournament, actingUser)) {
            throw bracketException(
                    TournamentBracketFailureReason.FORBIDDEN, "tournament.bracket.error.forbidden");
        }
    }

    private boolean canMutate(final Tournament tournament, final User actingUser) {
        if (tournament == null || actingUser == null || actingUser.getId() == null) {
            return false;
        }
        return tournament.getHost().getId().equals(actingUser.getId()) || isAdminMod();
    }

    private boolean canReadBracket(final Tournament tournament, final User viewer) {
        if (PUBLIC_BRACKET_STATUSES.contains(tournament.getStatus())) {
            return true;
        }
        return TournamentStatus.BRACKET_SETUP == tournament.getStatus()
                && canMutate(tournament, viewer);
    }

    private boolean isAdminMod() {
        final Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .filter(Objects::nonNull)
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_MOD_AUTHORITY::equals);
    }

    private void requireBracketSetup(final Tournament tournament) {
        if (TournamentStatus.BRACKET_SETUP != tournament.getStatus()) {
            throw bracketException(
                    TournamentBracketFailureReason.NOT_READY_FOR_BRACKET,
                    "tournament.bracket.error.notReady");
        }
    }

    private void requireInProgress(final Tournament tournament) {
        if (TournamentStatus.IN_PROGRESS != tournament.getStatus()) {
            throw bracketException(
                    TournamentBracketFailureReason.NOT_IN_PROGRESS,
                    "tournament.bracket.error.notInProgress");
        }
    }

    private void validateSupportedBracketSize(final Tournament tournament) {
        if (!SUPPORTED_BRACKET_SIZES.contains(tournament.getBracketSize())) {
            throw bracketException(
                    TournamentBracketFailureReason.NOT_READY_FOR_BRACKET,
                    "tournament.bracket.error.unsupportedSize");
        }
    }

    private void validateTeamCount(final Tournament tournament, final List<TournamentTeam> teams) {
        if (teams.size() < 2) {
            throw bracketException(
                    TournamentBracketFailureReason.UNDER_CAPACITY,
                    "tournament.bracket.error.underCapacity");
        }
        if (teams.size() > tournament.getBracketSize()) {
            throw bracketException(
                    TournamentBracketFailureReason.NOT_READY_FOR_BRACKET,
                    "tournament.bracket.error.teamCountMismatch");
        }
    }

    private List<TournamentTeam> orderedTeamsForStrategy(final Tournament tournament) {
        final List<TournamentTeam> teams = tournamentTeamDao.findByTournament(tournament.getId());
        final TournamentPairingStrategy strategy =
                tournament.getPairingStrategy() == null
                        ? TournamentPairingStrategy.RANDOM
                        : tournament.getPairingStrategy();
        if (strategy == TournamentPairingStrategy.RANDOM) {
            return randomOrder(teams);
        }
        if (strategy == TournamentPairingStrategy.ELO) {
            return eloOrder(tournament, teams);
        }
        final List<TournamentTeam> seeded = seedOrder(teams);
        validateManualSeedsPresent(seeded);
        return seeded;
    }

    private List<TournamentTeam> randomOrder(final List<TournamentTeam> teams) {
        final List<TournamentTeam> shuffled = new ArrayList<>(teams);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());
        return shuffled;
    }

    private List<TournamentTeam> eloOrder(
            final Tournament tournament, final List<TournamentTeam> teams) {
        if (tournament.getSport() == Sport.OTHER) {
            return seedOrder(teams);
        }
        final Map<Long, List<User>> membersByTeam =
                tournamentTeamDao.findMembersByTournament(tournament.getId()).stream()
                        .collect(
                                Collectors.groupingBy(
                                        member -> member.getTeam().getId(),
                                        Collectors.mapping(
                                                TournamentTeamMember::getUser,
                                                Collectors.toList())));
        return teams.stream()
                .sorted(
                        Comparator.comparingDouble(
                                        (TournamentTeam team) ->
                                                -averageElo(
                                                        membersByTeam.getOrDefault(
                                                                team.getId(), List.of()),
                                                        tournament.getSport()))
                                .thenComparing(
                                        team ->
                                                Optional.ofNullable(team.getSeedPosition())
                                                        .orElse(Integer.MAX_VALUE))
                                .thenComparing(TournamentTeam::getId))
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toList(), this::interleaveForBrackets));
    }

    private List<TournamentTeam> interleaveForBrackets(final List<TournamentTeam> sortedTeams) {
        final List<TournamentTeam> orderedTeams = new ArrayList<>(sortedTeams.size());
        int left = 0;
        int right = sortedTeams.size() - 1;
        while (left <= right) {
            orderedTeams.add(sortedTeams.get(left));
            left++;
            if (left <= right) {
                orderedTeams.add(sortedTeams.get(right));
                right--;
            }
        }
        return orderedTeams;
    }

    private double averageElo(final List<User> teamMembers, final Sport sport) {
        if (teamMembers.isEmpty()) {
            return 0;
        }
        return teamMembers.stream()
                .mapToInt(user -> userSportRatingService.getEffectiveElo(user, sport))
                .average()
                .orElse(0);
    }

    private List<TournamentTeam> seedOrder(final List<TournamentTeam> teams) {
        final boolean hasSeedPositions =
                teams.stream().anyMatch(team -> team.getSeedPosition() != null);
        if (!hasSeedPositions) {
            return List.copyOf(teams);
        }

        final Map<TournamentTeam, Integer> originalIndexes = new HashMap<>();
        for (int index = 0; index < teams.size(); index++) {
            originalIndexes.put(teams.get(index), index);
        }
        return teams.stream()
                .sorted(
                        Comparator.comparing(
                                        (TournamentTeam team) ->
                                                Optional.ofNullable(team.getSeedPosition())
                                                        .orElse(Integer.MAX_VALUE))
                                .thenComparingInt(originalIndexes::get))
                .toList();
    }

    private void validateManualSeedsPresent(final List<TournamentTeam> teams) {
        final Set<Integer> seen = new HashSet<>();
        for (final TournamentTeam team : teams) {
            final Integer seed = team.getSeedPosition();
            if (seed == null || seed <= 0 || !seen.add(seed)) {
                throw bracketException(
                        TournamentBracketFailureReason.MANUAL_PAIRINGS_REQUIRED,
                        "tournament.bracket.error.manualPairingsRequired");
            }
        }
    }

    private void validateManualPairings(
            final List<TournamentTeam> teams, final List<Long> orderedTeamIds) {
        if (orderedTeamIds == null || orderedTeamIds.size() != teams.size()) {
            throw bracketException(
                    TournamentBracketFailureReason.INVALID_PAIRINGS,
                    "tournament.bracket.error.invalidPairings");
        }
        final Set<Long> availableTeamIds =
                teams.stream().map(TournamentTeam::getId).collect(Collectors.toSet());
        final Set<Long> seen = new HashSet<>();
        for (final Long teamId : orderedTeamIds) {
            if (teamId == null || !availableTeamIds.contains(teamId) || !seen.add(teamId)) {
                throw bracketException(
                        TournamentBracketFailureReason.INVALID_PAIRINGS,
                        "tournament.bracket.error.invalidPairings");
            }
        }
    }

    private Map<Long, TournamentMatchScheduleRequest> schedulesByMatchId(
            final List<TournamentMatchScheduleRequest> schedules) {
        if (schedules == null) {
            return Map.of();
        }
        final Map<Long, TournamentMatchScheduleRequest> schedulesByMatch = new HashMap<>();
        for (final TournamentMatchScheduleRequest schedule : schedules) {
            if (schedule == null || schedule.getMatchId() <= 0) {
                throw bracketException(
                        TournamentBracketFailureReason.INVALID_SCHEDULE,
                        "tournament.bracket.error.invalidSchedule");
            }
            if (schedulesByMatch.put(schedule.getMatchId(), schedule) != null) {
                throw bracketException(
                        TournamentBracketFailureReason.INVALID_SCHEDULE,
                        "tournament.bracket.error.invalidSchedule");
            }
        }
        return schedulesByMatch;
    }

    private void validateScheduleCoverage(
            final List<TournamentMatch> matches,
            final Map<Long, TournamentMatchScheduleRequest> schedulesByMatch) {
        final Set<Long> matchIds =
                matches.stream().map(TournamentMatch::getId).collect(Collectors.toSet());
        if (!matchIds.containsAll(schedulesByMatch.keySet())) {
            throw bracketException(
                    TournamentBracketFailureReason.INVALID_SCHEDULE,
                    "tournament.bracket.error.invalidSchedule");
        }
        if (!schedulesByMatch.keySet().containsAll(matchIds)) {
            throw bracketException(
                    TournamentBracketFailureReason.MISSING_MATCH_SCHEDULE,
                    "tournament.bracket.error.missingMatchSchedule");
        }
    }

    private void validateSchedule(
            final TournamentMatchScheduleRequest schedule, final Instant now) {
        if (schedule.getStartsAt() == null
                || schedule.getEndsAt() == null
                || !schedule.getEndsAt().isAfter(schedule.getStartsAt())
                || isBlank(schedule.getAddress())) {
            throw bracketException(
                    TournamentBracketFailureReason.INVALID_SCHEDULE,
                    "tournament.bracket.error.invalidSchedule");
        }
        if (schedule.getStartsAt().isBefore(now)) {
            throw bracketException(
                    TournamentBracketFailureReason.SCHEDULE_BEFORE_NOW,
                    "tournament.bracket.error.beforeNow");
        }
        if ((schedule.getLatitude() == null) != (schedule.getLongitude() == null)) {
            throw bracketException(
                    TournamentBracketFailureReason.INVALID_SCHEDULE,
                    "tournament.bracket.error.invalidLocation");
        }
        if (schedule.getLatitude() != null
                && (schedule.getLatitude() < -90 || schedule.getLatitude() > 90)) {
            throw bracketException(
                    TournamentBracketFailureReason.INVALID_SCHEDULE,
                    "tournament.bracket.error.invalidLocation");
        }
        if (schedule.getLongitude() != null
                && (schedule.getLongitude() < -180 || schedule.getLongitude() > 180)) {
            throw bracketException(
                    TournamentBracketFailureReason.INVALID_SCHEDULE,
                    "tournament.bracket.error.invalidLocation");
        }
    }

    private void validateRoundOrdering(
            final List<TournamentMatch> matches,
            final Map<Long, TournamentMatchScheduleRequest> schedulesByMatch) {
        final Map<Integer, Instant> latestRoundEnd = new HashMap<>();
        for (final TournamentMatch match : matches) {
            final TournamentMatchScheduleRequest schedule = schedulesByMatch.get(match.getId());
            if (schedule == null || schedule.getEndsAt() == null) {
                continue;
            }
            latestRoundEnd.compute(
                    match.getRoundNumber(),
                    (round, existingEnd) ->
                            existingEnd == null || schedule.getEndsAt().isAfter(existingEnd)
                                    ? schedule.getEndsAt()
                                    : existingEnd);
        }

        for (final TournamentMatch match : matches) {
            final int previousRound = match.getRoundNumber() - 1;
            if (previousRound < 1) {
                continue;
            }
            final Instant previousRoundLatestEnd = latestRoundEnd.get(previousRound);
            if (previousRoundLatestEnd == null) {
                continue;
            }
            final TournamentMatchScheduleRequest schedule = schedulesByMatch.get(match.getId());
            if (schedule != null
                    && schedule.getStartsAt() != null
                    && schedule.getStartsAt().isBefore(previousRoundLatestEnd)) {
                throw bracketException(
                        TournamentBracketFailureReason.INVALID_ROUND_ORDER,
                        "tournament.bracket.error.invalidRoundOrder");
            }
        }
    }

    private void applySchedule(
            final TournamentMatch match,
            final TournamentMatchScheduleRequest schedule,
            final Instant now) {
        match.setScheduledStartsAt(schedule.getStartsAt());
        match.setScheduledEndsAt(schedule.getEndsAt());
        match.setAddress(schedule.getAddress());
        match.setLatitude(schedule.getLatitude());
        match.setLongitude(schedule.getLongitude());
        match.setStatus(TournamentMatchStatus.SCHEDULED);
        match.setUpdatedAt(now);
    }

    private void validateMatchReadyForDecision(final TournamentMatch match) {
        if (match.getTeamA() == null || match.getTeamB() == null) {
            throw bracketException(
                    TournamentBracketFailureReason.MATCH_NOT_READY,
                    "tournament.bracket.error.matchNotReady");
        }
        if (match.getWinnerTeam() != null) {
            throw bracketException(
                    TournamentBracketFailureReason.MATCH_ALREADY_DECIDED,
                    "tournament.bracket.error.matchAlreadyDecided");
        }
    }

    private TournamentTeam winnerTeam(
            final TournamentMatch match, final TournamentWinnerDeclarationRequest request) {
        if (request == null) {
            throw bracketException(
                    TournamentBracketFailureReason.WINNER_NOT_IN_MATCH,
                    "tournament.bracket.error.winnerNotInMatch");
        }
        if (sameId(match.getTeamA(), request.getWinnerTeamId())) {
            return match.getTeamA();
        }
        if (sameId(match.getTeamB(), request.getWinnerTeamId())) {
            return match.getTeamB();
        }
        throw bracketException(
                TournamentBracketFailureReason.WINNER_NOT_IN_MATCH,
                "tournament.bracket.error.winnerNotInMatch");
    }

    private boolean propagateWinner(
            final Tournament tournament,
            final TournamentMatch decidedMatch,
            final TournamentTeam winner,
            final Instant now) {
        final Optional<TournamentMatch> childMatch =
                findChildMatch(
                        tournament.getId(),
                        parent -> sameId(parent, Objects.requireNonNull(decidedMatch.getId())));
        if (childMatch.isEmpty()) {
            tournament.setStatus(TournamentStatus.COMPLETED);
            tournament.setCompletedAt(now);
            tournament.setUpdatedAt(now);
            return true;
        }

        final TournamentMatch child = childMatch.get();
        if (sameId(child.getParentMatchA(), decidedMatch.getId())) {
            child.setTeamA(winner);
        } else {
            child.setTeamB(winner);
        }
        if (child.getTeamA() != null
                && child.getTeamB() != null
                && TournamentMatchStatus.PENDING == child.getStatus()) {
            child.setStatus(TournamentMatchStatus.SCHEDULED);
        }
        child.setUpdatedAt(now);
        tournamentMatchDao.update(child);
        return false;
    }

    private TournamentTeam losingTeam(final TournamentMatch match, final TournamentTeam winner) {
        if (sameId(match.getTeamA(), Objects.requireNonNull(winner.getId()))) {
            return match.getTeamB();
        }
        return match.getTeamA();
    }

    private void updateRatingsForMatchResult(
            final Tournament tournament, final TournamentMatch match) {
        final Map<Long, List<User>> membersByTeam = membersByTeam(tournament.getId());
        userSportRatingService.applyMatchResult(
                membersByTeam.getOrDefault(match.getTeamA().getId(), List.of()),
                membersByTeam.getOrDefault(match.getTeamB().getId(), List.of()),
                tournament.getSport());
    }

    private Map<Long, List<User>> membersByTeam(final long tournamentId) {
        return tournamentTeamDao.findMembersByTournament(tournamentId).stream()
                .collect(
                        Collectors.groupingBy(
                                member -> member.getTeam().getId(),
                                Collectors.mapping(
                                        TournamentTeamMember::getUser, Collectors.toList())));
    }

    private Optional<TournamentMatch> findChildMatch(
            final long tournamentId, final Predicate<TournamentMatch> parentPredicate) {
        return tournamentMatchDao.findByTournament(tournamentId).stream()
                .filter(
                        match ->
                                parentPredicate.test(match.getParentMatchA())
                                        || parentPredicate.test(match.getParentMatchB()))
                .findFirst();
    }

    private TournamentMatch focusedMatch(
            final List<TournamentMatch> matches, final TournamentTeam viewerTeam) {
        if (viewerTeam != null) {
            final Optional<TournamentMatch> viewerMatch =
                    matches.stream()
                            .filter(match -> match.getWinnerTeam() == null)
                            .filter(match -> containsTeam(match, viewerTeam))
                            .findFirst();
            if (viewerMatch.isPresent()) {
                return viewerMatch.get();
            }
        }
        return matches.stream()
                .filter(match -> match.getWinnerTeam() == null)
                .findFirst()
                .orElseGet(() -> matches.isEmpty() ? null : matches.get(matches.size() - 1));
    }

    private boolean containsTeam(final TournamentMatch match, final TournamentTeam team) {
        return sameId(match.getTeamA(), Objects.requireNonNull(team.getId()))
                || sameId(match.getTeamB(), team.getId());
    }

    private static boolean sameId(final TournamentTeam team, final long teamId) {
        return team != null && team.getId() != null && team.getId() == teamId;
    }

    private static boolean sameId(final TournamentMatch match, final long matchId) {
        return match != null && match.getId() != null && match.getId() == matchId;
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private int highestPowerOfTwoAtMost(final int value) {
        int power = 1;
        while (power * 2 <= value) {
            power *= 2;
        }
        return power;
    }

    private static final class BracketSlot {
        private final TournamentTeam team;
        private final TournamentMatch parentMatch;

        private BracketSlot(final TournamentTeam team, final TournamentMatch parentMatch) {
            this.team = team;
            this.parentMatch = parentMatch;
        }

        private static BracketSlot fromTeam(final TournamentTeam team) {
            return new BracketSlot(team, null);
        }

        private static BracketSlot fromParent(final TournamentMatch parentMatch) {
            return new BracketSlot(null, parentMatch);
        }
    }

    private TournamentBracketException bracketException(
            final TournamentBracketFailureReason reason, final String messageCode) {
        return new TournamentBracketException(reason, message(messageCode));
    }

    private String message(final String code) {
        final Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(
                Objects.requireNonNull(code), null, code, Objects.requireNonNull(locale));
    }
}
