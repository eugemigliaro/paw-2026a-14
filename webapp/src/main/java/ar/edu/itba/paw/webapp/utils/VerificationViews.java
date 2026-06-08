package ar.edu.itba.paw.webapp.utils;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.ModelAndView;

public final class VerificationViews {

    private VerificationViews() {}

    public static DateTimeFormatter expiryFormatter(final Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale == null ? Locale.ENGLISH : locale);
    }

    public static ModelAndView buildErrorView(
            final String messageKey,
            final MessageSource ms,
            final Locale locale,
            final String actionHref) {
        final ModelAndView mav = new ModelAndView("verification/error");
        mav.addObject("title", ms.getMessage(messageKey, null, locale));
        mav.addObject("message", ms.getMessage(messageKey, null, locale));
        mav.addObject("actionHref", actionHref);
        return mav;
    }
}
