package ar.edu.itba.paw.services.security;

import static org.mockito.Mockito.when;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.internal.MatchDataService;
import ar.edu.itba.paw.services.internal.PlayerReviewDataService;
import ar.edu.itba.paw.services.internal.TournamentDataService;
import ar.edu.itba.paw.services.internal.UserDataService;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityServiceImplTest {

    private SecurityServiceImpl securityService;
    private MatchDataService matchDataService;
    private TournamentDataService tournamentDataService;
    private PlayerReviewDataService playerReviewService;
    private UserDataService userService;
    private ModerationService moderationService;

    @BeforeEach
    void setUp() {
        matchDataService = Mockito.mock(MatchDataService.class);
        tournamentDataService = Mockito.mock(TournamentDataService.class);
        playerReviewService = Mockito.mock(PlayerReviewDataService.class);
        userService = Mockito.mock(UserDataService.class);
        moderationService = Mockito.mock(ModerationService.class);
        securityService =
                new SecurityServiceImpl(
                        matchDataService,
                        tournamentDataService,
                        playerReviewService,
                        userService,
                        moderationService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    static class TestPrincipal implements AuthenticatedPrincipal {
        private final User user;

        TestPrincipal(final User user) {
            this.user = user;
        }

        @Override
        public User getAuthenticatedUser() {
            return user;
        }
    }

    @Test
    void isAuthenticatedAndCurrentUserId() {
        final User u = UserUtils.getUser(42L);
        final TestPrincipal p = new TestPrincipal(u);
        final var token =
                new UsernamePasswordAuthenticationToken(
                        p, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(token);

        Assertions.assertTrue(securityService.isAuthenticated());
        Assertions.assertEquals(42L, securityService.currentUser().getId());
    }

    @Test
    void unsupportedPrincipalIsNotAuthenticatedAsApplicationUser() {
        // Arrange
        final var token =
                new UsernamePasswordAuthenticationToken(
                        "unsupported-principal",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(token);

        // Exercise
        final boolean authenticated = securityService.isAuthenticated();
        final User currentUser = securityService.currentUser();

        // Assert
        Assertions.assertFalse(authenticated);
        Assertions.assertNull(currentUser);
    }

    @Test
    void isHostDelegatesToMatchService() {
        final User u = UserUtils.getUser(99L);
        final TestPrincipal p = new TestPrincipal(u);
        final var token =
                new UsernamePasswordAuthenticationToken(
                        p, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(token);

        final Match match = Mockito.mock(Match.class);
        final User host = Mockito.mock(User.class);

        when(host.getId()).thenReturn(99L);
        when(match.getHost()).thenReturn(host);

        when(matchDataService.findById(10L)).thenReturn(Optional.of(match));

        Assertions.assertTrue(securityService.isHost(10L));
    }

    @Test
    void canActAsAdminModReturnsTrueForMatchingAdminPrincipal() {
        // Arrange
        final User user = UserUtils.getUser(99L);
        final TestPrincipal principal = new TestPrincipal(user);
        final var token =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN_MOD")));
        SecurityContextHolder.getContext().setAuthentication(token);

        // Exercise
        final boolean result = securityService.canActAsAdminMod(UserUtils.getUser(99L));

        // Assert
        Assertions.assertTrue(result);
    }

    @Test
    void canActAsAdminModReturnsFalseForMatchingRegularPrincipal() {
        // Arrange
        final User user = UserUtils.getUser(99L);
        final TestPrincipal principal = new TestPrincipal(user);
        final var token =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(token);

        // Exercise
        final boolean result = securityService.canActAsAdminMod(UserUtils.getUser(99L));

        // Assert
        Assertions.assertFalse(result);
    }

    @Test
    void canActAsAdminModReturnsFalseForDetachedDifferentActor() {
        // Arrange
        final User user = UserUtils.getUser(99L);
        final TestPrincipal principal = new TestPrincipal(user);
        final var token =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN_MOD")));
        SecurityContextHolder.getContext().setAuthentication(token);

        // Exercise
        final boolean result = securityService.canActAsAdminMod(UserUtils.getUser(100L));

        // Assert
        Assertions.assertFalse(result);
    }

    @Test
    void canCloseRegistrationAllowsHostDuringRegistration() {
        // Arrange
        final User user = UserUtils.getUser(99L);
        final TestPrincipal principal = new TestPrincipal(user);
        final var token =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(token);

        final Tournament tournament = Mockito.mock(Tournament.class);
        final User host = Mockito.mock(User.class);
        when(host.getId()).thenReturn(99L);
        when(tournament.getHost()).thenReturn(host);
        when(tournament.getStatus()).thenReturn(TournamentStatus.REGISTRATION);
        when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));

        // Exercise + Assert
        Assertions.assertTrue(securityService.canCloseRegistration(10L));
    }

    @Test
    void canCloseRegistrationDeniesNonHostRegularUser() {
        // Arrange
        final User user = UserUtils.getUser(99L);
        final TestPrincipal principal = new TestPrincipal(user);
        final var token =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(token);

        final Tournament tournament = Mockito.mock(Tournament.class);
        final User host = Mockito.mock(User.class);
        when(host.getId()).thenReturn(1L);
        when(tournament.getHost()).thenReturn(host);
        when(tournamentDataService.findById(10L)).thenReturn(Optional.of(tournament));

        // Exercise + Assert
        Assertions.assertFalse(securityService.canCloseRegistration(10L));
    }
}
