package ar.edu.itba.paw.webapp.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.UserBan;
import ar.edu.itba.paw.models.types.ReportReason;
import ar.edu.itba.paw.models.types.ReportStatus;
import ar.edu.itba.paw.models.types.ReportTargetType;
import ar.edu.itba.paw.persistence.UserBanDao;
import ar.edu.itba.paw.webapp.utils.AuthenticationUtils;
import ar.edu.itba.paw.webapp.utils.UserUtils;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class BannedAccountAuthorizationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void redirectsBannedUserToBanPageForProtectedRoute() throws Exception {
        final UserBanDao userBanDao = Mockito.mock(UserBanDao.class);
        Mockito.when(userBanDao.findActiveBanForUser(UserUtils.getUser(7L), Mockito.any()))
                .thenReturn(Optional.of(sampleBan()));
        AuthenticationUtils.authenticateUser(7L);

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
        Mockito.when(userBanDao.findActiveBanForUser(UserUtils.getUser(7L), Mockito.any()))
                .thenReturn(Optional.of(sampleBan()));
        AuthenticationUtils.authenticateUser(7L);

        final MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/account/ban/appeal");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        new BannedAccountAuthorizationFilter(userBanDao)
                .doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        assertNull(response.getRedirectedUrl());
    }

    private static ModerationReport sampleReport() {
        return new ModerationReport(
                1L,
                UserUtils.getUser(7L),
                ReportTargetType.USER,
                7L,
                ReportReason.SPAM,
                "Details",
                ReportStatus.RESOLVED,
                null,
                null,
                null,
                null,
                null,
                (short) 0,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now());
    }

    private static UserBan sampleBan() {
        return new UserBan(11L, sampleReport(), Instant.now().plusSeconds(600));
    }
}
