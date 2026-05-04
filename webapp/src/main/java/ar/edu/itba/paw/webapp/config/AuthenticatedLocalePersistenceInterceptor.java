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
        if (language == null || language.isBlank()) {
            return true;
        }

        CurrentAuthenticatedUser.get()
                .ifPresent(
                        principal -> {
                            final Locale locale = LocaleContextHolder.getLocale();
                            userService.updatePreferredLanguage(
                                    principal.getUserId(),
                                    locale == null
                                            ? UserLanguages.normalizeLanguage(language)
                                            : UserLanguages.fromLocale(locale));
                        });
        return true;
    }
}
