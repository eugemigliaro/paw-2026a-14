package ar.edu.itba.paw.services.security;

import static org.mockito.Mockito.when;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.UserService;
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

    private MatchDao matchDao;
    private PlayerReviewService playerReviewService;
    private UserService userService;
    private SecurityServiceImpl securityService;

    @BeforeEach
    void setUp() {
        matchDao = Mockito.mock(MatchDao.class);
        playerReviewService = Mockito.mock(PlayerReviewService.class);
        userService = Mockito.mock(UserService.class);
        securityService = new SecurityServiceImpl(matchDao, playerReviewService, userService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    static class TestPrincipal {
        private final User user;

        TestPrincipal(final User user) {
            this.user = user;
        }

        public User getUser() {
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

        when(matchDao.findById(10L)).thenReturn(Optional.of(match));

        Assertions.assertTrue(securityService.isHost(10L));
    }

    @Test
    void hasReviewedDelegatesToPlayerReviewService() {
        final User u = UserUtils.getUser(7L);
        final TestPrincipal p = new TestPrincipal(u);
        final var token =
                new UsernamePasswordAuthenticationToken(
                        p, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(token);

        final User reviewed = UserUtils.getUser(13L);
        when(userService.findByUsername(reviewed.getUsername())).thenReturn(Optional.of(reviewed));
        when(playerReviewService.findReviewByPair(
                        Mockito.argThat(user -> user.getId().equals(7L)), Mockito.eq(reviewed)))
                .thenReturn(Optional.of(Mockito.mock(PlayerReview.class)));

        Assertions.assertTrue(securityService.hasReviewed(reviewed.getUsername()));
    }
}
