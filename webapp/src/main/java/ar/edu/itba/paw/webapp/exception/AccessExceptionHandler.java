package ar.edu.itba.paw.webapp.exception;

import ar.edu.itba.paw.services.exceptions.ForbiddenException;
import ar.edu.itba.paw.services.exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class AccessExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView notFound() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ModelAndView forbidden() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
