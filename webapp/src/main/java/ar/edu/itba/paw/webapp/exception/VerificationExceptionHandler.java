package ar.edu.itba.paw.webapp.exception;

import ar.edu.itba.paw.models.exceptions.verificationFailure.VerificationFailureException;
import ar.edu.itba.paw.webapp.controller.VerificationController;
import ar.edu.itba.paw.webapp.utils.VerificationViews;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(assignableTypes = VerificationController.class)
public class VerificationExceptionHandler {

    private final MessageSource messageSource;

    @Autowired
    public VerificationExceptionHandler(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(VerificationFailureException.class)
    public ModelAndView handleVerificationFailure(
            final VerificationFailureException exception, final Locale locale) {
        return VerificationViews.buildErrorView(
                "verification.message." + exception.getMessage(), messageSource, locale, "/");
    }
}
