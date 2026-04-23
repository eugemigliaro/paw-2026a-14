package ar.edu.itba.paw.webapp.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

public class ContinueFlagLoginEntryPoint extends LoginUrlAuthenticationEntryPoint {

    public ContinueFlagLoginEntryPoint() {
        super("/login");
    }

    @Override
    protected String buildRedirectUrlToLoginPage(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AuthenticationException authException) {
        return "/login?continue";
    }
}
