package ar.edu.itba.paw.webapp.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GeneralExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(final Exception exception) {
        return new ModelAndView("redirect:/errors/500");
    }
}
