package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.models.types.Sport;
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

    public static String cardDate(final TemporalAccessor temporal) {
        return ViewFormatUtils.formatCardDate(temporal, LocaleContextHolder.getLocale());
    }

    public static String time(final TemporalAccessor temporal) {
        return temporal == null
                ? ""
                : ViewFormatUtils.timeFormatter(LocaleContextHolder.getLocale()).format(temporal);
    }

    public static String mediaClass(final Sport sport) {
        return ViewFormatUtils.mediaClassFor(sport);
    }
}
