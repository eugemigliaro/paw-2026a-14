package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.utils.ErrorPageViews;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ErrorPageController {

    private final MessageSource messageSource;

    @Autowired
    public ErrorPageController(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @GetMapping("/errors/404")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView showNotFoundPage(final Locale locale) {
        return ErrorPageViews.build(messageSource, "404", locale);
    }

    @GetMapping("/errors/400")
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView showBadRequestPage(final Locale locale) {
        return ErrorPageViews.build(messageSource, "400", locale);
    }

    @GetMapping("/errors/405")
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ModelAndView showMethodNotAllowedPage(final Locale locale) {
        return ErrorPageViews.build(messageSource, "405", locale);
    }

    @GetMapping("/errors/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView showForbiddenPage(final Locale locale) {
        return ErrorPageViews.build(messageSource, "403", locale);
    }

    @GetMapping("/errors/409")
    @ResponseStatus(HttpStatus.CONFLICT)
    public ModelAndView showConflictPage(final Locale locale) {
        return ErrorPageViews.build(messageSource, "409", locale);
    }

    @GetMapping("/errors/500")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView showInternalServerErrorPage(final Locale locale) {
        return ErrorPageViews.build(messageSource, "500", locale);
    }
}
