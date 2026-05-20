package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.persistence.TournamentDao;
import ar.edu.itba.paw.services.exceptions.TournamentLifecycleException;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
    @Mock private MessageSource messageSource;

    private TournamentServiceImpl tournamentService;

    @BeforeEach
    public void setUp() {
        tournamentService =
                new TournamentServiceImpl(
                        tournamentDao, messageSource, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
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
    public void hostCanCreateDraft() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final CreateTournamentRequest request = validCreateRequest();
        final Tournament draft = tournament(10L, host, TournamentStatus.DRAFT);
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
                                request.getBannerImageMetadata(),
                                request.getFormat(),
                                request.getBracketSize(),
                                request.getTeamSize(),
                                request.isAllowSoloSignup(),
                                request.isAllowTeamDraft(),
                                request.getRegistrationOpensAt(),
                                request.getRegistrationClosesAt(),
                                TournamentStatus.DRAFT))
                .thenReturn(draft);

        // 2. Exercise
        final Tournament result = tournamentService.createDraft(host, request);

        // 3. Assert
        Assertions.assertSame(draft, result);
        Assertions.assertEquals(TournamentStatus.DRAFT, result.getStatus());
    }

    @Test
    public void nonHostCannotPublishTournament() {
        // 1. Arrange
        Mockito.when(tournamentDao.findById(10L))
                .thenReturn(
                        Optional.of(
                                tournament(10L, UserUtils.getUser(1L), TournamentStatus.DRAFT)));

        // 2. Exercise
        final TournamentLifecycleException exception =
                Assertions.assertThrows(
                        TournamentLifecycleException.class,
                        () -> tournamentService.publish(10L, UserUtils.getUser(2L)));

        // 3. Assert
        Assertions.assertEquals(TournamentLifecycleFailureReason.FORBIDDEN, exception.getReason());
    }

    @Test
    public void hostCanPublishValidDraft() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final Tournament draft = tournament(10L, host, TournamentStatus.DRAFT);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(draft));
        Mockito.when(tournamentDao.update(draft))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. Exercise
        final Tournament result = tournamentService.publish(10L, host);

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.REGISTRATION, result.getStatus());
        Assertions.assertEquals(FIXED_NOW, result.getPublishedAt());
        Assertions.assertEquals(FIXED_NOW, result.getUpdatedAt());
    }

    @Test
    public void adminModCanPublishTournamentForAnotherHost() {
        // 1. Arrange
        authenticateAdminMod();
        final Tournament draft = tournament(10L, UserUtils.getUser(1L), TournamentStatus.DRAFT);
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(draft));
        Mockito.when(tournamentDao.update(draft))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. Exercise
        final Tournament result = tournamentService.publish(10L, UserUtils.getUser(99L));

        // 3. Assert
        Assertions.assertEquals(TournamentStatus.REGISTRATION, result.getStatus());
        Assertions.assertEquals(FIXED_NOW, result.getPublishedAt());
    }

    @Test
    public void publishFailsWithInvalidBracketSize() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final Tournament draft =
                tournament(
                        10L,
                        host,
                        TournamentStatus.DRAFT,
                        TournamentFormat.SINGLE_ELIMINATION,
                        5,
                        1,
                        true,
                        false,
                        FIXED_NOW.plusSeconds(3600),
                        FIXED_NOW.plusSeconds(7200));
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(draft));

        // 2. Exercise
        final TournamentLifecycleException exception =
                Assertions.assertThrows(
                        TournamentLifecycleException.class,
                        () -> tournamentService.publish(10L, host));

        // 3. Assert
        Assertions.assertEquals(
                TournamentLifecycleFailureReason.INVALID_BRACKET_SIZE, exception.getReason());
    }

    @Test
    public void publishFailsWithInvalidTeamSize() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final Tournament draft =
                tournament(
                        10L,
                        host,
                        TournamentStatus.DRAFT,
                        TournamentFormat.SINGLE_ELIMINATION,
                        4,
                        0,
                        true,
                        false,
                        FIXED_NOW.plusSeconds(3600),
                        FIXED_NOW.plusSeconds(7200));
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(draft));

        // 2. Exercise
        final TournamentLifecycleException exception =
                Assertions.assertThrows(
                        TournamentLifecycleException.class,
                        () -> tournamentService.publish(10L, host));

        // 3. Assert
        Assertions.assertEquals(
                TournamentLifecycleFailureReason.INVALID_TEAM_SIZE, exception.getReason());
    }

    @Test
    public void publishFailsWithInvalidRegistrationWindow() {
        // 1. Arrange
        final User host = UserUtils.getUser(1L);
        final Tournament draft =
                tournament(
                        10L,
                        host,
                        TournamentStatus.DRAFT,
                        TournamentFormat.SINGLE_ELIMINATION,
                        4,
                        1,
                        true,
                        false,
                        FIXED_NOW.plusSeconds(7200),
                        FIXED_NOW.plusSeconds(3600));
        Mockito.when(tournamentDao.findById(10L)).thenReturn(Optional.of(draft));

        // 2. Exercise
        final TournamentLifecycleException exception =
                Assertions.assertThrows(
                        TournamentLifecycleException.class,
                        () -> tournamentService.publish(10L, host));

        // 3. Assert
        Assertions.assertEquals(
                TournamentLifecycleFailureReason.INVALID_REGISTRATION_WINDOW,
                exception.getReason());
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
        Assertions.assertEquals(FIXED_NOW, result.getUpdatedAt());
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

    private static CreateTournamentRequest validCreateRequest() {
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
                4,
                1,
                true,
                false,
                FIXED_NOW.plusSeconds(3600),
                FIXED_NOW.plusSeconds(7200));
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
                FIXED_NOW.plusSeconds(3600),
                FIXED_NOW.plusSeconds(7200));
    }

    private static Tournament tournament(
            final long id, final User host, final TournamentStatus status) {
        return tournament(
                id,
                host,
                status,
                TournamentFormat.SINGLE_ELIMINATION,
                4,
                1,
                true,
                false,
                FIXED_NOW.plusSeconds(3600),
                FIXED_NOW.plusSeconds(7200));
    }

    private static Tournament tournament(
            final long id,
            final User host,
            final TournamentStatus status,
            final TournamentFormat format,
            final int bracketSize,
            final int teamSize,
            final boolean allowSoloSignup,
            final boolean allowTeamDraft,
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
                FIXED_NOW.plusSeconds(86400),
                FIXED_NOW.plusSeconds(90000),
                BigDecimal.ZERO,
                null,
                format,
                bracketSize,
                teamSize,
                allowSoloSignup,
                allowTeamDraft,
                registrationOpensAt,
                registrationClosesAt,
                status,
                FIXED_NOW,
                FIXED_NOW);
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
