package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.springframework.context.MessageSource;

public final class ViewFormatUtils {

    private ViewFormatUtils() {}

    public static String formatInstant(final Instant instant, final Locale locale) {
        return instant == null
                ? ""
                : DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(resolvedLocale(locale))
                        .withZone(ZoneId.systemDefault())
                        .format(instant);
    }

    public static String priceLabel(
            final BigDecimal pricePerPlayer,
            final Locale locale,
            final MessageSource messageSource) {
        if (pricePerPlayer == null) {
            return messageSource.getMessage("price.tbd", null, locale);
        }
        return pricePerPlayer.compareTo(BigDecimal.ZERO) == 0
                ? messageSource.getMessage("price.free", null, locale)
                : messageSource.getMessage("price.amount", new Object[] {pricePerPlayer}, locale);
    }

    public static String sportLabel(
            final Sport sport, final Locale locale, final MessageSource messageSource) {
        return messageSource.getMessage(
                "sport." + sport.getDbValue(),
                null,
                sport.getDisplayName(),
                resolvedLocale(locale));
    }

    public static String recurringLabel(
            final Match match, final Locale locale, final MessageSource messageSource) {
        return match.isRecurringOccurrence()
                ? messageSource.getMessage("event.recurringBadge", null, locale)
                : null;
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

    public static DateTimeFormatter timeFormatter(final Locale locale) {
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(resolvedLocale(locale));
    }

    public static Locale resolvedLocale(final Locale locale) {
        return locale == null ? Locale.ENGLISH : locale;
    }
}
