package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.TournamentSort;
import ar.edu.itba.paw.models.types.PersistableEnum;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.persistence.TournamentDao;
import ar.edu.itba.paw.persistence.TournamentSoloEntryDao;
import ar.edu.itba.paw.persistence.TournamentTeamDao;
import ar.edu.itba.paw.services.exceptions.TournamentLifecycleException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
    private final TournamentSoloEntryDao tournamentSoloEntryDao;
    private final TournamentTeamDao tournamentTeamDao;
    private final TournamentMailService tournamentMailService;
    private final MessageSource messageSource;
    private final Clock clock;

    public TournamentServiceImpl(
            final TournamentDao tournamentDao,
            final TournamentSoloEntryDao tournamentSoloEntryDao,
            final TournamentTeamDao tournamentTeamDao,
            final TournamentMailService tournamentMailService,
            final MessageSource messageSource,
            final Clock clock) {
        this.tournamentDao = tournamentDao;
        this.tournamentSoloEntryDao = tournamentSoloEntryDao;
        this.tournamentTeamDao = tournamentTeamDao;
        this.tournamentMailService = tournamentMailService;
        this.messageSource = messageSource;
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
                request.getBannerImageMetadata(),
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
            final String sport,
            final String startDate,
            final String endDate,
            final String sort,
            final int page,
            final int pageSize,
            final String timezone,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final Double latitude,
            final Double longitude) {
        final List<Sport> sportFilters = parseSports(sport);
        final TournamentSort sortFilter =
                hasCoordinates(latitude, longitude)
                        ? parseSort(sort)
                        : withoutDistance(parseSort(sort));
        final ZoneId zoneId = parseZone(timezone);
        final DateRange dateRange = parseDateRange(startDate, endDate, zoneId);

        return paginate(
                page,
                pageSize,
                DEFAULT_PAGE_SIZE,
                safePageSize ->
                        tournamentDao.countPublicTournaments(
                                query,
                                sportFilters,
                                dateRange.start(),
                                dateRange.endExclusive(),
                                minPrice,
                                maxPrice),
                (offset, safePageSize) ->
                        tournamentDao.findPublicTournaments(
                                query,
                                sportFilters,
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
    public PaginatedResult<Tournament> findHostedTournaments(
            final User host,
            final String query,
            final String sport,
            final String startDate,
            final String endDate,
            final String sort,
            final int page,
            final int pageSize,
            final String timezone,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        return findHostedTournaments(
                host, null, query, sport, startDate, endDate, sort, page, pageSize, timezone,
                minPrice, maxPrice);
    }

    public PaginatedResult<Tournament> findHostedTournaments(
            final User host,
            final boolean past,
            final String query,
            final String sport,
            final String startDate,
            final String endDate,
            final String sort,
            final int page,
            final int pageSize,
            final String timezone,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        return findHostedTournaments(
                host,
                Boolean.valueOf(past),
                query,
                sport,
                startDate,
                endDate,
                sort,
                page,
                pageSize,
                timezone,
                minPrice,
                maxPrice);
    }

    private PaginatedResult<Tournament> findHostedTournaments(
            final User host,
            final Boolean past,
            final String query,
            final String sport,
            final String startDate,
            final String endDate,
            final String sort,
            final int page,
            final int pageSize,
            final String timezone,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        validateHost(host);
        final ZoneId zoneId = parseZone(timezone);
        final DateRange dateRange = parseDateRange(startDate, endDate, zoneId);
        final List<Sport> sports = parseSports(sport);
        final TournamentSort sortFilter = withoutDistance(parseSort(sort));
        final String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        final LinkedHashMap<Long, Tournament> userTournaments = new LinkedHashMap<>();
        addTournaments(userTournaments, tournamentDao.findHostedByUser(host, 0, Integer.MAX_VALUE));
        addTournaments(userTournaments, tournamentSoloEntryDao.findTournamentsByUser(host));
        addTournaments(userTournaments, tournamentTeamDao.findTournamentsByMember(host));

        final List<Tournament> filtered =
                userTournaments.values().stream()
                        .filter(
                                tournament ->
                                        past == null
                                                || belongsToHostedContext(
                                                        tournament, past.booleanValue()))
                        .filter(tournament -> matchesQuery(tournament, normalizedQuery))
                        .filter(
                                tournament ->
                                        sports.isEmpty() || sports.contains(tournament.getSport()))
                        .filter(tournament -> withinDateRange(tournament, dateRange))
                        .filter(tournament -> withinPriceRange(tournament, minPrice, maxPrice))
                        .sorted(hostedComparator(sortFilter))
                        .toList();

        final int safePage = page > 0 ? page : 1;
        final int safePageSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
        final int totalPages = Math.max(1, (filtered.size() + safePageSize - 1) / safePageSize);
        final int clampedPage = Math.min(safePage, totalPages);
        final int fromIndex = Math.min((clampedPage - 1) * safePageSize, filtered.size());
        final int toIndex = Math.min(fromIndex + safePageSize, filtered.size());

        return new PaginatedResult<>(
                filtered.subList(fromIndex, toIndex), filtered.size(), clampedPage, safePageSize);
    }

    @Override
    @Transactional
    public Tournament update(
            final long tournamentId, final User actingUser, final UpdateTournamentRequest request) {
        final Tournament tournament = findByIdOrThrow(tournamentId);
        validateCanMutate(tournament, actingUser);

        if (TournamentStatus.REGISTRATION != tournament.getStatus()) {
            throw lifecycleException(
                    TournamentLifecycleFailureReason.NOT_EDITABLE,
                    "tournament.lifecycle.error.notEditable");
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
        tournament.setBannerImageMetadata(request.getBannerImageMetadata());
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
            throw lifecycleException(
                    TournamentLifecycleFailureReason.NOT_CANCELLABLE,
                    "tournament.lifecycle.error.notCancellable");
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
                .orElseThrow(
                        () ->
                                lifecycleException(
                                        TournamentLifecycleFailureReason.TOURNAMENT_NOT_FOUND,
                                        "tournament.lifecycle.error.notFound"));
    }

    private void validateCanMutate(final Tournament tournament, final User actingUser) {
        if (!canMutate(tournament, actingUser)) {
            throw lifecycleException(
                    TournamentLifecycleFailureReason.FORBIDDEN,
                    "tournament.lifecycle.error.forbidden");
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

    private static TournamentSort withoutDistance(final TournamentSort sort) {
        return sort == TournamentSort.DISTANCE ? TournamentSort.SOONEST : sort;
    }

    private static List<Sport> parseSports(final String rawSports) {
        if (rawSports == null || rawSports.isBlank()) {
            return List.of();
        }

        final LinkedHashSet<Sport> sports = new LinkedHashSet<>();
        for (final String rawSport : rawSports.split(",")) {
            if (rawSport == null || rawSport.isBlank()) {
                continue;
            }
            PersistableEnum.fromDbValue(Sport.class, rawSport.trim()).ifPresent(sports::add);
        }

        return List.copyOf(sports);
    }

    private static DateRange parseDateRange(
            final String rawStartDate, final String rawEndDate, final ZoneId zoneId) {
        final LocalDate startDate = parseDate(rawStartDate);
        final LocalDate endDate = parseDate(rawEndDate);

        if (startDate == null && endDate == null) {
            return new DateRange(null, null);
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            final Instant start = endDate.atStartOfDay(zoneId).toInstant();
            final Instant endExclusive = startDate.plusDays(1).atStartOfDay(zoneId).toInstant();
            return new DateRange(start, endExclusive);
        }

        final Instant start = startDate == null ? null : startDate.atStartOfDay(zoneId).toInstant();
        final Instant endExclusive =
                endDate == null ? null : endDate.plusDays(1).atStartOfDay(zoneId).toInstant();
        return new DateRange(start, endExclusive);
    }

    private static LocalDate parseDate(final String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(rawDate.trim());
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static TournamentSort parseSort(final String rawSort) {
        if (rawSort == null || rawSort.isBlank()) {
            return TournamentSort.SOONEST;
        }
        return TournamentSort.fromQueryValue(rawSort).orElse(TournamentSort.SOONEST);
    }

    private static ZoneId parseZone(final String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.systemDefault();
        }

        try {
            return ZoneId.of(timezone);
        } catch (final Exception ignored) {
            return ZoneId.systemDefault();
        }
    }

    private static boolean matchesQuery(final Tournament tournament, final String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return contains(tournament.getTitle(), query)
                || contains(tournament.getDescription(), query)
                || contains(tournament.getAddress(), query)
                || (tournament.getSport() != null
                        && tournament
                                .getSport()
                                .getDbValue()
                                .toLowerCase(Locale.ROOT)
                                .contains(query));
    }

    private static void addTournaments(
            final LinkedHashMap<Long, Tournament> target, final List<Tournament> tournaments) {
        if (tournaments == null) {
            return;
        }
        for (final Tournament tournament : tournaments) {
            if (tournament != null && tournament.getId() != null) {
                target.putIfAbsent(tournament.getId(), tournament);
            }
        }
    }

    private static boolean contains(final String value, final String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private static boolean withinDateRange(final Tournament tournament, final DateRange dateRange) {
        final Instant startsAt = tournament.getStartsAt();
        if (startsAt == null) {
            return dateRange.start() == null && dateRange.endExclusive() == null;
        }
        return (dateRange.start() == null || !startsAt.isBefore(dateRange.start()))
                && (dateRange.endExclusive() == null
                        || startsAt.isBefore(dateRange.endExclusive()));
    }

    private static boolean belongsToHostedContext(final Tournament tournament, final boolean past) {
        if (tournament == null || tournament.getStatus() == null) {
            return false;
        }
        if (past) {
            return TournamentStatus.COMPLETED == tournament.getStatus()
                    || TournamentStatus.CANCELLED == tournament.getStatus();
        }
        return TournamentStatus.REGISTRATION == tournament.getStatus()
                || TournamentStatus.BRACKET_SETUP == tournament.getStatus()
                || TournamentStatus.IN_PROGRESS == tournament.getStatus();
    }

    private static boolean withinPriceRange(
            final Tournament tournament, final BigDecimal minPrice, final BigDecimal maxPrice) {
        final BigDecimal price =
                tournament.getPricePerPlayer() == null
                        ? BigDecimal.ZERO
                        : tournament.getPricePerPlayer();
        return (minPrice == null || price.compareTo(minPrice) >= 0)
                && (maxPrice == null || price.compareTo(maxPrice) <= 0);
    }

    private static Comparator<Tournament> hostedComparator(final TournamentSort sort) {
        if (TournamentSort.PRICE == sort) {
            return Comparator.comparing(
                            (Tournament tournament) ->
                                    tournament.getPricePerPlayer() == null
                                            ? BigDecimal.ZERO
                                            : tournament.getPricePerPlayer())
                    .thenComparing(hostedSoonestComparator());
        }
        return hostedSoonestComparator();
    }

    private static Comparator<Tournament> hostedSoonestComparator() {
        return Comparator.comparing(
                        Tournament::getStartsAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Tournament::getId);
    }

    private void validateHost(final User host) {
        if (host == null || host.getId() == null) {
            throw new IllegalArgumentException(message("user.error.null"));
        }
    }

    private void validateRequest(final CreateTournamentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(message("tournament.lifecycle.error.invalid"));
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
            throw lifecycleException(
                    TournamentLifecycleFailureReason.NOT_EDITABLE,
                    "tournament.lifecycle.error.notEditable");
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
            throw lifecycleException(
                    TournamentLifecycleFailureReason.INVALID_DETAILS,
                    "tournament.lifecycle.error.invalidDetails");
        }
        if (pricePerPlayer != null && pricePerPlayer.signum() < 0) {
            throw lifecycleException(
                    TournamentLifecycleFailureReason.INVALID_DETAILS,
                    "tournament.lifecycle.error.invalidDetails");
        }
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw lifecycleException(
                    TournamentLifecycleFailureReason.INVALID_SCHEDULE,
                    "tournament.lifecycle.error.invalidSchedule");
        }
        if ((latitude == null) != (longitude == null)) {
            throw lifecycleException(
                    TournamentLifecycleFailureReason.INVALID_SCHEDULE,
                    "tournament.lifecycle.error.invalidLocation");
        }
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw lifecycleException(
                    TournamentLifecycleFailureReason.INVALID_SCHEDULE,
                    "tournament.lifecycle.error.invalidLocation");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw lifecycleException(
                    TournamentLifecycleFailureReason.INVALID_SCHEDULE,
                    "tournament.lifecycle.error.invalidLocation");
        }
    }

    private void validateFormat(final TournamentFormat format) {
        if (TournamentFormat.SINGLE_ELIMINATION != format) {
            throw lifecycleException(
                    TournamentLifecycleFailureReason.INVALID_FORMAT,
                    "tournament.lifecycle.error.invalidFormat");
        }
    }

    private void validateBracketSize(final int bracketSize) {
        if (!SUPPORTED_BRACKET_SIZES.contains(bracketSize)) {
            throw lifecycleException(
                    TournamentLifecycleFailureReason.INVALID_BRACKET_SIZE,
                    "tournament.lifecycle.error.invalidBracketSize");
        }
    }

    private void validateTeamSize(final int teamSize) {
        if (teamSize < 1) {
            throw lifecycleException(
                    TournamentLifecycleFailureReason.INVALID_TEAM_SIZE,
                    "tournament.lifecycle.error.invalidTeamSize");
        }
    }

    private void validateJoinMode(final boolean allowSoloSignup, final boolean allowTeamDraft) {
        if (!allowSoloSignup && !allowTeamDraft) {
            throw lifecycleException(
                    TournamentLifecycleFailureReason.INVALID_JOIN_MODE,
                    "tournament.lifecycle.error.invalidJoinMode");
        }
    }

    private void validateRegistrationWindow(
            final Instant registrationOpensAt, final Instant registrationClosesAt) {
        if (registrationOpensAt == null
                || registrationClosesAt == null
                || !registrationClosesAt.isAfter(registrationOpensAt)) {
            throw lifecycleException(
                    TournamentLifecycleFailureReason.INVALID_REGISTRATION_WINDOW,
                    "tournament.lifecycle.error.invalidRegistrationWindow");
        }
    }

    private void validateFutureRegistrationClose(final Instant registrationClosesAt) {
        if (!registrationClosesAt.isAfter(Instant.now(clock))) {
            throw lifecycleException(
                    TournamentLifecycleFailureReason.INVALID_REGISTRATION_WINDOW,
                    "tournament.lifecycle.error.invalidRegistrationWindow");
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private TournamentLifecycleException lifecycleException(
            final TournamentLifecycleFailureReason reason, final String messageCode) {
        return new TournamentLifecycleException(reason, message(messageCode));
    }

    private String message(final String code) {
        final Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(
                Objects.requireNonNull(code), null, code, Objects.requireNonNull(locale));
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
