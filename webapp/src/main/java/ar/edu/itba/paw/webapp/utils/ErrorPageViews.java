package ar.edu.itba.paw.webapp.utils;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.ModelAndView;

public final class ErrorPageViews {

    private ErrorPageViews() {}

    public static ModelAndView build(
            final MessageSource messageSource, final String number, final Locale locale) {
        final ModelAndView mav = new ModelAndView("errors/error-page");
        mav.addObject("pageTitle", messageSource.getMessage("page.title." + number, null, locale));
        mav.addObject(
                "title", messageSource.getMessage("error." + number + ".title", null, locale));
        mav.addObject(
                "eyebrow", messageSource.getMessage("error." + number + ".eyebrow", null, locale));
        mav.addObject(
                "description",
                messageSource.getMessage("error." + number + ".description", null, locale));
        mav.addObject("number", number);
        return mav;
    }
}
