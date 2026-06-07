package ar.edu.itba.paw.webapp.security.annotation;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(final MethodParameter parameter) {
        return User.class.isAssignableFrom(parameter.getParameterType())
                && (parameter.hasParameterAnnotation(CurrentUser.class)
                        || parameter.hasParameterAnnotation(AuthenticatedUser.class));
    }

    @Override
    public Object resolveArgument(
            final MethodParameter parameter,
            final ModelAndViewContainer mavContainer,
            final NativeWebRequest webRequest,
            final WebDataBinderFactory binderFactory) {

        final User user = SecurityControllerUtils.currentUserOrNull();

        if (parameter.hasParameterAnnotation(AuthenticatedUser.class) && user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return user;
    }
}
