package ar.edu.itba.paw.webapp.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public class AppTimeZoneResolver {

    public static final String DEFAULT_TIMEZONE = "America/Argentina/Buenos_Aires";

    private final ZoneId defaultZone;

    public AppTimeZoneResolver(final String configuredTimezone) {
        this.defaultZone = resolveConfiguredZone(configuredTimezone);
    }

    public static AppTimeZoneResolver argentinaDefault() {
        return new AppTimeZoneResolver(DEFAULT_TIMEZONE);
    }

    public ZoneId defaultZone() {
        return defaultZone;
    }

    public ZoneId resolveOrDefault(final String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return defaultZone;
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (final Exception ignored) {
            return defaultZone;
        }
    }

    public String normalizeOrDefault(final String timezone) {
        return resolveOrDefault(timezone).getId();
    }

    public Instant toInstant(final LocalDate date, final LocalTime time, final String timezone) {
        return date.atTime(time).atZone(resolveOrDefault(timezone)).toInstant();
    }

    public LocalDateTime toLocalDateTime(final Instant instant) {
        return toLocalDateTime(instant, null);
    }

    public LocalDateTime toLocalDateTime(final Instant instant, final String timezone) {
        return LocalDateTime.ofInstant(instant, resolveOrDefault(timezone));
    }

    private static ZoneId resolveConfiguredZone(final String configuredTimezone) {
        if (configuredTimezone == null || configuredTimezone.isBlank()) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
        try {
            return ZoneId.of(configuredTimezone.trim());
        } catch (final Exception ignored) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }
}
