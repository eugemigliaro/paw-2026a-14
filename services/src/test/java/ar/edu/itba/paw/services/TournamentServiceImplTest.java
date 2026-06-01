package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.persistence.TournamentDao;
import ar.edu.itba.paw.persistence.TournamentSoloEntryDao;
import ar.edu.itba.paw.persistence.TournamentTeamDao;
import ar.edu.itba.paw.services.exceptions.TournamentLifecycleException;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
public class TournamentServiceImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-04-05T00:00:00Z");

    @Mock private TournamentDao tournamentDao;
    @Mock private TournamentSoloEntryDao tournamentSoloEntryDao;
    @Mock private TournamentTeamDao tournamentTeamDao;
    @Mock private TournamentMailService tournamentMailService;
    @Mock private ImageService imageService;
    @Mock private MessageSource messageSource;

    private TournamentServiceImpl tournamentService;

    @BeforeEach
    public void setUp() {
        tournamentService =
                new TournamentServiceImpl(
                        tournamentDao,
                        tournamentMailService,
                        imageService,
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
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void hostCanCreateTournamentOpenForRegistration() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final CreateTournamentRequest request = validCreateRequest();
        final Tournament tournament = tournament(10L, host, TournamentStatus.REGISTRATION);
        Mockito.when(
                        tournamentDao.create(
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
                                null,
                                request.getFormat(),
                                request.getBracketSize(),
                                request.getTeamSize(),
                                request.isAllowSoloSignup(),
                                request.isAllowTeamDraft(),
                                request.getRegistrationOpensAt(),
                                request.getRegistrationClosesAt(),
                                TournamentStatus.REGISTRATION))
                .thenReturn(tournament);

        // 2. Exercise
        final Tournament result = tournamentService.createTournament(host, request);

        // 3. Assert
        Assertions.assertSame(tournament, result);
        Assertions.assertEquals(TournamentStatus.REGISTRATION, result.getStatus());
    }

    @Test
    public void createFailsWithInvalidBracketSize() {
        // 1. Arrange
        final CreateTournamentRequest request =
                createRequest(
                        5, 1, true, false, futureRegistrationOpen(), futureRegistrationClose());

        // 2. Exercise
        final TournamentLifecycleException exception =
                Assertions.assertThrows(
                        TournamentLifecycleException.class,
                        () -> tournamentService.createTournament(UserUtils.getUser(1L), request));

        // 3. Assert
        Assertions.assertEquals(
                TournamentLifecycleFailureReason.INVALID_BRACKET_SIZE, exception.getReason());
    }

    @Test
    public void createFailsWithInvalidTeamSize() {
        // 1. Arrange
        final CreateTournamentRequest request =
                createRequest(
                        4, 0, true, false, futureRegistrationOpen(), futureRegistrationClose());

        // 2. Exercise
        final TournamentLifecycleException exception =
                Assertions.assertThrows(
                        TournamentLifecycleException.class,
                        () -> tournamentService.createTournament(UserUtils.getUser(1L), request));

        // 3. Assert
        Assertions.assertEquals(
                TournamentLifecycleFailureReason.INVALID_TEAM_SIZE, exception.getReason());
    }

    @Test
    public void createFailsWithInvalidRegistrationWindow() {
        // 1. Arrange
        final CreateTournamentRequest request =
                createRequest(
                        4,
                        1,
                        true,
                        false,
                        FIXED_NOW.plusSeconds(7200),
                        FIXED_NOW.plusSeconds(3600));

        // 2. Exercise
        final TournamentLifecycleException exception =
                Assertions.assertThrows(
                        TournamentLifecycleException.class,
                        () -> tournamentService.createTournament(UserUtils.getUser(1L), request));

        // 3. Assert
        Assertions.assertEquals(
                TournamentLifecycleFailureReason.INVALID_REGISTRATION_WINDOW,
                exception.getReason());
    }

    @Test
    public void createFailsWithPastRegistrationClose() {
        // 1. Arrange
        final CreateTournamentRequest request =
                createRequest(
                        4,
                        1,
                        true,
                        false,
                        FIXED_NOW.minusSeconds(7200),
                        FIXED_NOW.minusSeconds(3600));

        // 2. Exercise
        final TournamentLifecycleException exception =
                Assertions.assertThrows(
                        TournamentLifecycleException.class,
                        () -> tournamentService.createTournament(UserUtils.getUser(1L), request));

        // 3. Assert
        Assertions.assertEquals(
                TournamentLifecycleFailureReason.INVALID_REGISTRATION_WINDOW,
                exception.getReason());
    }

    @Test
    public void nonHostCannotUpdateTournament() {
        // 1. Arrange
        Mockito.when(tournamentDao.findById(10L))
                .thenReturn(
                        Optional.of(
                                tournament(
                                        10L,
                                        UserUtils.getUser(1L),
                                        TournamentStatus.REGISTRATION)));

        // 2. Exercise
        final TournamentLifecycleException exception =
                Assertions.assertThrows(
                        TournamentLifecycleException.class,
                        () ->
                                tournamentService.update(
                                        10L, UserUtils.getUser(2L), validUpdateRequest()));

        // 3. Assert
        Assertions.assertEquals(TournamentLifecycleFailureReason.FORBIDDEN, exception.getReason());
    }

    @Test
    public void hostCanUpdateEditableFieldsBeforeCompletion() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final Tournament tournament = tournament(10L, host, TournamentStatus.REGISTRATION);
        final UpdateTournamentRequest request = validUpdateRequest();
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentDao.update(tournament))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. Exercise
        final Tournament result = tournamentService.update(10L, host, request);

        // 3. Assert
        Assertions.assertEquals(request.getTitle(), result.getTitle());
        Assertions.assertEquals(request.getAddress(), result.getAddress());
        Assertions.assertEquals(request.getStartsAt(), result.getStartsAt());
        Assertions.assertEquals(request.getBracketSize(), result.getBracketSize());
        Assertions.assertEquals(request.getTeamSize(), result.getTeamSize());
        Assertions.assertEquals(request.getRegistrationOpensAt(), result.getRegistrationOpensAt());
        Assertions.assertEquals(
                request.getRegistrationClosesAt(), result.getRegistrationClosesAt());
        Assertions.assertEquals(FIXED_NOW, result.getUpdatedAt());
    }

    @Test
    public void adminModCanUpdateTournamentForAnotherHost() {
        // 1. Arrange
        authenticateAdminMod();
        final Tournament tournament =
                tournament(10L, UserUtils.getUser(1L), TournamentStatus.REGISTRATION);
        final UpdateTournamentRequest request = validUpdateRequest();
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentDao.update(tournament))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. Exercise
        final Tournament result = tournamentService.update(10L, UserUtils.getUser(99L), request);

        // 3. Assert
        Assertions.assertEquals(request.getTitle(), result.getTitle());
        Assertions.assertEquals(FIXED_NOW, result.getUpdatedAt());
    }

    @Test
    public void updateFailsWhenTournamentIsCompleted() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        Mockito.when(tournamentDao.findById(10L))
                .thenReturn(Optional.of(tournament(10L, host, TournamentStatus.COMPLETED)));

        // 2. Exercise
        final TournamentLifecycleException exception =
                Assertions.assertThrows(
                        TournamentLifecycleException.class,
                        () -> tournamentService.update(10L, host, validUpdateRequest()));

        // 3. Assert
        Assertions.assertEquals(
                TournamentLifecycleFailureReason.NOT_EDITABLE, exception.getReason());
    }

    @Test
    public void updateFailsAfterRegistrationState() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        Mockito.when(tournamentDao.findById(10L))
                .thenReturn(Optional.of(tournament(10L, host, TournamentStatus.BRACKET_SETUP)));

        // 2. Exercise
        final TournamentLifecycleException exception =
                Assertions.assertThrows(
                        TournamentLifecycleException.class,
                        () -> tournamentService.update(10L, host, validUpdateRequest()));

        // 3. Assert
        Assertions.assertEquals(
                TournamentLifecycleFailureReason.NOT_EDITABLE, exception.getReason());
    }

    @Test
    public void cancelFailsWhenTournamentIsCompleted() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        Mockito.when(tournamentDao.findById(10L))
                .thenReturn(Optional.of(tournament(10L, host, TournamentStatus.COMPLETED)));

        // 2. Exercise
        final TournamentLifecycleException exception =
                Assertions.assertThrows(
                        TournamentLifecycleException.class,
                        () -> tournamentService.cancel(10L, host, "No longer available"));

        // 3. Assert
        Assertions.assertEquals(
                TournamentLifecycleFailureReason.NOT_CANCELLABLE, exception.getReason());
    }

    @Test
    public void hostCanCancelTournamentBeforeCompletion() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final Tournament tournament = tournament(10L, host, TournamentStatus.IN_PROGRESS);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(tournament));
        Mockito.when(tournamentDao.update(tournament))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. Exercise
        final Tournament result = tournamentService.cancel(10L, host, "Weather");

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.CANCELLED, result.getStatus());
        Assertions.assertEquals(FIXED_NOW, result.getCancelledAt());
        Assertions.assertEquals("Weather", result.getCancelReason());
    }

    @Test
    public void searchPublicTournamentsPaginatesAndNormalizesInvalidSortToSoonest() {
        // 1. Arrange
        final AtomicReference<EventSort> capturedSort = new AtomicReference<>();
        final AtomicInteger capturedOffset = new AtomicInteger(-1);
        final AtomicInteger capturedLimit = new AtomicInteger(-1);
        Mockito.when(
                        tournamentDao.countPublicTournaments(
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyList(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any()))
                .thenReturn(25);
        Mockito.when(
                        tournamentDao.findPublicTournaments(
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyList(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.anyInt()))
                .thenAnswer(
                        invocation -> {
                            capturedSort.set(invocation.getArgument(6));
                            capturedOffset.set(invocation.getArgument(9));
                            capturedLimit.set(invocation.getArgument(10));
                            return List.of(
                                    tournament(
                                            10L,
                                            UserUtils.getUser(1L),
                                            TournamentStatus.REGISTRATION));
                        });

        // 2. Exercise
        final PaginatedResult<Tournament> result =
                tournamentService.searchPublicTournaments(
                        "cup",
                        List.of(Sport.PADEL),
                        Instant.parse("2026-04-06T00:00:00Z"),
                        Instant.parse("2026-04-10T00:00:00Z"),
                        EventSort.SOONEST,
                        9,
                        10,
                        ZoneId.of("UTC"),
                        BigDecimal.ZERO,
                        BigDecimal.TEN,
                        null,
                        null);

        // 3. Assert
        Assertions.assertEquals(3, result.getPage());
        Assertions.assertEquals(25, result.getTotalCount());
        Assertions.assertEquals(EventSort.SOONEST, capturedSort.get());
        Assertions.assertEquals(20, capturedOffset.get());
        Assertions.assertEquals(10, capturedLimit.get());
    }

    @Test
    public void searchPublicTournamentsUsesDistanceSortOnlyWhenCoordinatesArePresent() {
        // 1. Arrange
        final AtomicReference<EventSort> capturedSort = new AtomicReference<>();
        Mockito.when(
                        tournamentDao.countPublicTournaments(
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyList(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any()))
                .thenReturn(1);
        Mockito.when(
                        tournamentDao.findPublicTournaments(
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyList(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.anyInt(),
                                ArgumentMatchers.anyInt()))
                .thenAnswer(
                        invocation -> {
                            capturedSort.set(invocation.getArgument(6));
                            return List.of(
                                    tournament(
                                            10L,
                                            UserUtils.getUser(1L),
                                            TournamentStatus.REGISTRATION));
                        });

        // 2. Exercise
        tournamentService.searchPublicTournaments(
                "",
                List.of(),
                null,
                null,
                EventSort.DISTANCE,
                1,
                12,
                ZoneId.of("UTC"),
                null,
                null,
                -34.60,
                -58.38);

        // 3. Assert
        Assertions.assertEquals(EventSort.DISTANCE, capturedSort.get());
    }

    @Test
    public void findHostedTournamentsIncludesSoloPoolAndTeamMemberships() {
        // 1. Arrange
        final User user = UserUtils.getUser(1L);
        final Tournament hosted = tournament(10L, user, TournamentStatus.REGISTRATION);
        tournament(11L, UserUtils.getUser(2L), TournamentStatus.REGISTRATION);
        tournament(12L, UserUtils.getUser(3L), TournamentStatus.IN_PROGRESS);
        Mockito.when(
                        tournamentDao.findDashboardTournaments(
                                user,
                                Boolean.FALSE,
                                Boolean.TRUE,
                                "",
                                null,
                                null,
                                null,
                                null,
                                null,
                                EventSort.SOONEST,
                                null,
                                null,
                                0,
                                12))
                .thenReturn(List.of(hosted));
        Mockito.when(
                        tournamentDao.countDashboardTournaments(
                                user,
                                Boolean.FALSE,
                                Boolean.TRUE,
                                "",
                                null,
                                null,
                                null,
                                null,
                                null))
                .thenReturn(1);

        // 2. Exercise
        final PaginatedResult<Tournament> result =
                tournamentService.findDashboardTournaments(
                        user,
                        false,
                        true,
                        "",
                        null,
                        null,
                        null,
                        EventSort.SOONEST,
                        1,
                        12,
                        ZoneId.of("UTC"),
                        null,
                        null,
                        null,
                        null);

        // 3. Assert
        Assertions.assertEquals(1, result.getTotalCount());
        Assertions.assertTrue(result.getItems().contains(hosted));
    }

    @Test
    public void findHostedTournamentsIncludesActiveTournamentWithoutScheduleInUpcoming() {
        // 1. Arrange
        final User user = UserUtils.getUser(1L);
        final Tournament unscheduled =
                tournamentWithSchedule(10L, user, TournamentStatus.REGISTRATION, null, null);
        final Tournament completed =
                tournamentWithSchedule(11L, user, TournamentStatus.COMPLETED, null, null);
        Mockito.when(
                        tournamentDao.findDashboardTournaments(
                                user,
                                Boolean.FALSE,
                                Boolean.TRUE,
                                "",
                                null,
                                null,
                                null,
                                null,
                                null,
                                EventSort.SOONEST,
                                null,
                                null,
                                0,
                                12))
                .thenReturn(List.of(unscheduled, completed));
        Mockito.when(
                        tournamentDao.countDashboardTournaments(
                                user,
                                Boolean.FALSE,
                                Boolean.TRUE,
                                "",
                                null,
                                null,
                                null,
                                null,
                                null))
                .thenReturn(1);

        // 2. Exercise
        final PaginatedResult<Tournament> result =
                tournamentService.findDashboardTournaments(
                        user,
                        false,
                        true,
                        "",
                        null,
                        null,
                        null,
                        EventSort.SOONEST,
                        1,
                        12,
                        ZoneId.of("UTC"),
                        null,
                        null,
                        null,
                        null);

        // 3. Assert
        Assertions.assertEquals(1, result.getTotalCount());
        Assertions.assertEquals(unscheduled, result.getItems().get(0));
    }

    @Test
    public void findHostedTournamentsExcludesActiveTournamentWithoutScheduleFromPast() {
        // 1. Arrange
        final User user = UserUtils.getUser(1L);
        tournamentWithSchedule(10L, user, TournamentStatus.REGISTRATION, null, null);
        final Tournament completed =
                tournamentWithSchedule(11L, user, TournamentStatus.COMPLETED, null, null);
        Mockito.when(
                        tournamentDao.findDashboardTournaments(
                                user,
                                Boolean.TRUE,
                                Boolean.TRUE,
                                "",
                                null,
                                null,
                                null,
                                null,
                                null,
                                EventSort.SOONEST,
                                null,
                                null,
                                0,
                                12))
                .thenReturn(List.of(completed));
        Mockito.when(
                        tournamentDao.countDashboardTournaments(
                                user, Boolean.TRUE, Boolean.TRUE, "", null, null, null, null, null))
                .thenReturn(1);

        // 2. Exercise
        final PaginatedResult<Tournament> result =
                tournamentService.findDashboardTournaments(
                        user,
                        true,
                        true,
                        "",
                        null,
                        null,
                        null,
                        EventSort.SOONEST,
                        1,
                        12,
                        ZoneId.of("UTC"),
                        null,
                        null,
                        null,
                        null);

        // 3. Assert
        Assertions.assertEquals(1, result.getTotalCount());
        Assertions.assertEquals(completed, result.getItems().get(0));
    }

    private static CreateTournamentRequest validCreateRequest() {
        return createRequest(
                4, 1, true, false, futureRegistrationOpen(), futureRegistrationClose());
    }

    private static CreateTournamentRequest createRequest(
            final int bracketSize,
            final int teamSize,
            final boolean allowSoloSignup,
            final boolean allowTeamDraft,
            final Instant registrationOpensAt,
            final Instant registrationClosesAt) {
        return new CreateTournamentRequest(
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
                teamSize,
                allowSoloSignup,
                allowTeamDraft,
                registrationOpensAt,
                registrationClosesAt);
    }

    private static UpdateTournamentRequest validUpdateRequest() {
        return new UpdateTournamentRequest(
                Sport.PADEL,
                "Updated Cup",
                "Updated description",
                "Updated Street 456",
                -34.61,
                -58.39,
                FIXED_NOW.plusSeconds(172800),
                FIXED_NOW.plusSeconds(176400),
                BigDecimal.TEN,
                null,
                8,
                2,
                FIXED_NOW.plusSeconds(3600),
                FIXED_NOW.plusSeconds(7200));
    }

    private static Tournament tournament(
            final long id, final User host, final TournamentStatus status) {
        return tournamentWithRegistrationWindow(
                id, host, status, futureRegistrationOpen(), futureRegistrationClose());
    }

    private static Tournament tournamentWithRegistrationWindow(
            final long id,
            final User host,
            final TournamentStatus status,
            final Instant registrationOpensAt,
            final Instant registrationClosesAt) {
        return tournamentWithSchedule(
                id,
                host,
                status,
                FIXED_NOW.plusSeconds(86400),
                FIXED_NOW.plusSeconds(90000),
                registrationOpensAt,
                registrationClosesAt);
    }

    private static Tournament tournamentWithSchedule(
            final long id,
            final User host,
            final TournamentStatus status,
            final Instant startsAt,
            final Instant endsAt) {
        return tournamentWithSchedule(
                id,
                host,
                status,
                startsAt,
                endsAt,
                futureRegistrationOpen(),
                futureRegistrationClose());
    }

    private static Tournament tournamentWithSchedule(
            final long id,
            final User host,
            final TournamentStatus status,
            final Instant startsAt,
            final Instant endsAt,
            final Instant registrationOpensAt,
            final Instant registrationClosesAt) {
        return new Tournament(
                id,
                host,
                Sport.FOOTBALL,
                "Saturday Cup",
                "Friendly tournament",
                "Club Street 123",
                -34.60,
                -58.38,
                startsAt,
                endsAt,
                BigDecimal.ZERO,
                null,
                TournamentFormat.SINGLE_ELIMINATION,
                4,
                1,
                true,
                false,
                registrationOpensAt,
                registrationClosesAt,
                status,
                FIXED_NOW,
                FIXED_NOW);
    }

    private static Instant futureRegistrationOpen() {
        return FIXED_NOW.plusSeconds(3600);
    }

    private static Instant futureRegistrationClose() {
        return FIXED_NOW.plusSeconds(7200);
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
