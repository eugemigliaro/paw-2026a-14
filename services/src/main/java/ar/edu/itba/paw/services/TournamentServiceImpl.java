package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.persistence.TournamentDao;
import ar.edu.itba.paw.services.exceptions.tournamentLifecycle.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TournamentServiceImpl implements TournamentService {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final List<Integer> SUPPORTED_BRACKET_SIZES = List.of(4, 8, 16);
    private static final String ADMIN_MOD_AUTHORITY = "ROLE_ADMIN_MOD";

    private final TournamentDao tournamentDao;
    private final TournamentMailService tournamentMailService;
    private final ImageService imageService;
    private final Clock clock;

    public TournamentServiceImpl(
            final TournamentDao tournamentDao,
            final TournamentMailService tournamentMailService,
            final ImageService imageService,
            final Clock clock) {
        this.tournamentDao = tournamentDao;
        this.tournamentMailService = tournamentMailService;
        this.imageService = imageService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Tournament createTournament(final User host, final CreateTournamentRequest request) {
        validateHost(host);
        validateRequest(request);

        return tournamentDao.create(
                host,
                request.getSport(),
                request.getTitle(),
                request.getDescription(),
                request.getAddress(),
                request.getLatitude(),
                request.getLongitude(),
                request.getStartsAt(),
                request.getEndsAt(),
                request.getPricePerPlayer(),
                imageService.resolveImageMetadata(request.getBannerImage()),
                request.getFormat(),
                request.getBracketSize(),
                request.getTeamSize(),
                request.isAllowSoloSignup(),
                request.isAllowTeamDraft(),
                request.getRegistrationOpensAt(),
                request.getRegistrationClosesAt(),
                TournamentStatus.REGISTRATION);
    }

    @Override
    public Optional<Tournament> findPublicTournament(final long tournamentId) {
        return tournamentDao.findPublicById(tournamentId);
    }

    @Override
    public Optional<Tournament> findTournamentForHost(final long tournamentId, final User host) {
        return tournamentDao
                .findById(tournamentId)
                .filter(tournament -> !tournament.isDeleted())
                .filter(tournament -> canMutate(tournament, host));
    }

    @Override
    public PaginatedResult<Tournament> searchPublicTournaments(
            final String query,
            final List<Sport> sport,
            final Instant startDate,
            final Instant endDate,
            final EventSort sort,
            final int page,
            final int pageSize,
            final ZoneId timezone,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final Double latitude,
            final Double longitude) {
        final EventSort sortFilter =
                hasCoordinates(latitude, longitude) ? sort : withoutDistance(sort);
        final DateRange dateRange = new DateRange(startDate, endDate);

        return paginate(
                page,
                pageSize,
                DEFAULT_PAGE_SIZE,
                safePageSize ->
                        tournamentDao.countPublicTournaments(
                                query,
                                sport,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice),
                (offset, safePageSize) ->
                        tournamentDao.findPublicTournaments(
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
    }

    @Override
    public PaginatedResult<Tournament> findDashboardTournaments(
            final User host,
            final Boolean upcoming,
            final Boolean includeHosted,
            final String query,
            final List<Sport> sport,
            final Instant startDate,
            final Instant endDate,
            final EventSort sort,
            final int page,
            final int pageSize,
            final ZoneId timezone,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final Double latitude,
            final Double longitude) {
        return paginate(
                page,
                pageSize,
                DEFAULT_PAGE_SIZE,
                safePageSize ->
                        tournamentDao.countDashboardTournaments(
                                host,
                                upcoming,
                                includeHosted,
                                query,
                                sport,
                                startDate,
                                endDate,
                                minPrice,
                                maxPrice),
                (offset, safePageSize) ->
                        tournamentDao.findDashboardTournaments(
                                host,
                                upcoming,
                                includeHosted,
                                query,
                                sport,
                                startDate,
                                endDate,
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
            throw new TournamentLifecycleNotEditableException(
                    "This tournament can no longer be edited");
        }

        validateUpdateRequest(request);

        tournament.setSport(request.getSport());
        tournament.setTitle(request.getTitle());
        tournament.setDescription(request.getDescription());
        tournament.setAddress(request.getAddress());
        tournament.setLatitude(request.getLatitude());
        tournament.setLongitude(request.getLongitude());
        tournament.setStartsAt(request.getStartsAt());
        tournament.setEndsAt(request.getEndsAt());
        tournament.setPricePerPlayer(request.getPricePerPlayer());
        if (!(request.getBannerImage() == null
                || request.getBannerImage().getContentLength() <= 0)) {
            tournament.setBannerImageMetadata(
                    imageService.resolveImageMetadata(request.getBannerImage()));
        }
        tournament.setBracketSize(request.getBracketSize());
        tournament.setTeamSize(request.getTeamSize());
        tournament.setRegistrationOpensAt(request.getRegistrationOpensAt());
        tournament.setRegistrationClosesAt(request.getRegistrationClosesAt());
        tournament.setUpdatedAt(Instant.now(clock));
        return tournamentDao.update(tournament);
    }

    @Override
    @Transactional
    public Tournament cancel(final long tournamentId, final User actingUser, final String reason) {
        final Tournament tournament = findByIdOrThrow(tournamentId);
        validateCanMutate(tournament, actingUser);

        if (TournamentStatus.COMPLETED == tournament.getStatus()) {
            throw new TournamentLifecycleNotCancellableException(
                    "This tournament cannot be cancelled");
        }

        if (TournamentStatus.CANCELLED == tournament.getStatus()) {
            return tournament;
        }

        final Instant now = Instant.now(clock);
        tournament.setStatus(TournamentStatus.CANCELLED);
        tournament.setCancelledAt(now);
        tournament.setCancelReason(reason);
        tournament.setUpdatedAt(now);
        final Tournament updatedTournament = tournamentDao.update(tournament);
        tournamentMailService.sendTournamentCancelledEmail(updatedTournament);
        return updatedTournament;
    }

    private Tournament findByIdOrThrow(final long tournamentId) {
        return tournamentDao
                .findById(tournamentId)
                .filter(tournament -> !tournament.isDeleted())
                .orElseThrow(() -> new TournamentLifecycleException("Tournament not found"));
    }

    private void validateCanMutate(final Tournament tournament, final User actingUser) {
        if (!canMutate(tournament, actingUser)) {
            throw new TournamentLifecycleForbiddenException(
                    "You don't have permission to perform this action");
        }
    }

    private boolean canMutate(final Tournament tournament, final User actingUser) {
        if (tournament == null || actingUser == null || actingUser.getId() == null) {
            return false;
        }
        return tournament.getHost().getId().equals(actingUser.getId()) || isAdminMod();
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

    private static boolean hasCoordinates(final Double latitude, final Double longitude) {
        return latitude != null && longitude != null;
    }

    private static EventSort withoutDistance(final EventSort sort) {
        return sort == EventSort.DISTANCE ? EventSort.SOONEST : sort;
    }

    private void validateHost(final User host) {
        if (host == null || host.getId() == null) {
            throw new TournamentLifecycleInvalidUserException(
                    "User must be authenticated to perform this action");
        }
    }

    private void validateRequest(final CreateTournamentRequest request) {
        if (request == null) {
            throw new TournamentLifecycleException("Request cannot be null");
        }
        validateCommonFields(
                request.getSport(),
                request.getTitle(),
                request.getAddress(),
                request.getStartsAt(),
                request.getEndsAt(),
                request.getPricePerPlayer(),
                request.getLatitude(),
                request.getLongitude());
        validateFormat(request.getFormat());
        validateBracketSize(request.getBracketSize());
        validateTeamSize(request.getTeamSize());
        validateJoinMode(request.isAllowSoloSignup(), request.isAllowTeamDraft());
        validateRegistrationWindow(
                request.getRegistrationOpensAt(), request.getRegistrationClosesAt());
        validateFutureRegistrationClose(request.getRegistrationClosesAt());
    }

    private void validateUpdateRequest(final UpdateTournamentRequest request) {
        if (request == null) {
            throw new TournamentLifecycleException("Request cannot be null");
        }
        validateCommonFields(
                request.getSport(),
                request.getTitle(),
                request.getAddress(),
                request.getStartsAt(),
                request.getEndsAt(),
                request.getPricePerPlayer(),
                request.getLatitude(),
                request.getLongitude());
        validateBracketSize(request.getBracketSize());
        validateTeamSize(request.getTeamSize());
        validateRegistrationWindow(
                request.getRegistrationOpensAt(), request.getRegistrationClosesAt());
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
            throw new TournamentLifecycleException("Tournament details are invalid");
        }
        if (pricePerPlayer != null && pricePerPlayer.signum() < 0) {
            throw new TournamentLifecycleException("Price per player cannot be negative");
        }
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new TournamentLifecycleInvalidScheduleException("Invalid tournament schedule");
        }
        if ((latitude == null) != (longitude == null)) {
            throw new TournamentLifecycleInvalidLocationException("Invalid tournament location");
        }
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new TournamentLifecycleInvalidLocationException("Invalid tournament location");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new TournamentLifecycleInvalidLocationException("Invalid tournament location");
        }
    }

    private void validateFormat(final TournamentFormat format) {
        if (TournamentFormat.SINGLE_ELIMINATION != format) {
            throw new TournamentLifecycleInvalidFormatException("Invalid tournament format");
        }
    }

    private void validateBracketSize(final int bracketSize) {
        if (!SUPPORTED_BRACKET_SIZES.contains(bracketSize)) {
            throw new TournamentLifecycleInvalidBracketSizeException(
                    "Invalid tournament bracket size");
        }
    }

    private void validateTeamSize(final int teamSize) {
        if (teamSize < 1) {
            throw new TournamentLifecycleInvalidTeamSizeException("Invalid tournament team size");
        }
    }

    private void validateJoinMode(final boolean allowSoloSignup, final boolean allowTeamDraft) {
        if (!allowSoloSignup && !allowTeamDraft) {
            throw new TournamentLifecycleInvalidJoinModeException("Invalid tournament join mode");
        }
    }

    private void validateRegistrationWindow(
            final Instant registrationOpensAt, final Instant registrationClosesAt) {
        if (registrationOpensAt == null
                || registrationClosesAt == null
                || !registrationClosesAt.isAfter(registrationOpensAt)) {
            throw new TournamentLifecycleInvalidRegistrationWindowException(
                    "Invalid tournament registration window");
        }
    }

    private void validateFutureRegistrationClose(final Instant registrationClosesAt) {
        if (!registrationClosesAt.isAfter(Instant.now(clock))) {
            throw new TournamentLifecycleInvalidRegistrationWindowException(
                    "Invalid tournament registration window");
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

    private record DateRange(Instant start, Instant endExclusive) {}
}
