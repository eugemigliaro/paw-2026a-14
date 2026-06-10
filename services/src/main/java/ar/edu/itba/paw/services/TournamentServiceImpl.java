package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PlatformTime;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.tournament.*;
import ar.edu.itba.paw.models.exceptions.tournamentLifecycle.*;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.services.internal.TournamentDataService;
import ar.edu.itba.paw.services.utils.DistanceUtils;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TournamentServiceImpl implements TournamentService {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final List<Integer> SUPPORTED_BRACKET_SIZES = List.of(4, 8, 16);

    private final TournamentDataService tournamentDataService;
    private final TournamentRegistrationService tournamentRegistrationService;
    private final TournamentMailService tournamentMailService;
    private final ImageService imageService;
    private final SecurityService securityService;
    private final Clock clock;

    public TournamentServiceImpl(
            final TournamentDataService tournamentDataService,
            final TournamentRegistrationService tournamentRegistrationService,
            final TournamentMailService tournamentMailService,
            final ImageService imageService,
            final SecurityService securityService,
            final Clock clock) {
        this.tournamentDataService = tournamentDataService;
        this.tournamentRegistrationService = tournamentRegistrationService;
        this.tournamentMailService = tournamentMailService;
        this.imageService = imageService;
        this.securityService = securityService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Tournament createTournament(final User host, final CreateTournamentRequest request) {
        validateHost(host);
        validateRequest(request);
        final Instant startsAt = toInstant(request.getStartDate(), request.getStartTime());
        final Instant endsAt = toInstant(request.getEndDate(), request.getEndTime());

        return tournamentDataService.create(
                host,
                request.getSport(),
                request.getTitle(),
                request.getDescription(),
                request.getAddress(),
                request.getLatitude(),
                request.getLongitude(),
                startsAt,
                endsAt,
                request.getPricePerPlayer(),
                imageService.resolveImageMetadata(request.getBannerImage()),
                request.getFormat(),
                request.getBracketSize(),
                request.getTeamSize(),
                request.isAllowSoloSignup(),
                request.isAllowTeamDraft(),
                toInstant(request.getRegistrationOpensDate(), request.getRegistrationOpensTime()),
                toInstant(request.getRegistrationClosesDate(), request.getRegistrationClosesTime()),
                TournamentStatus.REGISTRATION);
    }

    @Override
    public Optional<Tournament> findPublicTournament(final long tournamentId) {
        return tournamentDataService.findPublicById(tournamentId);
    }

    @Override
    public Optional<Tournament> findTournamentForHost(final long tournamentId, final User host) {
        return tournamentDataService
                .findById(tournamentId)
                .filter(tournament -> !tournament.isDeleted())
                .filter(tournament -> canMutate(tournament, host));
    }

    @Override
    public Optional<Tournament> findEditableTournamentForHost(
            final long tournamentId, final User host) {
        return findTournamentForHost(tournamentId, host)
                .filter(tournament -> TournamentStatus.REGISTRATION == tournament.getStatus());
    }

    @Override
    public TournamentManagementPermissions getManagementPermissions(
            final Tournament tournament, final User actingUser) {
        final boolean canMutate = canMutate(tournament, actingUser);
        final TournamentStatus status = tournament == null ? null : tournament.getStatus();
        final boolean canCloseRegistration = TournamentStatus.REGISTRATION == status && canMutate;
        final boolean canEditTournament = TournamentStatus.REGISTRATION == status && canMutate;
        final boolean canCancelTournament =
                status != null
                        && TournamentStatus.COMPLETED != status
                        && TournamentStatus.CANCELLED != status
                        && canMutate;
        final boolean canManageBracket = TournamentStatus.BRACKET_SETUP == status && canMutate;
        final boolean canViewBracket =
                TournamentStatus.IN_PROGRESS == status
                        || TournamentStatus.COMPLETED == status
                        || TournamentStatus.CANCELLED == status;
        final boolean canDefineMatchDates = TournamentStatus.BRACKET_SETUP == status && canMutate;
        final boolean canManageResults = TournamentStatus.IN_PROGRESS == status && canMutate;
        return new TournamentManagementPermissions(
                canCloseRegistration,
                canEditTournament,
                canCancelTournament,
                canManageBracket,
                canViewBracket,
                canDefineMatchDates,
                canManageResults);
    }

    @Override
    public PaginatedResult<Tournament> searchPublicTournaments(
            final String query,
            final List<Sport> sport,
            final LocalDate startDate,
            final LocalDate endDate,
            final EventSort sort,
            final int page,
            final int pageSize,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final Double latitude,
            final Double longitude) {
        final EventSort sortFilter =
                hasCoordinates(latitude, longitude) ? sort : withoutDistance(sort);
        final DateRange dateRange = DateRange.of(startDate, endDate);

        final PaginatedResult<Tournament> result =
                paginate(
                        page,
                        pageSize,
                        DEFAULT_PAGE_SIZE,
                        safePageSize ->
                                tournamentDataService.countPublicTournaments(
                                        query,
                                        sport,
                                        dateRange.start(),
                                        dateRange.endExclusive(),
                                        minPrice,
                                        maxPrice),
                        (offset, safePageSize) ->
                                tournamentDataService.findPublicTournaments(
                                        query,
                                        sport,
                                        dateRange.start(),
                                        dateRange.endExclusive(),
                                        minPrice,
                                        maxPrice,
                                        sortFilter,
                                        latitude,
                                        longitude,
                                        offset,
                                        safePageSize));

        if (sort == EventSort.DISTANCE && hasCoordinates(latitude, longitude)) {
            hydrateDistances(result.getItems(), latitude, longitude);
        }

        return result;
    }

    @Override
    public PaginatedResult<Tournament> findDashboardTournaments(
            final User host,
            final Boolean upcoming,
            final Boolean includeHosted,
            final String query,
            final List<Sport> sport,
            final LocalDate startDate,
            final LocalDate endDate,
            final EventSort sort,
            final int page,
            final int pageSize,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final Double latitude,
            final Double longitude) {
        final DateRange dateRange = DateRange.of(startDate, endDate);
        return paginate(
                page,
                pageSize,
                DEFAULT_PAGE_SIZE,
                safePageSize ->
                        tournamentDataService.countDashboardTournaments(
                                host,
                                upcoming,
                                includeHosted,
                                query,
                                sport,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice),
                (offset, safePageSize) ->
                        tournamentDataService.findDashboardTournaments(
                                host,
                                upcoming,
                                includeHosted,
                                query,
                                sport,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice,
                                sort,
                                latitude,
                                longitude,
                                offset,
                                safePageSize));
    }

    @Override
    @Transactional
    public Tournament update(
            final long tournamentId, final User actingUser, final UpdateTournamentRequest request) {
        final Tournament tournament = findByIdOrThrow(tournamentId);
        validateCanMutate(tournament, actingUser);

        if (TournamentStatus.REGISTRATION != tournament.getStatus()) {
            throw new TournamentLifecycleNotEditableException();
        }

        validateUpdateRequest(request);

        tournament.setSport(request.getSport());
        tournament.setTitle(request.getTitle());
        tournament.setDescription(request.getDescription());
        tournament.setAddress(request.getAddress());
        tournament.setLatitude(request.getLatitude());
        tournament.setLongitude(request.getLongitude());
        tournament.setStartsAt(toInstant(request.getStartDate(), request.getStartTime()));
        tournament.setEndsAt(toInstant(request.getEndDate(), request.getEndTime()));
        tournament.setPricePerPlayer(request.getPricePerPlayer());
        if (!(request.getBannerImage() == null
                || request.getBannerImage().getContentLength() <= 0)) {
            tournament.setBannerImageMetadata(
                    imageService.resolveImageMetadata(request.getBannerImage()));
        }
        tournament.setBracketSize(request.getBracketSize());
        tournament.setTeamSize(request.getTeamSize());
        tournament.setRegistrationOpensAt(
                toInstant(request.getRegistrationOpensDate(), request.getRegistrationOpensTime()));
        tournament.setRegistrationClosesAt(
                toInstant(
                        request.getRegistrationClosesDate(), request.getRegistrationClosesTime()));
        tournament.setUpdatedAt(Instant.now(clock));
        return tournamentDataService.update(tournament);
    }

    @Override
    @Transactional
    public Tournament cancel(final long tournamentId, final User actingUser, final String reason) {
        final Tournament tournament = findByIdOrThrow(tournamentId);
        validateCanMutate(tournament, actingUser);

        if (TournamentStatus.COMPLETED == tournament.getStatus()) {
            throw new TournamentLifecycleNotCancellableException();
        }

        if (TournamentStatus.CANCELLED == tournament.getStatus()) {
            return tournament;
        }

        final Instant now = Instant.now(clock);
        tournament.setStatus(TournamentStatus.CANCELLED);
        tournament.setCancelledAt(now);
        tournament.setCancelReason(reason);
        tournament.setUpdatedAt(now);
        final Tournament updatedTournament = tournamentDataService.update(tournament);
        tournamentMailService.sendTournamentCancelledEmail(updatedTournament);
        return updatedTournament;
    }

    @Override
    public Set<Long> findParticipatingTournamentIds(
            final User user, final List<Long> tournamentIds) {
        if (user == null
                || user.getId() == null
                || tournamentIds == null
                || tournamentIds.isEmpty()) {
            return Set.of();
        }
        final List<Long> safeTournamentIds =
                tournamentIds.stream().filter(id -> id != null).distinct().toList();
        if (safeTournamentIds.isEmpty()) {
            return Set.of();
        }
        return tournamentDataService.findParticipatingTournamentIds(user, safeTournamentIds);
    }

    @Override
    public TournamentViewerCapabilities viewerCapabilities(
            final Tournament tournament, final User viewer) {
        if (tournament == null) {
            return new TournamentViewerCapabilities(
                    false, false, false, false, false, false, false, false, false, false, true,
                    false);
        }

        final Instant now = Instant.now(clock);
        final boolean registrationOpen = isRegistrationOpenNow(tournament, now);
        final boolean registrationNotStarted = isRegistrationNotStarted(tournament, now);
        final boolean canMutate = canMutate(tournament, viewer);
        final boolean canCloseRegistration =
                TournamentStatus.REGISTRATION == tournament.getStatus() && canMutate;
        final boolean canEditTournament =
                TournamentStatus.REGISTRATION == tournament.getStatus() && canMutate;
        final boolean canCancelTournament =
                TournamentStatus.COMPLETED != tournament.getStatus()
                        && TournamentStatus.CANCELLED != tournament.getStatus()
                        && canMutate;
        final boolean canManageBracket =
                TournamentStatus.BRACKET_SETUP == tournament.getStatus() && canMutate;
        final boolean canManageResults =
                TournamentStatus.IN_PROGRESS == tournament.getStatus() && canMutate;
        final boolean canViewBracket =
                TournamentStatus.IN_PROGRESS == tournament.getStatus()
                        || TournamentStatus.COMPLETED == tournament.getStatus()
                        || TournamentStatus.CANCELLED == tournament.getStatus();

        final boolean authenticatedViewer = viewer != null && viewer.getId() != null;
        final TournamentSoloEntryStatus soloStatus =
                tournamentRegistrationService
                        .findSoloEntry(tournament.getId(), viewer)
                        .map(entry -> entry.getStatus())
                        .orElse(null);
        final boolean userHasTeam =
                tournamentRegistrationService.findUserTeam(tournament.getId(), viewer).isPresent();
        final boolean canJoinSolo =
                authenticatedViewer
                        && registrationOpen
                        && tournament.isAllowSoloSignup()
                        && !tournamentRegistrationService.isSoloPoolFull(tournament.getId())
                        && !userHasTeam
                        && soloStatus != TournamentSoloEntryStatus.IN_POOL
                        && soloStatus != TournamentSoloEntryStatus.ASSIGNED;
        final boolean canLeaveSolo =
                authenticatedViewer
                        && registrationOpen
                        && soloStatus == TournamentSoloEntryStatus.IN_POOL;
        final boolean requiresLoginToJoin =
                !authenticatedViewer && registrationOpen && tournament.isAllowSoloSignup();
        final boolean closeRegistrationBlockedByCapacity =
                canCloseRegistration
                        && tournamentRegistrationService
                                .getRegistrationReadiness(tournament.getId())
                                .isCancellationRisk();
        final boolean closeRegistrationDisabled =
                !registrationOpen || closeRegistrationBlockedByCapacity;

        return new TournamentViewerCapabilities(
                canJoinSolo,
                canLeaveSolo,
                requiresLoginToJoin,
                registrationNotStarted,
                canCloseRegistration,
                canEditTournament,
                canCancelTournament,
                canManageBracket,
                canManageResults,
                canViewBracket,
                closeRegistrationDisabled,
                closeRegistrationBlockedByCapacity);
    }

    private Tournament findByIdOrThrow(final long tournamentId) {
        return tournamentDataService
                .findById(tournamentId)
                .filter(tournament -> !tournament.isDeleted())
                .orElseThrow(() -> new TournamentNotFoundException());
    }

    private void validateCanMutate(final Tournament tournament, final User actingUser) {
        if (!canMutate(tournament, actingUser)) {
            throw new TournamentForbiddenActionException();
        }
    }

    private boolean canMutate(final Tournament tournament, final User actingUser) {
        if (tournament == null || actingUser == null || actingUser.getId() == null) {
            return false;
        }
        return tournament.getHost().getId().equals(actingUser.getId())
                || securityService.canActAsAdminMod(actingUser);
    }

    private boolean isRegistrationOpenNow(final Tournament tournament, final Instant now) {
        final Instant opensAt = tournament.getRegistrationOpensAt();
        final Instant closesAt = tournament.getRegistrationClosesAt();
        return TournamentStatus.REGISTRATION == tournament.getStatus()
                && opensAt != null
                && closesAt != null
                && !now.isBefore(opensAt)
                && now.isBefore(closesAt);
    }

    private boolean isRegistrationNotStarted(final Tournament tournament, final Instant now) {
        final Instant opensAt = tournament.getRegistrationOpensAt();
        return TournamentStatus.REGISTRATION == tournament.getStatus()
                && opensAt != null
                && now.isBefore(opensAt);
    }

    private static boolean hasCoordinates(final Double latitude, final Double longitude) {
        return latitude != null && longitude != null;
    }

    private static Instant toInstant(final LocalDate date, final LocalTime time) {
        return date == null || time == null ? null : PlatformTime.toInstant(date, time);
    }

    private static EventSort withoutDistance(final EventSort sort) {
        return sort == EventSort.DISTANCE ? EventSort.SOONEST : sort;
    }

    private void validateHost(final User host) {
        if (host == null || host.getId() == null) {
            throw new IllegalArgumentException("exception.user.notNull");
        }
    }

    private void validateRequest(final CreateTournamentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("invalidRequest");
        }
        validateSport(request.getSport());
        validateCommonFields(
                request.getSport(),
                request.getTitle(),
                request.getAddress(),
                toInstant(request.getStartDate(), request.getStartTime()),
                toInstant(request.getEndDate(), request.getEndTime()),
                request.getPricePerPlayer(),
                request.getLatitude(),
                request.getLongitude());
        validateFormat(request.getFormat());
        validateBracketSize(request.getBracketSize());
        validateTeamSize(request.getTeamSize());
        validateJoinMode(request.isAllowSoloSignup(), request.isAllowTeamDraft());
        final Instant registrationOpensAt =
                toInstant(request.getRegistrationOpensDate(), request.getRegistrationOpensTime());
        final Instant registrationClosesAt =
                toInstant(request.getRegistrationClosesDate(), request.getRegistrationClosesTime());
        validateRegistrationWindow(registrationOpensAt, registrationClosesAt);
        validateFutureRegistrationClose(registrationClosesAt);
        validateSchedule(
                toInstant(request.getStartDate(), request.getStartTime()),
                toInstant(request.getEndDate(), request.getEndTime()),
                registrationClosesAt);
    }

    private void validateUpdateRequest(final UpdateTournamentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("invalidRequest");
        }
        validateSport(request.getSport());
        validateCommonFields(
                request.getSport(),
                request.getTitle(),
                request.getAddress(),
                toInstant(request.getStartDate(), request.getStartTime()),
                toInstant(request.getEndDate(), request.getEndTime()),
                request.getPricePerPlayer(),
                request.getLatitude(),
                request.getLongitude());
        validateBracketSize(request.getBracketSize());
        validateTeamSize(request.getTeamSize());
        validateRegistrationWindow(
                toInstant(request.getRegistrationOpensDate(), request.getRegistrationOpensTime()),
                toInstant(
                        request.getRegistrationClosesDate(), request.getRegistrationClosesTime()));
        validateSchedule(
                toInstant(request.getStartDate(), request.getStartTime()),
                toInstant(request.getEndDate(), request.getEndTime()),
                toInstant(
                        request.getRegistrationClosesDate(), request.getRegistrationClosesTime()));
    }

    private void validateCommonFields(
            final Sport sport,
            final String title,
            final String address,
            final Instant startsAt,
            final Instant endsAt,
            final BigDecimal pricePerPlayer,
            final Double latitude,
            final Double longitude) {
        if (sport == null || isBlank(title) || isBlank(address)) {
            throw new TournamentLifecycleException("invalidRequest");
        }
        if (pricePerPlayer != null && pricePerPlayer.signum() < 0) {
            throw new TournamentLifecycleException("invalidRequest");
        }
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new TournamentLifecycleInvalidScheduleException();
        }
        if ((latitude == null) != (longitude == null)) {
            throw new TournamentLifecycleInvalidLocationException();
        }
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new TournamentLifecycleInvalidLocationException();
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new TournamentLifecycleInvalidLocationException();
        }
    }

    private void validateSport(final Sport sport) {
        if (sport == null) {
            throw new TournamentLifecycleException("invalidSport");
        }
    }

    private void validateFormat(final TournamentFormat format) {
        if (TournamentFormat.SINGLE_ELIMINATION != format) {
            throw new TournamentLifecycleInvalidFormatException();
        }
    }

    private void validateBracketSize(final int bracketSize) {
        if (!SUPPORTED_BRACKET_SIZES.contains(bracketSize)) {
            throw new TournamentLifecycleInvalidBracketSizeException();
        }
    }

    private void validateTeamSize(final int teamSize) {
        if (teamSize < 1) {
            throw new TournamentLifecycleInvalidTeamSizeException();
        }
    }

    private void validateJoinMode(final boolean allowSoloSignup, final boolean allowTeamDraft) {
        if (!allowSoloSignup && !allowTeamDraft) {
            throw new TournamentLifecycleInvalidJoinModeException();
        }
    }

    private void validateRegistrationWindow(
            final Instant registrationOpensAt, final Instant registrationClosesAt) {
        if (registrationOpensAt == null
                || registrationClosesAt == null
                || !registrationClosesAt.isAfter(registrationOpensAt)) {
            throw new TournamentLifecycleInvalidRegistrationWindowException();
        }
    }

    private void validateSchedule(
            final Instant startsAt, final Instant endsAt, final Instant registrationClosesAt) {
        if (startsAt == null || endsAt == null) {
            throw new TournamentLifecycleInvalidScheduleException();
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new TournamentLifecycleInvalidScheduleException();
        }
        if (registrationClosesAt != null && !startsAt.isAfter(registrationClosesAt)) {
            throw new TournamentLifecycleInvalidScheduleException();
        }
    }

    private void validateFutureRegistrationClose(final Instant registrationClosesAt) {
        if (!registrationClosesAt.isAfter(Instant.now(clock))) {
            throw new TournamentLifecycleInvalidRegistrationWindowException();
        }
    }

    private void hydrateDistances(
            final List<Tournament> tournaments, final Double latitude, final Double longitude) {
        for (Tournament tournament : tournaments) {
            if (tournament.getLatitude() != null && tournament.getLongitude() != null) {
                tournament.setDistanceKmFromViewer(
                        DistanceUtils.distanceKm(
                                latitude,
                                longitude,
                                tournament.getLatitude(),
                                tournament.getLongitude()));
            }
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private PaginatedResult<Tournament> paginate(
            final int page,
            final int pageSize,
            final int defaultPageSize,
            final CountSupplier countSupplier,
            final SliceSupplier sliceSupplier) {
        final int safePage = page > 0 ? page : 1;
        final int safePageSize = pageSize > 0 ? pageSize : defaultPageSize;

        final int totalCount = countSupplier.count(safePageSize);
        final int totalPages = Math.max(1, (totalCount + safePageSize - 1) / safePageSize);
        final int clampedPage = Math.min(safePage, totalPages);
        final int offset = (clampedPage - 1) * safePageSize;
        final List<Tournament> items = sliceSupplier.items(offset, safePageSize);

        return new PaginatedResult<>(items, totalCount, clampedPage, safePageSize);
    }

    @FunctionalInterface
    private interface CountSupplier {
        int count(int safePageSize);
    }

    @FunctionalInterface
    private interface SliceSupplier {
        List<Tournament> items(int offset, int safePageSize);
    }

    private record DateRange(Instant start, Instant endExclusive) {
        static DateRange of(final LocalDate start, final LocalDate end) {
            return new DateRange(
                    start == null ? null : start.atStartOfDay(PlatformTime.ZONE).toInstant(),
                    end == null
                            ? null
                            : end.plusDays(1).atStartOfDay(PlatformTime.ZONE).toInstant());
        }
    }
}
