package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.RecurrenceEndMode;
import ar.edu.itba.paw.models.RecurrenceFrequency;
import java.time.LocalDate;
import java.time.ZoneId;

public class CreateRecurrenceRequest {

    private final RecurrenceFrequency frequency;
    private final RecurrenceEndMode endMode;
    private final LocalDate untilDate;
    private final Integer occurrenceCount;
    private final ZoneId zoneId;

    public CreateRecurrenceRequest(
            final RecurrenceFrequency frequency,
            final RecurrenceEndMode endMode,
            final LocalDate untilDate,
            final Integer occurrenceCount,
            final ZoneId zoneId) {
        this.frequency = frequency;
        this.endMode = endMode;
        this.untilDate = untilDate;
        this.occurrenceCount = occurrenceCount;
        this.zoneId = zoneId;
    }

    public RecurrenceFrequency getFrequency() {
        return frequency;
    }

    public RecurrenceEndMode getEndMode() {
        return endMode;
    }

    public LocalDate getUntilDate() {
        return untilDate;
    }

    public Integer getOccurrenceCount() {
        return occurrenceCount;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }
}
