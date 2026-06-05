package ar.edu.itba.paw.services.security;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchDao;
import ar.edu.itba.paw.services.SecurityService;
import java.lang.reflect.Method;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("securityService")
@Transactional(readOnly = true)
public class SecurityServiceImpl implements SecurityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityServiceImpl.class);

    private final MatchDao matchDao;

    public SecurityServiceImpl(final MatchDao matchDao) {
        this.matchDao = matchDao;
    }

    @Override
    public boolean isAuthenticated() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        final Object principal = auth.getPrincipal();
        return principal != null && extractUserFromPrincipal(principal) != null;
    }

    @Override
    public User currentUser() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        final Object principal = auth.getPrincipal();
        return extractUserFromPrincipal(principal);
    }

    private User extractUserFromPrincipal(final Object principal) {
        if (principal == null) return null;
        try {
            final Method m = principal.getClass().getMethod("getUser");
            final Object id = m.invoke(principal);
            if (id instanceof User) return (User) id;
            return null;
        } catch (final Exception e) {
            LOGGER.debug(
                    "Unable to extract user from principal of type {}",
                    principal.getClass().getName(),
                    e);
            return null;
        }
    }

    @Override
    public boolean isHost(final Long matchId) {
        if (matchId == null) {
            return false;
        }
        final User current = currentUser();
        if (current == null) {
            return false;
        }
        final Optional<Match> match = matchDao.findById(matchId);
        return match.map(m -> current.getId().equals(m.getHost().getId())).orElse(false);
    }
}
