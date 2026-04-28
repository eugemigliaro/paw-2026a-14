package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.CreateMatchRequest;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.MatchCancellationFailureReason;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.MatchUpdateFailureReason;
import ar.edu.itba.paw.services.PasswordResetPreview;
import ar.edu.itba.paw.services.RegisterAccountRequest;
import ar.edu.itba.paw.services.UpdateMatchRequest;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.VerificationConfirmationResult;
import ar.edu.itba.paw.services.VerificationFailureReason;
import ar.edu.itba.paw.services.VerificationPreview;
import ar.edu.itba.paw.services.VerificationPreviewDetail;
import ar.edu.itba.paw.services.VerificationRequestResult;
import ar.edu.itba.paw.services.exceptions.ImageUploadException;
import ar.edu.itba.paw.services.exceptions.MatchCancellationException;
import ar.edu.itba.paw.services.exceptions.MatchParticipationException;
import ar.edu.itba.paw.services.exceptions.MatchReservationException;
import ar.edu.itba.paw.services.exceptions.MatchUpdateException;
import ar.edu.itba.paw.services.exceptions.VerificationFailureException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.FilterGroupViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.MatchListControlsViewModel;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class PawUiRouteTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T00:00:00Z");

    private MockMvc mockMvc;
    private AtomicReference<String> lastSportsFilter;
    private AtomicReference<Long> lastReservedMatchId;
    private AtomicReference<Long> lastReservedUserId;
    private AtomicReference<Long> lastCancelledReservationMatchId;
    private AtomicReference<Long> lastCancelledReservationUserId;
    private AtomicReference<Long> lastHostCancelledMatchId;
    private AtomicReference<Long> lastHostCancelledUserId;
    private AtomicReference<Long> lastCancelledSeriesMatchId;
    private AtomicReference<Long> lastCancelledSeriesUserId;
    private AtomicReference<MatchReservationException> reservationFailure;
    private AtomicReference<MatchParticipationException> reservationCancellationFailure;
    private AtomicReference<MatchReservationException> seriesReservationFailure;
    private AtomicReference<MatchReservationException> seriesCancellationFailure;
    private AtomicReference<Boolean> currentUserHasReservation;
    private AtomicReference<Boolean> currentUserHasSeriesReservation;
    private AtomicReference<CreateMatchRequest> lastCreateMatchRequest;
    private AtomicReference<UpdateMatchRequest> lastUpdateMatchRequest;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        final MessageSource messageSource = messageSource();
        final LocalValidatorFactoryBean validator = validator(messageSource);

        lastSportsFilter = new AtomicReference<>();
        lastReservedMatchId = new AtomicReference<>();
        lastReservedUserId = new AtomicReference<>();
        lastCancelledReservationMatchId = new AtomicReference<>();
        lastCancelledReservationUserId = new AtomicReference<>();
        lastHostCancelledMatchId = new AtomicReference<>();
        lastHostCancelledUserId = new AtomicReference<>();
        lastCancelledSeriesMatchId = new AtomicReference<>();
        lastCancelledSeriesUserId = new AtomicReference<>();
        reservationFailure = new AtomicReference<>();
        reservationCancellationFailure = new AtomicReference<>();
        seriesReservationFailure = new AtomicReference<>();
        seriesCancellationFailure = new AtomicReference<>();
        currentUserHasReservation = new AtomicReference<>(false);
        currentUserHasSeriesReservation = new AtomicReference<>(false);
        lastCreateMatchRequest = new AtomicReference<>();
        lastUpdateMatchRequest = new AtomicReference<>();

        final Match realMatch =
                new Match(
                        42L,
                        Sport.PADEL,
                        7L,
                        "Downtown Club",
                        "Sunrise Padel",
                        "Friendly\\n doubles session",
                        Instant.parse("2026-04-06T10:00:00Z"),
                        Instant.parse("2026-04-06T12:00:00Z"),
                        8,
                        BigDecimal.TEN,
                        "public",
                        "open",
                        2,
                        null);
        final Match footballMatch =
                new Match(
                        43L,
                        Sport.FOOTBALL,
                        8L,
                        "North Arena",
                        "Afterwork Football",
                        "Fast 5v5",
                        Instant.parse("2026-04-07T19:00:00Z"),
                        Instant.parse("2026-04-07T20:30:00Z"),
                        10,
                        BigDecimal.ZERO,
                        "public",
                        "open",
                        4,
                        null);
        final Match completedMatch =
                new Match(
                        44L,
                        Sport.BASKETBALL,
                        7L,
                        "South Sports Center",
                        "Weekend Basketball",
                        "Completed tournament",
                        Instant.parse("2026-04-03T19:00:00Z"),
                        Instant.parse("2026-04-03T21:00:00Z"),
                        10,
                        BigDecimal.ZERO,
                        "public",
                        "completed",
                        10,
                        null);
        final Match cancelledFutureMatch =
                new Match(
                        45L,
                        Sport.TENNIS,
                        7L,
                        "City Tennis Club",
                        "Sunday Tennis",
                        "Cancelled due to weather",
                        Instant.parse("2026-04-08T12:00:00Z"),
                        Instant.parse("2026-04-08T14:00:00Z"),
                        6,
                        BigDecimal.TEN,
                        "public",
                        "cancelled",
                        2,
                        null);
        final Match privateInviteOnlyMatch =
                new Match(
                        51L,
                        Sport.PADEL,
                        7L,
                        "Members Club",
                        "Invite Night Padel",
                        "Private doubles session",
                        Instant.parse("2026-04-10T21:00:00Z"),
                        Instant.parse("2026-04-10T22:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        "private",
                        "invite_only",
                        "open",
                        2,
                        null);
        final Match recurringMatch =
                new Match(
                        46L,
                        Sport.PADEL,
                        7L,
                        "Downtown Club",
                        "Weekly Padel",
                        "Friendly recurring session",
                        Instant.parse("2026-04-09T18:00:00Z"),
                        Instant.parse("2026-04-09T19:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        "public",
                        "direct",
                        "open",
                        1,
                        null,
                        600L,
                        1);
        final Match recurringSecondOccurrence =
                new Match(
                        47L,
                        Sport.PADEL,
                        7L,
                        "Downtown Club",
                        "Weekly Padel",
                        "Friendly recurring session",
                        Instant.parse("2026-04-16T18:00:00Z"),
                        Instant.parse("2026-04-16T19:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        "public",
                        "direct",
                        "open",
                        0,
                        null,
                        600L,
                        2);
        final Match recurringPastOccurrence =
                new Match(
                        48L,
                        Sport.PADEL,
                        7L,
                        "Downtown Club",
                        "Weekly Padel",
                        "Friendly recurring session",
                        Instant.parse("2026-03-26T18:00:00Z"),
                        Instant.parse("2026-03-26T19:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        "public",
                        "direct",
                        "open",
                        4,
                        null,
                        600L,
                        0);
        final Match recurringFullOccurrence =
                new Match(
                        49L,
                        Sport.PADEL,
                        7L,
                        "Downtown Club",
                        "Weekly Padel",
                        "Friendly recurring session",
                        Instant.parse("2026-04-23T18:00:00Z"),
                        Instant.parse("2026-04-23T19:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        "public",
                        "direct",
                        "open",
                        8,
                        null,
                        600L,
                        3);
        final Match recurringCancelledOccurrence =
                new Match(
                        50L,
                        Sport.PADEL,
                        7L,
                        "Downtown Club",
                        "Weekly Padel",
                        "Friendly recurring session",
                        Instant.parse("2026-04-30T18:00:00Z"),
                        Instant.parse("2026-04-30T19:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        "public",
                        "direct",
                        "cancelled",
                        0,
                        null,
                        600L,
                        4);

        final MatchService matchService =
                new MatchService() {
                    @Override
                    public Match createMatch(final CreateMatchRequest request) {
                        lastCreateMatchRequest.set(request);
                        return new Match(
                                43L,
                                request.getSport(),
                                request.getHostUserId(),
                                request.getAddress(),
                                request.getTitle(),
                                request.getDescription(),
                                request.getStartsAt(),
                                request.getEndsAt(),
                                request.getMaxPlayers(),
                                request.getPricePerPlayer(),
                                request.getVisibility(),
                                request.getJoinPolicy(),
                                request.getStatus(),
                                0,
                                null,
                                request.isRecurring() ? 600L : null,
                                request.isRecurring() ? 1 : null);
                    }

                    @Override
                    public Optional<Match> findMatchById(final Long matchId) {
                        if (matchId == 42L) {
                            return Optional.of(realMatch);
                        }
                        if (matchId == 43L) {
                            return Optional.of(footballMatch);
                        }
                        if (matchId == 44L) {
                            return Optional.of(completedMatch);
                        }
                        if (matchId == 45L) {
                            return Optional.of(cancelledFutureMatch);
                        }
                        if (matchId == 51L) {
                            return Optional.of(privateInviteOnlyMatch);
                        }
                        if (matchId == 46L) {
                            return Optional.of(recurringMatch);
                        }
                        if (matchId == 47L) {
                            return Optional.of(recurringSecondOccurrence);
                        }
                        if (matchId == 48L) {
                            return Optional.of(recurringPastOccurrence);
                        }
                        if (matchId == 49L) {
                            return Optional.of(recurringFullOccurrence);
                        }
                        if (matchId == 50L) {
                            return Optional.of(recurringCancelledOccurrence);
                        }
                        return Optional.empty();
                    }

                    @Override
                    public Optional<Match> findPublicMatchById(final Long matchId) {
                        return findMatchById(matchId);
                    }

                    @Override
                    public List<Match> findSeriesOccurrences(final Long seriesId) {
                        return Long.valueOf(600L).equals(seriesId)
                                ? List.of(
                                        recurringPastOccurrence,
                                        recurringMatch,
                                        recurringSecondOccurrence,
                                        recurringFullOccurrence,
                                        recurringCancelledOccurrence)
                                : List.of();
                    }

                    @Override
                    public List<User> findConfirmedParticipants(final Long matchId) {
                        return matchId == 42L
                                ? List.of(
                                        new User(
                                                2L,
                                                "first@test.com",
                                                "first-player",
                                                "First",
                                                "Player",
                                                null,
                                                77L),
                                        new User(3L, "second@test.com", "second-player"))
                                : List.of();
                    }

                    @Override
                    public Match updateMatch(
                            final Long matchId,
                            final Long actingUserId,
                            final UpdateMatchRequest request) {
                        if (matchId != 42L) {
                            throw new MatchUpdateException(
                                    MatchUpdateFailureReason.MATCH_NOT_FOUND, "Missing match");
                        }
                        if (actingUserId != 7L) {
                            throw new MatchUpdateException(
                                    MatchUpdateFailureReason.FORBIDDEN, "Forbidden");
                        }
                        if (request.getMaxPlayers() < 2) {
                            throw new MatchUpdateException(
                                    MatchUpdateFailureReason.CAPACITY_BELOW_CONFIRMED,
                                    "Capacity too low");
                        }
                        lastUpdateMatchRequest.set(request);
                        return new Match(
                                matchId,
                                request.getSport(),
                                actingUserId,
                                request.getAddress(),
                                request.getTitle(),
                                request.getDescription(),
                                request.getStartsAt(),
                                request.getEndsAt(),
                                request.getMaxPlayers(),
                                request.getPricePerPlayer(),
                                request.getVisibility(),
                                request.getJoinPolicy(),
                                request.getStatus(),
                                0,
                                request.getBannerImageId());
                    }

                    @Override
                    public Match cancelMatch(final Long matchId, final Long actingUserId) {
                        if (matchId != 42L && matchId != 47L) {
                            throw new MatchCancellationException(
                                    MatchCancellationFailureReason.MATCH_NOT_FOUND,
                                    "Missing match");
                        }
                        if (actingUserId != 7L) {
                            throw new MatchCancellationException(
                                    MatchCancellationFailureReason.FORBIDDEN, "Forbidden");
                        }
                        lastHostCancelledMatchId.set(matchId);
                        lastHostCancelledUserId.set(actingUserId);
                        return new Match(
                                matchId,
                                Sport.PADEL,
                                actingUserId,
                                "Downtown Club",
                                "Cancelled Match",
                                "Cancelled Description",
                                Instant.parse("2026-04-06T10:00:00Z"),
                                Instant.parse("2026-04-06T12:00:00Z"),
                                8,
                                BigDecimal.TEN,
                                "public",
                                "direct",
                                "cancelled",
                                2,
                                null,
                                matchId == 47L ? 600L : null,
                                matchId == 47L ? 2 : null);
                    }

                    @Override
                    public PaginatedResult<Match> findHostedMatches(
                            final Long hostUserId,
                            final Boolean upcoming,
                            final String query,
                            final String sport,
                            final String visibility,
                            final String status,
                            final String startDate,
                            final String endDate,
                            final java.math.BigDecimal minPrice,
                            final java.math.BigDecimal maxPrice,
                            final String sort,
                            final String timezone,
                            final int page,
                            final int pageSize) {
                        final List<Match> items =
                                status != null && status.contains(EventStatus.COMPLETED.getValue())
                                        ? List.of(completedMatch)
                                        : List.of(realMatch);
                        return new PaginatedResult<>(items, items.size(), 1, pageSize);
                    }

                    @Override
                    public PaginatedResult<Match> findJoinedMatches(
                            final Long userId,
                            final Boolean upcoming,
                            final String query,
                            final String sport,
                            final String visibility,
                            final String status,
                            final String startDate,
                            final String endDate,
                            final java.math.BigDecimal minPrice,
                            final java.math.BigDecimal maxPrice,
                            final String sort,
                            final String timezone,
                            final int page,
                            final int pageSize) {
                        final List<Match> items =
                                Boolean.FALSE.equals(upcoming)
                                        ? List.of(completedMatch)
                                        : List.of(realMatch, cancelledFutureMatch);
                        return new PaginatedResult<>(items, items.size(), 1, pageSize);
                    }

                    @Override
                    public PaginatedResult<Match> searchPublicMatches(
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
                        lastSportsFilter.set(sport);
                        return new PaginatedResult<>(
                                List.of(realMatch, footballMatch), 2, 1, pageSize);
                    }
                };

        final MatchReservationService matchReservationService =
                new MatchReservationService() {
                    @Override
                    public boolean hasActiveReservation(final Long matchId, final Long userId) {
                        if (Boolean.TRUE.equals(currentUserHasReservation.get())
                                && userId == 9L
                                && (matchId == 42L || matchId == 51L)) {
                            return true;
                        }
                        return Boolean.TRUE.equals(currentUserHasSeriesReservation.get())
                                && userId == 9L
                                && (matchId == 46L || matchId == 47L);
                    }

                    @Override
                    public void reserveSpot(final Long matchId, final Long userId) {
                        final MatchReservationException failure = reservationFailure.get();
                        if (failure != null) {
                            throw failure;
                        }

                        lastReservedMatchId.set(matchId);
                        lastReservedUserId.set(userId);
                    }

                    @Override
                    public void reserveSeries(final Long matchId, final Long userId) {
                        final MatchReservationException failure = seriesReservationFailure.get();
                        if (failure != null) {
                            throw failure;
                        }

                        lastReservedMatchId.set(matchId);
                        lastReservedUserId.set(userId);
                    }

                    @Override
                    public void cancelSeriesReservations(final Long matchId, final Long userId) {
                        final MatchReservationException failure = seriesCancellationFailure.get();
                        if (failure != null) {
                            throw failure;
                        }

                        lastCancelledSeriesMatchId.set(matchId);
                        lastCancelledSeriesUserId.set(userId);
                    }
                };

        final MatchParticipationService matchParticipationService =
                new MatchParticipationService() {
                    @Override
                    public void requestToJoin(final Long matchId, final Long userId) {
                        // No-op for route rendering tests.
                    }

                    @Override
                    public void cancelJoinRequest(final Long matchId, final Long userId) {
                        // No-op for route rendering tests.
                    }

                    @Override
                    public boolean hasPendingRequest(final Long matchId, final Long userId) {
                        return false;
                    }

                    @Override
                    public void approveRequest(
                            final Long matchId, final Long hostUserId, final Long targetUserId) {
                        // No-op for route rendering tests.
                    }

                    @Override
                    public void rejectRequest(
                            final Long matchId, final Long hostUserId, final Long targetUserId) {
                        // No-op for route rendering tests.
                    }

                    @Override
                    public void removeParticipant(
                            final Long matchId, final Long hostUserId, final Long targetUserId) {
                        if (hostUserId.equals(targetUserId)) {
                            final MatchParticipationException failure =
                                    reservationCancellationFailure.get();
                            if (failure != null) {
                                throw failure;
                            }

                            lastCancelledReservationMatchId.set(matchId);
                            lastCancelledReservationUserId.set(targetUserId);
                        }
                    }

                    @Override
                    public List<User> findPendingRequests(
                            final Long matchId, final Long hostUserId) {
                        return List.of();
                    }

                    @Override
                    public List<User> findConfirmedParticipants(
                            final Long matchId, final Long hostUserId) {
                        return List.of();
                    }

                    @Override
                    public List<Match> findPendingRequestMatches(final Long userId) {
                        return List.of();
                    }

                    @Override
                    public void inviteUser(
                            final Long matchId, final Long hostUserId, final String email) {}

                    @Override
                    public void acceptInvite(final Long matchId, final Long userId) {}

                    @Override
                    public void declineInvite(final Long matchId, final Long userId) {}

                    @Override
                    public boolean hasInvitation(final Long matchId, final Long userId) {
                        return false;
                    }

                    @Override
                    public List<User> findInvitedUsers(final Long matchId, final Long hostUserId) {
                        return List.of();
                    }

                    @Override
                    public List<User> findDeclinedInvitees(
                            final Long matchId, final Long hostUserId) {
                        return List.of();
                    }

                    @Override
                    public List<Match> findInvitedMatches(final Long userId) {
                        return List.of();
                    }
                };

        final UserService userService =
                new UserService() {
                    private User currentUser =
                            new User(
                                    9L,
                                    "host@test.com",
                                    "host-player",
                                    "Jamie",
                                    "Rivera",
                                    "+1 555 123 4567",
                                    null);

                    @Override
                    public User createUser(final String email, final String username) {
                        return new User(9L, email, username);
                    }

                    @Override
                    public Optional<User> findByEmail(final String email) {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<User> findById(final Long id) {
                        if (id.equals(currentUser.getId())) {
                            return Optional.of(currentUser);
                        }
                        if (id.equals(2L)) {
                            return Optional.of(
                                    new User(
                                            2L,
                                            "first@test.com",
                                            "first-player",
                                            "First",
                                            "Player",
                                            null,
                                            77L));
                        }
                        if (id.equals(3L)) {
                            return Optional.of(
                                    new User(
                                            3L,
                                            "second@test.com",
                                            "second-player",
                                            "Second",
                                            "Player",
                                            null,
                                            null));
                        }
                        if (id.equals(7L)) {
                            return Optional.of(
                                    new User(
                                            7L,
                                            "host@test.com",
                                            "host-player",
                                            "Jamie",
                                            "Rivera",
                                            "+1 555 123 4567",
                                            88L));
                        }
                        return Optional.of(new User(id, "host@test.com", "host-player"));
                    }

                    @Override
                    public Optional<User> findByUsername(final String username) {
                        if (currentUser.getUsername().equals(username)) {
                            return Optional.of(currentUser);
                        }
                        if ("second-player".equals(username)) {
                            return Optional.of(
                                    new User(
                                            3L,
                                            "second@test.com",
                                            "second-player",
                                            "Second",
                                            "Player",
                                            null,
                                            null));
                        }
                        if ("host-player".equals(username)) {
                            return Optional.of(
                                    new User(
                                            9L,
                                            "host@test.com",
                                            "host-player",
                                            "Jamie",
                                            "Rivera",
                                            "+1 555 123 4567",
                                            currentUser.getProfileImageId()));
                        }
                        return Optional.empty();
                    }

                    @Override
                    public User updateProfile(
                            final Long id,
                            final String username,
                            final String name,
                            final String lastName,
                            final String phone,
                            final String profileImageContentType,
                            final long profileImageContentLength,
                            final InputStream profileImageContentStream) {
                        if (profileImageContentType != null
                                && !profileImageContentType.startsWith("image/")) {
                            throw new ImageUploadException(ImageUploadException.UNSUPPORTED_FORMAT);
                        }
                        currentUser =
                                new User(
                                        id,
                                        currentUser.getEmail(),
                                        username.trim().toLowerCase(Locale.ROOT),
                                        name.trim(),
                                        lastName.trim(),
                                        phone == null || phone.isBlank() ? null : phone.trim(),
                                        profileImageContentLength > 0
                                                ? Long.valueOf(500L)
                                                : currentUser.getProfileImageId());
                        return currentUser;
                    }

                    @Override
                    public User updateProfileImage(
                            final Long id,
                            final String contentType,
                            final long contentLength,
                            final InputStream contentStream) {
                        currentUser =
                                new User(
                                        id,
                                        currentUser.getEmail(),
                                        currentUser.getUsername(),
                                        currentUser.getName(),
                                        currentUser.getLastName(),
                                        currentUser.getPhone(),
                                        500L);
                        return currentUser;
                    }
                };

        final AccountAuthService accountAuthService =
                new AccountAuthService() {
                    @Override
                    public VerificationRequestResult register(
                            final RegisterAccountRequest request) {
                        return new VerificationRequestResult(
                                request.getEmail(), Instant.parse("2026-04-06T18:00:00Z"));
                    }

                    @Override
                    public Optional<VerificationRequestResult> resendVerification(
                            final String email) {
                        return Optional.empty();
                    }

                    @Override
                    public VerificationPreview getVerificationPreview(final String rawToken) {
                        if ("invalid".equals(rawToken)) {
                            throw new VerificationFailureException(
                                    VerificationFailureReason.NOT_FOUND, "Missing link");
                        }

                        return new VerificationPreview(
                                "Verify your Match Point account",
                                "Confirm your email address to activate the account.",
                                "player@test.com",
                                Instant.parse("2026-04-06T18:00:00Z"),
                                "Verify account",
                                "/login?verified=1",
                                List.of(
                                        new VerificationPreviewDetail(
                                                "Username", "player-account")));
                    }

                    @Override
                    public VerificationConfirmationResult confirmVerification(
                            final String rawToken) {
                        if ("invalid".equals(rawToken)) {
                            throw new VerificationFailureException(
                                    VerificationFailureReason.NOT_FOUND, "Missing link");
                        }

                        return new VerificationConfirmationResult(9L, "/login?verified=1", "done");
                    }

                    @Override
                    public Optional<VerificationRequestResult> requestPasswordReset(
                            final String email) {
                        return Optional.empty();
                    }

                    @Override
                    public PasswordResetPreview getPasswordResetPreview(final String rawToken) {
                        return new PasswordResetPreview(
                                "player@test.com", Instant.parse("2026-04-06T18:00:00Z"));
                    }

                    @Override
                    public VerificationConfirmationResult resetPassword(
                            final String rawToken, final String newPassword) {
                        return new VerificationConfirmationResult(
                                9L, "/login?reset=1", "Password reset");
                    }

                    @Override
                    public Optional<UserAccount> findAccountByEmail(final String email) {
                        return Optional.of(
                                new UserAccount(
                                        9L,
                                        email,
                                        "host-player",
                                        "{bcrypt}hash",
                                        UserRole.USER,
                                        FIXED_NOW));
                    }
                };

        final ImageService imageService =
                new ImageService() {
                    @Override
                    public Long store(
                            final String contentType,
                            final long contentLength,
                            final InputStream contentStream)
                            throws IOException {
                        return 500L;
                    }

                    @Override
                    public Optional<ar.edu.itba.paw.models.ImageMetadata> findMetadataById(
                            final Long imageId) {
                        return Optional.empty();
                    }

                    @Override
                    public boolean streamContentById(
                            final Long imageId, final OutputStream outputStream)
                            throws IOException {
                        return false;
                    }
                };

        final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new FeedController(matchService, messageSource),
                                new EventController(
                                        matchService,
                                        matchReservationService,
                                        matchParticipationService,
                                        userService,
                                        messageSource,
                                        fixedClock),
                                new PublicProfileController(userService, messageSource),
                                new AccountController(userService, messageSource),
                                new HostController(
                                        matchService, imageService, fixedClock, messageSource),
                                new MatchDashboardController(matchService, messageSource),
                                new ErrorPageController(messageSource),
                                new VerificationController(accountAuthService, messageSource))
                        .setViewResolvers(viewResolver)
                        .setLocaleResolver(localeResolver())
                        .addInterceptors(localeChangeInterceptor())
                        .setValidator(validator)
                        .setConversionService(new DefaultFormattingConversionService())
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getFeedRouteRendersFeedPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"))
                .andExpect(model().attributeExists("shell"))
                .andExpect(model().attributeExists("feedPage"));
    }

    @Test
    void getFeedRouteWithSpanishLocaleLocalizesShellAndCards() throws Exception {
        mockMvc.perform(get("/").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("feed/index"))
                .andExpect(
                        model().attribute(
                                        "shell",
                                        Matchers.hasProperty("hostAction", Matchers.nullValue())))
                .andExpect(
                        model().attribute(
                                        "feedPage",
                                        Matchers.hasProperty(
                                                "title",
                                                Matchers.is(
                                                        "Encontr\u00e1 tu pr\u00f3ximo partido."))));
    }

    @Test
    void getFeedRouteWithRepeatedSportParamsPassesCommaSeparatedToService() throws Exception {
        mockMvc.perform(get("/").param("sport", "padel").param("sport", "football"))
                .andExpect(status().isOk());

        Assertions.assertNotNull(lastSportsFilter.get());
        Assertions.assertTrue(lastSportsFilter.get().contains("padel"));
        Assertions.assertTrue(lastSportsFilter.get().contains("football"));
    }

    @Test
    void getFeedRouteWithCommaSeparatedSportParamAcceptsMultipleSports() throws Exception {
        mockMvc.perform(get("/").param("sport", "padel,tennis"))
                .andExpect(status().isOk())
                .andExpect(
                        model().attribute(
                                        "selectedSports",
                                        Matchers.containsInAnyOrder("padel", "tennis")));
    }

    @Test
    void getFeedRouteWithMinAndMaxPricePropagatesToModel() throws Exception {
        mockMvc.perform(get("/").param("minPrice", "5").param("maxPrice", "25"))
                .andExpect(status().isOk())
                .andExpect(
                        model().attribute(
                                        "selectedMinPrice",
                                        Matchers.comparesEqualTo(new BigDecimal("5"))))
                .andExpect(
                        model().attribute(
                                        "selectedMaxPrice",
                                        Matchers.comparesEqualTo(new BigDecimal("25"))));
    }

    @Test
    void getRealMatchDetailsRouteRendersMatchPageForAnonymousUsers() throws Exception {
        mockMvc.perform(get("/matches/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attributeExists("reservationRequestPath"))
                .andExpect(model().attribute("reservationRequiresLogin", true))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty(
                                                "aboutParagraphs",
                                                Matchers.contains("Friendly\n doubles session"))))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty(
                                                "hostProfileHref",
                                                Matchers.is("/users/host-player"))))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty(
                                                "hostProfileImageUrl", Matchers.is("/images/88"))))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty(
                                                "participants",
                                                Matchers.contains(
                                                        Matchers.allOf(
                                                                Matchers.hasProperty(
                                                                        "profileHref",
                                                                        Matchers.is(
                                                                                "/users/first-player")),
                                                                Matchers.hasProperty(
                                                                        "profileImageUrl",
                                                                        Matchers.is("/images/77"))),
                                                        Matchers.allOf(
                                                                Matchers.hasProperty(
                                                                        "profileHref",
                                                                        Matchers.is(
                                                                                "/users/second-player")),
                                                                Matchers.hasProperty(
                                                                        "profileImageUrl",
                                                                        Matchers.is(
                                                                                "/assets/default-profile-avatar.svg")))))));
    }

    @Test
    void getRealMatchDetailsRouteForAuthenticatedUsersEnablesDirectReservation() throws Exception {
        authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("reservationRequiresLogin", false));
    }

    @Test
    void getRealMatchDetailsRouteForJoinedUserExposesReservationCancellation() throws Exception {
        currentUserHasReservation.set(true);
        authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("isConfirmedParticipant", true))
                .andExpect(model().attribute("reservationCancellationEnabled", true))
                .andExpect(
                        model().attribute(
                                        "reservationCancelPath",
                                        "/matches/42/reservations/cancel"));
    }

    @Test
    void getPrivateInviteOnlyMatchDetailsForJoinedUserExposesReservationCancellation()
            throws Exception {
        currentUserHasReservation.set(true);
        authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/51"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("isConfirmedParticipant", true))
                .andExpect(model().attribute("reservationCancellationEnabled", true))
                .andExpect(
                        model().attribute(
                                        "reservationCancelPath",
                                        "/matches/51/reservations/cancel"));
    }

    @Test
    void getRecurringMatchDetailsRouteExposesRecurringOccurrenceStates() throws Exception {
        mockMvc.perform(get("/matches/46"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty("occurrences", Matchers.hasSize(5))))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty(
                                                "occurrences",
                                                Matchers.hasItem(
                                                        Matchers.hasProperty(
                                                                "statusLabel",
                                                                Matchers.is("Completed"))))))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty(
                                                "occurrences",
                                                Matchers.hasItem(
                                                        Matchers.hasProperty(
                                                                "statusLabel",
                                                                Matchers.is("Full"))))))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty(
                                                "occurrences",
                                                Matchers.hasItem(
                                                        Matchers.hasProperty(
                                                                "statusLabel",
                                                                Matchers.is("Cancelled"))))))
                .andExpect(model().attribute("seriesReservationEnabled", true))
                .andExpect(
                        model().attribute(
                                        "seriesReservationPath",
                                        "/matches/46/recurring-reservations"));
    }

    @Test
    void getRecurringMatchDetailsRouteForJoinedUserExposesSeriesCancellation() throws Exception {
        currentUserHasSeriesReservation.set(true);
        authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/46"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("seriesCancellationEnabled", true))
                .andExpect(
                        model().attribute(
                                        "seriesReservationCancelPath",
                                        "/matches/46/recurring-reservations/cancel"));
    }

    @Test
    void getPastRecurringMatchDetailsRouteExposesCompletedNotice() throws Exception {
        mockMvc.perform(get("/matches/48"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("reservationEnabled", false))
                .andExpect(
                        model().attribute("eventStateNotice", "This event has already occurred."));
    }

    @Test
    void getRealMatchDetailsRouteForHostExposesManagementActions() throws Exception {
        authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/42"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hostCanManage", true))
                .andExpect(model().attribute("hostCanEdit", true))
                .andExpect(model().attribute("hostCanCancel", true))
                .andExpect(model().attribute("hostEditPath", "/host/matches/42/edit"))
                .andExpect(model().attribute("hostCancelPath", "/host/matches/42/cancel"));
    }

    @Test
    void getRealMatchDetailsRouteForHostDisablesManagementOnCompletedEvent() throws Exception {
        authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/44"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hostCanManage", true))
                .andExpect(model().attribute("hostCanEdit", false))
                .andExpect(model().attribute("hostCanCancel", false));
    }

    @Test
    void getRealMatchDetailsRouteWithSpanishHostActionLocalizesNotice() throws Exception {
        authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/42").param("hostAction", "updated").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(
                        model().attribute(
                                        "hostActionNotice",
                                        "Tu evento fue actualizado correctamente."));
    }

    @Test
    void postReservationRequestWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/matches/42/reservations")).andExpect(status().isUnauthorized());
    }

    @Test
    void postReservationRequestAsAuthenticatedUserRedirectsToConfirmedEvent() throws Exception {
        authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/42/reservations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42?reservation=confirmed"));

        Assertions.assertEquals(42L, lastReservedMatchId.get());
        Assertions.assertEquals(9L, lastReservedUserId.get());
    }

    @Test
    void postReservationRequestWithSpanishLocaleLocalizesReservationErrors() throws Exception {
        authenticateUser(9L, "player@test.com", "player-account");
        reservationFailure.set(new MatchReservationException("already_joined", "Already reserved"));

        mockMvc.perform(post("/matches/42/reservations").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("reservationRequiresLogin", false))
                .andExpect(
                        model().attribute(
                                        "reservationError",
                                        "Tu cuenta ya tiene una reserva confirmada para este evento."));
    }

    @Test
    void postReservationCancelWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/matches/42/reservations/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postReservationCancelAsAuthenticatedUserRedirectsToCancelledEvent() throws Exception {
        authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/42/reservations/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42?reservation=cancelled"));

        Assertions.assertEquals(42L, lastCancelledReservationMatchId.get());
        Assertions.assertEquals(9L, lastCancelledReservationUserId.get());
    }

    @Test
    void postPrivateInviteOnlyReservationCancelRedirectsToUpcomingMatches() throws Exception {
        authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/51/reservations/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/player/matches/upcoming"));

        Assertions.assertEquals(51L, lastCancelledReservationMatchId.get());
        Assertions.assertEquals(9L, lastCancelledReservationUserId.get());
    }

    @Test
    void postReservationCancelWithSpanishLocaleLocalizesReservationErrors() throws Exception {
        authenticateUser(9L, "player@test.com", "player-account");
        reservationCancellationFailure.set(
                new MatchParticipationException("not_joined", "No active reservation"));

        mockMvc.perform(post("/matches/42/reservations/cancel").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(
                        model().attribute(
                                        "reservationError",
                                        "No ten\u00e9s una reserva activa para este evento."));
    }

    @Test
    void postSeriesReservationRequestWithoutAuthenticatedUserReturnsUnauthorized()
            throws Exception {
        mockMvc.perform(post("/matches/46/recurring-reservations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postSeriesReservationRequestAsAuthenticatedUserRedirectsToConfirmedEvent()
            throws Exception {
        authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/46/recurring-reservations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/46?reservation=recurringConfirmed"));

        Assertions.assertEquals(46L, lastReservedMatchId.get());
        Assertions.assertEquals(9L, lastReservedUserId.get());
    }

    @Test
    void postSeriesReservationRequestWithSpanishLocaleLocalizesReservationErrors()
            throws Exception {
        authenticateUser(9L, "player@test.com", "player-account");
        seriesReservationFailure.set(
                new MatchReservationException("series_already_joined", "Already joined"));

        mockMvc.perform(post("/matches/46/recurring-reservations").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(
                        model().attribute(
                                        "seriesReservationError",
                                        "Tu cuenta ya tiene reservas confirmadas para las fechas futuras recurrentes."));
    }

    @Test
    void postSeriesReservationCancelWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/matches/46/recurring-reservations/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postSeriesReservationCancelAsAuthenticatedUserRedirectsToCancelledEvent()
            throws Exception {
        authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/46/recurring-reservations/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/46?reservation=recurringCancelled"));

        Assertions.assertEquals(46L, lastCancelledSeriesMatchId.get());
        Assertions.assertEquals(9L, lastCancelledSeriesUserId.get());
    }

    @Test
    void postSeriesReservationCancelWithSpanishLocaleLocalizesReservationErrors() throws Exception {
        authenticateUser(9L, "player@test.com", "player-account");
        seriesCancellationFailure.set(
                new MatchReservationException("series_not_joined", "No future reservations"));

        mockMvc.perform(post("/matches/46/recurring-reservations/cancel").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(
                        model().attribute(
                                        "seriesReservationError",
                                        "No ten\u00e9s reservas futuras para este evento recurrente."));
    }

    @Test
    void getVerificationPreviewRendersConfirmPage() throws Exception {
        mockMvc.perform(get("/verifications/abc123"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification/confirm"))
                .andExpect(model().attributeExists("preview"));
    }

    @Test
    void postVerificationConfirmRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/verifications/abc123/confirm"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?verified=1"));
    }

    @Test
    void getRemovedMockMatchRouteReturnsNotFound() throws Exception {
        mockMvc.perform(get("/matches/sunrise-padel-championship"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRemovedComponentPreviewRouteReturnsNotFound() throws Exception {
        mockMvc.perform(get("/ui/components")).andExpect(status().isNotFound());
    }

    @Test
    void getInvalidVerificationRendersErrorPage() throws Exception {
        mockMvc.perform(get("/verifications/invalid"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification/error"))
                .andExpect(model().attributeExists("message"));
    }

    @Test
    void getNotFoundErrorRouteRenders404Page() throws Exception {
        mockMvc.perform(get("/errors/404"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attributeExists("shell"))
                .andExpect(model().attribute("number", "404"));
    }

    @Test
    void getBadRequestErrorRouteRenders400Page() throws Exception {
        mockMvc.perform(get("/errors/400"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attributeExists("shell"))
                .andExpect(model().attribute("number", "400"));
    }

    @Test
    void getForbiddenErrorRouteRenders403Page() throws Exception {
        mockMvc.perform(get("/errors/403"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attributeExists("shell"))
                .andExpect(model().attribute("number", "403"));
    }

    @Test
    void getInternalServerErrorRouteRenders500Page() throws Exception {
        mockMvc.perform(get("/errors/500"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("errors/error-page"))
                .andExpect(model().attributeExists("shell"))
                .andExpect(model().attribute("number", "500"));
    }

    @Test
    void postHostPublishWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postHostPublishCreatesAndRedirectsForAuthenticatedUsers() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/43"));

        Assertions.assertNotNull(lastCreateMatchRequest.get());
        Assertions.assertFalse(lastCreateMatchRequest.get().isRecurring());
    }

    @Test
    void postHostPublishCreatesRecurringMatchWithOccurrenceCount() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("recurring", "true")
                                .param("recurrenceFrequency", "weekly")
                                .param("recurrenceEndMode", "occurrence_count")
                                .param("recurrenceOccurrenceCount", "3")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/43"));

        final CreateMatchRequest request = lastCreateMatchRequest.get();
        Assertions.assertNotNull(request);
        Assertions.assertTrue(request.isRecurring());
        Assertions.assertEquals("weekly", request.getRecurrence().getFrequency().getValue());
        Assertions.assertEquals(3, request.getRecurrence().getOccurrenceCount());
    }

    @Test
    void postHostPublishCreatesRecurringMatchWithUntilDate() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("recurring", "true")
                                .param("recurrenceFrequency", "weekly")
                                .param("recurrenceEndMode", "until_date")
                                .param("recurrenceUntilDate", "2099-04-24")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/43"));

        final CreateMatchRequest request = lastCreateMatchRequest.get();
        Assertions.assertNotNull(request);
        Assertions.assertTrue(request.isRecurring());
        Assertions.assertEquals("until_date", request.getRecurrence().getEndMode().getValue());
        Assertions.assertEquals(LocalDate.of(2099, 4, 24), request.getRecurrence().getUntilDate());
    }

    @Test
    void postHostPublishRejectsInvalidRecurrenceFrequency() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("recurring", "true")
                                .param("recurrenceFrequency", "yearly")
                                .param("recurrenceEndMode", "occurrence_count")
                                .param("recurrenceOccurrenceCount", "3")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(
                        model().attributeHasFieldErrors("createEventForm", "recurrenceFrequency"));
    }

    @Test
    void postHostPublishRejectsRecurringUntilDateTooSoonForFrequency() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("recurring", "true")
                                .param("recurrenceFrequency", "weekly")
                                .param("recurrenceEndMode", "until_date")
                                .param("recurrenceUntilDate", "2099-04-12")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(
                        model().attributeHasFieldErrors("createEventForm", "recurrenceUntilDate"));
    }

    @Test
    void postHostPublishAcceptsOtherSportOption() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "other")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/43"));
    }

    @Test
    void postHostPublishWithInvalidEndTimeRerendersFormWithError() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "later")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "endTime"));
    }

    @Test
    void postHostPublishWithEndEqualToStartRerendersFormWithFriendlyError() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "18:00")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "endTime"));
    }

    @Test
    void postHostPublishWithEndBeforeStartRerendersFormWithFriendlyError() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "17:45")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "endTime"));
    }

    @Test
    void getHostEditRouteRendersPrefilledFormForHost() throws Exception {
        authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches/42/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attribute("isEditMode", true))
                .andExpect(model().attribute("formAction", "/host/matches/42/edit"))
                .andExpect(
                        model().attribute(
                                        "createEventForm",
                                        Matchers.hasProperty(
                                                "title", Matchers.is("Sunrise Padel"))))
                .andExpect(
                        model().attribute(
                                        "createEventForm",
                                        Matchers.allOf(
                                                Matchers.hasProperty(
                                                        "visibility", Matchers.is("public")),
                                                Matchers.hasProperty(
                                                        "joinPolicy", Matchers.is("direct")),
                                                Matchers.hasProperty(
                                                        "endDate",
                                                        Matchers.is(LocalDate.of(2026, 4, 6))),
                                                Matchers.hasProperty(
                                                        "endTime",
                                                        Matchers.is(LocalTime.of(9, 0))))));
    }

    @Test
    void getHostEditRouteWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/host/matches/42/edit")).andExpect(status().isUnauthorized());
    }

    @Test
    void getHostEditRouteForNonHostReturnsNotFound() throws Exception {
        authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/host/matches/42/edit")).andExpect(status().isNotFound());
    }

    @Test
    void getHostEditRouteForCompletedMatchReturnsNotFound() throws Exception {
        authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches/44/edit")).andExpect(status().isNotFound());
    }

    @Test
    void getHostEditRouteForCancelledMatchReturnsNotFound() throws Exception {
        authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches/45/edit")).andExpect(status().isNotFound());
    }

    @Test
    void postHostEditRedirectsToDetailOnSuccess() throws Exception {
        authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/42/edit")
                                .param("title", "Updated Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "20:15")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42?hostAction=updated"));
    }

    @Test
    void postHostEditPersistsPrivateVisibilityAsInviteOnly() throws Exception {
        authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/42/edit")
                                .param("title", "Updated Private Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "private")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "20:15")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42?hostAction=updated"));

        final UpdateMatchRequest request = lastUpdateMatchRequest.get();
        Assertions.assertNotNull(request);
        Assertions.assertEquals("private", request.getVisibility());
        Assertions.assertEquals("invite_only", request.getJoinPolicy());
    }

    @Test
    void postHostEditWithCapacityBelowConfirmedRerendersFormWithError() throws Exception {
        authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/42/edit")
                                .param("title", "Updated Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-11")
                                .param("endTime", "00:15")
                                .param("maxPlayers", "1")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "maxPlayers"));
    }

    @Test
    void postHostEditWithEndBeforeStartRerendersFormWithErrorOnEndTime() throws Exception {
        authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/42/edit")
                                .param("title", "Updated Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "17:45")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "endTime"));
    }

    @Test
    void postHostPublishWithPrivateEventSucceedsRegardlessOfJoinPolicy() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("visibility", "private")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void postHostEditForCompletedMatchReturnsNotFound() throws Exception {
        authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/44/edit")
                                .param("title", "Updated Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "20:15")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postHostEditForCancelledMatchReturnsNotFound() throws Exception {
        authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/45/edit")
                                .param("title", "Updated Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("sport", "padel")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "20:15")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postHostCancelWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/host/matches/42/cancel")).andExpect(status().isUnauthorized());
    }

    @Test
    void postHostCancelForNonHostReturnsNotFound() throws Exception {
        authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/host/matches/42/cancel")).andExpect(status().isNotFound());
    }

    @Test
    void postHostCancelRedirectsToDetailOnSuccess() throws Exception {
        authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(post("/host/matches/42/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42?hostAction=cancelled"));
    }

    @Test
    void postHostCancelRecurringOccurrenceRedirectsToSelectedOccurrence() throws Exception {
        // Arrange
        authenticateUser(7L, "host@test.com", "host-player");

        // Exercise and Assert
        mockMvc.perform(post("/host/matches/47/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/47?hostAction=cancelled"));
        Assertions.assertEquals(47L, lastHostCancelledMatchId.get());
        Assertions.assertEquals(7L, lastHostCancelledUserId.get());
    }

    @Test
    void postHostCancelForCompletedEventReturnsNotFound() throws Exception {
        authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(post("/host/matches/44/cancel")).andExpect(status().isNotFound());
    }

    @Test
    void getHostAllMatchesRouteRendersDashboardPage() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/list"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attributeExists("listTitle"));
    }

    @Test
    void getHostFinishedMatchesRouteRendersFinishedPage() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches/finished").locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/list"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attributeExists("listTitle"));
    }

    @Test
    void getHostFinishedMatchesDefaultsDateRangeAndNoLegacyTimeOption() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");
        final String today = LocalDate.now(ZoneId.systemDefault()).toString();

        final MvcResult result =
                mockMvc.perform(get("/host/matches/finished").locale(Locale.ENGLISH))
                        .andExpect(status().isOk())
                        .andExpect(view().name("matches/list"))
                        .andExpect(
                                model().attribute("selectedStartDateValue", Matchers.nullValue()))
                        .andExpect(model().attribute("selectedEndDateValue", today))
                        .andReturn();

        assertNoTomorrowTimeOption(
                (MatchListControlsViewModel)
                        result.getModelAndView().getModel().get("listControls"));
    }

    @Test
    void getPlayerPastMatchesRouteRendersPastPage() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/player/matches/past"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/list"))
                .andExpect(model().attributeExists("events"));
    }

    @Test
    void getPlayerPastMatchesDefaultsDateRangeAndNoLegacyTimeOption() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");
        final String today = LocalDate.now(ZoneId.systemDefault()).toString();

        final MvcResult result =
                mockMvc.perform(get("/player/matches/past").locale(Locale.ENGLISH))
                        .andExpect(status().isOk())
                        .andExpect(view().name("matches/list"))
                        .andExpect(
                                model().attribute("selectedStartDateValue", Matchers.nullValue()))
                        .andExpect(model().attribute("selectedEndDateValue", today))
                        .andReturn();

        assertNoTomorrowTimeOption(
                (MatchListControlsViewModel)
                        result.getModelAndView().getModel().get("listControls"));
    }

    @Test
    void getPlayerUpcomingMatchesRouteRendersUpcomingPage() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");
        final String today = LocalDate.now(ZoneId.systemDefault()).toString();

        mockMvc.perform(get("/player/matches/upcoming"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/list"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attribute("selectedStartDateValue", today))
                .andExpect(model().attribute("selectedEndDateValue", Matchers.nullValue()));
    }

    @Test
    void getAccountRouteRendersPrivateAccountPageForAuthenticatedUsers() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/account"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/index"))
                .andExpect(model().attributeExists("accountProfile"))
                .andExpect(model().attributeExists("shell"));
    }

    @Test
    void getAccountRouteWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/account")).andExpect(status().isUnauthorized());
    }

    @Test
    void getAccountEditRouteRendersEditablePageForAuthenticatedUsers() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/account/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/edit"))
                .andExpect(model().attributeExists("accountProfileForm"));
    }

    @Test
    void postAccountEditRouteWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(
                        post("/account/edit")
                                .param("username", "updated_user")
                                .param("name", "Taylor")
                                .param("lastName", "Morgan")
                                .param("phone", ""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postAccountEditRouteUpdatesProfileAndRedirects() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/account/edit")
                                .param("username", "updated_user")
                                .param("name", "Taylor")
                                .param("lastName", "Morgan")
                                .param("phone", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account?updated=1"));
    }

    @Test
    void postAccountEditRouteUpdatesPictureAndRedirects() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        multipart("/account/edit")
                                .file(
                                        new MockMultipartFile(
                                                "profileImage",
                                                "avatar.png",
                                                "image/png",
                                                new byte[] {1, 2, 3}))
                                .param("username", "host-player")
                                .param("name", "Jamie")
                                .param("lastName", "Rivera")
                                .param("phone", "+1 555 123 4567"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account?updated=1"));
    }

    @Test
    void postAccountEditRouteShowsLocalizedImageErrorForUnsupportedFormat() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");
        final String expectedMessage =
                messageSource()
                        .getMessage(
                                "account.profileImage.error.invalidFormat", null, Locale.ENGLISH);

        mockMvc.perform(
                        multipart("/account/edit")
                                .file(
                                        new MockMultipartFile(
                                                "profileImage",
                                                "avatar.pdf",
                                                "application/pdf",
                                                new byte[] {1, 2, 3}))
                                .locale(Locale.ENGLISH)
                                .param("username", "host-player")
                                .param("name", "Jamie")
                                .param("lastName", "Rivera")
                                .param("phone", "+1 555 123 4567"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/edit"))
                .andExpect(model().attribute("accountProfileImageError", expectedMessage));
    }

    @Test
    void getPublicProfileRouteRendersPublicProfileForAnonymousUsers() throws Exception {
        mockMvc.perform(get("/users/host-player"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attributeExists("profilePage"))
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty(
                                                "username", Matchers.is("host-player"))))
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty("name", Matchers.is("Jamie"))))
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty("lastName", Matchers.is("Rivera"))))
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty(
                                                "phone", Matchers.is("+1 555 123 4567"))))
                .andExpect(
                        model().attribute(
                                        "profilePage", Matchers.not(Matchers.hasProperty("email"))))
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty(
                                                "profileImageUrl",
                                                Matchers.is(
                                                        "/assets/default-profile-avatar.svg"))));
    }

    @Test
    void getPublicProfileRouteUsesUploadedProfileImageWhenPresent() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        multipart("/account/edit")
                                .file(
                                        new MockMultipartFile(
                                                "profileImage",
                                                "avatar.png",
                                                "image/png",
                                                new byte[] {1, 2, 3}))
                                .param("username", "host-player")
                                .param("name", "Jamie")
                                .param("lastName", "Rivera")
                                .param("phone", "+1 555 123 4567"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/users/host-player"))
                .andExpect(status().isOk())
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty(
                                                "profileImageUrl", Matchers.is("/images/500"))));
    }

    @Test
    void getOwnPublicProfileRouteShowsEditAction() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/users/host-player"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("profileEditHref", "/account/edit"));
    }

    @Test
    void getOtherPublicProfileRouteDoesNotShowEditAction() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/users/second-player"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attributeDoesNotExist("profileEditHref"));
    }

    @Test
    void getPublicProfileRouteWithSpanishLocaleLocalizesPageCopy() throws Exception {
        mockMvc.perform(get("/users/host-player").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attribute("profileTitle", "Perfil p\u00fablico"))
                .andExpect(model().attribute("profileUsernameLabel", "Usuario"))
                .andExpect(model().attribute("profilePhoneLabel", "Tel\u00e9fono"));
    }

    @Test
    void getUnknownPublicProfileRouteReturnsNotFound() throws Exception {
        mockMvc.perform(get("/users/missing-player")).andExpect(status().isNotFound());
    }

    @Test
    void getHostAllMatchesRouteWithSpanishLocaleLocalizesHeader() throws Exception {
        authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/list"))
                .andExpect(model().attribute("listTitle", "Panel de eventos organizados"));
    }

    private void authenticateUser(final Long userId, final String email, final String username) {
        final AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(
                        new UserAccount(
                                userId, email, username, "{bcrypt}hash", UserRole.USER, FIXED_NOW));
        final UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static void assertNoTomorrowTimeOption(final MatchListControlsViewModel listControls) {
        final boolean hasTomorrowOption =
                listControls.getFilterGroups().stream()
                        .map(FilterGroupViewModel::getOptions)
                        .flatMap(List::stream)
                        .anyMatch(
                                option ->
                                        option.getHref() != null
                                                && option.getHref().contains("time=tomorrow"));

        Assertions.assertFalse(hasTomorrowOption);
    }

    private static MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    private static SessionLocaleResolver localeResolver() {
        final SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        return localeResolver;
    }

    private static LocaleChangeInterceptor localeChangeInterceptor() {
        final LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        return localeChangeInterceptor;
    }

    private static LocalValidatorFactoryBean validator(final MessageSource messageSource) {
        final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        validator.afterPropertiesSet();
        return validator;
    }
}
