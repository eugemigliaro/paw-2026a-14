package ar.edu.itba.paw.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.match.MatchException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationAlreadyJoinedException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationNotJoinedException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationSeriesAlreadyJoinedException;
import ar.edu.itba.paw.models.exceptions.matchParticipation.MatchParticipationSeriesNotJoinedException;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.services.MatchActionCapabilities;
import ar.edu.itba.paw.services.MatchInteractionState;
import ar.edu.itba.paw.services.MatchManagementPermissions;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.webapp.security.annotation.CurrentUserArgumentResolver;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.MatchUtils;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class EventControllerTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T00:00:00Z");

    private MockMvc mockMvc;
    private MatchService matchService;
    private MatchReservationService matchReservationService;
    private MatchParticipationService matchParticipationService;
    private PlayerReviewService playerReviewService;

    private MatchException reservationFailure;
    private MatchException reservationCancellationFailure;
    private MatchException seriesReservationFailure;
    private MatchException seriesCancellationFailure;
    private boolean currentUserHasReservation;
    private boolean currentUserHasSeriesReservation;
    private boolean currentUserHasJoinRequest;
    private boolean currentUserHasSeriesJoinRequest;
    private boolean occurrenceReservationCancellationUsed;
    private boolean seriesReservationCancellationUsed;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        reservationFailure = null;
        reservationCancellationFailure = null;
        seriesReservationFailure = null;
        seriesCancellationFailure = null;
        currentUserHasReservation = false;
        currentUserHasSeriesReservation = false;
        currentUserHasJoinRequest = false;
        currentUserHasSeriesJoinRequest = false;
        occurrenceReservationCancellationUsed = false;
        seriesReservationCancellationUsed = false;

        final Match realMatch =
                MatchUtils.match(42L)
                        .address("Downtown Club")
                        .coords(-34.61, -58.38)
                        .title("Sunrise Padel")
                        .description("Friendly\\n doubles session")
                        .price(BigDecimal.TEN)
                        .joinedPlayers(2)
                        .build();
        final Match footballMatch =
                MatchUtils.match(43L)
                        .sport(Sport.FOOTBALL)
                        .hostId(8L)
                        .address("North Arena")
                        .title("Afterwork Football")
                        .description("Fast 5v5")
                        .startsAt(Instant.parse("2026-04-07T19:00:00Z"))
                        .endsAt(Instant.parse("2026-04-07T20:30:00Z"))
                        .maxPlayers(10)
                        .price(BigDecimal.ZERO)
                        .joinedPlayers(4)
                        .build();
        final Match completedMatch =
                MatchUtils.match(44L)
                        .sport(Sport.BASKETBALL)
                        .address("South Sports Center")
                        .title("Weekend Basketball")
                        .description("Completed tournament")
                        .startsAt(Instant.parse("2026-04-03T19:00:00Z"))
                        .endsAt(Instant.parse("2026-04-03T21:00:00Z"))
                        .maxPlayers(10)
                        .price(BigDecimal.ZERO)
                        .status(EventStatus.COMPLETED)
                        .joinedPlayers(10)
                        .build();
        final Match cancelledFutureMatch =
                MatchUtils.match(45L)
                        .sport(Sport.TENNIS)
                        .address("City Tennis Club")
                        .title("Sunday Tennis")
                        .description("Cancelled due to weather")
                        .startsAt(Instant.parse("2026-04-08T12:00:00Z"))
                        .endsAt(Instant.parse("2026-04-08T14:00:00Z"))
                        .maxPlayers(6)
                        .price(BigDecimal.TEN)
                        .status(EventStatus.CANCELLED)
                        .joinedPlayers(2)
                        .build();
        final Match privateInviteOnlyMatch =
                MatchUtils.match(51L)
                        .address("Members Club")
                        .title("Invite Night Padel")
                        .description("Private doubles session")
                        .startsAt(Instant.parse("2026-04-10T21:00:00Z"))
                        .endsAt(Instant.parse("2026-04-10T22:30:00Z"))
                        .price(BigDecimal.TEN)
                        .visibility(EventVisibility.PRIVATE)
                        .joinPolicy(EventJoinPolicy.INVITE_ONLY)
                        .joinedPlayers(2)
                        .build();
        final Match recurringMatch =
                MatchUtils.match(46L)
                        .address("Downtown Club")
                        .title("Weekly Padel")
                        .description("Friendly recurring session")
                        .startsAt(Instant.parse("2026-04-09T18:00:00Z"))
                        .endsAt(Instant.parse("2026-04-09T19:30:00Z"))
                        .price(BigDecimal.TEN)
                        .joinedPlayers(1)
                        .series(MatchUtils.getMatchSeries(600L, UserUtils.getUser(7L)))
                        .seriesOccurrenceIndex(1)
                        .build();
        final Match recurringSecondOccurrence =
                MatchUtils.match(47L)
                        .address("Downtown Club")
                        .title("Weekly Padel")
                        .description("Friendly recurring session")
                        .startsAt(Instant.parse("2026-04-16T18:00:00Z"))
                        .endsAt(Instant.parse("2026-04-16T19:30:00Z"))
                        .price(BigDecimal.TEN)
                        .joinedPlayers(0)
                        .series(MatchUtils.getMatchSeries(600L, UserUtils.getUser(7L)))
                        .seriesOccurrenceIndex(2)
                        .build();
        final Match recurringPastOccurrence =
                MatchUtils.match(48L)
                        .address("Downtown Club")
                        .title("Weekly Padel")
                        .description("Friendly recurring session")
                        .startsAt(Instant.parse("2026-03-26T18:00:00Z"))
                        .endsAt(Instant.parse("2026-03-26T19:30:00Z"))
                        .price(BigDecimal.TEN)
                        .joinedPlayers(4)
                        .series(MatchUtils.getMatchSeries(600L, UserUtils.getUser(7L)))
                        .seriesOccurrenceIndex(0)
                        .build();
        final Match recurringInProgressOccurrence =
                MatchUtils.match(55L)
                        .address("Downtown Club")
                        .title("Weekly Padel")
                        .description("Friendly recurring session")
                        .startsAt(Instant.parse("2026-04-04T23:00:00Z"))
                        .endsAt(Instant.parse("2026-04-05T01:00:00Z"))
                        .price(BigDecimal.TEN)
                        .joinedPlayers(4)
                        .series(MatchUtils.getMatchSeries(600L, UserUtils.getUser(7L)))
                        .seriesOccurrenceIndex(1)
                        .build();
        final Match recurringFullOccurrence =
                MatchUtils.match(49L)
                        .address("Downtown Club")
                        .title("Weekly Padel")
                        .description("Friendly recurring session")
                        .startsAt(Instant.parse("2026-04-23T18:00:00Z"))
                        .endsAt(Instant.parse("2026-04-23T19:30:00Z"))
                        .price(BigDecimal.TEN)
                        .joinedPlayers(8)
                        .series(MatchUtils.getMatchSeries(600L, UserUtils.getUser(7L)))
                        .seriesOccurrenceIndex(3)
                        .build();
        final Match recurringCancelledOccurrence =
                MatchUtils.match(50L)
                        .address("Downtown Club")
                        .title("Weekly Padel")
                        .description("Friendly recurring session")
                        .startsAt(Instant.parse("2026-04-30T18:00:00Z"))
                        .endsAt(Instant.parse("2026-04-30T19:30:00Z"))
                        .price(BigDecimal.TEN)
                        .status(EventStatus.CANCELLED)
                        .joinedPlayers(0)
                        .series(MatchUtils.getMatchSeries(600L, UserUtils.getUser(7L)))
                        .seriesOccurrenceIndex(4)
                        .build();
        final Match approvalRecurringMatch =
                MatchUtils.match(52L)
                        .address("Downtown Club")
                        .title("Approval Weekly Padel")
                        .description("Recurring session with host approval")
                        .startsAt(Instant.parse("2026-04-09T20:00:00Z"))
                        .endsAt(Instant.parse("2026-04-09T21:30:00Z"))
                        .price(BigDecimal.TEN)
                        .joinPolicy(EventJoinPolicy.APPROVAL_REQUIRED)
                        .joinedPlayers(1)
                        .series(MatchUtils.getMatchSeries(700L, UserUtils.getUser(7L)))
                        .seriesOccurrenceIndex(1)
                        .build();
        final Match approvalRecurringSecondOccurrence =
                MatchUtils.match(53L)
                        .address("Downtown Club")
                        .title("Approval Weekly Padel")
                        .description("Recurring session with host approval")
                        .startsAt(Instant.parse("2026-04-16T20:00:00Z"))
                        .endsAt(Instant.parse("2026-04-16T21:30:00Z"))
                        .price(BigDecimal.TEN)
                        .joinPolicy(EventJoinPolicy.APPROVAL_REQUIRED)
                        .joinedPlayers(0)
                        .series(MatchUtils.getMatchSeries(700L, UserUtils.getUser(7L)))
                        .seriesOccurrenceIndex(2)
                        .build();
        final Match approvalRecurringPastOccurrence =
                MatchUtils.match(54L)
                        .address("Downtown Club")
                        .title("Approval Weekly Padel")
                        .description("Recurring session with host approval")
                        .startsAt(Instant.parse("2026-03-26T20:00:00Z"))
                        .endsAt(Instant.parse("2026-03-26T21:30:00Z"))
                        .price(BigDecimal.TEN)
                        .joinPolicy(EventJoinPolicy.APPROVAL_REQUIRED)
                        .joinedPlayers(4)
                        .series(MatchUtils.getMatchSeries(700L, UserUtils.getUser(7L)))
                        .seriesOccurrenceIndex(0)
                        .build();
        final Match pendingFutureMatch =
                MatchUtils.match(56L)
                        .address("Downtown Club")
                        .title("Approval Future Padel")
                        .description("Future session with host approval")
                        .startsAt(Instant.parse("2030-04-09T20:00:00Z"))
                        .endsAt(Instant.parse("2030-04-09T21:30:00Z"))
                        .price(BigDecimal.TEN)
                        .joinPolicy(EventJoinPolicy.APPROVAL_REQUIRED)
                        .joinedPlayers(1)
                        .build();

        matchService = Mockito.mock(MatchService.class);

        Mockito.when(matchService.findMatchById(42L)).thenReturn(Optional.of(realMatch));
        Mockito.when(matchService.findMatchById(43L)).thenReturn(Optional.of(footballMatch));
        Mockito.when(matchService.findMatchById(44L)).thenReturn(Optional.of(completedMatch));
        Mockito.when(matchService.findMatchById(45L)).thenReturn(Optional.of(cancelledFutureMatch));
        Mockito.when(matchService.findMatchById(51L))
                .thenReturn(Optional.of(privateInviteOnlyMatch));
        Mockito.when(matchService.findMatchById(46L)).thenReturn(Optional.of(recurringMatch));
        Mockito.when(matchService.findMatchById(47L))
                .thenReturn(Optional.of(recurringSecondOccurrence));
        Mockito.when(matchService.findMatchById(48L))
                .thenReturn(Optional.of(recurringPastOccurrence));
        Mockito.when(matchService.findMatchById(55L))
                .thenReturn(Optional.of(recurringInProgressOccurrence));
        Mockito.when(matchService.findMatchById(49L))
                .thenReturn(Optional.of(recurringFullOccurrence));
        Mockito.when(matchService.findMatchById(50L))
                .thenReturn(Optional.of(recurringCancelledOccurrence));
        Mockito.when(matchService.findMatchById(52L))
                .thenReturn(Optional.of(approvalRecurringMatch));
        Mockito.when(matchService.findMatchById(53L))
                .thenReturn(Optional.of(approvalRecurringSecondOccurrence));
        Mockito.when(matchService.findMatchById(54L))
                .thenReturn(Optional.of(approvalRecurringPastOccurrence));
        Mockito.when(matchService.findMatchById(56L)).thenReturn(Optional.of(pendingFutureMatch));
        Mockito.when(matchService.findMatchById(ArgumentMatchers.anyLong()))
                .thenAnswer(
                        invocation -> {
                            final Long matchId = invocation.getArgument(0);
                            return switch (matchId.intValue()) {
                                case 42 -> Optional.of(realMatch);
                                case 43 -> Optional.of(footballMatch);
                                case 44 -> Optional.of(completedMatch);
                                case 45 -> Optional.of(cancelledFutureMatch);
                                case 51 -> Optional.of(privateInviteOnlyMatch);
                                case 46 -> Optional.of(recurringMatch);
                                case 47 -> Optional.of(recurringSecondOccurrence);
                                case 48 -> Optional.of(recurringPastOccurrence);
                                case 55 -> Optional.of(recurringInProgressOccurrence);
                                case 49 -> Optional.of(recurringFullOccurrence);
                                case 50 -> Optional.of(recurringCancelledOccurrence);
                                case 52 -> Optional.of(approvalRecurringMatch);
                                case 53 -> Optional.of(approvalRecurringSecondOccurrence);
                                case 54 -> Optional.of(approvalRecurringPastOccurrence);
                                case 56 -> Optional.of(pendingFutureMatch);
                                default -> Optional.empty();
                            };
                        });
        Mockito.when(
                        matchService.findVisibleMatchById(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenAnswer(
                        invocation ->
                                matchService.findMatchById(invocation.getArgument(0, Long.class)));

        Mockito.when(matchService.findSeriesOccurrences(ArgumentMatchers.anyLong()))
                .thenAnswer(
                        invocation -> {
                            final Long seriesId = invocation.getArgument(0);
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
                        });

        Mockito.when(
                        matchService.findSeriesOccurrencesPage(
                                ArgumentMatchers.anyLong(),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.anyInt()))
                .thenAnswer(
                        invocation -> {
                            final Long seriesId = invocation.getArgument(0);
                            final int page = invocation.getArgument(1);
                            final int pageSize = invocation.getArgument(2);
                            final List<Match> all = matchService.findSeriesOccurrences(seriesId);
                            final int safePage = Math.max(1, page);
                            final int offset = Math.max(0, (safePage - 1) * pageSize);
                            final List<Match> items =
                                    offset >= all.size()
                                            ? List.of()
                                            : all.subList(
                                                    offset,
                                                    Math.min(offset + pageSize, all.size()));
                            return new PaginatedResult<>(items, all.size(), safePage, pageSize);
                        });

        Mockito.when(matchService.findConfirmedParticipants(ArgumentMatchers.anyLong()))
                .thenAnswer(
                        invocation -> {
                            final Long matchId = invocation.getArgument(0);
                            return matchId == 42L
                                    ? List.of(UserUtils.getUser(2L), UserUtils.getUser(3L))
                                    : List.of();
                        });

        Mockito.when(
                        matchService.actionCapabilities(
                                ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            final Match match = invocation.getArgument(0);
                            final User viewer = invocation.getArgument(1);
                            return actionCapabilities(match, viewer);
                        });
        Mockito.when(
                        matchService.getMatchManagementPermissions(
                                ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            final Match match = invocation.getArgument(0);
                            final User viewer = invocation.getArgument(1);
                            return managementPermissions(match, viewer);
                        });
        Mockito.when(
                        matchService.getMatchInteractionState(
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            final Match match = invocation.getArgument(0);
                            final List<Match> seriesOccurrences = invocation.getArgument(1);
                            final User viewer = invocation.getArgument(2);
                            return interactionState(match, seriesOccurrences, viewer);
                        });

        matchReservationService = Mockito.mock(MatchReservationService.class);

        Mockito.when(
                        matchReservationService.hasActiveReservation(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            final Long matchId = invocation.getArgument(0);
                            final User user = invocation.getArgument(1);
                            if (user == null) {
                                return false;
                            }
                            if (currentUserHasReservation
                                    && (user.getId() == 9L || user.getId() == 7L)
                                    && (matchId == 42L || matchId == 51L)) {
                                return true;
                            }
                            return currentUserHasSeriesReservation
                                    && user.getId() == 9L
                                    && (matchId == 46L || matchId == 47L);
                        });

        Mockito.when(
                        matchReservationService.findActiveFutureReservationMatchIdsForSeries(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            final Long seriesId = invocation.getArgument(0);
                            final User user = invocation.getArgument(1);
                            if (currentUserHasSeriesReservation
                                    && user != null
                                    && user.getId() == 9L
                                    && seriesId == 600L) {
                                return Set.of(46L, 47L);
                            }
                            return Set.of();
                        });

        Mockito.doAnswer(
                        invocation -> {
                            final User user = invocation.getArgument(1);
                            if (reservationFailure != null) {
                                throw reservationFailure;
                            }
                            if (user == null) {
                                throw new MatchParticipationNotJoinedException();
                            }
                            return null;
                        })
                .when(matchReservationService)
                .reserveSpot(ArgumentMatchers.anyLong(), ArgumentMatchers.any());

        Mockito.doAnswer(
                        invocation -> {
                            final User user = invocation.getArgument(1);
                            if (seriesReservationFailure != null) {
                                throw seriesReservationFailure;
                            }
                            if (user == null) {
                                throw new MatchParticipationNotJoinedException();
                            }
                            return null;
                        })
                .when(matchReservationService)
                .reserveSeries(ArgumentMatchers.anyLong(), ArgumentMatchers.any());

        Mockito.doAnswer(
                        invocation -> {
                            final User user = invocation.getArgument(1);
                            seriesReservationCancellationUsed = true;
                            if (seriesCancellationFailure != null) {
                                throw seriesCancellationFailure;
                            }
                            if (user == null) {
                                throw new MatchParticipationNotJoinedException();
                            }
                            return null;
                        })
                .when(matchReservationService)
                .cancelSeriesReservations(ArgumentMatchers.anyLong(), ArgumentMatchers.any());

        matchParticipationService = Mockito.mock(MatchParticipationService.class);

        Mockito.when(
                        matchParticipationService.hasPendingRequest(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            final Long matchId = invocation.getArgument(0);
                            final User user = invocation.getArgument(1);
                            if (user == null) {
                                throw new MatchParticipationNotJoinedException();
                            }
                            return currentUserHasJoinRequest
                                    && user.getId() == 9L
                                    && (matchId == 52L || matchId == 53L || matchId == 56L);
                        });

        Mockito.when(
                        matchParticipationService.hasPendingSeriesRequest(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            final Long matchId = invocation.getArgument(0);
                            final User user = invocation.getArgument(1);
                            return currentUserHasSeriesJoinRequest
                                    && user != null
                                    && user.getId() == 9L
                                    && (matchId == 52L || matchId == 53L);
                        });

        Mockito.when(
                        matchParticipationService.findPendingFutureRequestMatchIdsForSeries(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            final Long seriesId = invocation.getArgument(0);
                            final User user = invocation.getArgument(1);
                            if (currentUserHasSeriesJoinRequest
                                    && user != null
                                    && user.getId() == 9L
                                    && seriesId == 700L) {
                                return Set.of(52L, 53L);
                            }
                            return Set.of();
                        });

        Mockito.doAnswer(
                        invocation -> {
                            final User host = invocation.getArgument(1);
                            final User target = invocation.getArgument(2);
                            if (host.equals(target)) {
                                occurrenceReservationCancellationUsed = true;
                                if (reservationCancellationFailure != null) {
                                    throw reservationCancellationFailure;
                                }
                            }
                            return null;
                        })
                .when(matchParticipationService)
                .removeParticipant(
                        ArgumentMatchers.anyLong(), ArgumentMatchers.any(), ArgumentMatchers.any());

        Mockito.when(
                        matchParticipationService.findPendingRequests(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            final Long matchId = invocation.getArgument(0);
                            final User host = invocation.getArgument(1);
                            if (matchId == 52L && host != null && host.getId() == 7L) {
                                return List.of(UserUtils.getUser(9L));
                            }
                            return List.of();
                        });

        Mockito.when(
                        matchParticipationService.hasInvitation(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenReturn(false);

        Mockito.when(
                        matchParticipationService.findInvitedUsers(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            final Long matchId = invocation.getArgument(0);
                            final User host = invocation.getArgument(1);
                            if (matchId == 51L && host != null && host.getId() == 7L) {
                                return List.of(UserUtils.getUser(9L));
                            }
                            return List.of();
                        });

        Mockito.when(
                        matchParticipationService.findDeclinedInvitees(
                                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            final Long matchId = invocation.getArgument(0);
                            final User host = invocation.getArgument(1);
                            if (matchId == 51L && host != null && host.getId() == 7L) {
                                return List.of(UserUtils.getUser(10L));
                            }
                            return List.of();
                        });

        playerReviewService = Mockito.mock(PlayerReviewService.class);
        Mockito.when(playerReviewService.findReviewableUserIds(ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            final User reviewer = invocation.getArgument(0);
                            return reviewer != null && reviewer.getId().equals(9L)
                                    ? Set.of(3L)
                                    : Set.of();
                        });

        final MessageSource messageSource = messageSource();

        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));

        mockMvc =
                MockMvcBuilders.standaloneSetup(
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
                                        14))
                        .setViewResolvers(viewResolver)
                        .setLocaleResolver(localeResolver())
                        .addInterceptors(localeChangeInterceptor())
                        .defaultRequest(get("/").locale(Locale.ENGLISH))
                        .setConversionService(conversionService())
                        .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getEventReturnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(get("/matches/999")).andExpect(status().isNotFound());
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
                                        "aboutParagraphs",
                                        Matchers.contains("Friendly\n doubles session")))
                .andExpect(model().attribute("hostProfileHref", Matchers.is("/users/user7")))
                .andExpect(
                        model().attribute(
                                        "hostProfileImageUrl",
                                        Matchers.is("/assets/default-profile-avatar.svg")))
                .andExpect(model().attribute("mapAvailable", Matchers.is(true)))
                .andExpect(
                        model().attribute(
                                        "mapTileUrlTemplate",
                                        Matchers.is("/assets/tiles/{z}/{x}/{y}.png")))
                .andExpect(model().attribute("mapLatitude", Matchers.closeTo(-34.61, 0.000001)))
                .andExpect(model().attribute("mapLongitude", Matchers.closeTo(-58.38, 0.000001)))
                .andExpect(
                        model().attribute(
                                        "participants",
                                        Matchers.contains(
                                                Matchers.allOf(
                                                        Matchers.hasProperty(
                                                                "username", Matchers.is("user2"))),
                                                Matchers.allOf(
                                                        Matchers.hasProperty(
                                                                "username",
                                                                Matchers.is("user3"))))))
                .andExpect(
                        model().attribute(
                                        "userProfileImageUrls",
                                        Matchers.allOf(
                                                Matchers.hasEntry(
                                                        2L, "/assets/default-profile-avatar.svg"),
                                                Matchers.hasEntry(
                                                        3L, "/assets/default-profile-avatar.svg"))))
                .andExpect(
                        model().attribute(
                                        "participantRemovePaths",
                                        Matchers.allOf(
                                                Matchers.not(Matchers.hasKey(2L)),
                                                Matchers.not(Matchers.hasKey(3L)))));
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
                                        "participants",
                                        Matchers.contains(
                                                Matchers.hasProperty(
                                                        "username", Matchers.is("user2")),
                                                Matchers.hasProperty(
                                                        "username", Matchers.is("user3")))))
                .andExpect(
                        model().attribute(
                                        "participantReviewHrefs",
                                        Matchers.allOf(
                                                Matchers.not(Matchers.hasKey(2L)),
                                                Matchers.hasEntry(
                                                        3L,
                                                        "/users/"
                                                                + "user3?reviewForm=open#reviews"))));
    }

    @Test
    void getRealMatchDetailsRouteForJoinedUserExposesReservationCancellation() throws Exception {
        currentUserHasReservation = true;
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("isConfirmedParticipant", true))
                .andExpect(model().attributeExists("matchActionCapabilities"))
                .andExpect(
                        model().attribute(
                                        "reservationCancelPath",
                                        "/matches/42/reservations/cancel"));
    }

    @Test
    void getPrivateInviteOnlyMatchDetailsForJoinedUserExposesReservationCancellation()
            throws Exception {
        currentUserHasReservation = true;
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/51"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("isConfirmedParticipant", true))
                .andExpect(model().attributeExists("matchActionCapabilities"))
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
                .andExpect(model().attributeExists("matchActionCapabilities"))
                .andExpect(model().attribute("isInvitedPlayer", false))
                .andExpect(model().attribute("hostPendingInviteCount", 1))
                .andExpect(model().attribute("hostPendingInvitesOpen", true))
                .andExpect(model().attribute("hostDeclinedInvites", Matchers.hasSize(1)));
    }

    @Test
    void getRecurringMatchDetailsRouteExposesRecurringOccurrenceStates() throws Exception {
        // Page 1 (size 5) shows first 5 of 6 occurrences: past, in-progress, current, second, full
        mockMvc.perform(get("/matches/46"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("occurrences", Matchers.hasSize(5)))
                .andExpect(
                        model().attribute(
                                        "occurrenceDisplayStateKeys",
                                        Matchers.hasEntry(48L, "completed")))
                .andExpect(
                        model().attribute(
                                        "occurrenceDisplayStateKeys",
                                        Matchers.hasEntry(55L, "inProgress")))
                .andExpect(
                        model().attribute(
                                        "occurrenceStatusTones",
                                        Matchers.hasEntry(55L, "in-progress")))
                .andExpect(
                        model().attribute(
                                        "occurrenceDisplayStateKeys",
                                        Matchers.hasEntry(49L, "full")))
                .andExpect(model().attribute("seriesReservationEnabled", true))
                .andExpect(
                        model().attribute(
                                        "seriesReservationPath",
                                        "/matches/46/recurring-reservations"));

        // Page 2 shows the 6th occurrence: cancelled
        mockMvc.perform(get("/matches/46").param("seriesPage", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("occurrences", Matchers.hasSize(1)))
                .andExpect(
                        model().attribute(
                                        "occurrenceDisplayStateKeys",
                                        Matchers.hasEntry(50L, "cancelled")))
                .andExpect(
                        model().attribute(
                                        "occurrenceVisibleHrefs",
                                        Matchers.not(Matchers.hasKey(50L))));
    }

    @Test
    void getRecurringMatchDetailsRouteLocalizesInProgressOccurrenceState() throws Exception {
        mockMvc.perform(get("/matches/46").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(
                        model().attribute(
                                        "occurrenceDisplayStateKeys",
                                        Matchers.hasEntry(55L, "inProgress")));
    }

    @Test
    void getRecurringApprovalRequiredMatchDetailsRouteExposesSingleAndSeriesJoinRequests()
            throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/52"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attributeExists("matchActionCapabilities"))
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
        currentUserHasSeriesJoinRequest = true;
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/52"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("hasPendingJoinRequest", false))
                .andExpect(model().attributeExists("matchActionCapabilities"))
                .andExpect(model().attribute("seriesJoinRequestEnabled", false))
                .andExpect(model().attribute("seriesJoinRequestPending", true));
    }

    @Test
    void getRecurringApprovalRequiredMatchDetailsRouteHidesReservationErrorWhenJoinRequestPending()
            throws Exception {
        currentUserHasSeriesJoinRequest = true;
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
        currentUserHasJoinRequest = true;
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
        currentUserHasJoinRequest = true;
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

        // Cancelled occurrence is the 6th item, so it appears on page 2 (page size = 5)
        mockMvc.perform(get("/matches/46").param("seriesPage", "2"))
                .andExpect(status().isOk())
                .andExpect(
                        model().attribute(
                                        "occurrenceDisplayStateKeys",
                                        Matchers.hasEntry(50L, "cancelled")))
                .andExpect(
                        model().attribute(
                                        "occurrenceVisibleHrefs",
                                        Matchers.hasEntry(50L, "/matches/50")));
    }

    @Test
    void getRecurringMatchDetailsRouteForJoinedUserExposesSeriesCancellation() throws Exception {
        currentUserHasSeriesReservation = true;
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/46"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("isConfirmedParticipant", true))
                .andExpect(model().attributeExists("matchActionCapabilities"))
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
                .andExpect(model().attributeExists("matchActionCapabilities"));
    }

    @Test
    void getRealMatchDetailsRouteShowsReservationConfirmedNoticeWhenUserIsStillJoined()
            throws Exception {
        currentUserHasReservation = true;
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(get("/matches/42").param("reservation", "confirmed"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("isConfirmedParticipant", true))
                .andExpect(model().attribute("reservationConfirmed", true))
                .andExpect(model().attributeExists("matchActionCapabilities"));
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
                .andExpect(model().attributeExists("matchActionCapabilities"))
                .andExpect(
                        model().attribute("eventStateNoticeCode", "event.state.completedNotice"));
    }

    @Test
    void getRealMatchDetailsRouteForHostExposesManagementActions() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/42"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hostCanManage", true))
                .andExpect(model().attributeExists("matchActionCapabilities"))
                .andExpect(model().attribute("seriesReservationEnabled", false))
                .andExpect(model().attribute("seriesJoinRequestEnabled", false))
                .andExpect(model().attribute("hostEditPath", "/host/matches/42/edit"))
                .andExpect(model().attribute("hostCancelPath", "/host/matches/42/cancel"))
                .andExpect(
                        model().attribute(
                                        "participantRemovePaths",
                                        Matchers.allOf(
                                                Matchers.hasEntry(
                                                        2L,
                                                        "/host/matches/42/participants/2/remove"),
                                                Matchers.hasEntry(
                                                        3L,
                                                        "/host/matches/42/participants/3/remove"))));
    }

    @Test
    void getRecurringMatchDetailsRouteForHostExposesSeriesManagementActions() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/46"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hostCanManage", true))
                .andExpect(model().attributeExists("matchActionCapabilities"))
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
                .andExpect(model().attributeExists("matchActionCapabilities"))
                .andExpect(model().attribute("seriesReservationEnabled", true))
                .andExpect(model().attribute("seriesJoinRequestEnabled", false))
                .andExpect(model().attribute("hasPendingJoinRequest", false))
                .andExpect(model().attribute("hostPendingRequestCount", 1))
                .andExpect(model().attribute("hostPendingRequestsOpen", true))
                .andExpect(model().attribute("hostPendingRequests", Matchers.hasSize(1)));
    }

    @Test
    void getRealMatchDetailsRouteForHostDisablesManagementOnCompletedEvent() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/44"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hostCanManage", true))
                .andExpect(model().attributeExists("matchActionCapabilities"));
    }

    @Test
    void getRealMatchDetailsRouteUnderSpanishLocaleExposesHostActionCode() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/42").flashAttr("hostAction", "updated").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hostActionCode", "updated"));
    }

    @Test
    void getRecurringMatchDetailsRouteExposesSeriesHostActionCode() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/46").flashAttr("hostAction", "seriesUpdated"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hostActionCode", "seriesUpdated"));
    }

    @Test
    void getRealMatchDetailsRouteHidesUpdatedNoticeAfterRefresh() throws Exception {
        AuthenticationUtils.authenticateUser(7L, "host@test.com", "host-player");

        mockMvc.perform(get("/matches/42"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hostActionCode", Matchers.nullValue()));
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
    }

    @Test
    void postReservationRequestWhenAlreadyReservedShowsError() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");
        reservationFailure = new MatchParticipationAlreadyJoinedException();

        mockMvc.perform(post("/matches/42/reservations").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attribute("reservationRequiresLogin", false))
                .andExpect(model().attributeExists("reservationError"));
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
    }

    @Test
    void postRecurringOccurrenceReservationCancelAsAuthenticatedUserCancelsOnlySelectedDate()
            throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/46/reservations/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches/46"))
                .andExpect(flash().attribute("reservationStatus", "cancelled"));

        Assertions.assertTrue(occurrenceReservationCancellationUsed);
        Assertions.assertFalse(seriesReservationCancellationUsed);
    }

    @Test
    void postPrivateInviteOnlyReservationCancelRedirectsToUpcomingMatches() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");

        mockMvc.perform(post("/matches/51/reservations/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matches"));
    }

    @Test
    void postReservationCancelWithNoActiveReservationShowsError() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");
        reservationCancellationFailure = new MatchParticipationNotJoinedException();

        mockMvc.perform(post("/matches/42/reservations/cancel").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attributeExists("reservationError"));
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
    }

    @Test
    void postSeriesReservationRequestWhenAlreadyJoinedShowsError() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");
        seriesReservationFailure = new MatchParticipationSeriesAlreadyJoinedException();

        mockMvc.perform(post("/matches/46/recurring-reservations").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attributeExists("seriesReservationError"));
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
    }

    @Test
    void postSeriesReservationCancelWithNoFutureReservationsShowsError() throws Exception {
        AuthenticationUtils.authenticateUser(9L, "player@test.com", "player-account");
        seriesCancellationFailure = new MatchParticipationSeriesNotJoinedException();

        mockMvc.perform(post("/matches/46/recurring-reservations/cancel").param("lang", "es"))
                .andExpect(status().isOk())
                .andExpect(view().name("matches/detail"))
                .andExpect(model().attributeExists("seriesReservationError"));
    }

    private MatchActionCapabilities actionCapabilities(final Match match, final User viewer) {
        final boolean host =
                viewer != null
                        && match.getHost() != null
                        && viewer.getId().equals(match.getHost().getId());
        final boolean activeParticipant =
                viewer != null
                        && currentUserHasReservation
                        && (Long.valueOf(42L).equals(match.getId())
                                || Long.valueOf(51L).equals(match.getId()));
        final boolean seriesParticipant =
                viewer != null && currentUserHasSeriesReservation && match.isRecurringOccurrence();
        final boolean invited =
                viewer != null
                        && (Long.valueOf(51L).equals(match.getId())
                                || Long.valueOf(50L).equals(match.getId()));
        final boolean visible =
                match.getStatus() == EventStatus.DRAFT
                        ? host
                        : match.getVisibility() == EventVisibility.PRIVATE
                                        || match.getStatus() == EventStatus.CANCELLED
                                ? host || activeParticipant || seriesParticipant || invited
                                : match.getVisibility() == EventVisibility.PUBLIC;
        final boolean editable =
                host
                        && match.getStatus() != EventStatus.COMPLETED
                        && match.getStatus() != EventStatus.CANCELLED
                        && match.getStartsAt().isAfter(FIXED_NOW);
        final boolean reserve =
                match.getStatus() == EventStatus.OPEN
                        && match.getStartsAt().isAfter(FIXED_NOW)
                        && match.getAvailableSpots() > 0
                        && (host
                                || (match.getVisibility() == EventVisibility.PUBLIC
                                        && match.getJoinPolicy() == EventJoinPolicy.DIRECT));
        final boolean cancelReservation =
                (activeParticipant || seriesParticipant)
                        && match.getStatus() == EventStatus.OPEN
                        && match.getStartsAt().isAfter(FIXED_NOW);
        final boolean requestToJoin =
                match.getVisibility() == EventVisibility.PUBLIC
                        && match.getJoinPolicy() == EventJoinPolicy.APPROVAL_REQUIRED
                        && match.getStatus() == EventStatus.OPEN
                        && match.getStartsAt().isAfter(FIXED_NOW)
                        && match.getAvailableSpots() > 0
                        && !host
                        && !activeParticipant;
        return new MatchActionCapabilities(
                visible,
                editable,
                editable,
                editable,
                reserve,
                cancelReservation,
                requestToJoin,
                editable && match.isRecurringOccurrence(),
                editable && match.isRecurringOccurrence());
    }

    private MatchManagementPermissions managementPermissions(final Match match, final User viewer) {
        final boolean host =
                viewer != null
                        && match.getHost() != null
                        && viewer.getId().equals(match.getHost().getId());
        final MatchActionCapabilities capabilities = actionCapabilities(match, viewer);
        return new MatchManagementPermissions(
                host,
                host,
                host,
                capabilities.isCanEdit(),
                capabilities.isCanCancel(),
                capabilities.isCanEditSeries(),
                capabilities.isCanCancelSeries());
    }

    private MatchInteractionState interactionState(
            final Match match, final List<Match> seriesOccurrences, final User viewer) {
        final MatchActionCapabilities capabilities = actionCapabilities(match, viewer);
        final boolean authenticated = viewer != null;
        final boolean host =
                authenticated
                        && match.getHost() != null
                        && viewer.getId().equals(match.getHost().getId());
        final boolean confirmedParticipant =
                authenticated
                        && ((currentUserHasReservation
                                        && (Long.valueOf(42L).equals(match.getId())
                                                || Long.valueOf(51L).equals(match.getId())))
                                || (currentUserHasSeriesReservation
                                        && match.isRecurringOccurrence()));
        final boolean pendingJoinRequest =
                authenticated
                        && currentUserHasJoinRequest
                        && (Long.valueOf(52L).equals(match.getId())
                                || Long.valueOf(53L).equals(match.getId())
                                || Long.valueOf(56L).equals(match.getId()));
        final boolean seriesJoinRequestPending =
                authenticated
                        && currentUserHasSeriesJoinRequest
                        && match.isRecurringOccurrence()
                        && Long.valueOf(700L).equals(match.getSeries().getId());
        final boolean invitedPlayer =
                authenticated
                        && !host
                        && (Long.valueOf(51L).equals(match.getId())
                                || Long.valueOf(50L).equals(match.getId()));
        final boolean recurring = match.isRecurringOccurrence();
        final boolean seriesHasAvailableFutureOccurrence =
                recurring
                        && seriesOccurrences.stream()
                                .anyMatch(
                                        occurrence ->
                                                occurrence.getStatus() == EventStatus.OPEN
                                                        && occurrence
                                                                .getStartsAt()
                                                                .isAfter(FIXED_NOW)
                                                        && occurrence.getAvailableSpots() > 0);
        final boolean directSeriesReservationAvailable =
                recurring
                        && (host || match.getJoinPolicy() == EventJoinPolicy.DIRECT)
                        && seriesHasAvailableFutureOccurrence
                        && !confirmedParticipant;
        final boolean seriesJoinRequestAvailable =
                recurring
                        && !host
                        && match.getJoinPolicy() == EventJoinPolicy.APPROVAL_REQUIRED
                        && seriesHasAvailableFutureOccurrence
                        && !pendingJoinRequest
                        && !seriesJoinRequestPending;
        return new MatchInteractionState(
                confirmedParticipant,
                pendingJoinRequest,
                invitedPlayer,
                authenticated && capabilities.isCanReserve(),
                authenticated && capabilities.isCanCancelReservation(),
                directSeriesReservationAvailable,
                currentUserHasSeriesReservation && recurring,
                authenticated && currentUserHasSeriesReservation && recurring,
                authenticated && capabilities.isCanRequestToJoin() && !pendingJoinRequest,
                authenticated && seriesJoinRequestAvailable,
                seriesJoinRequestPending,
                !authenticated && capabilities.isCanReserve(),
                !authenticated && directSeriesReservationAvailable,
                !authenticated && seriesJoinRequestAvailable);
    }

    private static MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    private static DefaultFormattingConversionService conversionService() {
        return new DefaultFormattingConversionService();
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
}
