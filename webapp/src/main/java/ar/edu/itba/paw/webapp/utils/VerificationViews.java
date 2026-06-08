package ar.edu.itba.paw.webapp.utils;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.springframework.web.servlet.ModelAndView;

public final class VerificationViews {

    private VerificationViews() {}

    public static DateTimeFormatter expiryFormatter(final Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale == null ? Locale.ENGLISH : locale);
    }

    public static ModelAndView buildErrorView(final String messageCode, final String backHref) {
        final ModelAndView mav = new ModelAndView("verification/error");
        mav.addObject("messageCode", messageCode);
        mav.addObject("backHref", backHref);
        return mav;
    }
}
