package ar.edu.itba.paw.webapp.exception;

import ar.edu.itba.paw.models.exceptions.registration.PasswordInvalidException;
import ar.edu.itba.paw.models.exceptions.verificationFailure.VerificationFailureException;
import ar.edu.itba.paw.webapp.controller.PasswordResetController;
import ar.edu.itba.paw.webapp.utils.VerificationViews;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(assignableTypes = PasswordResetController.class)
public class PasswordResetExceptionHandler {

    @ExceptionHandler(VerificationFailureException.class)
    public ModelAndView handleVerificationFailure(final VerificationFailureException exception) {
        return VerificationViews.buildErrorView(
                "verification.message." + exception.getMessage(), "/forgot-password");
    }

    @ExceptionHandler(PasswordInvalidException.class)
    public ModelAndView handlePasswordInvalid(final PasswordInvalidException exception) {
        return VerificationViews.buildErrorView(
                "auth.registration.error." + exception.getMessage(), "/forgot-password");
    }
}
