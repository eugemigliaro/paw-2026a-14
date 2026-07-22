package ar.edu.itba.paw.webapp.exception;

import ar.edu.itba.paw.models.exceptions.pagination.InvalidPaginationException;
import ar.edu.itba.paw.webapp.utils.ErrorPageViews;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GeneralExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView handleResponseStatusException(
            final ResponseStatusException exception, final HttpServletResponse response) {
        exception
                .getResponseHeaders()
                .forEach(
                        (name, values) -> values.forEach(value -> response.addHeader(name, value)));
        final HttpStatus status = exception.getStatus();
        final String number = errorPageNumber(status);
        if (number == null) {
            throw exception;
        }
        return ErrorPageViews.build(number, status);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ModelAndView handleMethodNotAllowed(
            final HttpRequestMethodNotSupportedException exception,
            final HttpServletResponse response) {
        final String[] supportedMethods = exception.getSupportedMethods();
        if (supportedMethods != null && supportedMethods.length > 0) {
            response.setHeader("Allow", String.join(", ", supportedMethods));
        }
        return ErrorPageViews.build("405");
    }

    @ExceptionHandler({
        BindException.class,
        HttpMessageNotReadableException.class,
        InvalidPaginationException.class,
        MethodArgumentNotValidException.class,
        MissingServletRequestParameterException.class,
        MissingServletRequestPartException.class,
        ServletRequestBindingException.class,
        TypeMismatchException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleBadRequest(final Exception exception) {
        return ErrorPageViews.build("400");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNoHandlerFound(final NoHandlerFoundException exception) {
        return ErrorPageViews.build("404");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(final Exception exception) {
        return new ModelAndView("redirect:/errors/500");
    }

    private static String errorPageNumber(final HttpStatus status) {
        if (status == HttpStatus.BAD_REQUEST
                || status == HttpStatus.FORBIDDEN
                || status == HttpStatus.NOT_FOUND
                || status == HttpStatus.METHOD_NOT_ALLOWED
                || status == HttpStatus.CONFLICT
                || status == HttpStatus.INTERNAL_SERVER_ERROR) {
            return String.valueOf(status.value());
        }
        return null;
    }
}
