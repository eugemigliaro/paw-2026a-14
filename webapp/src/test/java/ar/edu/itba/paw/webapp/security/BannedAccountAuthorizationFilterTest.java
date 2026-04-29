package ar.edu.itba.paw.webapp.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.UserRole;
import ar.edu.itba.paw.persistence.UserBanDao;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class BannedAccountAuthorizationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void redirectsBannedUserToBanPageForProtectedRoute() throws Exception {
        final UserBanDao userBanDao = Mockito.mock(UserBanDao.class);
        Mockito.when(userBanDao.findActiveBanForUser(Mockito.eq(7L), Mockito.any()))
                .thenReturn(Optional.of(sampleBan()));
        authenticateUser(7L);

        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/account");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        new BannedAccountAuthorizationFilter(userBanDao)
                .doFilter(request, response, new MockFilterChain());

        assertEquals(302, response.getStatus());
        assertEquals("/account/ban", response.getRedirectedUrl());
    }

    @Test
    void allowsBannedUserAppealRoute() throws Exception {
        final UserBanDao userBanDao = Mockito.mock(UserBanDao.class);
        Mockito.when(userBanDao.findActiveBanForUser(Mockito.eq(7L), Mockito.any()))
                .thenReturn(Optional.of(sampleBan()));
        authenticateUser(7L);

        final MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/account/ban/appeal");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        new BannedAccountAuthorizationFilter(userBanDao)
                .doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        assertNull(response.getRedirectedUrl());
    }

    private static UserBan sampleBan() {
        return new UserBan(
                11L,
                7L,
                1L,
                "Reason",
                Instant.now().plusSeconds(600),
                Instant.now(),
                null,
                0,
                null,
                null,
                null,
                null);
    }

    private static void authenticateUser(final Long userId) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUserPrincipal(
                                        new UserAccount(
                                                userId,
                                                "user@test.com",
                                                "user",
                                                "{bcrypt}hash",
                                                UserRole.USER,
                                                Instant.parse("2026-04-10T10:00:00Z"))),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
