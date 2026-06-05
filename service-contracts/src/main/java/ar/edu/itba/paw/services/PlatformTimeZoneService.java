package ar.edu.itba.paw.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public interface PlatformTimeZoneService {

    String DEFAULT_TIMEZONE = "America/Argentina/Buenos_Aires";

    ZoneId defaultZone();

    ZoneId resolveOrDefault(String timezone);

    String normalizeOrDefault(String timezone);

    Instant toInstant(LocalDate date, LocalTime time, String timezone);

    LocalDateTime toLocalDateTime(Instant instant);

    LocalDateTime toLocalDateTime(Instant instant, String timezone);
}
