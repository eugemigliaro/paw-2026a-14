package ar.edu.itba.paw.services.security;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.SecurityService;
import ar.edu.itba.paw.services.UserService;
import java.lang.reflect.Method;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("securityService")
public class SecurityServiceImpl implements SecurityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityServiceImpl.class);

    private final MatchDao matchDao;
    private final PlayerReviewService playerReviewService;
    private final UserService userService;

    public SecurityServiceImpl(
            final MatchDao matchDao,
            final PlayerReviewService playerReviewService,
            final UserService userService) {
        this.matchDao = matchDao;
        this.playerReviewService = playerReviewService;
        this.userService = userService;
    }

    @Override
    public boolean isAuthenticated() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        final Object principal = auth.getPrincipal();
        return principal != null && extractUserIdFromPrincipal(principal) != null;
    }

    @Override
    public Long currentUserId() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        final Object principal = auth.getPrincipal();
        return extractUserIdFromPrincipal(principal);
    }

    private Long extractUserIdFromPrincipal(final Object principal) {
        if (principal == null) return null;
        try {
            final Method m = principal.getClass().getMethod("getUserId");
            final Object id = m.invoke(principal);
            if (id instanceof Long) return (Long) id;
            if (id instanceof Integer) return ((Integer) id).longValue();
            return null;
        } catch (final Exception e) {
            LOGGER.debug(
                    "Unable to extract user id from principal of type {}",
                    principal.getClass().getName());
            return null;
        }
    }

    @Override
    public boolean isHost(final Long matchId) {
        if (matchId == null) {
            return false;
        }
        final Long current = currentUserId();
        if (current == null) {
            return false;
        }
        final Optional<Match> match = matchDao.findById(matchId);
        return match.map(m -> current.equals(m.getHostUserId())).orElse(false);
    }

    @Override
    public boolean hasReviewed(final String username) {
        if (username == null) return false;
        final Long current = currentUserId();
        if (current == null) return false;
        final Optional<User> reviewed = userService.findByUsername(username);
        return reviewed.map(
                        user ->
                                playerReviewService
                                        .findReviewByPair(current, user.getId())
                                        .isPresent())
                .orElse(false);
    }
}
