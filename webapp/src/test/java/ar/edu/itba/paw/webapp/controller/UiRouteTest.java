package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.PendingJoinRequest;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.PlayerReviewSummary;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.query.PlayerReviewFilter;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.UserRole;
import ar.edu.itba.paw.services.AccountAuthService;
import ar.edu.itba.paw.services.CreateMatchRequest;
import ar.edu.itba.paw.services.ImageService;
import ar.edu.itba.paw.services.MatchCancellationFailureReason;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.MatchUpdateFailureReason;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.PasswordResetPreview;
import ar.edu.itba.paw.services.PlayerReviewService;
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
import ar.edu.itba.paw.services.exceptions.PlayerReviewException;
import ar.edu.itba.paw.services.exceptions.VerificationFailureException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.MatchUtils;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.EventCardViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.FeedPageViewModel;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class UiRouteTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T00:00:00Z");

    private MockMvc mockMvc;
    private AtomicReference<String> lastSportsFilter;
    private AtomicReference<Double> lastSearchLatitude;
    private AtomicReference<Double> lastSearchLongitude;
    private AtomicReference<Long> lastReservedMatchId;
    private AtomicReference<Long> lastReservedUserId;
    private AtomicReference<Long> lastCancelledReservationMatchId;
    private AtomicReference<Long> lastCancelledReservationUserId;
    private AtomicReference<Long> lastHostCancelledMatchId;
    private AtomicReference<Long> lastHostCancelledUserId;
    private AtomicReference<Long> lastHostSeriesUpdatedMatchId;
    private AtomicReference<Long> lastHostSeriesUpdatedUserId;
    private AtomicReference<Long> lastHostSeriesCancelledMatchId;
    private AtomicReference<Long> lastHostSeriesCancelledUserId;
    private AtomicReference<Long> lastCancelledSeriesMatchId;
    private AtomicReference<Long> lastCancelledSeriesUserId;
    private AtomicReference<Long> lastSeriesJoinRequestMatchId;
    private AtomicReference<Long> lastSeriesJoinRequestUserId;
    private AtomicReference<MatchReservationException> reservationFailure;
    private AtomicReference<MatchParticipationException> reservationCancellationFailure;
    private AtomicReference<MatchReservationException> seriesReservationFailure;
    private AtomicReference<MatchReservationException> seriesCancellationFailure;
    private AtomicReference<MatchParticipationException> seriesJoinRequestFailure;
    private AtomicReference<Boolean> currentUserHasReservation;
    private AtomicReference<Boolean> currentUserHasSeriesReservation;
    private AtomicReference<Boolean> currentUserHasJoinRequest;
    private AtomicReference<Boolean> currentUserHasSeriesJoinRequest;
    private AtomicReference<CreateMatchRequest> lastCreateMatchRequest;
    private AtomicReference<UpdateMatchRequest> lastUpdateMatchRequest;
    private AtomicReference<Match> lastUpdatedMatch;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        final MessageSource messageSource = messageSource();
        final LocalValidatorFactoryBean validator = validator(messageSource);

        lastSportsFilter = new AtomicReference<>();
        lastSearchLatitude = new AtomicReference<>();
        lastSearchLongitude = new AtomicReference<>();
        lastReservedMatchId = new AtomicReference<>();
        lastReservedUserId = new AtomicReference<>();
        lastCancelledReservationMatchId = new AtomicReference<>();
        lastCancelledReservationUserId = new AtomicReference<>();
        lastHostCancelledMatchId = new AtomicReference<>();
        lastHostCancelledUserId = new AtomicReference<>();
        lastHostSeriesUpdatedMatchId = new AtomicReference<>();
        lastHostSeriesUpdatedUserId = new AtomicReference<>();
        lastHostSeriesCancelledMatchId = new AtomicReference<>();
        lastHostSeriesCancelledUserId = new AtomicReference<>();
        lastCancelledSeriesMatchId = new AtomicReference<>();
        lastCancelledSeriesUserId = new AtomicReference<>();
        lastSeriesJoinRequestMatchId = new AtomicReference<>();
        lastSeriesJoinRequestUserId = new AtomicReference<>();
        reservationFailure = new AtomicReference<>();
        reservationCancellationFailure = new AtomicReference<>();
        seriesReservationFailure = new AtomicReference<>();
        seriesCancellationFailure = new AtomicReference<>();
        seriesJoinRequestFailure = new AtomicReference<>();
        currentUserHasReservation = new AtomicReference<>(false);
        currentUserHasSeriesReservation = new AtomicReference<>(false);
        currentUserHasJoinRequest = new AtomicReference<>(false);
        currentUserHasSeriesJoinRequest = new AtomicReference<>(false);
        lastCreateMatchRequest = new AtomicReference<>();
        lastUpdateMatchRequest = new AtomicReference<>();
        lastUpdatedMatch = new AtomicReference<>();

        final Match realMatch =
                new Match(
                        42L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Downtown Club",
                        -34.61,
                        -58.38,
                        "Sunrise Padel",
                        "Friendly\\n doubles session",
                        Instant.parse("2026-04-06T10:00:00Z"),
                        Instant.parse("2026-04-06T12:00:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        2,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null);
        final Match footballMatch =
                new Match(
                        43L,
                        Sport.FOOTBALL,
                        UserUtils.getUser(8L),
                        "North Arena",
                        null,
                        null,
                        "Afterwork Football",
                        "Fast 5v5",
                        Instant.parse("2026-04-07T19:00:00Z"),
                        Instant.parse("2026-04-07T20:30:00Z"),
                        10,
                        BigDecimal.ZERO,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        4,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null);
        final Match completedMatch =
                new Match(
                        44L,
                        Sport.BASKETBALL,
                        UserUtils.getUser(7L),
                        "South Sports Center",
                        null,
                        null,
                        "Weekend Basketball",
                        "Completed tournament",
                        Instant.parse("2026-04-03T19:00:00Z"),
                        Instant.parse("2026-04-03T21:00:00Z"),
                        10,
                        BigDecimal.ZERO,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.COMPLETED,
                        10,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null);
        final Match cancelledFutureMatch =
                new Match(
                        45L,
                        Sport.TENNIS,
                        UserUtils.getUser(7L),
                        "City Tennis Club",
                        null,
                        null,
                        "Sunday Tennis",
                        "Cancelled due to weather",
                        Instant.parse("2026-04-08T12:00:00Z"),
                        Instant.parse("2026-04-08T14:00:00Z"),
                        6,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.CANCELLED,
                        2,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null);
        final Match privateInviteOnlyMatch =
                new Match(
                        51L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Members Club",
                        null,
                        null,
                        "Invite Night Padel",
                        "Private doubles session",
                        Instant.parse("2026-04-10T21:00:00Z"),
                        Instant.parse("2026-04-10T22:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        2,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null);
        final Match recurringMatch =
                new Match(
                        46L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Downtown Club",
                        null,
                        null,
                        "Weekly Padel",
                        "Friendly recurring session",
                        Instant.parse("2026-04-09T18:00:00Z"),
                        Instant.parse("2026-04-09T19:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        1,
                        null,
                        MatchUtils.getMatchSeries(600L, UserUtils.getUser(7L)),
                        1,
                        false,
                        null,
                        null,
                        null);
        final Match recurringSecondOccurrence =
                new Match(
                        47L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Downtown Club",
                        null,
                        null,
                        "Weekly Padel",
                        "Friendly recurring session",
                        Instant.parse("2026-04-16T18:00:00Z"),
                        Instant.parse("2026-04-16T19:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        0,
                        null,
                        MatchUtils.getMatchSeries(600L, UserUtils.getUser(7L)),
                        2,
                        false,
                        null,
                        null,
                        null);
        final Match recurringPastOccurrence =
                new Match(
                        48L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Downtown Club",
                        null,
                        null,
                        "Weekly Padel",
                        "Friendly recurring session",
                        Instant.parse("2026-03-26T18:00:00Z"),
                        Instant.parse("2026-03-26T19:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        4,
                        null,
                        MatchUtils.getMatchSeries(600L, UserUtils.getUser(7L)),
                        0,
                        false,
                        null,
                        null,
                        null);
        final Match recurringInProgressOccurrence =
                new Match(
                        55L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Downtown Club",
                        null,
                        null,
                        "Weekly Padel",
                        "Friendly recurring session",
                        Instant.parse("2026-04-04T23:00:00Z"),
                        Instant.parse("2026-04-05T01:00:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        4,
                        null,
                        MatchUtils.getMatchSeries(600L, UserUtils.getUser(7L)),
                        1,
                        false,
                        null,
                        null,
                        null);
        final Match recurringFullOccurrence =
                new Match(
                        49L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Downtown Club",
                        null,
                        null,
                        "Weekly Padel",
                        "Friendly recurring session",
                        Instant.parse("2026-04-23T18:00:00Z"),
                        Instant.parse("2026-04-23T19:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        8,
                        null,
                        MatchUtils.getMatchSeries(600L, UserUtils.getUser(7L)),
                        3,
                        false,
                        null,
                        null,
                        null);
        final Match recurringCancelledOccurrence =
                new Match(
                        50L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Downtown Club",
                        null,
                        null,
                        "Weekly Padel",
                        "Friendly recurring session",
                        Instant.parse("2026-04-30T18:00:00Z"),
                        Instant.parse("2026-04-30T19:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.CANCELLED,
                        0,
                        null,
                        MatchUtils.getMatchSeries(600L, UserUtils.getUser(7L)),
                        4,
                        false,
                        null,
                        null,
                        null);
        final Match approvalRecurringMatch =
                new Match(
                        52L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Downtown Club",
                        null,
                        null,
                        "Approval Weekly Padel",
                        "Recurring session with host approval",
                        Instant.parse("2026-04-09T20:00:00Z"),
                        Instant.parse("2026-04-09T21:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        1,
                        null,
                        MatchUtils.getMatchSeries(700L, UserUtils.getUser(7L)),
                        1,
                        false,
                        null,
                        null,
                        null);
        final Match approvalRecurringSecondOccurrence =
                new Match(
                        53L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Downtown Club",
                        null,
                        null,
                        "Approval Weekly Padel",
                        "Recurring session with host approval",
                        Instant.parse("2026-04-16T20:00:00Z"),
                        Instant.parse("2026-04-16T21:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        0,
                        null,
                        MatchUtils.getMatchSeries(700L, UserUtils.getUser(7L)),
                        2,
                        false,
                        null,
                        null,
                        null);
        final Match approvalRecurringPastOccurrence =
                new Match(
                        54L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Downtown Club",
                        null,
                        null,
                        "Approval Weekly Padel",
                        "Recurring session with host approval",
                        Instant.parse("2026-03-26T20:00:00Z"),
                        Instant.parse("2026-03-26T21:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        4,
                        null,
                        MatchUtils.getMatchSeries(700L, UserUtils.getUser(7L)),
                        0,
                        false,
                        null,
                        null,
                        null);
        final Match pendingFutureMatch =
                new Match(
                        56L,
                        Sport.PADEL,
                        UserUtils.getUser(7L),
                        "Downtown Club",
                        null,
                        null,
                        "Approval Future Padel",
                        "Future session with host approval",
                        Instant.parse("2030-04-09T20:00:00Z"),
                        Instant.parse("2030-04-09T21:30:00Z"),
                        8,
                        BigDecimal.TEN,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.APPROVAL_REQUIRED,
                        EventStatus.OPEN,
                        1,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null);

        final MatchService matchService =
                new MatchService() {
                    @Override
                    public Match createMatch(final CreateMatchRequest request) {
                        lastCreateMatchRequest.set(request);
                        final EventJoinPolicy joinPolicy =
                                EventVisibility.PRIVATE.equals(request.getVisibility())
                                        ? EventJoinPolicy.INVITE_ONLY
                                        : request.getJoinPolicy();
                        return new Match(
                                43L,
                                request.getSport(),
                                request.getHost(),
                                request.getAddress(),
                                null,
                                null,
                                request.getTitle(),
                                request.getDescription(),
                                request.getStartsAt(),
                                request.getEndsAt(),
                                request.getMaxPlayers(),
                                request.getPricePerPlayer(),
                                request.getVisibility(),
                                joinPolicy,
                                request.getStatus(),
                                0,
                                null,
                                request.isRecurring()
                                        ? MatchUtils.getMatchSeries(700L, request.getHost())
                                        : null,
                                request.isRecurring() ? 1 : null,
                                false,
                                null,
                                null,
                                null);
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
                        if (matchId == 55L) {
                            return Optional.of(recurringInProgressOccurrence);
                        }
                        if (matchId == 49L) {
                            return Optional.of(recurringFullOccurrence);
                        }
                        if (matchId == 50L) {
                            return Optional.of(recurringCancelledOccurrence);
                        }
                        if (matchId == 52L) {
                            return Optional.of(approvalRecurringMatch);
                        }
                        if (matchId == 53L) {
                            return Optional.of(approvalRecurringSecondOccurrence);
                        }
                        if (matchId == 54L) {
                            return Optional.of(approvalRecurringPastOccurrence);
                        }
                        if (matchId == 56L) {
                            return Optional.of(pendingFutureMatch);
                        }
                        return Optional.empty();
                    }

                    @Override
                    public Optional<Match> findPublicMatchById(final Long matchId) {
                        return findMatchById(matchId);
                    }

                    @Override
                    public List<Match> findSeriesOccurrences(final Long seriesId) {
                        if (Long.valueOf(600L).equals(seriesId)) {
                            return List.of(
                                    recurringPastOccurrence, recurringInProgressOccurrence,
                                    recurringMatch, recurringSecondOccurrence,
                                    recurringFullOccurrence, recurringCancelledOccurrence);
                        }
                        if (Long.valueOf(700L).equals(seriesId)) {
                            return List.of(
                                    approvalRecurringPastOccurrence,
                                    approvalRecurringMatch,
                                    approvalRecurringSecondOccurrence);
                        }
                        return List.of();
                    }

                    @Override
                    public List<User> findConfirmedParticipants(final Long matchId) {
                        return matchId == 42L
                                ? List.of(UserUtils.getUser(2L), UserUtils.getUser(3L))
                                : List.of();
                    }

                    @Override
                    public Match updateMatch(
                            final Long matchId,
                            final User actingUser,
                            final UpdateMatchRequest request) {
                        if (matchId != 42L) {
                            throw new MatchUpdateException(
                                    MatchUpdateFailureReason.MATCH_NOT_FOUND, "Missing match");
                        }
                        if (actingUser.getId() != 7L) {
                            throw new MatchUpdateException(
                                    MatchUpdateFailureReason.FORBIDDEN, "Forbidden");
                        }
                        if (request.getMaxPlayers() < 2) {
                            throw new MatchUpdateException(
                                    MatchUpdateFailureReason.CAPACITY_BELOW_CONFIRMED,
                                    "Capacity too low");
                        }

                        final EventJoinPolicy joinPolicy =
                                EventVisibility.PRIVATE == request.getVisibility()
                                        ? EventJoinPolicy.INVITE_ONLY
                                        : request.getJoinPolicy();

                        lastUpdateMatchRequest.set(request);
                        final Match result =
                                new Match(
                                        matchId,
                                        request.getSport(),
                                        actingUser,
                                        request.getAddress(),
                                        null,
                                        null,
                                        request.getTitle(),
                                        request.getDescription(),
                                        request.getStartsAt(),
                                        request.getEndsAt(),
                                        request.getMaxPlayers(),
                                        request.getPricePerPlayer(),
                                        request.getVisibility(),
                                        joinPolicy,
                                        request.getStatus(),
                                        0,
                                        request.getBannerImageMetadata(),
                                        null,
                                        null,
                                        false,
                                        null,
                                        null,
                                        null);
                        lastUpdatedMatch.set(result);
                        return result;
                    }

                    @Override
                    public List<Match> updateSeriesFromOccurrence(
                            final Long matchId,
                            final User actingUser,
                            final UpdateMatchRequest request) {
                        if (matchId != 46L && matchId != 47L) {
                            throw new MatchUpdateException(
                                    MatchUpdateFailureReason.MATCH_NOT_FOUND, "Missing match");
                        }
                        if (actingUser.getId() != 7L) {
                            throw new MatchUpdateException(
                                    MatchUpdateFailureReason.FORBIDDEN, "Forbidden");
                        }
                        if (request.getMaxPlayers() < 2) {
                            throw new MatchUpdateException(
                                    MatchUpdateFailureReason.CAPACITY_BELOW_CONFIRMED,
                                    "Capacity too low");
                        }
                        lastHostSeriesUpdatedMatchId.set(matchId);
                        lastHostSeriesUpdatedUserId.set(actingUser.getId());
                        lastUpdateMatchRequest.set(request);
                        final EventJoinPolicy joinPolicy =
                                EventVisibility.PRIVATE == request.getVisibility()
                                        ? EventJoinPolicy.INVITE_ONLY
                                        : request.getJoinPolicy();
                        final Match result =
                                new Match(
                                        matchId,
                                        request.getSport(),
                                        actingUser,
                                        request.getAddress(),
                                        null,
                                        null,
                                        request.getTitle(),
                                        request.getDescription(),
                                        request.getStartsAt(),
                                        request.getEndsAt(),
                                        request.getMaxPlayers(),
                                        request.getPricePerPlayer(),
                                        request.getVisibility(),
                                        joinPolicy,
                                        request.getStatus(),
                                        0,
                                        request.getBannerImageMetadata(),
                                        MatchUtils.getMatchSeries(600L, actingUser),
                                        matchId == 46L ? 1 : 2,
                                        false,
                                        null,
                                        null,
                                        null);
                        lastUpdatedMatch.set(result);
                        return List.of(result);
                    }

                    @Override
                    public Match cancelMatch(final Long matchId, final User actingUser) {
                        if (matchId != 42L && matchId != 47L) {
                            throw new MatchCancellationException(
                                    MatchCancellationFailureReason.MATCH_NOT_FOUND,
                                    "Missing match");
                        }
                        if (actingUser.getId() != 7L) {
                            throw new MatchCancellationException(
                                    MatchCancellationFailureReason.FORBIDDEN, "Forbidden");
                        }
                        lastHostCancelledMatchId.set(matchId);
                        lastHostCancelledUserId.set(actingUser.getId());
                        return new Match(
                                matchId,
                                Sport.PADEL,
                                actingUser,
                                "Downtown Club",
                                null,
                                null,
                                "Cancelled Match",
                                "Cancelled Description",
                                Instant.parse("2026-04-06T10:00:00Z"),
                                Instant.parse("2026-04-06T12:00:00Z"),
                                8,
                                BigDecimal.TEN,
                                EventVisibility.PUBLIC,
                                EventJoinPolicy.DIRECT,
                                EventStatus.CANCELLED,
                                2,
                                null,
                                matchId == 47L ? MatchUtils.getMatchSeries(600L, actingUser) : null,
                                matchId == 47L ? 2 : null,
                                false,
                                null,
                                null,
                                null);
                    }

                    @Override
                    public List<Match> cancelSeriesFromOccurrence(
                            final Long matchId, final User actingUser) {
                        if (matchId != 46L && matchId != 47L) {
                            throw new MatchCancellationException(
                                    MatchCancellationFailureReason.MATCH_NOT_FOUND,
                                    "Missing match");
                        }
                        if (actingUser.getId() != 7L) {
                            throw new MatchCancellationException(
                                    MatchCancellationFailureReason.FORBIDDEN, "Forbidden");
                        }
                        lastHostSeriesCancelledMatchId.set(matchId);
                        lastHostSeriesCancelledUserId.set(actingUser.getId());
                        return List.of(
                                new Match(
                                        matchId,
                                        Sport.PADEL,
                                        actingUser,
                                        "Downtown Club",
                                        null,
                                        null,
                                        "Cancelled Series",
                                        "Cancelled Description",
                                        Instant.parse("2026-04-09T18:00:00Z"),
                                        Instant.parse("2026-04-09T19:30:00Z"),
                                        8,
                                        BigDecimal.TEN,
                                        EventVisibility.PUBLIC,
                                        EventJoinPolicy.DIRECT,
                                        EventStatus.CANCELLED,
                                        1,
                                        null,
                                        MatchUtils.getMatchSeries(600L, actingUser),
                                        matchId == 46L ? 1 : 2,
                                        false,
                                        null,
                                        null,
                                        null));
                    }

                    @Override
                    public PaginatedResult<Match> findHostedMatches(
                            final User host,
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
                                status != null
                                                && status.contains(
                                                        EventStatus.COMPLETED.getDbValue())
                                        ? List.of(completedMatch)
                                        : List.of(realMatch);
                        return new PaginatedResult<>(items, items.size(), 1, pageSize);
                    }

                    @Override
                    public PaginatedResult<Match> findJoinedMatches(
                            final User user,
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
                            final BigDecimal maxPrice,
                            final Double latitude,
                            final Double longitude) {
                        lastSportsFilter.set(sport);
                        lastSearchLatitude.set(latitude);
                        lastSearchLongitude.set(longitude);
                        return new PaginatedResult<>(
                                List.of(realMatch, footballMatch), 2, 1, pageSize);
                    }
                };

        final MatchReservationService matchReservationService =
                new MatchReservationService() {
                    @Override
                    public boolean hasActiveReservation(final Long matchId, final User user) {
                        if (user == null) {
                            return false;
                        }
                        if (Boolean.TRUE.equals(currentUserHasReservation.get())
                                && (user.getId() == 9L || user.getId() == 7L)
                                && (matchId == 42L || matchId == 51L)) {
                            return true;
                        }
                        return Boolean.TRUE.equals(currentUserHasSeriesReservation.get())
                                && user.getId() == 9L
                                && (matchId == 46L || matchId == 47L);
                    }

                    @Override
                    public Set<Long> findActiveFutureReservationMatchIdsForSeries(
                            final Long seriesId, final User user) {
                        if (Boolean.TRUE.equals(currentUserHasSeriesReservation.get())
                                && user != null
                                && user.getId() == 9L
                                && seriesId == 600L) {
                            return Set.of(46L, 47L);
                        }
                        return Set.of();
                    }

                    @Override
                    public void reserveSpot(final Long matchId, final User user) {
                        final MatchReservationException failure = reservationFailure.get();
                        if (failure != null) {
                            throw failure;
                        }

                        if (user == null) {
                            throw new MatchReservationException(
                                    null, "User must be authenticated to reserve a spot");
                        }

                        lastReservedMatchId.set(matchId);
                        lastReservedUserId.set(user.getId());
                    }

                    @Override
                    public void reserveSeries(final Long matchId, final User user) {
                        final MatchReservationException failure = seriesReservationFailure.get();
                        if (failure != null) {
                            throw failure;
                        }

                        if (user == null) {
                            throw new MatchReservationException(
                                    null, "User must be authenticated to reserve a spot");
                        }

                        lastReservedMatchId.set(matchId);
                        lastReservedUserId.set(user.getId());
                    }

                    @Override
                    public void cancelSeriesReservations(final Long matchId, final User user) {
                        final MatchReservationException failure = seriesCancellationFailure.get();
                        if (failure != null) {
                            throw failure;
                        }

                        if (user == null) {
                            throw new MatchReservationException(
                                    null, "User must be authenticated to reserve a spot");
                        }

                        lastCancelledSeriesMatchId.set(matchId);
                        lastCancelledSeriesUserId.set(user.getId());
                    }
                };

        final MatchParticipationService matchParticipationService =
                new MatchParticipationService() {
                    @Override
                    public void requestToJoin(final Long matchId, final User user) {
                        // No-op for route rendering tests.
                    }

                    @Override
                    public void requestToJoinSeries(final Long matchId, final User user) {
                        final MatchParticipationException failure = seriesJoinRequestFailure.get();
                        if (failure != null) {
                            throw failure;
                        }

                        if (user == null) {
                            throw new MatchParticipationException(
                                    null, "User must be authenticated to request to join a match");
                        }

                        lastSeriesJoinRequestMatchId.set(matchId);
                        lastSeriesJoinRequestUserId.set(user.getId());
                    }

                    @Override
                    public void cancelJoinRequest(final Long matchId, final User user) {
                        // No-op for route rendering tests.
                    }

                    @Override
                    public boolean hasPendingRequest(final Long matchId, final User user) {
                        if (user == null) {
                            throw new MatchParticipationException(
                                    null, "User must be authenticated to check pending requests");
                        }

                        return Boolean.TRUE.equals(currentUserHasJoinRequest.get())
                                && user.getId() == 9L
                                && (matchId == 52L || matchId == 53L || matchId == 56L);
                    }

                    @Override
                    public boolean hasPendingSeriesRequest(final Long matchId, final User user) {
                        return Boolean.TRUE.equals(currentUserHasSeriesJoinRequest.get())
                                && user != null
                                && user.getId() == 9L
                                && (matchId == 52L || matchId == 53L);
                    }

                    @Override
                    public Set<Long> findPendingFutureRequestMatchIdsForSeries(
                            final Long seriesId, final User user) {
                        if (Boolean.TRUE.equals(currentUserHasSeriesJoinRequest.get())
                                && user != null
                                && user.getId() == 9L
                                && seriesId == 700L) {
                            return Set.of(52L, 53L);
                        }
                        return Set.of();
                    }

                    @Override
                    public void approveRequest(
                            final Long matchId, final User host, final User targetUser) {
                        // No-op for route rendering tests.
                    }

                    @Override
                    public void rejectRequest(
                            final Long matchId, final User host, final User targetUser) {
                        // No-op for route rendering tests.
                    }

                    @Override
                    public void removeParticipant(
                            final Long matchId, final User host, final User target) {
                        if (host.equals(target)) {
                            final MatchParticipationException failure =
                                    reservationCancellationFailure.get();
                            if (failure != null) {
                                throw failure;
                            }

                            lastCancelledReservationMatchId.set(matchId);
                            lastCancelledReservationUserId.set(target.getId());
                        }
                    }

                    @Override
                    public List<User> findPendingRequests(final Long matchId, final User host) {
                        return List.of();
                    }

                    @Override
                    public List<PendingJoinRequest> findPendingRequestsForHost(final User host) {
                        return List.of(
                                new PendingJoinRequest(
                                        approvalRecurringMatch, UserUtils.getUser(9L), true));
                    }

                    @Override
                    public List<User> findConfirmedParticipants(
                            final Long matchId, final User host) {
                        return List.of();
                    }

                    @Override
                    public List<Match> findPendingRequestMatches(final User user) {
                        return Boolean.TRUE.equals(currentUserHasSeriesJoinRequest.get())
                                        && user != null
                                        && user.getId() == 9L
                                ? List.of(pendingFutureMatch)
                                : List.of();
                    }

                    @Override
                    public void inviteUser(
                            final Long matchId, final User host, final String email) {}

                    @Override
                    public void acceptInvite(final Long matchId, final User user) {}

                    @Override
                    public void declineInvite(final Long matchId, final User user) {}

                    @Override
                    public boolean hasInvitation(final Long matchId, final User user) {
                        return false;
                    }

                    @Override
                    public List<User> findInvitedUsers(final Long matchId, final User host) {
                        return List.of();
                    }

                    @Override
                    public List<User> findDeclinedInvitees(final Long matchId, final User host) {
                        return List.of();
                    }

                    @Override
                    public List<Match> findInvitedMatches(final User user) {
                        return List.of();
                    }
                };

        final UserService userService =
                new UserService() {
                    private User currentUser = UserUtils.getUser(9L);

                    @Override
                    public User createUser(final String email, final String username) {
                        return UserUtils.getUser(9L);
                    }

                    @Override
                    public Optional<User> findByEmail(final String email) {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<User> findById(final Long id) {
                        return Optional.of(UserUtils.getUser(id));
                    }

                    @Override
                    public List<User> findByIds(final java.util.Collection<Long> ids) {
                        return ids.stream()
                                .map(id -> findById(id).orElse(null))
                                .filter(java.util.Objects::nonNull)
                                .toList();
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
                                            currentUser.getProfileImageMetadata(),
                                            null));
                        }
                        return Optional.empty();
                    }

                    @Override
                    public User updateProfile(
                            final User user,
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
                                        user.getId(),
                                        currentUser.getEmail(),
                                        username.trim().toLowerCase(Locale.ROOT),
                                        name.trim(),
                                        lastName.trim(),
                                        phone == null || phone.isBlank() ? null : phone.trim(),
                                        profileImageContentLength > 0
                                                ? new ImageMetadata(
                                                        500L,
                                                        profileImageContentType,
                                                        profileImageContentLength)
                                                : currentUser.getProfileImageMetadata(),
                                        null);
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
                                        new ImageMetadata(500L, contentType, contentLength),
                                        null);
                        return currentUser;
                    }

                    @Override
                    public void updatePreferredLanguage(
                            final User user, final String preferredLanguage) {
                        currentUser =
                                new User(
                                        currentUser.getId(),
                                        currentUser.getEmail(),
                                        currentUser.getUsername(),
                                        currentUser.getName(),
                                        currentUser.getLastName(),
                                        currentUser.getPhone(),
                                        currentUser.getProfileImageMetadata(),
                                        preferredLanguage);
                    }
                };

        final PlayerReviewService playerReviewService =
                new PlayerReviewService() {
                    private PlayerReview viewerReview =
                            createPlayerReview(
                                    1L,
                                    9L,
                                    3L,
                                    PlayerReviewReaction.LIKE,
                                    "Good teammate",
                                    FIXED_NOW,
                                    FIXED_NOW,
                                    null);

                    private PlayerReview createPlayerReview(
                            Long id,
                            Long reviewerId,
                            Long reviewedId,
                            PlayerReviewReaction reaction,
                            String comment,
                            Instant createdAt,
                            Instant updatedAt,
                            Instant deletedAt) {
                        User reviewer = Mockito.mock(User.class);
                        User reviewed = Mockito.mock(User.class);
                        Mockito.when(reviewer.getId()).thenReturn(reviewerId);
                        Mockito.when(reviewed.getId()).thenReturn(reviewedId);
                        return new PlayerReview(
                                id,
                                reviewer,
                                reviewed,
                                reaction,
                                comment,
                                createdAt,
                                updatedAt,
                                deletedAt != null,
                                deletedAt,
                                null,
                                null);
                    }

                    @Override
                    public Optional<PlayerReview> findReviewByIdIncludingDeleted(Long reviewId) {
                        return Optional.empty();
                    }

                    @Override
                    public PlayerReview submitReview(
                            final User reviewer,
                            final User reviewed,
                            final PlayerReviewReaction reaction,
                            final String comment) {
                        if (reviewer == null
                                || reviewed == null
                                || !canReview(reviewer, reviewed)) {
                            throw new PlayerReviewException(
                                    PlayerReviewException.NOT_ELIGIBLE, "Not eligible");
                        }
                        viewerReview =
                                createPlayerReview(
                                        1L,
                                        reviewer.getId(),
                                        reviewed.getId(),
                                        reaction,
                                        comment == null || comment.isBlank()
                                                ? null
                                                : comment.trim(),
                                        FIXED_NOW,
                                        FIXED_NOW,
                                        null);
                        return viewerReview;
                    }

                    @Override
                    public void deleteReview(final User reviewer, final User reviewed) {
                        if (viewerReview == null
                                || reviewer == null
                                || reviewed == null
                                || !reviewer.getId().equals(viewerReview.getReviewer().getId())
                                || !reviewed.getId().equals(viewerReview.getReviewed().getId())) {
                            throw new PlayerReviewException(
                                    PlayerReviewException.NOT_FOUND, "Missing review");
                        }
                        viewerReview = null;
                    }

                    @Override
                    public Optional<PlayerReview> findReviewByPair(
                            final User reviewer, final User reviewed) {
                        return viewerReview == null
                                        || reviewer == null
                                        || reviewed == null
                                        || !reviewer.getId()
                                                .equals(viewerReview.getReviewer().getId())
                                        || !reviewed.getId()
                                                .equals(viewerReview.getReviewed().getId())
                                ? Optional.empty()
                                : Optional.of(viewerReview);
                    }

                    @Override
                    public PlayerReviewSummary findSummaryForUser(final User reviewed) {
                        return reviewed.getId().equals(3L)
                                ? new PlayerReviewSummary(3L, 1, 0, 1)
                                : new PlayerReviewSummary(3L, 0, 0, 0);
                    }

                    @Override
                    public PaginatedResult<PlayerReview> findReviewsForUser(
                            final User reviewed,
                            final PlayerReviewFilter filter,
                            final int page,
                            final int pageSize) {
                        final List<PlayerReview> reviews;
                        if (reviewed.getId().equals(3L) && viewerReview != null) {
                            if (filter != null
                                    && filter.getReaction().isPresent()
                                    && filter.getReaction().get() != viewerReview.getReaction()) {
                                reviews = List.of();
                            } else {
                                reviews = List.of(viewerReview);
                            }
                        } else {
                            reviews = List.of();
                        }
                        final int totalCount =
                                reviewed.getId().equals(3L)
                                                && filter == PlayerReviewFilter.BOTH
                                                && viewerReview != null
                                        ? 21
                                        : reviews.size();
                        final int totalPages =
                                totalCount == 0
                                        ? 1
                                        : (int) Math.ceil((double) totalCount / pageSize);
                        final int safePage = Math.min(Math.max(page, 1), totalPages);
                        return new PaginatedResult<>(reviews, totalCount, safePage, pageSize);
                    }

                    @Override
                    public boolean canReview(final User reviewer, final User reviewed) {
                        return reviewer != null
                                && reviewed != null
                                && reviewer.getId().equals(9L)
                                && reviewed.getId().equals(3L);
                    }

                    @Override
                    public Set<Long> findReviewableUserIds(final User reviewer) {
                        return reviewer != null && reviewer.getId().equals(9L)
                                ? Set.of(3L)
                                : Set.of();
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

                        return new VerificationConfirmationResult(
                                new UserAccount(
                                        9L,
                                        "player@test.com",
                                        "player-account",
                                        null,
                                        null,
                                        null,
                                        null,
                                        "{bcrypt}hash",
                                        UserRole.USER,
                                        FIXED_NOW,
                                        UserLanguages.DEFAULT_LANGUAGE),
                                "/",
                                "done");
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
                                        null,
                                        null,
                                        null,
                                        null,
                                        "{bcrypt}hash",
                                        UserRole.USER,
                                        FIXED_NOW,
                                        UserLanguages.DEFAULT_LANGUAGE));
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
        final ModerationService moderationService = Mockito.mock(ModerationService.class);
        Mockito.when(moderationService.findActiveBan(Mockito.any(User.class)))
                .thenReturn(Optional.empty());

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new FeedController(
                                        matchService,
                                        matchParticipationService,
                                        matchReservationService,
                                        messageSource),
                                new EventController(
                                        matchService,
                                        matchReservationService,
                                        matchParticipationService,
                                        playerReviewService,
                                        messageSource,
                                        fixedClock,
                                        true,
                                        "/assets/tiles/{z}/{x}/{y}.png",
                                        "Local Buenos Aires map tiles",
                                        14),
                                new PublicProfileController(
                                        userService, playerReviewService,
                                        moderationService, messageSource),
                                new PlayerParticipationController(matchParticipationService),
                                new AccountController(userService, messageSource),
                                new HostController(
                                        matchService, imageService, fixedClock, messageSource),
                                new HostParticipationController(
                                        matchService,
                                        matchParticipationService,
                                        userService,
                                        messageSource),
                                new MatchDashboardController(
                                        matchService,
                                        matchParticipationService,
                                        matchReservationService,
                                        messageSource),
                                new ErrorPageController(messageSource),
                                new VerificationController(accountAuthService, messageSource))
                        .setViewResolvers(viewResolver)
                        .setLocaleResolver(localeResolver())
                        .addInterceptors(localeChangeInterceptor())
                        .defaultRequest(get("/").locale(Locale.ENGLISH))
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
                .andExpect(model().attributeExists("feedPage"))
                .andExpect(model().attribute("nearMeUnavailable", false));
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
    void postExploreLocationStoresValidCoordinatesInSession() throws Exception {
        final MvcResult result =
                mockMvc.perform(
                                post("/explore/location")
                                        .param("latitude", "-34.61")
                                        .param("longitude", "-58.38"))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/?sort=distance"))
                        .andReturn();

        Assertions.assertEquals(
                -34.61, result.getRequest().getSession().getAttribute("exploreLocationLatitude"));
        Assertions.assertEquals(
                -58.38, result.getRequest().getSession().getAttribute("exploreLocationLongitude"));
    }

    @Test
    void postExploreLocationIgnoresInvalidCoordinates() throws Exception {
        final MvcResult result =
                mockMvc.perform(
                                post("/explore/location")
                                        .param("latitude", "-91")
                                        .param("longitude", "-58.38"))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/?sort=distance"))
                        .andReturn();

        Assertions.assertNull(
                result.getRequest().getSession().getAttribute("exploreLocationLatitude"));
        Assertions.assertNull(
                result.getRequest().getSession().getAttribute("exploreLocationLongitude"));
    }

    @Test
    void getFeedRouteShowsNearMeSortOptionBeforeLocationIsStored() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(
                        model().attribute(
                                        "sortOptions",
                                        Matchers.hasItem(
                                                Matchers.hasProperty(
                                                        "href",
                                                        Matchers.containsString(
                                                                "sort=distance")))));
    }

    @Test
    void getFeedRouteOmitsDistanceLabelWithoutStoredLocation() throws Exception {
        final MvcResult result = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();

        final FeedPageViewModel feedPage =
                (FeedPageViewModel) result.getModelAndView().getModel().get("feedPage");
        Assertions.assertNull(feedPage.getFeaturedEvents().get(0).getDistanceLabel());
    }

    @Test
    void getFeedRouteIncludesDistanceLabelWithStoredLocation() throws Exception {
        final MvcResult result =
                mockMvc.perform(
                                get("/").sessionAttr("exploreLocationLatitude", -34.60)
                                        .sessionAttr("exploreLocationLongitude", -58.38))
                        .andExpect(status().isOk())
                        .andReturn();

        final FeedPageViewModel feedPage =
                (FeedPageViewModel) result.getModelAndView().getModel().get("feedPage");
        Assertions.assertNotNull(feedPage.getFeaturedEvents().get(0).getDistanceLabel());
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
                                                "hostProfileHref", Matchers.is("/users/user7"))))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty(
                                                "hostProfileImageUrl",
                                                Matchers.is("/assets/default-profile-avatar.svg"))))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.allOf(
                                                Matchers.hasProperty(
                                                        "mapAvailable", Matchers.is(true)),
                                                Matchers.hasProperty(
                                                        "mapTileUrlTemplate",
                                                        Matchers.is(
                                                                "/assets/tiles/{z}/{x}/{y}.png")),
                                                Matchers.hasProperty(
                                                        "mapLatitude",
                                                        Matchers.closeTo(-34.61, 0.000001)),
                                                Matchers.hasProperty(
                                                        "mapLongitude",
                                                        Matchers.closeTo(-58.38, 0.000001)))))
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
                                                                                "/users/user2")),
                                                                Matchers.hasProperty(
                                                                        "profileImageUrl",
                                                                        Matchers.is(
                                                                                "/assets/default-profile-avatar.svg"))),
                                                        Matchers.allOf(
                                                                Matchers.hasProperty(
                                                                        "profileHref",
                                                                        Matchers.is(
                                                                                "/users/user3")),
                                                                Matchers.hasProperty(
                                                                        "profileImageUrl",
                                                                        Matchers.is(
                                                                                "/assets/default-profile-avatar.svg")))))));
    }

    @Test
    void getRealMatchDetailsRouteForAuthenticatedUsersEnablesDirectReservation() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("reservationRequiresLogin", false))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty(
                                                "participants",
                                                Matchers.contains(
                                                        Matchers.hasProperty(
                                                                "reviewHref", Matchers.nullValue()),
                                                        Matchers.hasProperty(
                                                                "reviewHref",
                                                                Matchers.is(
                                                                        "/users/"
                                                                                + "user3?reviewForm=open#reviews"))))));
    }

    @Test
    void getRealMatchDetailsRouteForJoinedUserExposesReservationCancellation() throws Exception {
        currentUserHasReservation.set(true);
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

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
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

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
    void getPrivateInviteOnlyMatchDetailsForHostExposesDirectReservation() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/51"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hostCanManage", true))
                .andExpect(model().attribute("reservationEnabled", true))
                .andExpect(model().attribute("joinRequestEnabled", false))
                .andExpect(model().attribute("isInvitedPlayer", false));
    }

    @Test
    void getRecurringMatchDetailsRouteExposesRecurringOccurrenceStates() throws Exception {
        mockMvc.perform(get("/matches/46"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty("occurrences", Matchers.hasSize(6))))
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
                                                        Matchers.allOf(
                                                                Matchers.hasProperty(
                                                                        "statusLabel",
                                                                        Matchers.is("In progress")),
                                                                Matchers.hasProperty(
                                                                        "statusTone",
                                                                        Matchers.is(
                                                                                "in-progress")))))))
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
                                                        Matchers.allOf(
                                                                Matchers.hasProperty(
                                                                        "statusLabel",
                                                                        Matchers.is("Cancelled")),
                                                                Matchers.hasProperty(
                                                                        "href",
                                                                        Matchers.nullValue()))))))
                .andExpect(model().attribute("seriesReservationEnabled", true))
                .andExpect(
                        model().attribute(
                                        "seriesReservationPath",
                                        "/matches/46/recurring-reservations"));
    }

    @Test
    void getRecurringMatchDetailsRouteLocalizesInProgressOccurrenceState() throws Exception {
        mockMvc.perform(get("/matches/46").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty(
                                                "occurrences",
                                                Matchers.hasItem(
                                                        Matchers.hasProperty(
                                                                "statusLabel",
                                                                Matchers.is("En curso"))))));
    }

    @Test
    void getRecurringApprovalRequiredMatchDetailsRouteExposesSingleAndSeriesJoinRequests()
            throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/52"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("joinRequestEnabled", true))
                .andExpect(model().attribute("joinRequestPath", "/matches/52/join-requests"))
                .andExpect(model().attribute("seriesJoinRequestEnabled", true))
                .andExpect(model().attribute("seriesJoinRequestPending", false))
                .andExpect(
                        model().attribute(
                                        "seriesJoinRequestPath",
                                        "/matches/52/recurring-join-requests"));
    }

    @Test
    void getRecurringApprovalRequiredMatchDetailsRouteShowsSeriesJoinPendingNotice()
            throws Exception {
        currentUserHasSeriesJoinRequest.set(true);
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/52"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("hasPendingJoinRequest", false))
                .andExpect(model().attribute("joinRequestEnabled", false))
                .andExpect(model().attribute("seriesJoinRequestEnabled", false))
                .andExpect(model().attribute("seriesJoinRequestPending", true));
    }

    @Test
    void getRecurringApprovalRequiredMatchDetailsRouteHidesReservationErrorWhenJoinRequestPending()
            throws Exception {
        currentUserHasSeriesJoinRequest.set(true);
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/52").param("reservationError", "closed"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("reservationError", Matchers.nullValue()))
                .andExpect(model().attribute("seriesJoinRequestPending", true));
    }

    @Test
    void getApprovalRequiredMatchDetailsRouteShowsOneTimeJoinRequestedNoticeFromFlash()
            throws Exception {
        currentUserHasJoinRequest.set(true);
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/56").flashAttr("joinRequested", true))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("joinRequested", true))
                .andExpect(model().attribute("hasPendingJoinRequest", true));
    }

    @Test
    void getApprovalRequiredMatchDetailsRouteHidesJoinRequestedNoticeAfterRefresh()
            throws Exception {
        currentUserHasJoinRequest.set(true);
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/56"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("joinRequested", false))
                .andExpect(model().attribute("hasPendingJoinRequest", true));
    }

    @Test
    void getRecurringMatchDetailsRouteForHostLinksCancelledOccurrences() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/46"))
                .andExpect(status().isOk())
                .andExpect(
                        model().attribute(
                                        "eventPage",
                                        Matchers.hasProperty(
                                                "occurrences",
                                                Matchers.hasItem(
                                                        Matchers.allOf(
                                                                Matchers.hasProperty(
                                                                        "statusLabel",
                                                                        Matchers.is("Cancelled")),
                                                                Matchers.hasProperty(
                                                                        "href",
                                                                        Matchers.is(
                                                                                "/matches/50")))))));
    }

    @Test
    void getRecurringMatchDetailsRouteForJoinedUserExposesSeriesCancellation() throws Exception {
        currentUserHasSeriesReservation.set(true);
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/46"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("isConfirmedParticipant", true))
                .andExpect(model().attribute("reservationCancellationEnabled", true))
                .andExpect(
                        model().attribute(
                                        "reservationCancelPath", "/matches/46/reservations/cancel"))
                .andExpect(model().attribute("seriesCancellationEnabled", true))
                .andExpect(
                        model().attribute(
                                        "seriesReservationCancelPath",
                                        "/matches/46/recurring-reservations/cancel"));
    }

    @Test
    void getRealMatchDetailsRouteHidesStaleReservationConfirmedNoticeWhenUserWasRemoved()
            throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/42").param("reservation", "confirmed"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("isConfirmedParticipant", false))
                .andExpect(model().attribute("reservationConfirmed", false))
                .andExpect(model().attribute("reservationEnabled", true));
    }

    @Test
    void getRealMatchDetailsRouteShowsReservationConfirmedNoticeWhenUserIsStillJoined()
            throws Exception {
        currentUserHasReservation.set(true);
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/42").param("reservation", "confirmed"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("isConfirmedParticipant", true))
                .andExpect(model().attribute("reservationConfirmed", true))
                .andExpect(model().attribute("reservationEnabled", true));
    }

    @Test
    void getRecurringMatchDetailsRouteHidesStaleSeriesReservationConfirmedNoticeWhenUserWasRemoved()
            throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/46").param("reservation", "recurringConfirmed"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("seriesReservationJoined", false))
                .andExpect(model().attribute("seriesReservationConfirmed", false))
                .andExpect(model().attribute("seriesReservationEnabled", true));
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
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/42"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hostCanManage", true))
                .andExpect(model().attribute("hostCanEdit", true))
                .andExpect(model().attribute("hostCanCancel", true))
                .andExpect(model().attribute("reservationEnabled", true))
                .andExpect(model().attribute("joinRequestEnabled", false))
                .andExpect(model().attribute("seriesReservationEnabled", false))
                .andExpect(model().attribute("seriesJoinRequestEnabled", false))
                .andExpect(model().attribute("hostEditPath", "/host/matches/42/edit"))
                .andExpect(model().attribute("hostCancelPath", "/host/matches/42/cancel"));
    }

    @Test
    void getRecurringMatchDetailsRouteForHostExposesSeriesManagementActions() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/46"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hostCanManage", true))
                .andExpect(model().attribute("hostCanEditSeries", true))
                .andExpect(model().attribute("hostCanCancelSeries", true))
                .andExpect(model().attribute("reservationEnabled", true))
                .andExpect(model().attribute("seriesReservationEnabled", true))
                .andExpect(model().attribute("hostSeriesEditPath", "/host/matches/46/series/edit"))
                .andExpect(
                        model().attribute(
                                        "hostSeriesCancelPath", "/host/matches/46/series/cancel"));
    }

    @Test
    void getApprovalRequiredMatchDetailsRouteForHostHidesJoinRequestActions() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/52"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hostCanManage", true))
                .andExpect(model().attribute("reservationEnabled", true))
                .andExpect(model().attribute("seriesReservationEnabled", true))
                .andExpect(model().attribute("joinRequestEnabled", false))
                .andExpect(model().attribute("seriesJoinRequestEnabled", false))
                .andExpect(model().attribute("hasPendingJoinRequest", false));
    }

    @Test
    void getRealMatchDetailsRouteForHostDisablesManagementOnCompletedEvent() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/44"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hostCanManage", true))
                .andExpect(model().attribute("hostCanEdit", false))
                .andExpect(model().attribute("hostCanCancel", false));
    }

    @Test
    void getRealMatchDetailsRouteWithSpanishHostActionLocalizesNotice() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/42").flashAttr("hostAction", "updated").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(
                        model().attribute(
                                        "hostActionNotice",
                                        "Tu evento fue actualizado correctamente."));
    }

    @Test
    void getRealMatchDetailsRouteWithSpanishSeriesHostActionLocalizesNotice() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(
                        get("/matches/46")
                                .flashAttr("hostAction", "seriesUpdated")
                                .param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(
                        model().attribute(
                                        "hostActionNotice",
                                        "Las pr\u00f3ximas fechas recurrentes "
                                                + "fueron actualizadas correctamente."));
    }

    @Test
    void getRealMatchDetailsRouteHidesUpdatedNoticeAfterRefresh() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/42"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hostActionNotice", Matchers.nullValue()));
    }

    @Test
    void postReservationRequestWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/matches/42/reservations")).andExpect(status().isUnauthorized());
    }

    @Test
    void postReservationRequestAsAuthenticatedUserRedirectsToConfirmedEvent() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/42/reservations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42"))
                .andExpect(flash().attribute("reservationStatus", "confirmed"));

        Assertions.assertEquals(42L, lastReservedMatchId.get());
        Assertions.assertEquals(9L, lastReservedUserId.get());
    }

    @Test
    void postReservationRequestWithSpanishLocaleLocalizesReservationErrors() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");
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
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/42/reservations/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42"))
                .andExpect(flash().attribute("reservationStatus", "cancelled"));

        Assertions.assertEquals(42L, lastCancelledReservationMatchId.get());
        Assertions.assertEquals(9L, lastCancelledReservationUserId.get());
    }

    @Test
    void postRecurringOccurrenceReservationCancelAsAuthenticatedUserCancelsOnlySelectedDate()
            throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/46/reservations/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/46"))
                .andExpect(flash().attribute("reservationStatus", "cancelled"));

        Assertions.assertEquals(46L, lastCancelledReservationMatchId.get());
        Assertions.assertEquals(9L, lastCancelledReservationUserId.get());
        Assertions.assertNull(lastCancelledSeriesMatchId.get());
        Assertions.assertNull(lastCancelledSeriesUserId.get());
    }

    @Test
    void postPrivateInviteOnlyReservationCancelRedirectsToUpcomingMatches() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/51/reservations/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events"));

        Assertions.assertEquals(51L, lastCancelledReservationMatchId.get());
        Assertions.assertEquals(9L, lastCancelledReservationUserId.get());
    }

    @Test
    void postPrivateInviteDeclineRedirectsToUpcomingMatches() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/51/invites/decline"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events"));
    }

    @Test
    void postReservationCancelWithSpanishLocaleLocalizesReservationErrors() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");
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
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/46/recurring-reservations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/46"))
                .andExpect(flash().attribute("reservationStatus", "recurringConfirmed"));

        Assertions.assertEquals(46L, lastReservedMatchId.get());
        Assertions.assertEquals(9L, lastReservedUserId.get());
    }

    @Test
    void postSeriesReservationRequestWithSpanishLocaleLocalizesReservationErrors()
            throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");
        seriesReservationFailure.set(
                new MatchReservationException("series_already_joined", "Already joined"));

        mockMvc.perform(post("/matches/46/recurring-reservations").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(
                        model().attribute(
                                        "seriesReservationError",
                                        "Tu cuenta ya tiene reservas confirmadas "
                                                + "para las fechas futuras recurrentes."));
    }

    @Test
    void postSeriesReservationCancelWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/matches/46/recurring-reservations/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postSeriesReservationCancelAsAuthenticatedUserRedirectsToCancelledEvent()
            throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/46/recurring-reservations/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/46"))
                .andExpect(flash().attribute("reservationStatus", "recurringCancelled"));

        Assertions.assertEquals(46L, lastCancelledSeriesMatchId.get());
        Assertions.assertEquals(9L, lastCancelledSeriesUserId.get());
    }

    @Test
    void postSeriesReservationCancelWithSpanishLocaleLocalizesReservationErrors() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");
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
    void postJoinRequestAsAuthenticatedUserRedirectsWithOneTimeNotice() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/56/join-requests"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/56"))
                .andExpect(flash().attribute("joinRequested", true));
    }

    @Test
    void postSeriesJoinRequestWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/matches/52/recurring-join-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postSeriesJoinRequestAsAuthenticatedUserRedirectsToRecurringRequestedEvent()
            throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/52/recurring-join-requests"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/52"))
                .andExpect(flash().attribute("seriesJoinRequested", true));

        Assertions.assertEquals(52L, lastSeriesJoinRequestMatchId.get());
        Assertions.assertEquals(9L, lastSeriesJoinRequestUserId.get());
    }

    @Test
    void postSeriesJoinRequestFailureRedirectsWithJoinErrorCode() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");
        seriesJoinRequestFailure.set(
                new MatchParticipationException("series_already_pending", "Already requested"));

        mockMvc.perform(post("/matches/52/recurring-join-requests"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/52?joinError=series_already_pending"));
    }

    @Test
    void getVerificationPreviewRendersConfirmPage() throws Exception {
        mockMvc.perform(get("/verifications/abc123"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification/confirm"))
                .andExpect(model().attributeExists("preview"));
    }

    @Test
    void postVerificationConfirmAuthenticatesAndRedirectsHome() throws Exception {
        final MvcResult result =
                mockMvc.perform(post("/verifications/abc123/confirm"))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/"))
                        .andReturn();

        final org.springframework.security.core.context.SecurityContext securityContext =
                (org.springframework.security.core.context.SecurityContext)
                        result.getRequest().getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        Assertions.assertNotNull(securityContext);
        Assertions.assertTrue(securityContext.getAuthentication().isAuthenticated());
        Assertions.assertEquals(
                9L,
                ((AuthenticatedUserPrincipal) securityContext.getAuthentication().getPrincipal())
                        .getUser()
                        .getId());
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
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
    void postHostPublishPassesValidCoordinatesToService() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("latitude", "-34.61")
                                .param("longitude", "-58.38")
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
        Assertions.assertEquals(-34.61, lastCreateMatchRequest.get().getLatitude());
        Assertions.assertEquals(-58.38, lastCreateMatchRequest.get().getLongitude());
    }

    @Test
    void postHostPublishRejectsPartialCoordinates() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("latitude", "-34.61")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "longitude"));

        Assertions.assertNull(lastCreateMatchRequest.get());
    }

    @Test
    void postHostPublishRejectsOutOfRangeCoordinates() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/new")
                                .param("title", "Host Test Match")
                                .param("description", "Friendly game")
                                .param("address", "Downtown Club")
                                .param("latitude", "-91")
                                .param("longitude", "-58.38")
                                .param("sport", "padel")
                                .param("visibility", "public")
                                .param("joinPolicy", "direct")
                                .param("eventDate", "2099-04-10")
                                .param("eventTime", "18:00")
                                .param("endDate", "2099-04-10")
                                .param("endTime", "19:30")
                                .param("maxPlayers", "8")
                                .param("pricePerPlayer", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attributeHasFieldErrors("createEventForm", "latitude"));

        Assertions.assertNull(lastCreateMatchRequest.get());
    }

    @Test
    void postHostPublishCreatesRecurringMatchWithOccurrenceCount() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
        Assertions.assertEquals("weekly", request.getRecurrence().getFrequency().getDbValue());
        Assertions.assertEquals(3, request.getRecurrence().getOccurrenceCount());
    }

    @Test
    void postHostPublishCreatesRecurringMatchWithUntilDate() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
        Assertions.assertEquals("until_date", request.getRecurrence().getEndMode().getDbValue());
        Assertions.assertEquals(LocalDate.of(2099, 4, 24), request.getRecurrence().getUntilDate());
    }

    @Test
    void postHostPublishRejectsInvalidRecurrenceFrequency() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

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
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/host/matches/42/edit")).andExpect(status().isNotFound());
    }

    @Test
    void getHostEditRouteForCompletedMatchReturnsNotFound() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches/44/edit")).andExpect(status().isNotFound());
    }

    @Test
    void getHostEditRouteForCancelledMatchReturnsNotFound() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches/45/edit")).andExpect(status().isNotFound());
    }

    @Test
    void getHostSeriesEditRouteRendersPrefilledFormForHost() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches/47/series/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/create-match"))
                .andExpect(model().attribute("isEditMode", true))
                .andExpect(model().attribute("isSeriesEditMode", true))
                .andExpect(model().attribute("formAction", "/host/matches/47/series/edit"))
                .andExpect(model().attribute("formTitle", "Edit recurring dates"))
                .andExpect(
                        model().attribute(
                                        "createEventForm",
                                        Matchers.hasProperty(
                                                "title", Matchers.is("Weekly Padel"))));
    }

    @Test
    void getHostSeriesEditRouteForSingleEventReturnsNotFound() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches/42/series/edit")).andExpect(status().isNotFound());
    }

    @Test
    void postHostEditRedirectsToDetailOnSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

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
                .andExpect(redirectedUrl("/matches/42"))
                .andExpect(flash().attribute("hostAction", "updated"));
    }

    @Test
    void postHostSeriesEditRedirectsToDetailOnSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/host/matches/47/series/edit")
                                .param("title", "Updated Weekly Padel")
                                .param("description", "Updated recurring game")
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
                .andExpect(redirectedUrl("/matches/47"))
                .andExpect(flash().attribute("hostAction", "seriesUpdated"));

        Assertions.assertEquals(47L, lastHostSeriesUpdatedMatchId.get());
        Assertions.assertEquals(7L, lastHostSeriesUpdatedUserId.get());
        Assertions.assertEquals("Updated Weekly Padel", lastUpdateMatchRequest.get().getTitle());
    }

    @Test
    void postHostEditPersistsPrivateVisibilityAsInviteOnly() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

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
                .andExpect(redirectedUrl("/matches/42"))
                .andExpect(flash().attribute("hostAction", "updated"));

        final Match updatedMatch = lastUpdatedMatch.get();
        Assertions.assertNotNull(updatedMatch);
        Assertions.assertEquals(EventVisibility.PRIVATE, updatedMatch.getVisibility());
        Assertions.assertEquals(EventJoinPolicy.INVITE_ONLY, updatedMatch.getJoinPolicy());
    }

    @Test
    void postHostEditWithCapacityBelowConfirmedRerendersFormWithError() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

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
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

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
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

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
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

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
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/host/matches/42/cancel")).andExpect(status().isNotFound());
    }

    @Test
    void postHostCancelRedirectsToDetailOnSuccess() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(post("/host/matches/42/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/42"))
                .andExpect(flash().attribute("hostAction", "cancelled"));
    }

    @Test
    void postHostCancelRecurringOccurrenceRedirectsToSelectedOccurrence() throws Exception {
        // Arrange
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        // Exercise and Assert
        mockMvc.perform(post("/host/matches/47/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/47"))
                .andExpect(flash().attribute("hostAction", "cancelled"));
        Assertions.assertEquals(47L, lastHostCancelledMatchId.get());
        Assertions.assertEquals(7L, lastHostCancelledUserId.get());
    }

    @Test
    void postHostCancelRecurringSeriesRedirectsToSelectedOccurrence() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(post("/host/matches/47/series/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/47"))
                .andExpect(flash().attribute("hostAction", "seriesCancelled"));

        Assertions.assertEquals(47L, lastHostSeriesCancelledMatchId.get());
        Assertions.assertEquals(7L, lastHostSeriesCancelledUserId.get());
    }

    @Test
    void postHostCancelRecurringSeriesForSingleEventReturnsNotFound() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(post("/host/matches/42/series/cancel")).andExpect(status().isNotFound());
    }

    @Test
    void postHostCancelForCompletedEventReturnsNotFound() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(post("/host/matches/44/cancel")).andExpect(status().isNotFound());
    }

    @Test
    void getHostJoinRequestsRouteRendersAggregateRequestsPage() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/requests"))
                .andExpect(status().isOk())
                .andExpect(view().name("host/participation/requests"))
                .andExpect(model().attribute("aggregateRequests", true))
                .andExpect(model().attributeExists("pendingRequests"))
                .andExpect(model().attribute("matchesUrl", "/events"));
    }

    @Test
    void removedDashboardRoutesAreNotMapped() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/host/matches")).andExpect(status().isNotFound());
        mockMvc.perform(get("/host/matches/finished")).andExpect(status().isNotFound());
        mockMvc.perform(get("/player/matches/past")).andExpect(status().isNotFound());
        mockMvc.perform(get("/player/matches/upcoming")).andExpect(status().isNotFound());
        mockMvc.perform(get("/player/matches/requests")).andExpect(status().isNotFound());
        mockMvc.perform(get("/player/matches/invites")).andExpect(status().isNotFound());
    }

    @Test
    void getEventsRouteRendersEventsPage() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/list"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attribute("pageTitleCode", "page.title.events"));
    }

    @Test
    void getEventsRouteDoesNotIncludePendingCategoryByDefault() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");
        currentUserHasSeriesJoinRequest.set(true);

        final MvcResult result =
                mockMvc.perform(get("/events"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("events/list"))
                        .andReturn();

        final List<EventCardViewModel> events = getEventsModel(result);
        Assertions.assertTrue(
                events.stream()
                        .noneMatch(event -> "Approval Future Padel".equals(event.getTitle())));
    }

    @Test
    void getEventsRouteIncludesPendingOnlyWhenPendingCategorySelected() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");
        currentUserHasSeriesJoinRequest.set(true);

        final MvcResult result =
                mockMvc.perform(get("/events").param("category", "pending"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("events/list"))
                        .andReturn();

        final List<EventCardViewModel> events = getEventsModel(result);
        Assertions.assertTrue(
                events.stream()
                        .anyMatch(event -> "Approval Future Padel".equals(event.getTitle())));
    }

    @Test
    void getEventsRouteShowsHostedAndGoingBadgesTogether() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");
        currentUserHasReservation.set(true);

        final MvcResult result =
                mockMvc.perform(get("/events").param("category", "hosted"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("events/list"))
                        .andReturn();

        final List<EventCardViewModel> events = getEventsModel(result);
        final EventCardViewModel hostedEvent =
                events.stream()
                        .filter(event -> "Sunrise Padel".equals(event.getTitle()))
                        .findFirst()
                        .orElseThrow(AssertionError::new);
        Assertions.assertTrue(
                hostedEvent.getRelationshipBadges().stream()
                        .anyMatch(badge -> "my_event".equals(badge.getType())));
        Assertions.assertTrue(
                hostedEvent.getRelationshipBadges().stream()
                        .anyMatch(badge -> "going".equals(badge.getType())));
    }

    @Test
    void getAccountRouteRendersPrivateAccountPageForAuthenticatedUsers() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/account"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/index"))
                .andExpect(model().attributeExists("accountProfile"))
                .andExpect(model().attributeExists("accountProfileForm"))
                .andExpect(model().attributeExists("shell"));
    }

    @Test
    void getAccountRouteWithSpanishLocaleLocalizesPublicProfileAction() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/account").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/index"))
                .andExpect(
                        model().attribute("accountPublicProfileLabel", "Ver perfil p\u00fablico"));
    }

    @Test
    void getAccountRouteWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/account")).andExpect(status().isUnauthorized());
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
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/account/edit")
                                .param("username", "updated_user")
                                .param("name", "Taylor")
                                .param("lastName", "Morgan")
                                .param("phone", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"))
                .andExpect(flash().attribute("accountUpdated", true));
    }

    @Test
    void postAccountEditRouteUpdatesPictureAndRedirects() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
                .andExpect(redirectedUrl("/account"))
                .andExpect(flash().attribute("accountUpdated", true));
    }

    @Test
    void postAccountEditRouteShowsLocalizedImageErrorForUnsupportedFormat() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");
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
                .andExpect(view().name("account/index"))
                .andExpect(model().attribute("accountProfileImageError", expectedMessage));
    }

    @Test
    void getPublicProfileRouteRendersPublicProfileForAnonymousUsers() throws Exception {
        mockMvc.perform(get("/users/host-player"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attributeExists("profilePage"))
                .andExpect(model().attributeExists("reviewSummary"))
                .andExpect(model().attributeExists("profileReviews"))
                .andExpect(model().attribute("reviewLikeLabel", "Likes"))
                .andExpect(model().attribute("reviewDislikeLabel", "Dislikes"))
                .andExpect(model().attribute("reviewFormVisible", false))
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
                                        "profilePage",
                                        Matchers.hasProperty(
                                                "email", Matchers.is("host@test.com"))))
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty(
                                                "profileImageUrl",
                                                Matchers.is(
                                                        "/assets/default-profile-avatar.svg"))));
    }

    @Test
    void getPublicProfileRouteShowsReviewActionsForEligibleAuthenticatedViewer() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/users/second-player"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attribute("reviewCanSubmit", true))
                .andExpect(model().attribute("reviewFormVisible", false))
                .andExpect(model().attribute("reviewLikeLabel", "Like"))
                .andExpect(model().attribute("reviewDislikeLabel", "Dislikes"))
                .andExpect(
                        model().attribute(
                                        "reviewFormPath",
                                        "/users/second-player?reviewFilter=both&reviewPage=1&reviewForm=open#reviews"))
                .andExpect(model().attributeExists("viewerReview"))
                .andExpect(
                        model().attribute(
                                        "reviewSummary",
                                        Matchers.hasProperty("reviewCount", Matchers.is(1L))));
    }

    @Test
    void getPublicProfileRouteFiltersPositiveReviews() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/users/second-player").param("reviewFilter", "positive"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attribute("selectedReviewFilter", "positive"))
                .andExpect(model().attribute("profileReviews", Matchers.hasSize(1)))
                .andExpect(
                        model().attribute(
                                        "profileReviews",
                                        Matchers.hasItem(
                                                Matchers.hasProperty(
                                                        "reaction", Matchers.is("like")))))
                .andExpect(
                        model().attribute(
                                        "reviewFilterOptions",
                                        Matchers.hasItem(
                                                Matchers.allOf(
                                                        Matchers.hasProperty(
                                                                "label", Matchers.is("Positive")),
                                                        Matchers.hasProperty(
                                                                "href",
                                                                Matchers.is(
                                                                        "/users/second-player?reviewFilter=positive&reviewPage=1#reviews")),
                                                        Matchers.hasProperty(
                                                                "active", Matchers.is(true))))));
    }

    @Test
    void getPublicProfileRoutePaginatesReviews() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/users/second-player").param("reviewPage", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attribute("reviewTotalPages", 3))
                .andExpect(
                        model().attribute(
                                        "reviewPreviousPageHref",
                                        "/users/second-player?reviewFilter=both&reviewPage=1#reviews"))
                .andExpect(
                        model().attribute(
                                        "reviewNextPageHref",
                                        "/users/second-player?reviewFilter=both&reviewPage=3#reviews"))
                .andExpect(
                        model().attribute(
                                        "reviewPaginationItems",
                                        Matchers.hasItem(
                                                Matchers.allOf(
                                                        Matchers.hasProperty(
                                                                "label", Matchers.is("2")),
                                                        Matchers.hasProperty(
                                                                "current", Matchers.is(true))))));
    }

    @Test
    void getPublicProfileRouteFallsBackForInvalidReviewPage() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/users/second-player").param("reviewPage", "bad"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(
                        model().attribute(
                                        "reviewNextPageHref",
                                        "/users/second-player?reviewFilter=both&reviewPage=2#reviews"));
    }

    @Test
    void getPublicProfileRouteOpensReviewFormWhenRequested() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        get("/users/second-player")
                                .param("reviewFilter", "positive")
                                .param("reviewPage", "2")
                                .param("reviewForm", "open"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attribute("reviewCanSubmit", true))
                .andExpect(model().attribute("reviewFormVisible", true))
                .andExpect(
                        model().attribute(
                                        "reviewSectionPath",
                                        "/users/second-player?reviewFilter=positive&reviewPage=1#reviews"));
    }

    @Test
    void postPublicProfileReviewSavesReviewAndRedirects() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(
                        post("/users/second-player/reviews")
                                .param("reaction", "dislike")
                                .param("comment", "Arrived late"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/second-player#reviews"))
                .andExpect(flash().attribute("reviewStatus", "saved"));
    }

    @Test
    void postPublicProfileReviewDeleteRemovesReviewAndRedirects() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(post("/users/second-player/reviews/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/second-player#reviews"))
                .andExpect(flash().attribute("reviewStatus", "deleted"));
    }

    @Test
    void getPublicProfileRouteUsesUploadedProfileImageWhenPresent() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

        mockMvc.perform(get("/users/host-player"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("profileEditHref", "/account"))
                .andExpect(model().attribute("reviewFormVisible", false));
    }

    @Test
    void getOtherPublicProfileRouteDoesNotShowEditAction() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "host@test.com", "host-player");

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
                .andExpect(
                        model().attribute(
                                        "profilePage",
                                        Matchers.hasProperty(
                                                "email", Matchers.is("host@test.com"))))
                .andExpect(model().attribute("profileUsernameLabel", "Usuario"))
                .andExpect(model().attribute("profileEmailLabel", "Email"))
                .andExpect(model().attribute("profilePhoneLabel", "Tel\u00e9fono"));
    }

    @Test
    void getUnknownPublicProfileRouteReturnsNotFound() throws Exception {
        mockMvc.perform(get("/users/missing-player")).andExpect(status().isNotFound());
    }

    @SuppressWarnings("unchecked")
    private List<EventCardViewModel> getEventsModel(final MvcResult result) {
        return (List<EventCardViewModel>) result.getModelAndView().getModel().get("events");
    }

    private static MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
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
