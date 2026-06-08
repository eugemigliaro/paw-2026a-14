package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.models.types.Sport;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

public final class ViewFormatUtils {

    private ViewFormatUtils() {}

    public static String formatInstant(
            final Instant instant, final Locale locale, final ZoneId zoneId) {
        return instant == null
                ? ""
                : DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(resolvedLocale(locale))
                        .withZone(zoneId)
                        .format(instant);
    }

    public static String formatDate(final TemporalAccessor temporal, final Locale locale) {
        return temporal == null ? "" : dateFormatter(locale).format(temporal);
    }

    public static String formatDateTime(final TemporalAccessor temporal, final Locale locale) {
        return temporal == null ? "" : scheduleFormatter(locale).format(temporal);
    }

    public static String formatCardDate(final TemporalAccessor temporal, final Locale locale) {
        return temporal == null ? "" : cardDateFormatter(locale).format(temporal);
    }

    public static String mediaClassFor(final Sport sport) {
        switch (sport) {
            case FOOTBALL:
                return "media-tile--football";
            case TENNIS:
                return "media-tile--tennis";
            case BASKETBALL:
                return "media-tile--basketball";
            case PADEL:
                return "media-tile--padel";
            case OTHER:
            default:
                return "media-tile--other";
        }
    }

    public static DateTimeFormatter scheduleFormatter(final Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(resolvedLocale(locale));
    }

    public static DateTimeFormatter dateFormatter(final Locale locale) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(resolvedLocale(locale));
    }

    public static DateTimeFormatter cardDateFormatter(final Locale locale) {
        return DateTimeFormatter.ofPattern("EEE, MMM d", resolvedLocale(locale));
    }

    public static DateTimeFormatter timeFormatter(final Locale locale) {
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(resolvedLocale(locale));
    }

    public static Locale resolvedLocale(final Locale locale) {
        return locale == null ? Locale.ENGLISH : locale;
    }
}
