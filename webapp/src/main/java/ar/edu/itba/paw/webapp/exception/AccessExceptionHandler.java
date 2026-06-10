package ar.edu.itba.paw.webapp.exception;

import ar.edu.itba.paw.models.exceptions.ForbiddenException;
import ar.edu.itba.paw.models.exceptions.NotFoundException;
import ar.edu.itba.paw.webapp.utils.ErrorPageViews;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessExceptionHandler {

    private final MessageSource messageSource;

    public AccessExceptionHandler(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView notFound(final Locale locale) {
        return ErrorPageViews.build(messageSource, "404", locale);
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView forbidden(final Locale locale) {
        return ErrorPageViews.build(messageSource, "403", locale);
    }
}
