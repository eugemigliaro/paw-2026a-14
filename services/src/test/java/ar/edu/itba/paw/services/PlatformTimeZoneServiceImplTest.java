package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class PlatformTimeZoneServiceImplTest {

    @Test
    void blankTimezoneFallsBackToArgentina() {
        // 1. Arrange
        final PlatformTimeZoneService resolver = PlatformTimeZoneServiceImpl.argentinaDefault();

        // 2. Exercise
        final Instant instant =
                resolver.toInstant(LocalDate.of(2030, 4, 1), LocalTime.of(9, 0), "");

        // 3. Assert
        assertEquals(ZoneId.of("America/Argentina/Buenos_Aires"), resolver.defaultZone());
        assertEquals(Instant.parse("2030-04-01T12:00:00Z"), instant);
    }

    @Test
    void explicitValidTimezoneIsHonored() {
        // 1. Arrange
        final PlatformTimeZoneService resolver = PlatformTimeZoneServiceImpl.argentinaDefault();

        // 2. Exercise
        final Instant instant =
                resolver.toInstant(LocalDate.of(2030, 4, 1), LocalTime.of(9, 0), "UTC");

        // 3. Assert
        assertEquals(Instant.parse("2030-04-01T09:00:00Z"), instant);
    }

    @Test
    void invalidTimezoneFallsBackToArgentina() {
        // 1. Arrange
        final PlatformTimeZoneService resolver = PlatformTimeZoneServiceImpl.argentinaDefault();

        // 2. Exercise
        final String normalized = resolver.normalizeOrDefault("not-a-time-zone");

        // 3. Assert
        assertEquals("America/Argentina/Buenos_Aires", normalized);
    }
}
