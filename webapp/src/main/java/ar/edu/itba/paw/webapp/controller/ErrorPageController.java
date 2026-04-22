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
        return createErrorPageModelAndView(
                "page.title.404",
                "error.404.title",
                "error.404.eyebrow",
                "error.404.description",
                "404",
                locale);
    }

    @GetMapping("/errors/400")
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView showBadRequestPage(final Locale locale) {
        return createErrorPageModelAndView(
                "page.title.400",
                "error.400.title",
                "error.400.eyebrow",
                "error.400.description",
                "400",
                locale);
    }

    @GetMapping("/errors/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView showForbiddenPage(final Locale locale) {
        return createErrorPageModelAndView(
                "page.title.403",
                "error.403.title",
                "error.403.eyebrow",
                "error.403.description",
                "403",
                locale);
    }

    @GetMapping("/errors/500")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView showInternalServerErrorPage(final Locale locale) {
        return createErrorPageModelAndView(
                "page.title.500",
                "error.500.title",
                "error.500.eyebrow",
                "error.500.description",
                "500",
                locale);
    }

    private ModelAndView createErrorPageModelAndView(
            final String pageTitleKey,
            final String titleKey,
            final String eyebrowKey,
            final String descriptionKey,
            final String errorPageNumber,
            final Locale locale) {
        final ModelAndView mav = new ModelAndView("errors/error-page");
        mav.addObject("shell", ShellViewModelFactory.playerShell(messageSource, locale));
        mav.addObject("pageTitle", messageSource.getMessage(pageTitleKey, null, locale));
        mav.addObject("title", messageSource.getMessage(titleKey, null, locale));
        mav.addObject("eyebrow", messageSource.getMessage(eyebrowKey, null, locale));
        mav.addObject("description", messageSource.getMessage(descriptionKey, null, locale));
        mav.addObject("number", errorPageNumber);
        return mav;
    }
}
