package ar.edu.itba.paw.webapp.security;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

public class RememberMeLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final String rememberMeCookieName;
    private final String rememberMeParameterName;

    public RememberMeLoginSuccessHandler(
            final String rememberMeCookieName, final String rememberMeParameterName) {
        this.rememberMeCookieName = rememberMeCookieName;
        this.rememberMeParameterName = rememberMeParameterName;
    }

    @Override
    public void onAuthenticationSuccess(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Authentication authentication)
            throws ServletException, IOException {
        if (!rememberMeRequested(request)) {
            clearRememberMeCookie(request, response);
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }

    private boolean rememberMeRequested(final HttpServletRequest request) {
        final String value = request.getParameter(rememberMeParameterName);
        return value != null
                && ("true".equalsIgnoreCase(value)
                        || "on".equalsIgnoreCase(value)
                        || "yes".equalsIgnoreCase(value)
                        || "1".equals(value));
    }

    private void clearRememberMeCookie(
            final HttpServletRequest request, final HttpServletResponse response) {
        final Cookie cookie = new Cookie(rememberMeCookieName, "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath(cookiePath(request));
        response.addCookie(cookie);
    }

    private static String cookiePath(final HttpServletRequest request) {
        final String contextPath = request.getContextPath();
        return contextPath == null || contextPath.isBlank() ? "/" : contextPath;
    }
}
