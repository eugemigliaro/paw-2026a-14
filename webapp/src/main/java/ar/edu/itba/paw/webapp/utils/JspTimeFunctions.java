package ar.edu.itba.paw.webapp.utils;

import java.time.temporal.TemporalAccessor;
import org.springframework.context.i18n.LocaleContextHolder;

public final class JspTimeFunctions {

    private JspTimeFunctions() {}

    public static String date(final TemporalAccessor temporal) {
        return ViewFormatUtils.formatDate(temporal, LocaleContextHolder.getLocale());
    }

    public static String dateTime(final TemporalAccessor temporal) {
        return ViewFormatUtils.formatDateTime(temporal, LocaleContextHolder.getLocale());
    }
}
