package ar.edu.itba.paw.webapp.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class LoginFailureHandlerTest {

    @Test
    void failureHandlerRedirectsUnverifiedAccountsToVerificationErrorCode()
            throws java.io.IOException, ServletException {
        final LoginFailureHandler handler = new LoginFailureHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        request.setParameter("email", "pending@test.com");

        handler.onAuthenticationFailure(
                request,
                response,
                new EmailNotVerifiedAuthenticationException("Email verification required"));

        assertNotNull(response.getRedirectedUrl());
        assertTrue(response.getRedirectedUrl().contains("/login?error=verify"));
        assertTrue(response.getRedirectedUrl().contains("email=pending@test.com"));
    }

    @Test
    void failureHandlerRedirectsPasswordSetupAccountsToDedicatedErrorCode()
            throws java.io.IOException, ServletException {
        final LoginFailureHandler handler = new LoginFailureHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        request.setParameter("email", "legacy@test.com");

        handler.onAuthenticationFailure(
                request,
                response,
                new PasswordSetupRequiredAuthenticationException("Password setup required"));

        assertNotNull(response.getRedirectedUrl());
        assertTrue(response.getRedirectedUrl().contains("/login?error=set-password"));
        assertTrue(response.getRedirectedUrl().contains("email=legacy@test.com"));
    }

    @Test
    void failureHandlerRedirectsBadCredentialsToInvalidErrorCode()
            throws java.io.IOException, ServletException {
        final LoginFailureHandler handler = new LoginFailureHandler();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        request.setParameter("email", "player@test.com");

        handler.onAuthenticationFailure(
                request, response, new BadCredentialsException("Invalid credentials"));

        assertNotNull(response.getRedirectedUrl());
        assertTrue(response.getRedirectedUrl().contains("/login?error=invalid"));
        assertTrue(response.getRedirectedUrl().contains("email=player@test.com"));
    }
}
