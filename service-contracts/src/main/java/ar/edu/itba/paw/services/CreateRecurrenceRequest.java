package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.types.RecurrenceEndMode;
import ar.edu.itba.paw.models.types.RecurrenceFrequency;
import java.time.LocalDate;

public class CreateRecurrenceRequest {

    private final RecurrenceFrequency frequency;
    private final RecurrenceEndMode endMode;
    private final LocalDate untilDate;
    private final Integer occurrenceCount;

    public CreateRecurrenceRequest(
            final RecurrenceFrequency frequency,
            final RecurrenceEndMode endMode,
            final LocalDate untilDate,
            final Integer occurrenceCount) {
        this.frequency = frequency;
        this.endMode = endMode;
        this.untilDate = untilDate;
        this.occurrenceCount = occurrenceCount;
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
}
