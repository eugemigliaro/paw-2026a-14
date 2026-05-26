package ar.edu.itba.paw.webapp.security;

import ar.edu.itba.paw.persistence.UserBanDao;
import java.io.IOException;
import java.time.Instant;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

public class BannedAccountAuthorizationFilter extends OncePerRequestFilter {

    private final UserBanDao userBanDao;

    public BannedAccountAuthorizationFilter(final UserBanDao userBanDao) {
        this.userBanDao = userBanDao;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain)
            throws ServletException, IOException {
        final AuthenticatedUserPrincipal principal = CurrentAuthenticatedUser.get().orElse(null);
        final String path = requestPath(request);
        if (principal != null
                && userBanDao.findActiveBanForUser(principal.getUser(), Instant.now()).isPresent()
                && !isAllowedForBannedAccount(path, request.getMethod())) {
            response.sendRedirect(request.getContextPath() + "/account/ban");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String requestPath(final HttpServletRequest request) {
        final String contextPath = request.getContextPath();
        final String uri = request.getRequestURI();
        return contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri;
    }

    private static boolean isAllowedForBannedAccount(final String path, final String method) {
        if ("GET".equalsIgnoreCase(method)
                && (path.startsWith("/css/")
                        || path.startsWith("/js/")
                        || path.startsWith("/assets/")
                        || path.startsWith("/images/"))) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && "/account/ban".equals(path)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && "/account/ban/appeal".equals(path)) {
            return true;
        }
        return "POST".equalsIgnoreCase(method) && "/logout".equals(path);
    }
}
