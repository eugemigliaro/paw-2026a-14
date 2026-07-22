package ar.edu.itba.paw.webapp.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

public final class ErrorPageViews {

    private ErrorPageViews() {}

    public static ModelAndView build(final String number) {
        return build(number, null);
    }

    public static ModelAndView build(final String number, final HttpStatus status) {
        final ModelAndView mav = new ModelAndView("errors/error-page");
        mav.addObject("number", number);
        if (status != null) {
            mav.setStatus(status);
        }
        return mav;
    }
}
