package ar.edu.itba.paw.models;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class PlatformTime {

    public static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private PlatformTime() {}

    public static Instant toInstant(final LocalDate date, final LocalTime time) {
        return date.atTime(time).atZone(ZONE).toInstant();
    }

    public static OffsetDateTime toOffsetDateTime(final Instant instant) {
        return instant == null ? null : instant.atZone(ZONE).toOffsetDateTime();
    }
}
