package ar.edu.itba.paw.webapp.exception;

import ar.edu.itba.paw.models.exceptions.verificationFailure.VerificationFailureException;
import ar.edu.itba.paw.webapp.controller.VerificationController;
import ar.edu.itba.paw.webapp.utils.VerificationViews;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(assignableTypes = VerificationController.class)
public class VerificationExceptionHandler {

    @ExceptionHandler(VerificationFailureException.class)
    public ModelAndView handleVerificationFailure(final VerificationFailureException exception) {
        return VerificationViews.buildErrorView(
                "verification.message." + exception.getMessage(), "/");
    }
}
