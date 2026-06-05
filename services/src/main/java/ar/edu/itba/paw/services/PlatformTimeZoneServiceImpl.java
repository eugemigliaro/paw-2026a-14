package ar.edu.itba.paw.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PlatformTimeZoneServiceImpl implements PlatformTimeZoneService {

    private final ZoneId defaultZone;

    public PlatformTimeZoneServiceImpl(
            @Value("${app.timezone:" + PlatformTimeZoneService.DEFAULT_TIMEZONE + "}")
                    final String configuredTimezone) {
        this.defaultZone = resolveConfiguredZone(configuredTimezone);
    }

    public static PlatformTimeZoneService argentinaDefault() {
        return new PlatformTimeZoneServiceImpl(PlatformTimeZoneService.DEFAULT_TIMEZONE);
    }

    @Override
    public ZoneId defaultZone() {
        return defaultZone;
    }

    @Override
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

    @Override
    public String normalizeOrDefault(final String timezone) {
        return resolveOrDefault(timezone).getId();
    }

    @Override
    public Instant toInstant(final LocalDate date, final LocalTime time, final String timezone) {
        return date.atTime(time).atZone(resolveOrDefault(timezone)).toInstant();
    }

    @Override
    public LocalDateTime toLocalDateTime(final Instant instant) {
        return toLocalDateTime(instant, null);
    }

    @Override
    public LocalDateTime toLocalDateTime(final Instant instant, final String timezone) {
        return LocalDateTime.ofInstant(instant, resolveOrDefault(timezone));
    }

    private static ZoneId resolveConfiguredZone(final String configuredTimezone) {
        if (configuredTimezone == null || configuredTimezone.isBlank()) {
            return ZoneId.of(PlatformTimeZoneService.DEFAULT_TIMEZONE);
        }
        try {
            return ZoneId.of(configuredTimezone.trim());
        } catch (final Exception ignored) {
            return ZoneId.of(PlatformTimeZoneService.DEFAULT_TIMEZONE);
        }
    }
}
