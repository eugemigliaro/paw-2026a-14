package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.services.VerificationFailureException;
import ar.edu.itba.paw.services.VerificationFailureReason;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
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

    public static String titleFor(
            final VerificationFailureReason reason, final MessageSource ms, final Locale locale) {
        switch (reason) {
            case EXPIRED:
                return ms.getMessage("verification.error.expired", null, locale);
            case ALREADY_USED:
                return ms.getMessage("verification.error.alreadyUsed", null, locale);
            case INVALID_ACTION:
                return ms.getMessage("verification.error.invalidAction", null, locale);
            case NOT_FOUND:
            default:
                return ms.getMessage("verification.error.notFound", null, locale);
        }
    }

    public static ModelAndView buildErrorView(
            final VerificationFailureException ex,
            final MessageSource ms,
            final Locale locale,
            final String backHref) {
        final ModelAndView mav = new ModelAndView("verification/error");
        mav.addObject("shell", ShellViewModelFactory.browseShell(ms, locale));
        mav.addObject("title", titleFor(ex.getReason(), ms, locale));
        mav.addObject("message", ex.getMessage());
        mav.addObject("backHref", backHref);
        return mav;
    }
}
