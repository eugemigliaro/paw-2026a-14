package ar.edu.itba.paw.services.security;

import static org.mockito.Mockito.when;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchDao;
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
    private SecurityServiceImpl securityService;

    @BeforeEach
    void setUp() {
        matchDao = Mockito.mock(MatchDao.class);
        securityService = new SecurityServiceImpl(matchDao);
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
}
