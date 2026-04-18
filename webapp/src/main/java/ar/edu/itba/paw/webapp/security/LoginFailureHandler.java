package ar.edu.itba.paw.webapp.security;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.web.util.UriComponentsBuilder;

public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AuthenticationException exception)
            throws IOException, ServletException {
        final String errorCode = resolveErrorCode(exception);
        final UriComponentsBuilder builder =
                UriComponentsBuilder.fromPath(request.getContextPath() + "/login")
                        .queryParam("error", errorCode);

        final String email = request.getParameter("email");
        if (email != null && !email.isBlank()) {
            builder.queryParam("email", email);
        }

        getRedirectStrategy()
                .sendRedirect(request, response, builder.build().encode().toUriString());
    }

    private static String resolveErrorCode(final AuthenticationException exception) {
        if (exception instanceof EmailNotVerifiedAuthenticationException) {
            return "verify";
        }
        if (exception instanceof PasswordSetupRequiredAuthenticationException) {
            return "set-password";
        }
        if (exception instanceof BadCredentialsException) {
            return "invalid";
        }
        return "invalid";
    }
}
