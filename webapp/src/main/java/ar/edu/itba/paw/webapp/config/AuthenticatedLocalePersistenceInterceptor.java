package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import java.util.Locale;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthenticatedLocalePersistenceInterceptor implements HandlerInterceptor {

    static final String PERSIST_LANGUAGE_PARAMETER = "persistLang";
    private final UserService userService;

    public AuthenticatedLocalePersistenceInterceptor(final UserService userService) {
        this.userService = Objects.requireNonNull(userService);
    }

    @Override
    public boolean preHandle(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler) {
        final String language = request.getParameter("lang");
        if (!"true".equals(request.getParameter(PERSIST_LANGUAGE_PARAMETER))
                || !UserLanguages.isSupportedLanguage(language)) {
            return true;
        }

        CurrentAuthenticatedUser.get()
                .ifPresent(
                        principal -> {
                            final String preferredLanguage = resolvedLanguage(language);
                            userService.updatePreferredLanguage(
                                    principal.getUser(), preferredLanguage);
                        });
        return true;
    }

    private static String resolvedLanguage(final String language) {
        final Locale locale = LocaleContextHolder.getLocale();
        if (locale != null && UserLanguages.isSupportedLanguage(locale.getLanguage())) {
            return UserLanguages.fromLocale(locale);
        }
        return UserLanguages.normalizeLanguage(language);
    }
}
