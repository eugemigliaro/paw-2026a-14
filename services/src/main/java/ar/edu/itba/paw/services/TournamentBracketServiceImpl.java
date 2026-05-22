package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.persistence.TournamentDao;
import ar.edu.itba.paw.persistence.TournamentMatchDao;
import ar.edu.itba.paw.persistence.TournamentTeamDao;
import ar.edu.itba.paw.services.exceptions.TournamentBracketException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private final TournamentMailService tournamentMailService;
    private final MessageSource messageSource;
    private final Clock clock;

    public TournamentBracketServiceImpl(
            final TournamentDao tournamentDao,
            final TournamentTeamDao tournamentTeamDao,
            final TournamentMatchDao tournamentMatchDao,
            final TournamentMailService tournamentMailService,
            final MessageSource messageSource,
            final Clock clock) {
        this.tournamentDao = tournamentDao;
        this.tournamentTeamDao = tournamentTeamDao;
        this.tournamentMatchDao = tournamentMatchDao;
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

        final List<TournamentTeam> teams =
                seedOrder(tournamentTeamDao.findByTournament(tournamentId));
        validateTeamCount(tournament, teams);

        final List<TournamentMatch> createdMatches = createFixtures(tournament, teams);

        final Instant now = Instant.now(clock);
        tournament.setBracketGeneratedAt(now);
        tournament.setUpdatedAt(now);
        tournamentDao.update(tournament);
        return createdMatches;
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

        final List<TournamentMatch> roundOneMatches =
                matches.stream().filter(match -> match.getRoundNumber() == 1).toList();
        if (roundOneMatches.isEmpty()) {
            throw bracketException(
                    TournamentBracketFailureReason.BRACKET_NOT_GENERATED,
                    "tournament.bracket.error.notGenerated");
        }

        final Map<Long, TournamentMatchScheduleRequest> schedulesByMatch =
                schedulesByMatchId(schedules);
        validateOnlyRoundOneSchedules(roundOneMatches, schedulesByMatch);

        final Instant now = Instant.now(clock);
        for (final TournamentMatch match : roundOneMatches) {
            final TournamentMatchScheduleRequest schedule = schedulesByMatch.get(match.getId());
            if (schedule == null) {
                throw bracketException(
                        TournamentBracketFailureReason.MISSING_ROUND_ONE_SCHEDULE,
                        "tournament.bracket.error.missingRoundOneSchedule");
            }
            validateSchedule(schedule);
            applySchedule(match, schedule, now);
            tournamentMatchDao.update(match);
        }

        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournament.setStartedAt(now);
        tournament.setUpdatedAt(now);
        final Tournament updatedTournament = tournamentDao.update(tournament);
        tournamentMailService.sendBracketPublishedEmail(updatedTournament);
        return updatedTournament;
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
                tournament, teams, matches, viewerTeam, focusedMatch(matches, viewerTeam));
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

        final boolean completed = propagateWinner(tournament, updatedMatch, winner, now);
        tournamentMailService.sendMatchResultEmail(
                tournament, updatedMatch, winner, losingTeam(updatedMatch, winner));
        if (completed) {
            tournamentMailService.sendTournamentCompletedEmail(tournament, winner);
        }
        return updatedMatch;
    }

    @Override
    @Transactional
    public TournamentMatch recordWalkover(
            final long tournamentId,
            final long matchId,
            final long forfeitingTeamId,
            final User actingUser) {
        final Tournament tournament = findTournamentOrThrow(tournamentId);
        validateCanMutate(tournament, actingUser);
        requireInProgress(tournament);

        final TournamentMatch match = findMatchOrThrow(tournamentId, matchId);
        validateMatchReadyForDecision(match);
        final TournamentTeam advancingTeam = advancingTeam(match, forfeitingTeamId);

        final Instant now = Instant.now(clock);
        match.setWinnerTeam(advancingTeam);
        match.setStatus(TournamentMatchStatus.WALKOVER);
        match.setUpdatedAt(now);
        final TournamentMatch updatedMatch = tournamentMatchDao.update(match);

        final TournamentTeam forfeitingTeam = losingTeam(updatedMatch, advancingTeam);
        final boolean completed = propagateWinner(tournament, updatedMatch, advancingTeam, now);
        tournamentMailService.sendWalkoverEmail(
                tournament, updatedMatch, advancingTeam, forfeitingTeam);
        if (completed) {
            tournamentMailService.sendTournamentCompletedEmail(tournament, advancingTeam);
        }
        return updatedMatch;
    }

    private List<TournamentMatch> createFixtures(
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
        if (teams.size() < tournament.getBracketSize()) {
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

    private void validateOnlyRoundOneSchedules(
            final List<TournamentMatch> roundOneMatches,
            final Map<Long, TournamentMatchScheduleRequest> schedulesByMatch) {
        final Set<Long> roundOneMatchIds =
                roundOneMatches.stream().map(TournamentMatch::getId).collect(Collectors.toSet());
        if (!roundOneMatchIds.containsAll(schedulesByMatch.keySet())) {
            throw bracketException(
                    TournamentBracketFailureReason.INVALID_SCHEDULE,
                    "tournament.bracket.error.invalidSchedule");
        }
    }

    private void validateSchedule(final TournamentMatchScheduleRequest schedule) {
        if (schedule.getStartsAt() == null
                || schedule.getEndsAt() == null
                || !schedule.getEndsAt().isAfter(schedule.getStartsAt())
                || isBlank(schedule.getAddress())) {
            throw bracketException(
                    TournamentBracketFailureReason.INVALID_SCHEDULE,
                    "tournament.bracket.error.invalidSchedule");
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

    private TournamentTeam advancingTeam(final TournamentMatch match, final long forfeitingTeamId) {
        if (sameId(match.getTeamA(), forfeitingTeamId)) {
            return match.getTeamB();
        }
        if (sameId(match.getTeamB(), forfeitingTeamId)) {
            return match.getTeamA();
        }
        throw bracketException(
                TournamentBracketFailureReason.FORFEITING_TEAM_NOT_IN_MATCH,
                "tournament.bracket.error.forfeitingTeamNotInMatch");
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
            tournamentDao.update(tournament);
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
