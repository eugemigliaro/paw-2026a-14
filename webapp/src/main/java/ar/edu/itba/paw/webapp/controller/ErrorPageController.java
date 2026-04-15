package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
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
        final ModelAndView mav = new ModelAndView("errors/not-found");
        mav.addObject("shell", ShellViewModelFactory.browseShell(messageSource, locale));
        return mav;
    }
}
