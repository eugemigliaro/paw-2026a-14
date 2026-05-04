package ar.edu.itba.paw.services.security;

import static org.mockito.Mockito.when;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PlayerReview;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.UserService;
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
        private final Long id;

        TestPrincipal(final Long id) {
            this.id = id;
        }

        public Long getUserId() {
            return id;
        }
    }

    @Test
    void isAuthenticatedAndCurrentUserId() {
        final TestPrincipal p = new TestPrincipal(42L);
        final var token =
                new UsernamePasswordAuthenticationToken(
                        p, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(token);

        Assertions.assertTrue(securityService.isAuthenticated());
        Assertions.assertEquals(42L, securityService.currentUserId());
    }

    @Test
    void isHostDelegatesToMatchService() {
        final TestPrincipal p = new TestPrincipal(99L);
        final var token =
                new UsernamePasswordAuthenticationToken(
                        p, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(token);

        final Match match = Mockito.mock(Match.class);
        when(match.getHostUserId()).thenReturn(99L);
        when(matchDao.findById(10L)).thenReturn(Optional.of(match));

        Assertions.assertTrue(securityService.isHost(10L));
    }

    @Test
    void hasReviewedDelegatesToPlayerReviewService() {
        final TestPrincipal p = new TestPrincipal(7L);
        final var token =
                new UsernamePasswordAuthenticationToken(
                        p, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(token);

        final User reviewed = new User(13L, "u@example.com", "reviewedUser");
        when(userService.findByUsername("reviewedUser")).thenReturn(Optional.of(reviewed));
        when(playerReviewService.findReviewByPair(7L, 13L))
                .thenReturn(Optional.of(Mockito.mock(PlayerReview.class)));

        Assertions.assertTrue(securityService.hasReviewed("reviewedUser"));
    }
}
